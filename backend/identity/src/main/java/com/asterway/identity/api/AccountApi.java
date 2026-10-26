package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.PublicBootstrap;
import com.asterway.identity.api.model.AccountModels.TimeZonePage;
import com.asterway.identity.api.model.AccountModels.CaptchaChallenge;
import com.asterway.identity.api.model.AccountModels.CaptchaProof;
import com.asterway.identity.api.model.AccountModels.RegistrationInput;
import com.asterway.identity.api.model.AccountModels.LoginInput;
import com.asterway.identity.api.model.AccountModels.LoginGrant;
import com.asterway.identity.api.model.AccountModels.SelfSession;
import com.asterway.identity.api.model.AccountModels.CsrfInfo;
import com.asterway.identity.api.model.AccountModels.ProfileInput;
import com.asterway.identity.api.model.AccountModels.ProfileResult;
import com.asterway.identity.api.model.AccountModels.PasswordInput;
import com.asterway.identity.api.model.AccountModels.PasswordChangeResult;
import com.asterway.identity.api.model.AccountModels.ZoneInput;
import com.asterway.identity.api.model.AccountModels.DeviceTimeZoneResult;
import com.asterway.identity.api.model.AccountModels.LogoutResult;
import com.asterway.identity.api.model.AccountModels.OperationSnapshot;
import com.asterway.identity.api.model.RegistrationResult;

import java.util.UUID;

// 账户公开能力；身份与作用域由服务端构造，不能从请求体指定。
public interface AccountApi {

    // 应用启动及调度入口，不作为对外 HTTP 接口开放。
    void initializeSecurity();

    void applyPendingTimeZones();

    PublicBootstrap bootstrap();

    TimeZonePage timeZones(String query, int limit, String cursor);

    CaptchaChallenge createCaptcha(Context context, String purpose);

    CaptchaProof verifyCaptcha(Context context, UUID challengeId, double offsetX);

    RegistrationResult register(Context context, RegistrationInput input);

    LoginGrant login(Context context, LoginInput input);

    LoginGrant validateSession(UUID userId, long credentialVersion, long securityVersion);

    SelfSession current(Context context, CsrfInfo csrf);

    ProfileResult updateProfile(Context context, ProfileInput input);

    PasswordChangeResult changePassword(Context context, PasswordInput input);

    DeviceTimeZoneResult synchronizeZone(Context context, ZoneInput input);

    LogoutResult logout(Context context, Boolean confirmed, String sessionId);

    OperationSnapshot operation(Context context, String operation);
}
