package com.asterway.app.web.identity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 前端"注册"请求体 DTO。
// 通过 @Valid + 各校验注解做服务端参数校验（前端校验只是改善体验，不能代替这里的校验）。
// 注意：不接受 role/status/id/passwordHash，这些由服务端决定。
public record RegisterRequest(

        // 用户名：非空，只能中文/英文/数字，长度 1~30
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9\\p{IsHan}]{1,30}$",
                message = "用户名只能包含中文、英文字母和数字，长度为1到30个字符"
        )
        String username,

        // 昵称：最多 30 字符，不能全为空白
        @Size(max = 30, message = "昵称不能超过30个字符")
        @Pattern(
                regexp = ".*\\S.*",
                message = "昵称不能全为空白"
        )
        String nickname,

        // 密码：非空，长度 8~50
        @NotBlank(message = "密码不能为空")
        @Size(
                min = 8,
                max = 50,
                message = "密码长度必须在8到50个字符之间"
        )
        String password,

        // 确认密码：非空，长度 8~50
        @NotBlank(message = "确认密码不能为空")
        @Size(
                min = 8,
                max = 50,
                message = "确认密码长度必须在8到50个字符之间"
        )
        String confirmPassword

) {
}
