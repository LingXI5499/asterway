/*
 * 规定统计业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入统计习惯查询条件（StatisticsHabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
// 引入统计查询条件（StatisticsQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsQueryDTO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.vo.*;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 基于真实业务事实的历史统计，不提供人工修改统计值的接口。 */
// 定义接口 StatisticsService；规定统计业务服务可供其他代码调用的业务方法。
public interface StatisticsService {
    /* 按总／周／月／年／自定义周期汇总真实投入、习惯和状态动作。 */
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    StatisticsSummaryVO summary(StatisticsQueryDTO query);

    /* 按日期分页返回逐日趋势；不从当前状态推算过去完成。 */
    // 趋势列表（trends）方法：调用 trends 完成趋势列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    PageResultVO<StatisticsTrendVO> trends(StatisticsQueryDTO query);

    /* 分页读取真实状态、执行、打卡和安排时间线。 */
    // 成长时间线（timeline）方法：调用 timeline 完成成长时间线处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    PageResultVO<StatisticsTimelineVO> timeline(StatisticsQueryDTO query);

    /* 计算习惯完成率并按习惯或历史分类归属分页。 */
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsHabitQueryDTO 类型保存。
    StatisticsHabitReportVO habits(StatisticsHabitQueryDTO query);

    /* 读取当前有效未归档对象状态分布，独立于历史完成动作。 */
    // objects方法：调用 objects 完成objects处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    List<StatisticsObjectVO> objects(StatisticsQueryDTO query);
}
