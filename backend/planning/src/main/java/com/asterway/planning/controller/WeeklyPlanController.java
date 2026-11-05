package com.asterway.planning.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.planning.dto.*;
import com.asterway.planning.service.WeeklyPlanService;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.WeeklyPlanVO;

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

/** WeeklyPlan REST 入口；认证身份与数据范围由 Service 校验。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class WeeklyPlanController {
    private final WeeklyPlanService service;

    public WeeklyPlanController(WeeklyPlanService service) {
        this.service = service;
    }

    @GetMapping("/weekly-plans")
    public ResultVO<PageResultVO<WeeklyPlanVO>> page(@Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @GetMapping("/weekly-plans/{id}")
    public ResultVO<WeeklyPlanVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @GetMapping("/weekly-plans/by-week/{date}")
    public ResultVO<WeeklyPlanVO> byWeek(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResultVO.success(service.byWeek(date, false));
    }

    @PutMapping("/weekly-plans/by-week/{date}")
    public ResultVO<WeeklyPlanVO> create(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResultVO.success(service.byWeek(date, true));
    }

    @GetMapping("/weekly-plans/{id}/tasks")
    public ResultVO<PageResultVO<PlanningTaskVO>> tasks(
            @PathVariable @Positive long id, @Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.tasks(id, query));
    }

    @DeleteMapping("/weekly-plans/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id,
            @RequestParam @PositiveOrZero long version,
            @RequestParam(required = false) String handling) {
        service.delete(id, version, handling);
        return ResultVO.success(null);
    }
}
