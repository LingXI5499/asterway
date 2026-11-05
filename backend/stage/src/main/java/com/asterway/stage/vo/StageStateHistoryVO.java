package com.asterway.stage.vo;

import java.time.Instant;

/** 主体真实历史；BASELINE 仅说明初始化状态，不计为完成动作。 */
public record StageStateHistoryVO(
        String id,
        String stageId,
        String eventType,
        String beforeStatus,
        String afterStatus,
        String nameSnapshot,
        long entityVersion,
        Instant occurredAt,
        String sourceType,
        Instant createdAt) {}
