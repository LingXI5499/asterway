package com.asterway.app.web.identity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 前端"登录"请求体 DTO。
// 密码这里只做非空和最大长度校验，不做最小长度限制，
// 因为登录时不能因"密码太短"而拒绝，也不能对密码做 trim 等改动。
public record LoginRequest(

        // 用户名：非空，只能中文/英文/数字，长度 1~30
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9\\p{IsHan}]{1,30}$",
                message = "用户名格式不正确"
        )
        String username,

        // 密码：非空，最长 50 字符
        @NotBlank(message = "密码不能为空")
        @Size(max = 50, message = "密码长度不能超过50个字符")
        String password

) {
}
