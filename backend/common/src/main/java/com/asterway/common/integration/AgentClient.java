package com.asterway.common.integration;

import com.asterway.common.config.AgentConfig;
import com.asterway.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** 固定内部端点的公共 HTTP 客户端；不记录提问、认证头、密钥或上游错误原文。 */
@Component
public class AgentClient {

    private static final int MAX_JSON_BYTES = 262_144;
    private static final Pattern CONVERSATION_PATH =
            Pattern.compile(
                    "/internal/agent/conversations(?:/\\d+)?(?:/(?:messages(?:/stream)?|close))?"
                            + "(?:/turns/\\d+(?:/cancel)?)?");
    private static final Pattern WORKSPACE_PATH = Pattern.compile(
            "/internal/agent/(?:memories(?:/[1-9][0-9]*(?:/(?:confirm|reject|archive))?|/candidates)?"
                    + "|knowledge(?:/tasks|/[1-9][0-9]*(?:/(?:chunks|revisions|content|retry)"
                    + "|/revisions/[1-9][0-9]*/chunks/[1-9][0-9]*)?)?"
                    + "|planning/proposals(?:/[1-9][0-9]*(?:/(?:revisions|confirm|execute|cancel|progress))?)?"
                    + "|reviews(?:/[1-9][0-9]*(?:/(?:retry|proposals))?|/latest/conversation/[1-9][0-9]*)?"
                    + "|runs/(?:calls|tasks(?:/[1-9][0-9]*/retry)?))");
    private final AgentConfig config;
    private final ObjectMapper json;
    private final HttpClient http;
    private final ScheduledExecutorService deadlines =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread thread = new Thread(r, "agent-stream-deadline");
                        thread.setDaemon(true);
                        return thread;
                    });

    public AgentClient(AgentConfig config, ObjectMapper json) {
        this.config = config;
        this.json = json;
        this.http =
                HttpClient.newBuilder()
                        // 本地 Uvicorn 使用 HTTP/1.1，关闭 h2c 升级协商。
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(config.connectTimeout())
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
    }

    public JsonNode chat(String message, AgentRequestContext context) {
        HttpRequest request = request("/internal/agent/chat", message, context, false);
        try {
            HttpResponse<InputStream> response =
                    http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                var deadline =
                        deadlines.schedule(
                                () -> closeQuietly(body),
                                config.requestTimeout().toMillis(),
                                TimeUnit.MILLISECONDS);
                try {
                    JsonNode result = readJson(body);
                    if (response.statusCode() != 200) {
                        throw upstreamFailure(response.statusCode(), result);
                    }
                    if (!result.path("reply").isTextual()
                            || result.path("reply").asText().isBlank()
                            || !result.path("model").isTextual()
                            || !result.path("finish_reason").isTextual()) {
                        throw unavailable("AGENT_INVALID_RESULT", "对话服务返回结果不完整");
                    }
                    return result;
                } finally {
                    deadline.cancel(false);
                }
            }
        } catch (HttpTimeoutException error) {
            throw unavailable("AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw unavailable("AGENT_CANCELLED", "对话请求已取消");
        } catch (IOException error) {
            throw unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务");
        }
    }

    public void stream(String message, AgentRequestContext context, OutputStream output)
            throws IOException {
        streamRequest("/internal/agent/chat/stream", Map.of("message", message), context, output);
    }

    public void streamMessage(
            long conversationId,
            String message,
            String requestId,
            AgentRequestContext context,
            OutputStream output)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("requestId", requestId);
        streamRequest(
                "/internal/agent/conversations/" + conversationId + "/messages/stream",
                body,
                context,
                output);
    }

    private void streamRequest(
            String path, Object payload, AgentRequestContext context, OutputStream output)
            throws IOException {
        // 请求构造在异步任务内完成，浏览器取消后 InputStream 随 try 立即关闭。
        HttpRequest request = streamRequestBuilder(path, payload, context);
        HttpResponse<InputStream> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            writeError(output, unavailable("AGENT_CANCELLED", "对话请求已取消"), context.traceId());
            return;
        } catch (IOException error) {
            writeError(output, unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务"), context.traceId());
            return;
        }
        try (InputStream body = response.body()) {
            var deadline =
                    deadlines.schedule(
                            () -> closeQuietly(body),
                            config.requestTimeout().toMillis(),
                            TimeUnit.MILLISECONDS);
            try {
                if (response.statusCode() != 200) {
                    JsonNode result;
                    try {
                        result = readJson(body);
                    } catch (IOException error) {
                        writeError(
                                output,
                                unavailable("AGENT_INVALID_RESULT", "对话服务返回了无法识别的结果"),
                                context.traceId());
                        return;
                    }
                    writeError(
                            output,
                            upstreamFailure(response.statusCode(), result),
                            context.traceId());
                    return;
                }
                if (!response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .startsWith("text/event-stream")) {
                    writeError(
                            output,
                            unavailable("AGENT_INVALID_RESULT", "对话服务未返回流式结果"),
                            context.traceId());
                    return;
                }
                byte[] buffer = new byte[8192];
                while (true) {
                    int length;
                    try {
                        length = body.read(buffer);
                    } catch (IOException error) {
                        writeError(
                                output,
                                unavailable("AGENT_STREAM_INTERRUPTED", "回复连接中断，请重试"),
                                context.traceId());
                        return;
                    }
                    if (length == -1) {
                        return;
                    }
                    // 不缓存整个回复；客户端写出失败向外传播，关闭上游且不重复发送。
                    output.write(buffer, 0, length);
                    output.flush();
                }
            } catch (BusinessException error) {
                writeError(output, error, context.traceId());
            } finally {
                deadline.cancel(false);
            }
        }
    }

    public void requireConfigured() {
        if (config.internalKey().isEmpty()) {
            throw new BusinessException(503, "AGENT_NOT_CONFIGURED", "成长助手内部连接尚未配置");
        }
    }

    public JsonNode conversationPage(
            Map<String, String> query, AgentRequestContext context) {
        return jsonRequest("GET", "/internal/agent/conversations", query, null, context);
    }

    public JsonNode conversation(long conversationId, AgentRequestContext context) {
        return jsonRequest(
                "GET", "/internal/agent/conversations/" + conversationId, null, null, context);
    }

    public JsonNode createConversation(String title, AgentRequestContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (title != null && !title.isBlank()) {
            body.put("title", title.trim());
        }
        return jsonRequest(
                "POST", "/internal/agent/conversations", null, body, context);
    }

    public JsonNode renameConversation(
            long conversationId, String title, long version, AgentRequestContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("version", version);
        return jsonRequest(
                "PATCH",
                "/internal/agent/conversations/" + conversationId,
                null,
                body,
                context);
    }

    public JsonNode closeConversation(
            long conversationId, long version, AgentRequestContext context) {
        return jsonRequest(
                "POST",
                "/internal/agent/conversations/" + conversationId + "/close",
                null,
                Map.of("version", version),
                context);
    }

    public JsonNode deleteConversation(
            long conversationId, long version, AgentRequestContext context) {
        return jsonRequest(
                "DELETE",
                "/internal/agent/conversations/" + conversationId,
                Map.of("version", Long.toString(version)),
                null,
                context);
    }

    public JsonNode conversationMessages(
            long conversationId,
            Long beforeMessageNo,
            int size,
            AgentRequestContext context) {
        Map<String, String> query = new LinkedHashMap<>();
        if (beforeMessageNo != null) {
            query.put("beforeMessageNo", Long.toString(beforeMessageNo));
        }
        query.put("size", Integer.toString(size));
        return jsonRequest(
                "GET",
                "/internal/agent/conversations/" + conversationId + "/messages",
                query,
                null,
                context);
    }

    public JsonNode conversationTurn(
            long conversationId, long turnId, AgentRequestContext context) {
        return jsonRequest(
                "GET",
                "/internal/agent/conversations/" + conversationId + "/turns/" + turnId,
                null,
                null,
                context);
    }

    public JsonNode cancelTurn(
            long conversationId, long turnId, long version, AgentRequestContext context) {
        return jsonRequest(
                "POST",
                "/internal/agent/conversations/" + conversationId + "/turns/" + turnId + "/cancel",
                null,
                Map.of("version", version),
                context);
    }

    public JsonNode sendMessage(
            long conversationId,
            String message,
            String requestId,
            AgentRequestContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("requestId", requestId);
        return jsonRequest(
                "POST",
                "/internal/agent/conversations/" + conversationId + "/messages",
                null,
                body,
                context);
    }

    public JsonNode workspaceRequest(String method, String path, Map<String, String> query,
            JsonNode body, AgentRequestContext context) {
        if (!WORKSPACE_PATH.matcher(path).matches()) {
            throw new BusinessException(400, "AGENT_REQUEST_INVALID", "不支持的成长助手资源");
        }
        return jsonRequest(method, path, query, body, context);
    }

    public JsonNode workspaceUpload(String path, Map<String, String> query, String mime,
            byte[] content, AgentRequestContext context) {
        if (!path.matches("/internal/agent/knowledge(?:/[1-9][0-9]*/content)?")) {
            throw new BusinessException(400, "AGENT_REQUEST_INVALID", "不支持的文档资源");
        }
        return jsonRequest(path.endsWith("/content") ? "PUT" : "POST", path, query,
                content, context, mime);
    }

    private JsonNode jsonRequest(
            String method,
            String path,
            Map<String, String> query,
            Object body,
            AgentRequestContext context) {
        return jsonRequest(method, path, query, body, context, null);
    }

    private JsonNode jsonRequest(String method, String path, Map<String, String> query,
            Object body, AgentRequestContext context, String fileMime) {
        if (!CONVERSATION_PATH.matcher(path).matches() && !WORKSPACE_PATH.matcher(path).matches()) {
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        requireConfigured();
        HttpRequest.Builder builder;
        try {
            builder =
                    HttpRequest.newBuilder(config.endpoint(path + queryString(query)))
                            .timeout(config.requestTimeout())
                            .header("Accept", "application/json")
                            .header("Authorization", context.authorization())
                            .header("X-Trace-ID", context.traceId())
                            .header("X-Asterway-Agent-Key", config.internalKey());
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else if (body instanceof byte[] bytes) {
                builder.header("Content-Type", "application/octet-stream")
                        .header("X-File-Type", fileMime == null ? "application/octet-stream" : fileMime)
                        .method(method, HttpRequest.BodyPublishers.ofByteArray(bytes));
            } else {
                builder.header("Content-Type", "application/json")
                        .method(
                                method,
                                HttpRequest.BodyPublishers.ofByteArray(
                                        json.writeValueAsBytes(body)));
            }
        } catch (IOException error) {
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<InputStream> response =
                    http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                var deadline =
                        deadlines.schedule(
                                () -> closeQuietly(stream),
                                config.requestTimeout().toMillis(),
                                TimeUnit.MILLISECONDS);
                try {
                    JsonNode result = readJson(stream);
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw upstreamFailure(response.statusCode(), result);
                    }
                    return result;
                } finally {
                    deadline.cancel(false);
                }
            }
        } catch (HttpTimeoutException error) {
            throw unavailable("AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw unavailable("AGENT_CANCELLED", "对话请求已取消");
        } catch (IOException error) {
            throw unavailable("AGENT_UNAVAILABLE", "暂时无法连接对话服务");
        }
    }

    private String queryString(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (result.length() > 0) {
                result.append('&');
            }
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return result.length() == 0 ? "" : "?" + result;
    }

    private HttpRequest streamRequestBuilder(
            String path, Object body, AgentRequestContext context) {
        requireConfigured();
        if (!CONVERSATION_PATH.matcher(path).matches()
                && !"/internal/agent/chat/stream".equals(path)) {
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
        try {
            return HttpRequest.newBuilder(config.endpoint(path))
                    .timeout(config.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .header("Authorization", context.authorization())
                    .header("X-Trace-ID", context.traceId())
                    .header("X-Asterway-Agent-Key", config.internalKey())
                    .POST(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    json.writeValueAsBytes(body)))
                    .build();
        } catch (IOException error) {
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
    }

    private HttpRequest request(
            String path, String message, AgentRequestContext context, boolean stream) {
        requireConfigured();
        try {
            return HttpRequest.newBuilder(config.endpoint(path))
                    .timeout(config.requestTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", stream ? "text/event-stream" : "application/json")
                    .header("Authorization", context.authorization())
                    .header("X-Trace-ID", context.traceId())
                    .header("X-Asterway-Agent-Key", config.internalKey())
                    .POST(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    json.writeValueAsBytes(Map.of("message", message))))
                    .build();
        } catch (IOException error) {
            throw unavailable("AGENT_REQUEST_INVALID", "无法构造对话请求");
        }
    }

    private JsonNode readJson(InputStream body) throws IOException {
        byte[] bytes = body.readNBytes(MAX_JSON_BYTES + 1);
        if (bytes.length > MAX_JSON_BYTES) {
            throw unavailable("AGENT_RESULT_TOO_LARGE", "对话服务返回内容过多");
        }
        JsonNode result;
        try {
            result = json.readTree(bytes);
        } catch (JsonProcessingException error) {
            throw unavailable("AGENT_INVALID_RESULT", "对话服务返回了无法识别的结果");
        }
        if (result == null || !result.isObject()) {
            throw unavailable("AGENT_INVALID_RESULT", "对话服务返回结构不正确");
        }
        return result;
    }

    private BusinessException upstreamFailure(int status, JsonNode result) {
        String code = result.path("code").asText("");
        if (code.startsWith("AGENT_INTERNAL_")) {
            return new BusinessException(503, "AGENT_CONNECTION_NOT_CONFIGURED", "成长助手内部连接配置不一致");
        }
        return switch (status) {
            case 401 -> BusinessException.unauthorized();
            case 403 -> BusinessException.forbidden();
            case 404 -> BusinessException.notFound();
            case 400, 422 -> new BusinessException(status, safeCode(result, "AGENT_REQUEST_INVALID"),
                    safeMessage(result, "请求内容不符合要求"));
            case 409 ->
                    new BusinessException(
                            409,
                            safeCode(result, "STATE_CONFLICT"),
                            safeMessage(result, "会话状态已变化，请刷新后重试"),
                            result.hasNonNull("data") ? result.get("data") : null);
            case 429 ->
                    new BusinessException(
                            429,
                            safeCode(result, "AGENT_RATE_LIMIT"),
                            safeMessage(result, "对话服务繁忙，请稍后重试"),
                            result.hasNonNull("data") ? result.get("data") : null);
            case 500, 502 -> new BusinessException(502, safeCode(result, "AGENT_UPSTREAM_FAILED"),
                    safeMessage(result, "成长助手暂时不可用，请稍后重试"));
            case 503 -> new BusinessException(503, "AGENT_NOT_READY", "成长助手尚未配置完成");
            case 504 -> new BusinessException(504, "AGENT_TIMEOUT", "对话服务响应超时，请稍后重试");
            default -> unavailable("AGENT_UPSTREAM_FAILED", "成长助手暂时不可用，请稍后重试");
        };
    }

    private String safeCode(JsonNode result, String fallback) {
        String code = result.path("code").asText("");
        return code.matches("[A-Z][A-Z0-9_]{0,63}") ? code : fallback;
    }

    private String safeMessage(JsonNode result, String fallback) {
        String message = result.path("message").asText("");
        return message.isBlank() || message.length() > 200 ? fallback : message;
    }

    private BusinessException unavailable(String code, String message) {
        return new BusinessException(502, code, message);
    }

    private void writeError(OutputStream output, BusinessException error, String traceId)
            throws IOException {
        String frame =
                "event: error\ndata: "
                        + json.writeValueAsString(
                                Map.of(
                                        "code",
                                        error.getCode(),
                                        "message",
                                        error.getMessage(),
                                        "status",
                                        error.getStatus(),
                                        "traceId",
                                        traceId))
                        + "\n\n";
        output.write(frame.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
            // 超时或取消后的关闭失败不改变原来的业务结果。
        }
    }

    @PreDestroy
    public void shutdown() {
        deadlines.shutdownNow();
    }
}
