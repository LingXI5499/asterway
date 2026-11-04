package com.asterway.identity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 拼图校验请求，offset 是透明拼图左上角的横坐标。 */
public record CaptchaVerifyDTO(
        @NotBlank @Size(max = 64) String challengeId,
        @NotBlank @Size(min = 16, max = 128) String clientNonce,
        @NotNull @Min(0) @Max(256) Integer offset
) {
}
