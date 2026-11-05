package com.asterway.notification.mapper;

import com.asterway.notification.dto.NotificationQueryDTO;
import com.asterway.notification.entity.NotificationMessageEntity;
import com.asterway.notification.vo.GoalConditionVO;
import com.asterway.notification.vo.NotificationMessageVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/** 消息与目标条件在相同数据库事务内维护，不引入外部发件箱。 */
@Mapper
public interface NotificationMessageMapper {
    NotificationMessageEntity find(@Param("userId") long userId, @Param("id") long id);

    NotificationMessageVO view(@Param("userId") long userId, @Param("id") long id);

    List<NotificationMessageVO> page(
            @Param("userId") long userId, @Param("q") NotificationQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") NotificationQueryDTO query);

    long unread(@Param("userId") long userId);

    int insert(NotificationMessageEntity row);

    int markRead(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("version") long version,
            @Param("now") Instant now);

    int markAllRead(@Param("userId") long userId, @Param("now") Instant now);

    int handle(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("version") long version,
            @Param("status") String status,
            @Param("now") Instant now);

    GoalConditionVO condition(@Param("userId") long userId, @Param("goalId") long goalId);

    int updateCondition(
            @Param("userId") long userId,
            @Param("goalId") long goalId,
            @Param("version") long version,
            @Param("met") boolean met,
            @Param("round") long round,
            @Param("now") Instant now);

    int invalidateGoal(
            @Param("userId") long userId,
            @Param("goalId") long goalId,
            @Param("keepRound") long keepRound,
            @Param("now") Instant now);

    int clearUserConditions(@Param("userId") long userId, @Param("now") Instant now);

    int invalidateUser(@Param("userId") long userId, @Param("now") Instant now);
}
