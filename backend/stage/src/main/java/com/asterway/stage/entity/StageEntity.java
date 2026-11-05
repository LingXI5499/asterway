package com.asterway.stage.entity;

import java.time.Instant;
import java.time.LocalDate;

/** 单表持久化对象；不直接作为 HTTP 返回值 */
public class StageEntity {

    /** 标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 所属目标。 */
    private Long goalId;

    /** 名称。 */
    private String name;

    /** 描述。 */
    private String description;

    /** 完成条件。 */
    private String completionCriteria;

    /** 开始日期。 */
    private LocalDate startDate;

    /** 结束日期。 */
    private LocalDate endDate;

    /** 排序。 */
    private Integer sortOrder;

    /** 状态。 */
    private String status;

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

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
