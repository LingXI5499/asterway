package com.asterway.goal.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.goal.dto.GoalArchiveDTO;
import com.asterway.goal.dto.GoalSaveDTO;
import com.asterway.goal.service.GoalService;
import com.asterway.goal.vo.GoalStateHistoryVO;
import com.asterway.goal.vo.GoalVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Goal REST 入口；只绑定、校验请求并调用 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class GoalController {
    @GetMapping("/goals/{id}/state-history")
    public ResultVO<PageResultVO<GoalStateHistoryVO>> history(
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.history(id, query));
    }

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping("/goals/{id}")
    public ResultVO<GoalVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @PatchMapping("/goals/{id}")
    public ResultVO<GoalVO> update(
            @PathVariable @Positive long id, @Valid @RequestBody GoalSaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 完整替换基本资料；可选字段为 null 时清空。 */
    @PutMapping("/goals/{id}")
    public ResultVO<GoalVO> replace(
            @PathVariable @Positive long id, @Valid @RequestBody GoalSaveDTO dto) {
        return ResultVO.success(service.replace(id, dto));
    }

    @PatchMapping("/goals/{id}/status")
    public ResultVO<GoalVO> status(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthStatusDTO dto) {
        return ResultVO.success(service.status(id, dto));
    }

    @DeleteMapping("/goals/{id}")
    public ResultVO<Void> delete(@PathVariable @Positive long id) {
        service.delete(id, 0L);
        return ResultVO.success(null);
    }

    @GetMapping("/goals")
    public ResultVO<PageResultVO<GoalVO>> page(@Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<GoalVO> create(@Valid @RequestBody GoalSaveDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    @PostMapping("/goals/{id}/archive")
    public ResultVO<GoalVO> archive(
            @PathVariable @Positive long id, @Valid @RequestBody GoalArchiveDTO dto) {
        return ResultVO.success(service.archive(id, dto));
    }

    @PostMapping("/goals/{id}/restore")
    public ResultVO<GoalVO> restore(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.restore(id, dto.version()));
    }
}
