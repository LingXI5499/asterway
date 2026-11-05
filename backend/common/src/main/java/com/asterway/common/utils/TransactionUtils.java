package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
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

    /** 单端撤销没有 auth_version 兜底，Redis 失败时必须向调用方报告，不能静默宣称已退出。 */
    public static void afterCommitRequired(Runnable action) {
        Runnable requiredAction = () -> {
            try {
                action.run();
            } catch (RuntimeException error) {
                LOGGER.warn("登录会话撤销未完成，异常类型：{}", error.getClass().getSimpleName());
                throw new BusinessException(
                        503, "SESSION_REVOCATION_UNAVAILABLE", "登录会话撤销暂不可用，请稍后重试");
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            requiredAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                requiredAction.run();
            }
        });
    }
}
