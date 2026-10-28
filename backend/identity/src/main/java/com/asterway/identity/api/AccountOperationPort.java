package com.asterway.identity.api;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.model.AccountContext;

import java.util.function.Supplier;

// 账户命令的幂等执行能力，具体结果存储由应用层提供。
public interface AccountOperationPort {

    <T> Execution<T> execute(AccountContext context, String operation, Object input, Class<T> resultType,
                             Supplier<Outcome<T>> action);

    <T> Execution<T> existing(AccountContext context, String operation, Object input, Class<T> resultType);

    boolean logoutKeyUsedElsewhere(AccountContext context);

    void clearExpired();

    static <T> Outcome<T> success(T data) {
        return new Outcome<>(data, null);
    }

    static <T> Outcome<T> failure(BusinessException failure) {
        return new Outcome<>(null, failure);
    }

    record Outcome<T>(T data, BusinessException failure) {
    }

    record Execution<T>(T data, BusinessException failure, boolean replayed) {
    }
}
