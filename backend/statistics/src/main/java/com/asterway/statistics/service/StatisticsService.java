package com.asterway.statistics.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
import com.asterway.statistics.dto.StatisticsQueryDTO;
import com.asterway.statistics.vo.*;

import java.util.List;

/** 基于真实业务事实的历史统计，不提供人工修改统计值的接口。 */
public interface StatisticsService {
    /** 按总／周／月／年／自定义周期汇总真实投入、习惯和状态动作。 */
    StatisticsSummaryVO summary(StatisticsQueryDTO query);

    /** 按日期分页返回逐日趋势；不从当前状态推算过去完成。 */
    PageResultVO<StatisticsTrendVO> trends(StatisticsQueryDTO query);

    /** 分页读取真实状态、执行、打卡和安排时间线。 */
    PageResultVO<StatisticsTimelineVO> timeline(StatisticsQueryDTO query);

    /** 计算习惯完成率并按习惯或历史分类归属分页。 */
    StatisticsHabitReportVO habits(StatisticsHabitQueryDTO query);

    /** 读取当前有效未归档对象状态分布，独立于历史完成动作。 */
    List<StatisticsObjectVO> objects(StatisticsQueryDTO query);
}
