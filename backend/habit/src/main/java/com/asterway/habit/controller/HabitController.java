package com.asterway.habit.controller;

import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.habit.dto.*;
import com.asterway.habit.service.HabitService;
import com.asterway.habit.vo.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 习惯主链入口；打卡不接收日期或自填时间。 */
@RestController
@Validated
@RequestMapping("/asterway/habits")
public class HabitController {
    private final HabitService service;

    public HabitController(HabitService service) {
        this.service = service;
    }

    @GetMapping
    public ResultVO<PageResultVO<HabitVO>> page(@Valid @ModelAttribute HabitQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @GetMapping("/today")
    public ResultVO<PageResultVO<HabitTodayVO>> today(@Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.today(query));
    }

    @GetMapping("/{id}")
    public ResultVO<HabitVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<HabitVO> create(@Valid @RequestBody HabitSaveDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    @PatchMapping("/{id}")
    public ResultVO<HabitVO> patch(
            @PathVariable @Positive long id, @Valid @RequestBody HabitSaveDTO dto) {
        return ResultVO.success(service.update(id, dto, false));
    }

    @PutMapping("/{id}")
    public ResultVO<HabitVO> replace(
            @PathVariable @Positive long id, @Valid @RequestBody HabitSaveDTO dto) {
        return ResultVO.success(service.update(id, dto, true));
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }

    @PostMapping("/{id}/pause")
    public ResultVO<HabitVO> pause(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.pause(id, dto.version()));
    }

    @PostMapping("/{id}/resume")
    public ResultVO<HabitVO> resume(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.resume(id, dto.version()));
    }

    @PostMapping("/{id}/end")
    public ResultVO<HabitVO> end(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.end(id, dto.version()));
    }

    @PostMapping("/{id}/reopen")
    public ResultVO<HabitVO> reopen(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.reopen(id, dto.version()));
    }

    @PostMapping("/{id}/checkins")
    public ResultVO<HabitCheckinVO> checkin(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.checkin(id, dto.version()));
    }

    @DeleteMapping("/{id}/checkins/{checkinId}")
    public ResultVO<HabitCheckinVO> cancel(
            @PathVariable @Positive long id,
            @PathVariable @Positive long checkinId,
            @RequestParam @PositiveOrZero long version) {
        return ResultVO.success(service.cancel(id, checkinId, version));
    }

    @GetMapping("/{id}/checkins")
    public ResultVO<PageResultVO<HabitCheckinVO>> checkins(
            @PathVariable @Positive long id, @Valid @ModelAttribute HabitCheckinQueryDTO query) {
        return ResultVO.success(service.checkins(id, query));
    }

    @GetMapping("/{id}/revisions")
    public ResultVO<PageResultVO<HabitRevisionVO>> revisions(
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.revisions(id, query));
    }
}
