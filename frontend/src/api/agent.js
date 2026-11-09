/* 引入 ./http 提供的组件、状态函数或请求方法。 */
/* 封装成长助手接口请求，供页面复用同一套路径、参数和响应处理。 */
import {
  ApiError,
  acceptSessionHeaders,
  api,
  captureSessionRequest,
  checkSessionRequest,
  rejectUnauthorized,
} from "./http";

const /* 最大FRAMECHARS（MAX_FRAME_CHARS）保存固定数值 262144。 */ MAX_FRAME_CHARS = 262144;
const /* 最大REPLYCHARS（MAX_REPLY_CHARS）保存固定数值 64000。 */ MAX_REPLY_CHARS = 64000;
const /* 单条数据（item）保存表达式 (value) => encodeURIComponent(String(value)) 的结果。 */ item = (value) => encodeURIComponent(String(value));
const /* 可选（optional）保存条件判断的结果：(value) => value !== "" && value !== null && value !== undefined。 */ optional = (value) =>
  value !== "" && value !== null && value !== undefined;

/* 拼接查询串；空值不参与请求，由服务端使用默认值。 */
/* 查询条件文字（queryText）：调用 queryText 完成查询条件文字处理。 */
function queryText(params) {
  const /* 搜索（search）保存新建 地址搜索参数集合（URLSearchParams） 实例。 */ search = new URLSearchParams();
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (const /* 声明 键值（[key, value]），稍后再填入处理结果。 */ [key, value] of Object.entries(params || {}))
    /* 检查条件 optional(value) 是否成立，成立时走这一分支。 */
    if (optional(value)) /* 调用 search.set 完成设置处理。 */ search.set(key, String(value));
  const /* 文字（text）保存调用 search.toString 完成to文字处理。 */ text = search.toString();
  /* 返回根据前面的条件，从两个候选结果中选择一个：text ? "?" + text : ""，当前函数到此结束。 */
  return text ? "?" + text : "";
}

/*
 * 读取 POST SSE。
 * 只接受 message_start → delta* → message_end 的严格顺序；
 * 保活注释不构成回复，出错或乱序直接终止，避免展示不完整内容。
 */
