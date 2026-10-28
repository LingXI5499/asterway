package com.asterway.app.web.identity;

import com.asterway.app.service.AccountSessionService;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.LoginDTO;
import com.asterway.app.web.identity.vo.AccountSessionVO;
import com.asterway.app.web.identity.vo.CsrfVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 认证会话入口；框架令牌和会话建立由应用用例协调。
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户", description = "账户认证与当前会话")
public class AccountController {

    private final AccountSessionService useCase;

    public AccountController(AccountSessionService useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "获取当前会话 CSRF 令牌")
    @GetMapping("/csrf")
    public ApiResponse<CsrfVO> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfVO data = useCase.csrf(request, response);
        return ApiResponse.success(data, (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE));
    }

    @Operation(summary = "登录", description = "连续5次有效凭据失败锁定5分钟；滑动失败不计入。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/login")
    public ApiResponse<AccountSessionVO> login(
            @RequestBody LoginDTO body, HttpServletRequest request, HttpServletResponse response
    ) {
        AccountSessionVO data = useCase.login(body.toInput(), request, response);
        return ApiResponse.success(data, (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE));
    }

    @Operation(summary = "查看本人资料与会话快照")
    @GetMapping("/me")
    public ApiResponse<AccountSessionVO> me(HttpServletRequest request, HttpServletResponse response) {
        AccountSessionVO data = useCase.current(request, response);
        return ApiResponse.success(data, (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE));
    }
}
