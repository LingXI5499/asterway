package com.asterway.dashboard.controller;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
import com.asterway.dashboard.service.DashboardService;
import com.asterway.dashboard.vo.*;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

/** 当前普通用户的今日看板，不支持指定其他用户。 */
@RestController
@RequestMapping("/asterway/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ResultVO<DashboardOverviewVO> overview() {
        return ResultVO.success(service.overview());
    }

    @GetMapping("/tasks")
    public ResultVO<PageResultVO<DashboardTaskVO>> tasks(
            @Valid @ModelAttribute DashboardTaskQueryDTO query) {
        return ResultVO.success(service.tasks(query));
    }

    @GetMapping("/goals")
    public ResultVO<PageResultVO<DashboardGoalVO>> goals(
            @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.goals(query));
    }

    @GetMapping("/habits")
    public ResultVO<PageResultVO<DashboardHabitVO>> habits(
            @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.habits(query));
    }
}
