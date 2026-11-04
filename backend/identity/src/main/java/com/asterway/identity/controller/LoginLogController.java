package com.asterway.identity.controller;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.service.LoginLogService;
import com.asterway.identity.vo.LoginLogVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录日志只提供按范围查询。 */
@RestController
@Validated
@RequestMapping("/asterway/login-logs")
public class LoginLogController {

    private final LoginLogService service;

    public LoginLogController(LoginLogService service) {
        this.service = service;
    }

    /** 分页查询可见审计记录。 */
    @GetMapping
    @PreAuthorize("@access.hasPermission('identity:login_log:list')")
    public ResultVO<PageResultVO<LoginLogVO>> page(@Valid @ModelAttribute LogQueryDTO query) {
        return ResultVO.success(service.page(query));
    }
}
