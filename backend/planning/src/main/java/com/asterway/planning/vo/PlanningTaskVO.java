package com.asterway.planning.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 计划任务来源投影；所有关联同时限定用户 */
public class PlanningTaskVO {

    /** 安排标识。 */
    private String assignmentId;

    /** 安排版本。 */
    private Long assignmentVersion;

    /** 任务标识。 */
    private String taskId;

    /** 任务名称。 */
    private String taskName;

    /** 任务状态。 */
    private String status;

    /** 优先级。 */
    private String priority;

    /** 权重。 */
    private Integer weight;

    /** 阶段标识。 */
    private String stageId;

    /** 阶段名称。 */
    private String stageName;

    /** 目标标识。 */
    private String goalId;

    /** 目标名称。 */
    private String goalName;

    /** 周计划。 */
    private String weeklyPlanId;

    /** 日计划。 */
    private String dailyPlanId;

    /** 周一日期。 */
    private LocalDate weekStartDate;

    /** 安排日期。 */
    private LocalDate planDate;

    /** 安排排序。 */
    private Integer sortOrder;

    /** 来源目标归档时间。 */
    private Instant archivedAt;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getAssignmentVersion() {
        return assignmentVersion;
    }

    public void setAssignmentVersion(Long assignmentVersion) {
        this.assignmentVersion = assignmentVersion;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getStageId() {
        return stageId;
    }

    public void setStageId(String stageId) {
        this.stageId = stageId;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public String getWeeklyPlanId() {
        return weeklyPlanId;
    }

    public void setWeeklyPlanId(String weeklyPlanId) {
        this.weeklyPlanId = weeklyPlanId;
    }

    public String getDailyPlanId() {
        return dailyPlanId;
    }

    public void setDailyPlanId(String dailyPlanId) {
        this.dailyPlanId = dailyPlanId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }
}
