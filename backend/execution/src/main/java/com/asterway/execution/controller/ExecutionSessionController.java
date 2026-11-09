/*
 * 提供执行计时会话请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.controller;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入执行计时数据范围Enum（ExecutionScopeEnum），下面可以直接使用这个类型或静态成员。
import com.asterway.common.enums.ExecutionScopeEnum;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入执行计时Control（ExecutionControlDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionControlDTO;
// 引入执行计时心跳（ExecutionHeartbeatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
// 引入执行计时查询条件（ExecutionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionQueryDTO;
// 引入执行计时开始（ExecutionStartDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionStartDTO;
// 引入执行计时会话业务服务（ExecutionSessionService），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.service.ExecutionSessionService;
// 引入执行计时详情（ExecutionDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionDetailVO;
// 引入执行计时连续计时段（ExecutionSegmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSegmentVO;
// 引入执行计时会话（ExecutionSessionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSessionVO;
// 引入执行计时汇总（ExecutionSummaryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSummaryVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;

// 引入HTTP请求状态（HttpStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpStatus;
// 引入响应（ResponseEntity），下面可以直接使用这个类型或静态成员。
import org.springframework.http.ResponseEntity;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 执行会话的十四个 REST 入口；校验请求并调用同一 Service 接口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 ExecutionSessionController；提供执行计时会话请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class ExecutionSessionController {
    // 业务服务（service），使用 ExecutionSessionService 类型保存。
    private final ExecutionSessionService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 ExecutionSessionService 类型保存。
    public ExecutionSessionController(ExecutionSessionService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 新建返回 201，幂等返回和恢复原会话返回 200，data 结构保持一致。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 开始（start）方法：调用 start 完成开始处理；参数负责传入本次处理所需的数据。
    @PostMapping("/executions")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<ExecutionSessionVO>> start(
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求参数对象（dto），使用 ExecutionStartDTO 类型保存。
            @Valid @RequestBody ExecutionStartDTO dto) {
        // 准备结果（result），类型为 null；初始内容是调用 service.start 完成开始处理。
        var result = service.start(dto);
        // 把根据前面的条件，从两个候选结果中选择一个：ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(ResultVO.success(r…交还给调用方，本方法到此结束。
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                // 继续对上一步结果执行调用 body(ResultVO.success 完成成功处理。
                .body(ResultVO.success(result.execution()));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    @GetMapping("/executions/current")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSessionVO> current() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.current());
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    @PostMapping("/executions/{id}/pause")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSessionVO> pause(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.pause(id, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    @PostMapping("/executions/{id}/resume")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSessionVO> resume(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.resume(id, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // finish方法：调用 finish 完成finish处理；参数负责传入本次处理所需的数据。
    @PostMapping("/executions/{id}/finish")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSessionVO> finish(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.finish(id, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 心跳（heartbeat）方法：调用 heartbeat 完成心跳处理；参数负责传入本次处理所需的数据。
    @PostMapping("/executions/{id}/heartbeat")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSessionVO> heartbeat(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 ExecutionHeartbeatDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionHeartbeatDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.heartbeat(id, dto));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping("/executions")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<ExecutionSessionVO>> page(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/executions/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<ExecutionDetailVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 计时段列表（segments）方法：调用 segments 完成计时段列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/executions/{id}/segments")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<ExecutionSegmentVO>> segments(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.segments(id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 任务执行记录（taskExecutions）方法：调用 taskExecutions 完成任务执行记录处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{id}/executions")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<ExecutionSessionVO>> taskExecutions(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.taskExecutions(id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 任务汇总（taskSummary）方法：调用 taskSummary 完成任务汇总处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks/{id}/execution-summary")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSummaryVO> taskSummary(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.summary(ExecutionScopeEnum.TASK, id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 阶段汇总（stageSummary）方法：调用 stageSummary 完成阶段汇总处理；参数负责传入本次处理所需的数据。
    @GetMapping("/stages/{id}/execution-summary")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSummaryVO> stageSummary(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.summary(ExecutionScopeEnum.STAGE, id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 目标汇总（goalSummary）方法：调用 goalSummary 完成目标汇总处理；参数负责传入本次处理所需的数据。
    @GetMapping("/goals/{id}/execution-summary")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<ExecutionSummaryVO> goalSummary(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.summary(ExecutionScopeEnum.GOAL, id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    @GetMapping("/executions/summary")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    public ResultVO<ExecutionSummaryVO> summary(@Valid @ModelAttribute ExecutionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.summary(null, null, query));
    }
}
