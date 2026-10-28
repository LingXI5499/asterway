package com.asterway.identity.mapper;

import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// 账户时区区间和待生效变更的数据访问契约。
public interface AccountTimeZoneMapper {

    List<AccountState> dueZoneAccounts();

    ZonePeriod findZone(@Param("id") UUID id, @Param("userId") UUID userId);

    int insertZone(ZonePeriod period);

    int closeZone(@Param("id") UUID id, @Param("at") OffsetDateTime at);

    int applyZone(@Param("userId") UUID userId, @Param("periodId") UUID periodId);

    int updatePendingZone(
            @Param("userId") UUID userId,
            @Param("zoneId") String zoneId,
            @Param("effectiveAt") OffsetDateTime effectiveAt
    );
}
