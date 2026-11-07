package com.asterway.common.integration;

/** 异步转发所需的不可变请求上下文；不能在异步线程中重新读取 SecurityContext。 */
public record AgentRequestContext(long userId, String authorization, String traceId) {

    @Override
    public String toString() {
        return "AgentRequestContext[userId=" + userId + ", traceId=" + traceId + "]";
    }
}
