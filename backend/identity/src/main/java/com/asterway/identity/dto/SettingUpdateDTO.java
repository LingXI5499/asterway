package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 同时更新两项账户配置；每项配置独立比较版本，在同一事务保存。 */
public record SettingUpdateDTO(
        @NotBlank @Size(max = 30) String siteName,
        @NotNull Boolean registrationEnabled,
        @NotNull @PositiveOrZero Long siteNameVersion,
        @NotNull @PositiveOrZero Long registrationVersion
) {
}
