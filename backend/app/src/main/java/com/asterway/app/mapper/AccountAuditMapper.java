package com.asterway.app.mapper;

import com.asterway.identity.api.model.AccountContext;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

// 账户审计事实的数据访问契约。
public interface AccountAuditMapper {

    void audit(
            @Param("context") AccountContext context,
            @Param("targetId") UUID targetId,
            @Param("action") String action,
            @Param("outcome") String outcome
    );
}
