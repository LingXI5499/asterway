package com.asterway.identity.service;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountExecutionPort;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.DeviceTimeZoneResult;
import com.asterway.identity.api.model.AccountModels.SelfAccount;
import com.asterway.identity.api.model.AccountModels.ZoneInput;
import com.asterway.identity.assembler.AccountSnapshotAssembler;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.validation.AccountValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

// 时区在旧业务日结束后生效；修改目标不会不断顺延原来的切换边界。
@Service
public class AccountTimeZoneService {

    private static final Logger log = LoggerFactory.getLogger(AccountTimeZoneService.class);

    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final AccountAccessService accessService;
    private final AccountExecutionPort executions;
    private final AccountIdempotencyService idempotencyService;
    private final AccountValidator validator;
    private final AccountSnapshotAssembler snapshotAssembler;

    public AccountTimeZoneService(
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor,
            AccountAccessService accessService,
            AccountExecutionPort executions,
            AccountIdempotencyService idempotencyService,
            AccountValidator validator,
            AccountSnapshotAssembler snapshotAssembler
    ) {
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        this.accessService = accessService;
        this.executions = executions;
        this.idempotencyService = idempotencyService;
        this.validator = validator;
        this.snapshotAssembler = snapshotAssembler;
    }

    public void applyDue(Context context) {
        transactionExecutor.independent(() -> applyLocked(context));
    }

    public void applyPending() {
        for (AccountState account : mapper.dueZoneAccounts()) {
            Context context = new Context(
                    null, "USER:" + account.id(), account.id(), UUID.randomUUID().toString(),
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

    private Boolean applyLocked(Context context) {
        AccountState account = accessService.locked(context);
        OffsetDateTime boundary = account.pendingZoneEffectiveAt();
        if (boundary == null || mapper.now().isBefore(boundary)) {
            return Boolean.FALSE;
        }
        // 执行模块尚未实现可信暂停适配时不能自行改写执行或投入历史。
        if (executions.unfinishedCount(account.id()) > 0) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        ZonePeriod previous = mapper.findZone(account.currentZonePeriodId(), account.id());
        ZonePeriod next = ZonePeriod.create(account.id(), previous.revision() + 1,
                account.pendingZoneId(), boundary, "DEVICE_CHANGE");
        mapper.closeZone(previous.id(), boundary);
        mapper.insertZone(next);
        mapper.applyZone(account.id(), next.id());
        return Boolean.TRUE;
    }

    public DeviceTimeZoneResult synchronize(Context context, ZoneInput input) {
        validator.zone(input.detectedZoneId());
        long expectedVersion = validator.version(input.expectedVersion());
        applyDue(context);
        AccountIdempotencyService.Execution<DeviceTimeZoneResult> result = idempotencyService.execute(
                context, "API-AUTH-010", input, DeviceTimeZoneResult.class,
                () -> AccountIdempotencyService.success(updateLocked(context, input.detectedZoneId(), expectedVersion))
        );
        return result.data();
    }

    private DeviceTimeZoneResult updateLocked(Context context, String target, long expectedVersion) {
        AccountState account = accessService.locked(context);
        accessService.requireVersion(account, expectedVersion);
        ZonePeriod period = mapper.findZone(account.currentZonePeriodId(), account.id());
        boolean changed = false;
        if (target.equals(period.zoneId())) {
            if (account.pendingZoneId() != null) {
                mapper.updatePendingZone(account.id(), null, null);
                changed = true;
            }
        } else if (!target.equals(account.pendingZoneId())) {
            OffsetDateTime boundary = account.pendingZoneEffectiveAt();
            if (boundary == null) {
                ZoneId zone = ZoneId.of(period.zoneId());
                boundary = mapper.now().toInstant().atZone(zone).toLocalDate().plusDays(1)
                        .atStartOfDay(zone).toOffsetDateTime();
            }
            mapper.updatePendingZone(account.id(), target, boundary);
            changed = true;
        }
        AccountState current = mapper.findState(account.id());
        ZonePeriod currentPeriod = mapper.findZone(current.currentZonePeriodId(), current.id());
        SelfAccount snapshot = snapshotAssembler.assemble(current, currentPeriod);
        DeviceTimeZoneResult result = new DeviceTimeZoneResult(
                snapshot, changed, mapper.now().toInstant()
        );
        return result;
    }
}
