package com.asterway.dashboard.dto;

import com.asterway.common.dto.PageQueryDTO;

import jakarta.validation.constraints.Pattern;

/** 看板任务按分组独立分页；不指定分组时仍返回全部今日及本周未定日任务。 */
public class DashboardTaskQueryDTO extends PageQueryDTO {

    @Pattern(regexp = "TODAY|WEEK_UNDATED", message = "任务分组必须为 TODAY 或 WEEK_UNDATED")
    private String section;

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
