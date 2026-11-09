/*
 * 声明Dashboard读取的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.dto;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 只由 Service 构造的查询上下文，控制端身份不返回前端。 */
// 定义保存一组固定字段的 record DashboardReadDTO；声明Dashboard读取的请求字段和校验条件；接口收到数据后按这些限制检查。
public record DashboardReadDTO(
        // 用户编号（userId），使用 long 类型保存。
        // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
        // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
        long userId, LocalDate today, String timeZone, Instant now, String sessionId) {}
