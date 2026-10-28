package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.RegistrationInput;
import com.asterway.identity.api.model.RegistrationResult;

// 普通账户注册能力，不负责建立框架会话。
public interface AccountRegistrationApi {

    RegistrationResult register(AccountContext context, RegistrationInput input);
}
