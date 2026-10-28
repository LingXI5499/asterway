package com.asterway.app.web.identity.dto;

import com.asterway.identity.api.model.LoginInput;

import java.util.UUID;

public record LoginDTO(
        String username,
        String password,
        UUID captchaChallengeId,
        String captchaVerificationToken
) {

    public LoginInput toInput() {
        LoginInput converted = new LoginInput(username, password, captchaChallengeId, captchaVerificationToken);
        return converted;
    }

    @Override
    public String toString() {
        return "LoginDTO[protected]";
    }
}
