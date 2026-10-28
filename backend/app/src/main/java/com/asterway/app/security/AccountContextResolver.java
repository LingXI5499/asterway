package com.asterway.app.security;

import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.model.AccountContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 匿名操作使用随机服务端作用域，认证操作使用稳定 UUID，均不接受客户端指定。
@Component
public class AccountContextResolver {

    private static final String ANONYMOUS_SCOPE = AccountContextResolver.class.getName() + ".anonymousScope";

    public AccountContext context(HttpServletRequest request, UUID key, boolean anonymousScope) {
        AccountPrincipal principal = principal();
        String scope;
        if (anonymousScope || principal == null) {
            HttpSession session = request.getSession(true);
            synchronized (session) {
                UUID id = (UUID) session.getAttribute(ANONYMOUS_SCOPE);
                if (id == null) {
                    id = UUID.randomUUID();
                    session.setAttribute(ANONYMOUS_SCOPE, id);
                }
                scope = AccountContext.ANONYMOUS_SCOPE_PREFIX + id;
            }
        } else {
            scope = AccountContext.USER_SCOPE_PREFIX + principal.accountId();
        }
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        AccountContext context = new AccountContext(
                key, scope, principal == null ? null : principal.accountId(), traceId,
                principal == null ? 0 : principal.credentialVersion(),
                principal == null ? 0 : principal.securityVersion()
        );
        return context;
    }

    public UUID key(HttpServletRequest request) {
        String value = request.getHeader("Idempotency-Key");
        if (value == null || !value.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
            Map<String, Object> violation = Map.of("field", "Idempotency-Key", "message", "请提供标准 UUID 操作标识");
            throw new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of("violations", List.of(violation)));
        }
        UUID key = UUID.fromString(value);
        return key;
    }

    public AccountPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            return principal;
        }
        return null;
    }
}
