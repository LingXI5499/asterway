package com.asterway.planning.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.planning.entity.WeeklyPlanEntity;
import com.asterway.planning.mapper.DailyPlanMapper;
import com.asterway.planning.mapper.PlanningTaskMapper;
import com.asterway.planning.mapper.TaskAssignmentMapper;
import com.asterway.planning.mapper.WeeklyPlanMapper;
import com.asterway.planning.service.WeeklyPlanService;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.WeeklyPlanVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

/** 周计划按用户和周一幂等创建；不绑定某一个目标。 */
@Service
public class WeeklyPlanServiceImpl implements WeeklyPlanService {
    private final WeeklyPlanMapper mapper;
    private final DailyPlanMapper days;
    private final TaskAssignmentMapper assignments;
    private final PlanningTaskMapper tasks;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;

    public WeeklyPlanServiceImpl(
            WeeklyPlanMapper mapper,
            DailyPlanMapper days,
            TaskAssignmentMapper assignments,
            PlanningTaskMapper tasks,
            GrowthContextUtils ctx, ExecutionLifecycleProvider execution) {
        this.mapper = mapper;
        this.days = days;
        this.assignments = assignments;
        this.tasks = tasks;
        this.ctx = ctx;
        this.execution = execution;
    }

    @Override
    public PageResultVO<WeeklyPlanVO> page(GrowthQueryDTO query) {
        long userId = ctx.read("planning:weekly:list");
        ctx.prepare(query, userId);
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(this::toVO).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public WeeklyPlanVO get(long id) {
        long userId = ctx.read("planning:weekly:read");
        return toVO(required(userId, id));
    }

    @Override
    @Transactional
    public WeeklyPlanVO byWeek(LocalDate date, boolean create) {
        long userId =
                create ? ctx.write("planning:weekly:manage") : ctx.read("planning:weekly:read");
        if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw BusinessException.badRequest("周计划日期必须为星期一");
        }
        WeeklyPlanEntity row = mapper.byDate(userId, date);
        if (row == null && create) {
            if (date.plusDays(6).isBefore(ctx.today(userId))) {
                throw BusinessException.conflict("不能新建已结束周的计划");
            }
            row = new WeeklyPlanEntity();
            row.setUserId(userId);
            row.setWeekStartDate(date);
            mapper.insert(row);
            row = required(userId, row.getId());
        }
        return row == null ? null : toVO(row);
    }

    @Override
    public PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query) {
        long userId = ctx.read("planning:weekly:read");
        required(userId, id);
        ctx.prepare(query, userId);
        query.setWeeklyPlanId(id);
        return new PageResultVO<>(
                tasks.page(userId, query, false),
                tasks.count(userId, query, false),
                query.getPage(),
                query.getSize());
    }

    @Override
    @Transactional
    public void delete(long id, long version, String handling) {
        long userId = ctx.write("planning:weekly:manage");
        WeeklyPlanEntity row = required(userId, id);
        ctx.version(row.getVersion(), version);
        execution.requireNoRunning(userId, ExecutionScopeEnum.WEEK, id);
        if (row.getWeekStartDate().plusDays(6).isBefore(ctx.today(userId))) {
            throw BusinessException.conflict("已结束的周计划只读");
        }
        GrowthQueryDTO q = new GrowthQueryDTO();
        q.setWeeklyPlanId(id);
        q.setSize(100);
        q.setCurrentOnly(true);
        boolean hasChildren = assignments.count(userId, q) > 0 || days.count(userId, q) > 0;
        if (hasChildren && !"RETURN_TO_POOL".equals(handling)) {
            throw BusinessException.conflict("请明确确认退回任务池并处理本周日计划");
        }
        // 每轮读取第一页，避免在结束/删除记录后使用偏移量跳过数据。
        while (true) {
            var batch = assignments.page(userId, q);
            if (batch.isEmpty()) {
                break;
            }
            for (var assignment : batch) {
                ctx.scope("task", userId, assignment.getTaskId(), true);
                assignment.setEndedAt(Instant.now());
                assignment.setEndReason("WEEKLY_PLAN_DELETED");
                ctx.changed(assignments.update(assignment));
            }
        }
        while (true) {
            var batch = days.page(userId, q);
            if (batch.isEmpty()) {
                break;
            }
            for (var day : batch) {
                if (day.getPlanDate().isBefore(ctx.today(userId))) {
                    throw BusinessException.conflict("本周包含过去的日计划，不能普通删除");
                }
                ctx.changed(days.softDelete(day));
            }
        }
        ctx.changed(mapper.softDelete(row));
    }

    private WeeklyPlanEntity required(long userId, long id) {
        WeeklyPlanEntity row = mapper.findById(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private WeeklyPlanVO toVO(WeeklyPlanEntity row) {
        return new WeeklyPlanVO(
                ctx.id(row.getId()),
                row.getWeekStartDate(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
