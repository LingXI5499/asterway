/*
 * 实现统计业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入用户CalendarUtils（UserCalendarUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.UserCalendarUtils;
// 引入统计习惯查询条件（StatisticsHabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
// 引入统计查询条件（StatisticsQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsQueryDTO;
// 引入统计区间（StatisticsRangeDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsRangeDTO;
// 引入统计数据库读写器（StatisticsMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.mapper.StatisticsMapper;
// 引入统计业务服务（StatisticsService），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.service.StatisticsService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.vo.*;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Isolation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Isolation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入日Of周（DayOfWeek），下面可以直接使用这个类型或静态成员。
import java.time.DayOfWeek;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;
// 引入TemporalAdjusters，下面可以直接使用这个类型或静态成员。
import java.time.temporal.TemporalAdjusters;
// 引入Array列表（ArrayList），下面可以直接使用这个类型或静态成员。
import java.util.ArrayList;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Locale，下面可以直接使用这个类型或静态成员。
import java.util.Locale;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入Function，下面可以直接使用这个类型或静态成员。
import java.util.function.Function;
// 引入Collectors，下面可以直接使用这个类型或静态成员。
import java.util.stream.Collectors;

/* 周期边界、权限和统计组合；各事实先单独聚合，避免联结放大分子分母。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 StatisticsServiceImpl；实现统计业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 把下面的修改放进事务，成功一起提交，异常按配置回滚。
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class StatisticsServiceImpl implements StatisticsService {
    // 访问数据库的 Mapper（mapper），使用 StatisticsMapper 类型保存。
    private final StatisticsMapper mapper;
    // 访问权限（access），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils access;
    // calendar，使用 UserCalendarUtils 类型保存。
    private final UserCalendarUtils calendar;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public StatisticsServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 StatisticsMapper 类型保存。
            // 访问权限（access），使用 GrowthContextUtils 类型保存。
            // calendar，使用 UserCalendarUtils 类型保存。
            StatisticsMapper mapper, GrowthContextUtils access, UserCalendarUtils calendar) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把访问权限（access）保存到 this.access。
        this.access = access;
        // 把calendar保存到 this.calendar。
        this.calendar = calendar;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    public StatisticsSummaryVO summary(StatisticsQueryDTO query) {
        // 准备区间（range），类型为 null；初始内容是调用 range 完成区间处理。
        var range = range(query);
        // 把新建 统计汇总（StatisticsSummaryVO） 实例交还给调用方，本方法到此结束。
        return new StatisticsSummaryVO(
                // 调用 range.period 完成统计周期处理。
                range.period(),
                // 调用 range.dateFrom 完成日期来源处理。
                range.dateFrom(),
                // 调用 range.dateTo 完成日期To处理。
                range.dateTo(),
                // 调用 range.timeZone 完成用户使用的时区处理。
                range.timeZone(),
                // 调用 range.now 完成当前时间处理。
                range.now(),
                // 调用 access.id 完成编号处理。
                access.id(range.goalId()),
                // 调用 access.id 完成编号处理。
                access.id(range.stageId()),
                // 调用 access.id 完成编号处理。
                access.id(range.taskId()),
                // 调用 mapper.execution 完成执行计时处理。
                mapper.execution(range),
                // 调用 mapper.habitTotals 完成习惯Totals处理。
                mapper.habitTotals(range),
                // 调用 mapper.stateTotals 完成当前状态Totals处理。
                mapper.stateTotals(range));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 趋势列表（trends）方法：调用 trends 完成趋势列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    public PageResultVO<StatisticsTrendVO> trends(StatisticsQueryDTO query) {
        // 准备区间（range），类型为 null；初始内容是调用 range 完成区间处理。
        var range = range(query);
        // 准备总数（total），类型为 long；初始内容是调用 ChronoUnit.DAYS.between 完成between处理。
        long total = ChronoUnit.DAYS.between(range.dateFrom(), range.dateTo()) + 1;
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 检查条件 offset >= total 是否成立；成立时执行下面的分支。
        if (offset >= total) {
            // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
            return new PageResultVO<>(List.of(), total, query.getPage(), query.getSize());
        }
        // 按日期分页后再查询本页事实，避免一次返回多年逐日曲线。
        // 准备来源（from），类型为 LocalDate；初始内容是调用 range.dateFrom 完成日期来源处理。
        LocalDate from = range.dateFrom().plusDays(offset);
        // 准备to，类型为 LocalDate；初始内容是调用 from.plusDays 完成plus日列表处理。
        LocalDate to = from.plusDays(query.getSize() - 1L);
        // 检查条件 to.isAfter(range.dateTo()) 是否成立；成立时执行下面的分支。
        if (to.isAfter(range.dateTo())) {
            // 把调用 range.dateTo 完成日期To处理保存到 to。
            to = range.dateTo();
        }
        // 准备slice，类型为 null；初始内容是新建 统计区间（StatisticsRangeDTO） 实例。
        var slice =
                // 继续传入或组合新建 统计区间（StatisticsRangeDTO） 实例，完成上面尚未结束的调用或声明。
                new StatisticsRangeDTO(
                        // 调用 range.userId 完成用户编号处理。
                        range.userId(),
                        // 调用 range.period 完成统计周期处理。
                        range.period(),
                        // 继续传入或组合来源（from），完成上面尚未结束的调用或声明。
                        from,
                        // 继续传入或组合to，完成上面尚未结束的调用或声明。
                        to,
                        // 调用 range.timeZone 完成用户使用的时区处理。
                        range.timeZone(),
                        // 调用 from.atStartOfDay 完成at开始Of日处理。
                        from.atStartOfDay(java.time.ZoneId.of(range.timeZone())).toInstant(),
                        // 调用 to.plusDays 完成plus日列表处理。
                        to.plusDays(1)
                                // 继续对上一步结果执行调用 atStartOfDay(java.time.ZoneId.of 完成of处理。
                                .atStartOfDay(java.time.ZoneId.of(range.timeZone()))
                                // 继续对上一步结果执行调用 toInstant 完成toInstant处理。
                                .toInstant(),
                        // 调用 range.now 完成当前时间处理。
                        range.now(),
                        // 调用 range.goalId 完成目标编号处理。
                        range.goalId(),
                        // 调用 range.stageId 完成阶段编号处理。
                        range.stageId(),
                        // 调用 range.taskId 完成任务编号处理。
                        range.taskId());
        // 准备执行记录（executions），类型为 null；初始内容是把前面筛选、转换后的结果汇成列表。
        var executions =
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                mapper.executionDays(slice).stream()
                        // 继续对上一步结果执行调用 collect 完成collect处理。
                        .collect(
                                // 继续传入或组合调用 Collectors.toMap 完成to对应表处理，完成上面尚未结束的调用或声明。
                                Collectors.toMap(
                                        // 继续传入或组合表达式 StatisticsExecutionDayVO::businessDate 的结果，完成上面尚未结束的调用或声明。
                                        StatisticsExecutionDayVO::businessDate,
                                        // 调用 Function.identity 完成账户管理处理。
                                        Function.identity()));
        // 准备习惯列表（habits），类型为 null；初始内容是把前面筛选、转换后的结果汇成列表。
        var habits =
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                mapper.habitDays(slice).stream()
                        // 继续对上一步结果执行调用 collect 完成collect处理。
                        .collect(
                                // 继续传入或组合调用 Collectors.toMap 完成to对应表处理，完成上面尚未结束的调用或声明。
                                Collectors.toMap(
                                        // 继续传入或组合调用 StatisticsHabitDayVO::businessDate, Function.identity 完成账户管理处理，完成上面尚未结束的调用或声明。
                                        StatisticsHabitDayVO::businessDate, Function.identity()));
        // 准备states，类型为 null；初始内容是把前面筛选、转换后的结果汇成列表。
        var states =
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                mapper.stateDays(slice).stream()
                        // 继续对上一步结果执行调用 collect 完成collect处理。
                        .collect(
                                // 继续传入或组合调用 Collectors.toMap 完成to对应表处理，完成上面尚未结束的调用或声明。
                                Collectors.toMap(
                                        // 继续传入或组合调用 StatisticsStateDayVO::businessDate, Function.identity 完成账户管理处理，完成上面尚未结束的调用或声明。
                                        StatisticsStateDayVO::businessDate, Function.identity()));
        // 准备结果（result），类型为 List<StatisticsTrendVO>；初始内容是一个空列表，用来保存后续多条数据。
        List<StatisticsTrendVO> result = new ArrayList<>();
        // 准备日期（date），类型为 LocalDate；初始内容是来源（from）。
        // 逐次执行循环：for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1))；每轮处理一条数据或推进一次检查。
        // 把调用 date.plusDays 完成plus日列表处理保存到 date。
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            // 准备执行计时（execution），类型为 null；初始内容是读取 Or默认值（OrDefault） 的值。
            var execution =
                    // 读取 Or默认值（OrDefault） 的值。
                    executions.getOrDefault(date, new StatisticsExecutionDayVO(date, 0, 0, 0, 0));
            // 准备习惯（habit），类型为 null；初始内容是读取 Or默认值（OrDefault） 的值。
            var habit = habits.getOrDefault(date, new StatisticsHabitDayVO(date, 0, 0, null));
            // 准备当前状态（state），类型为 null；初始内容是读取 Or默认值（OrDefault） 的值。
            var state = states.getOrDefault(date, new StatisticsStateDayVO(date, 0, 0, 0, 0, 0, 0));
            // 加入集合或登记一条待保存记录。
            result.add(
                    // 继续传入或组合新建 统计趋势（StatisticsTrendVO） 实例，完成上面尚未结束的调用或声明。
                    new StatisticsTrendVO(
                            // 继续传入或组合日期（date），完成上面尚未结束的调用或声明。
                            date,
                            // 调用 execution.actualMilliseconds 完成实际Milliseconds处理。
                            execution.actualMilliseconds(),
                            // 调用 execution.actualSeconds 完成实际秒数处理。
                            execution.actualSeconds(),
                            // 调用 execution.sessions 完成会话列表处理。
                            execution.sessions(),
                            // 调用 habit.doneCount 完成已完成数量处理。
                            habit.doneCount(),
                            // 调用 habit.dueCount 完成应完成数量处理。
                            habit.dueCount(),
                            // 调用 habit.completionRate 完成完成情况频率处理。
                            habit.completionRate(),
                            // 调用 state.goalCompletionActions 完成目标完成情况Actions处理。
                            state.goalCompletionActions(),
                            // 调用 state.stageCompletionActions 完成阶段完成情况Actions处理。
                            state.stageCompletionActions(),
                            // 调用 state.taskCompletionActions 完成任务完成情况Actions处理。
                            state.taskCompletionActions(),
                            // 调用 state.completedDistinctTasks 完成已完成Distinct任务列表处理。
                            state.completedDistinctTasks(),
                            // 调用 state.goalArchiveActions 完成目标归档Actions处理。
                            state.goalArchiveActions(),
                            // 调用 state.goalRestoreActions 完成目标恢复Actions处理。
                            state.goalRestoreActions()));
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(result, total, query.getPage(), query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 成长时间线（timeline）方法：调用 timeline 完成成长时间线处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    public PageResultVO<StatisticsTimelineVO> timeline(StatisticsQueryDTO query) {
        // 准备区间（range），类型为 null；初始内容是调用 range 完成区间处理。
        var range = range(query);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 mapper.timeline 完成成长时间线处理。
                mapper.timeline(range, query.offset(), query.getSize()),
                // 调用 mapper.timelineCount 完成成长时间线数量处理。
                mapper.timelineCount(range),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 StatisticsHabitQueryDTO 类型保存。
    public StatisticsHabitReportVO habits(StatisticsHabitQueryDTO query) {
        // 准备区间（range），类型为 null；初始内容是调用 range 完成区间处理。
        var range = range(query);
        // 准备分组（group），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：query.getGroupBy() == null ? "HABIT" : query.getGroupBy().toUpperCase(Locale.ROOT)。
        String group =
                // 根据前面的条件，从两个候选结果中选择一个：query.getGroupBy() == null ? "HABIT" : query.getGroupBy().toUpperCase(Locale.ROOT)。
                query.getGroupBy() == null ? "HABIT" : query.getGroupBy().toUpperCase(Locale.ROOT);
        // 检查条件 !Set.of("HABIT", "CATEGORY").contains(group) 是否成立；成立时执行下面的分支。
        if (!Set.of("HABIT", "CATEGORY").contains(group)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("习惯统计只支持 HABIT 或 CATEGORY 分组");
        }
        // 准备数据列表（items），类型为 null；初始内容是新建 当前页结果（PageResultVO） 实例。
        var items =
                // 继续传入或组合新建 当前页结果（PageResultVO） 实例，完成上面尚未结束的调用或声明。
                new PageResultVO<>(
                        // 调用 mapper.habitGroups 完成习惯分组列表处理。
                        mapper.habitGroups(range, group, query.offset(), query.getSize()),
                        // 调用 mapper.habitGroupCount 完成习惯分组数量处理。
                        mapper.habitGroupCount(range, group),
                        // 读取 当前页（Page） 的值。
                        query.getPage(),
                        // 读取 长度或数量校验规则（Size） 的值。
                        query.getSize());
        // 把新建 统计习惯复盘报告（StatisticsHabitReportVO） 实例交还给调用方，本方法到此结束。
        return new StatisticsHabitReportVO(
                // 调用 range.period 完成统计周期处理。
                range.period(),
                // 调用 range.dateFrom 完成日期来源处理。
                range.dateFrom(),
                // 调用 range.dateTo 完成日期To处理。
                range.dateTo(),
                // 调用 range.timeZone 完成用户使用的时区处理。
                range.timeZone(),
                // 继续传入或组合分组（group），完成上面尚未结束的调用或声明。
                group,
                // 调用 mapper.habitTotals 完成习惯Totals处理。
                mapper.habitTotals(range),
                // 继续传入或组合表达式 items) 的结果，完成上面尚未结束的调用或声明。
                items);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // objects方法：调用 objects 完成objects处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    public List<StatisticsObjectVO> objects(StatisticsQueryDTO query) {
        // 把调用 mapper.objects 完成objects处理交还给调用方，本方法到此结束。
        return mapper.objects(range(query));
    }

    // 区间（range）方法：调用 range 完成区间处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    private StatisticsRangeDTO range(StatisticsQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是调用 access.read 完成读取处理。
        long userId = access.read("statistics:report:read");
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 准备日（day），类型为 null；初始内容是调用 calendar.read 完成读取处理。
        var day = calendar.read(userId);
        // 准备统计周期（period），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：query.getPeriod() == null ? "MONTH" : query.getPeriod().toUpperCase(Locale.ROOT)。
        String period =
                // 根据前面的条件，从两个候选结果中选择一个：query.getPeriod() == null ? "MONTH" : query.getPeriod().toUpperCase(Locale.ROOT)。
                query.getPeriod() == null ? "MONTH" : query.getPeriod().toUpperCase(Locale.ROOT);
        // 检查条件 !Set.of("TOTAL", "WEEK", "MONTH", "YEAR", "RANGE").contains(period) 是否成立；成立时执行下面的分支。
        if (!Set.of("TOTAL", "WEEK", "MONTH", "YEAR", "RANGE").contains(period)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("统计周期无效");
        }
        // 准备anchor，类型为 LocalDate；初始内容是根据前面的条件，从两个候选结果中选择一个：query.getAnchorDate() == null ? day.today() : query.getAnchorDate()。
        LocalDate anchor = query.getAnchorDate() == null ? day.today() : query.getAnchorDate();
        // 检查条件 anchor.isAfter(day.today()) 是否成立；成立时执行下面的分支。
        if (anchor.isAfter(day.today())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("不能查询未来统计周期");
        }
        // 来源（from），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate from;
        // to，使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate to;
        // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
        switch (period) {
            // 匹配这一种取值时使用本分支：case "TOTAL"。
            case "TOTAL" -> {
                // 把调用 mapper.firstDate 完成首次日期处理保存到 from。
                from = mapper.firstDate(userId, day.zone().getId());
                // 检查from == null || from.isAfter(day.today())，即对应内容是否不存在；成立时执行下面的分支。
                if (from == null || from.isAfter(day.today())) {
                    // 把调用 day.today 完成今天处理保存到 from。
                    from = day.today();
                }
                // 把调用 day.today 完成今天处理保存到 to。
                to = day.today();
            }
            // 匹配这一种取值时使用本分支：case "WEEK"。
            case "WEEK" -> {
                // 把调用 anchor.with 完成with处理保存到 from。
                from = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                // 把调用 from.plusDays 完成plus日列表处理保存到 to。
                to = from.plusDays(6);
            }
            // 匹配这一种取值时使用本分支：case "MONTH"。
            case "MONTH" -> {
                // 把调用 anchor.withDayOfMonth 完成with日Of月处理保存到 from。
                from = anchor.withDayOfMonth(1);
                // 把调用 from.plusMonths 完成plusMonths处理保存到 to。
                to = from.plusMonths(1).minusDays(1);
            }
            // 匹配这一种取值时使用本分支：case "YEAR"。
            case "YEAR" -> {
                // 把调用 anchor.withDayOfYear 完成with日Of年处理保存到 from。
                from = anchor.withDayOfYear(1);
                // 把调用 from.plusYears 完成plusYears处理保存到 to。
                to = from.plusYears(1).minusDays(1);
            }
            // 匹配这一种取值时使用本分支：case "RANGE"。
            case "RANGE" -> {
                // 把读取 日期来源（DateFrom） 的值保存到 from。
                from = query.getDateFrom();
                // 把读取 日期To（DateTo） 的值保存到 to。
                to = query.getDateTo();
                // 检查from == null || to == null || from.isAfter(to) || to.isAfter(day.today())，即对应内容是否不存在；成立时执行下面的分支。
                if (from == null || to == null || from.isAfter(to) || to.isAfter(day.today())) {
                    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                    throw BusinessException.badRequest("自定义起止日期须同时提供，顺序正确且不能在未来");
                }
            }
            // 匹配这一种取值时使用本分支：default。
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            default -> throw new IllegalStateException("统计周期未处理");
        }
        // 检查条件 !"RANGE".equals(period) && (query.getDateFrom() != null || query.getDateTo() != null) 是否成立；成立时执行下面的分支。
        if (!"RANGE".equals(period) && (query.getDateFrom() != null || query.getDateTo() != null)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("指定起止日期时请使用 RANGE 周期");
        }
        // 检查条件 to.isAfter(day.today()) 是否成立；成立时执行下面的分支。
        if (to.isAfter(day.today())) {
            // 把调用 day.today 完成今天处理保存到 to。
            to = day.today();
        }
        // 准备目标编号（goalId），类型为 Long；初始内容是读取 目标编号（GoalId） 的值。
        Long goalId = positive(query.getGoalId());
        // 准备阶段编号（stageId），类型为 Long；初始内容是读取 阶段编号（StageId） 的值。
        Long stageId = positive(query.getStageId());
        // 准备任务编号（taskId），类型为 Long；初始内容是读取 任务编号（TaskId） 的值。
        Long taskId = positive(query.getTaskId());
        // 检查条件 taskId != null 是否成立；成立时执行下面的分支。
        if (taskId != null) {
            // 准备数据范围（scope），类型为 null；初始内容是调用 mapper.scope 完成数据范围处理。
            var scope = mapper.scope(userId, "TASK", taskId);
            // 检查scope == null，即对应内容是否不存在；成立时执行下面的分支。
            if (scope == null) {
                // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
                throw BusinessException.notFound();
            }
            // 检查条件 stageId != null && !Objects.equals(stageId, scope.getStageId()) || goalId != null && !Objects.equals(goalId, scope.getGoalId()) 是否成立；成立时执行下面的分支。
            if (stageId != null && !Objects.equals(stageId, scope.getStageId())
                    // 继续补充同一个判断或备用结果：条件 || goalId != null && !Objects.equals(goalId, scope.getGoalId())) { 是否成立。
                    || goalId != null && !Objects.equals(goalId, scope.getGoalId())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("任务与指定阶段或目标不匹配");
            }
            // 把读取 阶段编号（StageId） 的值保存到 stageId。
            stageId = scope.getStageId();
            // 把读取 目标编号（GoalId） 的值保存到 goalId。
            goalId = scope.getGoalId();
        }
        // 检查条件 stageId != null 是否成立；成立时执行下面的分支。
        if (stageId != null) {
            // 准备数据范围（scope），类型为 null；初始内容是调用 mapper.scope 完成数据范围处理。
            var scope = mapper.scope(userId, "STAGE", stageId);
            // 检查scope == null，即对应内容是否不存在；成立时执行下面的分支。
            if (scope == null) {
                // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
                throw BusinessException.notFound();
            }
            // 检查条件 goalId != null && !Objects.equals(goalId, scope.getGoalId()) 是否成立；成立时执行下面的分支。
            if (goalId != null && !Objects.equals(goalId, scope.getGoalId())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("阶段与指定目标不匹配");
            }
            // 把读取 目标编号（GoalId） 的值保存到 goalId。
            goalId = scope.getGoalId();
        }
        // 检查goalId != null && mapper.scope(userId, "GOAL", goalId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (goalId != null && mapper.scope(userId, "GOAL", goalId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把新建 统计区间（StatisticsRangeDTO） 实例交还给调用方，本方法到此结束。
        return new StatisticsRangeDTO(
                // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
                userId,
                // 继续传入或组合统计周期（period），完成上面尚未结束的调用或声明。
                period,
                // 继续传入或组合来源（from），完成上面尚未结束的调用或声明。
                from,
                // 继续传入或组合to，完成上面尚未结束的调用或声明。
                to,
                // 调用 day.zone 完成时区处理。
                day.zone().getId(),
                // 调用 day.start 完成开始处理。
                day.start(from),
                // 调用 day.start 完成开始处理。
                day.start(to.plusDays(1)),
                // 调用 day.now 完成当前时间处理。
                day.now(),
                // 继续传入或组合目标编号（goalId），完成上面尚未结束的调用或声明。
                goalId,
                // 继续传入或组合阶段编号（stageId），完成上面尚未结束的调用或声明。
                stageId,
                // 继续传入或组合表达式 taskId) 的结果，完成上面尚未结束的调用或声明。
                taskId);
    }

    // 正数（positive）方法：调用 positive 完成正数处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long positive(Long id) {
        // 检查条件 id != null && id <= 0 是否成立；成立时执行下面的分支。
        if (id != null && id <= 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("资源标识必须为正数");
        }
        // 把编号（id）交还给调用方，本方法到此结束。
        return id;
    }
}
