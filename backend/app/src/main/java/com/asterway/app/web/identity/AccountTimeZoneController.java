package com.asterway.app.web.identity;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.DeviceTimeZoneDTO;
import com.asterway.app.web.identity.vo.DeviceTimeZoneVO;
import com.asterway.identity.api.AccountTimeZoneApi;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.DeviceTimeZoneResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账户")
public class AccountTimeZoneController {

    private final AccountTimeZoneApi accounts;
    private final AccountContextResolver contexts;

    public AccountTimeZoneController(AccountTimeZoneApi accounts, AccountContextResolver contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Operation(summary = "同步自动识别的设备时区", description = "当前业务日结束后切换，不立即重排当前日期。")
    @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
    @PutMapping("/device-time-zone")
    public ApiResponse<DeviceTimeZoneVO> zone(
            @RequestBody DeviceTimeZoneDTO body, HttpServletRequest request
    ) {
        AccountContext context = contexts.context(request, contexts.key(request), false);
        DeviceTimeZoneResult result = accounts.synchronize(context, body.toInput());
        return ApiResponse.success(
                DeviceTimeZoneVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
