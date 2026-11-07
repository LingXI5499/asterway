package com.asterway.agent.controller;

import com.asterway.agent.service.AgentWorkspaceService;
import com.asterway.common.result.ResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/** 只暴露已实现资源；内部端点和第三方密钥不会返回浏览器。 */
@RestController
@RequestMapping("/asterway/agent")
public class AgentWorkspaceController {
    private final AgentWorkspaceService service;

    public AgentWorkspaceController(AgentWorkspaceService service) {
        this.service = service;
    }

    @GetMapping({"/memories", "/knowledge", "/knowledge/tasks", "/knowledge/{id}",
            "/knowledge/{id}/chunks", "/knowledge/{id}/revisions",
            "/knowledge/{id}/revisions/{revisionId}/chunks/{chunkId}",
            "/planning/proposals", "/planning/proposals/{id}", "/planning/proposals/{id}/progress",
            "/reviews", "/reviews/{id}", "/reviews/latest/conversation/{id}", "/runs/calls", "/runs/tasks"})
    public ResultVO<JsonNode> read(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return ResultVO.success(service.request("GET", resource(request), query, null));
    }

    /** 新建资源返回 201；动作接口仍返回 200，前端只依赖 2xx。 */
    @PostMapping({"/memories/candidates", "/planning/proposals", "/reviews", "/reviews/{id}/proposals"})
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<JsonNode> create(HttpServletRequest request, @RequestParam Map<String, String> query,
            @RequestBody(required = false) JsonNode body) {
        return ResultVO.success(service.request("POST", resource(request), query, body));
    }

    @PostMapping({"/memories/{id}/confirm", "/memories/{id}/reject",
            "/memories/{id}/archive", "/planning/proposals/{id}/revisions",
            "/planning/proposals/{id}/confirm", "/planning/proposals/{id}/execute",
            "/planning/proposals/{id}/cancel", "/knowledge/{id}/retry", "/reviews/{id}/retry", "/runs/tasks/{id}/retry"})
    public ResultVO<JsonNode> action(HttpServletRequest request, @RequestParam Map<String, String> query,
            @RequestBody(required = false) JsonNode body) {
        return ResultVO.success(service.request("POST", resource(request), query, body));
    }

    @PatchMapping("/memories/{id}")
    public ResultVO<JsonNode> update(HttpServletRequest request, @RequestBody JsonNode body) {
        return ResultVO.success(service.request("PATCH", resource(request), Map.of(), body));
    }

    @DeleteMapping({"/memories/{id}", "/knowledge/{id}"})
    public ResultVO<JsonNode> delete(HttpServletRequest request, @RequestParam Map<String, String> query) {
        return ResultVO.success(service.request("DELETE", resource(request), query, null));
    }

    @PostMapping(value = "/knowledge", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<JsonNode> upload(HttpServletRequest request, @RequestParam Map<String, String> query,
            @RequestHeader(value = "X-File-Type", defaultValue = "application/octet-stream") String mime,
            @RequestBody byte[] content) {
        return ResultVO.success(service.upload(resource(request), query, mime, content));
    }

    @PutMapping(value = "/knowledge/{id}/content", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResultVO<JsonNode> replace(HttpServletRequest request, @RequestParam Map<String, String> query,
            @RequestHeader(value = "X-File-Type", defaultValue = "application/octet-stream") String mime,
            @RequestBody byte[] content) {
        return ResultVO.success(service.upload(resource(request), query, mime, content));
    }

    private String resource(HttpServletRequest request) {
        return request.getRequestURI().substring("/asterway/agent/".length());
    }
}
