/*
 * 实现成长助手工作区业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service.impl;

// 引入成长助手工作区业务服务（AgentWorkspaceService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentWorkspaceService;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长助手客户端（AgentClient），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentClient;
// 引入成长助手请求上下文（AgentRequestContext），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.AgentRequestContext;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入对象节点（ObjectNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.node.ObjectNode;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入请求上下文Holder（RequestContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.RequestContextHolder;
// 引入Servlet请求Attributes（ServletRequestAttributes），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.ServletRequestAttributes;

/* 固定资源代理；Python 仍复核有效身份、权限、数据归属和完整业务参数。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 AgentWorkspaceServiceImpl；实现成长助手工作区业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentWorkspaceServiceImpl implements AgentWorkspaceService {
    // 准备最大文件BYTES（MAX_FILE_BYTES），类型为 int；初始内容是固定数值 26214400。
    private static final int MAX_FILE_BYTES = 26_214_400;
    // 客户端（client），使用 AgentClient 类型保存。
    private final AgentClient client;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 客户端（client），使用 AgentClient 类型保存。
    public AgentWorkspaceServiceImpl(AgentClient client) {
        // 把客户端（client）保存到 this.client。
        this.client = client;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 请求（request）方法：调用 request 完成请求处理；参数负责传入本次处理所需的数据。
    @Override
    // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
    // resource，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    // 请求正文（body），使用 JsonNode 类型保存。
    public JsonNode request(String method, String resource, Map<String, String> query, JsonNode body) {
        // 准备上下文（context），类型为 AgentRequestContext；初始内容是调用 context 完成上下文处理。
        AgentRequestContext context = context(permission(method, resource));
        // 准备提交数据（payload），类型为 JsonNode；初始内容是根据前面的条件，从两个候选结果中选择一个：body == null ? null : body.deepCopy()。
        JsonNode payload = body == null ? null : body.deepCopy();
        // 对象（object），使用 ObjectNode 类型保存。
        // 检查条件 payload instanceof ObjectNode object 是否成立；成立时执行下面的分支。
        if (payload instanceof ObjectNode object) {
            // 前端 ID 保留字符串精度，内部严格契约使用 JSON 整数。
            // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
            // 逐次执行循环：for (String field : new String[]；每轮处理一条数据或推进一次检查。
            for (String field : new String[] {"conversationId", "sourceTurnId", "turnId", "supersedesId", "planVersionId", "sourceRunId"}) {
                // 准备值（value），类型为 JsonNode；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                JsonNode value = object.get(field);
                // 检查条件 value != null && value.isTextual() 是否成立；成立时执行下面的分支。
                if (value != null && value.isTextual()) {
                    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                    try {
                        // 准备编号（id），类型为 long；初始内容是把输入转换成数值，供比较、计算或按编号查询。
                        long id = Long.parseLong(value.asText());
                        // 检查条件 id <= 0 是否成立；成立时执行下面的分支。
                        // 中止正常处理并抛出异常：新建 数值格式Exception（NumberFormatException） 实例；由上层按失败处理。
                        if (id <= 0) throw new NumberFormatException();
                        // 保存指定键对应的值。
                        object.put(field, id);
                    // 错误（error），使用 NumberFormatException 类型保存。
                    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
                    } catch (NumberFormatException error) {
                        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                        throw BusinessException.badRequest("业务标识必须为正整数");
                    }
                }
            }
        }
        // 把调用 client.workspaceRequest 完成工作区请求处理交还给调用方，本方法到此结束。
        return client.workspaceRequest(method, "/internal/agent/" + resource, query, payload, context);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 上传（upload）方法：调用 upload 完成上传处理；参数负责传入本次处理所需的数据。
    @Override
    // resource，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    // mime类型（mimeType），使用 String 类型保存；可为 null，表示尚未提供。
    // 内容（content），使用 byte[] 类型保存。
    public JsonNode upload(String resource, Map<String, String> query, String mimeType, byte[] content) {
        AgentRequestContext context = context("agent:knowledge:manage");
        if (content == null || content.length == 0) {
            throw new BusinessException(422, "KNOWLEDGE_EMPTY", "文件为空，请选择有内容的文档");
        }
        // 读取与页面相同的有效上限；服务端配置调低时，Java也使用调低后的值。
        JsonNode capabilities = client.workspaceRequest("GET", "/internal/agent/knowledge/capabilities", Map.of(), null, context);
        long maxBytes = Math.min(MAX_FILE_BYTES, capabilities.path("maxFileBytes").asLong(MAX_FILE_BYTES));
        if (content.length > maxBytes) {
            throw new BusinessException(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的大小上限，请拆分后导入");
        }
        String fileName = query.getOrDefault("fileName", "").toLowerCase(java.util.Locale.ROOT);
        boolean supported = false;
        for (JsonNode format : capabilities.path("formats")) {
            String extension = format.path("extension").asText();
            if (!extension.isEmpty() && fileName.endsWith(extension)) supported = true;
        }
        if (!supported) {
            throw new BusinessException(422, "KNOWLEDGE_TYPE_UNSUPPORTED", "支持TXT、Markdown（.md）、PDF和DOCX文件");
        }
        return client.workspaceUpload("/internal/agent/" + resource, query, mimeType, content, context);
    }

    // 权限（permission）方法：调用 permission 完成权限处理；参数负责传入本次处理所需的数据。
    // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
    // resource，使用 String 类型保存；可为 null，表示尚未提供。
    private String permission(String method, String resource) {
        // 检查条件 resource.matches("reviews/[1-9][0-9]* /proposals") 是否成立；成立时执行下面的分支。
        // 把固定文字 'agent:workflow:manage'交还给调用方，本方法到此结束。
        if (resource.matches("reviews/[1-9][0-9]*/proposals")) return "agent:workflow:manage";
        // 检查条件 resource.equals("runs/tasks") || resource.matches("runs/tasks/[1-9][0-9]* /retry") 是否成立；成立时执行下面的分支。
        // 把固定文字 'agent:run:read'交还给调用方，本方法到此结束。
        if (resource.equals("runs/tasks") || resource.matches("runs/tasks/[1-9][0-9]*/retry")) return "agent:run:read";
        // subject，使用 String 类型保存；可为 null，表示尚未提供。
        if (resource.matches("conversations/[1-9][0-9]*/knowledge-bases")) return "agent:conversation:update";
        String subject;
        // 检查条件 resource.startsWith("memories") 是否成立；成立时执行下面的分支。
        // 把固定文字 'memory'保存到 subject。
        if (resource.startsWith("memories")) subject = "memory";
        // 检查条件 resource.startsWith("knowledge") 是否成立；成立时执行下面的分支。
        // 把固定文字 'knowledge'保存到 subject。
        else if (resource.startsWith("knowledge")) subject = "knowledge";
        // 检查条件 resource.startsWith("planning/proposals") 是否成立；成立时执行下面的分支。
        // 把固定文字 'workflow'保存到 subject。
        else if (resource.startsWith("planning/proposals")) subject = "workflow";
        // 检查条件 resource.startsWith("reviews") 是否成立；成立时执行下面的分支。
        // 把固定文字 'review'保存到 subject。
        else if (resource.startsWith("reviews")) subject = "review";
        // 检查条件 resource.equals("runs/calls") 是否成立；成立时执行下面的分支。
        // 把固定文字 'agent:run:read'交还给调用方，本方法到此结束。
        else if (resource.equals("runs/calls")) return "agent:run:read";
        // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
        else throw BusinessException.notFound();
        // 把根据前面的条件，从两个候选结果中选择一个："agent:" + subject + ("GET".equals(method) ? ":read" : ":manage")交还给调用方，本方法到此结束。
        return "agent:" + subject + ("GET".equals(method) ? ":read" : ":manage");
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    private AgentRequestContext context(String permission) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission(permission);
        // 属性集合（attrs），使用 ServletRequestAttributes 类型保存。
        // 检查条件 !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) 是否成立；成立时执行下面的分支。
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
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
