/*
 * 规定任务任务安排业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.service;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入任务安排保存（AssignmentSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.dto.AssignmentSaveDTO;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;
// 引入任务任务安排（TaskAssignmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.TaskAssignmentVO;

/* TaskAssignment 的完整业务链路。 */
// 定义接口 TaskAssignmentService；规定任务任务安排业务服务可供其他代码调用的业务方法。
public interface TaskAssignmentService {
    /* 当前用户范围内的资源操作。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 任务编号（taskId），使用 long 类型保存。
    TaskAssignmentVO get(long taskId);

    /* 当前用户范围内的资源操作。 */
    // 安排（assign）方法：调用 assign 完成安排处理；参数负责传入本次处理所需的数据。
    // 任务编号（taskId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AssignmentSaveDTO 类型保存。
    TaskAssignmentVO assign(long taskId, AssignmentSaveDTO dto);

    /* 当前用户范围内的资源操作。 */
    // 移除（remove）方法：调用 remove 完成移除处理；参数负责传入本次处理所需的数据。
    // 任务编号（taskId），使用 long 类型保存。
    // 任务安排编号（assignmentId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void remove(long taskId, long assignmentId, long version);

    /* 当前用户范围内的资源操作。 */
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    // 任务编号（taskId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    PageResultVO<TaskAssignmentVO> history(long taskId, GrowthQueryDTO query);

    /* 当前用户范围内的资源操作。 */
    // 等待处理（pending）方法：调用 pending 完成等待处理处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    PageResultVO<PlanningTaskVO> pending(GrowthQueryDTO query);
}
