package com.asterway.identity.controller;

import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.SettingUpdateDTO;
import com.asterway.identity.service.SettingService;
import com.asterway.identity.vo.PublicSettingVO;
import com.asterway.identity.vo.SettingVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 两项账户配置的读取与更新入口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class SettingController {

    private final SettingService service;

    public SettingController(SettingService service) {
        this.service = service;
    }

    /** 读取公开配置。 */
    @GetMapping("/public/account-settings")
    public ResultVO<PublicSettingVO> publicSettings() {
        return ResultVO.success(service.publicSettings());
    }

    /** 超管读取完整配置及版本。 */
    @GetMapping("/settings/account")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:setting:read')")
    public ResultVO<SettingVO> get() {
        return ResultVO.success(service.get());
    }

    /** 超管原子更新两项配置。 */
    @PutMapping("/settings/account")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:setting:update')")
    public ResultVO<SettingVO> update(@Valid @RequestBody SettingUpdateDTO dto) {
        return ResultVO.success(service.update(dto));
    }
}
