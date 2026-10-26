package com.asterway.identity.service;

import com.asterway.identity.api.AccountExecutionPort;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.CsrfInfo;
import com.asterway.identity.api.model.AccountModels.ProfileInput;
import com.asterway.identity.api.model.AccountModels.ProfileResult;
import com.asterway.identity.api.model.AccountModels.SelfAccount;
import com.asterway.identity.api.model.AccountModels.SelfSession;
import com.asterway.identity.assembler.AccountSnapshotAssembler;
import com.asterway.identity.infrastructure.security.AccountSecurityLedger;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

// 只负责本人资料读取及用户名、昵称修改，改名保留已有 UUID 会话。
@Service
public class AccountProfileService {

    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final AccountIdempotencyService idempotencyService;
    private final AccountAccessService accessService;
    private final AccountTimeZoneService timeZones;
    private final AccountSecurityLedger ledger;
    private final AccountExecutionPort executions;
    private final AccountValidator validator;
    private final AccountSnapshotAssembler snapshotAssembler;

    public AccountProfileService(
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor,
            AccountIdempotencyService idempotencyService,
            AccountAccessService accessService,
            AccountTimeZoneService timeZones,
            AccountSecurityLedger ledger,
            AccountExecutionPort executions,
            AccountValidator validator,
            AccountSnapshotAssembler snapshotAssembler
    ) {
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.timeZones = timeZones;
        this.ledger = ledger;
        this.executions = executions;
        this.validator = validator;
        this.snapshotAssembler = snapshotAssembler;
    }

    public SelfSession current(Context context, CsrfInfo csrf) {
        timeZones.applyDue(context);
        SelfSession snapshot = transactionExecutor.required(() -> snapshotLocked(context, csrf));
        return snapshot;
    }

    private SelfSession snapshotLocked(Context context, CsrfInfo csrf) {
        AccountState state = accessService.locked(context);
        ZonePeriod period = mapper.findZone(state.currentZonePeriodId(), state.id());
        SelfAccount account = snapshotAssembler.assemble(state, period);
        OffsetDateTime now = mapper.now();
        LocalDate date = now.toInstant().atZone(ZoneId.of(account.timeZone().activeZoneId())).toLocalDate();
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        SelfSession snapshot = new SelfSession(
                account, csrf, now.toInstant(), date, monday, monday.plusDays(6),
                executions.unfinishedCount(state.id()) > 0
        );
        return snapshot;
    }

    public ProfileResult updateProfile(Context context, ProfileInput input) {
        long expectedVersion = validator.version(input.expectedVersion());
        if (input.username() == null && input.nickname() == null) {
            validator.invalid("username", "至少提交用户名或昵称中的一项");
        }
        if (input.username() != null) {
            validator.username(input.username());
        }
        if (input.nickname() != null) {
            validator.nickname(input.nickname());
        }
        timeZones.applyDue(context);
        AccountIdempotencyService.Execution<ProfileResult> execution = idempotencyService.execute(
                context, "API-AUTH-007", input, ProfileResult.class,
                () -> AccountIdempotencyService.success(updateProfileLocked(context, input, expectedVersion))
        );
        return execution.data();
    }

    private ProfileResult updateProfileLocked(Context context, ProfileInput input, long expectedVersion) {
        AccountState before = accessService.locked(context);
        accessService.requireVersion(before, expectedVersion);
        String username = input.username() == null ? before.username() : input.username();
        String nickname = input.nickname() == null ? before.nickname() : input.nickname();
        boolean renamed = !username.equals(before.username());
        boolean changed = renamed || !nickname.equals(before.nickname());
        if (changed) {
            AccountState after = before.withProfile(username, nickname);
            if (renamed) {
                ledger.prepare(context, "LOGIN_NAME", before, after);
            }
            mapper.updateProfile(before.id(), username, nickname, renamed);
            mapper.audit(context, before.id(), "ACCOUNT_PROFILE_UPDATE", "SUCCESS");
        }
        AccountState current = mapper.findState(before.id());
        ZonePeriod period = mapper.findZone(current.currentZonePeriodId(), current.id());
        SelfAccount account = snapshotAssembler.assemble(current, period);
        ProfileResult result = new ProfileResult(account, changed);
        return result;
    }
}
