package com.asterway.identity.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.AccountValidationUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.MenuSaveDTO;
import com.asterway.identity.entity.MenuEntity;
import com.asterway.identity.entity.PermissionEntity;
import com.asterway.identity.mapper.MenuMapper;
import com.asterway.identity.mapper.PermissionMapper;
import com.asterway.identity.mapper.RoleMapper;
import com.asterway.identity.service.MenuService;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.vo.MenuVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 菜单管理与当前授权菜单共用同一链路，不由前端决定实际接口权限。 */
@Service
public class MenuServiceImpl implements MenuService {

    private final MenuMapper mapper;

    private final PermissionMapper permissions;

    private final RoleMapper roles;

    private final OperationLogService logs;

    public MenuServiceImpl(
            MenuMapper mapper,
            PermissionMapper permissions,
            RoleMapper roles,
            OperationLogService logs) {
        this.mapper = mapper;
        this.permissions = permissions;
        this.roles = roles;
        this.logs = logs;
    }

    private static final Set<String> COMPONENTS =
            Set.of(
                    "account/profile",
                    "account/devices",
                    "system/users",
                    "system/roles",
                    "system/permissions",
                    "system/menus",
                    "system/sessions",
                    "system/login_logs",
                    "system/operation_logs",
                    "system/account_settings",
                    "growth/goals",
                    "growth/stages",
                    "growth/tasks",
                    "planning/weekly",
                    "planning/daily",
                    "planning/pending",
                    "execution/index",
                    "habit/index",
                    "habit/categories",
                    "notification/index",
                    "dashboard/index",
                    "statistics/index",
                    "agent/index",
                    "agent/memories",
                    "agent/knowledge",
                    "agent/runs",
                    "agent/reviews");

