/*
 * 提供习惯请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.controller;

// 引入成长业务版本号（GrowthVersionDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthVersionDTO;
// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.*;
// 引入习惯业务服务（HabitService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.*;

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

/* 习惯主链入口；打卡不接收日期或自填时间。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 HabitController；提供习惯请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/habits")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class HabitController {
    // 业务服务（service），使用 HabitService 类型保存。
    private final HabitService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 HabitService 类型保存。
    public HabitController(HabitService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    public ResultVO<PageResultVO<HabitVO>> page(@Valid @ModelAttribute HabitQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 今天（today）方法：调用 today 完成今天处理；参数负责传入本次处理所需的数据。
    @GetMapping("/today")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public ResultVO<PageResultVO<HabitTodayVO>> today(@Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.today(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<HabitVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    public ResultVO<HabitVO> create(@Valid @RequestBody HabitSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // patch方法：调用 patch 完成patch处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> patch(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody HabitSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto, false));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @PutMapping("/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> replace(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody HabitSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto, true));
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

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/pause")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> pause(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.pause(id, dto.version()));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/resume")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> resume(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.resume(id, dto.version()));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 结束（end）方法：调用 end 完成结束处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/end")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> end(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.end(id, dto.version()));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // reopen方法：调用 reopen 完成reopen处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/reopen")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitVO> reopen(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.reopen(id, dto.version()));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 打卡（checkin）方法：核对 in；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/checkins")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitCheckinVO> checkin(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.checkin(id, dto.version()));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/{id}/checkins/{checkinId}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<HabitCheckinVO> cancel(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            @PathVariable @Positive long id,
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 打卡编号（checkinId），使用 long 类型保存。
            @PathVariable @Positive long checkinId,
            // 从 URL 查询参数中读取这个值。
            // 要求数值为零或正数。
            // 版本号（version），使用 long 类型保存。
            @RequestParam @PositiveOrZero long version) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.cancel(id, checkinId, version));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 打卡记录（checkins）方法：核对 ins；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/checkins")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<HabitCheckinVO>> checkins(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 HabitCheckinQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute HabitCheckinQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.checkins(id, query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 修订记录（revisions）方法：调用 revisions 完成修订记录处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/revisions")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<HabitRevisionVO>> revisions(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.revisions(id, query));
    }
}
