package com.asterway.app.web.identity;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.RegisterDTO;
import com.asterway.app.web.identity.vo.RegistrationVO;
import com.asterway.identity.api.AccountRegistrationApi;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.RegistrationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户")
public class AccountRegistrationController {

    private final AccountRegistrationApi accounts;
    private final AccountContextResolver contexts;

    public AccountRegistrationController(AccountRegistrationApi accounts, AccountContextResolver contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Operation(summary = "注册普通账户", description = "注册后跳转登录，不自动建立认证会话。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationVO>> register(
            @RequestBody RegisterDTO body, HttpServletRequest request
    ) {
        AccountContext context = contexts.context(request, contexts.key(request), true);
        RegistrationResult result = accounts.register(context, body.toInput());
        ApiResponse<RegistrationVO> payload = ApiResponse.success(
                RegistrationVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }
}