/* 读取事件分段回复流（readEventStream）：读取 事件分段回复流（EventStream）。 */
async function readEventStream(
  response,
  { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal, /* 直接使用已有的上下文（context）作为同名字段。 */ context, /* 直接使用已有的事件发生时开始（onStart）作为同名字段。 */ onStart, /* 直接使用已有的事件发生时Delta（onDelta）作为同名字段。 */ onDelta, /* 直接使用已有的事件发生时结束（onEnd）作为同名字段。 */ onEnd, /* 直接使用已有的事件发生时业务工具（onTool）作为同名字段。 */ onTool, /* 直接使用已有的事件发生时Activity（onActivity）作为同名字段。 */ onActivity, /* 直接使用已有的事件发生时规划工作流（onWorkflow）作为同名字段。 */ onWorkflow, /* 直接使用已有的事件发生时复盘（onReview）作为同名字段。 */ onReview },
) {
  const /* reader保存读取 Reader 的值。 */ reader = response.body.getReader();
  const /* decoder保存新建 文字Decoder（TextDecoder） 实例。 */ decoder = new TextDecoder("utf-8");
  let /* 缓冲区（buffer）保存固定文字 ''。 */ buffer = "",
    /* 开始时（started）保存假，表示条件不成立或开关关闭。 */
    started = false,
    /* 已结束（ended）保存假，表示条件不成立或开关关闭。 */
    ended = false,
    /* reply长度（replyLength）保存固定数值 0。 */
    replyLength = 0,
    /* 总数Reply长度（totalReplyLength）保存固定数值 0。 */
    totalReplyLength = 0;

  /* frame：调用 frame 完成frame处理。 */
  function frame(text) {
    const /* lines保存按指定分隔规则拆分文字。 */ lines = text.split("\n");
    let /* 事件（event）保存固定文字 'message'。 */ event = "message";
    const /* 数据（data）保存一个空列表，用来保存后续多条数据。 */ data = [];
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    for (const /* 声明 line，稍后再填入处理结果。 */ line of lines) {
      /* 检查条件 !line || line.startsWith(":") 是否成立，成立时走这一分支。 */
      if (!line || line.startsWith(":")) continue;
      const /* colon保存调用 line.indexOf 完成索引Of处理。 */ colon = line.indexOf(":");
      const /* 键（key）保存根据前面的条件，从两个候选结果中选择一个：colon < 0 ? line : line.slice(0, colon)。 */ key = colon < 0 ? line : line.slice(0, colon);
      const /* 值（value）保存根据前面的条件，从两个候选结果中选择一个：colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "")。 */ value = colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "");
      /* 检查条件 key === "event" 是否成立，成立时走这一分支。 */
      if (key === "event") /* 把值（value）写入 event。 */ event = value;
      /* 检查条件 key === "data" 是否成立，成立时走这一分支。 */
      if (key === "data") /* 调用 data.push 完成push处理。 */ data.push(value);
    }
    /* 检查条件 !data.length 是否成立，成立时走这一分支。 */
    if (!data.length) /* 返回空内容，当前函数到此结束。 */ return; // 保活注释不构成回复或成功事件。
    let /* 声明 提交数据（payload），稍后再填入处理结果。 */ payload;
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      /* 把调用 JSON.parse 完成解析处理写入 payload。 */
      payload = JSON.parse(data.join("\n"));
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
      /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
      throw new ApiError("流式内容格式错误，请重试", 502, "INVALID_STREAM");
    }
    /* 检查条件 !payload || typeof payload !== "object" || Array.isArray(payload) 是否成立，成立时走这一分支。 */
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
      /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
      throw new ApiError("流式内容结构错误，请重试", 502, "INVALID_STREAM");
    }
    /* 检查条件 event === "error" 是否成立，成立时走这一分支。 */
    if (event === "error") {
      /* 检查条件 payload.status === 401 是否成立，成立时走这一分支。 */
      if (payload.status === 401) /* 调用 rejectUnauthorized 完成拒绝Unauthorized处理。 */ rejectUnauthorized(context);
      const /* 错误（error）保存新建 接口错误（ApiError） 实例。 */ error = new ApiError(
        payload.message || "回复中断，请重试",
        payload.status || 502,
        payload.code,
        payload.traceId,
      );
      // 保留服务端可公开的会话与轮次标识，供页面决定是否刷新或恢复。
      /* 把优先使用前面的有效值，没有时采用后面的备用值：payload.conversationId || null写入 error.conversationId。 */
      error.conversationId = payload.conversationId || null;
      /* 把优先使用前面的有效值，没有时采用后面的备用值：payload.turnId || null写入 error.turnId。 */
      error.turnId = payload.turnId || null;
      /* 抛出错误（error），当前操作按失败处理。 */
      throw error;
    }
    /* 检查条件 event === "tool_start" || event === "tool_end" 是否成立，成立时走这一分支。 */
    if (event === "tool_start" || event === "tool_end") {
      /* 检查条件 !started 是否成立，成立时走这一分支。 */
      if (!started)
        /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
        throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
      /* 检查条件 event === "tool_start" && payload.resetReply === true 是否成立，成立时走这一分支。 */
      if (event === "tool_start" && payload.resetReply === true)
        /* 把固定数值 0写入 replyLength。 */
        replyLength = 0;
      /* 更新本次处理状态：onTool?.(event, payload)。 */
      onTool?.(event, payload);
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    /* 检查条件 event === "activity" || event === "workflow_update" || event === "review_update" 是否成立，成立时走这一分支。 */
    if (event === "activity" || event === "workflow_update" || event === "review_update") {
      /* 检查条件 !started 是否成立，成立时走这一分支。 */
      if (!started)
        /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
        throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
      /* 检查条件 event === "activity" 是否成立，成立时走这一分支。 */
      if (event === "activity") /* 更新本次处理状态：onActivity?.(payload)。 */ onActivity?.(payload);
      else /* 检查条件 event === "review_update" 是否成立，成立时走这一分支。 */ if (event === "review_update") /* 更新本次处理状态：onReview?.(payload)。 */ onReview?.(payload);
      else /* 更新本次处理状态：onWorkflow?.(payload)。 */ onWorkflow?.(payload);
      /* 返回空内容，当前函数到此结束。 */
      return;
    }
    /* 检查条件 event === "message_start" && !started 是否成立，成立时走这一分支。 */
    if (event === "message_start" && !started) {
      /* 把真，表示条件成立或开关打开写入 started。 */
      started = true;
      /* 更新本次处理状态：onStart?.(payload)。 */
      onStart?.(payload);
    } else /* 检查条件 event === "delta" && started && typeof payload.text === "string" 是否成立，成立时走这一分支。 */ if (
      event === "delta" &&
      started &&
      typeof payload.text === "string"
    ) {
      /* 更新本次处理状态：replyLength += payload.text.length。 */
      replyLength += payload.text.length;
      /* 更新本次处理状态：totalReplyLength += payload.text.length。 */
      totalReplyLength += payload.text.length;
      /* 检查条件 totalReplyLength > MAX_REPLY_CHARS 是否成立，成立时走这一分支。 */
      if (totalReplyLength > MAX_REPLY_CHARS)
        /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
        throw new ApiError("回复过长，已停止接收", 502, "REPLY_TOO_LARGE");
      /* 更新本次处理状态：onDelta?.(payload.text)。 */
      onDelta?.(payload.text);
    } else /* 检查条件 event === "message_end" && started && replyLength > 0 && typeof payload.finishReason === "string" 是否成立，成立时走这一分支。 */ if (
      event === "message_end" &&
      started &&
      replyLength > 0 &&
      typeof payload.finishReason === "string"
    ) {
      /* 把真，表示条件成立或开关打开写入 ended。 */
      ended = true;
      /* 更新本次处理状态：onEnd?.(payload)。 */
      onEnd?.(payload);
    } else {
      /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
      throw new ApiError("流式事件顺序异常，请重试", 502, "INVALID_STREAM");
    }
  }

  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    while (!ended) {
      const /* 值已完成（{ value, done }）保存等待 调用 reader.read 完成读取处理 完成后取得结果。 */ { /* 直接使用已有的值（value）作为同名字段。 */ value, /* 直接使用已有的已完成（done）作为同名字段。 */ done } = await reader.read();
      /* 核对 会话请求（SessionRequest）。 */
      checkSessionRequest(context);
      /* 检查条件 signal?.aborted 是否成立，成立时走这一分支。 */
      if (signal?.aborted) /* 抛出新建 DOMException 实例，当前操作按失败处理。 */ throw new DOMException("生成已停止", "AbortError");
      /* 把调用 (buffer + decoder.decode 完成decode处理写入 buffer。 */
      buffer = (buffer + decoder.decode(value, { /* 分段回复流（stream）字段使用条件判断的结果：!done。 */ stream: !done })).replace(
        /\r\n/g,
        "\n",
      );
      let /* 声明 boundary，稍后再填入处理结果。 */ boundary;
      /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
      while (!ended && (boundary = buffer.indexOf("\n\n")) >= 0) {
        /* 调用 frame(buffer.slice 完成slice处理。 */
        frame(buffer.slice(0, boundary));
        /* 把调用 buffer.slice 完成slice处理写入 buffer。 */
        buffer = buffer.slice(boundary + 2);
      }
      /* 检查条件 buffer.length > MAX_FRAME_CHARS 是否成立，成立时走这一分支。 */
      if (buffer.length > MAX_FRAME_CHARS)
        /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
        throw new ApiError("流式内容超出限制", 502, "FRAME_TOO_LARGE");
      /* 检查条件 done 是否成立，成立时走这一分支。 */
      if (done) break;
    }
    /* 检查条件 !ended 是否成立，成立时走这一分支。 */
    if (!ended)
      /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
      throw new ApiError(
        "连接已中断，回复尚未完成，请重试",
        502,
        "STREAM_INCOMPLETE",
      );
  } finally {
    /* 等待调用 reader.cancel 完成取消处理。 */
    await reader.cancel().catch(() => {});
    /* 调用 reader.releaseLock 完成release加锁处理。 */
    reader.releaseLock();
  }
}

