package com.asterway.task.entity;

import java.time.Instant;
import java.time.LocalDate;

/** 单表持久化对象；不直接作为 HTTP 返回值 */
public class TaskEntity {

    /** 标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 所属阶段。 */
    private Long stageId;

    /** 名称。 */
    private String name;

    /** 描述。 */
    private String description;

    /** 完成条件。 */
    private String completionCriteria;

    /** 优先级。 */
    private String priority;

    /** 正整数权重。 */
    private Integer weight;

    /** 预计分钟数。 */
    private Integer estimatedMinutes;

    /** 开始日期。 */
    private LocalDate startDate;

    /** 截止日期。 */
    private LocalDate dueDate;

    /** 状态。 */
    private String status;

    /** 排序。 */
    private Integer sortOrder;

    /** 完成时间。 */
    private Instant completedAt;

    /** 乐观锁版本。 */
    private Long version;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 删除时间。 */
    private Instant deletedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompletionCriteria() {
        return completionCriteria;
    }

    public void setCompletionCriteria(String completionCriteria) {
        this.completionCriteria = completionCriteria;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
