package com.asterway.planning.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.planning.dto.AssignmentSaveDTO;
import com.asterway.planning.entity.TaskAssignmentEntity;
import com.asterway.planning.mapper.PlanningTaskMapper;
import com.asterway.planning.mapper.TaskAssignmentMapper;
import com.asterway.planning.service.TaskAssignmentService;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.TaskAssignmentVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/** 任务安排唯一写入口：保留安排历史，不复制任务，不自动延期。 */
@Service
public class TaskAssignmentServiceImpl implements TaskAssignmentService {
    private final TaskAssignmentMapper mapper;
    private final PlanningTaskMapper tasks;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;

    public TaskAssignmentServiceImpl(
            TaskAssignmentMapper mapper, PlanningTaskMapper tasks, GrowthContextUtils ctx,
            ExecutionLifecycleProvider execution) {
        this.mapper = mapper;
        this.tasks = tasks;
        this.ctx = ctx;
        this.execution = execution;
    }

    @Override
    public TaskAssignmentVO get(long taskId) {
        long userId = ctx.read("planning:assignment:read");
        ctx.scope("task", userId, taskId, false);
        var row = mapper.current(userId, taskId);
        return row == null ? null : toVO(row);
    }

    @Override
    @Transactional
    public TaskAssignmentVO assign(long taskId, AssignmentSaveDTO dto) {
        long userId = ctx.write("planning:assignment:manage");
        var task = ctx.scope("task", userId, taskId, true);
        if ("COMPLETED".equals(task.getStatus())) {
            throw BusinessException.conflict("已完成任务不能重新安排");
        }
        var week = ctx.scope("week", userId, dto.weeklyPlanId(), false);
        if (week.getWeekStartDate().plusDays(6).isBefore(ctx.today(userId))) {
            throw BusinessException.conflict("不能安排到已结束的周");
        }
        if (dto.dailyPlanId() != null) {
            var day = ctx.scope("day", userId, dto.dailyPlanId(), false);
            if (!Objects.equals(day.getWeeklyPlanId(), dto.weeklyPlanId())
                    || day.getPlanDate().isBefore(week.getWeekStartDate())
                    || day.getPlanDate().isAfter(week.getWeekStartDate().plusDays(6))) {
                throw BusinessException.badRequest("日计划与周计划不一致");
            }
            if (day.getPlanDate().isBefore(ctx.today(userId))) {
                throw BusinessException.conflict("不能安排到过去日期");
            }
        }
        TaskAssignmentEntity old = mapper.current(userId, taskId);
        int order = ctx.order(dto.sortOrder());
        if (old == null) {
            if (dto.assignmentId() != null || dto.version() != null) {
                throw BusinessException.conflict("原安排已变化，请刷新");
            }
        } else {
            if (!Objects.equals(old.getId(), dto.assignmentId())) {
                throw BusinessException.conflict("当前安排已变化，请刷新");
            }
            ctx.version(old.getVersion(), dto.version());
            if (Objects.equals(old.getWeeklyPlanId(), dto.weeklyPlanId())
                    && Objects.equals(old.getDailyPlanId(), dto.dailyPlanId())) {
                if (!Objects.equals(old.getSortOrder(), order)) {
                    old.setSortOrder(order);
                    ctx.changed(mapper.update(old));
                }
                return toVO(mapper.current(userId, taskId));
            }
            execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
            old.setEndedAt(Instant.now());
            old.setEndReason("RESCHEDULED");
            ctx.changed(mapper.update(old));
        }
        TaskAssignmentEntity row = new TaskAssignmentEntity();
        if (old == null) {
            execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
        }
        row.setUserId(userId);
        row.setTaskId(taskId);
        row.setWeeklyPlanId(dto.weeklyPlanId());
        row.setDailyPlanId(dto.dailyPlanId());
        row.setSortOrder(order);
        row.setAssignedAt(Instant.now());
        mapper.insert(row);
        return toVO(mapper.current(userId, taskId));
    }

    @Override
    @Transactional
    public void remove(long taskId, long assignmentId, long version) {
        long userId = ctx.write("planning:assignment:remove");
        ctx.scope("task", userId, taskId, true);
        TaskAssignmentEntity row = mapper.current(userId, taskId);
        if (row == null || !Objects.equals(row.getId(), assignmentId)) {
            throw BusinessException.conflict("当前安排已变化，请刷新");
        }
        ctx.version(row.getVersion(), version);
        execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
        row.setEndedAt(Instant.now());
        row.setEndReason("RETURNED_TO_POOL");
        ctx.changed(mapper.update(row));
    }

    @Override
    public PageResultVO<TaskAssignmentVO> history(long taskId, GrowthQueryDTO query) {
        long userId = ctx.read("planning:assignment:read");
        ctx.prepare(query, userId);
        query.setTaskId(taskId);
        query.setCurrentOnly(false);
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(this::toVO).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public PageResultVO<PlanningTaskVO> pending(GrowthQueryDTO query) {
        long userId = ctx.read("planning:pending:list");
        ctx.prepare(query, userId);
        return new PageResultVO<>(
                tasks.page(userId, query, true),
                tasks.count(userId, query, true),
                query.getPage(),
                query.getSize());
    }

    private TaskAssignmentVO toVO(TaskAssignmentEntity row) {
        return new TaskAssignmentVO(
                ctx.id(row.getId()),
                ctx.id(row.getTaskId()),
                ctx.id(row.getWeeklyPlanId()),
                ctx.id(row.getDailyPlanId()),
                row.getSortOrder(),
                row.getAssignedAt(),
                row.getEndedAt(),
                row.getEndReason(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
