package com.asterway.app.web.identity;

import com.asterway.app.application.identity.AccountHttpContext;
import com.asterway.app.application.identity.AccountUseCase;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.model.AccountModels.CaptchaChallenge;
import com.asterway.identity.api.model.AccountModels.CaptchaProof;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.CsrfInfo;
import com.asterway.identity.api.model.AccountModels.DeviceTimeZoneResult;
import com.asterway.identity.api.model.AccountModels.LoginInput;
import com.asterway.identity.api.model.AccountModels.LogoutResult;
import com.asterway.identity.api.model.AccountModels.PasswordChangeResult;
import com.asterway.identity.api.model.AccountModels.PasswordInput;
import com.asterway.identity.api.model.AccountModels.ProfileInput;
import com.asterway.identity.api.model.AccountModels.ProfileResult;
import com.asterway.identity.api.model.AccountModels.RegistrationInput;
import com.asterway.identity.api.model.AccountModels.SelfSession;
import com.asterway.identity.api.model.AccountModels.ZoneInput;
import com.asterway.identity.api.model.RegistrationResult;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

// HTTP 层只校验接入形式、构造服务端上下文并转换统一响应。
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户", description = "注册、登录、本人资料、密码、时区及当前会话")
public class AccountController {

    private final AccountApi accounts;
    private final AccountUseCase useCase;
    private final AccountHttpContext contexts;

    public AccountController(AccountApi accounts, AccountUseCase useCase, AccountHttpContext contexts) {
        this.accounts = accounts;
        this.useCase = useCase;
        this.contexts = contexts;
    }

