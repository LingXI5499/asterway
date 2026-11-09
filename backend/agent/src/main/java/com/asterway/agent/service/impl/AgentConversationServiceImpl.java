/*
 * 实现成长助手对话业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service.impl;

// 引入成长助手对话创建（AgentConversationCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationCreateDTO;
// 引入成长助手对话消息（AgentConversationMessageDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationMessageDTO;
// 引入成长助手对话查询条件（AgentConversationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationQueryDTO;
// 引入成长助手对话标题（AgentConversationTitleDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationTitleDTO;
// 引入成长助手对话业务服务（AgentConversationService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentConversationService;
// 引入成长助手对话详情（AgentConversationDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationDetailVO;
// 引入成长助手对话（AgentConversationVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationVO;
// 引入成长助手消息当前页（AgentMessagePageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentMessagePageVO;
// 引入成长助手消息（AgentMessageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentMessageVO;
// 引入成长助手发送结果（AgentSendResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentSendResultVO;
// 引入成长助手本轮对话（AgentTurnVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentTurnVO;
// 引入成长业务版本号（GrowthVersionDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthVersionDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长助手客户端（AgentClient），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentClient;
// 引入成长助手请求上下文（AgentRequestContext），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentRequestContext;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;

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
// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入跳过的记录数日期时间（OffsetDateTime），下面可以直接使用这个类型或静态成员。
import java.time.OffsetDateTime;
// 引入日期时间解析Exception（DateTimeParseException），下面可以直接使用这个类型或静态成员。
import java.time.format.DateTimeParseException;
// 引入Array列表（ArrayList），下面可以直接使用这个类型或静态成员。
import java.util.ArrayList;
// 引入Linked摘要值对应表（LinkedHashMap），下面可以直接使用这个类型或静态成员。
import java.util.LinkedHashMap;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;

/* 每个操作在同步阶段完成权限检查，异步流只使用已捕获的请求上下文。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 AgentConversationServiceImpl；实现成长助手对话业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentConversationServiceImpl implements AgentConversationService {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER =
            // 读取 日志记录器（Logger） 的值。
            LoggerFactory.getLogger(AgentConversationServiceImpl.class);
    // 客户端（client），使用 AgentClient 类型保存。
    private final AgentClient client;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 客户端（client），使用 AgentClient 类型保存。
    public AgentConversationServiceImpl(AgentClient client) {
        // 把客户端（client）保存到 this.client。
        this.client = client;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 AgentConversationCreateDTO 类型保存。
    public AgentConversationDetailVO create(AgentConversationCreateDTO dto) {
        // 把创建 对话（Conversation）交还给调用方，本方法到此结束。
        return detail(client.createConversation(dto.title(), dto.knowledgeBaseIds(), context("agent:conversation:create")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 AgentConversationQueryDTO 类型保存。
    public PageResultVO<AgentConversationVO> page(AgentConversationQueryDTO query) {
        // 准备parameters，类型为 Map<String, String>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, String> parameters = new LinkedHashMap<>();
        // 检查条件 query.getStatus() != null 是否成立；成立时执行下面的分支。
        if (query.getStatus() != null) {
            // 保存指定键对应的值。
            parameters.put("status", query.getStatus());
        }
        // 检查输入或集合是否为空：query.getKeyword() != null && !query.getKeyword().isBlank()；成立时执行下面的分支。
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            // 保存指定键对应的值。
            parameters.put("keyword", query.getKeyword().trim());
        }
        // 保存指定键对应的值。
        parameters.put("page", Integer.toString(query.getPage()));
        // 保存指定键对应的值。
        parameters.put("size", Integer.toString(query.getSize()));
        // 准备结果（result），类型为 JsonNode；初始内容是调用 client.conversationPage 完成对话当前页处理。
        JsonNode result =
                // 调用 client.conversationPage 完成对话当前页处理。
                client.conversationPage(parameters, context("agent:conversation:list"));
        // 准备数据列表（items），类型为 List<AgentConversationVO>；初始内容是一个空列表，用来保存后续多条数据。
        List<AgentConversationVO> items = new ArrayList<>();
        // 节点（node），使用 JsonNode 类型保存。
        // 逐次执行循环：for (JsonNode node : result.path("items"))；每轮处理一条数据或推进一次检查。
        for (JsonNode node : result.path("items")) {
            // 加入集合或登记一条待保存记录。
            items.add(conversation(node));
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 继续传入或组合数据列表（items），完成上面尚未结束的调用或声明。
                items,
                // 调用 result.path 完成路径处理。
                result.path("total").asLong(),
                // 调用 result.path 完成路径处理。
                result.path("page").asInt(query.getPage()),
                // 调用 result.path 完成路径处理。
                result.path("size").asInt(query.getSize()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 详情（detail）方法：调用 detail 完成详情处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    public AgentConversationDetailVO detail(long conversationId) {
        return detail(client.conversation(conversationId, context("agent:conversation:read")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 重命名（rename）方法：调用 rename 完成重命名处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public AgentConversationDetailVO rename(
            // 对话编号（conversationId），使用 long 类型保存。
            // 请求参数对象（dto），使用 AgentConversationTitleDTO 类型保存。
            long conversationId, AgentConversationTitleDTO dto) {
        // 把调用 detail(client.renameConversation 完成重命名对话处理交还给调用方，本方法到此结束。
        return detail(
                // 继续传入或组合调用 client.renameConversation 完成重命名对话处理，完成上面尚未结束的调用或声明。
                client.renameConversation(
                        // 继续传入或组合对话编号（conversationId），完成上面尚未结束的调用或声明。
                        conversationId,
                        // 调用 dto.title 完成标题处理。
                        dto.title().trim(),
                        // 调用 dto.version 完成版本号处理。
                        dto.version(),
                        // 调用 context 完成上下文处理。
                        context("agent:conversation:update")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 关闭（close）方法：释放连接、文件或客户端占用的资源；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
    public AgentConversationDetailVO close(long conversationId, GrowthVersionDTO dto) {
        // 把调用 detail(client.closeConversation 完成关闭对话处理交还给调用方，本方法到此结束。
        return detail(
                // 继续传入或组合调用 client.closeConversation 完成关闭对话处理，完成上面尚未结束的调用或声明。
                client.closeConversation(
                        // 继续传入或组合调用 conversationId, dto.version 完成版本号处理，完成上面尚未结束的调用或声明。
                        conversationId, dto.version(), context("agent:conversation:close")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long conversationId, long version) {
        // 删除 对话（Conversation）。
        client.deleteConversation(
                // 继续传入或组合表达式 conversationId, version, context("agent:conversation:delete")) 的结果，完成上面尚未结束的调用或声明。
                conversationId, version, context("agent:conversation:delete"));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 消息列表（messages）方法：调用 messages 完成消息列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 变化前消息No（beforeMessageNo），使用 Long 类型保存；可为 null，表示尚未提供。
    // 每页条数（size），使用 int 类型保存。
    public AgentMessagePageVO messages(long conversationId, Long beforeMessageNo, int size) {
        // 准备结果（result），类型为 JsonNode；初始内容是调用 client.conversationMessages 完成对话消息列表处理。
        JsonNode result =
                // 继续传入或组合调用 client.conversationMessages 完成对话消息列表处理，完成上面尚未结束的调用或声明。
                client.conversationMessages(
                        // 继续传入或组合对话编号（conversationId），完成上面尚未结束的调用或声明。
                        conversationId,
                        // 继续传入或组合变化前消息No（beforeMessageNo），完成上面尚未结束的调用或声明。
                        beforeMessageNo,
                        // 继续传入或组合每页条数（size），完成上面尚未结束的调用或声明。
                        size,
                        // 调用 context 完成上下文处理。
                        context("agent:conversation:read"));
        // 准备数据列表（items），类型为 List<AgentMessageVO>；初始内容是一个空列表，用来保存后续多条数据。
        List<AgentMessageVO> items = new ArrayList<>();
        // 节点（node），使用 JsonNode 类型保存。
        // 逐次执行循环：for (JsonNode node : result.path("items"))；每轮处理一条数据或推进一次检查。
        for (JsonNode node : result.path("items")) {
            // 加入集合或登记一条待保存记录。
            items.add(message(node));
        }
        // 准备下一个变化前（nextBefore），类型为 Long；初始内容是根据前面的条件，从两个候选结果中选择一个：result.hasNonNull("nextBeforeMessageNo") ? result.get("nextBeforeMessageNo").asLong() : null。
        Long nextBefore =
                // 判断是否有 Non空值（NonNull）。
                result.hasNonNull("nextBeforeMessageNo")
                        // 继续补充同一个判断或备用结果：条件 ? result.get("nextBeforeMessageNo").asLong() 是否成立。
                        ? result.get("nextBeforeMessageNo").asLong()
                        // 继续补充同一个判断或备用结果：条件 : null; 是否成立。
                        : null;
        // 把新建 成长助手消息当前页（AgentMessagePageVO） 实例交还给调用方，本方法到此结束。
        return new AgentMessagePageVO(items, result.path("hasMore").asBoolean(false), nextBefore);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 本轮对话（turn）方法：调用 turn 完成本轮对话处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 本轮对话编号（turnId），使用 long 类型保存。
    public AgentTurnVO turn(long conversationId, long turnId) {
        // 把调用 turn(client.conversationTurn 完成对话本轮对话处理交还给调用方，本方法到此结束。
        return turn(
                // 继续传入或组合调用 client.conversationTurn 完成对话本轮对话处理，完成上面尚未结束的调用或声明。
                client.conversationTurn(
                        // 继续传入或组合表达式 conversationId, turnId, context("agent:conversation:read"))) 的结果，完成上面尚未结束的调用或声明。
                        conversationId, turnId, context("agent:conversation:read")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 本轮对话编号（turnId），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
    public AgentTurnVO cancel(long conversationId, long turnId, GrowthVersionDTO dto) {
        // 把取消 本轮对话（Turn）交还给调用方，本方法到此结束。
        return turn(
                // 继续传入或组合取消 本轮对话（Turn），完成上面尚未结束的调用或声明。
                client.cancelTurn(
                        // 继续传入或组合对话编号（conversationId），完成上面尚未结束的调用或声明。
                        conversationId,
                        // 继续传入或组合本轮对话编号（turnId），完成上面尚未结束的调用或声明。
                        turnId,
                        // 调用 dto.version 完成版本号处理。
                        dto.version(),
                        // 调用 context 完成上下文处理。
                        context("agent:chat:send")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 发送（send）方法：调用 send 完成发送处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
    public AgentSendResultVO send(long conversationId, AgentConversationMessageDTO dto) {
        // 准备结果（result），类型为 JsonNode；初始内容是调用 client.sendMessage 完成发送消息处理。
        JsonNode result =
                // 继续传入或组合调用 client.sendMessage 完成发送消息处理，完成上面尚未结束的调用或声明。
                client.sendMessage(
                        // 继续传入或组合对话编号（conversationId），完成上面尚未结束的调用或声明。
                        conversationId,
                        // 调用 dto.message 完成消息处理。
                        dto.message(),
                        // 调用 dto.requestId 完成请求编号处理。
                        dto.requestId().toString(), dto.knowledgeBaseIds(),
                        // 调用 context 完成上下文处理。
                        context("agent:chat:send"));
        // 把新建 成长助手发送结果（AgentSendResultVO） 实例交还给调用方，本方法到此结束。
        return new AgentSendResultVO(
                // 调用 result.path 完成路径处理。
                result.path("reply").asText(),
                // 调用 result.path 完成路径处理。
                result.path("model").asText(),
                // 调用 result.path 完成路径处理。
                result.path("finishReason").asText(),
                // 调用 result.path 完成路径处理。
                result.path("conversationId").asText(),
                // 调用 result.path 完成路径处理。
                result.path("turnId").asText(),
                // 调用 result.path 完成路径处理。
                result.path("requestId").asText(),
                // 调用 result.path 完成路径处理。
                result.path("turnVersion").asLong());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    @Override
    // 对话编号（conversationId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
    public StreamingResponseBody stream(long conversationId, AgentConversationMessageDTO dto) {
        // 准备上下文（context），类型为 AgentRequestContext；初始内容是调用 context 完成上下文处理。
        AgentRequestContext context = context("agent:chat:send");
        // 要求满足 已配置（Configured）。
        client.requireConfigured();
        // 输出（output），使用 null 类型保存。
        // 把调用 (output)->{ try { client.streamMessage 完成分段回复流消息处理交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return output -> {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 client.streamMessage 完成分段回复流消息处理。
                client.streamMessage(
                        // 继续传入或组合调用 conversationId, dto.message 完成消息处理，完成上面尚未结束的调用或声明。
                        conversationId, dto.message(), dto.requestId().toString(), dto.knowledgeBaseIds(), context, output);
            // 错误（error），使用 IOException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (IOException error) {
                // 浏览器主动停止后无法再写出结果；上游由客户端 try-with-resources 关闭。
                // 记录debug级别的运行信息，供定位处理过程和失败原因。
                LOGGER.debug("Agent 会话流客户端已断开，traceId={}", context.traceId());
            }
        };
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    private AgentRequestContext context(String permission) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission(permission);
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

    // 对话（conversation）方法：调用 conversation 完成对话处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    private AgentConversationVO conversation(JsonNode node) {
        // 把新建 成长助手对话（AgentConversationVO） 实例交还给调用方，本方法到此结束。
        return new AgentConversationVO(
                // 调用 node.path 完成路径处理。
                node.path("id").asText(),
                // 调用 node.path 完成路径处理。
                node.path("title").asText(),
                // 调用 node.path 完成路径处理。
                node.path("status").asText(),
                // 调用 node.path 完成路径处理。
                node.path("summaryThroughMessageNo").asLong(),
                // 调用 instant 完成instant处理。
                instant(node, "lastMessageAt"),
                // 调用 instant 完成instant处理。
                instant(node, "createdAt"),
                // 调用 instant 完成instant处理。
                instant(node, "updatedAt"),
                // 调用 node.path 完成路径处理。
                node.path("version").asLong(),
                knowledgeIds(node));
    }

    // 详情（detail）方法：调用 detail 完成详情处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    private AgentConversationDetailVO detail(JsonNode node) {
        // 把新建 成长助手对话详情（AgentConversationDetailVO） 实例交还给调用方，本方法到此结束。
        return new AgentConversationDetailVO(
                // 调用 node.path 完成路径处理。
                node.path("id").asText(),
                // 调用 node.path 完成路径处理。
                node.path("title").asText(),
                // 调用 node.path 完成路径处理。
                node.path("status").asText(),
                // 调用 node.path 完成路径处理。
                node.path("summaryThroughMessageNo").asLong(),
                // 调用 instant 完成instant处理。
                instant(node, "lastMessageAt"),
                // 调用 instant 完成instant处理。
                instant(node, "createdAt"),
                // 调用 instant 完成instant处理。
                instant(node, "updatedAt"),
                // 调用 node.path 完成路径处理。
                node.path("version").asLong(),
                // 根据前面的条件，从两个候选结果中选择一个：node.hasNonNull("runningTurn") ? turn(node.get("runningTurn")) : null)。
                node.hasNonNull("runningTurn") ? turn(node.get("runningTurn")) : null,
                knowledgeIds(node));
    }

    // 本轮对话（turn）方法：调用 turn 完成本轮对话处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    private AgentTurnVO turn(JsonNode node) {
        // 把新建 成长助手本轮对话（AgentTurnVO） 实例交还给调用方，本方法到此结束。
        return new AgentTurnVO(
                // 调用 node.path 完成路径处理。
                node.path("id").asText(),
                // 调用 node.path 完成路径处理。
                node.path("conversationId").asText(),
                // 调用 node.path 完成路径处理。
                node.path("requestId").asText(),
                // 调用 node.path 完成路径处理。
                node.path("status").asText(),
                // 调用 text 完成文字处理。
                text(node, "finishReason"),
                // 调用 text 完成文字处理。
                text(node, "errorCode"),
                // 调用 instant 完成instant处理。
                instant(node, "startedAt"),
                // 调用 instant 完成instant处理。
                instant(node, "finishedAt"),
                // 调用 text 完成文字处理。
                text(node, "reply"),
                // 调用 node.path 完成路径处理。
                node.path("version").asLong());
    }

    // 消息（message）方法：调用 message 完成消息处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    private AgentMessageVO message(JsonNode node) {
        // 把新建 成长助手消息（AgentMessageVO） 实例交还给调用方，本方法到此结束。
        return new AgentMessageVO(
                // 调用 node.path 完成路径处理。
                node.path("id").asText(),
                // 调用 node.path 完成路径处理。
                node.path("messageNo").asLong(),
                // 调用 node.path 完成路径处理。
                node.path("role").asText(),
                // 调用 node.path 完成路径处理。
                node.path("content").asText(),
                // 调用 node.path 完成路径处理。
                node.path("isComplete").asBoolean(false),
                // 调用 text 完成文字处理。
                text(node, "finishReason"),
                // 调用 text 完成文字处理。
                text(node, "model"),
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：node.hasNonNull("usage") ? node.get("usage") : null，完成上面尚未结束的调用或声明。
                node.hasNonNull("usage") ? node.get("usage") : null,
                // 调用 instant 完成instant处理。
                instant(node, "createdAt"),
                // 调用 node.path 完成路径处理。
                node.path("citations"),
                // 调用 node.path 完成路径处理。
                node.path("usedMemoryIds"));
    }

    // 文字（text）方法：调用 text 完成文字处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
    private String text(JsonNode node, String field) {
        // 准备值（value），类型为 JsonNode；初始内容是调用 node.path 完成路径处理。
        JsonNode value = node.path(field);
        // 把根据前面的条件，从两个候选结果中选择一个：value.isTextual() ? value.asText() : null交还给调用方，本方法到此结束。
        return value.isTextual() ? value.asText() : null;
    }

    // instant方法：调用 instant 完成instant处理；参数负责传入本次处理所需的数据。
    // 节点（node），使用 JsonNode 类型保存。
    // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
    private Instant instant(JsonNode node, String field) {
        // 准备值（value），类型为 String；初始内容是调用 text 完成文字处理。
        String value = text(node, field);
        // 检查value == null || value.isBlank()，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value.isBlank()) {
            // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
            return null;
        }
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把调用 OffsetDateTime.parse 完成解析处理交还给调用方，本方法到此结束。
            return OffsetDateTime.parse(value).toInstant();
        // 错误（error），使用 DateTimeParseException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (DateTimeParseException error) {
            // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
            return null;
        }
    }
    // JSON编号使用字符串返回，避免前端丢失大整数精度。
    private List<String> knowledgeIds(JsonNode node) {
        List<String> ids = new ArrayList<>();
        for (JsonNode id : node.path("knowledgeBaseIds")) ids.add(id.asText());
        return ids;
    }
}
