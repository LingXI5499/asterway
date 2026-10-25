package com.asterway.app.web.identity;

import com.asterway.app.application.identity.AccountUseCase;
import com.asterway.app.security.AccountPrincipal;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.request.LoginRequest;
import com.asterway.app.web.identity.request.RegisterRequest;
import com.asterway.app.web.identity.response.LoginResponse;
import com.asterway.app.web.identity.response.RegistrationResponse;

import com.asterway.identity.api.model.LoginCommand;
import com.asterway.identity.api.model.RegisterCommand;
import com.asterway.identity.api.model.RegistrationResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 账户认证相关的 HTTP 入口控制器。
// 统一路径前缀 /api/v1/auth，对外暴露五个端点：
//   GET  /csrf      —— 初始化 CSRF Token
//   POST /register  —— 注册
//   POST /login     —— 登录并建立服务端会话
//   GET  /me        —— 查询当前登录状态
//   POST /logout    —— 注销当前会话
// 职责单一：解析参数、调用用例层、组装统一响应，业务逻辑都在 AccountUseCase 里。
@RestController
@RequestMapping("/api/v1/auth")
public class AccountController {

    // 账户用例（业务编排层）
    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    // 初始化 CSRF Token：触发 token 生成，Cookie 由安全过滤链自动写入响应
    @GetMapping("/csrf")
    public ApiResponse<Void> csrf(
            CsrfToken csrfToken,
            HttpServletRequest request
    ) {
        // 主动取一次 token，触发生成并随响应下发 Cookie
        csrfToken.getToken();

        return ApiResponse.successWithTraceId(
                traceId(request)
        );
    }

    // 注册：把请求 DTO 转成业务命令，调用用例层，返回 201 Created
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegisterRequest requestBody,
            HttpServletRequest request
    ) {
        // DTO -> 业务命令对象
        RegisterCommand command = new RegisterCommand(
                requestBody.username(),
                requestBody.nickname(),
                requestBody.password(),
                requestBody.confirmPassword()
        );

        RegistrationResult result =
                accountUseCase.register(command);

        // 业务结果 -> 响应 VO
        RegistrationResponse response = new RegistrationResponse(
                result.accountId(),
                result.username(),
                result.loginRequired(),
                result.createdAt()
        );

        // 注册成功返回 201
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        response,
                        traceId(request)
                ));
    }

    // 登录：校验凭据并建立服务端会话
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        LoginCommand command = new LoginCommand(
                requestBody.username(),
                requestBody.password()
        );

        // 用例层会完成认证并建立会话
        LoginResponse loginResponse = accountUseCase.login(
                command,
                request,
                response
        );

        return ApiResponse.success(
                loginResponse,
                traceId(request)
        );
    }

    // 查询当前登录账户：从 Spring Security 注入的 principal 里取信息。
    // 如果未登录，会被安全过滤链拦截，根本走不到这里。
    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(
            @AuthenticationPrincipal AccountPrincipal principal,
            HttpServletRequest request
    ) {
        LoginResponse response = new LoginResponse(
                principal.accountId(),
                principal.username(),
                principal.nickname(),
                principal.role()
        );

        return ApiResponse.success(
                response,
                traceId(request)
        );
    }

    // 注销当前会话
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        accountUseCase.logout(request, response);

        return ApiResponse.successWithTraceId(
                traceId(request)
        );
    }

    // 从请求属性里取出 TraceIdFilter 写入的追踪 ID，用于问题排查
    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );
    }
}
