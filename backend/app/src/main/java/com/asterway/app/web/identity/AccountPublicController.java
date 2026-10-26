package com.asterway.app.web.identity;

import com.asterway.app.application.identity.AccountHttpContext;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.OperationSnapshot;
import com.asterway.identity.api.model.AccountModels.PublicBootstrap;
import com.asterway.identity.api.model.AccountModels.TimeZonePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 公开配置无需登录；操作结果查询仍由过滤链核对当前本人身份。
@RestController
@Tag(name = "账户辅助接口")
public class AccountPublicController {

    private final AccountApi accounts;
    private final AccountHttpContext contexts;

    public AccountPublicController(AccountApi accounts, AccountHttpContext contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Operation(summary = "读取站点信息和注册开关")
    @GetMapping("/api/v1/public/bootstrap")
    public ApiResponse<PublicBootstrap> bootstrap(HttpServletRequest request) {
        PublicBootstrap data = accounts.bootstrap();
        ApiResponse<PublicBootstrap> response = ApiResponse.success(data, traceId(request));
        return response;
    }

    @Operation(summary = "分页查询有效 IANA 时区")
    @GetMapping("/api/v1/public/time-zones")
    public ApiResponse<TimeZonePage> timeZones(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor,
            HttpServletRequest request
    ) {
        TimeZonePage data = accounts.timeZones(q, limit, cursor);
        ApiResponse<TimeZonePage> response = ApiResponse.success(data, traceId(request));
        return response;
    }

    @Operation(summary = "核实本人账户操作结果", description = "查询不到不代表未执行，不重发会话或执行原操作。")
    @GetMapping("/api/v1/operations/{idempotencyKey}")
    public ApiResponse<OperationSnapshot> operation(
            @PathVariable UUID idempotencyKey, @RequestParam String operation, HttpServletRequest request
    ) {
        Context context = contexts.context(request, idempotencyKey, false);
        OperationSnapshot data = accounts.operation(context, operation);
        ApiResponse<OperationSnapshot> response = ApiResponse.success(data, traceId(request));
        return response;
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }
}
