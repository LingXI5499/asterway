package com.asterway.identity.vo;

import java.util.List;

/** 角色及当前授权标识，用于同一角色的授权回显。 */
public record RoleVO(
        String id,
        String code,
        String name,
        String description,
        boolean builtin,
        boolean enabled,
        long version,
        List<String> permissionIds,
        List<String> menuIds
) {
}
