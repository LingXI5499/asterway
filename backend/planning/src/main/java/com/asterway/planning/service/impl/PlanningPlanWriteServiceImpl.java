/*
 * 实现规划方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.service.impl;

// 引入方案写入Port（PlanWritePort），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.PlanWritePort;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入方案写入提交数据Utils（PlanWritePayloadUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.PlanWritePayloadUtils;
// 引入任务安排保存（AssignmentSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.dto.AssignmentSaveDTO;
// 引入每周方案业务服务（WeeklyPlanService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.WeeklyPlanService;
// 引入每日方案业务服务（DailyPlanService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.DailyPlanService;
// 引入任务任务安排业务服务（TaskAssignmentService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.TaskAssignmentService;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;

/* 计划始终复用真实任务与现有计划，不复制任务实体。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 PlanningPlanWriteServiceImpl；实现规划方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class PlanningPlanWriteServiceImpl implements PlanWritePort {
    // 周计划列表（weeks），使用 WeeklyPlanService 类型保存。
    private final WeeklyPlanService weeks;
    // 日列表（days），使用 DailyPlanService 类型保存。
    private final DailyPlanService days;
    // 任务安排列表（assignments），使用 TaskAssignmentService 类型保存。
    private final TaskAssignmentService assignments;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 周计划列表（weeks），使用 WeeklyPlanService 类型保存。
    // 日列表（days），使用 DailyPlanService 类型保存。
    public PlanningPlanWriteServiceImpl(WeeklyPlanService weeks, DailyPlanService days,
            // 任务安排列表（assignments），使用 TaskAssignmentService 类型保存。
            // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
            TaskAssignmentService assignments, ObjectMapper json) {
        // 把周计划列表（weeks）保存到 this.weeks。
        this.weeks = weeks;
        // 把日列表（days）保存到 this.days。
        this.days = days;
        // 把任务安排列表（assignments）保存到 this.assignments。
        this.assignments = assignments;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // domain方法：调用 domain 完成domain处理；参数负责传入本次处理所需的数据。
    @Override
    // 把固定文字 'planning'交还给调用方，本方法到此结束。
    public String domain() { return "planning"; }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    @Override
    // 操作（action），使用 Action 类型保存。
    // command，使用 JsonNode 类型保存。
    public JsonNode write(Action action, JsonNode command) {
        // 准备值（value），类型为 Object；初始内容是调用 switch (action) { case WEEK -> yield weeks.byWeek 完成按周处理。
        // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
        Object value = switch (action) {
            // 匹配这一种取值时使用本分支：case WEEK。
            case WEEK -> weeks.byWeek(LocalDate.parse(command.path("date").asText()), true);
            // 匹配这一种取值时使用本分支：case DAY。
            case DAY -> days.byDate(command.path("parentId").asLong(), LocalDate.parse(command.path("date").asText()));
            // 匹配这一种取值时使用本分支：case CURRENT_ASSIGNMENT。
            case CURRENT_ASSIGNMENT -> assignments.get(command.path("parentId").asLong());
            // 匹配这一种取值时使用本分支：case ASSIGN。
            case ASSIGN -> assignments.assign(command.path("parentId").asLong(),
                    // 保存 请求参数对象（DTO）。
                    PlanWritePayloadUtils.saveDTO(command.path("data"), AssignmentSaveDTO.class, json));
            // 匹配这一种取值时使用本分支：default。
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            default -> throw BusinessException.badRequest("不支持的计划操作");
        };
        // 把调用 json.valueToTree 完成值ToTree处理交还给调用方，本方法到此结束。
        return json.valueToTree(value);
    }
}
