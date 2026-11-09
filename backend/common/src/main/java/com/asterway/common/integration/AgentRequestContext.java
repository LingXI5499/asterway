/*
 * 提供成长助手请求上下文相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.integration;

/* 异步转发所需的不可变请求上下文；不能在异步线程中重新读取 SecurityContext。 */
// 定义保存一组固定字段的 record AgentRequestContext；提供成长助手请求上下文相关的共享实现或应用配置。
// 用户编号（userId），使用 long 类型保存。
// 授权（authorization），使用 String 类型保存；可为 null，表示尚未提供。
// 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
public record AgentRequestContext(long userId, String authorization, String traceId) {

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // to文字（toString）方法：调用 toString 完成to文字处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public String toString() {
        // 把固定文字 'AgentRequestContext[userId=" + userId + ", traceId=" + traceId + "]'交还给调用方，本方法到此结束。
        return "AgentRequestContext[userId=" + userId + ", traceId=" + traceId + "]";
    }
}
