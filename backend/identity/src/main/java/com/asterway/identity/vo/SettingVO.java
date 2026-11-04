package com.asterway.identity.vo;

/** 两项固定账户配置及其更新版本。 */
public record SettingVO(
        String siteName,
        boolean registrationEnabled,
        long siteNameVersion,
        long registrationVersion
) {
}
