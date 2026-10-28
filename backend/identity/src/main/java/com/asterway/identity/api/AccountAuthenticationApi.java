package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.LoginGrant;
import com.asterway.identity.api.model.LoginInput;

import java.util.UUID;

// 账户认证与会话资格能力。
public interface AccountAuthenticationApi {

    LoginGrant login(AccountContext context, LoginInput input);

    LoginGrant validateSession(UUID userId, long credentialVersion, long securityVersion);
}
