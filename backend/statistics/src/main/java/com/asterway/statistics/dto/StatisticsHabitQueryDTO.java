package com.asterway.statistics.dto;

/** 习惯统计分组；只支持 HABIT 或 CATEGORY，不接受任意 SQL 维度。 */
public class StatisticsHabitQueryDTO extends StatisticsQueryDTO {
    private String groupBy = "HABIT";

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }
}
