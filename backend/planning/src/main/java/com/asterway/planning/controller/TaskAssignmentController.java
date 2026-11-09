/*
 * 提供任务任务安排请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.controller;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入任务安排保存（AssignmentSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.dto.AssignmentSaveDTO;
// 引入任务任务安排业务服务（TaskAssignmentService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.TaskAssignmentService;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;
// 引入任务任务安排（TaskAssignmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.TaskAssignmentVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入删除字段对应关系（DeleteMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.DeleteMapping;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Put字段对应关系（PutMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PutMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入请求参数（RequestParam），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestParam;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* TaskAssignment REST 入口；认证身份与数据范围由 Service 校验。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 TaskAssignmentController；提供任务任务安排请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class TaskAssignmentController {
    // 业务服务（service），使用 TaskAssignmentService 类型保存。
    private final TaskAssignmentService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 TaskAssignmentService 类型保存。
    public TaskAssignmentController(TaskAssignmentService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{taskId}/assignment")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 任务编号（taskId），使用 long 类型保存。
    public ResultVO<TaskAssignmentVO> get(@PathVariable @Positive long taskId) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(taskId));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 安排（assign）方法：调用 assign 完成安排处理；参数负责传入本次处理所需的数据。
    @PutMapping("/tasks/{taskId}/assignment")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TaskAssignmentVO> assign(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 任务编号（taskId），使用 long 类型保存。
            // 请求参数对象（dto），使用 AssignmentSaveDTO 类型保存。
            @PathVariable @Positive long taskId, @Valid @RequestBody AssignmentSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.assign(taskId, dto));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 移除（remove）方法：调用 remove 完成移除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/tasks/{taskId}/assignment")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> remove(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 任务编号（taskId），使用 long 类型保存。
            @PathVariable @Positive long taskId,
            // 从 URL 查询参数中读取这个值。
            // 要求数值大于零。
            // 任务安排编号（assignmentId），使用 long 类型保存。
            @RequestParam @Positive long assignmentId,
            // 从 URL 查询参数中读取这个值。
            // 要求数值为零或正数。
            // 版本号（version），使用 long 类型保存。
            @RequestParam @PositiveOrZero long version) {
        // 调用 service.remove 完成移除处理。
        service.remove(taskId, assignmentId, version);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{taskId}/assignment-history")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<TaskAssignmentVO>> history(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 任务编号（taskId），使用 long 类型保存。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @PathVariable @Positive long taskId, @Valid @ModelAttribute GrowthQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.history(taskId, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 等待处理（pending）方法：调用 pending 完成等待处理处理；参数负责传入本次处理所需的数据。
    @GetMapping("/planning/pending-tasks")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<PlanningTaskVO>> pending(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @Valid @ModelAttribute GrowthQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.pending(query));
    }
}
