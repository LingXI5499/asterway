package com.asterway.app.web.identity;

import com.asterway.app.service.AccountSessionService;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.LogoutDTO;
import com.asterway.app.web.identity.vo.LogoutVO;
import com.asterway.identity.api.model.LogoutResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户")
public class AccountLogoutController {

    private final AccountSessionService useCase;

    public AccountLogoutController(AccountSessionService useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "正常退出当前会话", description = "须先结束全部未结束执行；不影响其他设备。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/logout")
    public ApiResponse<LogoutVO> logout(
            @RequestBody LogoutDTO body, HttpServletRequest request, HttpServletResponse response
    ) {
        LogoutResult result = useCase.logout(body.confirmed(), request, response);
        return ApiResponse.success(
                LogoutVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
