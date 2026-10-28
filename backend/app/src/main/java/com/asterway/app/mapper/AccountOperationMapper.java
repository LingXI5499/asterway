package com.asterway.app.mapper;

import com.asterway.app.model.AccountOperationRecord;
import com.asterway.identity.api.model.AccountContext;
import org.apache.ibatis.annotations.Param;

// 账户命令幂等结果的数据访问契约。
public interface AccountOperationMapper {

    void lockScope(@Param("scope") String scope);

    AccountOperationRecord findOperation(@Param("context") AccountContext context, @Param("operation") String operation);

    void insertOperation(
            @Param("context") AccountContext context,
            @Param("operation") String operation,
            @Param("fingerprint") byte[] fingerprint
    );

    void finishOperation(
            @Param("context") AccountContext context,
            @Param("operation") String operation,
            @Param("code") String code,
            @Param("status") int status,
            @Param("summary") String summary
    );

    boolean logoutKeyUsedElsewhere(@Param("context") AccountContext context);

    void cleanExpiredOperations();
}
