import {
  ApiError,
  acceptSessionHeaders,
  api,
  captureSessionRequest,
  checkSessionRequest,
  rejectUnauthorized,
} from "./http";

const MAX_FRAME_CHARS = 262144;
const MAX_REPLY_CHARS = 64000;
const item = (value) => encodeURIComponent(String(value));
const optional = (value) =>
  value !== "" && value !== null && value !== undefined;

/** 拼接查询串；空值不参与请求，由服务端使用默认值。 */
function queryText(params) {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params || {}))
    if (optional(value)) search.set(key, String(value));
  const text = search.toString();
  return text ? "?" + text : "";
}

/**
 * 读取 POST SSE。
 * 只接受 message_start → delta* → message_end 的严格顺序；
 * 保活注释不构成回复，出错或乱序直接终止，避免展示不完整内容。
 */
async function readEventStream(
  response,
  { signal, context, onStart, onDelta, onEnd, onTool, onActivity, onWorkflow, onReview },
) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "",
    started = false,
    ended = false,
    replyLength = 0,
    totalReplyLength = 0;

  function frame(text) {
    const lines = text.split("\n");
    let event = "message";
    const data = [];
    for (const line of lines) {
      if (!line || line.startsWith(":")) continue;
      const colon = line.indexOf(":");
      const key = colon < 0 ? line : line.slice(0, colon);
      const value = colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "");
      if (key === "event") event = value;
      if (key === "data") data.push(value);
    }
    if (!data.length) return; // 保活注释不构成回复或成功事件。
    let payload;
    try {
      payload = JSON.parse(data.join("\n"));
    } catch {
      throw new ApiError("流式内容格式错误，请重试", 502, "INVALID_STREAM");
    }
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
      throw new ApiError("流式内容结构错误，请重试", 502, "INVALID_STREAM");
    }
    if (event === "error") {
      if (payload.status === 401) rejectUnauthorized(context);
      const error = new ApiError(
        payload.message || "回复中断，请重试",
        payload.status || 502,
        payload.code,
        payload.traceId,
      );
      // 保留服务端可公开的会话与轮次标识，供页面决定是否刷新或恢复。
      error.conversationId = payload.conversationId || null;
      error.turnId = payload.turnId || null;
      throw error;
    }
    if (event === "tool_start" || event === "tool_end") {
      if (!started)
        throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
      if (event === "tool_start" && payload.resetReply === true)
        replyLength = 0;
      onTool?.(event, payload);
      return;
    }
    if (event === "activity" || event === "workflow_update" || event === "review_update") {
      if (!started)
        throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
      if (event === "activity") onActivity?.(payload);
      else if (event === "review_update") onReview?.(payload);
      else onWorkflow?.(payload);
      return;
    }
    if (event === "message_start" && !started) {
      started = true;
      onStart?.(payload);
    } else if (
      event === "delta" &&
      started &&
      typeof payload.text === "string"
    ) {
      replyLength += payload.text.length;
      totalReplyLength += payload.text.length;
      if (totalReplyLength > MAX_REPLY_CHARS)
        throw new ApiError("回复过长，已停止接收", 502, "REPLY_TOO_LARGE");
      onDelta?.(payload.text);
    } else if (
      event === "message_end" &&
      started &&
      replyLength > 0 &&
      typeof payload.finishReason === "string"
    ) {
      ended = true;
      onEnd?.(payload);
    } else {
      throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
    }
  }

  try {
    while (!ended) {
      const { value, done } = await reader.read();
      checkSessionRequest(context);
      if (signal?.aborted) throw new DOMException("生成已停止", "AbortError");
      buffer = (buffer + decoder.decode(value, { stream: !done })).replace(
        /\r\n/g,
        "\n",
      );
      let boundary;
      while (!ended && (boundary = buffer.indexOf("\n\n")) >= 0) {
        frame(buffer.slice(0, boundary));
        buffer = buffer.slice(boundary + 2);
      }
      if (buffer.length > MAX_FRAME_CHARS)
        throw new ApiError("流式内容超出限制", 502, "FRAME_TOO_LARGE");
      if (done) break;
    }
    if (!ended)
      throw new ApiError(
        "连接已中断，回复尚未完成，请重试",
        502,
        "STREAM_INCOMPLETE",
      );
  } finally {
    await reader.cancel().catch(() => {});
    reader.releaseLock();
  }
}

/** 所有流式入口共用会话校验、错误映射与帧读取。 */
async function openEventStream(path, payload, handlers) {
  const context = captureSessionRequest();
  const response = await fetch("/asterway" + path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream, application/json",
      Authorization: `Bearer ${context.token}`,
    },
    body: JSON.stringify(payload),
    signal: handlers.signal,
  });
  acceptSessionHeaders(context, response);
  if (!response.ok) {
    const result = await response.json().catch(() => ({}));
    if (response.status === 401) rejectUnauthorized(context);
    throw new ApiError(
      result.message || `对话请求失败（${response.status}）`,
      response.status,
      result.code,
      result.traceId,
    );
  }
  if (
    !response.body ||
    !response.headers.get("Content-Type")?.includes("text/event-stream")
  ) {
    throw new ApiError("对话服务未返回流式内容，请重试", 502, "INVALID_STREAM");
  }
  await readEventStream(response, { ...handlers, context });
}

/** P1.1 单轮对话；保留供旧入口与后端兼容，页面已改用会话发送。 */
export function streamChat(message, handlers) {
  return openEventStream("/agent/chat/stream", { message }, handlers);
}

/** 会话内发送；requestId 由浏览器生成，用于重放与幂等。 */
export function streamConversationMessage(
  conversationId,
  { message, requestId },
  handlers,
) {
  return openEventStream(
    `/agent/conversations/${item(conversationId)}/messages/stream`,
    { message, requestId },
    handlers,
  );
}

/** 会话公开接口；所有写操作都携带版本，冲突由服务端返回 409。 */
export const agentApi = {
  conversations: (params, signal) =>
    api("/agent/conversations" + queryText(params), { signal }),
  conversation: (conversationId, signal) =>
    api("/agent/conversations/" + item(conversationId), { signal }),
  createConversation: (title) =>
    api("/agent/conversations", {
      method: "POST",
      body: optional(title) && String(title).trim() ? { title } : {},
    }),
  renameConversation: (conversationId, title, version) =>
    api("/agent/conversations/" + item(conversationId), {
      method: "PATCH",
      body: { title, version },
    }),
  closeConversation: (conversationId, version) =>
    api("/agent/conversations/" + item(conversationId) + "/close", {
      method: "POST",
      body: { version },
    }),
  deleteConversation: (conversationId, version) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "?version=" +
        encodeURIComponent(String(version)),
      { method: "DELETE" },
    ),
  messages: (conversationId, params, signal) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "/messages" +
        queryText(params),
      { signal },
    ),
  turn: (conversationId, turnId, signal) =>
    api(
      "/agent/conversations/" + item(conversationId) + "/turns/" + item(turnId),
      { signal },
    ),
  cancelTurn: (conversationId, turnId, version) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "/turns/" +
        item(turnId) +
        "/cancel",
      { method: "POST", body: { version } },
    ),
};
