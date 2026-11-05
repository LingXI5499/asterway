package com.asterway.habit.vo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** 习惯当前资料；历史标准及分类以修订和打卡事实为准。 */
public class HabitVO {
    /** 习惯标识。 */
    private String id;

    /** 生命周期状态。 */
    private String status;

    /** 当前修订号。 */
    private Long revisionNo;

    /** 结束归档时间。 */
    private Instant endedAt;

    /** 已封存日。 */
    private LocalDate historyLockedThrough;

    /** 乐观锁版本。 */
    private Long version;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 当前修订标识。 */
    private String revisionId;

    /** 当前名称。 */
    private String name;

    /** 描述。 */
    private String description;

    /** 完成标准。 */
    private String completionCriteria;

    /** 可空分类标识。 */
    private String categoryId;

    /** 分类名称。 */
    private String categoryName;

    /** 分类已删除时仍保留当前引用。 */
    private Boolean categoryDeleted;

    /** 执行周期。 */
    private String cycleType;

    /** 内部星期掩码，对外返回 weekdays。 */
    private Integer weekdayMask;

    /** 开始日期。 */
    private LocalDate startDate;

    /** 提示时间。 */
    private LocalTime expectedTime;

    /** 今日是否已有有效打卡。 */
    private Boolean checkedToday;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRevisionNo() {
        return revisionNo;
    }

    public void setRevisionNo(Long revisionNo) {
        this.revisionNo = revisionNo;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDate getHistoryLockedThrough() {
        return historyLockedThrough;
    }

    public void setHistoryLockedThrough(LocalDate historyLockedThrough) {
        this.historyLockedThrough = historyLockedThrough;
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

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Boolean getCategoryDeleted() {
        return categoryDeleted;
    }

    public void setCategoryDeleted(Boolean categoryDeleted) {
        this.categoryDeleted = categoryDeleted;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
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

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    public LocalTime getExpectedTime() {
        return expectedTime;
    }

    public void setExpectedTime(LocalTime expectedTime) {
        this.expectedTime = expectedTime;
    }

    public Boolean getCheckedToday() {
        return checkedToday;
    }

    public void setCheckedToday(Boolean checkedToday) {
        this.checkedToday = checkedToday;
    }

    /** API 使用周一至周日的 1～7 数组，不向客户端暴露位运算存储。 */
    public java.util.List<Integer> getWeekdays() {
        return java.util.stream.IntStream.rangeClosed(1, 7)
                .filter(day -> weekdayMask != null && (weekdayMask & (1 << (day - 1))) != 0)
                .boxed()
                .toList();
    }
}
