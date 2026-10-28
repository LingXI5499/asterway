package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.ProfileResult;

public record ProfileUpdateVO(
        AccountVO account,
        boolean changed
) {

    public static ProfileUpdateVO from(ProfileResult value) {
        ProfileUpdateVO converted = new ProfileUpdateVO(AccountVO.from(value.account()), value.changed());
        return converted;
    }
}
