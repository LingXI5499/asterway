package com.asterway.common.dto;

import java.time.LocalDate;

/** 成长业务统一查询条件；用户身份不从请求参数读取 */
public class GrowthQueryDTO extends PageQueryDTO {

    public int getOffset() {
        return offset();
    }

    /** 关键字。 */
    private String keyword;

    /** 状态。 */
    private String status;

    /** 任务优先级。 */
    private String priority;

    /** 任务位置 POOL/WEEK/DAY。 */
    private String assignmentStatus;

    /** 目标时间范围 ALL/CURRENT/FUTURE。 */
    private String timing;

    /** 是否归档。 */
    private Boolean archived;

    /** 目标。 */
    private Long goalId;

    /** 阶段。 */
    private Long stageId;

    /** 任务。 */
    private Long taskId;

    /** 周计划。 */
    private Long weeklyPlanId;

    /** 日计划。 */
    private Long dailyPlanId;

    /** 开始日期下限。 */
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate startFrom;

    /** 开始日期上限。 */
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate startTo;

    /** 计划日期下限。 */
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** 计划日期上限。 */
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    /** 由 Service 按用户时区覆盖。 */
    @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate today;

    /** 是否仅当前安排。 */
    private boolean currentOnly;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
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

    public LocalDate getStartFrom() {
        return startFrom;
    }

    public void setStartFrom(LocalDate startFrom) {
        this.startFrom = startFrom;
    }

    public LocalDate getStartTo() {
        return startTo;
    }

    public void setStartTo(LocalDate startTo) {
        this.startTo = startTo;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public LocalDate getToday() {
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    public boolean getCurrentOnly() {
        return currentOnly;
    }

    public void setCurrentOnly(boolean currentOnly) {
        this.currentOnly = currentOnly;
    }
}
