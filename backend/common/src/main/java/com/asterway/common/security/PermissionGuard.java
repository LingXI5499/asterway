package com.asterway.common.security;

import com.asterway.common.utils.SecurityUtils;

import org.springframework.stereotype.Component;

/** 方法权限表达式入口；对象级管理范围继续由 Service 校验。 */
@Component("access")
public class PermissionGuard {

    public boolean hasPermission(String permission) {
        AccountPrincipal principal = SecurityUtils.current();
        return !(com.asterway.common.utils.GrowthAccessUtils.permission(permission)
                        && (com.asterway.common.utils.GrowthAccessUtils.manager(principal.roles())
                                || !principal.roles().contains("USER")))
                && (principal.superAdmin() || principal.permissions().contains(permission));
    }

    public boolean isSuperAdmin() {
        return SecurityUtils.current().superAdmin();
    }
}
