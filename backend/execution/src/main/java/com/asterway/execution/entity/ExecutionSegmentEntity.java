package com.asterway.execution.entity;

import java.time.Instant;

/** 一次连续实际执行，只映射时间段表，暂停间隔不产生记录。 */
public class ExecutionSegmentEntity {

    /** 连续执行时间段主键。 */
    private Long id;

    /** 所属执行会话。 */
    private Long executionId;

    /** 本段开始时的任务安排，历史不改写。 */
    private Long assignmentId;

    /** 会话内从一开始的时间段序号。 */
    private Integer segmentNo;

    /** 本次开始或继续时间。 */
    private Instant startedAt;

    /** 本次暂停或结束的有效截止时间。 */
    private Instant endedAt;

    /** 停止本段的原因。 */
    private String endReason;

    /** 本段的计划及待生效时区边界。 */
    private Instant eligibleUntil;

    /** 本段开始时的 IANA 时区快照。 */
    private String timeZone;

    /** 本段采用的超时秒数快照。 */
    private Integer heartbeatTimeoutSeconds;

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

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Integer getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Integer segmentNo) {
        this.segmentNo = segmentNo;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public Instant getEligibleUntil() {
        return eligibleUntil;
    }

    public void setEligibleUntil(Instant eligibleUntil) {
        this.eligibleUntil = eligibleUntil;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public void setHeartbeatTimeoutSeconds(Integer heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
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
