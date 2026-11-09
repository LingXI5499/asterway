/*
 * 提供成长助手Chat请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.controller;

// 引入成长助手Chat（AgentChatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentChatDTO;
// 引入成长助手Chat业务服务（AgentChatService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentChatService;
// 引入成长助手Chat结果（AgentChatResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentChatResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;

// 引入Media类型（MediaType），下面可以直接使用这个类型或静态成员。
import org.springframework.http.MediaType;
// 引入响应（ResponseEntity），下面可以直接使用这个类型或静态成员。
import org.springframework.http.ResponseEntity;
// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;
// 引入Streaming响应请求正文（StreamingResponseBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/* 浏览器唯一公开对话入口；SSE 采用事件协议，普通接口沿用 ResultVO。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 AgentChatController；提供成长助手Chat请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/agent")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentChatController {

    // 业务服务（service），使用 AgentChatService 类型保存。
    private final AgentChatService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 AgentChatService 类型保存。
    public AgentChatController(AgentChatService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 对话（chat）方法：调用 chat 完成对话处理；参数负责传入本次处理所需的数据。
    @PostMapping("/chat")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    public ResultVO<AgentChatResultVO> chat(@Valid @RequestBody AgentChatDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.chat(dto));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 分段回复流（stream）方法：调用 stream 完成分段回复流处理；参数负责传入本次处理所需的数据。
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 AgentChatDTO 类型保存。
    public ResponseEntity<StreamingResponseBody> stream(@Valid @RequestBody AgentChatDTO dto) {
        // 把调用 ResponseEntity.ok 完成ok处理交还给调用方，本方法到此结束。
        return ResponseEntity.ok()
                // 继续对上一步结果执行调用 header 完成请求头处理。
                .header("Cache-Control", "no-store")
                // 继续对上一步结果执行调用 header 完成请求头处理。
                .header("X-Accel-Buffering", "no")
                // 继续对上一步结果执行调用 contentType 完成内容类型处理。
                .contentType(MediaType.TEXT_EVENT_STREAM)
                // 继续对上一步结果执行调用 body(service.stream 完成分段回复流处理。
                .body(service.stream(dto));
    }
}
