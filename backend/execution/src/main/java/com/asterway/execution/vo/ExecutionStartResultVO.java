package com.asterway.execution.vo;

/** 仅供 Controller 选择 201/200，HTTP data 仍直接返回 execution。 */
public record ExecutionStartResultVO(boolean created, ExecutionSessionVO execution) {}
