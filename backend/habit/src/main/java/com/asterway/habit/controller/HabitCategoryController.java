package com.asterway.habit.controller;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.habit.dto.*;
import com.asterway.habit.service.HabitCategoryService;
import com.asterway.habit.vo.HabitCategoryVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 用户个人分类 REST 入口，只绑定参数并调用 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway/habit-categories")
public class HabitCategoryController {
    private final HabitCategoryService service;

    public HabitCategoryController(HabitCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResultVO<PageResultVO<HabitCategoryVO>> page(
            @Valid @ModelAttribute HabitQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<HabitCategoryVO> create(@Valid @RequestBody HabitCategorySaveDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResultVO<HabitCategoryVO> update(
            @PathVariable @Positive long id, @Valid @RequestBody HabitCategorySaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }
}
