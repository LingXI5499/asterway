package com.asterway.common.security;

import com.asterway.common.enums.ExecutionScopeEnum;

import java.time.Instant;

/** 跨模块执行联动。调用方必须在父业务事务内，所有实现先取得同一用户行锁。 */
public interface ExecutionLifecycleProvider {
    /** 完成任务前，要求该任务所有执行均已结束。 */
    void requireTaskFinished(long userId, long taskId);

    /** 改期、移出或删除计划前，要求范围内没有运行中的执行；暂停记录允许保留。 */
    void requireNoRunning(long userId, ExecutionScopeEnum scope, long id);

    /** 父链状态或开始日期写入后校验运行资格，不满足则令父操作回滚。 */
    void validateRunningEligibility(long userId);

    /** 删除任务时结束未结束会话，实际时间段仍按既有截止时间封顶。 */
    void finishTask(long userId, long taskId);

    /** 归档目标时结束全部未结束会话，包含暂停与已逻辑删除子项的历史关联。 */
    void finishGoal(long userId, long goalId);

    /** 登录撤销前暂停对应控制端；sessionId 为空时处理该用户的全部控制端。 */
    void pauseLogin(long userId, String sessionId);

    /** 时区改动同步缩短开放段的资格截止；空值不延长已经确认的边界。 */
    void shortenBoundary(long userId, Instant effectiveAt);
}
