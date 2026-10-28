package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.CaptchaChallenge;
import com.asterway.identity.api.model.CaptchaProof;

import java.util.UUID;

// 账户拼图挑战的创建与验证能力。
public interface AccountCaptchaApi {

    CaptchaChallenge create(AccountContext context, String purpose);

    CaptchaProof verify(AccountContext context, UUID challengeId, double offsetX);
}
