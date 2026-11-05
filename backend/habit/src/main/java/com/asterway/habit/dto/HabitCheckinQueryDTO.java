package com.asterway.habit.dto;

import com.asterway.common.dto.PageQueryDTO;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 本人列表筛选，不接收所属用户标识。 */
public class HabitCheckinQueryDTO extends PageQueryDTO {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private Boolean cancelled;

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

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public int getOffset() {
        return offset();
    }
}
