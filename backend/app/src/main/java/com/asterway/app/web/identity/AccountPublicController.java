package com.asterway.app.web.identity;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.TimeZoneQueryDTO;
import com.asterway.app.web.identity.vo.PublicBootstrapVO;
import com.asterway.app.web.identity.vo.TimeZonePageVO;
import com.asterway.identity.api.AccountPublicApi;
import com.asterway.identity.api.model.PublicBootstrap;
import com.asterway.identity.api.model.TimeZonePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

// 公开站点配置与时区字典不需要登录。
@RestController
@Tag(name = "账户辅助接口")
public class AccountPublicController {

    private final AccountPublicApi accounts;

    public AccountPublicController(AccountPublicApi accounts) {
        this.accounts = accounts;
    }

    @Operation(summary = "读取站点信息和注册开关")
    @GetMapping("/api/v1/public/bootstrap")
    public ApiResponse<PublicBootstrapVO> bootstrap(HttpServletRequest request) {
        PublicBootstrap result = accounts.bootstrap();
        return ApiResponse.success(
                PublicBootstrapVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }

    @Operation(summary = "分页查询有效 IANA 时区")
    @GetMapping("/api/v1/public/time-zones")
    public ApiResponse<TimeZonePageVO> timeZones(
            @ParameterObject @ModelAttribute TimeZoneQueryDTO query, HttpServletRequest request
    ) {
        TimeZonePage result = accounts.timeZones(query.q(), query.effectiveLimit(), query.cursor());
        return ApiResponse.success(
                TimeZonePageVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
