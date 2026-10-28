package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.CaptchaProof;

import java.time.Instant;
import java.util.UUID;

public record CaptchaProofVO(
        UUID challengeId,
        String verificationToken,
        Instant expiresAt
) {

    public static CaptchaProofVO from(CaptchaProof value) {
        CaptchaProofVO converted = new CaptchaProofVO(value.challengeId(), value.verificationToken(), value.expiresAt());
        return converted;
    }

    @Override
    public String toString() {
        return "CaptchaProofVO[protected]";
    }
}
