package com.asterway.identity.controller;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.PermissionQueryDTO;
import com.asterway.identity.dto.PermissionSaveDTO;
import com.asterway.identity.service.PermissionService;
import com.asterway.identity.vo.PermissionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 动作权限管理链路，新增、修改与删除共用同一服务。 */
@RestController
@Validated
@RequestMapping("/asterway/permissions")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    /** 分页查询权限。 */
    @GetMapping
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:list')")
    public ResultVO<PageResultVO<PermissionVO>> page(@Valid @ModelAttribute PermissionQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    /** 读取权限。 */
    @GetMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:read')")
    public ResultVO<PermissionVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    /** 新增扩展权限。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:create')")
    public ResultVO<PermissionVO> create(@Valid @RequestBody PermissionSaveDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    /** 更新权限。 */
    @PatchMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:update')")
    public ResultVO<PermissionVO> update(@PathVariable @Positive long id, @Valid @RequestBody PermissionSaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 删除未引用的扩展权限。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:delete')")
    public ResultVO<Void> delete(@PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }
}
