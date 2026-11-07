package com.asterway.agent.controller;

import com.asterway.agent.dto.AgentConversationCreateDTO;
import com.asterway.agent.dto.AgentConversationMessageDTO;
import com.asterway.agent.dto.AgentConversationQueryDTO;
import com.asterway.agent.dto.AgentConversationTitleDTO;
import com.asterway.agent.service.AgentConversationService;
import com.asterway.agent.vo.AgentConversationDetailVO;
import com.asterway.agent.vo.AgentConversationVO;
import com.asterway.agent.vo.AgentMessagePageVO;
import com.asterway.agent.vo.AgentSendResultVO;
import com.asterway.agent.vo.AgentTurnVO;
import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 会话公开入口；浏览器只访问 Java，内部密钥与模型密钥不离开服务端。 */
@RestController
@Validated
@RequestMapping("/asterway/agent/conversations")
public class AgentConversationController {

    private final AgentConversationService service;

    public AgentConversationController(AgentConversationService service) {
        this.service = service;
    }

    @GetMapping
    public ResultVO<PageResultVO<AgentConversationVO>> page(
            @Valid @ModelAttribute AgentConversationQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<AgentConversationDetailVO> create(
            @Valid @RequestBody AgentConversationCreateDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResultVO<AgentConversationDetailVO> detail(@PathVariable @Positive long id) {
        return ResultVO.success(service.detail(id));
    }

    @PatchMapping("/{id}")
    public ResultVO<AgentConversationDetailVO> rename(
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationTitleDTO dto) {
        return ResultVO.success(service.rename(id, dto));
    }

    @PostMapping("/{id}/close")
    public ResultVO<AgentConversationDetailVO> close(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.close(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }

    @GetMapping("/{id}/messages")
    public ResultVO<AgentMessagePageVO> messages(
            @PathVariable @Positive long id,
            @RequestParam(required = false) @Positive Long beforeMessageNo,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResultVO.success(service.messages(id, beforeMessageNo, size));
    }

    @GetMapping("/{id}/turns/{turnId}")
    public ResultVO<AgentTurnVO> turn(
            @PathVariable @Positive long id, @PathVariable @Positive long turnId) {
        return ResultVO.success(service.turn(id, turnId));
    }

    @PostMapping("/{id}/turns/{turnId}/cancel")
    public ResultVO<AgentTurnVO> cancel(
            @PathVariable @Positive long id,
            @PathVariable @Positive long turnId,
            @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.cancel(id, turnId, dto));
    }

    @PostMapping("/{id}/messages")
    public ResultVO<AgentSendResultVO> send(
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationMessageDTO dto) {
        return ResultVO.success(service.send(id, dto));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable @Positive long id, @Valid @RequestBody AgentConversationMessageDTO dto) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.stream(id, dto));
    }
}
