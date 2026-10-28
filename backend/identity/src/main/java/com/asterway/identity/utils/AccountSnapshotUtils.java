package com.asterway.identity.utils;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.model.AccountSnapshot;
import com.asterway.identity.api.model.TimeZoneState;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;

import java.time.OffsetDateTime;

// 将已读取的账户和时区组装为公开快照，不查询数据库或判断会话资格。
public final class AccountSnapshotUtils {

    private AccountSnapshotUtils() {
    }

    public static AccountSnapshot assemble(AccountState account, ZonePeriod period) {
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
        AccountSnapshot snapshot = new AccountSnapshot(
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
