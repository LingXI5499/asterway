/*
 * 提供追踪标识编号筛选相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.filter;

// 引入筛选Chain（FilterChain），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.FilterChain;
// 引入ServletException，下面可以直接使用这个类型或静态成员。
import jakarta.servlet.ServletException;
// 引入Java HTTP 请求对象（HttpServletRequest），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletRequest;
// 引入Java HTTP 响应对象（HttpServletResponse），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletResponse;
// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;
// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;
// 引入Ordered，下面可以直接使用这个类型或静态成员。
import org.springframework.core.Ordered;
// 引入顺序（Order），下面可以直接使用这个类型或静态成员。
import org.springframework.core.annotation.Order;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;
// 引入OncePer请求筛选（OncePerRequestFilter），下面可以直接使用这个类型或静态成员。
import org.springframework.web.filter.OncePerRequestFilter;

/* 为一次请求创建追踪标识，请求结束必须清理线程上下文。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 TraceIdFilter；提供追踪标识编号筛选相关的共享实现或应用配置。
@Component
// 使用 Order 注解配置下面的类、方法或参数。
@Order(Ordered.HIGHEST_PRECEDENCE)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class TraceIdFilter extends OncePerRequestFilter {

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // do筛选内部（doFilterInternal）方法：调用 doFilterInternal 完成do筛选内部处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 响应（response），使用 HttpServletResponse 类型保存。
    // chain，使用 FilterChain 类型保存。
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws ServletException, IOException {
        // 准备本次请求的追踪编号（traceId），类型为 String；初始内容是调用 UUID.randomUUID 完成randomUUID处理。
        String traceId = UUID.randomUUID().toString();
        // 保存指定键对应的值。
        MDC.put("traceId", traceId);
        // 设置 请求头（Header） 的值。
        response.setHeader("X-Trace-Id", traceId);
        // 设置 请求头（Header） 的值。
        response.setHeader("Cache-Control", "no-store");
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把请求交给过滤链中的下一步处理。
            chain.doFilter(request, response);
        // 无论成功还是失败，都执行下面的资源与状态清理。
        } finally {
            // 调用 MDC.remove 完成移除处理。
            MDC.remove("traceId");
        }
    }
}
