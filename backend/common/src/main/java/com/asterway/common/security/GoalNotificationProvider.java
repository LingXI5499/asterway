/*
 * 处理目标通知提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

/* 目标、任务与账户调用的达标提醒契约，不增加业务工程依赖。 */
// 定义接口 GoalNotificationProvider；处理目标通知提供器相关的当前身份、权限或安全数据。
public interface GoalNotificationProvider {
    /* 已持有用户锁；精确重算达标轮次与消息，加入父事务。 */
    // 刷新目标（refreshGoal）方法：按目标最新状态刷新达标提醒；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    void refreshGoal(long userId, long goalId);

    /* 账号失去个人业务资格，撤回待处理消息并清除达标缓存。 */
    // suspend通知列表（suspendNotifications）方法：调用 suspendNotifications 完成suspend通知列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    void suspendNotifications(long userId);
}
