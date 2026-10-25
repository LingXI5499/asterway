package com.asterway.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

// SPA（单页应用）专用的 CSRF Token 处理。
// Spring Security 默认给浏览器用"掩码 token"，但 SPA 前端是从 Cookie 里读原始 token
// 再放进请求头提交的，所以这里需要做特殊处理：
//   - 响应时把原始 token 写进 Cookie 发给浏览器；
//   - 解析时，如果请求头带了 token，就按"明文"方式解析（不走掩码）。
public class SpaCsrfTokenRequestHandler
        implements CsrfTokenRequestHandler {

    // 明文方式的处理器：请求头里的 token 直接当原始值
    private final CsrfTokenRequestAttributeHandler plain =
            new CsrfTokenRequestAttributeHandler();

    // 掩码（XOR）方式的处理器：Spring Security 5.8+ 默认行为
    private final XorCsrfTokenRequestAttributeHandler xor =
            new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> deferredCsrfToken
    ) {
        // 先用掩码处理器处理
        xor.handle(request, response, deferredCsrfToken);

        // 主动取一次 token，触发生成并把原始值写入 Cookie 发给浏览器
        deferredCsrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        // 从请求头取 CSRF token 值
        String headerValue = request.getHeader(
                csrfToken.getHeaderName()
        );

        // 如果请求头带了值，就按明文解析（SPA 的原始 token）
        if (StringUtils.hasText(headerValue)) {
            return plain.resolveCsrfTokenValue(
                    request,
                    csrfToken
            );
        }

        // 否则回退到掩码解析（兼容表单提交等场景）
        return xor.resolveCsrfTokenValue(
                request,
                csrfToken
        );
    }
}
