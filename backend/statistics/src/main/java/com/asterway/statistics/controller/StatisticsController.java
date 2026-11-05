package com.asterway.statistics.controller;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.statistics.dto.StatisticsHabitQueryDTO;
import com.asterway.statistics.dto.StatisticsQueryDTO;
import com.asterway.statistics.service.StatisticsService;
import com.asterway.statistics.vo.*;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 历史统计只读 REST 入口，不接受人工修改历史或耗时。 */
@RestController
@RequestMapping("/asterway/statistics")
public class StatisticsController {
    private final StatisticsService service;

    public StatisticsController(StatisticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResultVO<StatisticsSummaryVO> summary(@Valid @ModelAttribute StatisticsQueryDTO query) {
        return ResultVO.success(service.summary(query));
    }

    @GetMapping("/trends")
    public ResultVO<PageResultVO<StatisticsTrendVO>> trends(
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        return ResultVO.success(service.trends(query));
    }

    @GetMapping("/timeline")
    public ResultVO<PageResultVO<StatisticsTimelineVO>> timeline(
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        return ResultVO.success(service.timeline(query));
    }

    @GetMapping("/habits")
    public ResultVO<StatisticsHabitReportVO> habits(
            @Valid @ModelAttribute StatisticsHabitQueryDTO query) {
        return ResultVO.success(service.habits(query));
    }

    @GetMapping("/objects")
    public ResultVO<List<StatisticsObjectVO>> objects(
            @Valid @ModelAttribute StatisticsQueryDTO query) {
        return ResultVO.success(service.objects(query));
    }
}
