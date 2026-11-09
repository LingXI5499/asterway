/*
 * 处理执行计时Lifecycle提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入执行计时数据范围Enum（ExecutionScopeEnum），下面可以直接使用这个类型或静态成员。
import com.asterway.common.enums.ExecutionScopeEnum;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 跨模块执行联动。调用方必须在父业务事务内，所有实现先取得同一用户行锁。 */
// 定义接口 ExecutionLifecycleProvider；处理执行计时Lifecycle提供器相关的当前身份、权限或安全数据。
public interface ExecutionLifecycleProvider {
    /* 完成任务前，要求该任务所有执行均已结束。 */
    // require任务Finished（requireTaskFinished）方法：要求满足 任务Finished（TaskFinished）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 任务编号（taskId），使用 long 类型保存。
    void requireTaskFinished(long userId, long taskId);

    /* 改期、移出或删除计划前，要求范围内没有运行中的执行；暂停记录允许保留。 */
    // requireNo运行中（requireNoRunning）方法：要求满足 No运行中（NoRunning）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 long 类型保存。
    void requireNoRunning(long userId, ExecutionScopeEnum scope, long id);

    /* 父链状态或开始日期写入后校验运行资格，不满足则令父操作回滚。 */
    // 检查合法性运行中Eligibility（validateRunningEligibility）方法：重新核对正在运行的计时是否仍满足任务和计划条件；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    void validateRunningEligibility(long userId);

    /* 删除任务时结束未结束会话，实际时间段仍按既有截止时间封顶。 */
    // finish任务（finishTask）方法：调用 finishTask 完成finish任务处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 任务编号（taskId），使用 long 类型保存。
    void finishTask(long userId, long taskId);

    /* 归档目标时结束全部未结束会话，包含暂停与已逻辑删除子项的历史关联。 */
    // finish目标（finishGoal）方法：调用 finishGoal 完成finish目标处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    void finishGoal(long userId, long goalId);

    /* 登录撤销前暂停对应控制端；sessionId 为空时处理该用户的全部控制端。 */
    // 暂停登录（pauseLogin）方法：调用 pauseLogin 完成暂停登录处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    void pauseLogin(long userId, String sessionId);

    /* 时区改动同步缩短开放段的资格截止；空值不延长已经确认的边界。 */
    // shortenBoundary方法：调用 shortenBoundary 完成shortenBoundary处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 生效At（effectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    void shortenBoundary(long userId, Instant effectiveAt);
}
