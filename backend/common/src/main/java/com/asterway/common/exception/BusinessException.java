/*
 * 提供带业务错误码和可读提示的异常相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.exception;

/* 预期业务拒绝，包含 HTTP 状态和可公开数据；不携带密码等敏感信息。 */
// 定义类 BusinessException；提供带业务错误码和可读提示的异常相关的共享实现或应用配置。
public class BusinessException extends RuntimeException {

    // 状态（status），使用 int 类型保存。
    private final int status;
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private final String code;
    // 数据（data），使用 Object 类型保存。
    private final Object data;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    public BusinessException(int status, String code, String message) {
        // 调用 this 完成this处理。
        this(status, code, message, null);
    }

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    // 数据（data），使用 Object 类型保存。
    public BusinessException(int status, String code, String message, Object data) {
        // 调用 super 完成super处理。
        super(message);
        // 把状态（status）保存到 this.status。
        this.status = status;
        // 把编码（code）保存到 this.code。
        this.code = code;
        // 把数据（data）保存到 this.data。
        this.data = data;
    }

    // 读取 状态（Status） 的值。
    public int getStatus() {
        // 把状态（status）交还给调用方，本方法到此结束。
        return status;
    }

    // 读取 编码（Code） 的值。
    public String getCode() {
        // 把编码（code）交还给调用方，本方法到此结束。
        return code;
    }

    // 读取 数据（Data） 的值。
    public Object getData() {
        // 把数据（data）交还给调用方，本方法到此结束。
        return data;
    }

    // bad请求（badRequest）方法：调用 badRequest 完成bad请求处理；参数负责传入本次处理所需的数据。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    public static BusinessException badRequest(String message) {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(400, "INVALID_ARGUMENT", message);
    }

    // conflict方法：调用 conflict 完成conflict处理；参数负责传入本次处理所需的数据。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    public static BusinessException conflict(String message) {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(409, "STATE_CONFLICT", message);
    }

    // notFound方法：调用 notFound 完成notFound处理；参数负责传入本次处理所需的数据。
    public static BusinessException notFound() {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(404, "NOT_FOUND", "资源不存在或不在可访问范围内");
    }

    // unauthorized方法：调用 unauthorized 完成unauthorized处理；参数负责传入本次处理所需的数据。
    public static BusinessException unauthorized() {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(401, "UNAUTHORIZED", "登录已失效，请重新登录");
    }

    // forbidden方法：调用 forbidden 完成forbidden处理；参数负责传入本次处理所需的数据。
    public static BusinessException forbidden() {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(403, "FORBIDDEN", "没有执行此操作的权限");
    }
}
