package com.asterway.identity.service;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountExecutionPort;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountTimeZoneApi;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSnapshot;
import com.asterway.identity.api.model.DeviceTimeZoneResult;
import com.asterway.identity.api.model.ZoneInput;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.mapper.AccountTimeZoneMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.utils.AccountSnapshotUtils;
import com.asterway.identity.utils.AccountValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

// 时区在旧业务日结束后生效；修改目标不会不断顺延原来的切换边界。
@Service
public class AccountTimeZoneService implements AccountTimeZoneApi {

    private final AccountTimeZoneMapper timeZoneMapper;

    private static final Logger log = LoggerFactory.getLogger(AccountTimeZoneService.class);

    private final AccountMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final AccountAccessService accessService;
    private final AccountExecutionPort executions;
    private final AccountOperationPort idempotencyService;

    public AccountTimeZoneService(
            AccountTimeZoneMapper timeZoneMapper,
            AccountMapper mapper,
            PlatformTransactionManager transactionManager,
            AccountAccessService accessService,
            AccountExecutionPort executions,
            AccountOperationPort idempotencyService
    ) {
        this.timeZoneMapper = timeZoneMapper;
        this.mapper = mapper;
        this.transactionManager = transactionManager;
        this.accessService = accessService;
        this.executions = executions;
        this.idempotencyService = idempotencyService;
    }

    public void applyDue(AccountContext context) {
        TransactionUtils.independent(transactionManager, () -> applyLocked(context));
    }

    @Override
    public void applyPending() {
        for (AccountState account : timeZoneMapper.dueZoneAccounts()) {
            AccountContext context = new AccountContext(
                    null, AccountContext.USER_SCOPE_PREFIX + account.id(), account.id(), UUID.randomUUID().toString(),
                    account.credentialVersion(), account.securityVersion()
            );
            try {
                applyDue(context);
            } catch (BusinessException exception) {
                log.info("账户时区到期处理等待业务条件，accountId={}, code={}",
                        account.id(), exception.getErrorCode().code());
            } catch (RuntimeException exception) {
                log.error("账户时区到期处理失败，accountId={}", account.id(), exception);
            }
        }
    }

    private Boolean applyLocked(AccountContext context) {
        AccountState account = accessService.locked(context);
        OffsetDateTime boundary = account.pendingZoneEffectiveAt();
        if (boundary == null || mapper.now().isBefore(boundary)) {
            return Boolean.FALSE;
        }
        // 执行模块尚未实现可信暂停适配时不能自行改写执行或投入历史。
        if (executions.unfinishedCount(account.id()) > 0) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        ZonePeriod previous = timeZoneMapper.findZone(account.currentZonePeriodId(), account.id());
        ZonePeriod next = ZonePeriod.create(account.id(), previous.revision() + 1,
                account.pendingZoneId(), boundary, "DEVICE_CHANGE");
        timeZoneMapper.closeZone(previous.id(), boundary);
        timeZoneMapper.insertZone(next);
        timeZoneMapper.applyZone(account.id(), next.id());
        return Boolean.TRUE;
    }

    @Override
    public DeviceTimeZoneResult synchronize(AccountContext context, ZoneInput input) {
        AccountValidationUtils.zone(input.detectedZoneId());
        long expectedVersion = AccountValidationUtils.version(input.expectedVersion());
        applyDue(context);
        AccountOperationPort.Execution<DeviceTimeZoneResult> result = idempotencyService.execute(
                context, AccountOperationCode.SYNCHRONIZE_TIME_ZONE.code(), input, DeviceTimeZoneResult.class,
                () -> AccountOperationPort.success(updateLocked(context, input.detectedZoneId(), expectedVersion))
        );
        return result.data();
    }

    private DeviceTimeZoneResult updateLocked(AccountContext context, String target, long expectedVersion) {
        AccountState account = accessService.locked(context);
        accessService.requireVersion(account, expectedVersion);
        ZonePeriod period = timeZoneMapper.findZone(account.currentZonePeriodId(), account.id());
        boolean changed = false;
        if (target.equals(period.zoneId())) {
            if (account.pendingZoneId() != null) {
                timeZoneMapper.updatePendingZone(account.id(), null, null);
                changed = true;
            }
        } else if (!target.equals(account.pendingZoneId())) {
            OffsetDateTime boundary = account.pendingZoneEffectiveAt();
            if (boundary == null) {
                ZoneId zone = ZoneId.of(period.zoneId());
                boundary = mapper.now().toInstant().atZone(zone).toLocalDate().plusDays(1)
                        .atStartOfDay(zone).toOffsetDateTime();
            }
            timeZoneMapper.updatePendingZone(account.id(), target, boundary);
            changed = true;
        }
        AccountState current = mapper.findState(account.id());
        ZonePeriod currentPeriod = timeZoneMapper.findZone(current.currentZonePeriodId(), current.id());
        AccountSnapshot snapshot = AccountSnapshotUtils.assemble(current, currentPeriod);
        DeviceTimeZoneResult result = new DeviceTimeZoneResult(
                snapshot, changed, mapper.now().toInstant()
        );
        return result;
    }
}
