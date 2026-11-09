/*
 * 提供习惯分类请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.controller;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.*;
// 引入习惯分类业务服务（HabitCategoryService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitCategoryService;
// 引入习惯分类（HabitCategoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.HabitCategoryVO;

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
// 引入*，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.*;

/* 用户个人分类 REST 入口，只绑定参数并调用 Service 接口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 HabitCategoryController；提供习惯分类请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/habit-categories")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class HabitCategoryController {
    // 业务服务（service），使用 HabitCategoryService 类型保存。
    private final HabitCategoryService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 HabitCategoryService 类型保存。
    public HabitCategoryController(HabitCategoryService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<HabitCategoryVO>> page(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 HabitQueryDTO 类型保存。
            @Valid @ModelAttribute HabitQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
    public ResultVO<HabitCategoryVO> create(@Valid @RequestBody HabitCategorySaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PutMapping("/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitCategoryVO> update(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody HabitCategorySaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/{id}")
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
}