/* 所有流式入口共用会话校验、错误映射与帧读取。 */
/* 是否展开事件分段回复流（openEventStream）：调用 openEventStream 完成是否展开事件分段回复流处理。 */
async function openEventStream(path, payload, handlers) {
  const /* 上下文（context）保存调用 captureSessionRequest 完成capture会话请求处理。 */ context = captureSessionRequest();
  const /* 响应（response）保存等待 调用 fetch 完成获取处理 完成后取得结果。 */ response = await fetch("/asterway" + path, {
    /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
    method: "POST",
    /* 请求头集合（headers）字段使用按表达式计算得到的值：{ "Content-Type": "application/json", Accept: "text/event-stream, application/json", Authorization: `Bearer ${context.to…。 */
    headers: {
      /* 内容类型（Content-Type）字段使用固定文字 'application/json'。 */
      "Content-Type": "application/json",
      /* 接受（Accept）字段使用固定文字 'text/event-stream, application/json'。 */
      Accept: "text/event-stream, application/json",
      /* 授权（Authorization）字段使用表达式 `Bearer ${context.token}` 的结果。 */
      Authorization: `Bearer ${context.token}`,
    },
    /* 请求正文（body）字段使用调用 JSON.stringify 完成stringify处理。 */
    body: JSON.stringify(payload),
    /* 取消信号（signal）字段使用表达式 handlers.signal 的结果。 */
    signal: handlers.signal,
  });
  /* 调用 acceptSessionHeaders 完成接受会话请求头集合处理。 */
  acceptSessionHeaders(context, response);
  /* 检查条件 !response.ok 是否成立，成立时走这一分支。 */
  if (!response.ok) {
    const /* 结果（result）保存等待 调用 response.json 完成JSON 编码和解析库处理 完成后取得结果。 */ result = await response.json().catch(() => ({}));
    /* 检查条件 response.status === 401 是否成立，成立时走这一分支。 */
    if (response.status === 401) /* 调用 rejectUnauthorized 完成拒绝Unauthorized处理。 */ rejectUnauthorized(context);
    /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
    throw new ApiError(
      result.message || `对话请求失败（${response.status}）`,
      response.status,
      result.code,
      result.traceId,
    );
  }
  /* 检查条件 !response.body || !response.headers.get("Content-Type")?.includes("text/event-stream") 是否成立，成立时走这一分支。 */
  if (
    !response.body ||
    !response.headers.get("Content-Type")?.includes("text/event-stream")
  ) {
    /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
    throw new ApiError("对话服务未返回流式内容，请重试", 502, "INVALID_STREAM");
  }
  /* 等待读取 事件分段回复流（EventStream）。 */
  await readEventStream(response, { ...handlers, /* 直接使用已有的上下文（context）作为同名字段。 */ context });
}

