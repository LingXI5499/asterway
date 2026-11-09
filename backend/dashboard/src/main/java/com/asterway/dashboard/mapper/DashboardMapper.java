/*
 * 声明Dashboard数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.mapper;

// 引入Dashboard读取（DashboardReadDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.dto.DashboardReadDTO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.vo.*;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 只读看板 Mapper；各类事实独立聚合，避免多表一对多联结重复计数。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 DashboardMapper；声明Dashboard数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface DashboardMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // overview方法：调用 overview 完成overview处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 DashboardReadDTO 类型保存。
    DashboardMetricsVO overview(@Param("ctx") DashboardReadDTO context);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 DashboardReadDTO 类型保存。
    DashboardExecutionVO current(@Param("ctx") DashboardReadDTO context);

    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    List<DashboardTaskVO> tasks(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 DashboardReadDTO 类型保存。
            @Param("ctx") DashboardReadDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // section，使用 String 类型保存；可为 null，表示尚未提供。
            @Param("section") String section,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 任务列表数量（tasksCount）方法：调用 tasksCount 完成任务列表数量处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 DashboardReadDTO 类型保存。
    // section，使用 String 类型保存；可为 null，表示尚未提供。
    long tasksCount(@Param("ctx") DashboardReadDTO context, @Param("section") String section);

    // 目标列表（goals）方法：调用 goals 完成目标列表处理；参数负责传入本次处理所需的数据。
    List<DashboardGoalVO> goals(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 DashboardReadDTO 类型保存。
            @Param("ctx") DashboardReadDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 目标列表数量（goalsCount）方法：调用 goalsCount 完成目标列表数量处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 DashboardReadDTO 类型保存。
    long goalsCount(@Param("ctx") DashboardReadDTO context);

    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    List<DashboardHabitVO> habits(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 DashboardReadDTO 类型保存。
            @Param("ctx") DashboardReadDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 习惯列表数量（habitsCount）方法：调用 habitsCount 完成习惯列表数量处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 DashboardReadDTO 类型保存。
    long habitsCount(@Param("ctx") DashboardReadDTO context);
}
