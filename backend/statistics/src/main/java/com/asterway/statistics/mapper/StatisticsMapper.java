/*
 * 声明统计数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.mapper;

// 引入成长业务数据范围数据（GrowthScopeData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GrowthScopeData;
// 引入统计区间（StatisticsRangeDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsRangeDTO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.vo.*;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 只读事实统计，全部语句使用绑定参数并限定当前用户。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 StatisticsMapper；声明统计数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface StatisticsMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 首次日期（firstDate）方法：调用 firstDate 完成首次日期处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 时区（zone），使用 String 类型保存；可为 null，表示尚未提供。
    LocalDate firstDate(@Param("userId") long userId, @Param("zone") String zone);

    // 数据范围（scope）方法：调用 scope 完成数据范围处理；参数负责传入本次处理所需的数据。
    GrowthScopeData scope(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 类型（type），使用 String 类型保存；可为 null，表示尚未提供。
            // 编号（id），使用 long 类型保存。
            @Param("userId") long userId, @Param("type") String type, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 执行计时（execution）方法：调用 execution 完成执行计时处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    StatisticsExecutionVO execution(@Param("ctx") StatisticsRangeDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 执行计时日列表（executionDays）方法：调用 executionDays 完成执行计时日列表处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    List<StatisticsExecutionDayVO> executionDays(@Param("ctx") StatisticsRangeDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 习惯Totals（habitTotals）方法：调用 habitTotals 完成习惯Totals处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    StatisticsHabitTotalsVO habitTotals(@Param("ctx") StatisticsRangeDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 习惯日列表（habitDays）方法：调用 habitDays 完成习惯日列表处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    List<StatisticsHabitDayVO> habitDays(@Param("ctx") StatisticsRangeDTO context);

    // 习惯分组列表（habitGroups）方法：调用 habitGroups 完成习惯分组列表处理；参数负责传入本次处理所需的数据。
    List<StatisticsHabitGroupVO> habitGroups(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 StatisticsRangeDTO 类型保存。
            @Param("ctx") StatisticsRangeDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分组按（groupBy），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("groupBy") String groupBy,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 习惯分组数量（habitGroupCount）方法：调用 habitGroupCount 完成习惯分组数量处理；参数负责传入本次处理所需的数据。
    long habitGroupCount(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 StatisticsRangeDTO 类型保存。
            // 分组按（groupBy），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("ctx") StatisticsRangeDTO context, @Param("groupBy") String groupBy);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 当前状态日列表（stateDays）方法：调用 stateDays 完成当前状态日列表处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    List<StatisticsStateDayVO> stateDays(@Param("ctx") StatisticsRangeDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 当前状态Totals（stateTotals）方法：调用 stateTotals 完成当前状态Totals处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    StatisticsStateTotalsVO stateTotals(@Param("ctx") StatisticsRangeDTO context);

    // 成长时间线（timeline）方法：调用 timeline 完成成长时间线处理；参数负责传入本次处理所需的数据。
    List<StatisticsTimelineVO> timeline(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 StatisticsRangeDTO 类型保存。
            @Param("ctx") StatisticsRangeDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 成长时间线数量（timelineCount）方法：调用 timelineCount 完成成长时间线数量处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    long timelineCount(@Param("ctx") StatisticsRangeDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // objects方法：调用 objects 完成objects处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 StatisticsRangeDTO 类型保存。
    List<StatisticsObjectVO> objects(@Param("ctx") StatisticsRangeDTO context);
}
