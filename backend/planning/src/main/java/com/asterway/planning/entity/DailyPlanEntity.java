package com.asterway.planning.entity;

import java.time.Instant;
import java.time.LocalDate;

/** 单表持久化对象；不直接作为 HTTP 返回值 */
public class DailyPlanEntity {

    /** 标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 所属周计划。 */
    private Long weeklyPlanId;

    /** 计划日期。 */
    private LocalDate planDate;

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

    public Long getWeeklyPlanId() {
        return weeklyPlanId;
    }

    public void setWeeklyPlanId(Long weeklyPlanId) {
        this.weeklyPlanId = weeklyPlanId;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
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
