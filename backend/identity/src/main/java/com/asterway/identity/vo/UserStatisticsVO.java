package com.asterway.identity.vo;

/** 按操作者可见范围统计账户，禁用不等于删除。 */
public record UserStatisticsVO(
        long users,
        long administrators,
        long disabled,
        long deleted
) {
}
