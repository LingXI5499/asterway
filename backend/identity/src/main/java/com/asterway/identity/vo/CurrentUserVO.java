package com.asterway.identity.vo;

import java.util.List;
import java.util.Set;

/** 应用启动时确认的当前身份和授权菜单。 */
public record CurrentUserVO(
        UserVO user,
        Set<String> roles,
        Set<String> permissions,
        List<MenuVO> menus
) {
}
