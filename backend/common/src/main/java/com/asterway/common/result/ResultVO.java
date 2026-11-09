/*
 * 提供结果相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.result;

// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;

/* HTTP 统一响应。业务错误同时保留真实 HTTP 状态，不以 200 掩盖失败。 */
// 定义保存一组固定字段的 record ResultVO；提供结果相关的共享实现或应用配置。
// 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
// 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
// 数据（data），使用 T 类型保存。
// 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
public record ResultVO<T>(String code, String message, T data, String traceId) {

    /* 包装成功结果；允许 data 为空，保持响应字段稳定。 */
    // 成功（success）方法：调用 success 完成成功处理；参数负责传入本次处理所需的数据。
    // 数据（data），使用 T 类型保存。
    public static <T> ResultVO<T> success(T data) {
        // 把新建 结果（ResultVO） 实例交还给调用方，本方法到此结束。
        return new ResultVO<>("OK", "操作成功", data, MDC.get("traceId"));
    }

    /* 包装经过筛选的业务错误，不向客户端暴露异常堆栈。 */
    // 失败（failure）方法：调用 failure 完成失败处理；参数负责传入本次处理所需的数据。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    // 数据（data），使用 Object 类型保存。
    public static ResultVO<Object> failure(String code, String message, Object data) {
        // 把新建 结果（ResultVO） 实例交还给调用方，本方法到此结束。
        return new ResultVO<>(code, message, data, MDC.get("traceId"));
    }
}
