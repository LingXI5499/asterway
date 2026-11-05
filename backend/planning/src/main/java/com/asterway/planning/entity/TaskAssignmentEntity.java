package com.asterway.planning.entity;

import java.time.Instant;

/** 单表持久化对象；不直接作为 HTTP 返回值 */
public class TaskAssignmentEntity {

    /** 标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 任务。 */
    private Long taskId;

    /** 周计划。 */
    private Long weeklyPlanId;

    /** 日计划，可空。 */
    private Long dailyPlanId;

    /** 排序。 */
    private Integer sortOrder;

    /** 安排建立时间。 */
    private Instant assignedAt;

    /** 安排结束时间。 */
    private Instant endedAt;

    /** 结束原因。 */
    private String endReason;

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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getWeeklyPlanId() {
        return weeklyPlanId;
    }

    public void setWeeklyPlanId(Long weeklyPlanId) {
        this.weeklyPlanId = weeklyPlanId;
    }

    public Long getDailyPlanId() {
        return dailyPlanId;
    }

    public void setDailyPlanId(Long dailyPlanId) {
        this.dailyPlanId = dailyPlanId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
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
