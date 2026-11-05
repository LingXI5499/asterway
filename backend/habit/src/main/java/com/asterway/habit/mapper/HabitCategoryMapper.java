package com.asterway.habit.mapper;

import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.entity.HabitCategoryEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/** habit 持久化接口；所有 SQL 位于对应 XML。 */
@Mapper
public interface HabitCategoryMapper {
    HabitCategoryEntity find(@Param("userId") long userId, @Param("id") long id);

    List<HabitCategoryEntity> page(@Param("userId") long userId, @Param("q") HabitQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") HabitQueryDTO query);

    boolean nameExists(
            @Param("userId") long userId,
            @Param("name") String name,
            @Param("exceptId") Long exceptId);

    int insert(HabitCategoryEntity row);

    int update(HabitCategoryEntity row);

    int softDelete(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("version") long version,
            @Param("now") Instant now);
}
