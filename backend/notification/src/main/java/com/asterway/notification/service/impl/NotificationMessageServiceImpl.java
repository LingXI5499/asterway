package com.asterway.notification.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.mapper.GrowthSupportMapper;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.GoalCompletionProvider;
import com.asterway.common.security.GoalNotificationProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.notification.dto.NotificationHandleDTO;
import com.asterway.notification.dto.NotificationQueryDTO;
import com.asterway.notification.entity.NotificationMessageEntity;
import com.asterway.notification.mapper.NotificationMessageMapper;
import com.asterway.notification.service.NotificationMessageService;
import com.asterway.notification.vo.NotificationMessageVO;
import com.asterway.notification.vo.NotificationUnreadVO;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;

/** 目标达标、轮次和消息一致提交；消息已读与目标完成分别处理。 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class NotificationMessageServiceImpl
        implements NotificationMessageService, GoalNotificationProvider {
    private final NotificationMessageMapper mapper;
    private final GrowthSupportMapper support;
    private final GrowthContextUtils ctx;
    private final ObjectProvider<GoalCompletionProvider> goals;

    public NotificationMessageServiceImpl(
            NotificationMessageMapper mapper,
            GrowthSupportMapper support,
            GrowthContextUtils ctx,
            ObjectProvider<GoalCompletionProvider> goals) {
        this.mapper = mapper;
        this.support = support;
        this.ctx = ctx;
        this.goals = goals;
    }

    @Override
    public PageResultVO<NotificationMessageVO> page(NotificationQueryDTO query) {
        long userId = ctx.read("notification:message:list");
        query.offset();
        if (query.getHandleStatus() != null
                && !Set.of("PENDING", "CONFIRMED", "CONTINUED", "INVALIDATED")
                        .contains(query.getHandleStatus())) {
            throw BusinessException.badRequest("消息处理状态无效");
        }
        return new PageResultVO<>(
                mapper.page(userId, query),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public NotificationMessageVO get(long id) {
        long userId = ctx.read("notification:message:read");
        required(userId, id);
        return mapper.view(userId, id);
    }

    @Override
    public NotificationUnreadVO unread() {
        return new NotificationUnreadVO(mapper.unread(ctx.read("notification:message:list")));
    }

    @Override
    @Transactional
    public NotificationMessageVO markRead(long id, long version) {
        long userId = ctx.write("notification:message:mark_read");
        var row = required(userId, id);
        if (row.getReadAt() == null) {
            ctx.version(row.getVersion(), version);
            ctx.changed(mapper.markRead(userId, id, version, now()));
        }
        return mapper.view(userId, id);
    }

    @Override
    @Transactional
    public NotificationUnreadVO markAllRead() {
        long userId = ctx.write("notification:message:mark_read");
        mapper.markAllRead(userId, now());
        return new NotificationUnreadVO(mapper.unread(userId));
    }

    @Override
    @Transactional
    public NotificationMessageVO handle(long id, boolean confirm, NotificationHandleDTO dto) {
        long userId = ctx.write("notification:message:handle");
        var row = required(userId, id);
        String target = confirm ? "CONFIRMED" : "CONTINUED";
        if (target.equals(row.getHandleStatus())) {
            return mapper.view(userId, id);
        }
        if (!"PENDING".equals(row.getHandleStatus())) {
            throw BusinessException.conflict("该提醒已经处理或失效");
        }
        ctx.version(row.getVersion(), dto.version());
        var goal = mapper.condition(userId, row.getGoalId());
        if (goal == null
                || !Boolean.TRUE.equals(goal.getQualifies())
                || !Boolean.TRUE.equals(goal.getCompletionConditionMet())
                || !Objects.equals(goal.getCompletionRound(), row.getConditionRound())) {
            throw BusinessException.conflict("目标或达标条件已变化，不能处理旧提醒");
        }
        if (confirm) {
            if (dto.goalVersion() == null) {
                throw BusinessException.badRequest("确认完成必须提供目标版本");
            }
            ctx.version(goal.getVersion(), dto.goalVersion());
            // 延迟获取目标主链，避免通知与目标实现构造循环；状态历史由目标主链追加。
            goals.getObject().confirmFromNotification(userId, row.getGoalId(), dto.goalVersion());
        } else if (dto.goalVersion() != null) {
            ctx.version(goal.getVersion(), dto.goalVersion());
        }
        ctx.changed(mapper.handle(userId, id, row.getVersion(), target, now()));
        refreshGoal(userId, row.getGoalId());
        return mapper.view(userId, id);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshGoal(long userId, long goalId) {
        lock(userId);
        var goal = mapper.condition(userId, goalId);
        if (goal == null) {
            throw BusinessException.notFound();
        }
        boolean met = Boolean.TRUE.equals(goal.getQualifies());
        long round = goal.getCompletionRound();
        Instant now = now();
        if (met != Boolean.TRUE.equals(goal.getCompletionConditionMet())) {
            if (met) {
                round = Math.addExact(round, 1);
            }
            ctx.changed(mapper.updateCondition(userId, goalId, goal.getVersion(), met, round, now));
        }
        mapper.invalidateGoal(userId, goalId, met ? round : 0, now);
        if (met) {
            var notice = new NotificationMessageEntity();
            notice.setUserId(userId);
            notice.setGoalId(goalId);
            notice.setConditionRound(round);
            notice.setTitle("目标已达标，请确认完成");
            notice.setContent("目标「" + goal.getName() + "」的有效任务已全部完成，请确认目标完成或选择继续保持当前状态。");
            notice.setCreatedAt(now);
            notice.setUpdatedAt(now);
            // 唯一索引同时覆盖已处理和已失效消息，同轮永远不重发。
            mapper.insert(notice);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void suspendNotifications(long userId) {
        lock(userId);
        Instant now = now();
        mapper.clearUserConditions(userId, now);
        mapper.invalidateUser(userId, now);
    }

    private void lock(long userId) {
        if (support.lockAnyUser(userId) == null) {
            throw BusinessException.notFound();
        }
    }

    private NotificationMessageEntity required(long userId, long id) {
        var row = mapper.find(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
