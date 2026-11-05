package com.asterway.habit.mapper;

import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.entity.HabitEntity;
import com.asterway.habit.vo.HabitTodayVO;
import com.asterway.habit.vo.HabitVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** habit 持久化接口；所有 SQL 位于对应 XML。 */
@Mapper
public interface HabitMapper {
    HabitEntity find(@Param("userId") long userId, @Param("id") long id);

    int insert(HabitEntity row);

    int update(HabitEntity row);

    List<HabitVO> page(
            @Param("userId") long userId,
            @Param("q") HabitQueryDTO query,
            @Param("today") LocalDate today);

    long count(@Param("userId") long userId, @Param("q") HabitQueryDTO query);

    HabitVO view(
            @Param("userId") long userId, @Param("id") long id, @Param("today") LocalDate today);

    List<HabitTodayVO> today(
            @Param("userId") long userId,
            @Param("today") LocalDate today,
            @Param("offset") int offset,
            @Param("size") int size);

    long todayCount(@Param("userId") long userId, @Param("today") LocalDate today);

    List<HabitEntity> activeBatch(@Param("userId") long userId, @Param("afterId") long afterId);

    int sealHistory(
            @Param("userId") long userId,
            @Param("through") LocalDate through,
            @Param("now") Instant now);
}
