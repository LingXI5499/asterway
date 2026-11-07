package com.asterway.agent.vo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** 方案提交结果；重复请求返回同一份已保存结果。 */
public record AgentPlanCommitVO(String commitId, String status, JsonNode result, List<String> warnings) {}
