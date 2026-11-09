/*
 * 规定每日方案业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.service;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入每日方案（DailyPlanVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.DailyPlanVO;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* DailyPlan 的完整业务链路。 */
// 定义接口 DailyPlanService；规定每日方案业务服务可供其他代码调用的业务方法。
public interface DailyPlanService {
    /* 当前用户范围内的资源操作。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    DailyPlanVO get(long id);

    /* 按日期只读查询，未创建时返回 null。 */
    // 查找按日期（findByDate）方法：查找 按日期（ByDate）；参数负责传入本次处理所需的数据。
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    DailyPlanVO findByDate(long weeklyPlanId, LocalDate date);

    /* 当前用户范围内的资源操作。 */
    // 按日期（byDate）方法：调用 byDate 完成按日期处理；参数负责传入本次处理所需的数据。
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    DailyPlanVO byDate(long weeklyPlanId, LocalDate date);

    /* 当前用户范围内的资源操作。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    PageResultVO<DailyPlanVO> page(long weeklyPlanId, GrowthQueryDTO query);

    /* 当前用户范围内的资源操作。 */
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query);

    /* 当前用户范围内的资源操作。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    // handling，使用 String 类型保存；可为 null，表示尚未提供。
    void delete(long id, long version, String handling);
}
