package com.asterway.identity.vo;

/** 公开配置只包含站点名称和注册开关。 */
public record PublicSettingVO(
        String siteName,
        boolean registrationEnabled
) {
}
