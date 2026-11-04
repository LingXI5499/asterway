package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.security.AccountPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;

/** 当前身份及 Service 边界的权限检查，不从请求体读取操作者。 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AccountPrincipal current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw BusinessException.unauthorized();
        }
        return principal;
    }

    public static AccountPrincipal requirePermission(String permission) {
        AccountPrincipal principal = current();
        if (!principal.superAdmin() && !principal.permissions().contains(permission)) {
            throw BusinessException.forbidden();
        }
        return principal;
    }

    public static AccountPrincipal requireSuperAdmin(String permission) {
        AccountPrincipal principal = requirePermission(permission);
        if (!principal.superAdmin()) {
            throw BusinessException.forbidden();
        }
        return principal;
    }
}
