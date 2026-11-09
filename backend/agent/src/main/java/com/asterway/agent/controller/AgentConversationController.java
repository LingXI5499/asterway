/*
 * 提供成长助手对话请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.controller;

// 引入成长助手对话创建（AgentConversationCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationCreateDTO;
// 引入成长助手对话消息（AgentConversationMessageDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationMessageDTO;
// 引入成长助手对话查询条件（AgentConversationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationQueryDTO;
// 引入成长助手对话标题（AgentConversationTitleDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentConversationTitleDTO;
// 引入成长助手对话业务服务（AgentConversationService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentConversationService;
// 引入成长助手对话详情（AgentConversationDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationDetailVO;
// 引入成长助手对话（AgentConversationVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentConversationVO;
// 引入成长助手消息当前页（AgentMessagePageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentMessagePageVO;
// 引入成长助手发送结果（AgentSendResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentSendResultVO;
// 引入成长助手本轮对话（AgentTurnVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentTurnVO;
// 引入成长业务版本号（GrowthVersionDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthVersionDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入最大（Max），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Max;
// 引入最小（Min），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Min;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

// 引入HTTP请求状态（HttpStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpStatus;
// 引入Media类型（MediaType），下面可以直接使用这个类型或静态成员。
import org.springframework.http.MediaType;
// 引入响应（ResponseEntity），下面可以直接使用这个类型或静态成员。
import org.springframework.http.ResponseEntity;
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
// 引入Streaming响应请求正文（StreamingResponseBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/* 会话公开入口；浏览器只访问 Java，内部密钥与模型密钥不离开服务端。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 AgentConversationController；提供成长助手对话请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/agent/conversations")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentConversationController {

    // 业务服务（service），使用 AgentConversationService 类型保存。
    private final AgentConversationService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 AgentConversationService 类型保存。
    public AgentConversationController(AgentConversationService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<AgentConversationVO>> page(
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 AgentConversationQueryDTO 类型保存。
            @Valid @ModelAttribute AgentConversationQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentConversationDetailVO> create(
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求参数对象（dto），使用 AgentConversationCreateDTO 类型保存。
            @Valid @RequestBody AgentConversationCreateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 详情（detail）方法：调用 detail 完成详情处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<AgentConversationDetailVO> detail(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.detail(id));
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 重命名（rename）方法：调用 rename 完成重命名处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentConversationDetailVO> rename(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 AgentConversationTitleDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationTitleDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.rename(id, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 关闭（close）方法：释放连接、文件或客户端占用的资源；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/close")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentConversationDetailVO> close(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.close(id, dto));
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

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 消息列表（messages）方法：调用 messages 完成消息列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/messages")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentMessagePageVO> messages(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            @PathVariable @Positive long id,
            // 从 URL 查询参数中读取这个值。
            // 要求数值大于零。
            // 变化前消息No（beforeMessageNo），使用 Long 类型保存；可为 null，表示尚未提供。
            @RequestParam(required = false) @Positive Long beforeMessageNo,
            // 从 URL 查询参数中读取这个值。
            // 要求数值不小于注解给出的下限。
            // 要求数值不大于注解给出的上限。
            // 每页条数（size），使用 int 类型保存。
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.messages(id, beforeMessageNo, size));
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 本轮对话（turn）方法：调用 turn 完成本轮对话处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/turns/{turnId}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentTurnVO> turn(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            // 本轮对话编号（turnId），使用 long 类型保存。
            @PathVariable @Positive long id, @PathVariable @Positive long turnId) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.turn(id, turnId));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/turns/{turnId}/cancel")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentTurnVO> cancel(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            @PathVariable @Positive long id,
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 本轮对话编号（turnId），使用 long 类型保存。
            @PathVariable @Positive long turnId,
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求参数对象（dto），使用 GrowthVersionDTO 类型保存。
            @Valid @RequestBody GrowthVersionDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.cancel(id, turnId, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 发送（send）方法：调用 send 完成发送处理；参数负责传入本次处理所需的数据。
    @PostMapping("/{id}/messages")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<AgentSendResultVO> send(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationMessageDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.send(id, dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<StreamingResponseBody> stream(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 编号（id），使用 long 类型保存。
            // 请求参数对象（dto），使用 AgentConversationMessageDTO 类型保存。
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationMessageDTO dto) {
        // 把调用 ResponseEntity.ok 完成ok处理交还给调用方，本方法到此结束。
        return ResponseEntity.ok()
                // 继续对上一步结果执行调用 header 完成请求头处理。
                .header("Cache-Control", "no-store")
                // 继续对上一步结果执行调用 header 完成请求头处理。
                .header("X-Accel-Buffering", "no")
                // 继续对上一步结果执行调用 contentType 完成内容类型处理。
                .contentType(MediaType.TEXT_EVENT_STREAM)
                // 继续对上一步结果执行调用 body(service.stream 完成分段回复流处理。
                .body(service.stream(id, dto));
    }
}
