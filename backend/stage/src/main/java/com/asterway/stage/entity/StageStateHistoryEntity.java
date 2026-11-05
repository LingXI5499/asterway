package com.asterway.stage.entity;

import java.time.Instant;

/** 追加式状态事实；不提供修改或删除业务操作。 */
public class StageStateHistoryEntity {
    /** 历史事实标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 所属业务对象。 */
    private Long stageId;

    /** 真实事件类型。 */
    private String eventType;

    /** 变化前状态。 */
    private String beforeStatus;

    /** 变化后状态。 */
    private String afterStatus;

    /** 事件发生时的名称。 */
    private String nameSnapshot;

    /** 该事件对应的主体版本。 */
    private Long entityVersion;

    /** 业务发生时间。 */
    private Instant occurredAt;

    /** 用户、联动或初始化来源。 */
    private String sourceType;

    /** 记录入库时间。 */
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public void setNameSnapshot(String nameSnapshot) {
        this.nameSnapshot = nameSnapshot;
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
