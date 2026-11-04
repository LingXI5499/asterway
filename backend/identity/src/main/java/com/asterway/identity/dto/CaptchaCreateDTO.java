package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 验证码创建请求，浏览器需生成至少16字符的随机绑定标识。 */
public record CaptchaCreateDTO(
        @NotBlank @Pattern(regexp = "LOGIN|REGISTER") String scene,
        @NotBlank @Size(min = 16, max = 128) String clientNonce
) {
}
