package com.asterway.habit.mapper;

import com.asterway.habit.entity.HabitRevisionEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** habit 持久化接口；所有 SQL 位于对应 XML。 */
@Mapper
public interface HabitRevisionMapper {
    HabitRevisionEntity current(
            @Param("userId") long userId,
            @Param("habitId") long habitId,
            @Param("revisionNo") long revisionNo);

    int insert(HabitRevisionEntity row);

    long count(@Param("userId") long userId, @Param("habitId") long habitId);

    List<HabitRevisionEntity> page(
            @Param("userId") long userId,
            @Param("habitId") long habitId,
            @Param("offset") int offset,
            @Param("size") int size);
}
