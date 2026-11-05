package com.asterway.stage.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthReorderDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.stage.dto.StageSaveDTO;
import com.asterway.stage.service.StageService;
import com.asterway.stage.vo.StageStateHistoryVO;
import com.asterway.stage.vo.StageVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Stage REST 入口；只绑定、校验请求并调用 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class StageController {
    @GetMapping("/stages/{id}/state-history")
    public ResultVO<PageResultVO<StageStateHistoryVO>> history(
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.history(id, query));
    }

    private final StageService service;

    public StageController(StageService service) {
        this.service = service;
    }

    @GetMapping("/stages/{id}")
    public ResultVO<StageVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @PatchMapping("/stages/{id}")
    public ResultVO<StageVO> update(
            @PathVariable @Positive long id, @Valid @RequestBody StageSaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 完整替换基本资料；可选字段为 null 时清空。 */
    @PutMapping("/stages/{id}")
    public ResultVO<StageVO> replace(
            @PathVariable @Positive long id, @Valid @RequestBody StageSaveDTO dto) {
        return ResultVO.success(service.replace(id, dto));
    }

    @PatchMapping("/stages/{id}/status")
    public ResultVO<StageVO> status(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthStatusDTO dto) {
        return ResultVO.success(service.status(id, dto));
    }

    @DeleteMapping("/stages/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }

    @PostMapping("/goals/{parentId}/stages")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<StageVO> create(
            @PathVariable @Positive long parentId, @Valid @RequestBody StageSaveDTO dto) {
        return ResultVO.success(service.create(parentId, dto));
    }

    @PutMapping("/goals/{parentId}/stages/order")
    public ResultVO<Void> reorder(
            @PathVariable @Positive long parentId, @Valid @RequestBody GrowthReorderDTO dto) {
        service.reorder(parentId, dto);
        return ResultVO.success(null);
    }

    @GetMapping("/goals/{parentId}/stages")
    public ResultVO<PageResultVO<StageVO>> page(
            @PathVariable @Positive long parentId, @Valid @ModelAttribute GrowthQueryDTO query) {
        query.setGoalId(parentId);
        return ResultVO.success(service.page(query));
    }
}
