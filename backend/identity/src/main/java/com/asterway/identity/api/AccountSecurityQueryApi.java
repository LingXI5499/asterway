package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountSecurityState;

import java.util.List;
import java.util.UUID;

// 仅向应用安全核验开放必要事实，不返回完整账户存储对象。
public interface AccountSecurityQueryApi {

    AccountSecurityState find(UUID userId);

    List<AccountSecurityState> list();
}
