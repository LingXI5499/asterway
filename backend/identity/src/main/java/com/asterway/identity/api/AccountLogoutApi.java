package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.LogoutResult;

// 当前会话退出的业务检查能力。
public interface AccountLogoutApi {

    LogoutResult logout(AccountContext context, Boolean confirmed, String sessionId);
}
