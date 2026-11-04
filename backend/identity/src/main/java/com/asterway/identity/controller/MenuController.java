package com.asterway.identity.controller;

import com.asterway.common.result.ResultVO;
import com.asterway.identity.dto.MenuSaveDTO;
import com.asterway.identity.service.MenuService;
import com.asterway.identity.vo.MenuVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 菜单与按钮的同一 HTTP 管理入口。 */
@RestController
@Validated
@RequestMapping("/asterway/menus")
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    /** 读取平铺菜单。 */
    @GetMapping
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:list')")
    public ResultVO<List<MenuVO>> list() {
        return ResultVO.success(service.list());
    }

    /** 读取管理菜单树。 */
    @GetMapping("/tree")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:list')")
    public ResultVO<List<MenuVO>> tree() {
        return ResultVO.success(service.tree());
    }

    /** 读取单个菜单。 */
    @GetMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:read')")
    public ResultVO<MenuVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    /** 新增目录、页面或按钮。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:create')")
    public ResultVO<MenuVO> create(@Valid @RequestBody MenuSaveDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    /** 更新菜单。 */
    @PatchMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:update')")
    public ResultVO<MenuVO> update(@PathVariable @Positive long id, @Valid @RequestBody MenuSaveDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 删除未引用的扩展菜单。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:delete')")
    public ResultVO<Void> delete(@PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }
}
