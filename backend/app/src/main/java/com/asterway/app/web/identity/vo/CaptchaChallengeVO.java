package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.CaptchaChallenge;

import java.time.Instant;
import java.util.UUID;

public record CaptchaChallengeVO(
        UUID challengeId,
        String purpose,
        String backgroundImage,
        String pieceImage,
        int imageWidth,
        int imageHeight,
        int pieceWidth,
        int pieceHeight,
        int pieceTop,
        Instant expiresAt
) {

    public static CaptchaChallengeVO from(CaptchaChallenge value) {
        CaptchaChallengeVO converted = new CaptchaChallengeVO(
                value.challengeId(),
                value.purpose(),
                value.backgroundImage(),
                value.pieceImage(),
                value.imageWidth(),
                value.imageHeight(),
                value.pieceWidth(),
                value.pieceHeight(),
                value.pieceTop(),
                value.expiresAt()
        );
        return converted;
    }
}
