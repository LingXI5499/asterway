package com.asterway.app.mapper;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSecurityState;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

// 账户护栏和安全变更记录的数据访问契约。
public interface AccountSecurityMapper {

    UUID lockAccount(@Param("userId") UUID userId);

    String recoveryState(@Param("userId") UUID userId);

    void isolateAccount(@Param("userId") UUID userId);

    void insertGuard(@Param("userId") UUID userId);

    void recordSecurityChange(
            @Param("operationId") UUID operationId,
            @Param("context") AccountContext context,
            @Param("type") String type,
            @Param("before") AccountSecurityState before,
            @Param("after") AccountSecurityState after
    );

    boolean securityChangeCommitted(@Param("operationId") UUID operationId);

    boolean securityHistoryExists();

    void markLedgerSynced(@Param("operationId") UUID operationId);
}
