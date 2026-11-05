package com.asterway.common.security;

/** 目标、任务与账户调用的达标提醒契约，不增加业务工程依赖。 */
public interface GoalNotificationProvider {
    /** 已持有用户锁；精确重算达标轮次与消息，加入父事务。 */
    void refreshGoal(long userId, long goalId);

    /** 账号失去个人业务资格，撤回待处理消息并清除达标缓存。 */
    void suspendNotifications(long userId);
}
