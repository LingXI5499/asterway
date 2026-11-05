package com.asterway.goal.service;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.goal.dto.GoalArchiveDTO;
import com.asterway.goal.dto.GoalSaveDTO;
import com.asterway.goal.vo.GoalStateHistoryVO;
import com.asterway.goal.vo.GoalVO;

/** Goal 的完整业务链路，不按增删改查拆分。 */
public interface GoalService {
    /** 本人状态事实分页，归档或逻辑删除后仍保留历史读取。 */
    PageResultVO<GoalStateHistoryVO> history(long id, PageQueryDTO query);

    /** 当前用户范围内的业务操作。 */
    PageResultVO<GoalVO> page(GrowthQueryDTO query);

    /** 当前用户范围内的业务操作。 */
    GoalVO get(long id);

    /** 当前用户范围内的业务操作。 */
    GoalVO create(GoalSaveDTO dto);

    /** 当前用户范围内的业务操作。 */
    GoalVO update(long id, GoalSaveDTO dto);

    /** 当前用户范围内的业务操作。 */
    /** 完整替换资料，可清空可选日期。 */
    GoalVO replace(long id, GoalSaveDTO dto);

    GoalVO status(long id, GrowthStatusDTO dto);

    /** 当前用户范围内的业务操作。 */
    void delete(long id, long version);

    /** 当前用户范围内的业务操作。 */
    GoalVO archive(long id, GoalArchiveDTO dto);

    /** 当前用户范围内的业务操作。 */
    GoalVO restore(long id, long version);
}
