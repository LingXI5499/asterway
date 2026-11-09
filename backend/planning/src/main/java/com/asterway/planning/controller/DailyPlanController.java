/*
 * 提供每日方案请求入口的 HTTP 入口，校验输入后交给业务服务处理。
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
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.planning.dto.*;
// 引入每日方案业务服务（DailyPlanService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.DailyPlanService;
// 引入每日方案（DailyPlanVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.DailyPlanVO;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

// 引入日期时间格式（DateTimeFormat），下面可以直接使用这个类型或静态成员。
import org.springframework.format.annotation.DateTimeFormat;
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
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入请求参数（RequestParam），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestParam;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* DailyPlan REST 入口；认证身份与数据范围由 Service 校验。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 DailyPlanController；提供每日方案请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class DailyPlanController {
    // 业务服务（service），使用 DailyPlanService 类型保存。
    private final DailyPlanService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 DailyPlanService 类型保存。
    public DailyPlanController(DailyPlanService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 按日期（byDate）方法：调用 byDate 完成按日期处理；参数负责传入本次处理所需的数据。
    @GetMapping("/weekly-plans/{weeklyPlanId}/daily-plans/{date}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<DailyPlanVO> byDate(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 每周方案编号（weeklyPlanId），使用 long 类型保存。
            @PathVariable @Positive long weeklyPlanId,
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
            // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.findByDate(weeklyPlanId, date));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/daily-plans/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<DailyPlanVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PutMapping("/weekly-plans/{weeklyPlanId}/daily-plans/{date}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<DailyPlanVO> create(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 每周方案编号（weeklyPlanId），使用 long 类型保存。
            @PathVariable @Positive long weeklyPlanId,
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
            // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.byDate(weeklyPlanId, date));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping("/weekly-plans/{weeklyPlanId}/daily-plans")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<DailyPlanVO>> page(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 每周方案编号（weeklyPlanId），使用 long 类型保存。
            @PathVariable @Positive long weeklyPlanId,
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @Valid @ModelAttribute GrowthQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(weeklyPlanId, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/daily-plans/{id}/tasks")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<PlanningTaskVO>> tasks(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute GrowthQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.tasks(id, query));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/daily-plans/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> delete(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            @PathVariable @Positive long id,
            // 从 URL 查询参数中读取这个值。
            // 要求数值为零或正数。
            // 版本号（version），使用 long 类型保存。
            @RequestParam @PositiveOrZero long version,
            // 从 URL 查询参数中读取这个值。
            // handling，使用 String 类型保存；可为 null，表示尚未提供。
            @RequestParam(required = false) String handling) {
        // 调用 service.delete 完成删除处理。
        service.delete(id, version, handling);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }
}
