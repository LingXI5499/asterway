/*
 * 声明每日方案数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.mapper;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入每日方案（DailyPlanEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.entity.DailyPlanEntity;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* DailyPlan 单表持久化链路，SQL 位于对应 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 DailyPlanMapper；声明每日方案数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface DailyPlanMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找按编号（findById）方法：查找 按编号（ById）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    DailyPlanEntity findById(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    List<DailyPlanEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 DailyPlanEntity 类型保存。
    int insert(DailyPlanEntity row);

    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 DailyPlanEntity 类型保存。
    int update(DailyPlanEntity row);

    // soft删除（softDelete）方法：调用 softDelete 完成soft删除处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 DailyPlanEntity 类型保存。
    int softDelete(DailyPlanEntity row);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 按日期（byDate）方法：调用 byDate 完成按日期处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    DailyPlanEntity byDate(@Param("userId") long userId, @Param("date") LocalDate date);
}
