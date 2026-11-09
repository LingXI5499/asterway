/*
 * 提供成长助手工作区请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.controller;

// 引入成长助手工作区业务服务（AgentWorkspaceService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentWorkspaceService;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入Java HTTP 请求对象（HttpServletRequest），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletRequest;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入HTTP请求状态（HttpStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpStatus;
// 引入Media类型（MediaType），下面可以直接使用这个类型或静态成员。
import org.springframework.http.MediaType;
// 引入*，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.*;

/* 只暴露已实现资源；内部端点和第三方密钥不会返回浏览器。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 AgentWorkspaceController；提供成长助手工作区请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/agent")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentWorkspaceController {
    // 业务服务（service），使用 AgentWorkspaceService 类型保存。
    private final AgentWorkspaceService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 AgentWorkspaceService 类型保存。
    public AgentWorkspaceController(AgentWorkspaceService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（read）方法：调用 read 完成读取处理；参数负责传入本次处理所需的数据。
    @GetMapping({"/knowledge/capabilities", "/knowledge/bases", "/knowledge/bases/{id}", "/memories", "/knowledge", "/knowledge/tasks", "/knowledge/{id}",
            // 继续传入或组合固定文字 '/knowledge/{id}/chunks", "/knowledge/{id}/revisions'，完成上面尚未结束的调用或声明。
            "/knowledge/{id}/chunks", "/knowledge/{id}/revisions",
            // 继续传入或组合固定文字 '/knowledge/{id}/revisions/{revisionId}/chunks/{chunkId}'，完成上面尚未结束的调用或声明。
            "/knowledge/{id}/revisions/{revisionId}/chunks/{chunkId}",
            // 继续传入或组合固定文字 '/planning/proposals", "/planning/proposals/{id}", "/planning/proposals/{id}/prog…'，完成上面尚未结束的调用或声明。
            "/planning/proposals", "/planning/proposals/{id}", "/planning/proposals/{id}/progress",
            // 继续传入或组合按表达式计算得到的值："/reviews", "/reviews/{id}", "/reviews/latest/conversation/{id}", "/runs/calls", "/runs/tasks"})，完成上面尚未结束的调用或声明。
            "/reviews", "/reviews/{id}", "/reviews/latest/conversation/{id}", "/runs/calls", "/runs/tasks"})
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> read(HttpServletRequest request, @RequestParam Map<String, String> query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.request("GET", resource(request), query, null));
    }

    /* 新建资源返回 201；动作接口仍返回 200，前端只依赖 2xx。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping({"/knowledge/bases", "/memories/candidates", "/planning/proposals", "/reviews", "/reviews/{id}/proposals"})
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> create(HttpServletRequest request, @RequestParam Map<String, String> query,
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求正文（body），使用 JsonNode 类型保存。
            @RequestBody(required = false) JsonNode body) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.request("POST", resource(request), query, body));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 操作（action）方法：调用 action 完成操作处理；参数负责传入本次处理所需的数据。
    @PostMapping({"/knowledge/move", "/memories/{id}/confirm", "/memories/{id}/reject",
            // 继续传入或组合固定文字 '/memories/{id}/archive", "/planning/proposals/{id}/revisions'，完成上面尚未结束的调用或声明。
            "/memories/{id}/archive", "/planning/proposals/{id}/revisions",
            // 继续传入或组合固定文字 '/planning/proposals/{id}/confirm", "/planning/proposals/{id}/execute'，完成上面尚未结束的调用或声明。
            "/planning/proposals/{id}/confirm", "/planning/proposals/{id}/execute",
            // 继续传入或组合按表达式计算得到的值："/planning/proposals/{id}/cancel", "/knowledge/{id}/retry", "/reviews/{id}/retry", "/runs/tasks/{id}/retry"})，完成上面尚未结束的调用或声明。
            "/planning/proposals/{id}/cancel", "/knowledge/{id}/retry", "/reviews/{id}/retry", "/runs/tasks/{id}/retry"})
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> action(HttpServletRequest request, @RequestParam Map<String, String> query,
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求正文（body），使用 JsonNode 类型保存。
            @RequestBody(required = false) JsonNode body) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.request("POST", resource(request), query, body));
    }

    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping({"/memories/{id}", "/knowledge/bases/{id}", "/conversations/{id}/knowledge-bases"})
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 请求正文（body），使用 JsonNode 类型保存。
    public ResultVO<JsonNode> update(HttpServletRequest request, @RequestBody JsonNode body) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.request("PATCH", resource(request), Map.of(), body));
    }

    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping({"/knowledge/bases/{id}", "/memories/{id}", "/knowledge/{id}"})
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> delete(HttpServletRequest request, @RequestParam Map<String, String> query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.request("DELETE", resource(request), query, null));
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 上传（upload）方法：调用 upload 完成上传处理；参数负责传入本次处理所需的数据。
    @PostMapping(value = "/knowledge", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> upload(HttpServletRequest request, @RequestParam Map<String, String> query,
            // 使用 RequestHeader 注解配置下面的类、方法或参数。
            // mime，使用 String 类型保存；可为 null，表示尚未提供。
            @RequestHeader(value = "X-File-Type", defaultValue = "application/octet-stream") String mime,
            // 从请求 JSON 正文中读取这个参数对象。
            // 内容（content），使用 byte[] 类型保存。
            @RequestBody(required = false) byte[] content) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.upload(resource(request), query, mime, content));
    }

    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @PutMapping(value = "/knowledge/{id}/content", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    // 从 URL 查询参数中读取这个值。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    public ResultVO<JsonNode> replace(HttpServletRequest request, @RequestParam Map<String, String> query,
            // 使用 RequestHeader 注解配置下面的类、方法或参数。
            // mime，使用 String 类型保存；可为 null，表示尚未提供。
            @RequestHeader(value = "X-File-Type", defaultValue = "application/octet-stream") String mime,
            // 从请求 JSON 正文中读取这个参数对象。
            // 内容（content），使用 byte[] 类型保存。
            @RequestBody(required = false) byte[] content) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.upload(resource(request), query, mime, content));
    }

    // resource方法：调用 resource 完成resource处理；参数负责传入本次处理所需的数据。
    // 请求（request），使用 HttpServletRequest 类型保存。
    private String resource(HttpServletRequest request) {
        // 把集合或文字的长度交还给调用方，本方法到此结束。
        return request.getRequestURI().substring("/asterway/agent/".length());
    }
}
