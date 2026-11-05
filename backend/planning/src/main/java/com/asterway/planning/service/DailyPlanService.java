package com.asterway.planning.service;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.planning.vo.DailyPlanVO;
import com.asterway.planning.vo.PlanningTaskVO;

import java.time.LocalDate;

/** DailyPlan 的完整业务链路。 */
public interface DailyPlanService {
    /** 当前用户范围内的资源操作。 */
    DailyPlanVO get(long id);

    /** 按日期只读查询，未创建时返回 null。 */
    DailyPlanVO findByDate(long weeklyPlanId, LocalDate date);

    /** 当前用户范围内的资源操作。 */
    DailyPlanVO byDate(long weeklyPlanId, LocalDate date);

    /** 当前用户范围内的资源操作。 */
    PageResultVO<DailyPlanVO> page(long weeklyPlanId, GrowthQueryDTO query);

    /** 当前用户范围内的资源操作。 */
    PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query);

    /** 当前用户范围内的资源操作。 */
    void delete(long id, long version, String handling);
}
