package com.asterway.common.security;

/** 站内消息确认目标完成的中立契约，调用方已校验收件人和当前达标轮次。 */
public interface GoalCompletionProvider {
    /** 在同一用户锁事务内核对目标版本、更新状态并追加通知来源历史。 */
    void confirmFromNotification(long userId, long goalId, long expectedVersion);
}
