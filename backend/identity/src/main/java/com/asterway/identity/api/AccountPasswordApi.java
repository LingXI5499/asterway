package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.PasswordChangeResult;
import com.asterway.identity.api.model.PasswordInput;

// 密码校验、变更及旧会话撤销能力。
public interface AccountPasswordApi {

    PasswordChangeResult changePassword(AccountContext context, PasswordInput input);
}
