package com.asterway.execution.entity;

import java.time.Instant;

/** 任务的一次完整执行会话，只映射会话表，不作为接口返回对象。 */
public class ExecutionSessionEntity {

    /** 执行会话主键。 */
    private Long id;

    /** 所属用户，由认证上下文取得。 */
    private Long userId;

    /** 所属任务，逻辑删除后仍保留引用。 */
    private Long taskId;

    /** RUNNING、PAUSED 或 FINISHED。 */
    private String status;

    /** 首次开始时间，恢复时不覆盖。 */
    private Instant startedAt;

    /** 最终结束时间，未结束时为空。 */
    private Instant finishedAt;

    /** 最近有效心跳，开始和恢复时初始化。 */
    private Instant lastHeartbeatAt;

    /** 控制端登录 SID，只保存在持久化对象中。 */
    private String controllerSessionId;

    /** 主动结束、任务删除或目标归档原因。 */
    private String finishReason;

    /** 状态控制版本；心跳不递增。 */
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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getControllerSessionId() {
        return controllerSessionId;
    }

    public void setControllerSessionId(String controllerSessionId) {
        this.controllerSessionId = controllerSessionId;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
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
