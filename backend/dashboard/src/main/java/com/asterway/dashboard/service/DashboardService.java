/*
 * 规定Dashboard业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.service;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入Dashboard任务查询条件（DashboardTaskQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.vo.*;

/* 今日看板只读链路，不通过写操作生成看板数据。 */
// 定义接口 DashboardService；规定Dashboard业务服务可供其他代码调用的业务方法。
public interface DashboardService {
    /* 汇总今日计划、习惯、实际投入、未读数和当前执行。 */
    // overview方法：调用 overview 完成overview处理；参数负责传入本次处理所需的数据。
    DashboardOverviewVO overview();

    /* 分页列出今日有日期任务及本周未定日任务。 */
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 DashboardTaskQueryDTO 类型保存。
    PageResultVO<DashboardTaskVO> tasks(DashboardTaskQueryDTO query);

    /* 分页读取当前未归档目标及精确权重进度。 */
    // 目标列表（goals）方法：调用 goals 完成目标列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<DashboardGoalVO> goals(PageQueryDTO query);

    /* 分页读取今日习惯，已有完成事实优先保留。 */
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<DashboardHabitVO> habits(PageQueryDTO query);
}