/* P1.1 单轮对话；保留供旧入口与后端兼容，页面已改用会话发送。 */
export /* 分段回复流Chat（streamChat）：调用 streamChat 完成分段回复流Chat处理。 */ function streamChat(message, handlers) {
  /* 返回调用 openEventStream 完成是否展开事件分段回复流处理，当前函数到此结束。 */
  return openEventStream("/agent/chat/stream", { /* 直接使用已有的消息（message）作为同名字段。 */ message }, handlers);
}

/* 会话内发送；requestId 由浏览器生成，用于重放与幂等。 */
export /* 分段回复流对话消息（streamConversationMessage）：调用 streamConversationMessage 完成分段回复流对话消息处理。 */ function streamConversationMessage(
  conversationId,
  { /* 直接使用已有的消息（message）作为同名字段。 */ message, /* 直接使用已有的请求编号（requestId）作为同名字段。 */ requestId, knowledgeBaseIds },
  handlers,
) {
  /* 返回调用 openEventStream 完成是否展开事件分段回复流处理，当前函数到此结束。 */
  return openEventStream(
    `/agent/conversations/${item(conversationId)}/messages/stream`,
    { /* 直接使用已有的消息（message）作为同名字段。 */ message, /* 直接使用已有的请求编号（requestId）作为同名字段。 */ requestId, knowledgeBaseIds },
    handlers,
  );
}

