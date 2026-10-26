package com.asterway.identity.service;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.OperationSnapshot;
import com.asterway.identity.infrastructure.security.AccountCryptography;
import com.asterway.identity.infrastructure.serialization.AccountJsonCodec;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.OperationRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;

import java.security.MessageDigest;
import java.util.Map;
import java.util.function.Supplier;

// 同一意图先串行检查原结果，再执行版本与状态检查；长期结果不包含认证秘密。
@Service
public class AccountIdempotencyService {

    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final AccountCryptography cryptography;
    private final AccountJsonCodec jsonCodec;

    public AccountIdempotencyService(
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor,
            AccountCryptography cryptography,
            AccountJsonCodec jsonCodec
    ) {
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        this.cryptography = cryptography;
        this.jsonCodec = jsonCodec;
    }

    public <T> Execution<T> execute(
            Context context,
            String operation,
            Object input,
            Class<T> resultType,
            Supplier<Outcome<T>> action
    ) {
        byte[] fingerprint = cryptography.digest("request:" + operation, input);
        Execution<T> execution = transactionExecutor.required(status ->
                executeLocked(context, operation, fingerprint, resultType, action, status));
        if (execution.failure() != null) {
            throw execution.failure();
        }
        return execution;
    }

    // 认证前先核对已有结果，避免重试再次消费验证码或重复计算密码哈希。
    public <T> Execution<T> existing(Context context, String operation, Object input, Class<T> type) {
        OperationRecord record = mapper.findOperation(context, operation);
        if (record == null) {
            return null;
        }
        Execution<T> result = replay(record, cryptography.digest("request:" + operation, input), type);
        if (result.failure() != null) {
            throw result.failure();
        }
        return result;
    }

    private <T> Execution<T> executeLocked(
            Context context,
            String operation,
            byte[] fingerprint,
            Class<T> resultType,
            Supplier<Outcome<T>> action,
            TransactionStatus transactionStatus
    ) {
        mapper.lockScope("operation:" + context.scope() + ":" + operation + ":" + context.key());
        OperationRecord existing = mapper.findOperation(context, operation);
        if (existing != null) {
            Execution<T> replay = replay(existing, fingerprint, resultType);
            return replay;
        }
        mapper.insertOperation(context, operation, fingerprint);

        // 保存点确保业务拒绝没有部分写入；认证失败用 Outcome 提交失败计数。
        Object savepoint = transactionStatus.createSavepoint();
        Outcome<T> outcome;
        try {
            outcome = action.get();
        } catch (BusinessException exception) {
            transactionStatus.rollbackToSavepoint(savepoint);
            outcome = failure(exception);
        } catch (DataIntegrityViolationException exception) {
            transactionStatus.rollbackToSavepoint(savepoint);
            if (!isUsernameConflict(exception)) {
                throw exception;
            }
            outcome = failure(new BusinessException(IdentityErrorCode.USERNAME_TAKEN));
        } finally {
            transactionStatus.releaseSavepoint(savepoint);
        }
        String code = outcome.failure() == null ? "OK" : outcome.failure().getErrorCode().code();
        int status = outcome.failure() == null ? successStatus(operation) : errorStatus(code);
        Object summary = outcome.failure() == null ? outcome.data() : safeData(outcome.failure());
        mapper.finishOperation(context, operation, code, status, jsonCodec.write(summary));
        Execution<T> execution = new Execution<>(outcome.data(), outcome.failure(), false);
        return execution;
    }

    private <T> Execution<T> replay(OperationRecord record, byte[] fingerprint, Class<T> type) {
        if (!MessageDigest.isEqual(record.requestFingerprint(), fingerprint)) {
            throw new BusinessException(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED);
        }
        if ("PROCESSING".equals(record.state())) {
            throw new AccountException(IdentityErrorCode.REQUEST_IN_PROGRESS, Map.of("retryAfterSeconds", "1"));
        }
        if (!"OK".equals(record.resultCode())) {
            ErrorCode code = errorCode(record.resultCode());
            Map<String, Object> data = readMap(record.resultSummary());
            Execution<T> failed = new Execution<>(null, new AccountException(code, data), true);
            return failed;
        }
        T result = jsonCodec.read(record.resultSummary(), type);
        Execution<T> execution = new Execution<>(result, null, true);
        return execution;
    }

    public OperationSnapshot query(Context context, String operation) {
        if (!operation.matches("API-AUTH-(007|008|009|010)")) {
            throw new BusinessException(IdentityErrorCode.OPERATION_RESULT_NOT_FOUND);
        }
        OperationRecord record = mapper.findOperation(context, operation);
        if (record == null) {
            throw new BusinessException(IdentityErrorCode.OPERATION_RESULT_NOT_FOUND);
        }
        OperationSnapshot snapshot = new OperationSnapshot(
                operation, context.key(), record.state(),
                record.httpStatus() == null ? 0 : record.httpStatus(),
                record.resultCode(), readMap(record.resultSummary())
        );
        return snapshot;
    }

    public static <T> Outcome<T> success(T data) {
        Outcome<T> outcome = new Outcome<>(data, null);
        return outcome;
    }

    public static <T> Outcome<T> failure(BusinessException failure) {
        Outcome<T> outcome = new Outcome<>(null, failure);
        return outcome;
    }

    private Object safeData(BusinessException failure) {
        if (failure instanceof AccountException accountException) {
            return accountException.getData();
        }
        return Map.of();
    }

    private Map<String, Object> readMap(String json) {
        try {
            Map<String, Object> data = jsonCodec.read(json, new TypeReference<>() { });
            return data;
        } catch (IllegalStateException exception) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, exception);
        }
    }

    private ErrorCode errorCode(String code) {
        try {
            ErrorCode errorCode = IdentityErrorCode.valueOf(code);
            return errorCode;
        } catch (IllegalArgumentException exception) {
            ErrorCode errorCode = CommonErrorCode.valueOf(code);
            return errorCode;
        }
    }

    private boolean isUsernameConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("uq_app_user_username_key")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private int successStatus(String operation) {
        int status = "API-AUTH-004".equals(operation) ? 201 : 200;
        return status;
    }

    private int errorStatus(String code) {
        int status = switch (code) {
            case "CREDENTIALS_INVALID", "AUTH_REQUIRED" -> 401;
            case "REGISTRATION_CLOSED", "ACCOUNT_UNAVAILABLE", "FORBIDDEN" -> 403;
            case "CAPTCHA_INVALID", "CURRENT_PASSWORD_INVALID" -> 400;
            case "CAPTCHA_EXPIRED" -> 410;
            case "ACCOUNT_LOCKED" -> 423;
            case "RATE_LIMITED" -> 429;
            case "VALIDATION_ERROR", "PASSWORD_MISMATCH" -> 422;
            case "SERVICE_UNAVAILABLE", "RESULT_UNKNOWN" -> 503;
            default -> 409;
        };
        return status;
    }

    public record Outcome<T>(T data, BusinessException failure) {
    }

    public record Execution<T>(T data, BusinessException failure, boolean replayed) {
    }
}
