/*
 * 声明通知Unread返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.vo;

/* 未读数排除已失效消息，不将失效伪装成已读。 */
// 定义保存一组固定字段的 record NotificationUnreadVO；声明通知Unread返回给页面的字段，不直接把数据库实体全部暴露出去。
// unread数量（unreadCount），使用 long 类型保存。
public record NotificationUnreadVO(long unreadCount) {}