/* 会话公开接口；所有写操作都携带版本，冲突由服务端返回 409。 */
export const /* 成长助手接口（agentApi）保存按表达式计算得到的值：{ conversations: (params, signal) => api("/agent/conversations" + queryText(params), { signal }), conversation: (convers…。 */ agentApi = {
  knowledgeBases: (id, knowledgeBaseIds, version) => api(`/agent/conversations/${item(id)}/knowledge-bases`, { method: "PATCH", body: { knowledgeBaseIds, version } }),
  /* 对话列表（conversations）字段使用按表达式计算得到的值：(params, signal) => api("/agent/conversations" + queryText(params), { signal })。 */
  conversations: (params, signal) =>
    api("/agent/conversations" + queryText(params), { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  /* 对话（conversation）字段使用按表达式计算得到的值：(conversationId, signal) => api("/agent/conversations/" + item(conversationId), { signal })。 */
  conversation: (conversationId, signal) =>
    api("/agent/conversations/" + item(conversationId), { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  // 新会话显式保存初始资料范围，默认空列表。
  createConversation: (title, knowledgeBaseIds = []) => api("/agent/conversations", {
    method: "POST", body: { ...(optional(title) && String(title).trim() ? { title } : {}), knowledgeBaseIds },
  }),
  /* 重命名对话（renameConversation）字段使用按表达式计算得到的值：(conversationId, title, version) => api("/agent/conversations/" + item(conversationId), { method: "PATCH", body: { title…。 */
  renameConversation: (conversationId, title, version) =>
    api("/agent/conversations/" + item(conversationId), {
      /* 方法（method）字段使用固定状态或编码 PATCH（PATCH）。 */
      method: "PATCH",
      /* 请求正文（body）字段使用表达式 { title, version } 的结果。 */
      body: { /* 直接使用已有的标题（title）作为同名字段。 */ title, /* 直接使用已有的版本号（version）作为同名字段。 */ version },
    }),
  /* 关闭对话（closeConversation）字段使用按表达式计算得到的值：(conversationId, version) => api("/agent/conversations/" + item(conversationId) + "/close", { method: "POST", body: { ve…。 */
  closeConversation: (conversationId, version) =>
    api("/agent/conversations/" + item(conversationId) + "/close", {
      /* 方法（method）字段使用固定状态或编码 POST（POST）。 */
      method: "POST",
      /* 请求正文（body）字段使用表达式 { version } 的结果。 */
      body: { /* 直接使用已有的版本号（version）作为同名字段。 */ version },
    }),
  /* 删除对话（deleteConversation）字段使用按表达式计算得到的值：(conversationId, version) => api( "/agent/conversations/" + item(conversationId) + "?version=" + encodeURIComponent(Stri…。 */
  deleteConversation: (conversationId, version) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "?version=" +
        encodeURIComponent(String(version)),
      { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" },
    ),
  /* 消息列表（messages）字段使用按表达式计算得到的值：(conversationId, params, signal) => api( "/agent/conversations/" + item(conversationId) + "/messages" + queryText(params…。 */
  messages: (conversationId, params, signal) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "/messages" +
        queryText(params),
      { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal },
    ),
  /* 本轮对话（turn）字段使用按表达式计算得到的值：(conversationId, turnId, signal) => api( "/agent/conversations/" + item(conversationId) + "/turns/" + item(turnId), { si…。 */
  turn: (conversationId, turnId, signal) =>
    api(
      "/agent/conversations/" + item(conversationId) + "/turns/" + item(turnId),
      { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal },
    ),
  /* 取消本轮对话（cancelTurn）字段使用按表达式计算得到的值：(conversationId, turnId, version) => api( "/agent/conversations/" + item(conversationId) + "/turns/" + item(turnId) + "/…。 */
  cancelTurn: (conversationId, turnId, version) =>
    api(
      "/agent/conversations/" +
        item(conversationId) +
        "/turns/" +
        item(turnId) +
        "/cancel",
      { /* 方法（method）字段使用固定状态或编码 POST（POST）。 */ method: "POST", /* 请求正文（body）字段使用表达式 { version } 的结果。 */ body: { /* 直接使用已有的版本号（version）作为同名字段。 */ version } },
    ),
};
