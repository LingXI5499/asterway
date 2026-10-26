package com.asterway.identity.assembler;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.model.AccountModels.SelfAccount;
import com.asterway.identity.api.model.AccountModels.TimeZoneState;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

// 将已读取的账户和时区组装为公开快照，不查询数据库或判断会话资格。
@Component
public class AccountSnapshotAssembler {

    public SelfAccount assemble(AccountState account, ZonePeriod period) {
        if (period == null) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        OffsetDateTime pending = account.pendingZoneEffectiveAt();
        TimeZoneState zone = new TimeZoneState(
                period.zoneId(),
                period.id(),
                Long.toString(period.revision()),
                account.pendingZoneId(),
                pending == null ? null : pending.toInstant()
        );
        SelfAccount snapshot = new SelfAccount(
                account.id(),
                account.username(),
                account.nickname(),
                account.role(),
                Long.toString(account.version()),
                account.createdAt().toInstant(),
                account.updatedAt().toInstant(),
                zone
        );
        return snapshot;
    }
}