    @Override
    public List<MenuVO> list() {
        SecurityUtils.requireSuperAdmin("identity:menu:list");
        return mapper.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public List<MenuVO> tree() {
        SecurityUtils.requireSuperAdmin("identity:menu:list");
        return buildTree(mapper.findAll());
    }

    @Override
    public List<MenuVO> currentTree() {
        var actor = SecurityUtils.current();
        if (actor.mustChangePassword()) {
            return List.of();
        }
        List<MenuEntity> all = mapper.findAll();
        Set<Long> assigned =
                actor.superAdmin()
                        ? all.stream().map(MenuEntity::getId).collect(Collectors.toSet())
                        : new HashSet<>(mapper.findGrantedIds(actor.userId()));
        Map<Long, String> permissionCodes = new HashMap<>();
        for (PermissionEntity permission : permissions.findAll()) {
            if (Boolean.TRUE.equals(permission.getEnabled())) {
                permissionCodes.put(permission.getId(), permission.getCode());
            }
        }
        List<MenuEntity> allowed =
                all.stream()
                        .filter(
                                row ->
                                        assigned.contains(row.getId())
                                                && Boolean.TRUE.equals(row.getEnabled())
                                                && Boolean.TRUE.equals(row.getVisible()))
                        .filter(
                                row ->
                                        row.getPermissionId() == null
                                                || actor.superAdmin()
                                                || actor.permissions()
                                                        .contains(
                                                                permissionCodes.get(
                                                                        row.getPermissionId())))
                        .toList();
        if (com.asterway.common.utils.GrowthAccessUtils.manager(actor.roles())) {
            allowed =
                    allowed.stream()
                            .filter(
                                    row ->
                                            !com.asterway.common.utils.GrowthAccessUtils.menu(
                                                            row.getCode(), row.getComponentKey())
                                                    && !com.asterway.common.utils.GrowthAccessUtils
                                                            .permission(
                                                                    permissionCodes.get(
                                                                            row.getPermissionId())))
                            .toList();
        }
        return buildTree(allowed);
    }

    @Override
    public MenuVO get(long id) {
        SecurityUtils.requireSuperAdmin("identity:menu:read");
        return toVO(required(id));
    }

    @Override
    @Transactional
    public MenuVO create(MenuSaveDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:create");
        roles.lockSecurityGate();
        MenuEntity row = new MenuEntity();
        validateAndFill(row, dto);
        row.setCreatedBy(actor.userId());
        row.setUpdatedBy(actor.userId());
        mapper.insert(row);
        logs.append(
                "identity:menu:create", "MENU", row.getId().toString(), null, Map.of("version", 0));
        return toVO(required(row.getId()));
    }

    @Override
    @Transactional
    public MenuVO update(long id, MenuSaveDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:update");
        roles.lockSecurityGate();
        MenuEntity row = required(id);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code())) {
            throw BusinessException.conflict("内置菜单编码不能修改");
        }
        validateAndFill(row, dto);
        row.setUpdatedBy(actor.userId());
        if (mapper.update(row) != 1) {
            throw BusinessException.conflict("菜单已被修改，请刷新");
        }
        logs.append(
                "identity:menu:update",
                "MENU",
                Long.toString(id),
                null,
                Map.of("version", dto.version()));
        return toVO(required(id));
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:delete");
        roles.lockSecurityGate();
        MenuEntity row = required(id);
        AccountValidationUtils.version(version, row.getVersion());
        if (Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0) {
            throw BusinessException.conflict("内置菜单、已分配菜单或仍有子节点的菜单不能删除");
        }
        row.setUpdatedBy(actor.userId());
        if (mapper.softDelete(row) != 1) {
            throw BusinessException.conflict("菜单已被修改，请刷新");
        }
        logs.append(
                "identity:menu:delete",
                "MENU",
                Long.toString(id),
                null,
                Map.of("version", version));
    }

    private MenuEntity required(long id) {
        MenuEntity row = mapper.findById(id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private void validateAndFill(MenuEntity row, MenuSaveDTO dto) {
        if (dto.name() == null
                || dto.name().isBlank()
                || dto.code() == null
                || !dto.code().matches("^[a-z][a-z0-9_]{1,63}$")
                || dto.sortOrder() == null
                || dto.sortOrder() < 0
                || dto.visible() == null
                || dto.enabled() == null) {
            throw BusinessException.badRequest("菜单基本字段无效");
        }
        String type = dto.menuType();
        if (!List.of("DIRECTORY", "PAGE", "BUTTON").contains(type)) {
            throw BusinessException.badRequest("菜单类型无效");
        }
        if ("BUTTON".equals(type)) {
            if (dto.routePath() != null
                    || dto.componentKey() != null
                    || dto.permissionId() == null
                    || dto.parentId() == null) {
                throw BusinessException.badRequest("按钮必须属于页面并关联权限，不设置路由和组件");
            }
        } else {
            if (dto.routePath() == null || !dto.routePath().matches("/[A-Za-z0-9/_-]*")) {
                throw BusinessException.badRequest("菜单路径无效");
            }
            if ("PAGE".equals(type)
                    && (dto.componentKey() == null
                            || !COMPONENTS.contains(dto.componentKey())
                            || dto.permissionId() == null)) {
                throw BusinessException.badRequest("页面必须关联权限并使用有效组件键");
            }
            if ("DIRECTORY".equals(type)
                    && (dto.componentKey() != null || dto.permissionId() != null)) {
                throw BusinessException.badRequest("目录不设置页面组件或动作权限");
            }
        }
        if (dto.permissionId() != null) {
            PermissionEntity permission = permissions.findById(dto.permissionId());
            if (permission == null || !Boolean.TRUE.equals(permission.getEnabled())) {
                throw BusinessException.badRequest("关联权限不存在或已停用");
            }
        }
        if (row.getId() != null && !type.equals(row.getMenuType())) {
            for (MenuEntity child : mapper.findAll()) {
                if (!row.getId().equals(child.getParentId())) {
                    continue;
                }
                boolean compatible =
                        "DIRECTORY".equals(type) && !"BUTTON".equals(child.getMenuType())
                                || "PAGE".equals(type) && "BUTTON".equals(child.getMenuType());
                if (!compatible) {
                    throw BusinessException.conflict("已有子节点与新菜单类型不兼容，请先调整子节点");
                }
            }
        }
        Set<Long> seen = new HashSet<>();
        Long parentId = dto.parentId();
        if (row.getId() != null) {
            seen.add(row.getId());
        }
        boolean immediate = true;
        while (parentId != null) {
            if (!seen.add(parentId)) {
                throw BusinessException.badRequest("菜单父链不能形成循环");
            }
            MenuEntity parent = mapper.findById(parentId);
            if (parent == null || "BUTTON".equals(parent.getMenuType())) {
                throw BusinessException.badRequest("父菜单不存在或不能拥有子节点");
            }
            if (immediate
                    && !("BUTTON".equals(type) ? "PAGE" : "DIRECTORY")
                            .equals(parent.getMenuType())) {
                throw BusinessException.badRequest("父节点类型不匹配");
            }
            immediate = false;
            parentId = parent.getParentId();
        }
        row.setCode(dto.code());
        row.setParentId(dto.parentId());
        row.setName(dto.name());
        row.setMenuType(type);
        row.setRoutePath(dto.routePath());
        row.setComponentKey(dto.componentKey());
        row.setPermissionId(dto.permissionId());
        row.setIcon(dto.icon());
        row.setSortOrder(dto.sortOrder());
        row.setVisible(dto.visible());
        row.setEnabled(dto.enabled());
    }

    private List<MenuVO> buildTree(List<MenuEntity> rows) {
        Map<Long, MenuEntity> byId = new LinkedHashMap<>();
        rows.forEach(row -> byId.put(row.getId(), row));
        Map<Long, MenuVO> accepted = new LinkedHashMap<>();
        for (MenuEntity row : rows) {
            Set<Long> seen = new HashSet<>();
            MenuEntity cursor = row;
            boolean valid = true;
            while (cursor != null) {
                if (!seen.add(cursor.getId())) {
                    valid = false;
                    break;
                }
                if (cursor.getParentId() == null) {
                    break;
                }
                cursor = byId.get(cursor.getParentId());
                if (cursor == null) {
                    valid = false;
                }
            }
            if (valid) {
                accepted.put(row.getId(), toVO(row));
            }
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuEntity row : rows) {
            MenuVO node = accepted.get(row.getId());
            if (node == null) {
                continue;
            }
            if (row.getParentId() == null) {
                roots.add(node);
            } else {
                MenuVO parent = accepted.get(row.getParentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }

    private MenuVO toVO(MenuEntity row) {
        return new MenuVO(
                row.getId().toString(),
                row.getCode(),
                row.getParentId() == null ? null : row.getParentId().toString(),
                row.getName(),
                row.getMenuType(),
                row.getRoutePath(),
                row.getComponentKey(),
                row.getPermissionId() == null ? null : row.getPermissionId().toString(),
                row.getIcon(),
                row.getSortOrder(),
                Boolean.TRUE.equals(row.getVisible()),
                Boolean.TRUE.equals(row.getEnabled()),
                Boolean.TRUE.equals(row.getBuiltin()),
                row.getVersion(),
                new ArrayList<>());
    }
}
