package com.asterway.identity.service;

import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.model.AccountModels.CaptchaChallenge;
import com.asterway.identity.api.model.AccountModels.CaptchaProof;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.CsrfInfo;
import com.asterway.identity.api.model.AccountModels.DeviceTimeZoneResult;
import com.asterway.identity.api.model.AccountModels.LoginGrant;
import com.asterway.identity.api.model.AccountModels.LoginInput;
import com.asterway.identity.api.model.AccountModels.LogoutResult;
import com.asterway.identity.api.model.AccountModels.OperationSnapshot;
import com.asterway.identity.api.model.AccountModels.PasswordChangeResult;
import com.asterway.identity.api.model.AccountModels.PasswordInput;
import com.asterway.identity.api.model.AccountModels.ProfileInput;
import com.asterway.identity.api.model.AccountModels.ProfileResult;
import com.asterway.identity.api.model.AccountModels.PublicBootstrap;
import com.asterway.identity.api.model.AccountModels.RegistrationInput;
import com.asterway.identity.api.model.AccountModels.SelfSession;
import com.asterway.identity.api.model.AccountModels.TimeZonePage;
import com.asterway.identity.api.model.AccountModels.ZoneInput;
import com.asterway.identity.api.model.RegistrationResult;
import com.asterway.identity.infrastructure.security.AccountSecurityLedger;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 实现模块公开能力并分派给内部服务；框架会话由 app 的应用用例协调。
@Service
public class AccountService implements AccountApi {

    private final AccountPublicQueryService publicQueryService;

    private final AccountCaptchaService captchaService;

    private final AccountRegistrationService registrationService;

    private final AccountAuthenticationService authenticationService;

    private final AccountProfileService profileService;

    private final AccountPasswordService passwordService;

    private final AccountLogoutService logoutService;

    private final AccountTimeZoneService timeZoneService;

    private final AccountIdempotencyService idempotencyService;

    private final AccountSecurityLedger ledger;

    public AccountService(
            AccountPublicQueryService publicQueryService,
            AccountCaptchaService captchaService,
            AccountRegistrationService registrationService,
            AccountAuthenticationService authenticationService,
            AccountProfileService profileService,
            AccountPasswordService passwordService,
            AccountLogoutService logoutService,
            AccountTimeZoneService timeZoneService,
            AccountIdempotencyService idempotencyService,
            AccountSecurityLedger ledger
    ) {
        this.publicQueryService = publicQueryService;
        this.captchaService = captchaService;
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.profileService = profileService;
        this.passwordService = passwordService;
        this.logoutService = logoutService;
        this.timeZoneService = timeZoneService;
        this.idempotencyService = idempotencyService;
        this.ledger = ledger;
    }

    @Override
    public void initializeSecurity() {
        ledger.initialize();
    }

    @Override
    public void applyPendingTimeZones() {
        timeZoneService.applyPending();
    }

    @Override
    public PublicBootstrap bootstrap() {
        PublicBootstrap result = publicQueryService.bootstrap();
        return result;
    }

    @Override
    public TimeZonePage timeZones(String query, int limit, String cursor) {
        TimeZonePage result = publicQueryService.timeZones(query, limit, cursor);
        return result;
    }

    @Override
    public CaptchaChallenge createCaptcha(Context context, String purpose) {
        CaptchaChallenge result = captchaService.create(context, purpose);
        return result;
    }

    @Override
    public CaptchaProof verifyCaptcha(Context context, UUID challengeId, double offsetX) {
        CaptchaProof result = captchaService.verify(context, challengeId, offsetX);
        return result;
    }

    @Override
    public RegistrationResult register(Context context, RegistrationInput input) {
        RegistrationResult result = registrationService.register(context, input);
        return result;
    }

    @Override
    public LoginGrant login(Context context, LoginInput input) {
        LoginGrant result = authenticationService.login(context, input);
        return result;
    }

    @Override
    public LoginGrant validateSession(UUID userId, long credentialVersion, long securityVersion) {
        LoginGrant result = authenticationService.validateSession(userId, credentialVersion, securityVersion);
        return result;
    }

    @Override
    public SelfSession current(Context context, CsrfInfo csrf) {
        SelfSession result = profileService.current(context, csrf);
        return result;
    }

    @Override
    public ProfileResult updateProfile(Context context, ProfileInput input) {
        ProfileResult result = profileService.updateProfile(context, input);
        return result;
    }

    @Override
    public PasswordChangeResult changePassword(Context context, PasswordInput input) {
        PasswordChangeResult result = passwordService.changePassword(context, input);
        return result;
    }

    @Override
    public DeviceTimeZoneResult synchronizeZone(Context context, ZoneInput input) {
        DeviceTimeZoneResult result = timeZoneService.synchronize(context, input);
        return result;
    }

    @Override
    public LogoutResult logout(Context context, Boolean confirmed, String sessionId) {
        LogoutResult result = logoutService.logout(context, confirmed, sessionId);
        return result;
    }

    @Override
    public OperationSnapshot operation(Context context, String operation) {
        OperationSnapshot result = idempotencyService.query(context, operation);
        return result;
    }
}
