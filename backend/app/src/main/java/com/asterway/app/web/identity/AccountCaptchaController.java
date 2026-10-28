package com.asterway.app.web.identity;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.CaptchaChallengeDTO;
import com.asterway.app.web.identity.dto.CaptchaVerificationDTO;
import com.asterway.app.web.identity.vo.CaptchaChallengeVO;
import com.asterway.app.web.identity.vo.CaptchaProofVO;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.AccountCaptchaApi;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.CaptchaChallenge;
import com.asterway.identity.api.model.CaptchaProof;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户")
public class AccountCaptchaController {

    private final AccountCaptchaApi captchas;
    private final AccountContextResolver contexts;

    public AccountCaptchaController(AccountCaptchaApi captchas, AccountContextResolver contexts) {
        this.captchas = captchas;
        this.contexts = contexts;
    }

    @Operation(summary = "创建服务端滑动拼图")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/captcha-challenges")
    public ApiResponse<CaptchaChallengeVO> createCaptcha(
            @RequestBody CaptchaChallengeDTO body, HttpServletRequest request
    ) {
        AccountContext context = contexts.context(request, contexts.key(request), true);
        CaptchaChallenge result = captchas.create(context, body.purpose());
        return ApiResponse.success(
                CaptchaChallengeVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }

    @Operation(summary = "验证拼图水平位移")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/captcha-challenges/{challengeId}/verify")
    public ApiResponse<CaptchaProofVO> verifyCaptcha(
            @PathVariable UUID challengeId, @RequestBody CaptchaVerificationDTO body, HttpServletRequest request
    ) {
        if (body.offsetX() == null) {
            throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
        }
        AccountContext context = contexts.context(request, contexts.key(request), true);
        CaptchaProof result = captchas.verify(context, challengeId, body.offsetX());
        return ApiResponse.success(
                CaptchaProofVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
