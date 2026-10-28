package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSessionSnapshot;
import com.asterway.identity.api.model.ProfileInput;
import com.asterway.identity.api.model.ProfileResult;

// 本人资料及业务日快照能力，不接收 HTTP 安全令牌。
public interface AccountProfileApi {

    AccountSessionSnapshot current(AccountContext context);

    ProfileResult updateProfile(AccountContext context, ProfileInput input);
}
