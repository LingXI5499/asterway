/*
 * 声明成长业务Support数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.mapper;

// 引入成长业务数据范围数据（GrowthScopeData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GrowthScopeData;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* 仅提供跨模块归属、用户锁与关联处理，SQL 全部位于 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 GrowthSupportMapper；声明成长业务Support数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface GrowthSupportMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 加锁用户（lockUser）方法：调用 lockUser 完成加锁用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    Long lockUser(@Param("userId") long userId);

    /* 账户生命周期也需要锁定停用用户，不能使用仅有效账号的普通写入口。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 加锁Any用户（lockAnyUser）方法：调用 lockAnyUser 完成加锁Any用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    Long lockAnyUser(@Param("userId") long userId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // calendar账号（calendarAccount）方法：调用 calendarAccount 完成calendar账号处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    com.asterway.common.security.ExecutionAccountData calendarAccount(@Param("userId") long userId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 用户使用的时区（timeZone）方法：调用 timeZone 完成用户使用的时区处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    String timeZone(@Param("userId") long userId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 目标（goal）方法：调用 goal 完成目标处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    GrowthScopeData goal(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 阶段（stage）方法：调用 stage 完成阶段处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    GrowthScopeData stage(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 任务（task）方法：调用 task 完成任务处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    GrowthScopeData task(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 周（week）方法：调用 week 完成周处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    GrowthScopeData week(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 日（day）方法：调用 day 完成日处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    GrowthScopeData day(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 阶段列表（stages）方法：调用 stages 完成阶段列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    long stages(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    long tasks(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 结束任务（endTask）方法：调用 endTask 完成结束任务处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    int endTask(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 结束目标（endGoal）方法：调用 endGoal 完成结束目标处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    int endGoal(@Param("userId") long userId, @Param("id") long id);

    // 阶段Overview（stageOverview）方法：调用 stageOverview 完成阶段Overview处理；参数负责传入本次处理所需的数据。
    java.util.List<com.asterway.common.result.GrowthOverviewVO> stageOverview(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 编号（id），使用 long 类型保存。
            @Param("userId") long userId, @Param("id") long id);

    // 任务Overview（taskOverview）方法：调用 taskOverview 完成任务Overview处理；参数负责传入本次处理所需的数据。
    java.util.List<com.asterway.common.result.GrowthOverviewVO> taskOverview(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 编号（id），使用 long 类型保存。
            @Param("userId") long userId, @Param("id") long id);
}
