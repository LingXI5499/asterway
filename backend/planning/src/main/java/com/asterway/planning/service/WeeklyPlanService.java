package com.asterway.planning.service;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.WeeklyPlanVO;

import java.time.LocalDate;

/** WeeklyPlan 的完整业务链路。 */
public interface WeeklyPlanService {
    /** 当前用户范围内的资源操作。 */
    PageResultVO<WeeklyPlanVO> page(GrowthQueryDTO query);

    /** 当前用户范围内的资源操作。 */
    WeeklyPlanVO get(long id);

    /** 当前用户范围内的资源操作。 */
    WeeklyPlanVO byWeek(LocalDate date, boolean create);

    /** 当前用户范围内的资源操作。 */
    PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query);

    /** 当前用户范围内的资源操作。 */
    void delete(long id, long version, String handling);
}
