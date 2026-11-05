package com.asterway.task.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthReorderDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.task.dto.TaskSaveDTO;
import com.asterway.task.service.TaskService;
import com.asterway.task.vo.TaskStateHistoryVO;
import com.asterway.task.vo.TaskVO;

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

/** Task REST 入口；只绑定、校验请求并调用 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class TaskController {
    @GetMapping("/tasks/{id}/state-history")
    public ResultVO<PageResultVO<TaskStateHistoryVO>> history(
            @PathVariable @Positive long id, @Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.history(id, query));
    }

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/tasks/{id}")
    public ResultVO<TaskVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @PatchMapping("/tasks/{id}")
    public ResultVO<TaskVO> update(
            @PathVariable @Positive long id, @Valid @RequestBody TaskSaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 完整替换基本资料；可选字段为 null 时清空。 */
    @PutMapping("/tasks/{id}")
    public ResultVO<TaskVO> replace(
            @PathVariable @Positive long id, @Valid @RequestBody TaskSaveDTO dto) {
        return ResultVO.success(service.replace(id, dto));
    }

    @PatchMapping("/tasks/{id}/status")
    public ResultVO<TaskVO> status(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthStatusDTO dto) {
        return ResultVO.success(service.status(id, dto));
    }

    @DeleteMapping("/tasks/{id}")
    public ResultVO<Void> delete(
            @PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }

    @PostMapping("/stages/{parentId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<TaskVO> create(
            @PathVariable @Positive long parentId, @Valid @RequestBody TaskSaveDTO dto) {
        return ResultVO.success(service.create(parentId, dto));
    }

    @PutMapping("/stages/{parentId}/tasks/order")
    public ResultVO<Void> reorder(
            @PathVariable @Positive long parentId, @Valid @RequestBody GrowthReorderDTO dto) {
        service.reorder(parentId, dto);
        return ResultVO.success(null);
    }

    @GetMapping("/tasks")
    public ResultVO<PageResultVO<TaskVO>> page(@Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.page(query));
    }
}
