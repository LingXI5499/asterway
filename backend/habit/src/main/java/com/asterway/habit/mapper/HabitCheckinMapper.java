package com.asterway.habit.mapper;

import com.asterway.habit.dto.HabitCheckinQueryDTO;
import com.asterway.habit.entity.HabitCheckinEntity;
import com.asterway.habit.vo.HabitCheckinVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** habit 持久化接口；所有 SQL 位于对应 XML。 */
@Mapper
public interface HabitCheckinMapper {
    HabitCheckinEntity valid(
            @Param("userId") long userId,
            @Param("habitId") long habitId,
            @Param("today") LocalDate today);

    HabitCheckinEntity find(@Param("userId") long userId, @Param("id") long id);

    int insert(HabitCheckinEntity row);

    int cancel(HabitCheckinEntity row);

    HabitCheckinVO view(@Param("userId") long userId, @Param("id") long id);

    List<HabitCheckinVO> page(
            @Param("userId") long userId,
            @Param("habitId") long habitId,
            @Param("q") HabitCheckinQueryDTO query);

    long count(
            @Param("userId") long userId,
            @Param("habitId") long habitId,
            @Param("q") HabitCheckinQueryDTO query);
}
