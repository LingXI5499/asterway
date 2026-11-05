package com.asterway.task.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthReorderDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.security.GoalNotificationProvider;
import com.asterway.common.security.TaskStateHistoryProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.task.dto.TaskSaveDTO;
import com.asterway.task.entity.TaskEntity;
import com.asterway.task.entity.TaskStateHistoryEntity;
import com.asterway.task.mapper.TaskMapper;
import com.asterway.task.mapper.TaskStateHistoryMapper;
import com.asterway.task.service.TaskService;
import com.asterway.task.vo.TaskStateHistoryVO;
import com.asterway.task.vo.TaskVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Task 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
@Service
public class TaskServiceImpl implements TaskService, TaskStateHistoryProvider {
    private final TaskMapper mapper;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;
    private final TaskStateHistoryMapper histories;
    private final GoalNotificationProvider notices;

    public TaskServiceImpl(
            TaskMapper mapper,
            GrowthContextUtils ctx,
            ExecutionLifecycleProvider execution,
            TaskStateHistoryMapper histories,
            GoalNotificationProvider notices) {
        this.mapper = mapper;
        this.ctx = ctx;
        this.execution = execution;
        this.histories = histories;
        this.notices = notices;
    }

    @Override
    public PageResultVO<TaskVO> page(GrowthQueryDTO query) {
        long userId = ctx.read("task:task:list");
        ctx.prepare(query, userId);
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(row -> toVO(row, false)).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public TaskVO get(long id) {
        long userId = ctx.read("task:task:read");
        ctx.scope("task", userId, id, false);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public TaskVO create(long parentId, TaskSaveDTO dto) {
        long userId = ctx.write("task:task:create");
        var parent = ctx.scope("stage", userId, parentId, true);
        TaskEntity row = new TaskEntity();
        row.setUserId(userId);
        row.setStatus("NOT_STARTED");
        row.setStageId(parentId);
        fill(row, dto);
        mapper.insert(row);
        appendHistory(row, "CREATED", null, "USER_ACTION", 0L, Instant.now());
        notices.refreshGoal(userId, parent.getGoalId());
        return toVO(required(userId, row.getId()), true);
    }

    @Override
    @Transactional
    public TaskVO update(long id, TaskSaveDTO dto) {
        long userId = ctx.write("task:task:update");
        ctx.scope("task", userId, id, true);
        TaskEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        dto =
                new TaskSaveDTO(
                        dto.name() == null ? row.getName() : dto.name(),
                        dto.description() == null ? row.getDescription() : dto.description(),
                        dto.completionCriteria() == null
                                ? row.getCompletionCriteria()
                                : dto.completionCriteria(),
                        dto.priority() == null ? row.getPriority() : dto.priority(),
                        dto.weight() == null ? row.getWeight() : dto.weight(),
                        dto.estimatedMinutes() == null
                                ? row.getEstimatedMinutes()
                                : dto.estimatedMinutes(),
                        dto.startDate() == null ? row.getStartDate() : dto.startDate(),
                        dto.dueDate() == null ? row.getDueDate() : dto.dueDate(),
                        dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder(),
                        dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public TaskVO replace(long id, TaskSaveDTO dto) {
        long userId = ctx.write("task:task:update");
        ctx.scope("task", userId, id, true);
        TaskEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public TaskVO status(long id, GrowthStatusDTO dto) {
        long userId = ctx.write("task:task:status");
        ctx.scope("task", userId, id, true);
        TaskEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        String before = row.getStatus();
        String next = ctx.state(dto.status(), true);
        if (next.equals(before)) {
            return toVO(row, true);
        }
        Instant changedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        row.setStatus(next);
        if ("COMPLETED".equals(row.getStatus())) {
            execution.requireTaskFinished(userId, id);
            if (row.getCompletedAt() == null) {
                row.setCompletedAt(changedAt);
            }
        } else {
            row.setCompletedAt(null);
        }
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        appendHistory(
                row, "STATUS_CHANGED", before, "USER_ACTION", row.getVersion() + 1, changedAt);
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        long userId = ctx.write("task:task:delete");
        var scope = ctx.scope("task", userId, id, true);
        TaskEntity row = required(userId, id);
        ctx.version(row.getVersion(), version);
        execution.finishTask(userId, id);
        ctx.endTask(userId, id);
        ctx.changed(mapper.softDelete(row));
        appendHistory(
                row,
                "DELETED",
                row.getStatus(),
                "USER_ACTION",
                row.getVersion() + 1,
                Instant.now());
        notices.refreshGoal(userId, scope.getGoalId());
    }

    @Override
    @Transactional
    public void reorder(long parentId, GrowthReorderDTO dto) {
        long userId = ctx.write("task:task:sort");
        ctx.scope("stage", userId, parentId, true);
        if (dto.items() == null || dto.items().isEmpty() || dto.items().size() > 100) {
            throw BusinessException.badRequest("排序数量须为 1～100");
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (var item : dto.items()) {
            if (!ids.add(item.id())) {
                throw BusinessException.badRequest("排序标识不能重复");
            }
            TaskEntity row = required(userId, item.id());
            if (!java.util.Objects.equals(row.getStageId(), parentId)) {
                throw BusinessException.badRequest("只能调整同一父资源的顺序");
            }
            ctx.version(row.getVersion(), item.version());
            row.setSortOrder(ctx.order(item.sortOrder()));
            ctx.changed(mapper.update(row));
        }
    }

    @Override
    public PageResultVO<TaskStateHistoryVO> history(long id, PageQueryDTO query) {
        long userId = ctx.read("task:task:read");
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

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordExecutionStart(
            long userId, long taskId, String before, long version, Instant at) {
        ctx.lockLifecycleUser(userId);
        if ("IN_PROGRESS".equals(before)) {
            return;
        }
        var row = required(userId, taskId);
        if (!"IN_PROGRESS".equals(row.getStatus()) || row.getVersion() != version) {
            throw new IllegalStateException("执行开始与任务状态事实不一致");
        }
        appendHistory(row, "STATUS_CHANGED", before, "EXECUTION_START", version, at);
        notices.refreshGoal(userId, ctx.scope("task", userId, taskId, false).getGoalId());
    }

    private void appendHistory(
            TaskEntity row, String event, String before, String source, long version, Instant at) {
        var history = new TaskStateHistoryEntity();
        history.setUserId(row.getUserId());
        history.setTaskId(row.getId());
        history.setEventType(event);
        history.setBeforeStatus(before);
        history.setAfterStatus(row.getStatus());
        history.setNameSnapshot(row.getName());
        history.setEntityVersion(version);
        history.setOccurredAt(at.truncatedTo(ChronoUnit.MILLIS));
        history.setSourceType(source);
        ctx.changed(histories.insert(history));
    }

    private void fill(TaskEntity row, TaskSaveDTO dto) {
        ctx.dates(dto.startDate(), dto.dueDate());
        row.setName(ctx.name(dto.name()));
        row.setDescription(ctx.text(dto.description()));
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        row.setStartDate(dto.startDate());
        row.setDueDate(dto.dueDate());
        row.setSortOrder(ctx.order(dto.sortOrder()));
        String priority = dto.priority() == null ? "MEDIUM" : dto.priority();
        if (!java.util.Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority)) {
            throw BusinessException.badRequest("优先级无效");
        }
        int weight = dto.weight() == null ? 1 : dto.weight();
        if (weight <= 0 || dto.estimatedMinutes() != null && dto.estimatedMinutes() <= 0) {
            throw BusinessException.badRequest("权重及预计耗时必须为正数");
        }
        row.setPriority(priority);
        row.setWeight(weight);
        row.setEstimatedMinutes(dto.estimatedMinutes());
    }

    private TaskEntity required(long userId, long id) {
        TaskEntity row = mapper.findById(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private TaskVO toVO(TaskEntity row, boolean detail) {
        var scope = ctx.scope("task", row.getUserId(), row.getId(), false);
        return new TaskVO(
                ctx.id(row.getId()),
                ctx.id(row.getStageId()),
                row.getName(),
                row.getDescription(),
                row.getCompletionCriteria(),
                row.getPriority(),
                row.getWeight(),
                row.getEstimatedMinutes(),
                row.getStartDate(),
                row.getDueDate(),
                row.getStatus(),
                row.getSortOrder(),
                row.getCompletedAt(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                ctx.id(scope.getGoalId()),
                scope.getGoalName(),
                scope.getStageName(),
                scope.getAssignmentStatus(),
                ctx.id(scope.getWeeklyPlanId()),
                ctx.id(scope.getDailyPlanId()),
                scope.getArchivedAt());
    }
}
