package com.asterway.common.mapper;

import com.asterway.common.security.GrowthScopeData;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 仅提供跨模块归属、用户锁与关联处理，SQL 全部位于 XML。 */
@Mapper
public interface GrowthSupportMapper {
    Long lockUser(@Param("userId") long userId);

    /** 账户生命周期也需要锁定停用用户，不能使用仅有效账号的普通写入口。 */
    Long lockAnyUser(@Param("userId") long userId);

    com.asterway.common.security.ExecutionAccountData calendarAccount(@Param("userId") long userId);

    String timeZone(@Param("userId") long userId);

    GrowthScopeData goal(@Param("userId") long userId, @Param("id") long id);

    GrowthScopeData stage(@Param("userId") long userId, @Param("id") long id);

    GrowthScopeData task(@Param("userId") long userId, @Param("id") long id);

    GrowthScopeData week(@Param("userId") long userId, @Param("id") long id);

    GrowthScopeData day(@Param("userId") long userId, @Param("id") long id);

    long stages(@Param("userId") long userId, @Param("id") long id);

    long tasks(@Param("userId") long userId, @Param("id") long id);

    int endTask(@Param("userId") long userId, @Param("id") long id);

    int endGoal(@Param("userId") long userId, @Param("id") long id);

    java.util.List<com.asterway.common.result.GrowthOverviewVO> stageOverview(
            @Param("userId") long userId, @Param("id") long id);

    java.util.List<com.asterway.common.result.GrowthOverviewVO> taskOverview(
            @Param("userId") long userId, @Param("id") long id);
}
