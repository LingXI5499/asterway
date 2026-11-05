package com.asterway.identity.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.AccountValidationUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.MenuGrantDTO;
import com.asterway.identity.dto.PermissionGrantDTO;
import com.asterway.identity.dto.RoleUpdateDTO;
import com.asterway.identity.entity.MenuEntity;
import com.asterway.identity.entity.PermissionEntity;
import com.asterway.identity.entity.RoleEntity;
import com.asterway.identity.mapper.MenuMapper;
import com.asterway.identity.mapper.PermissionMapper;
import com.asterway.identity.mapper.RoleMapper;
import com.asterway.identity.mapper.RoleMenuMapper;
import com.asterway.identity.mapper.RolePermissionMapper;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.service.RoleService;
import com.asterway.identity.vo.RoleVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 角色展示与授权集中处理；关联表在同一事务内替换，不创建额外用例层。 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper mapper;

    private final PermissionMapper permissions;

    private final MenuMapper menus;

    private final RolePermissionMapper rolePermissions;

    private final RoleMenuMapper roleMenus;

    private final OperationLogService logs;

    public RoleServiceImpl(
            RoleMapper mapper,
            PermissionMapper permissions,
            MenuMapper menus,
            RolePermissionMapper rolePermissions,
            RoleMenuMapper roleMenus,
            OperationLogService logs) {
        this.mapper = mapper;
        this.permissions = permissions;
        this.menus = menus;
        this.rolePermissions = rolePermissions;
        this.roleMenus = roleMenus;
        this.logs = logs;
    }

    @Override
    public List<RoleVO> list() {
        SecurityUtils.requireSuperAdmin("identity:role:list");
        return mapper.findAll().stream()
                .filter(row -> Boolean.TRUE.equals(row.getBuiltin()))
                .map(this::toVO)
                .toList();
    }

    @Override
    public RoleVO get(long id) {
        SecurityUtils.requireSuperAdmin("identity:role:read");
        return toVO(required(id));
    }

    @Override
    @Transactional
    public RoleVO update(long id, RoleUpdateDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:role:update");
        mapper.lockSecurityGate();
        RoleEntity row = required(id);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        row.setName(dto.name());
        row.setDescription(dto.description() == null ? "" : dto.description());
        row.setUpdatedBy(actor.userId());
        if (mapper.update(row) != 1) {
            throw BusinessException.conflict("角色已被修改，请刷新");
        }
        logs.append(
                "identity:role:update",
                "ROLE",
                Long.toString(id),
                null,
                Map.of("version", dto.version()));
        return toVO(required(id));
    }

    @Override
    public List<String> permissions(long id) {
        SecurityUtils.requireSuperAdmin("identity:role:read");
        required(id);
        return rolePermissions.findIds(id).stream().map(Object::toString).toList();
    }

    @Override
    public List<String> menus(long id) {
        SecurityUtils.requireSuperAdmin("identity:role:read");
        required(id);
        return roleMenus.findIds(id).stream().map(Object::toString).toList();
    }

    @Override
    @Transactional
    public RoleVO grantPermissions(long id, PermissionGrantDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:role:grant_permission");
        mapper.lockSecurityGate();
        RoleEntity role = required(id);
        AccountValidationUtils.version(dto.version(), role.getVersion());
        AccountValidationUtils.distinctIds(dto.permissionIds());
        Map<Long, PermissionEntity> available = new HashMap<>();
        permissions.findAll().forEach(row -> available.put(row.getId(), row));
        for (Long permissionId : dto.permissionIds()) {
            PermissionEntity permission = available.get(permissionId);
            if (permission == null
                    || !Boolean.TRUE.equals(permission.getEnabled())
                    || !allowed(role.getCode(), permission.getCode())) {
                throw BusinessException.badRequest("权限不存在、已停用或超出该角色的管理边界");
            }
        }
        Set<Long> selected = new HashSet<>(dto.permissionIds());
        for (PermissionEntity permission : available.values()) {
            boolean mandatory =
                    Boolean.TRUE.equals(permission.getBuiltin())
                            && ("SUPER_ADMIN".equals(role.getCode())
                                            && !com.asterway.common.utils.GrowthAccessUtils
                                                    .permission(permission.getCode())
                                    || "USER".equals(role.getCode())
                                            && personal(permission.getCode()));
            if (mandatory && !selected.contains(permission.getId())) {
                throw BusinessException.conflict("不能移除该内置角色的核心账户权限");
            }
        }
        advance(role, actor.userId());
        rolePermissions.deleteByOwner(id);
        for (Long permissionId : dto.permissionIds()) {
            rolePermissions.insert(id, permissionId, actor.userId());
        }
        logs.append(
                "identity:role:grant_permission",
                "ROLE",
                Long.toString(id),
                null,
                Map.of("permissionIds", dto.permissionIds()));
        return toVO(required(id));
    }

    @Override
    @Transactional
    public RoleVO grantMenus(long id, MenuGrantDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:role:grant_menu");
        mapper.lockSecurityGate();
        RoleEntity role = required(id);
        AccountValidationUtils.version(dto.version(), role.getVersion());
        AccountValidationUtils.distinctIds(dto.menuIds());
        Map<Long, MenuEntity> available = new HashMap<>();
        menus.findAll().forEach(row -> available.put(row.getId(), row));
        Set<Long> growthPermissionIds = new HashSet<>();
        permissions.findAll().stream()
                .filter(p -> com.asterway.common.utils.GrowthAccessUtils.permission(p.getCode()))
                .forEach(p -> growthPermissionIds.add(p.getId()));
        Set<Long> selected = new LinkedHashSet<>();
        Set<Long> permitted = new HashSet<>(rolePermissions.findIds(id));
        for (Long menuId : dto.menuIds()) {
            MenuEntity menu = available.get(menuId);
            Set<Long> chain = new HashSet<>();
            while (menu != null) {
                if (!"USER".equals(role.getCode())
                        && (com.asterway.common.utils.GrowthAccessUtils.menu(
                                        menu.getCode(), menu.getComponentKey())
                                || growthPermissionIds.contains(menu.getPermissionId()))) {
                    throw BusinessException.badRequest("成长业务菜单只能分配给普通用户角色");
                }
                if (!chain.add(menu.getId()) || !Boolean.TRUE.equals(menu.getEnabled())) {
                    throw BusinessException.badRequest("菜单父链循环或已停用");
                }
                if (menu.getPermissionId() != null
                        && !"SUPER_ADMIN".equals(role.getCode())
                        && !permitted.contains(menu.getPermissionId())) {
                    throw BusinessException.badRequest("请先授予菜单对应的动作权限");
                }
                selected.add(menu.getId());
                if (menu.getParentId() == null) {
                    break;
                }
                menu = available.get(menu.getParentId());
            }
            if (menu == null) {
                throw BusinessException.badRequest("菜单或父节点不存在");
            }
        }
        for (MenuEntity menu : available.values()) {
            boolean mandatory =
                    Boolean.TRUE.equals(menu.getBuiltin())
                            && ("SUPER_ADMIN".equals(role.getCode())
                                            && !com.asterway.common.utils.GrowthAccessUtils.menu(
                                                    menu.getCode(), menu.getComponentKey())
                                            && !growthPermissionIds.contains(menu.getPermissionId())
                                    || "USER".equals(role.getCode())
                                            && Set.of("account_root", "profile", "devices")
                                                    .contains(menu.getCode()));
            if (mandatory && !selected.contains(menu.getId())) {
                throw BusinessException.conflict("不能移除该内置角色的核心菜单");
            }
        }
        advance(role, actor.userId());
        roleMenus.deleteByOwner(id);
        for (Long menuId : selected) {
            roleMenus.insert(id, menuId, actor.userId());
        }
        logs.append(
                "identity:role:grant_menu",
                "ROLE",
                Long.toString(id),
                null,
                Map.of("menuIds", selected));
        return toVO(required(id));
    }

    private boolean personal(String code) {
        return code.startsWith("identity:")
                && Set.of("profile", "password", "timezone", "device").contains(code.split(":")[1]);
    }

    private boolean allowed(String role, String code) {
        if (com.asterway.common.utils.GrowthAccessUtils.permission(code)) {
            return "USER".equals(role)
                    && com.asterway.common.constant.GrowthPermissionConstants.CODES.contains(code);
        }
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        if (!code.startsWith("identity:")) {
            return com.asterway.common.constant.GrowthPermissionConstants.CODES.contains(code);
        }
        if (personal(code)) {
            return true;
        }
        return "ADMIN".equals(role)
                && (code.startsWith("identity:user:") && !code.endsWith(":assign_role")
                        || Set.of("session", "login_log", "operation_log")
                                .contains(code.split(":")[1]));
    }

    private void advance(RoleEntity role, long actorId) {
        if (mapper.advanceVersion(role.getId(), role.getVersion(), actorId) != 1) {
            throw BusinessException.conflict("角色授权已变化，请刷新");
        }
    }

    private RoleEntity required(long id) {
        RoleEntity role = mapper.findById(id);
        if (role == null || !Boolean.TRUE.equals(role.getBuiltin())) {
            throw BusinessException.notFound();
        }
        return role;
    }

    private RoleVO toVO(RoleEntity row) {
        return new RoleVO(
                row.getId().toString(),
                row.getCode(),
                row.getName(),
                row.getDescription(),
                Boolean.TRUE.equals(row.getBuiltin()),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getVersion(),
                rolePermissions.findIds(row.getId()).stream().map(Object::toString).toList(),
                roleMenus.findIds(row.getId()).stream().map(Object::toString).toList());
    }
}
