/*
 * 处理Security错误Handler相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入Java HTTP 请求对象（HttpServletRequest），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletRequest;
// 引入Java HTTP 响应对象（HttpServletResponse），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletResponse;
// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;
// 引入StandardCharsets，下面可以直接使用这个类型或静态成员。
import java.nio.charset.StandardCharsets;
// 引入访问权限DeniedException（AccessDeniedException），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.AccessDeniedException;
// 引入身份认证Exception（AuthenticationException），下面可以直接使用这个类型或静态成员。
import org.springframework.security.core.AuthenticationException;
// 引入身份认证Entry时间点（AuthenticationEntryPoint），下面可以直接使用这个类型或静态成员。
import org.springframework.security.web.AuthenticationEntryPoint;
// 引入访问权限DeniedHandler（AccessDeniedHandler），下面可以直接使用这个类型或静态成员。
import org.springframework.security.web.access.AccessDeniedHandler;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* 过滤器异常响应，与 Controller 全局异常使用相同 JSON 结构。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 SecurityErrorHandler；处理Security错误Handler相关的当前身份、权限或安全数据。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public SecurityErrorHandler(ObjectMapper json) {
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // commence方法：调用 commence 完成commence处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 响应（response），使用 HttpServletResponse 类型保存。
    // 错误（error），使用 AuthenticationException 类型保存。
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 把内容写入当前输出或响应。
        write(response, 401, "UNAUTHORIZED", "登录已失效，请重新登录");
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // handle方法：调用 handle 完成handle处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 响应（response），使用 HttpServletResponse 类型保存。
    // 错误（error），使用 AccessDeniedException 类型保存。
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 把内容写入当前输出或响应。
        write(response, 403, "FORBIDDEN", "没有执行此操作的权限");
    }

    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 响应（response），使用 HttpServletResponse 类型保存。
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        // 设置 状态（Status） 的值。
        response.setStatus(status);
        // 设置 内容类型（ContentType） 的值。
        response.setContentType("application/json");
        // 设置 CharacterEncoding 的值。
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 写入 值（Value）。
        json.writeValue(response.getOutputStream(), ResultVO.failure(code, message, null));
    }
}
