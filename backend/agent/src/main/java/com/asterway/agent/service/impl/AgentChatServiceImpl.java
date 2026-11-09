/*
 * 实现成长助手Chat业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service.impl;

// 引入成长助手Chat（AgentChatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentChatDTO;
// 引入成长助手Chat业务服务（AgentChatService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentChatService;
// 引入成长助手Chat结果（AgentChatResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentChatResultVO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长助手客户端（AgentClient），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentClient;
// 引入成长助手请求上下文（AgentRequestContext），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentRequestContext;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;

// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入请求上下文Holder（RequestContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.RequestContextHolder;
// 引入Servlet请求Attributes（ServletRequestAttributes），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.ServletRequestAttributes;
// 引入Streaming响应请求正文（StreamingResponseBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;

/* 同步阶段完成授权，异步阶段只使用已捕获的当前请求上下文。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 AgentChatServiceImpl；实现成长助手Chat业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentChatServiceImpl implements AgentChatService {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentChatServiceImpl.class);
    // 客户端（client），使用 AgentClient 类型保存。
    private final AgentClient client;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 客户端（client），使用 AgentClient 类型保存。
    public AgentChatServiceImpl(AgentClient client) {
        // 把客户端（client）保存到 this.client。
        this.client = client;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 对话（chat）方法：调用 chat 完成对话处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    public AgentChatResultVO chat(AgentChatDTO dto) {
        // 准备结果（result），类型为 null；初始内容是调用 client.chat 完成对话处理。
        var result = client.chat(dto.message(), context());
        // 把新建 成长助手Chat结果（AgentChatResultVO） 实例交还给调用方，本方法到此结束。
        return new AgentChatResultVO(
                // 调用 result.path 完成路径处理。
                result.path("reply").asText(),
                // 调用 result.path 完成路径处理。
                result.path("model").asText(),
                // 调用 result.path 完成路径处理。
                result.path("finish_reason").asText());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    public StreamingResponseBody stream(AgentChatDTO dto) {
        // 准备上下文（context），类型为 AgentRequestContext；初始内容是调用 context 完成上下文处理。
        AgentRequestContext context = context();
        // 要求满足 已配置（Configured）。
        client.requireConfigured();
        // 输出（output），使用 null 类型保存。
        // 把调用 (output)->{ try { client.stream 完成分段回复流处理交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return output -> {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 client.stream 完成分段回复流处理。
                client.stream(dto.message(), context, output);
            // 错误（error），使用 IOException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (IOException error) {
                // 浏览器主动停止后无法再写出任何结果；上游已由客户端 try-with-resources 关闭。
                // 不交给 JSON 异常处理器，避免把正常取消变成 500 或二次写出错误。
                // 记录debug级别的运行信息，供定位处理过程和失败原因。
                LOGGER.debug("Agent 流式客户端已断开，traceId={}", context.traceId());
            }
        };
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    private AgentRequestContext context() {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("agent:chat:send");
        // 检查条件 !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) 是否成立；成立时执行下面的分支。
        if (!(RequestContextHolder.getRequestAttributes()
                // 属性集合（attrs），使用 ServletRequestAttributes 类型保存。
                instanceof ServletRequestAttributes attrs)) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 准备授权（authorization），类型为 String；初始内容是读取 本次 HTTP 请求（Request） 的值。
        String authorization = attrs.getRequest().getHeader("Authorization");
        // 检查authorization == null || !authorization.startsWith("Bearer ")，即对应内容是否不存在；成立时执行下面的分支。
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把新建 成长助手请求上下文（AgentRequestContext） 实例交还给调用方，本方法到此结束。
        return new AgentRequestContext(actor.userId(), authorization, MDC.get("traceId"));
    }
}
