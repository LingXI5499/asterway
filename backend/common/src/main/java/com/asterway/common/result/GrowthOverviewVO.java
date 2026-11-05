package com.asterway.common.result;

import java.math.BigDecimal;

/** 目标和阶段详情使用的轻量子项，不引入业务工程依赖。 */
public record GrowthOverviewVO(String id, String name, String status, BigDecimal progress) {}
