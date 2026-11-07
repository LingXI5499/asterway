package com.asterway.agent.vo;

/** 普通对话结果；非 stop 的结束原因代表回复可能被截断，界面不得显示为完整成功。 */
public record AgentChatResultVO(String reply, String model, String finishReason) {}
