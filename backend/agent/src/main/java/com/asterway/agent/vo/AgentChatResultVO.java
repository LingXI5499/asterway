/*
 * 声明成长助手Chat结果返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

/* 普通对话结果；非 stop 的结束原因代表回复可能被截断，界面不得显示为完整成功。 */
// 定义保存一组固定字段的 record AgentChatResultVO；声明成长助手Chat结果返回给页面的字段，不直接把数据库实体全部暴露出去。
// reply，使用 String 类型保存；可为 null，表示尚未提供。
// 模型（model），使用 String 类型保存；可为 null，表示尚未提供。
// finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
public record AgentChatResultVO(String reply, String model, String finishReason) {}
