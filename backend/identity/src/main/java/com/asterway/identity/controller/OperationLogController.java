package com.asterway.identity.controller;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.vo.OperationLogVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 操作审计只提供按范围查询。 */
@RestController
@Validated
@RequestMapping("/asterway/operation-logs")
public class OperationLogController {

    private final OperationLogService service;

    public OperationLogController(OperationLogService service) {
        this.service = service;
    }

    /** 分页查询可见审计记录。 */
    @GetMapping
    @PreAuthorize("@access.hasPermission('identity:operation_log:list')")
    public ResultVO<PageResultVO<OperationLogVO>> page(@Valid @ModelAttribute LogQueryDTO query) {
        return ResultVO.success(service.page(query));
    }
}
