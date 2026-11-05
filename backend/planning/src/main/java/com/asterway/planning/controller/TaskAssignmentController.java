package com.asterway.planning.controller;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.planning.dto.AssignmentSaveDTO;
import com.asterway.planning.service.TaskAssignmentService;
import com.asterway.planning.vo.PlanningTaskVO;
import com.asterway.planning.vo.TaskAssignmentVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TaskAssignment REST 入口；认证身份与数据范围由 Service 校验。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class TaskAssignmentController {
    private final TaskAssignmentService service;

    public TaskAssignmentController(TaskAssignmentService service) {
        this.service = service;
    }

    @GetMapping("/tasks/{taskId}/assignment")
    public ResultVO<TaskAssignmentVO> get(@PathVariable @Positive long taskId) {
        return ResultVO.success(service.get(taskId));
    }

    @PutMapping("/tasks/{taskId}/assignment")
    public ResultVO<TaskAssignmentVO> assign(
            @PathVariable @Positive long taskId, @Valid @RequestBody AssignmentSaveDTO dto) {
        return ResultVO.success(service.assign(taskId, dto));
    }

    @DeleteMapping("/tasks/{taskId}/assignment")
    public ResultVO<Void> remove(
            @PathVariable @Positive long taskId,
            @RequestParam @Positive long assignmentId,
            @RequestParam @PositiveOrZero long version) {
        service.remove(taskId, assignmentId, version);
        return ResultVO.success(null);
    }

    @GetMapping("/tasks/{taskId}/assignment-history")
    public ResultVO<PageResultVO<TaskAssignmentVO>> history(
            @PathVariable @Positive long taskId, @Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.history(taskId, query));
    }

    @GetMapping("/planning/pending-tasks")
    public ResultVO<PageResultVO<PlanningTaskVO>> pending(
            @Valid @ModelAttribute GrowthQueryDTO query) {
        return ResultVO.success(service.pending(query));
    }
}
