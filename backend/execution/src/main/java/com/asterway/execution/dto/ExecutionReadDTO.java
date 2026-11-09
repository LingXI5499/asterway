/*
 * 声明执行计时读取的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.dto;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* Service 内部构造的查询上下文，不作为 HTTP 入参绑定，保证时间和 owner 不受客户端控制。 */
// 定义保存一组固定字段的 record ExecutionReadDTO；声明执行计时读取的请求字段和校验条件；接口收到数据后按这些限制检查。
public record ExecutionReadDTO(
        // 用户编号（userId），使用 long 类型保存。
        long userId,
        // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
        String sessionId,
        // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant now,
        // 区间开始（rangeStart），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant rangeStart,
        // 区间结束（rangeEnd），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant rangeEnd,
        // 心跳间隔秒数（heartbeatIntervalSeconds），使用 int 类型保存。
        int heartbeatIntervalSeconds) {}
