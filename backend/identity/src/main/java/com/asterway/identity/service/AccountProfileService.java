package com.asterway.identity.service;

import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.AccountExecutionPort;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountProfileApi;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSessionSnapshot;
import com.asterway.identity.api.model.AccountSnapshot;
import com.asterway.identity.api.model.ProfileInput;
import com.asterway.identity.api.model.ProfileResult;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.mapper.AccountTimeZoneMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.utils.AccountSnapshotUtils;
import com.asterway.identity.utils.AccountValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

// 只负责本人资料读取及用户名、昵称修改，改名保留已有 UUID 会话。
@Service
public class AccountProfileService implements AccountProfileApi {

    private final AccountAuditPort audit;

    private final AccountTimeZoneMapper timeZoneMapper;

    private final AccountMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final AccountOperationPort idempotencyService;
    private final AccountAccessService accessService;
    private final AccountTimeZoneService timeZones;
    private final AccountSecurityPort ledger;
    private final AccountExecutionPort executions;

    public AccountProfileService(
            AccountAuditPort audit,
            AccountTimeZoneMapper timeZoneMapper,
            AccountMapper mapper,
            PlatformTransactionManager transactionManager,
            AccountOperationPort idempotencyService,
            AccountAccessService accessService,
            AccountTimeZoneService timeZones,
            AccountSecurityPort ledger,
            AccountExecutionPort executions
    ) {
        this.audit = audit;
        this.timeZoneMapper = timeZoneMapper;
        this.mapper = mapper;
        this.transactionManager = transactionManager;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.timeZones = timeZones;
        this.ledger = ledger;
        this.executions = executions;
    }

    @Override
    public AccountSessionSnapshot current(AccountContext context) {
        timeZones.applyDue(context);
        AccountSessionSnapshot snapshot = TransactionUtils.required(transactionManager, () -> snapshotLocked(context));
        return snapshot;
    }

    private AccountSessionSnapshot snapshotLocked(AccountContext context) {
        AccountState state = accessService.locked(context);
        ZonePeriod period = timeZoneMapper.findZone(state.currentZonePeriodId(), state.id());
        AccountSnapshot account = AccountSnapshotUtils.assemble(state, period);
        OffsetDateTime now = mapper.now();
        LocalDate date = now.toInstant().atZone(ZoneId.of(account.timeZone().activeZoneId())).toLocalDate();
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        AccountSessionSnapshot snapshot = new AccountSessionSnapshot(
                account, now.toInstant(), date, monday, monday.plusDays(6),
                executions.unfinishedCount(state.id()) > 0
        );
        return snapshot;
    }

    @Override
    public ProfileResult updateProfile(AccountContext context, ProfileInput input) {
        long expectedVersion = AccountValidationUtils.version(input.expectedVersion());
        if (input.username() == null && input.nickname() == null) {
            AccountValidationUtils.invalid("username", "至少提交用户名或昵称中的一项");
        }
        if (input.username() != null) {
            AccountValidationUtils.username(input.username());
        }
        if (input.nickname() != null) {
            AccountValidationUtils.nickname(input.nickname());
        }
        timeZones.applyDue(context);
        AccountOperationPort.Execution<ProfileResult> execution = idempotencyService.execute(
                context, AccountOperationCode.UPDATE_PROFILE.code(), input, ProfileResult.class,
                () -> AccountOperationPort.success(updateProfileLocked(context, input, expectedVersion))
        );
        return execution.data();
    }

    private ProfileResult updateProfileLocked(AccountContext context, ProfileInput input, long expectedVersion) {
        AccountState before = accessService.locked(context);
        accessService.requireVersion(before, expectedVersion);
        String username = input.username() == null ? before.username() : input.username();
        String nickname = input.nickname() == null ? before.nickname() : input.nickname();
        boolean renamed = !username.equals(before.username());
        boolean changed = renamed || !nickname.equals(before.nickname());
        if (changed) {
            AccountState after = before.withProfile(username, nickname);
            if (renamed) {
                ledger.prepare(context, "LOGIN_NAME", before.securityState(), after.securityState());
            }
            mapper.updateProfile(before.id(), username, nickname, renamed);
            audit.record(context, before.id(), "ACCOUNT_PROFILE_UPDATE", "SUCCESS");
        }
        AccountState current = mapper.findState(before.id());
        ZonePeriod period = timeZoneMapper.findZone(current.currentZonePeriodId(), current.id());
        AccountSnapshot account = AccountSnapshotUtils.assemble(current, period);
        ProfileResult result = new ProfileResult(account, changed);
        return result;
    }
}
