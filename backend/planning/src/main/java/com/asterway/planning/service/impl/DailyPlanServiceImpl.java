package com.asterway.planning.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.planning.entity.DailyPlanEntity;
import com.asterway.planning.entity.TaskAssignmentEntity;
import com.asterway.planning.mapper.DailyPlanMapper;
import com.asterway.planning.mapper.PlanningTaskMapper;
import com.asterway.planning.mapper.TaskAssignmentMapper;
import com.asterway.planning.service.DailyPlanService;
import com.asterway.planning.vo.DailyPlanVO;
import com.asterway.planning.vo.PlanningTaskVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** 日计划按需创建，日期、所属周和用户必须一致。 */
@Service
public class DailyPlanServiceImpl implements DailyPlanService {
    private final DailyPlanMapper mapper;
    private final TaskAssignmentMapper assignments;
    private final PlanningTaskMapper tasks;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;

    public DailyPlanServiceImpl(
            DailyPlanMapper mapper,
            TaskAssignmentMapper assignments,
            PlanningTaskMapper tasks,
            GrowthContextUtils ctx, ExecutionLifecycleProvider execution) {
        this.mapper = mapper;
        this.assignments = assignments;
        this.tasks = tasks;
        this.ctx = ctx;
        this.execution = execution;
    }

    @Override
    public DailyPlanVO get(long id) {
        long userId = ctx.read("planning:daily:read");
        ctx.scope("day", userId, id, false);
        return toVO(required(userId, id));
    }

    @Override
    public DailyPlanVO findByDate(long weeklyPlanId, LocalDate date) {
        long userId = ctx.read("planning:daily:read");
        var week = ctx.scope("week", userId, weeklyPlanId, false);
        if (date.isBefore(week.getWeekStartDate())
                || date.isAfter(week.getWeekStartDate().plusDays(6))) {
            throw BusinessException.badRequest("日期不属于对应周计划");
        }
        var row = mapper.byDate(userId, date);
        if (row == null) {
            return null;
        }
        if (!Objects.equals(row.getWeeklyPlanId(), weeklyPlanId)) {
            throw BusinessException.conflict("日计划所属周不一致");
        }
        return toVO(row);
    }

    @Override
    @Transactional
    public DailyPlanVO byDate(long weeklyPlanId, LocalDate date) {
        long userId = ctx.write("planning:daily:manage");
        var week = ctx.scope("week", userId, weeklyPlanId, false);
        if (date.isBefore(week.getWeekStartDate())
                || date.isAfter(week.getWeekStartDate().plusDays(6))) {
            throw BusinessException.badRequest("日计划日期必须属于对应自然周");
        }
        DailyPlanEntity row = mapper.byDate(userId, date);
        if (row != null) {
            if (!Objects.equals(row.getWeeklyPlanId(), weeklyPlanId)) {
                throw BusinessException.conflict("该日期已属于其他有效周计划");
            }
            return toVO(row);
        }
        if (date.isBefore(ctx.today(userId))) {
            throw BusinessException.conflict("过去的日计划只读，不能新建");
        }
        row = new DailyPlanEntity();
        row.setUserId(userId);
        row.setWeeklyPlanId(weeklyPlanId);
        row.setPlanDate(date);
        mapper.insert(row);
        return toVO(required(userId, row.getId()));
    }

    @Override
    public PageResultVO<DailyPlanVO> page(long weeklyPlanId, GrowthQueryDTO query) {
        long userId = ctx.read("planning:daily:list");
        ctx.scope("week", userId, weeklyPlanId, false);
        ctx.prepare(query, userId);
        query.setWeeklyPlanId(weeklyPlanId);
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(this::toVO).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query) {
        long userId = ctx.read("planning:daily:read");
        var day = ctx.scope("day", userId, id, false);
        ctx.prepare(query, userId);
        query.setDailyPlanId(id);
        query.setWeeklyPlanId(day.getWeeklyPlanId());
        return new PageResultVO<>(
                tasks.page(userId, query, false),
                tasks.count(userId, query, false),
                query.getPage(),
                query.getSize());
    }

    @Override
    @Transactional
    public void delete(long id, long version, String handling) {
        long userId = ctx.write("planning:daily:manage");
        ctx.scope("day", userId, id, false);
        DailyPlanEntity row = required(userId, id);
        ctx.version(row.getVersion(), version);
        execution.requireNoRunning(userId, ExecutionScopeEnum.DAY, id);
        if (row.getPlanDate().isBefore(ctx.today(userId))) {
            throw BusinessException.conflict("过去的日计划只读，请通过明确的任务改期处理");
        }
        GrowthQueryDTO q = new GrowthQueryDTO();
        q.setDailyPlanId(id);
        q.setCurrentOnly(true);
        q.setSize(100);
        if (assignments.count(userId, q) > 0
                && !java.util.Set.of("RETURN_TO_POOL", "KEEP_IN_WEEK")
                        .contains(handling == null ? "" : handling)) {
            throw BusinessException.conflict("请明确选择退回任务池或保留在本周队列");
        }
        while (true) {
            var batch = assignments.page(userId, q);
            if (batch.isEmpty()) {
                break;
            }
            for (var assignment : batch) {
                var task = ctx.scope("task", userId, assignment.getTaskId(), true);
                if ("KEEP_IN_WEEK".equals(handling) && "COMPLETED".equals(task.getStatus())) {
                    throw BusinessException.conflict("已完成任务不能重新安排到周队列");
                }
                assignment.setEndedAt(Instant.now());
                assignment.setEndReason("DAILY_PLAN_DELETED");
                ctx.changed(assignments.update(assignment));
                if ("KEEP_IN_WEEK".equals(handling)) {
                    TaskAssignmentEntity replacement = new TaskAssignmentEntity();
                    replacement.setUserId(userId);
                    replacement.setTaskId(assignment.getTaskId());
                    replacement.setWeeklyPlanId(assignment.getWeeklyPlanId());
                    replacement.setSortOrder(assignment.getSortOrder());
                    replacement.setAssignedAt(Instant.now());
                    assignments.insert(replacement);
                }
            }
        }
        ctx.changed(mapper.softDelete(row));
    }

    private DailyPlanEntity required(long userId, long id) {
        DailyPlanEntity row = mapper.findById(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private DailyPlanVO toVO(DailyPlanEntity row) {
        return new DailyPlanVO(
                ctx.id(row.getId()),
                ctx.id(row.getWeeklyPlanId()),
                row.getPlanDate(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
