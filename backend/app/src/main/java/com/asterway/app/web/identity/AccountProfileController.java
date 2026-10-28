package com.asterway.app.web.identity;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.ProfileUpdateDTO;
import com.asterway.app.web.identity.vo.ProfileUpdateVO;
import com.asterway.identity.api.AccountProfileApi;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.ProfileResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户")
public class AccountProfileController {

    private final AccountProfileApi accounts;
    private final AccountContextResolver contexts;

    public AccountProfileController(AccountProfileApi accounts, AccountContextResolver contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Operation(summary = "修改本人用户名或昵称", description = "至少提交一项，省略表示保留，显式 null 被拒绝。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PatchMapping("/profile")
    public ApiResponse<ProfileUpdateVO> profile(
            @RequestBody ProfileUpdateDTO body, HttpServletRequest request
    ) {
        AccountContext context = contexts.context(request, contexts.key(request), false);
        ProfileResult result = accounts.updateProfile(context, body.toInput());
        return ApiResponse.success(
                ProfileUpdateVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
