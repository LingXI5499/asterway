package com.asterway.identity.vo;

/** 动作权限安全展示对象。 */
public record PermissionVO(
        String id,
        String code,
        String name,
        String description,
        boolean builtin,
        boolean enabled,
        long version
) {
}
