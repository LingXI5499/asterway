package com.asterway.identity.controller;

import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.MenuGrantDTO;
import com.asterway.identity.dto.PermissionGrantDTO;
import com.asterway.identity.dto.RoleUpdateDTO;
import com.asterway.identity.service.RoleService;
import com.asterway.identity.vo.RoleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 角色及其权限菜单授权共用一个 Controller，不按动作拆分。 */
@RestController
@Validated
@RequestMapping("/asterway/roles")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    /** 查询三个内置角色。 */
    @GetMapping
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:list')")
    public ResultVO<List<RoleVO>> list() {
        return ResultVO.success(service.list());
    }

    /** 读取角色详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    public ResultVO<RoleVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    /** 修改角色展示信息。 */
    @PatchMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:update')")
    public ResultVO<RoleVO> update(@PathVariable @Positive long id, @Valid @RequestBody RoleUpdateDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 读取角色动作权限集合。 */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    public ResultVO<List<String>> permissions(@PathVariable @Positive long id) {
        return ResultVO.success(service.permissions(id));
    }

    /** 替换角色动作权限集合。 */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:grant_permission')")
    public ResultVO<RoleVO> grantPermissions(
            @PathVariable @Positive long id,
            @Valid @RequestBody PermissionGrantDTO dto
    ) {
        return ResultVO.success(service.grantPermissions(id, dto));
    }

    /** 读取角色菜单集合。 */
    @GetMapping("/{id}/menus")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    public ResultVO<List<String>> menus(@PathVariable @Positive long id) {
        return ResultVO.success(service.menus(id));
    }

    /** 替换角色菜单并补齐祖先节点。 */
    @PutMapping("/{id}/menus")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:grant_menu')")
    public ResultVO<RoleVO> grantMenus(@PathVariable @Positive long id, @Valid @RequestBody MenuGrantDTO dto) {
        return ResultVO.success(service.grantMenus(id, dto));
    }
}
