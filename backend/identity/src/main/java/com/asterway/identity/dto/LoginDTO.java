package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 凭据登录输入，验证码票据必须绑定本次浏览器随机标识。 */
public record LoginDTO(
        @NotBlank @Size(max = 60) String username,
        @NotNull @Size(max = 100) String password,
        @NotBlank @Size(max = 64) String captchaTicket,
        @NotBlank @Size(min = 16, max = 128) String clientNonce
) {
}
