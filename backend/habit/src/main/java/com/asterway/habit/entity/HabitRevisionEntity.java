package com.asterway.habit.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** 单表持久化对象；所有归属和写入时间由服务端维护。 */
public class HabitRevisionEntity {
    /** 修订标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** 所属习惯。 */
    private Long habitId;

    /** 连续修订号。 */
    private Long revisionNo;

    /** 规则或生命周期动作。 */
    private String actionType;

    /** 名称快照。 */
    private String name;

    /** 描述快照。 */
    private String description;

    /** 完成标准快照。 */
    private String completionCriteria;

    /** 分类快照，可空。 */
    private Long categoryId;

    /** DAILY／WEEKLY_DAYS。 */
    private String cycleType;

    /** 星期位图，周一为最低位。 */
    private Integer weekdayMask;

    /** 本地开始日期。 */
    private LocalDate startDate;

    /** 提示时间，仅 HH:mm。 */
    private LocalTime expectedTime;

    /** 动作后状态。 */
    private String statusSnapshot;

    /** 动作后删除标志。 */
    private Boolean deletedSnapshot;

    /** 本次修订生效日。 */
    private LocalDate effectiveDate;

    /** 服务端动作时间。 */
    private Instant changedAt;

    /** 动作时账号有效时区。 */
    private String timeZone;

    /** 入库时间。 */
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

    public Long getHabitId() {
        return habitId;
    }

    public void setHabitId(Long habitId) {
        this.habitId = habitId;
    }

    public Long getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(Long revisionNo) {
        this.revisionNo = revisionNo;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public Integer getWeekdayMask() {
        return weekdayMask;
    }

    public void setWeekdayMask(Integer weekdayMask) {
        this.weekdayMask = weekdayMask;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalTime getExpectedTime() {
        return expectedTime;
    }

    public void setExpectedTime(LocalTime expectedTime) {
        this.expectedTime = expectedTime;
    }

    public String getStatusSnapshot() {
        return statusSnapshot;
    }

    public void setStatusSnapshot(String statusSnapshot) {
        this.statusSnapshot = statusSnapshot;
    }

    public Boolean getDeletedSnapshot() {
        return deletedSnapshot;
    }

    public void setDeletedSnapshot(Boolean deletedSnapshot) {
        this.deletedSnapshot = deletedSnapshot;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
