package com.asterway.app.web.identity;

import com.asterway.app.service.AccountSessionService;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.PasswordChangeDTO;
import com.asterway.app.web.identity.vo.PasswordChangeVO;
import com.asterway.identity.api.model.PasswordChangeResult;
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
public class AccountPasswordController {

    private final AccountSessionService useCase;

    public AccountPasswordController(AccountSessionService useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "修改本人密码", description = "校验原密码并撤销全部旧会话；须先结束 RUNNING 和 PAUSED 执行。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/password/change")
    public ApiResponse<PasswordChangeVO> password(
            @RequestBody PasswordChangeDTO body, HttpServletRequest request, HttpServletResponse response
    ) {
        PasswordChangeResult result = useCase.changePassword(body.toInput(), request, response);
        return ApiResponse.success(
                PasswordChangeVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
