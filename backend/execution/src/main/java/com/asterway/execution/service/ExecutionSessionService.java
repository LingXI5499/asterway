/*
 * 规定执行计时会话业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.service;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入执行计时数据范围Enum（ExecutionScopeEnum），下面可以直接使用这个类型或静态成员。
import com.asterway.common.enums.ExecutionScopeEnum;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入执行计时Control（ExecutionControlDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionControlDTO;
// 引入执行计时心跳（ExecutionHeartbeatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
// 引入执行计时查询条件（ExecutionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionQueryDTO;
// 引入执行计时开始（ExecutionStartDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionStartDTO;
// 引入执行计时详情（ExecutionDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionDetailVO;
// 引入执行计时连续计时段（ExecutionSegmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSegmentVO;
// 引入执行计时会话（ExecutionSessionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSessionVO;
// 引入执行计时开始结果（ExecutionStartResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionStartResultVO;
// 引入执行计时汇总（ExecutionSummaryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSummaryVO;

/* 执行会话完整业务链路；从属时间段不另建一套 CRUD Service。 */
// 定义接口 ExecutionSessionService；规定执行计时会话业务服务可供其他代码调用的业务方法。
public interface ExecutionSessionService {
    /* 开始任务，有暂停会话则恢复；返回是否新建以选择 HTTP 状态。 */
    // 开始（start）方法：调用 start 完成开始处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 ExecutionStartDTO 类型保存。
    ExecutionStartResultVO start(ExecutionStartDTO dto);

    /* 查询当前运行会话，没有时返回 null。 */
    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    ExecutionSessionVO current();

    /* 暂停当前段，同一状态的重复请求不改写时间。 */
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    ExecutionSessionVO pause(long id, ExecutionControlDTO dto);

    /* 恢复暂停会话，重新验证任务和计划资格。 */
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    ExecutionSessionVO resume(long id, ExecutionControlDTO dto);

    /* 结束会话，不自动完成任务，也不为暂停间隔补段。 */
    // finish方法：调用 finish 完成finish处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    ExecutionSessionVO finish(long id, ExecutionControlDTO dto);

    /* 只允许当前控制端为当前开放段发送有效心跳。 */
    // 心跳（heartbeat）方法：调用 heartbeat 完成心跳处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionHeartbeatDTO 类型保存。
    ExecutionSessionVO heartbeat(long id, ExecutionHeartbeatDTO dto);

    /* 分页查询本人执行记录，可按有效时间段与日期的交集筛选。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    PageResultVO<ExecutionSessionVO> page(ExecutionQueryDTO query);

    /* 读取会话和首屏时间段。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    ExecutionDetailVO get(long id);

    /* 通过父会话的 owner 校验后读取时间线分页。 */
    // 计时段列表（segments）方法：调用 segments 完成计时段列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<ExecutionSegmentVO> segments(long id, PageQueryDTO query);

    /* 按历史口径读取任务执行，允许任务已逻辑删除。 */
    // 任务执行记录（taskExecutions）方法：调用 taskExecutions 完成任务执行记录处理；参数负责传入本次处理所需的数据。
    // 任务编号（taskId），使用 long 类型保存。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    PageResultVO<ExecutionSessionVO> taskExecutions(long taskId, ExecutionQueryDTO query);

    /* 汇总指定任务／阶段／目标；scope 为空时汇总本人全部执行，日期默认今天。 */
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    ExecutionSummaryVO summary(ExecutionScopeEnum scope, Long id, ExecutionQueryDTO query);

    /* 内部调度入口，逐用户短事务处理到期会话，无 HTTP 暴露。 */
    // scan已过期（scanExpired）方法：调用 scanExpired 完成scan已过期处理；参数负责传入本次处理所需的数据。
    void scanExpired();
}
