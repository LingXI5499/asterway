package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.LogoutResult;

public record LogoutVO(
        boolean loggedOut
) {

    public static LogoutVO from(LogoutResult value) {
        LogoutVO converted = new LogoutVO(value.loggedOut());
        return converted;
    }
}
