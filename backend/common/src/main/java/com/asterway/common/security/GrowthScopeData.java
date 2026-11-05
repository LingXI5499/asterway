package com.asterway.common.security;

import java.time.Instant;
import java.time.LocalDate;

/** 成长业务归属校验的中立投影，不依赖业务 Entity */
public class GrowthScopeData {

    /** 资源标识。 */
    private Long id;

    /** 版本。 */
    private Long version;

    /** 目标标识。 */
    private Long goalId;

    /** 阶段标识。 */
    private Long stageId;

    /** 周计划标识。 */
    private Long weeklyPlanId;

    /** 状态。 */
    private String status;

    /** 所属目标归档时间。 */
    private Instant archivedAt;

    /** 日计划日期。 */
    private LocalDate planDate;

    /** 周一日期。 */
    private LocalDate weekStartDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public Long getWeeklyPlanId() {
        return weeklyPlanId;
    }

    public void setWeeklyPlanId(Long weeklyPlanId) {
        this.weeklyPlanId = weeklyPlanId;
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

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    private String goalName;

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String value) {
        this.goalName = value;
    }

    private String stageName;

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String value) {
        this.stageName = value;
    }

    private String assignmentStatus;

    public String getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(String value) {
        this.assignmentStatus = value;
    }

    private Long dailyPlanId;

    public Long getDailyPlanId() {
        return dailyPlanId;
    }

    public void setDailyPlanId(Long value) {
        this.dailyPlanId = value;
    }
}
