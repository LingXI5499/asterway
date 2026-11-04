package com.asterway.identity.vo;

import java.util.List;

/** 目录、页面和按钮树节点，组件键由前端白名单解释。 */
public record MenuVO(
        String id,
        String code,
        String parentId,
        String name,
        String menuType,
        String routePath,
        String componentKey,
        String permissionId,
        String icon,
        int sortOrder,
        boolean visible,
        boolean enabled,
        boolean builtin,
        long version,
        List<MenuVO> children
) {
}
