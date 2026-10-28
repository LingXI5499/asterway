package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSecurityState;

import java.util.UUID;

// 安全账本核验及账户护栏能力，不暴露底层文件和数据库实现。
public interface AccountSecurityPort {

    void initialize();

    void verify(AccountSecurityState state);

    void prepare(AccountContext context, String type, AccountSecurityState before, AccountSecurityState after);

    UUID lockAccount(UUID userId);

    String recoveryState(UUID userId);

    void insertGuard(UUID userId);
}
