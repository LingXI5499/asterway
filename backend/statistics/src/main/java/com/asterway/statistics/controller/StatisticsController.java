/*
 * 提供统计请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.controller;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入统计习惯查询条件（StatisticsHabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
// 引入统计查询条件（StatisticsQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.dto.StatisticsQueryDTO;
// 引入统计业务服务（StatisticsService），下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.service.StatisticsService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.statistics.vo.*;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;

// 引入*，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.*;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 历史统计只读 REST 入口，不接受人工修改历史或耗时。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 StatisticsController；提供统计请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/statistics")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class StatisticsController {
    // 业务服务（service），使用 StatisticsService 类型保存。
    private final StatisticsService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 StatisticsService 类型保存。
    public StatisticsController(StatisticsService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    @GetMapping("/summary")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
    public ResultVO<StatisticsSummaryVO> summary(@Valid @ModelAttribute StatisticsQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.summary(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 趋势列表（trends）方法：调用 trends 完成趋势列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/trends")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<StatisticsTrendVO>> trends(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.trends(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 成长时间线（timeline）方法：调用 timeline 完成成长时间线处理；参数负责传入本次处理所需的数据。
    @GetMapping("/timeline")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<StatisticsTimelineVO>> timeline(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.timeline(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/habits")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<StatisticsHabitReportVO> habits(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 StatisticsHabitQueryDTO 类型保存。
            @Valid @ModelAttribute StatisticsHabitQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.habits(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // objects方法：调用 objects 完成objects处理；参数负责传入本次处理所需的数据。
    @GetMapping("/objects")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<List<StatisticsObjectVO>> objects(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 StatisticsQueryDTO 类型保存。
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.objects(query));
    }
}
