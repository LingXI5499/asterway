package com.asterway.execution.controller;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.execution.dto.ExecutionControlDTO;
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
import com.asterway.execution.dto.ExecutionQueryDTO;
import com.asterway.execution.dto.ExecutionStartDTO;
import com.asterway.execution.service.ExecutionSessionService;
import com.asterway.execution.vo.ExecutionDetailVO;
import com.asterway.execution.vo.ExecutionSegmentVO;
import com.asterway.execution.vo.ExecutionSessionVO;
import com.asterway.execution.vo.ExecutionSummaryVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 执行会话的十四个 REST 入口；校验请求并调用同一 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class ExecutionSessionController {
    private final ExecutionSessionService service;

    public ExecutionSessionController(ExecutionSessionService service) {
        this.service = service;
    }

    /** 新建返回 201，幂等返回和恢复原会话返回 200，data 结构保持一致。 */
    @PostMapping("/executions")
    public ResponseEntity<ResultVO<ExecutionSessionVO>> start(
            @Valid @RequestBody ExecutionStartDTO dto) {
        var result = service.start(dto);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ResultVO.success(result.execution()));
    }

    @GetMapping("/executions/current")
    public ResultVO<ExecutionSessionVO> current() {
        return ResultVO.success(service.current());
    }

    @PostMapping("/executions/{id}/pause")
    public ResultVO<ExecutionSessionVO> pause(
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        return ResultVO.success(service.pause(id, dto));
    }

    @PostMapping("/executions/{id}/resume")
    public ResultVO<ExecutionSessionVO> resume(
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        return ResultVO.success(service.resume(id, dto));
    }

    @PostMapping("/executions/{id}/finish")
    public ResultVO<ExecutionSessionVO> finish(
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionControlDTO dto) {
        return ResultVO.success(service.finish(id, dto));
    }

    @PostMapping("/executions/{id}/heartbeat")
    public ResultVO<ExecutionSessionVO> heartbeat(
            @PathVariable @Positive long id, @Valid @RequestBody ExecutionHeartbeatDTO dto) {
        return ResultVO.success(service.heartbeat(id, dto));
    }

    @GetMapping("/executions")
    public ResultVO<PageResultVO<ExecutionSessionVO>> page(
            @Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @GetMapping("/executions/{id}")
    public ResultVO<ExecutionDetailVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @GetMapping("/executions/{id}/segments")
    public ResultVO<PageResultVO<ExecutionSegmentVO>> segments(
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.segments(id, query));
    }

    @GetMapping("/tasks/{id}/executions")
    public ResultVO<PageResultVO<ExecutionSessionVO>> taskExecutions(
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.taskExecutions(id, query));
    }

    @GetMapping("/tasks/{id}/execution-summary")
    public ResultVO<ExecutionSummaryVO> taskSummary(
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.summary(ExecutionScopeEnum.TASK, id, query));
    }

    @GetMapping("/stages/{id}/execution-summary")
    public ResultVO<ExecutionSummaryVO> stageSummary(
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.summary(ExecutionScopeEnum.STAGE, id, query));
    }

    @GetMapping("/goals/{id}/execution-summary")
    public ResultVO<ExecutionSummaryVO> goalSummary(
            @PathVariable @Positive long id, @Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.summary(ExecutionScopeEnum.GOAL, id, query));
    }

    @GetMapping("/executions/summary")
    public ResultVO<ExecutionSummaryVO> summary(@Valid @ModelAttribute ExecutionQueryDTO query) {
        return ResultVO.success(service.summary(null, null, query));
    }
}
