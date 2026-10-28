package com.asterway.identity.service;

import com.asterway.common.api.CryptographyApi;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountCaptchaApi;
import com.asterway.identity.api.AccountConfigurationPort;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.CaptchaChallenge;
import com.asterway.identity.api.model.CaptchaProof;
import com.asterway.identity.constant.CaptchaConstants;
import com.asterway.identity.mapper.AccountCaptchaMapper;
import com.asterway.identity.mapper.AccountLoginAttemptMapper;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.CaptchaState;
import com.asterway.identity.utils.CaptchaImageUtils;
import com.asterway.identity.utils.CaptchaImageUtils.Material;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 挑战验证和消费使用独立短事务，随后注册拒绝不会恢复已消费的凭据。
@Service
public class AccountCaptchaService implements AccountCaptchaApi {

    private final AccountOperationPort operations;

    private final AccountLoginAttemptMapper loginAttemptMapper;

    private final AccountCaptchaMapper captchaMapper;

    private final AccountConfigurationPort configuration;

    private final AccountMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final CryptographyApi cryptography;

    public AccountCaptchaService(
            AccountOperationPort operations,
            AccountLoginAttemptMapper loginAttemptMapper,
            AccountCaptchaMapper captchaMapper,
            AccountConfigurationPort configuration,
            AccountMapper mapper,
            PlatformTransactionManager transactionManager,
            CryptographyApi cryptography
    ) {
        this.operations = operations;
        this.loginAttemptMapper = loginAttemptMapper;
        this.captchaMapper = captchaMapper;
        this.configuration = configuration;
        this.mapper = mapper;
        this.transactionManager = transactionManager;
        this.cryptography = cryptography;
    }

    @Override
    public CaptchaChallenge create(AccountContext context, String purpose) {
        if (context.userId() != null || !(CaptchaConstants.REGISTER_PURPOSE.equals(purpose) || CaptchaConstants.LOGIN_PURPOSE.equals(purpose))) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        CaptchaChallenge challenge = TransactionUtils.independent(transactionManager, () -> createLocked(context, purpose));
        return challenge;
    }

    private CaptchaChallenge createLocked(AccountContext context, String purpose) {
        mapper.lockScope("captcha:" + context.scope());
        byte[] binding = binding(context);
        CaptchaState existing = captchaMapper.findCreatedCaptcha(binding, context.key());
        if (existing != null) {
            if (!existing.purpose().equals(purpose)) {
                throw new BusinessException(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            requireValid(existing);
            CaptchaChallenge challenge = material(existing).challenge();
            return challenge;
        }
        if (CaptchaConstants.REGISTER_PURPOSE.equals(purpose) && !configuration.registrationOpen()) {
            throw new BusinessException(IdentityErrorCode.REGISTRATION_CLOSED);
        }
        if (captchaMapper.countRecentCaptchas(binding) >= CaptchaConstants.CHALLENGES_PER_MINUTE) {
            throw new AccountException(IdentityErrorCode.RATE_LIMITED, Map.of("retryAfterSeconds", Integer.toString(CaptchaConstants.RETRY_SECONDS)));
        }
        OffsetDateTime now = mapper.now();
        UUID id = UUID.randomUUID();
        Material material = CaptchaImageUtils.create(id, purpose, now.plusSeconds(CaptchaConstants.LIFETIME_SECONDS).toInstant());
        CaptchaState state = new CaptchaState(
                id, purpose, binding, cryptography.encrypt(material), now, now.plusSeconds(CaptchaConstants.LIFETIME_SECONDS), null,
                context.key(), null, null, null, null, CaptchaConstants.ISSUED
        );
        captchaMapper.insertCaptcha(state);
        captchaMapper.cleanExpiredCaptchas();
        loginAttemptMapper.cleanExpiredAttempts();
        operations.clearExpired();
        return material.challenge();
    }

    @Override
    public CaptchaProof verify(AccountContext context, UUID id, double offsetX) {
        if (!Double.isFinite(offsetX) || offsetX < 0 || offsetX > CaptchaConstants.MAX_OFFSET || context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
        }
        Verification result = TransactionUtils.independent(transactionManager, () -> verifyLocked(context, id, offsetX));
        if (result.failure() != null) {
            throw new BusinessException(result.failure());
        }
        return result.proof();
    }

    private Verification verifyLocked(AccountContext context, UUID id, double offsetX) {
        CaptchaState state = captchaMapper.findCaptcha(id);
        requireBinding(state, context);
        requireValid(state);
        byte[] fingerprint = cryptography.digest("captcha-offset", offsetX);
        if (state.verifyKey() != null && state.verifyKey().equals(context.key())) {
            if (!MessageDigest.isEqual(state.verifyFingerprint(), fingerprint)) {
                throw new BusinessException(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            if (CaptchaConstants.FAILED.equals(state.state())) {
                return new Verification(null, IdentityErrorCode.CAPTCHA_INVALID);
            }
            return new Verification(proof(state), null);
        }
        if (!CaptchaConstants.ISSUED.equals(state.state())) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        Material material = material(state);
        boolean early = mapper.now().isBefore(state.createdAt().plusNanos(CaptchaConstants.MINIMUM_ELAPSED_MILLIS * 1_000_000L));
        boolean correct = !early && Math.abs(material.answerX() - offsetX) <= CaptchaConstants.OFFSET_TOLERANCE;
        String nextState = correct ? CaptchaConstants.VERIFIED : CaptchaConstants.FAILED;
        captchaMapper.verifyCaptcha(id, context.key(), fingerprint, nextState, state.verificationData());
        Verification result = correct
                ? new Verification(proof(state), null)
                : new Verification(null, IdentityErrorCode.CAPTCHA_INVALID);
        return result;
    }

    public void consume(AccountContext context, String purpose, UUID id, String token, Object authenticationInput) {
        TransactionUtils.independent(transactionManager, () -> {
            CaptchaState state = captchaMapper.findCaptcha(id);
            requireBinding(state, context);
            requireValid(state);
            if (!purpose.equals(state.purpose()) || !Objects.equals(material(state).verificationToken(), token)) {
                throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
            }
            byte[] fingerprint = cryptography.digest("captcha-consumption", authenticationInput);
            if (CaptchaConstants.CONSUMED.equals(state.state())) {
                if (context.key().equals(state.consumedKey())
                        && MessageDigest.isEqual(fingerprint, state.consumedFingerprint())) {
                    return Boolean.TRUE;
                }
                throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
            }
            if (!CaptchaConstants.VERIFIED.equals(state.state())) {
                throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
            }
            captchaMapper.consumeCaptcha(id, context.key(), fingerprint);
            return Boolean.TRUE;
        });
    }

    private CaptchaProof proof(CaptchaState state) {
        CaptchaProof proof = new CaptchaProof(state.id(), material(state).verificationToken(), state.expiresAt().toInstant());
        return proof;
    }

    private Material material(CaptchaState state) {
        Material material = cryptography.decrypt(state.verificationData(), Material.class);
        return material;
    }

    private byte[] binding(AccountContext context) {
        byte[] digest = cryptography.digest("captcha-binding", context.scope());
        return digest;
    }

    private void requireBinding(CaptchaState state, AccountContext context) {
        if (state == null || !MessageDigest.isEqual(state.bindingDigest(), binding(context))) {
            throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
        }
    }

    private void requireValid(CaptchaState state) {
        if (!mapper.now().isBefore(state.expiresAt())) {
            throw new BusinessException(IdentityErrorCode.CAPTCHA_EXPIRED);
        }
    }

    private record Verification(CaptchaProof proof, IdentityErrorCode failure) {
    }
}
