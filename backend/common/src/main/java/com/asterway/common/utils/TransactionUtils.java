package com.asterway.common.utils;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

// 只封装事务执行方式；业务顺序、锁及持久化仍由 Service 明确管理。
public final class TransactionUtils {

    private TransactionUtils() {
    }

    public static <T> T required(PlatformTransactionManager manager, Supplier<T> work) {
        T result = required(manager, status -> work.get());
        return result;
    }

    public static <T> T required(PlatformTransactionManager manager, Function<TransactionStatus, T> work) {
        TransactionTemplate transaction = new TransactionTemplate(manager);
        T result = transaction.execute(work::apply);
        return Objects.requireNonNull(result, "事务结果不能为空");
    }

    public static <T> T independent(PlatformTransactionManager manager, Supplier<T> work) {
        TransactionTemplate transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        T result = transaction.execute(status -> work.get());
        return Objects.requireNonNull(result, "事务结果不能为空");
    }
}
