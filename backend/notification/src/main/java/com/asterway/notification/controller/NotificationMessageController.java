package com.asterway.notification.controller;

import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.notification.dto.*;
import com.asterway.notification.service.NotificationMessageService;
import com.asterway.notification.vo.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 本人站内消息入口；读取、已读、确认和继续分别表达业务动作。 */
@RestController
@Validated
@RequestMapping("/asterway/notifications")
public class NotificationMessageController {
    private final NotificationMessageService service;

    public NotificationMessageController(NotificationMessageService service) {
        this.service = service;
    }

    @GetMapping
    public ResultVO<PageResultVO<NotificationMessageVO>> page(
            @Valid @ModelAttribute NotificationQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    @GetMapping("/{id}")
    public ResultVO<NotificationMessageVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    @GetMapping("/unread-count")
    public ResultVO<NotificationUnreadVO> unread() {
        return ResultVO.success(service.unread());
    }

    @PatchMapping("/{id}/read")
    public ResultVO<NotificationMessageVO> markRead(
            @PathVariable @Positive long id, @Valid @RequestBody GrowthVersionDTO dto) {
        return ResultVO.success(service.markRead(id, dto.version()));
    }

    @PostMapping("/read-all")
    public ResultVO<NotificationUnreadVO> markAllRead() {
        return ResultVO.success(service.markAllRead());
    }

    @PostMapping("/{id}/confirm")
    public ResultVO<NotificationMessageVO> confirm(
            @PathVariable @Positive long id, @Valid @RequestBody NotificationHandleDTO dto) {
        return ResultVO.success(service.handle(id, true, dto));
    }

    @PostMapping("/{id}/continue")
    public ResultVO<NotificationMessageVO> continueGoal(
            @PathVariable @Positive long id, @Valid @RequestBody NotificationHandleDTO dto) {
        return ResultVO.success(service.handle(id, false, dto));
    }
}
