package com.asterway.planning.service;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.planning.dto.AssignmentSaveDTO;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.TaskAssignmentVO;

/** TaskAssignment 的完整业务链路。 */
public interface TaskAssignmentService {
    /** 当前用户范围内的资源操作。 */
    TaskAssignmentVO get(long taskId);

    /** 当前用户范围内的资源操作。 */
    TaskAssignmentVO assign(long taskId, AssignmentSaveDTO dto);

    /** 当前用户范围内的资源操作。 */
    void remove(long taskId, long assignmentId, long version);

    /** 当前用户范围内的资源操作。 */
    PageResultVO<TaskAssignmentVO> history(long taskId, GrowthQueryDTO query);

    /** 当前用户范围内的资源操作。 */
    PageResultVO<PlanningTaskVO> pending(GrowthQueryDTO query);
}
