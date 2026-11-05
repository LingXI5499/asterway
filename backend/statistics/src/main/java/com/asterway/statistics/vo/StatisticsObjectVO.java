package com.asterway.statistics.vo;

/** 当前有效未归档对象概况，与历史完成动作分开。 */
public record StatisticsObjectVO(String kind, String status, long objectCount) {}
