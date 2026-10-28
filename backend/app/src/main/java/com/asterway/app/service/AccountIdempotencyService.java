package com.asterway.app.service;

import com.asterway.app.mapper.AccountOperationMapper;
import com.asterway.app.model.AccountOperationRecord;
import com.asterway.app.web.error.ApiErrorFactory;
import com.asterway.common.api.CryptographyApi;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.JsonUtils;
import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.OperationSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.security.MessageDigest;
import java.util.Map;
import java.util.function.Supplier;

import static com.asterway.identity.api.AccountOperationPort.failure;

// 同一意图先串行检查原结果，再执行版本与状态检查；长期结果不包含认证秘密。
@Service
public class AccountIdempotencyService implements AccountOperationPort {

    private final ApiErrorFactory errorFactory;

    private final AccountOperationMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final CryptographyApi cryptography;
    private final ObjectMapper objectMapper;

    public AccountIdempotencyService(
            ApiErrorFactory errorFactory,
            AccountOperationMapper mapper,
            PlatformTransactionManager transactionManager,
            CryptographyApi cryptography,
            ObjectMapper objectMapper
    ) {
        this.errorFactory = errorFactory;
        this.mapper = mapper;
        this.transactionManager = transactionManager;
        this.cryptography = cryptography;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Execution<T> execute(
            AccountContext context,
            String operation,
            Object input,
            Class<T> resultType,
            Supplier<Outcome<T>> action
    ) {
        byte[] fingerprint = cryptography.digest("request:" + operation, input);
        Execution<T> execution = TransactionUtils.required(transactionManager, status ->
                executeLocked(context, operation, fingerprint, resultType, action, status));
        if (execution.failure() != null) {
            throw execution.failure();
        }
        return execution;
    }

    // 认证前先核对已有结果，避免重试再次消费验证码或重复计算密码哈希。
    @Override
    public <T> Execution<T> existing(AccountContext context, String operation, Object input, Class<T> type) {
        AccountOperationRecord record = mapper.findOperation(context, operation);
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
            AccountContext context,
            String operation,
            byte[] fingerprint,
            Class<T> resultType,
            Supplier<Outcome<T>> action,
            TransactionStatus transactionStatus
    ) {
        mapper.lockScope("operation:" + context.scope() + ":" + operation + ":" + context.key());
        AccountOperationRecord existing = mapper.findOperation(context, operation);
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
        int status = outcome.failure() == null ? errorFactory.successStatus(operation) : errorFactory.resolveStatus(code).value();
        Object summary = outcome.failure() == null ? outcome.data() : safeData(outcome.failure());
        mapper.finishOperation(context, operation, code, status, JsonUtils.write(objectMapper, summary));
        Execution<T> execution = new Execution<>(outcome.data(), outcome.failure(), false);
        return execution;
    }

    private <T> Execution<T> replay(AccountOperationRecord record, byte[] fingerprint, Class<T> type) {
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
        T result = JsonUtils.read(objectMapper, record.resultSummary(), type);
        Execution<T> execution = new Execution<>(result, null, true);
        return execution;
    }

    public OperationSnapshot query(AccountContext context, String operation) {
        if (!AccountOperationCode.supportsResultQuery(operation)) {
            throw new BusinessException(IdentityErrorCode.OPERATION_RESULT_NOT_FOUND);
        }
        AccountOperationRecord record = mapper.findOperation(context, operation);
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

    private Object safeData(BusinessException failure) {
        if (failure instanceof AccountException accountException) {
            return accountException.getData();
        }
        return Map.of();
    }

    private Map<String, Object> readMap(String json) {
        try {
            Map<String, Object> data = JsonUtils.read(objectMapper, json, new TypeReference<>() { });
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

    @Override
    public boolean logoutKeyUsedElsewhere(AccountContext context) {
        return mapper.logoutKeyUsedElsewhere(context);
    }

    @Override
    public void clearExpired() {
        mapper.cleanExpiredOperations();
    }
}
