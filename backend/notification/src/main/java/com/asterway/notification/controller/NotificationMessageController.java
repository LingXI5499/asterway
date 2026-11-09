/*
 * 提供通知消息请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.controller;

// 引入成长业务版本号（GrowthVersionDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthVersionDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.*;
// 引入通知消息业务服务（NotificationMessageService），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.service.NotificationMessageService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.*;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;

// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入*，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.*;

/* 本人站内消息入口；读取、已读、确认和继续分别表达业务动作。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 NotificationMessageController；提供通知消息请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/notifications")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class NotificationMessageController {
    // 业务服务（service），使用 NotificationMessageService 类型保存。
    private final NotificationMessageService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 NotificationMessageService 类型保存。
    public NotificationMessageController(NotificationMessageService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<NotificationMessageVO>> page(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 NotificationQueryDTO 类型保存。
            @Valid @ModelAttribute NotificationQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<NotificationMessageVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // unread方法：调用 unread 完成unread处理；参数负责传入本次处理所需的数据。
    @GetMapping("/unread-count")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<NotificationUnreadVO> unread() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.unread());
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // mark读取（markRead）方法：调用 markRead 完成mark读取处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}/read")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<NotificationMessageVO> markRead(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.markRead(id, dto.version()));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // mark全部读取（markAllRead）方法：调用 markAllRead 完成mark全部读取处理；参数负责传入本次处理所需的数据。
    @PostMapping("/read-all")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<NotificationUnreadVO> markAllRead() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.markAllRead());
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 确认（confirm）方法：调用 confirm 完成确认处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/confirm")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<NotificationMessageVO> confirm(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 NotificationHandleDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody NotificationHandleDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.handle(id, true, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // continue目标（continueGoal）方法：调用 continueGoal 完成continue目标处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/continue")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<NotificationMessageVO> continueGoal(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 NotificationHandleDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody NotificationHandleDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.handle(id, false, dto));
    }
}
