package com.asterway.dashboard.service;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
import com.asterway.dashboard.vo.*;

/** 今日看板只读链路，不通过写操作生成看板数据。 */
public interface DashboardService {
    /** 汇总今日计划、习惯、实际投入、未读数和当前执行。 */
    DashboardOverviewVO overview();

    /** 分页列出今日有日期任务及本周未定日任务。 */
    PageResultVO<DashboardTaskVO> tasks(DashboardTaskQueryDTO query);

    /** 分页读取当前未归档目标及精确权重进度。 */
    PageResultVO<DashboardGoalVO> goals(PageQueryDTO query);

    /** 分页读取今日习惯，已有完成事实优先保留。 */
    PageResultVO<DashboardHabitVO> habits(PageQueryDTO query);
}
