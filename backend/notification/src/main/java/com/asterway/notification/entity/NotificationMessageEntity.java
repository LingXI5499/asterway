package com.asterway.notification.entity;

import java.time.Instant;

/** 单表持久化对象；所有归属和写入时间由服务端维护。 */
public class NotificationMessageEntity {
    /** 消息标识。 */
    private Long id;

    /** 收件人。 */
    private Long userId;

    /** 来源目标。 */
    private Long goalId;

    /** 达标轮次。 */
    private Long conditionRound;

    /** 本期仅 GOAL_COMPLETION_CONFIRM。 */
    private String messageType;

    /** 标题快照。 */
    private String title;

    /** 正文快照。 */
    private String content;

    /** 首次已读时间。 */
    private Instant readAt;

    /** 处理状态，与已读独立。 */
    private String handleStatus;

    /** 处理或失效时间。 */
    private Instant handledAt;

    /** 乐观锁版本。 */
    private Long version;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

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

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public Long getConditionRound() {
        return conditionRound;
    }

    public void setConditionRound(Long conditionRound) {
        this.conditionRound = conditionRound;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus;
    }

    public Instant getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(Instant handledAt) {
        this.handledAt = handledAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
