package com.asterway.goal.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.security.GoalCompletionProvider;
import com.asterway.common.security.GoalNotificationProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.goal.dto.GoalArchiveDTO;
import com.asterway.goal.dto.GoalSaveDTO;
import com.asterway.goal.entity.GoalEntity;
import com.asterway.goal.entity.GoalStateHistoryEntity;
import com.asterway.goal.mapper.GoalMapper;
import com.asterway.goal.mapper.GoalStateHistoryMapper;
import com.asterway.goal.service.GoalService;
import com.asterway.goal.vo.GoalStateHistoryVO;
import com.asterway.goal.vo.GoalVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Goal 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
@Service
public class GoalServiceImpl implements GoalService, GoalCompletionProvider {
    private final GoalMapper mapper;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;
    private final GoalStateHistoryMapper histories;
    private final GoalNotificationProvider notices;

    public GoalServiceImpl(
            GoalMapper mapper,
            GrowthContextUtils ctx,
            ExecutionLifecycleProvider execution,
            GoalStateHistoryMapper histories,
            GoalNotificationProvider notices) {
        this.mapper = mapper;
        this.ctx = ctx;
        this.execution = execution;
        this.histories = histories;
        this.notices = notices;
    }

    @Override
    public PageResultVO<GoalVO> page(GrowthQueryDTO query) {
        long userId = ctx.read("goal:goal:list");
        ctx.prepare(query, userId);
        if (query.getArchived() == null) {
            query.setArchived(false);
        }
        if (query.getTiming() == null || query.getTiming().isBlank()) {
            // 默认工作区排除未来目标；明确筛选状态或归档时展示对应的完整结果。
            query.setTiming(
                    Boolean.TRUE.equals(query.getArchived())
                                    || query.getStatus() != null && !query.getStatus().isBlank()
                            ? "ALL"
                            : "CURRENT");
        }
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(row -> toVO(row, false)).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public GoalVO get(long id) {
        long userId = ctx.read("goal:goal:read");
        ctx.scope("goal", userId, id, false);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public GoalVO create(GoalSaveDTO dto) {
        long userId = ctx.write("goal:goal:create");
        GoalEntity row = new GoalEntity();
        row.setUserId(userId);
        row.setStatus("NOT_STARTED");
        fill(row, dto);
        mapper.insert(row);
        appendHistory(row, "CREATED", null, null, "USER_ACTION", 0L, Instant.now());
        return toVO(required(userId, row.getId()), true);
    }

    @Override
    @Transactional
    public GoalVO update(long id, GoalSaveDTO dto) {
        long userId = ctx.write("goal:goal:update");
        ctx.scope("goal", userId, id, true);
        GoalEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        dto =
                new GoalSaveDTO(
                        dto.name() == null ? row.getName() : dto.name(),
                        dto.description() == null ? row.getDescription() : dto.description(),
                        dto.completionCriteria() == null
                                ? row.getCompletionCriteria()
                                : dto.completionCriteria(),
                        dto.startDate() == null ? row.getStartDate() : dto.startDate(),
                        dto.expectedEndDate() == null
                                ? row.getExpectedEndDate()
                                : dto.expectedEndDate(),
                        dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        notices.refreshGoal(userId, id);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public GoalVO replace(long id, GoalSaveDTO dto) {
        long userId = ctx.write("goal:goal:update");
        ctx.scope("goal", userId, id, true);
        GoalEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        notices.refreshGoal(userId, id);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public GoalVO status(long id, GrowthStatusDTO dto) {
        long userId = ctx.write("goal:goal:status");
        ctx.scope("goal", userId, id, true);
        GoalEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        String before = row.getStatus();
        String next = ctx.state(dto.status(), false);
        if (next.equals(before)) {
            return toVO(row, true);
        }
        row.setStatus(next);
        if ("COMPLETED".equals(next)) {
            row.setCompletionConditionMet(false);
        }
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        appendHistory(
                row,
                "STATUS_CHANGED",
                before,
                row.getArchivedAt() != null,
                "USER_ACTION",
                row.getVersion() + 1,
                Instant.now());
        notices.refreshGoal(userId, id);
        return toVO(required(userId, id), true);
    }

    @Override
    public void delete(long id, long version) {
        long userId = ctx.read("goal:goal:archive");
        ctx.scope("goal", userId, id, false);
        throw new BusinessException(410, "GOAL_DELETE_RETIRED", "目标仅支持归档，请使用归档操作");
    }

    @Override
    @Transactional
    public GoalVO archive(long id, GoalArchiveDTO dto) {
        long userId = ctx.write("goal:goal:archive");
        GoalEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        if (!Boolean.TRUE.equals(dto.confirmed())) {
            throw BusinessException.badRequest("请确认结束目标全部未结束执行并移出计划");
        }
        // 已归档目标也执行联动，可按新规则处理旧 KEEP 策略遗留的安排，历史不删除。
        execution.finishGoal(userId, id);
        ctx.endGoal(userId, id);
        if (row.getArchivedAt() == null) {
            row.setArchivedAt(Instant.now());
            row.setCompletionConditionMet(false);
            ctx.changed(mapper.update(row));
            appendHistory(
                    row,
                    "ARCHIVED",
                    row.getStatus(),
                    false,
                    "USER_ACTION",
                    row.getVersion() + 1,
                    row.getArchivedAt());
        }
        notices.refreshGoal(userId, id);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public GoalVO restore(long id, long version) {
        long userId = ctx.write("goal:goal:restore");
        GoalEntity row = required(userId, id);
        ctx.version(row.getVersion(), version);
        if (row.getArchivedAt() == null) {
            return toVO(row, true);
        }
        row.setArchivedAt(null);
        ctx.changed(mapper.update(row));
        appendHistory(
                row,
                "RESTORED",
                row.getStatus(),
                true,
                "USER_ACTION",
                row.getVersion() + 1,
                Instant.now());
        notices.refreshGoal(userId, id);
        return toVO(required(userId, id), true);
    }

    @Override
    public PageResultVO<GoalStateHistoryVO> history(long id, PageQueryDTO query) {
        long userId = ctx.read("goal:goal:read");
        int offset = query.offset();
        if (histories.historicalSubject(userId, id) == null) {
            throw BusinessException.notFound();
        }
        return new PageResultVO<>(
                histories.page(userId, id, offset, query.getSize()),
                histories.count(userId, id),
                query.getPage(),
                query.getSize());
    }

    /** 通知主链已经校验精确条件和轮次；目标只负责自己的状态、历史与执行联动。 */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmFromNotification(long userId, long goalId, long expectedVersion) {
        ctx.lockLifecycleUser(userId);
        GoalEntity row = required(userId, goalId);
        ctx.version(row.getVersion(), expectedVersion);
        if (row.getArchivedAt() != null || "COMPLETED".equals(row.getStatus())) {
            throw BusinessException.conflict("目标已归档或完成，不能确认旧提醒");
        }
        String before = row.getStatus();
        row.setStatus("COMPLETED");
        row.setCompletionConditionMet(false);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        appendHistory(
                row,
                "STATUS_CHANGED",
                before,
                false,
                "NOTIFICATION",
                expectedVersion + 1,
                Instant.now());
        // 当前消息由调用方先标记 CONFIRMED，再统一撤回其他待处理消息，避免误使自身失效。
    }

    private void appendHistory(
            GoalEntity row,
            String event,
            String before,
            Boolean beforeArchived,
            String source,
            long version,
            Instant at) {
        var history = new GoalStateHistoryEntity();
        history.setUserId(row.getUserId());
        history.setGoalId(row.getId());
        history.setEventType(event);
        history.setBeforeStatus(before);
        history.setAfterStatus(row.getStatus());
        history.setBeforeArchived(beforeArchived);
        history.setAfterArchived(row.getArchivedAt() != null);
        history.setNameSnapshot(row.getName());
        history.setEntityVersion(version);
        history.setOccurredAt(at.truncatedTo(ChronoUnit.MILLIS));
        history.setSourceType(source);
        ctx.changed(histories.insert(history));
    }

    private void fill(GoalEntity row, GoalSaveDTO dto) {
        ctx.dates(dto.startDate(), dto.expectedEndDate());
        row.setName(ctx.name(dto.name()));
        row.setDescription(ctx.text(dto.description()));
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        row.setStartDate(dto.startDate());
        row.setExpectedEndDate(dto.expectedEndDate());
    }

    private GoalEntity required(long userId, long id) {
        GoalEntity row = mapper.findById(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private GoalVO toVO(GoalEntity row, boolean detail) {
        return new GoalVO(
                ctx.id(row.getId()),
                row.getName(),
                row.getDescription(),
                row.getCompletionCriteria(),
                row.getStartDate(),
                row.getExpectedEndDate(),
                row.getStatus(),
                row.getArchivedAt(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                mapper.progress(row.getUserId(), row.getId()),
                detail ? ctx.stageOverview(row.getUserId(), row.getId()) : java.util.List.of(),
                Boolean.TRUE.equals(row.getCompletionConditionMet()),
                row.getCompletionRound());
    }
}
