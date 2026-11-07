package com.asterway.agent.controller;

import com.asterway.agent.dto.AgentChatDTO;
import com.asterway.agent.service.AgentChatService;
import com.asterway.agent.vo.AgentChatResultVO;
import com.asterway.common.result.ResultVO;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 浏览器唯一公开对话入口；SSE 采用事件协议，普通接口沿用 ResultVO。 */
@RestController
@RequestMapping("/asterway/agent")
public class AgentChatController {

    private final AgentChatService service;

    public AgentChatController(AgentChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ResultVO<AgentChatResultVO> chat(@Valid @RequestBody AgentChatDTO dto) {
        return ResultVO.success(service.chat(dto));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(@Valid @RequestBody AgentChatDTO dto) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.stream(dto));
    }
}