    @Operation(summary = "获取当前会话 CSRF 令牌")
    @GetMapping("/csrf")
    public ApiResponse<CsrfInfo> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfInfo info = useCase.csrf(request, response);
        ApiResponse<CsrfInfo> result = success(info, request);
        return result;
    }

    @Operation(summary = "创建服务端滑动拼图")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/captcha-challenges")
    public ApiResponse<CaptchaChallenge> createCaptcha(@RequestBody ChallengeRequest body, HttpServletRequest request) {
        Context context = contexts.context(request, contexts.key(request), true);
        CaptchaChallenge challenge = accounts.createCaptcha(context, body.purpose());
        ApiResponse<CaptchaChallenge> result = success(challenge, request);
        return result;
    }

    @Operation(summary = "验证拼图水平位移")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/captcha-challenges/{challengeId}/verify")
    public ApiResponse<CaptchaProof> verifyCaptcha(
            @PathVariable UUID challengeId, @RequestBody VerifyRequest body, HttpServletRequest request
    ) {
        if (body.offsetX() == null) {
            throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
        }
        Context context = contexts.context(request, contexts.key(request), true);
        CaptchaProof proof = accounts.verifyCaptcha(context, challengeId, body.offsetX());
        ApiResponse<CaptchaProof> result = success(proof, request);
        return result;
    }

    @Operation(summary = "注册普通账户", description = "注册后跳转登录，不自动建立认证会话。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = @Content(schema = @Schema(implementation = RegistrationInput.class))
    )
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResult>> register(
            @RequestBody JsonNode body, HttpServletRequest request
    ) {
        Context context = contexts.context(request, contexts.key(request), true);
        RegistrationInput input = registrationInput(body);
        RegistrationResult registration = accounts.register(context, input);
        ApiResponse<RegistrationResult> payload = success(registration, request);
        ResponseEntity<ApiResponse<RegistrationResult>> response = ResponseEntity.status(HttpStatus.CREATED).body(payload);
        return response;
    }

    @Operation(summary = "登录", description = "连续5次有效凭据失败锁定5分钟；滑动失败不计入。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/login")
    public ApiResponse<SelfSession> login(
            @RequestBody LoginInput body, HttpServletRequest request, HttpServletResponse response
    ) {
        SelfSession snapshot = useCase.login(body, request, response);
        ApiResponse<SelfSession> result = success(snapshot, request);
        return result;
    }

    @Operation(summary = "查看本人资料与会话快照")
    @GetMapping("/me")
    public ApiResponse<SelfSession> me(HttpServletRequest request, HttpServletResponse response) {
        SelfSession snapshot = useCase.current(request, response);
        ApiResponse<SelfSession> result = success(snapshot, request);
        return result;
    }

    @Operation(summary = "修改本人用户名或昵称", description = "至少提交一项，省略表示保留，显式 null 被拒绝。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true, content = @Content(schema = @Schema(implementation = ProfileInput.class))
    )
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PatchMapping("/profile")
    public ApiResponse<ProfileResult> profile(@RequestBody JsonNode body, HttpServletRequest request) {
        ProfileInput input = profileInput(body);
        Context context = contexts.context(request, contexts.key(request), false);
        ProfileResult profile = accounts.updateProfile(context, input);
        ApiResponse<ProfileResult> result = success(profile, request);
        return result;
    }

    @Operation(summary = "修改本人密码", description = "校验原密码并撤销全部旧会话；须先结束 RUNNING 和 PAUSED 执行。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/password/change")
    public ApiResponse<PasswordChangeResult> password(
            @RequestBody PasswordInput body, HttpServletRequest request, HttpServletResponse response
    ) {
        PasswordChangeResult changed = useCase.changePassword(body, request, response);
        ApiResponse<PasswordChangeResult> result = success(changed, request);
        return result;
    }

    @Operation(summary = "正常退出当前会话", description = "须先结束全部未结束执行；不影响其他设备。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PostMapping("/logout")
    public ApiResponse<LogoutResult> logout(
            @RequestBody ConfirmRequest body, HttpServletRequest request, HttpServletResponse response
    ) {
        LogoutResult loggedOut = useCase.logout(body.confirmed(), request, response);
        ApiResponse<LogoutResult> result = success(loggedOut, request);
        return result;
    }

    @Operation(summary = "同步自动识别的设备时区", description = "当前业务日结束后切换，不立即重排当前日期。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PutMapping("/device-time-zone")
    public ApiResponse<DeviceTimeZoneResult> zone(@RequestBody ZoneInput body, HttpServletRequest request) {
        Context context = contexts.context(request, contexts.key(request), false);
        DeviceTimeZoneResult synchronizedZone = accounts.synchronizeZone(context, body);
        ApiResponse<DeviceTimeZoneResult> result = success(synchronizedZone, request);
        return result;
    }

    private ProfileInput profileInput(JsonNode body) {
        if (!body.isObject()) {
            throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
        }
        Set<String> allowed = Set.of("username", "nickname", "expectedVersion");
        body.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field) || !body.get(field).isTextual()) {
                throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
            }
        });
        String version = body.has("expectedVersion") ? body.get("expectedVersion").asText() : null;
        String username = body.has("username") ? body.get("username").asText() : null;
        String nickname = body.has("nickname") ? body.get("nickname").asText() : null;
        ProfileInput input = new ProfileInput(version, username, nickname);
        return input;
    }

    private RegistrationInput registrationInput(JsonNode body) {
        Set<String> allowed = Set.of("username", "nickname", "password", "confirmPassword", "zoneId",
                "captchaChallengeId", "captchaVerificationToken");
        if (!body.isObject()) {
            throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
        }
        body.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field) || !body.get(field).isTextual()) {
                throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
            }
        });
        UUID challengeId = null;
        if (body.has("captchaChallengeId")) {
            String value = body.get("captchaChallengeId").asText();
            if (!value.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
            }
            challengeId = UUID.fromString(value);
        }
        RegistrationInput input = new RegistrationInput(
                text(body, "username"), text(body, "nickname"), text(body, "password"),
                text(body, "confirmPassword"), text(body, "zoneId"), challengeId,
                text(body, "captchaVerificationToken")
        );
        return input;
    }

    private String text(JsonNode body, String field) {
        String value = body.has(field) ? body.get(field).asText() : null;
        return value;
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        ApiResponse<T> response = ApiResponse.success(data, traceId);
        return response;
    }

    public record ChallengeRequest(String purpose) {
    }

    public record VerifyRequest(Double offsetX) {
    }

    public record ConfirmRequest(Boolean confirmed) {
    }
}
