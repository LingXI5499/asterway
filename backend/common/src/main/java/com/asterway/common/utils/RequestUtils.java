/*
 * 集中处理请求Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入Java HTTP 请求对象（HttpServletRequest），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletRequest;
// 引入请求上下文Holder（RequestContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.RequestContextHolder;
// 引入Servlet请求Attributes（ServletRequestAttributes），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.ServletRequestAttributes;

/* 请求元数据；当前本地开发不信任客户端伪造的代理 IP 请求头。 */
// 定义类 RequestUtils；集中处理请求Utils的重复规则，避免各业务模块分别实现。
public final class RequestUtils {

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private RequestUtils() {
    }

    // ipAddress方法：调用 ipAddress 完成ipAddress处理；参数负责传入本次处理所需的数据。
    public static String ipAddress() {
        // 准备请求（request），类型为 HttpServletRequest；初始内容是调用 request 完成请求处理。
        HttpServletRequest request = request();
        // 把根据前面的条件，从两个候选结果中选择一个：request == null ? null : request.getRemoteAddr()交还给调用方，本方法到此结束。
        return request == null ? null : request.getRemoteAddr();
    }

    // 用户成长助手（userAgent）方法：调用 userAgent 完成用户成长助手处理；参数负责传入本次处理所需的数据。
    public static String userAgent() {
        // 准备请求（request），类型为 HttpServletRequest；初始内容是调用 request 完成请求处理。
        HttpServletRequest request = request();
        // 准备值（value），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：request == null ? "" : request.getHeader("User-Agent")。
        String value = request == null ? "" : request.getHeader("User-Agent");
        // 把根据前面的条件，从两个候选结果中选择一个：value == null ? "" : value.substring(0, Math.min(512, value.length()))交还给调用方，本方法到此结束。
        return value == null ? "" : value.substring(0, Math.min(512, value.length()));
    }

    // 请求（request）方法：调用 request 完成请求处理；参数负责传入本次处理所需的数据。
    private static HttpServletRequest request() {
        // attributes，使用 ServletRequestAttributes 类型保存。
        // 把根据前面的条件，从两个候选结果中选择一个：RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes ? attribu…交还给调用方，本方法到此结束。
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                // 继续补充同一个判断或备用结果：条件 ? attributes.getRequest() : null; 是否成立。
                ? attributes.getRequest() : null;
    }
}
