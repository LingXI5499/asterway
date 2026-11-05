package com.asterway.statistics.dto;

import com.asterway.common.dto.PageQueryDTO;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 多维统计条件；身份、时区与今天由服务端填写，不从客户端接收。 */
public class StatisticsQueryDTO extends PageQueryDTO {
    private String period = "MONTH";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate anchorDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private Long goalId;
    private Long stageId;
    private Long taskId;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalDate getAnchorDate() {
        return anchorDate;
    }

    public void setAnchorDate(LocalDate anchorDate) {
        this.anchorDate = anchorDate;
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

    public int getOffset() {
        return offset();
    }
}
