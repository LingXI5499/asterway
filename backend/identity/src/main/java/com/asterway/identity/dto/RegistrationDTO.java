package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 注册只填写用户名和两次密码；验证码凭据由客户端验证流程提供。 */
public record RegistrationDTO(
        @NotBlank @Size(max = 60) String username,
        @NotNull @Size(max = 100) String password,
        @NotNull @Size(max = 100) String confirmPassword,
        @NotBlank @Size(max = 64) String captchaTicket,
        @NotBlank @Size(min = 16, max = 128) String clientNonce
) {
}
