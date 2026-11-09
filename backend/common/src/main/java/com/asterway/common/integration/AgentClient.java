/*
 * 提供成长助手客户端相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.integration;

// 引入成长助手Config（AgentConfig），下面可以直接使用这个类型或静态成员。
import com.asterway.common.config.AgentConfig;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入JSON数据ProcessingException（JsonProcessingException），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.core.JsonProcessingException;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;

// 引入提前Destroy（PreDestroy），下面可以直接使用这个类型或静态成员。
import jakarta.annotation.PreDestroy;

// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;
// 引入输入分段回复流（InputStream），下面可以直接使用这个类型或静态成员。
import java.io.InputStream;
// 引入输出分段回复流（OutputStream），下面可以直接使用这个类型或静态成员。
import java.io.OutputStream;
// 引入地址Encoder（URLEncoder），下面可以直接使用这个类型或静态成员。
import java.net.URLEncoder;
// 引入HTTP请求客户端（HttpClient），下面可以直接使用这个类型或静态成员。
import java.net.http.HttpClient;
// 引入HTTP请求请求（HttpRequest），下面可以直接使用这个类型或静态成员。
import java.net.http.HttpRequest;
// 引入HTTP请求响应（HttpResponse），下面可以直接使用这个类型或静态成员。
import java.net.http.HttpResponse;
// 引入HTTP请求超时Exception（HttpTimeoutException），下面可以直接使用这个类型或静态成员。
import java.net.http.HttpTimeoutException;
// 引入StandardCharsets，下面可以直接使用这个类型或静态成员。
import java.nio.charset.StandardCharsets;
// 引入Linked摘要值对应表（LinkedHashMap），下面可以直接使用这个类型或静态成员。
import java.util.LinkedHashMap;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
import java.util.List;
// 引入Executors，下面可以直接使用这个类型或静态成员。
import java.util.concurrent.Executors;
// 引入定时执行Executor业务服务（ScheduledExecutorService），下面可以直接使用这个类型或静态成员。
import java.util.concurrent.ScheduledExecutorService;
// 引入时间Unit（TimeUnit），下面可以直接使用这个类型或静态成员。
import java.util.concurrent.TimeUnit;
// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import java.util.regex.Pattern;

/* 固定内部端点的公共 HTTP 客户端；不记录提问、认证头、密钥或上游错误原文。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 AgentClient；提供成长助手客户端相关的共享实现或应用配置。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentClient {

    // 准备最大JSON数据BYTES（MAX_JSON_BYTES），类型为 int；初始内容是固定数值 262144。
    private static final int MAX_JSON_BYTES = 262_144;
    // 准备对话路径（CONVERSATION_PATH），类型为 Pattern；初始内容是调用 Pattern.compile 完成compile处理。
    private static final Pattern CONVERSATION_PATH =
            // 继续传入或组合调用 Pattern.compile 完成compile处理，完成上面尚未结束的调用或声明。
            Pattern.compile(
                    // 继续传入或组合固定文字 '/internal/agent/conversations(?:/\\\\d+)?(?:/(?:messages(?:/stream)?|close))?'，完成上面尚未结束的调用或声明。
                    "/internal/agent/conversations(?:/\\d+)?(?:/(?:messages(?:/stream)?|close))?"
                            // 继续传入或组合按表达式计算得到的值：+ "(?:/turns/\\d+(?:/cancel)?)?")，完成上面尚未结束的调用或声明。
                            + "(?:/turns/\\d+(?:/cancel)?)?");
    // 准备工作区路径（WORKSPACE_PATH），类型为 Pattern；初始内容是调用 Pattern.compile 完成compile处理。
    // 只代理已经实现的业务路径，不接受任意内部地址。
    private static final Pattern WORKSPACE_PATH = Pattern.compile(
            "/internal/agent/(?:memories(?:/[1-9][0-9]*(?:/(?:confirm|reject|archive))?|/candidates)?"
                    + "|knowledge(?:/capabilities|/bases(?:/[1-9][0-9]*)?|/move|/tasks|/[1-9][0-9]*(?:/(?:chunks|revisions|content|retry)|/revisions/[1-9][0-9]*/chunks/[1-9][0-9]*)?)?"
                    + "|conversations/[1-9][0-9]*/knowledge-bases"
                    + "|planning/proposals(?:/[1-9][0-9]*(?:/(?:revisions|confirm|execute|cancel|progress))?)?"
                    + "|reviews(?:/[1-9][0-9]*(?:/(?:retry|proposals))?|/latest/conversation/[1-9][0-9]*)?"
                    + "|runs/(?:calls|tasks(?:/[1-9][0-9]*/retry)?))");
    private final AgentConfig config;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;
    // HTTP请求（http），使用 HttpClient 类型保存。
    private final HttpClient http;
    // 准备deadlines，类型为 ScheduledExecutorService；初始内容是调用 Executors.newSingleThreadScheduledExecutor 完成新SingleThread定时执行Executor处理。
    private final ScheduledExecutorService deadlines =
            // 继续传入或组合调用 Executors.newSingleThreadScheduledExecutor 完成新SingleThread定时执行Executor处理，完成上面尚未结束的调用或声明。
            Executors.newSingleThreadScheduledExecutor(
                    // r，使用 null 类型保存。
                    // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                    r -> {
                        // 准备thread，类型为 Thread；初始内容是新建 Thread 实例。
                        Thread thread = new Thread(r, "agent-stream-deadline");
                        // 设置 Daemon 的值。
                        thread.setDaemon(true);
                        // 把thread交还给调用方，本方法到此结束。
                        return thread;
                    });

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // config，使用 AgentConfig 类型保存。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public AgentClient(AgentConfig config, ObjectMapper json) {
        // 把config保存到 this.config。
        this.config = config;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
        // 把调用 HttpClient.newBuilder 完成新Builder处理保存到 this.http。
        this.http =
                // 调用 HttpClient.newBuilder 完成新Builder处理。
                HttpClient.newBuilder()
                        // 本地 Uvicorn 使用 HTTP/1.1，关闭 h2c 升级协商。
                        // 继续对上一步结果执行调用 version 完成版本号处理。
                        .version(HttpClient.Version.HTTP_1_1)
                        // 继续对上一步结果执行调用 connectTimeout(config.connectTimeout 完成连接超时处理。
                        .connectTimeout(config.connectTimeout())
                        // 继续对上一步结果执行调用 followRedirects 完成followRedirects处理。
                        .followRedirects(HttpClient.Redirect.NEVER)
                        // 继续对上一步结果执行调用 build 完成构造处理。
                        .build();
    }

    // 对话（chat）方法：调用 chat 完成对话处理；参数负责传入本次处理所需的数据。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    // 上下文（context），使用 AgentRequestContext 类型保存。
    public JsonNode chat(String message, AgentRequestContext context) {
        // 准备请求（request），类型为 HttpRequest；初始内容是调用 request 完成请求处理。
        HttpRequest request = request("/internal/agent/chat", message, context, false);
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备响应（response），类型为 HttpResponse<InputStream>；初始内容是调用 http.send 完成发送处理。
            HttpResponse<InputStream> response =
                    // 调用 http.send 完成发送处理。
                    http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // 准备请求正文（body），类型为 InputStream；初始内容是调用 response.body 完成请求正文处理。
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try (InputStream body = response.body()) {
                // 准备截止时间（deadline），类型为 null；初始内容是调用 deadlines.schedule 完成安排处理。
                var deadline =
                        // 继续传入或组合调用 deadlines.schedule 完成安排处理，完成上面尚未结束的调用或声明。
                        deadlines.schedule(
                                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                                () -> closeQuietly(body),
                                // 调用 config.requestTimeout 完成请求超时处理。
                                config.requestTimeout().toMillis(),
                                // 继续传入或组合表达式 TimeUnit.MILLISECONDS) 的结果，完成上面尚未结束的调用或声明。
                                TimeUnit.MILLISECONDS);
                // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                try {
                    // 准备结果（result），类型为 JsonNode；初始内容是读取 JSON 编码和解析库（Json）。
                    JsonNode result = readJson(body);
                    // 检查条件 response.statusCode() != 200 是否成立；成立时执行下面的分支。
                    if (response.statusCode() != 200) {
                        // 中止正常处理并抛出异常：调用 upstreamFailure(response.statusCode 完成状态编码处理；由上层按失败处理。
                        throw upstreamFailure(response.statusCode(), result);
                    }
                    // 检查输入或集合是否为空：!result.path("reply").isTextual() || result.path("reply").asText().isBlank() || !result.path("model").isTextual() || !result.path("finish_reason"…；成立时执行下面的分支。
                    if (!result.path("reply").isTextual()
                            // 继续补充同一个判断或备用结果：输入或集合是否为空：|| result.path("reply").asText().isBlank()。
                            || result.path("reply").asText().isBlank()
                            // 继续补充同一个判断或备用结果：条件 || !result.path("model").isTextual() 是否成立。
                            || !result.path("model").isTextual()
                            // 继续补充同一个判断或备用结果：条件 || !result.path("finish_reason").isTextual()) { 是否成立。
                            || !result.path("finish_reason").isTextual()) {
                        // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
                        throw unavailable("AGENT_INVALID_RESULT", "对话服务返回结果不完整");
                    }
                    // 把结果（result）交还给调用方，本方法到此结束。
                    return result;
                // 无论成功还是失败，都执行下面的资源与状态清理。
                } finally {
                    // 调用 deadline.cancel 完成取消处理。
                    deadline.cancel(false);
                }
            }
        // 错误（error），使用 HttpTimeoutException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (HttpTimeoutException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
        // 错误（error），使用 InterruptedException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (InterruptedException error) {
            // 调用 Thread.currentThread 完成当前Thread处理。
            Thread.currentThread().interrupt();
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_CANCELLED", "对话请求已取消");
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务");
        }
    }

    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    // 上下文（context），使用 AgentRequestContext 类型保存。
    // 输出（output），使用 OutputStream 类型保存。
    public void stream(String message, AgentRequestContext context, OutputStream output)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 调用 streamRequest("/internal/agent/chat/stream", Map.of 完成of处理。
        streamRequest("/internal/agent/chat/stream", Map.of("message", message), context, output);
    }

    // 分段回复流消息（streamMessage）方法：调用 streamMessage 完成分段回复流消息处理；参数负责传入本次处理所需的数据。
    public void streamMessage(
            // 对话编号（conversationId），使用 long 类型保存。
            long conversationId,
            // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
            String message,
            // 请求编号（requestId），使用 String 类型保存；可为 null，表示尚未提供。
            String requestId,
            // 上下文（context），使用 AgentRequestContext 类型保存。
            List<Long> knowledgeBaseIds, AgentRequestContext context,
            // 输出（output），使用 OutputStream 类型保存。
            OutputStream output)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 准备请求正文（body），类型为 Map<String, Object>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, Object> body = new LinkedHashMap<>();
        // 保存指定键对应的值。
        body.put("message", message);
        // 保存指定键对应的值。
        body.put("requestId", requestId);
        if (knowledgeBaseIds != null) body.put("knowledgeBaseIds", knowledgeBaseIds);
        // 调用 streamRequest 完成分段回复流请求处理。
        streamRequest(
                // 继续传入或组合固定文字 '/internal/agent/conversations/" + conversationId + "/messages/stream'，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/messages/stream",
                // 继续传入或组合请求正文（body），完成上面尚未结束的调用或声明。
                body,
                // 继续传入或组合上下文（context），完成上面尚未结束的调用或声明。
                context,
                // 继续传入或组合表达式 output) 的结果，完成上面尚未结束的调用或声明。
                output);
    }

    // 分段回复流请求（streamRequest）方法：调用 streamRequest 完成分段回复流请求处理；参数负责传入本次处理所需的数据。
    private void streamRequest(
            // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
            // 提交数据（payload），使用 Object 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            // 输出（output），使用 OutputStream 类型保存。
            String path, Object payload, AgentRequestContext context, OutputStream output)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 请求构造在异步任务内完成，浏览器取消后 InputStream 随 try 立即关闭。
        // 准备请求（request），类型为 HttpRequest；初始内容是调用 streamRequestBuilder 完成分段回复流请求Builder处理。
        HttpRequest request = streamRequestBuilder(path, payload, context);
        // 响应（response），使用 HttpResponse<InputStream> 类型保存。
        HttpResponse<InputStream> response;
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把调用 http.send 完成发送处理保存到 response。
            response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        // 错误（error），使用 InterruptedException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (InterruptedException error) {
            // 调用 Thread.currentThread 完成当前Thread处理。
            Thread.currentThread().interrupt();
            // 调用 writeError(output, unavailable("AGENT_CANCELLED", "U+5bf9U+8bddU+8bf7U+6c42U+5df2U+53d6U+6d88"), context.traceId 完成本次请求的追踪编号处理。
            writeError(output, unavailable("AGENT_CANCELLED", "对话请求已取消"), context.traceId());
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 调用 writeError(output, unavailable("AGENT_UNAVAILABLE", "U+6682U+65f6U+65e0U+6cd5U+8fdeU+63a5U+5bf9U+8bddU+670dU+52a1"), context.traceId 完成本次请求的追踪编号处理。
            writeError(output, unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务"), context.traceId());
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备请求正文（body），类型为 InputStream；初始内容是调用 response.body 完成请求正文处理。
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try (InputStream body = response.body()) {
            // 准备截止时间（deadline），类型为 null；初始内容是调用 deadlines.schedule 完成安排处理。
            var deadline =
                    // 继续传入或组合调用 deadlines.schedule 完成安排处理，完成上面尚未结束的调用或声明。
                    deadlines.schedule(
                            // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                            () -> closeQuietly(body),
                            // 调用 config.requestTimeout 完成请求超时处理。
                            config.requestTimeout().toMillis(),
                            // 继续传入或组合表达式 TimeUnit.MILLISECONDS) 的结果，完成上面尚未结束的调用或声明。
                            TimeUnit.MILLISECONDS);
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 检查条件 response.statusCode() != 200 是否成立；成立时执行下面的分支。
                if (response.statusCode() != 200) {
                    // 结果（result），使用 JsonNode 类型保存。
                    JsonNode result;
                    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                    try {
                        // 把读取 JSON 编码和解析库（Json）保存到 result。
                        result = readJson(body);
                    // 错误（error），使用 IOException 类型保存。
                    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
                    } catch (IOException error) {
                        // 调用 writeError(output, unavailable("AGENT_INVALID_RESULT", "U+5bf9U+8bddU+670dU+52a1U+8fd4U+56deU+4e86U+65e0U+6cd5U+8bc6U+522bU+7684U+7ed3U+679c"), context.traceId 完成本次请求的追踪编号处理。
                        writeError(
                                // 继续传入或组合输出（output），完成上面尚未结束的调用或声明。
                                output,
                                // 调用 unavailable 完成unavailable处理。
                                unavailable("AGENT_INVALID_RESULT", "对话服务返回了无法识别的结果"),
                                // 调用 context.traceId 完成本次请求的追踪编号处理。
                                context.traceId());
                        // 把返回（return）交还给调用方，本方法到此结束。
                        return;
                    }
                    // 调用 writeError(output, upstreamFailure(response.statusCode 完成状态编码处理。
                    writeError(
                            // 继续传入或组合输出（output），完成上面尚未结束的调用或声明。
                            output,
                            // 调用 upstreamFailure(response.statusCode 完成状态编码处理。
                            upstreamFailure(response.statusCode(), result),
                            // 调用 context.traceId 完成本次请求的追踪编号处理。
                            context.traceId());
                    // 把返回（return）交还给调用方，本方法到此结束。
                    return;
                }
                // 检查条件 !response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream") 是否成立；成立时执行下面的分支。
                if (!response.headers()
                        // 继续对上一步结果执行调用 firstValue 完成首次值处理。
                        .firstValue("Content-Type")
                        // 继续对上一步结果执行调用 orElse 完成orElse处理。
                        .orElse("")
                        // 继续对上一步结果执行判断文字是否具有指定前缀。
                        .startsWith("text/event-stream")) {
                    // 调用 writeError(output, unavailable("AGENT_INVALID_RESULT", "U+5bf9U+8bddU+670dU+52a1U+672aU+8fd4U+56deU+6d41U+5f0fU+7ed3U+679c"), context.traceId 完成本次请求的追踪编号处理。
                    writeError(
                            // 继续传入或组合输出（output），完成上面尚未结束的调用或声明。
                            output,
                            // 调用 unavailable 完成unavailable处理。
                            unavailable("AGENT_INVALID_RESULT", "对话服务未返回流式结果"),
                            // 调用 context.traceId 完成本次请求的追踪编号处理。
                            context.traceId());
                    // 把返回（return）交还给调用方，本方法到此结束。
                    return;
                }
                // 准备缓冲区（buffer），类型为 byte[]；初始内容是新建 byte8192（byte[8192]） 实例。
                byte[] buffer = new byte[8192];
                // 逐次执行循环：while (true)；每轮处理一条数据或推进一次检查。
                while (true) {
                    // 长度（length），使用 int 类型保存。
                    int length;
                    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                    try {
                        // 把调用 body.read 完成读取处理保存到 length。
                        length = body.read(buffer);
                    // 错误（error），使用 IOException 类型保存。
                    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
                    } catch (IOException error) {
                        // 调用 writeError(output, unavailable("AGENT_STREAM_INTERRUPTED", "U+56deU+590dU+8fdeU+63a5U+4e2dU+65adU+ff0cU+8bf7U+91cdU+8bd5"), context.traceId 完成本次请求的追踪编号处理。
                        writeError(
                                // 继续传入或组合输出（output），完成上面尚未结束的调用或声明。
                                output,
                                // 调用 unavailable 完成unavailable处理。
                                unavailable("AGENT_STREAM_INTERRUPTED", "回复连接中断，请重试"),
                                // 调用 context.traceId 完成本次请求的追踪编号处理。
                                context.traceId());
                        // 把返回（return）交还给调用方，本方法到此结束。
                        return;
                    }
                    // 检查条件 length == -1 是否成立；成立时执行下面的分支。
                    if (length == -1) {
                        // 把返回（return）交还给调用方，本方法到此结束。
                        return;
                    }
                    // 不缓存整个回复；客户端写出失败向外传播，关闭上游且不重复发送。
                    // 把内容写入当前输出或响应。
                    output.write(buffer, 0, length);
                    // 把待写入的数据库操作发送到数据库，但此时还没有提交事务。
                    output.flush();
                }
            // 错误（error），使用 BusinessException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (BusinessException error) {
                // 调用 writeError(output, error, context.traceId 完成本次请求的追踪编号处理。
                writeError(output, error, context.traceId());
            // 无论成功还是失败，都执行下面的资源与状态清理。
            } finally {
                // 调用 deadline.cancel 完成取消处理。
                deadline.cancel(false);
            }
        }
    }

    // requireConfigured方法：要求满足 已配置（Configured）；参数负责传入本次处理所需的数据。
    public void requireConfigured() {
        // 检查输入或集合是否为空：config.internalKey().isEmpty()；成立时执行下面的分支。
        if (config.internalKey().isEmpty()) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(503, "AGENT_NOT_CONFIGURED", "成长助手内部连接尚未配置");
        }
    }

    // 对话当前页（conversationPage）方法：调用 conversationPage 完成对话当前页处理；参数负责传入本次处理所需的数据。
    public JsonNode conversationPage(
            // 查询条件（query），使用 Map<String, String> 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            Map<String, String> query, AgentRequestContext context) {
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest("GET", "/internal/agent/conversations", query, null, context);
    }

    // 对话（conversation）方法：调用 conversation 完成对话处理；参数负责传入本次处理所需的数据。
    // 对话编号（conversationId），使用 long 类型保存。
    // 上下文（context），使用 AgentRequestContext 类型保存。
    public JsonNode conversation(long conversationId, AgentRequestContext context) {
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合按表达式计算得到的值："GET", "/internal/agent/conversations/" + conversationId, null, null, context)，完成上面尚未结束的调用或声明。
                "GET", "/internal/agent/conversations/" + conversationId, null, null, context);
    }

    // 创建对话（createConversation）方法：创建 对话（Conversation）；参数负责传入本次处理所需的数据。
    // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
    // 上下文（context），使用 AgentRequestContext 类型保存。
    public JsonNode createConversation(String title, List<Long> knowledgeBaseIds, AgentRequestContext context) {
        // 准备请求正文（body），类型为 Map<String, Object>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, Object> body = new LinkedHashMap<>();
        // 检查输入或集合是否为空：title != null && !title.isBlank()；成立时执行下面的分支。
        if (title != null && !title.isBlank()) {
            // 保存指定键对应的值。
            body.put("title", title.trim());
        }
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        body.put("knowledgeBaseIds", knowledgeBaseIds == null ? List.of() : knowledgeBaseIds);
        return jsonRequest(
                // 继续传入或组合按表达式计算得到的值："POST", "/internal/agent/conversations", null, body, context)，完成上面尚未结束的调用或声明。
                "POST", "/internal/agent/conversations", null, body, context);
    }

    // 重命名对话（renameConversation）方法：调用 renameConversation 完成重命名对话处理；参数负责传入本次处理所需的数据。
    public JsonNode renameConversation(
            // 对话编号（conversationId），使用 long 类型保存。
            // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
            // 版本号（version），使用 long 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            long conversationId, String title, long version, AgentRequestContext context) {
        // 准备请求正文（body），类型为 Map<String, Object>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, Object> body = new LinkedHashMap<>();
        // 保存指定键对应的值。
        body.put("title", title);
        // 保存指定键对应的值。
        body.put("version", version);
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 PATCH（PATCH），完成上面尚未结束的调用或声明。
                "PATCH",
                // 继续传入或组合按表达式计算得到的值："/internal/agent/conversations/" + conversationId，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId,
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合请求正文（body），完成上面尚未结束的调用或声明。
                body,
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 关闭对话（closeConversation）方法：调用 closeConversation 完成关闭对话处理；参数负责传入本次处理所需的数据。
    public JsonNode closeConversation(
            // 对话编号（conversationId），使用 long 类型保存。
            // 版本号（version），使用 long 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            long conversationId, long version, AgentRequestContext context) {
        // 把调用 jsonRequest("POST", "/internal/agent/conversations/" + conversationId + "/close", null, Map.of 完成of处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 POST（POST），完成上面尚未结束的调用或声明。
                "POST",
                // 继续传入或组合固定文字 '/internal/agent/conversations/" + conversationId + "/close'，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/close",
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("version", version),
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 删除对话（deleteConversation）方法：删除 对话（Conversation）；参数负责传入本次处理所需的数据。
    public JsonNode deleteConversation(
            // 对话编号（conversationId），使用 long 类型保存。
            // 版本号（version），使用 long 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            long conversationId, long version, AgentRequestContext context) {
        // 把调用 jsonRequest("DELETE", "/internal/agent/conversations/" + conversationId, Map.of 完成of处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 DELETE（删除），完成上面尚未结束的调用或声明。
                "DELETE",
                // 继续传入或组合按表达式计算得到的值："/internal/agent/conversations/" + conversationId，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId,
                // 调用 Map.of 完成of处理。
                Map.of("version", Long.toString(version)),
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 对话消息列表（conversationMessages）方法：调用 conversationMessages 完成对话消息列表处理；参数负责传入本次处理所需的数据。
    public JsonNode conversationMessages(
            // 对话编号（conversationId），使用 long 类型保存。
            long conversationId,
            // 变化前消息No（beforeMessageNo），使用 Long 类型保存；可为 null，表示尚未提供。
            Long beforeMessageNo,
            // 每页条数（size），使用 int 类型保存。
            int size,
            // 上下文（context），使用 AgentRequestContext 类型保存。
            AgentRequestContext context) {
        // 准备查询条件（query），类型为 Map<String, String>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, String> query = new LinkedHashMap<>();
        // 检查条件 beforeMessageNo != null 是否成立；成立时执行下面的分支。
        if (beforeMessageNo != null) {
            // 保存指定键对应的值。
            query.put("beforeMessageNo", Long.toString(beforeMessageNo));
        }
        // 保存指定键对应的值。
        query.put("size", Integer.toString(size));
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 GET（读取），完成上面尚未结束的调用或声明。
                "GET",
                // 继续传入或组合固定文字 '/internal/agent/conversations/" + conversationId + "/messages'，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/messages",
                // 继续传入或组合查询条件（query），完成上面尚未结束的调用或声明。
                query,
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 对话本轮对话（conversationTurn）方法：调用 conversationTurn 完成对话本轮对话处理；参数负责传入本次处理所需的数据。
    public JsonNode conversationTurn(
            // 对话编号（conversationId），使用 long 类型保存。
            // 本轮对话编号（turnId），使用 long 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            long conversationId, long turnId, AgentRequestContext context) {
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 GET（读取），完成上面尚未结束的调用或声明。
                "GET",
                // 继续传入或组合按表达式计算得到的值："/internal/agent/conversations/" + conversationId + "/turns/" + turnId，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/turns/" + turnId,
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 取消本轮对话（cancelTurn）方法：取消 本轮对话（Turn）；参数负责传入本次处理所需的数据。
    public JsonNode cancelTurn(
            // 对话编号（conversationId），使用 long 类型保存。
            // 本轮对话编号（turnId），使用 long 类型保存。
            // 版本号（version），使用 long 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            long conversationId, long turnId, long version, AgentRequestContext context) {
        // 把调用 jsonRequest("POST", "/internal/agent/conversations/" + conversationId + "/turns/" + turnId + "/cancel", null, Map.of 完成of处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 POST（POST），完成上面尚未结束的调用或声明。
                "POST",
                // 继续传入或组合固定文字 '/internal/agent/conversations/" + conversationId + "/turns/" + turnId + "/cancel'，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/turns/" + turnId + "/cancel",
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("version", version),
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 发送消息（sendMessage）方法：调用 sendMessage 完成发送消息处理；参数负责传入本次处理所需的数据。
    public JsonNode sendMessage(
            // 对话编号（conversationId），使用 long 类型保存。
            long conversationId,
            // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
            String message,
            // 请求编号（requestId），使用 String 类型保存；可为 null，表示尚未提供。
            String requestId,
            // 上下文（context），使用 AgentRequestContext 类型保存。
            List<Long> knowledgeBaseIds, AgentRequestContext context) {
        // 准备请求正文（body），类型为 Map<String, Object>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<String, Object> body = new LinkedHashMap<>();
        // 保存指定键对应的值。
        body.put("message", message);
        // 保存指定键对应的值。
        body.put("requestId", requestId);
        if (knowledgeBaseIds != null) body.put("knowledgeBaseIds", knowledgeBaseIds);
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(
                // 继续传入或组合固定状态或编码 POST（POST），完成上面尚未结束的调用或声明。
                "POST",
                // 继续传入或组合固定文字 '/internal/agent/conversations/" + conversationId + "/messages'，完成上面尚未结束的调用或声明。
                "/internal/agent/conversations/" + conversationId + "/messages",
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 继续传入或组合请求正文（body），完成上面尚未结束的调用或声明。
                body,
                // 继续传入或组合表达式 context) 的结果，完成上面尚未结束的调用或声明。
                context);
    }

    // 工作区请求（workspaceRequest）方法：调用 workspaceRequest 完成工作区请求处理；参数负责传入本次处理所需的数据。
    // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
    // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public JsonNode workspaceRequest(String method, String path, Map<String, String> query,
            // 请求正文（body），使用 JsonNode 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            JsonNode body, AgentRequestContext context) {
        // 检查条件 !WORKSPACE_PATH.matcher(path).matches() 是否成立；成立时执行下面的分支。
        if (!WORKSPACE_PATH.matcher(path).matches()) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(400, "AGENT_REQUEST_INVALID", "不支持的成长助手资源");
        }
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(method, path, query, body, context);
    }

    // 工作区上传（workspaceUpload）方法：调用 workspaceUpload 完成工作区上传处理；参数负责传入本次处理所需的数据。
    // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    // mime，使用 String 类型保存；可为 null，表示尚未提供。
    public JsonNode workspaceUpload(String path, Map<String, String> query, String mime,
            // 内容（content），使用 byte[] 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            byte[] content, AgentRequestContext context) {
        // 检查条件 !path.matches("/internal/agent/knowledge(?:/[1-9][0-9]* /content)?") 是否成立；成立时执行下面的分支。
        if (!path.matches("/internal/agent/knowledge(?:/[1-9][0-9]*/content)?")) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(400, "AGENT_REQUEST_INVALID", "不支持的文档资源");
        }
        // 把根据前面的条件，从两个候选结果中选择一个：jsonRequest(path.endsWith("/content") ? "PUT" : "POST", path, query, content, context, mime)交还给调用方，本方法到此结束。
        return jsonRequest(path.endsWith("/content") ? "PUT" : "POST", path, query,
                // 继续传入或组合表达式 content, context, mime) 的结果，完成上面尚未结束的调用或声明。
                content, context, mime);
    }

    // JSON数据请求（jsonRequest）方法：调用 jsonRequest 完成JSON数据请求处理；参数负责传入本次处理所需的数据。
    private JsonNode jsonRequest(
            // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
            String method,
            // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
            String path,
            // 查询条件（query），使用 Map<String, String> 类型保存。
            Map<String, String> query,
            // 请求正文（body），使用 Object 类型保存。
            Object body,
            // 上下文（context），使用 AgentRequestContext 类型保存。
            AgentRequestContext context) {
        // 把调用 jsonRequest 完成JSON数据请求处理交还给调用方，本方法到此结束。
        return jsonRequest(method, path, query, body, context, null);
    }

    // JSON数据请求（jsonRequest）方法：调用 jsonRequest 完成JSON数据请求处理；参数负责传入本次处理所需的数据。
    // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
    // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    private JsonNode jsonRequest(String method, String path, Map<String, String> query,
            // 请求正文（body），使用 Object 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            // 文件Mime（fileMime），使用 String 类型保存；可为 null，表示尚未提供。
            Object body, AgentRequestContext context, String fileMime) {
        // 检查条件 !CONVERSATION_PATH.matcher(path).matches() && !WORKSPACE_PATH.matcher(path).matches() 是否成立；成立时执行下面的分支。
        if (!CONVERSATION_PATH.matcher(path).matches() && !WORKSPACE_PATH.matcher(path).matches()) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        // 要求满足 已配置（Configured）。
        requireConfigured();
        // builder，使用 HttpRequest.Builder 类型保存。
        HttpRequest.Builder builder;
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把调用 HttpRequest.newBuilder 完成新Builder处理保存到 builder。
            builder =
                    // 调用 HttpRequest.newBuilder 完成新Builder处理。
                    HttpRequest.newBuilder(config.endpoint(path + queryString(query)))
                            // 继续对上一步结果执行调用 timeout(config.requestTimeout 完成请求超时处理。
                            .timeout(config.requestTimeout())
                            // 继续对上一步结果执行调用 header 完成请求头处理。
                            .header("Accept", "application/json")
                            // 继续对上一步结果执行调用 header("Authorization", context.authorization 完成授权处理。
                            .header("Authorization", context.authorization())
                            // 继续对上一步结果执行调用 header("X-Trace-ID", context.traceId 完成本次请求的追踪编号处理。
                            .header("X-Trace-ID", context.traceId())
                            // 继续对上一步结果执行调用 header("X-Asterway-Agent-Key", config.internalKey 完成内部键处理。
                            .header("X-Asterway-Agent-Key", config.internalKey());
            // 检查body == null，即对应内容是否不存在；成立时执行下面的分支。
            if (body == null) {
                // 调用 builder.method 完成方法处理。
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            // bytes，使用 byte[] 类型保存。
            // 检查条件 body instanceof byte[] bytes 是否成立；成立时执行下面的分支。
            } else if (body instanceof byte[] bytes) {
                // 调用 builder.header 完成请求头处理。
                builder.header("Content-Type", "application/octet-stream")
                        // 继续对上一步结果执行根据前面的条件，从两个候选结果中选择一个：header("X-File-Type", fileMime == null ? "application/octet-stream" : fileMime)。
                        .header("X-File-Type", fileMime == null ? "application/octet-stream" : fileMime)
                        // 继续对上一步结果执行调用 method(method, HttpRequest.BodyPublishers.ofByteArray 完成ofByteArray处理。
                        .method(method, HttpRequest.BodyPublishers.ofByteArray(bytes));
            // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
            } else {
                // 调用 builder.header 完成请求头处理。
                builder.header("Content-Type", "application/json")
                        // 继续对上一步结果执行调用 method 完成方法处理。
                        .method(
                                // 继续传入或组合方法（method），完成上面尚未结束的调用或声明。
                                method,
                                // 继续传入或组合调用 HttpRequest.BodyPublishers.ofByteArray 完成ofByteArray处理，完成上面尚未结束的调用或声明。
                                HttpRequest.BodyPublishers.ofByteArray(
                                        // 写入 值作为Bytes（ValueAsBytes）。
                                        json.writeValueAsBytes(body)));
            }
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        // 准备请求（request），类型为 HttpRequest；初始内容是调用 builder.build 完成构造处理。
        HttpRequest request = builder.build();
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备响应（response），类型为 HttpResponse<InputStream>；初始内容是调用 http.send 完成发送处理。
            HttpResponse<InputStream> response =
                    // 调用 http.send 完成发送处理。
                    http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            // 准备分段回复流（stream），类型为 InputStream；初始内容是调用 response.body 完成请求正文处理。
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try (InputStream stream = response.body()) {
                // 准备截止时间（deadline），类型为 null；初始内容是调用 deadlines.schedule 完成安排处理。
                var deadline =
                        // 继续传入或组合调用 deadlines.schedule 完成安排处理，完成上面尚未结束的调用或声明。
                        deadlines.schedule(
                                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                                () -> closeQuietly(stream),
                                // 调用 config.requestTimeout 完成请求超时处理。
                                config.requestTimeout().toMillis(),
                                // 继续传入或组合表达式 TimeUnit.MILLISECONDS) 的结果，完成上面尚未结束的调用或声明。
                                TimeUnit.MILLISECONDS);
                // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                try {
                    // 准备结果（result），类型为 JsonNode；初始内容是读取 JSON 编码和解析库（Json）。
                    JsonNode result = readJson(stream);
                    // 检查条件 response.statusCode() < 200 || response.statusCode() >= 300 是否成立；成立时执行下面的分支。
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        // 中止正常处理并抛出异常：调用 upstreamFailure(response.statusCode 完成状态编码处理；由上层按失败处理。
                        throw upstreamFailure(response.statusCode(), result);
                    }
                    // 把结果（result）交还给调用方，本方法到此结束。
                    return result;
                // 无论成功还是失败，都执行下面的资源与状态清理。
                } finally {
                    // 调用 deadline.cancel 完成取消处理。
                    deadline.cancel(false);
                }
            }
        // 错误（error），使用 HttpTimeoutException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (HttpTimeoutException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
        // 错误（error），使用 InterruptedException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (InterruptedException error) {
            // 调用 Thread.currentThread 完成当前Thread处理。
            Thread.currentThread().interrupt();
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_CANCELLED", "对话请求已取消");
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务");
        }
    }

    // 查询条件文字（queryString）方法：调用 queryString 完成查询条件文字处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    private String queryString(Map<String, String> query) {
        // 检查query == null || query.isEmpty()，即对应内容是否不存在；成立时执行下面的分支。
        if (query == null || query.isEmpty()) {
            // 把固定文字 ''交还给调用方，本方法到此结束。
            return "";
        }
        // 准备结果（result），类型为 StringBuilder；初始内容是新建 文字Builder（StringBuilder） 实例。
        StringBuilder result = new StringBuilder();
        // entry，使用 Map.Entry<String, String> 类型保存。
        // 逐次执行循环：for (Map.Entry<String, String> entry : query.entrySet())；每轮处理一条数据或推进一次检查。
        for (Map.Entry<String, String> entry : query.entrySet()) {
            // 检查entry.getValue() == null，即对应内容是否不存在；成立时执行下面的分支。
            if (entry.getValue() == null) {
                // 继续传入或组合continue，完成上面尚未结束的调用或声明。
                continue;
            }
            // 检查条件 result.length() > 0 是否成立；成立时执行下面的分支。
            if (result.length() > 0) {
                // 把新条目放到列表末尾。
                result.append('&');
            }
            // 把新条目放到列表末尾。
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    // 继续对上一步结果执行把新条目放到列表末尾。
                    .append('=')
                    // 继续对上一步结果执行把原始密码编码成不可直接还原的密码摘要。
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        // 把根据前面的条件，从两个候选结果中选择一个：result.length() == 0 ? "" : "?" + result交还给调用方，本方法到此结束。
        return result.length() == 0 ? "" : "?" + result;
    }

    // 分段回复流请求Builder（streamRequestBuilder）方法：调用 streamRequestBuilder 完成分段回复流请求Builder处理；参数负责传入本次处理所需的数据。
    private HttpRequest streamRequestBuilder(
            // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
            // 请求正文（body），使用 Object 类型保存。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            String path, Object body, AgentRequestContext context) {
        // 要求满足 已配置（Configured）。
        requireConfigured();
        // 检查条件 !CONVERSATION_PATH.matcher(path).matches() && !"/internal/agent/chat/stream".equals(path) 是否成立；成立时执行下面的分支。
        if (!CONVERSATION_PATH.matcher(path).matches()
                // 继续补充同一个判断或备用结果：条件 && !"/internal/agent/chat/stream".equals(path)) { 是否成立。
                && !"/internal/agent/chat/stream".equals(path)) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把调用 HttpRequest.newBuilder 完成新Builder处理交还给调用方，本方法到此结束。
            return HttpRequest.newBuilder(config.endpoint(path))
                    // 继续对上一步结果执行调用 timeout(config.requestTimeout 完成请求超时处理。
                    .timeout(config.requestTimeout())
                    // 继续对上一步结果执行调用 header 完成请求头处理。
                    .header("Content-Type", "application/json")
                    // 继续对上一步结果执行调用 header 完成请求头处理。
                    .header("Accept", "text/event-stream")
                    // 继续对上一步结果执行调用 header("Authorization", context.authorization 完成授权处理。
                    .header("Authorization", context.authorization())
                    // 继续对上一步结果执行调用 header("X-Trace-ID", context.traceId 完成本次请求的追踪编号处理。
                    .header("X-Trace-ID", context.traceId())
                    // 继续对上一步结果执行调用 header("X-Asterway-Agent-Key", config.internalKey 完成内部键处理。
                    .header("X-Asterway-Agent-Key", config.internalKey())
                    // 继续对上一步结果执行调用 POST 完成POST处理。
                    .POST(
                            // 继续传入或组合调用 HttpRequest.BodyPublishers.ofByteArray 完成ofByteArray处理，完成上面尚未结束的调用或声明。
                            HttpRequest.BodyPublishers.ofByteArray(
                                    // 写入 值作为Bytes（ValueAsBytes）。
                                    json.writeValueAsBytes(body)))
                    // 继续对上一步结果执行调用 build 完成构造处理。
                    .build();
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
    }

    // 请求（request）方法：调用 request 完成请求处理；参数负责传入本次处理所需的数据。
    private HttpRequest request(
            // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
            // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
            // 上下文（context），使用 AgentRequestContext 类型保存。
            // 分段回复流（stream），使用 boolean 类型保存。
            String path, String message, AgentRequestContext context, boolean stream) {
        // 要求满足 已配置（Configured）。
        requireConfigured();
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把根据前面的条件，从两个候选结果中选择一个：HttpRequest.newBuilder(config.endpoint(path)).timeout(config.requestTimeout()).header("Content-Type"…交还给调用方，本方法到此结束。
            return HttpRequest.newBuilder(config.endpoint(path))
                    // 继续对上一步结果执行调用 timeout(config.requestTimeout 完成请求超时处理。
                    .timeout(config.requestTimeout())
                    // 继续对上一步结果执行调用 header 完成请求头处理。
                    .header("Content-Type", "application/json")
                    // 继续对上一步结果执行根据前面的条件，从两个候选结果中选择一个：header("Accept", stream ? "text/event-stream" : "application/json")。
                    .header("Accept", stream ? "text/event-stream" : "application/json")
                    // 继续对上一步结果执行调用 header("Authorization", context.authorization 完成授权处理。
                    .header("Authorization", context.authorization())
                    // 继续对上一步结果执行调用 header("X-Trace-ID", context.traceId 完成本次请求的追踪编号处理。
                    .header("X-Trace-ID", context.traceId())
                    // 继续对上一步结果执行调用 header("X-Asterway-Agent-Key", config.internalKey 完成内部键处理。
                    .header("X-Asterway-Agent-Key", config.internalKey())
                    // 继续对上一步结果执行调用 POST 完成POST处理。
                    .POST(
                            // 继续传入或组合调用 HttpRequest.BodyPublishers.ofByteArray 完成ofByteArray处理，完成上面尚未结束的调用或声明。
                            HttpRequest.BodyPublishers.ofByteArray(
                                    // 写入 值作为Bytes（ValueAsBytes）。
                                    json.writeValueAsBytes(Map.of("message", message))))
                    // 继续对上一步结果执行调用 build 完成构造处理。
                    .build();
        // 错误（error），使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
    }

    // 读取JSON数据（readJson）方法：读取 JSON 编码和解析库（Json）；参数负责传入本次处理所需的数据。
    // 请求正文（body），使用 InputStream 类型保存。
    private JsonNode readJson(InputStream body) throws IOException {
        // 准备bytes，类型为 byte[]；初始内容是读取 NBytes。
        byte[] bytes = body.readNBytes(MAX_JSON_BYTES + 1);
        // 检查条件 bytes.length > MAX_JSON_BYTES 是否成立；成立时执行下面的分支。
        if (bytes.length > MAX_JSON_BYTES) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_RESULT_TOO_LARGE", "对话服务返回内容过多");
        }
        // 结果（result），使用 JsonNode 类型保存。
        JsonNode result;
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把把 JSON 文字解析为可按字段读取的节点保存到 result。
            result = json.readTree(bytes);
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_INVALID_RESULT", "对话服务返回了无法识别的结果");
        }
        // 检查result == null || !result.isObject()，即对应内容是否不存在；成立时执行下面的分支。
        if (result == null || !result.isObject()) {
            // 中止正常处理并抛出异常：调用 unavailable 完成unavailable处理；由上层按失败处理。
            throw unavailable("AGENT_INVALID_RESULT", "对话服务返回结构不正确");
        }
        // 把结果（result）交还给调用方，本方法到此结束。
        return result;
    }

    // upstream失败（upstreamFailure）方法：调用 upstreamFailure 完成upstream失败处理；参数负责传入本次处理所需的数据。
    // 状态（status），使用 int 类型保存。
    // 结果（result），使用 JsonNode 类型保存。
    private BusinessException upstreamFailure(int status, JsonNode result) {
        // 准备编码（code），类型为 String；初始内容是调用 result.path 完成路径处理。
        String code = result.path("code").asText("");
        // 检查条件 code.startsWith("AGENT_INTERNAL_") 是否成立；成立时执行下面的分支。
        if (code.startsWith("AGENT_INTERNAL_")) {
            // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
            return new BusinessException(503, "AGENT_CONNECTION_NOT_CONFIGURED", "成长助手内部连接配置不一致");
        }
        // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
        // 把调用 switch (status) { case 401 -> yield BusinessException.unauthorized 完成unauthorized处理交还给调用方，本方法到此结束。
        return switch (status) {
            // 匹配这一种取值时使用本分支：case 401。
            case 401 -> BusinessException.unauthorized();
            // 匹配这一种取值时使用本分支：case 403。
            case 403 -> BusinessException.forbidden();
            // 匹配这一种取值时使用本分支：case 404。
            case 404 -> BusinessException.notFound();
            // 匹配这一种取值时使用本分支：case 400, 422。
            case 400, 422 -> new BusinessException(status, safeCode(result, "AGENT_REQUEST_INVALID"),
                    // 调用 safeMessage 完成safe消息处理。
                    safeMessage(result, "请求内容不符合要求"));
            // 匹配这一种取值时使用本分支：case 409。
            case 409 ->
                    // 继续传入或组合新建 带业务错误码和可读提示的异常（BusinessException） 实例，完成上面尚未结束的调用或声明。
                    new BusinessException(
                            // 继续传入或组合固定数值 409，完成上面尚未结束的调用或声明。
                            409,
                            // 调用 safeCode 完成safe编码处理。
                            safeCode(result, "STATE_CONFLICT"),
                            // 调用 safeMessage 完成safe消息处理。
                            safeMessage(result, "会话状态已变化，请刷新后重试"),
                            // 根据前面的条件，从两个候选结果中选择一个：result.hasNonNull("data") ? result.get("data") : null)。
                            result.hasNonNull("data") ? result.get("data") : null);
            // 匹配这一种取值时使用本分支：case 429。
            case 429 ->
                    // 继续传入或组合新建 带业务错误码和可读提示的异常（BusinessException） 实例，完成上面尚未结束的调用或声明。
                    new BusinessException(
                            // 继续传入或组合固定数值 429，完成上面尚未结束的调用或声明。
                            429,
                            // 调用 safeCode 完成safe编码处理。
                            safeCode(result, "AGENT_RATE_LIMIT"),
                            // 调用 safeMessage 完成safe消息处理。
                            safeMessage(result, "对话服务繁忙，请稍后重试"),
                            // 根据前面的条件，从两个候选结果中选择一个：result.hasNonNull("data") ? result.get("data") : null)。
                            result.hasNonNull("data") ? result.get("data") : null);
            // 匹配这一种取值时使用本分支：case 500, 502。
            case 500, 502 -> new BusinessException(502, safeCode(result, "AGENT_UPSTREAM_FAILED"),
                    // 调用 safeMessage 完成safe消息处理。
                    safeMessage(result, "成长助手暂时不可用，请稍后重试"));
            // 匹配这一种取值时使用本分支：case 503。
            case 503 -> new BusinessException(503, "AGENT_NOT_READY", "成长助手尚未配置完成");
            // 匹配这一种取值时使用本分支：case 504。
            case 504 -> new BusinessException(504, "AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
            // 匹配这一种取值时使用本分支：default。
            default -> unavailable("AGENT_UPSTREAM_FAILED", "成长助手暂时不可用，请稍后重试");
        };
    }

    // safe编码（safeCode）方法：调用 safeCode 完成safe编码处理；参数负责传入本次处理所需的数据。
    // 结果（result），使用 JsonNode 类型保存。
    // 备用值（fallback），使用 String 类型保存；可为 null，表示尚未提供。
    private String safeCode(JsonNode result, String fallback) {
        // 准备编码（code），类型为 String；初始内容是调用 result.path 完成路径处理。
        String code = result.path("code").asText("");
        // 把根据前面的条件，从两个候选结果中选择一个：code.matches("[A-Z][A-Z0-9_]{0,63}") ? code : fallback交还给调用方，本方法到此结束。
        return code.matches("[A-Z][A-Z0-9_]{0,63}") ? code : fallback;
    }

    // safe消息（safeMessage）方法：调用 safeMessage 完成safe消息处理；参数负责传入本次处理所需的数据。
    // 结果（result），使用 JsonNode 类型保存。
    // 备用值（fallback），使用 String 类型保存；可为 null，表示尚未提供。
    private String safeMessage(JsonNode result, String fallback) {
        // 准备消息（message），类型为 String；初始内容是调用 result.path 完成路径处理。
        String message = result.path("message").asText("");
        // 把根据前面的条件，从两个候选结果中选择一个：message.isBlank() || message.length() > 200 ? fallback : message交还给调用方，本方法到此结束。
        return message.isBlank() || message.length() > 200 ? fallback : message;
    }

    // unavailable方法：调用 unavailable 完成unavailable处理；参数负责传入本次处理所需的数据。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
    private BusinessException unavailable(String code, String message) {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(502, code, message);
    }

    // 写入错误（writeError）方法：写入 错误（Error）；参数负责传入本次处理所需的数据。
    // 输出（output），使用 OutputStream 类型保存。
    // 错误（error），使用 BusinessException 类型保存。
    // 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
    private void writeError(OutputStream output, BusinessException error, String traceId)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws IOException {
        // 准备frame，类型为 String；初始内容是固定文字 'event: error\\ndata: " + json.writeValueAsString(Map.of("code", error.getCode(), …'。
        String frame =
                // 继续传入或组合固定文字 'event: error\\ndata:'，完成上面尚未结束的调用或声明。
                "event: error\ndata: "
                        // 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的调用或声明。
                        + json.writeValueAsString(
                                // 继续传入或组合调用 Map.of 完成of处理，完成上面尚未结束的调用或声明。
                                Map.of(
                                        // 继续传入或组合固定文字 'code'，完成上面尚未结束的调用或声明。
                                        "code",
                                        // 读取 编码（Code） 的值。
                                        error.getCode(),
                                        // 继续传入或组合固定文字 'message'，完成上面尚未结束的调用或声明。
                                        "message",
                                        // 读取 消息（Message） 的值。
                                        error.getMessage(),
                                        // 继续传入或组合固定文字 'status'，完成上面尚未结束的调用或声明。
                                        "status",
                                        // 读取 状态（Status） 的值。
                                        error.getStatus(),
                                        // 继续传入或组合固定文字 'traceId'，完成上面尚未结束的调用或声明。
                                        "traceId",
                                        // 继续传入或组合表达式 traceId)) 的结果，完成上面尚未结束的调用或声明。
                                        traceId))
                        // 继续传入或组合按表达式计算得到的值：+ "\n\n"，完成上面尚未结束的调用或声明。
                        + "\n\n";
        // 把内容写入当前输出或响应。
        output.write(frame.getBytes(StandardCharsets.UTF_8));
        // 把待写入的数据库操作发送到数据库，但此时还没有提交事务。
        output.flush();
    }

    // 关闭Quietly（closeQuietly）方法：调用 closeQuietly 完成关闭Quietly处理；参数负责传入本次处理所需的数据。
    // 分段回复流（stream），使用 InputStream 类型保存。
    private void closeQuietly(InputStream stream) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 释放连接、文件或客户端占用的资源。
            stream.close();
        // ignored，使用 IOException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IOException ignored) {
            // 超时或取消后的关闭失败不改变原来的业务结果。
        }
    }

    // 使用 PreDestroy 注解配置下面的类、方法或参数。
    // shutdown方法：调用 shutdown 完成shutdown处理；参数负责传入本次处理所需的数据。
    @PreDestroy
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void shutdown() {
        // 调用 deadlines.shutdownNow 完成shutdown当前时间处理。
        deadlines.shutdownNow();
    }
}
