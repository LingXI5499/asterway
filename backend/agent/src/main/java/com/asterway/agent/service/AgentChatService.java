/*
 * 规定成长助手Chat业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service;

// 引入成长助手Chat（AgentChatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentChatDTO;
// 引入成长助手Chat结果（AgentChatResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentChatResultVO;

// 引入Streaming响应请求正文（StreamingResponseBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/* 单轮对话入口，普通结果和流式结果复用相同的身份及权限边界。 */
// 定义接口 AgentChatService；规定成长助手Chat业务服务可供其他代码调用的业务方法。
public interface AgentChatService {
    // 对话（chat）方法：调用 chat 完成对话处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    AgentChatResultVO chat(AgentChatDTO dto);

    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    StreamingResponseBody stream(AgentChatDTO dto);
}
