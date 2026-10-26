package com.asterway.identity.infrastructure.transaction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

// 将短事务边界集中管理，避免在 return 中展开事务回调和整段业务逻辑。
@Component
public class AccountTransactionExecutor {

    private final TransactionTemplate required;
    private final TransactionTemplate independent;

    public AccountTransactionExecutor(PlatformTransactionManager transactionManager) {
        required = new TransactionTemplate(transactionManager);
        independent = new TransactionTemplate(transactionManager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T required(Supplier<T> work) {
        T result = required.execute(status -> work.get());
        return Objects.requireNonNull(result, "事务结果不能为空");
    }

    public <T> T required(Function<TransactionStatus, T> work) {
        T result = required.execute(work::apply);
        return Objects.requireNonNull(result, "事务结果不能为空");
    }

    public <T> T independent(Supplier<T> work) {
        T result = independent.execute(status -> work.get());
        return Objects.requireNonNull(result, "事务结果不能为空");
    }
}
