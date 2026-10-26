package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.CaptchaChallenge;
import com.asterway.identity.api.model.AccountModels.CaptchaProof;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.captcha.CaptchaImageFactory;
import com.asterway.identity.captcha.CaptchaImageFactory.Material;
import com.asterway.identity.infrastructure.security.AccountCryptography;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.CaptchaState;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 挑战验证和消费使用独立短事务，随后注册拒绝不会恢复已消费的凭据。
@Service
public class AccountCaptchaService {

    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final AccountCryptography cryptography;
    private final CaptchaImageFactory images;

    public AccountCaptchaService(
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor,
            AccountCryptography cryptography,
            CaptchaImageFactory images
    ) {
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        this.cryptography = cryptography;
        this.images = images;
    }

    public CaptchaChallenge create(Context context, String purpose) {
        if (context.userId() != null || !("REGISTER".equals(purpose) || "LOGIN".equals(purpose))) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        CaptchaChallenge challenge = transactionExecutor.independent(() -> createLocked(context, purpose));
        return challenge;
    }

    private CaptchaChallenge createLocked(Context context, String purpose) {
        mapper.lockScope("captcha:" + context.scope());
        byte[] binding = binding(context);
        CaptchaState existing = mapper.findCreatedCaptcha(binding, context.key());
        if (existing != null) {
            if (!existing.purpose().equals(purpose)) {
                throw new BusinessException(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            requireValid(existing);
            CaptchaChallenge challenge = material(existing).challenge();
            return challenge;
        }
        if ("REGISTER".equals(purpose) && !mapper.registrationOpen()) {
            throw new BusinessException(IdentityErrorCode.REGISTRATION_CLOSED);
        }
        if (mapper.countRecentCaptchas(binding) >= 20) {
            throw new AccountException(IdentityErrorCode.RATE_LIMITED, Map.of("retryAfterSeconds", "60"));
        }
        OffsetDateTime now = mapper.now();
        UUID id = UUID.randomUUID();
        Material material = images.create(id, purpose, now.plusSeconds(120).toInstant());
        CaptchaState state = new CaptchaState(
                id, purpose, binding, cryptography.encrypt(material), now, now.plusSeconds(120), null,
                context.key(), null, null, null, null, "ISSUED"
        );
        mapper.insertCaptcha(state);
        mapper.cleanShortLivedData();
        return material.challenge();
    }

    public CaptchaProof verify(Context context, UUID id, double offsetX) {
        if (!Double.isFinite(offsetX) || offsetX < 0 || offsetX > 280 || context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
        }
        Verification result = transactionExecutor.independent(() -> verifyLocked(context, id, offsetX));
        if (result.failure() != null) {
            throw new BusinessException(result.failure());
        }
        return result.proof();
    }

    private Verification verifyLocked(Context context, UUID id, double offsetX) {
        CaptchaState state = mapper.findCaptcha(id);
        requireBinding(state, context);
        requireValid(state);
        byte[] fingerprint = cryptography.digest("captcha-offset", offsetX);
        if (state.verifyKey() != null && state.verifyKey().equals(context.key())) {
            if (!MessageDigest.isEqual(state.verifyFingerprint(), fingerprint)) {
                throw new BusinessException(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED);
            }
            if ("FAILED".equals(state.state())) {
                return new Verification(null, IdentityErrorCode.CAPTCHA_INVALID);
            }
            return new Verification(proof(state), null);
        }
        if (!"ISSUED".equals(state.state())) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        Material material = material(state);
        boolean early = mapper.now().isBefore(state.createdAt().plusNanos(300_000_000));
        boolean correct = !early && Math.abs(material.answerX() - offsetX) <= 4;
        String nextState = correct ? "VERIFIED" : "FAILED";
        mapper.verifyCaptcha(id, context.key(), fingerprint, nextState, state.verificationData());
        Verification result = correct
                ? new Verification(proof(state), null)
                : new Verification(null, IdentityErrorCode.CAPTCHA_INVALID);
        return result;
    }

    public void consume(Context context, String purpose, UUID id, String token, Object authenticationInput) {
        transactionExecutor.independent(() -> {
            CaptchaState state = mapper.findCaptcha(id);
            requireBinding(state, context);
            requireValid(state);
            if (!purpose.equals(state.purpose()) || !Objects.equals(material(state).verificationToken(), token)) {
                throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
            }
            byte[] fingerprint = cryptography.digest("captcha-consumption", authenticationInput);
            if ("CONSUMED".equals(state.state())) {
                if (context.key().equals(state.consumedKey())
                        && MessageDigest.isEqual(fingerprint, state.consumedFingerprint())) {
                    return Boolean.TRUE;
                }
                throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
            }
            if (!"VERIFIED".equals(state.state())) {
                throw new BusinessException(IdentityErrorCode.CAPTCHA_INVALID);
            }
            mapper.consumeCaptcha(id, context.key(), fingerprint);
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

    private byte[] binding(Context context) {
        byte[] digest = cryptography.digest("captcha-binding", context.scope());
        return digest;
    }

    private void requireBinding(CaptchaState state, Context context) {
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
