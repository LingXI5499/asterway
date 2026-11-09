/*
 * 集中处理事务Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入事务Synchronization（TransactionSynchronization），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionSynchronization;
// 引入事务Synchronization管理员（TransactionSynchronizationManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionSynchronizationManager;

/* 数据库提交后的辅助资源清理。安全失效以数据库 auth_version 为最终依据。 */
// 定义类 TransactionUtils；集中处理事务Utils的重复规则，避免各业务模块分别实现。
public final class TransactionUtils {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionUtils.class);

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private TransactionUtils() {
    }

    /* 提交后尽力清理 Redis；失败不将已经提交的数据库操作误报为未保存。 */
    // 变化后提交回执（afterCommit）方法：调用 afterCommit 完成变化后提交回执处理；参数负责传入本次处理所需的数据。
    // 操作（action），使用 Runnable 类型保存。
    public static void afterCommit(Runnable action) {
        // 准备safe操作（safeAction），类型为 Runnable；初始内容是调用 ()->{ try { action.run 完成方案运行处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        Runnable safeAction = () -> {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 action.run 完成方案运行处理。
                action.run();
            // 错误（error），使用 RuntimeException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (RuntimeException error) {
                // 记录warn级别的运行信息，供定位处理过程和失败原因。
                LOGGER.warn("提交后会话清理失败，将由数据库安全版本阻止旧身份访问，异常类型：{}",
                        // 读取 类（Class） 的值。
                        error.getClass().getSimpleName());
            }
        };
        // 检查条件 !TransactionSynchronizationManager.isSynchronizationActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 调用 safeAction.run 完成方案运行处理。
            safeAction.run();
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 定义类 ；集中处理事务Utils的重复规则，避免各业务模块分别实现。
        // 调用 TransactionSynchronizationManager.registerSynchronization 完成registerSynchronization处理。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // 说明此方法实现或改写了父类、接口中已经声明的方法。
            // 变化后提交回执（afterCommit）方法：调用 afterCommit 完成变化后提交回执处理；参数负责传入本次处理所需的数据。
            @Override
            // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
            public void afterCommit() {
                // 调用 safeAction.run 完成方案运行处理。
                safeAction.run();
            }
        });
    }

    /* 单端撤销没有 auth_version 兜底，Redis 失败时必须向调用方报告，不能静默宣称已退出。 */
    // 变化后提交回执必填（afterCommitRequired）方法：调用 afterCommitRequired 完成变化后提交回执必填处理；参数负责传入本次处理所需的数据。
    // 操作（action），使用 Runnable 类型保存。
    public static void afterCommitRequired(Runnable action) {
        // 准备必填操作（requiredAction），类型为 Runnable；初始内容是调用 ()->{ try { action.run 完成方案运行处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        Runnable requiredAction = () -> {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 action.run 完成方案运行处理。
                action.run();
            // 错误（error），使用 RuntimeException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (RuntimeException error) {
                // 记录warn级别的运行信息，供定位处理过程和失败原因。
                LOGGER.warn("登录会话撤销未完成，异常类型：{}", error.getClass().getSimpleName());
                // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
                throw new BusinessException(
                        // 继续传入或组合表达式 503, "SESSION_REVOCATION_UNAVAILABLE", "登录会话撤销暂不可用，请稍后重试") 的结果，完成上面尚未结束的调用或声明。
                        503, "SESSION_REVOCATION_UNAVAILABLE", "登录会话撤销暂不可用，请稍后重试");
            }
        };
        // 检查条件 !TransactionSynchronizationManager.isSynchronizationActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 调用 requiredAction.run 完成方案运行处理。
            requiredAction.run();
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 定义类 ；集中处理事务Utils的重复规则，避免各业务模块分别实现。
        // 调用 TransactionSynchronizationManager.registerSynchronization 完成registerSynchronization处理。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // 说明此方法实现或改写了父类、接口中已经声明的方法。
            // 变化后提交回执（afterCommit）方法：调用 afterCommit 完成变化后提交回执处理；参数负责传入本次处理所需的数据。
            @Override
            // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
            public void afterCommit() {
                // 调用 requiredAction.run 完成方案运行处理。
                requiredAction.run();
            }
        });
    }
}
