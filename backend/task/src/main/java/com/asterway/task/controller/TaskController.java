/*
 * 提供任务请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.task.controller;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入成长业务调整顺序（GrowthReorderDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthReorderDTO;
// 引入成长业务状态（GrowthStatusDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthStatusDTO;
// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入任务保存（TaskSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.dto.TaskSaveDTO;
// 引入任务业务服务（TaskService），下面可以直接使用这个类型或静态成员。
import com.asterway.task.service.TaskService;
// 引入任务当前状态历史（TaskStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.vo.TaskStateHistoryVO;
// 引入任务（TaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.vo.TaskVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

// 引入HTTP请求状态（HttpStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpStatus;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入删除字段对应关系（DeleteMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.DeleteMapping;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入Patch字段对应关系（PatchMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PatchMapping;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入Put字段对应关系（PutMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PutMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入请求参数（RequestParam），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestParam;
// 引入响应状态（ResponseStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ResponseStatus;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* Task REST 入口；只绑定、校验请求并调用 Service 接口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 TaskController；提供任务请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class TaskController {
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{id}/state-history")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<TaskStateHistoryVO>> history(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.history(id, query));
    }

    // 业务服务（service），使用 TaskService 类型保存。
    private final TaskService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 TaskService 类型保存。
    public TaskController(TaskService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<TaskVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/tasks/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TaskVO> update(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody TaskSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    /* 完整替换基本资料；可选字段为 null 时清空。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @PutMapping("/tasks/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TaskVO> replace(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody TaskSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.replace(id, dto));
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 状态（status）方法：调用 status 完成状态处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/tasks/{id}/status")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TaskVO> status(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthStatusDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthStatusDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.status(id, dto));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/tasks/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> delete(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 从 URL 查询参数中读取这个值。
            // 要求数值为零或正数。
            // 编号（id），使用 long 类型保存。
            // 版本号（version），使用 long 类型保存。
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        // 调用 service.delete 完成删除处理。
        service.delete(id, version);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping("/stages/{parentId}/tasks")
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TaskVO> create(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 上级编号（parentId），使用 long 类型保存。
            // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
            @PathVariable @Positive long parentId, @Valid @RequestBody TaskSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(parentId, dto));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 调整顺序（reorder）方法：调用 reorder 完成调整顺序处理；参数负责传入本次处理所需的数据。
    @PutMapping("/stages/{parentId}/tasks/order")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> reorder(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 上级编号（parentId），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthReorderDTO 类型保存。
            @PathVariable @Positive long parentId, @Valid @RequestBody GrowthReorderDTO dto) {
        // 调用 service.reorder 完成调整顺序处理。
        service.reorder(parentId, dto);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public ResultVO<PageResultVO<TaskVO>> page(@Valid @ModelAttribute GrowthQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }
}
