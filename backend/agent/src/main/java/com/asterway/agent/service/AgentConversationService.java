/*
 * 规定成长助手对话业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service;

// 引入成长助手对话创建（AgentConversationCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationCreateDTO;
// 引入成长助手对话消息（AgentConversationMessageDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationMessageDTO;
// 引入成长助手对话查询条件（AgentConversationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationQueryDTO;
// 引入成长助手对话标题（AgentConversationTitleDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationTitleDTO;
// 引入成长助手对话详情（AgentConversationDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationDetailVO;
// 引入成长助手对话（AgentConversationVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationVO;
// 引入成长助手消息当前页（AgentMessagePageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentMessagePageVO;
// 引入成长助手发送结果（AgentSendResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentSendResultVO;
// 引入成长助手本轮对话（AgentTurnVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentTurnVO;
// 引入成长业务版本号（GrowthVersionDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthVersionDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;

// 引入Streaming响应请求正文（StreamingResponseBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/* 会话持久化代理；Java 只做认证、权限和转发，不持有 Agent 表。 */
// 定义接口 AgentConversationService；规定成长助手对话业务服务可供其他代码调用的业务方法。
public interface AgentConversationService {

    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 AgentConversationCreateDTO 类型保存。
    AgentConversationDetailVO create(AgentConversationCreateDTO dto);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 AgentConversationQueryDTO 类型保存。
    PageResultVO<AgentConversationVO> page(AgentConversationQueryDTO query);

    // 详情（detail）方法：调用 detail 完成详情处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    AgentConversationDetailVO detail(long conversationId);

    // 重命名（rename）方法：调用 rename 完成重命名处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AgentConversationTitleDTO 类型保存。
    AgentConversationDetailVO rename(long conversationId, AgentConversationTitleDTO dto);

    // 关闭（close）方法：释放连接、文件或客户端占用的资源；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
    AgentConversationDetailVO close(long conversationId, GrowthVersionDTO dto);

    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long conversationId, long version);

    // 消息列表（messages）方法：调用 messages 完成消息列表处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 变化前消息No（beforeMessageNo），使用 Long 类型保存；可为 null，表示尚未提供。
    // 每页条数（size），使用 int 类型保存。
    AgentMessagePageVO messages(long conversationId, Long beforeMessageNo, int size);

    // 本轮对话（turn）方法：调用 turn 完成本轮对话处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 本轮对话编号（turnId），使用 long 类型保存。
    AgentTurnVO turn(long conversationId, long turnId);

    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 本轮对话编号（turnId），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
    AgentTurnVO cancel(long conversationId, long turnId, GrowthVersionDTO dto);

    // 发送（send）方法：调用 send 完成发送处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
    AgentSendResultVO send(long conversationId, AgentConversationMessageDTO dto);

    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
    StreamingResponseBody stream(long conversationId, AgentConversationMessageDTO dto);
}
