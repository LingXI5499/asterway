package com.asterway.statistics.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.common.utils.UserCalendarUtils;
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
import com.asterway.statistics.dto.StatisticsQueryDTO;
import com.asterway.statistics.dto.StatisticsRangeDTO;
import com.asterway.statistics.mapper.StatisticsMapper;
import com.asterway.statistics.service.StatisticsService;
import com.asterway.statistics.vo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 周期边界、权限和统计组合；各事实先单独聚合，避免联结放大分子分母。 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsMapper mapper;
    private final GrowthContextUtils access;
    private final UserCalendarUtils calendar;

    public StatisticsServiceImpl(
            StatisticsMapper mapper, GrowthContextUtils access, UserCalendarUtils calendar) {
        this.mapper = mapper;
        this.access = access;
        this.calendar = calendar;
    }

    @Override
    public StatisticsSummaryVO summary(StatisticsQueryDTO query) {
        var range = range(query);
        return new StatisticsSummaryVO(
                range.period(),
                range.dateFrom(),
                range.dateTo(),
                range.timeZone(),
                range.now(),
                access.id(range.goalId()),
                access.id(range.stageId()),
                access.id(range.taskId()),
                mapper.execution(range),
                mapper.habitTotals(range),
                mapper.stateTotals(range));
    }

    @Override
    public PageResultVO<StatisticsTrendVO> trends(StatisticsQueryDTO query) {
        var range = range(query);
        long total = ChronoUnit.DAYS.between(range.dateFrom(), range.dateTo()) + 1;
        int offset = query.offset();
        if (offset >= total) {
            return new PageResultVO<>(List.of(), total, query.getPage(), query.getSize());
        }
        // 按日期分页后再查询本页事实，避免一次返回多年逐日曲线。
        LocalDate from = range.dateFrom().plusDays(offset);
        LocalDate to = from.plusDays(query.getSize() - 1L);
        if (to.isAfter(range.dateTo())) {
            to = range.dateTo();
        }
        var slice =
                new StatisticsRangeDTO(
                        range.userId(),
                        range.period(),
                        from,
                        to,
                        range.timeZone(),
                        from.atStartOfDay(java.time.ZoneId.of(range.timeZone())).toInstant(),
                        to.plusDays(1)
                                .atStartOfDay(java.time.ZoneId.of(range.timeZone()))
                                .toInstant(),
                        range.now(),
                        range.goalId(),
                        range.stageId(),
                        range.taskId());
        var executions =
                mapper.executionDays(slice).stream()
                        .collect(
                                Collectors.toMap(
                                        StatisticsExecutionDayVO::businessDate,
                                        Function.identity()));
        var habits =
                mapper.habitDays(slice).stream()
                        .collect(
                                Collectors.toMap(
                                        StatisticsHabitDayVO::businessDate, Function.identity()));
        var states =
                mapper.stateDays(slice).stream()
                        .collect(
                                Collectors.toMap(
                                        StatisticsStateDayVO::businessDate, Function.identity()));
        List<StatisticsTrendVO> result = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var execution =
                    executions.getOrDefault(date, new StatisticsExecutionDayVO(date, 0, 0, 0, 0));
            var habit = habits.getOrDefault(date, new StatisticsHabitDayVO(date, 0, 0, null));
            var state = states.getOrDefault(date, new StatisticsStateDayVO(date, 0, 0, 0, 0, 0, 0));
            result.add(
                    new StatisticsTrendVO(
                            date,
                            execution.actualMilliseconds(),
                            execution.actualSeconds(),
                            execution.sessions(),
                            habit.doneCount(),
                            habit.dueCount(),
                            habit.completionRate(),
                            state.goalCompletionActions(),
                            state.stageCompletionActions(),
                            state.taskCompletionActions(),
                            state.completedDistinctTasks(),
                            state.goalArchiveActions(),
                            state.goalRestoreActions()));
        }
        return new PageResultVO<>(result, total, query.getPage(), query.getSize());
    }

    @Override
    public PageResultVO<StatisticsTimelineVO> timeline(StatisticsQueryDTO query) {
        var range = range(query);
        return new PageResultVO<>(
                mapper.timeline(range, query.offset(), query.getSize()),
                mapper.timelineCount(range),
                query.getPage(),
                query.getSize());
    }

    @Override
    public StatisticsHabitReportVO habits(StatisticsHabitQueryDTO query) {
        var range = range(query);
        String group =
                query.getGroupBy() == null ? "HABIT" : query.getGroupBy().toUpperCase(Locale.ROOT);
        if (!Set.of("HABIT", "CATEGORY").contains(group)) {
            throw BusinessException.badRequest("习惯统计只支持 HABIT 或 CATEGORY 分组");
        }
        var items =
                new PageResultVO<>(
                        mapper.habitGroups(range, group, query.offset(), query.getSize()),
                        mapper.habitGroupCount(range, group),
                        query.getPage(),
                        query.getSize());
        return new StatisticsHabitReportVO(
                range.period(),
                range.dateFrom(),
                range.dateTo(),
                range.timeZone(),
                group,
                mapper.habitTotals(range),
                items);
    }

    @Override
    public List<StatisticsObjectVO> objects(StatisticsQueryDTO query) {
        return mapper.objects(range(query));
    }

    private StatisticsRangeDTO range(StatisticsQueryDTO query) {
        long userId = access.read("statistics:report:read");
        query.offset();
        var day = calendar.read(userId);
        String period =
                query.getPeriod() == null ? "MONTH" : query.getPeriod().toUpperCase(Locale.ROOT);
        if (!Set.of("TOTAL", "WEEK", "MONTH", "YEAR", "RANGE").contains(period)) {
            throw BusinessException.badRequest("统计周期无效");
        }
        LocalDate anchor = query.getAnchorDate() == null ? day.today() : query.getAnchorDate();
        if (anchor.isAfter(day.today())) {
            throw BusinessException.badRequest("不能查询未来统计周期");
        }
        LocalDate from;
        LocalDate to;
        switch (period) {
            case "TOTAL" -> {
                from = mapper.firstDate(userId, day.zone().getId());
                if (from == null || from.isAfter(day.today())) {
                    from = day.today();
                }
                to = day.today();
            }
            case "WEEK" -> {
                from = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                to = from.plusDays(6);
            }
            case "MONTH" -> {
                from = anchor.withDayOfMonth(1);
                to = from.plusMonths(1).minusDays(1);
            }
            case "YEAR" -> {
                from = anchor.withDayOfYear(1);
                to = from.plusYears(1).minusDays(1);
            }
            case "RANGE" -> {
                from = query.getDateFrom();
                to = query.getDateTo();
                if (from == null || to == null || from.isAfter(to) || to.isAfter(day.today())) {
                    throw BusinessException.badRequest("自定义起止日期须同时提供，顺序正确且不能在未来");
                }
            }
            default -> throw new IllegalStateException("统计周期未处理");
        }
        if (!"RANGE".equals(period) && (query.getDateFrom() != null || query.getDateTo() != null)) {
            throw BusinessException.badRequest("指定起止日期时请使用 RANGE 周期");
        }
        if (to.isAfter(day.today())) {
            to = day.today();
        }
        Long goalId = positive(query.getGoalId());
        Long stageId = positive(query.getStageId());
        Long taskId = positive(query.getTaskId());
        if (taskId != null) {
            var scope = mapper.scope(userId, "TASK", taskId);
            if (scope == null) {
                throw BusinessException.notFound();
            }
            if (stageId != null && !Objects.equals(stageId, scope.getStageId())
                    || goalId != null && !Objects.equals(goalId, scope.getGoalId())) {
                throw BusinessException.badRequest("任务与指定阶段或目标不匹配");
            }
            stageId = scope.getStageId();
            goalId = scope.getGoalId();
        }
        if (stageId != null) {
            var scope = mapper.scope(userId, "STAGE", stageId);
            if (scope == null) {
                throw BusinessException.notFound();
            }
            if (goalId != null && !Objects.equals(goalId, scope.getGoalId())) {
                throw BusinessException.badRequest("阶段与指定目标不匹配");
            }
            goalId = scope.getGoalId();
        }
        if (goalId != null && mapper.scope(userId, "GOAL", goalId) == null) {
            throw BusinessException.notFound();
        }
        return new StatisticsRangeDTO(
                userId,
                period,
                from,
                to,
                day.zone().getId(),
                day.start(from),
                day.start(to.plusDays(1)),
                day.now(),
                goalId,
                stageId,
                taskId);
    }

    private Long positive(Long id) {
        if (id != null && id <= 0) {
            throw BusinessException.badRequest("资源标识必须为正数");
        }
        return id;
    }
}
