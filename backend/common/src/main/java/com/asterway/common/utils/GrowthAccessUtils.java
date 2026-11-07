package com.asterway.common.utils;

import java.util.Set;

/** 成长业务只属于普通用户，管理身份优先于账户保留的 USER 基础角色。 */
public final class GrowthAccessUtils {
    private GrowthAccessUtils() {}

    public static boolean manager(Set<String> roles) {
        return roles.contains("ADMIN") || roles.contains("SUPER_ADMIN");
    }

    public static boolean permission(String code) {
        return code != null
                && (code.startsWith("goal:")
                        || code.startsWith("stage:")
                        || code.startsWith("task:")
                        || code.startsWith("planning:")
                        || code.startsWith("execution:")
                        || code.startsWith("habit:")
                        || code.startsWith("notification:")
                        || code.startsWith("dashboard:")
                        || code.startsWith("statistics:")
                        || code.startsWith("agent:"));
    }

    public static boolean menu(String code, String component) {
        return code != null && code.startsWith("growth_")
                || component != null
                        && (component.startsWith("growth/")
                                || component.startsWith("planning/")
                                || component.startsWith("execution/")
                                || component.startsWith("habit/")
                                || component.startsWith("notification/")
                                || component.startsWith("dashboard/")
                                || component.startsWith("statistics/")
                                || component.startsWith("agent/"));
    }
}
