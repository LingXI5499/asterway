/*
 * 提供Dashboard请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.controller;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入Dashboard任务查询条件（DashboardTaskQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
// 引入Dashboard业务服务（DashboardService），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.service.DashboardService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.vo.*;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;

// 引入*，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.*;

/* 当前普通用户的今日看板，不支持指定其他用户。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 DashboardController；提供Dashboard请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/dashboard")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class DashboardController {
    // 业务服务（service），使用 DashboardService 类型保存。
    private final DashboardService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 DashboardService 类型保存。
    public DashboardController(DashboardService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // overview方法：调用 overview 完成overview处理；参数负责传入本次处理所需的数据。
    @GetMapping("/overview")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<DashboardOverviewVO> overview() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.overview());
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tasks")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<DashboardTaskVO>> tasks(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 DashboardTaskQueryDTO 类型保存。
            @Valid @ModelAttribute DashboardTaskQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.tasks(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 目标列表（goals）方法：调用 goals 完成目标列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/goals")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<DashboardGoalVO>> goals(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.goals(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/habits")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<DashboardHabitVO>> habits(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.habits(query));
    }
}
