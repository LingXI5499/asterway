package com.asterway.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 数据库提交后的辅助资源清理。安全失效以数据库 auth_version 为最终依据。 */
public final class TransactionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionUtils.class);

    private TransactionUtils() {
    }

    /** 提交后尽力清理 Redis；失败不将已经提交的数据库操作误报为未保存。 */
    public static void afterCommit(Runnable action) {
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (RuntimeException error) {
                LOGGER.warn("提交后会话清理失败，将由数据库安全版本阻止旧身份访问，异常类型：{}",
                        error.getClass().getSimpleName());
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeAction.run();
            }
        });
    }
}
