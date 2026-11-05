package com.asterway.planning.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.planning.dto.*;
import com.asterway.planning.service.DailyPlanService;
import com.asterway.planning.vo.DailyPlanVO;
import com.asterway.planning.vo.PlanningTaskVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** DailyPlan REST 入口；认证身份与数据范围由 Service 校验。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class DailyPlanController {
    private final DailyPlanService service;

    public DailyPlanController(DailyPlanService service) {
        this.service = service;
    }

    @GetMapping("/weekly-plans/{weeklyPlanId}/daily-plans/{date}")
    public ResultVO<DailyPlanVO> byDate(
            @PathVariable @Positive long weeklyPlanId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResultVO.success(service.findByDate(weeklyPlanId, date));
    }

    @GetMapping("/daily-plans/{id}")
    public ResultVO<DailyPlanVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @PutMapping("/weekly-plans/{weeklyPlanId}/daily-plans/{date}")
    public ResultVO<DailyPlanVO> create(
            @PathVariable @Positive long weeklyPlanId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResultVO.success(service.byDate(weeklyPlanId, date));
    }

    @GetMapping("/weekly-plans/{weeklyPlanId}/daily-plans")
    public ResultVO<PageResultVO<DailyPlanVO>> page(
            @PathVariable @Positive long weeklyPlanId,
            @Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.page(weeklyPlanId, query));
    }

    @GetMapping("/daily-plans/{id}/tasks")
    public ResultVO<PageResultVO<PlanningTaskVO>> tasks(
            @PathVariable @Positive long id, @Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.tasks(id, query));
    }

    @DeleteMapping("/daily-plans/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id,
            @RequestParam @PositiveOrZero long version,
            @RequestParam(required = false) String handling) {
        service.delete(id, version, handling);
        return ResultVO.success(null);
    }
}
