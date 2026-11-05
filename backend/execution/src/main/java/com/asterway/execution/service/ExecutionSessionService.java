package com.asterway.execution.service;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.result.PageResultVO;
import com.asterway.execution.dto.ExecutionControlDTO;
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
import com.asterway.execution.dto.ExecutionQueryDTO;
import com.asterway.execution.dto.ExecutionStartDTO;
import com.asterway.execution.vo.ExecutionDetailVO;
import com.asterway.execution.vo.ExecutionSegmentVO;
import com.asterway.execution.vo.ExecutionSessionVO;
import com.asterway.execution.vo.ExecutionStartResultVO;
import com.asterway.execution.vo.ExecutionSummaryVO;

/** 执行会话完整业务链路；从属时间段不另建一套 CRUD Service。 */
public interface ExecutionSessionService {
    /** 开始任务，有暂停会话则恢复；返回是否新建以选择 HTTP 状态。 */
    ExecutionStartResultVO start(ExecutionStartDTO dto);

    /** 查询当前运行会话，没有时返回 null。 */
    ExecutionSessionVO current();

    /** 暂停当前段，同一状态的重复请求不改写时间。 */
    ExecutionSessionVO pause(long id, ExecutionControlDTO dto);

    /** 恢复暂停会话，重新验证任务和计划资格。 */
    ExecutionSessionVO resume(long id, ExecutionControlDTO dto);

    /** 结束会话，不自动完成任务，也不为暂停间隔补段。 */
    ExecutionSessionVO finish(long id, ExecutionControlDTO dto);

    /** 只允许当前控制端为当前开放段发送有效心跳。 */
    ExecutionSessionVO heartbeat(long id, ExecutionHeartbeatDTO dto);

    /** 分页查询本人执行记录，可按有效时间段与日期的交集筛选。 */
    PageResultVO<ExecutionSessionVO> page(ExecutionQueryDTO query);

    /** 读取会话和首屏时间段。 */
    ExecutionDetailVO get(long id);

    /** 通过父会话的 owner 校验后读取时间线分页。 */
    PageResultVO<ExecutionSegmentVO> segments(long id, PageQueryDTO query);

    /** 按历史口径读取任务执行，允许任务已逻辑删除。 */
    PageResultVO<ExecutionSessionVO> taskExecutions(long taskId, ExecutionQueryDTO query);

    /** 汇总指定任务／阶段／目标；scope 为空时汇总本人全部执行，日期默认今天。 */
    ExecutionSummaryVO summary(ExecutionScopeEnum scope, Long id, ExecutionQueryDTO query);

    /** 内部调度入口，逐用户短事务处理到期会话，无 HTTP 暴露。 */
    void scanExpired();
}
