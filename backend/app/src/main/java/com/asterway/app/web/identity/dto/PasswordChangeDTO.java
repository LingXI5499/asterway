package com.asterway.app.web.identity.dto;

import com.asterway.identity.api.model.PasswordInput;

public record PasswordChangeDTO(
        String expectedVersion,
        String currentPassword,
        String newPassword,
        String confirmPassword,
        Boolean confirmed
) {

    public PasswordInput toInput() {
        PasswordInput converted = new PasswordInput(expectedVersion, currentPassword, newPassword, confirmPassword, confirmed);
        return converted;
    }

    @Override
    public String toString() {
        return "PasswordChangeDTO[protected]";
    }
}
