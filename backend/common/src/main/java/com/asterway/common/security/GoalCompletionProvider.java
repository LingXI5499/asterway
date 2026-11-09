/*
 * 处理目标完成情况提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

/* 站内消息确认目标完成的中立契约，调用方已校验收件人和当前达标轮次。 */
// 定义接口 GoalCompletionProvider；处理目标完成情况提供器相关的当前身份、权限或安全数据。
public interface GoalCompletionProvider {
    /* 在同一用户锁事务内核对目标版本、更新状态并追加通知来源历史。 */
    // 确认来源通知（confirmFromNotification）方法：确认 来源通知（FromNotification）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    // 预计版本号（expectedVersion），使用 long 类型保存。
    void confirmFromNotification(long userId, long goalId, long expectedVersion);
}
