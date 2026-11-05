package com.asterway.execution.dto;

import com.asterway.common.dto.PageQueryDTO;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 会话列表和汇总筛选；所有范围在 Service 中再次校验。 */
public class ExecutionQueryDTO extends PageQueryDTO {

    /** 可选任务筛选。 */
    private Long taskId;

    /** 可选目标筛选。 */
    private Long goalId;

    /** 可选阶段筛选。 */
    private Long stageId;

    /** 可选会话状态。 */
    private String status;

    /** 用户当地开始日期，和结束日期成对提供。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    /** 用户当地结束日期，包含当日。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
