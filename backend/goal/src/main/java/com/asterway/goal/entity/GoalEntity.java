package com.asterway.goal.entity;

import java.time.Instant;
import java.time.LocalDate;

/** 单表持久化对象；不直接作为 HTTP 返回值 */
public class GoalEntity {

    /** 标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 名称。 */
    private String name;

    /** 描述。 */
    private String description;

    /** 完成条件。 */
    private String completionCriteria;

    /** 开始日期。 */
    private LocalDate startDate;

    /** 预计结束日期。 */
    private LocalDate expectedEndDate;

    /** 状态。 */
    private String status;

    /** 归档时间。 */
    private Instant archivedAt;

    /** 精确达标缓存，只用于通知轮次，不存进度百分比。 */
    private Boolean completionConditionMet;

    /** 未达标转达标时增加的提醒轮次。 */
    private Long completionRound;

    public Boolean getCompletionConditionMet() {
        return completionConditionMet;
    }

    public void setCompletionConditionMet(Boolean completionConditionMet) {
        this.completionConditionMet = completionConditionMet;
    }

    public Long getCompletionRound() {
        return completionRound;
    }

    public void setCompletionRound(Long completionRound) {
        this.completionRound = completionRound;
    }

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

    public LocalDate getExpectedEndDate() {
        return expectedEndDate;
    }

    public void setExpectedEndDate(LocalDate expectedEndDate) {
        this.expectedEndDate = expectedEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
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
