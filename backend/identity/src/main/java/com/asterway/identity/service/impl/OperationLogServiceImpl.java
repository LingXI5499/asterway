package com.asterway.identity.service.impl;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.AccountPrincipal;
import com.asterway.common.utils.JsonUtils;
import com.asterway.common.utils.RequestUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.entity.OperationLogEntity;
import com.asterway.identity.mapper.OperationLogMapper;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.vo.OperationLogVO;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** 操作审计只追加；与成功业务写入保持同一事务。 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper mapper;

    private final JsonUtils json;

    @Override
    public void recordGrowthSuccess(String action, long ownerId) {
        String resourceId = null;
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            String path = attributes.getRequest().getServletPath();
            resourceId = path.substring(0, Math.min(100, path.length()));
        }
        append(action, "GROWTH", resourceId, ownerId, Map.of());
    }

    public OperationLogServiceImpl(OperationLogMapper mapper, JsonUtils json) {
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public void append(
            String action,
            String resourceType,
            String resourceId,
            Long targetUserId,
            Map<String, Object> summary) {
        Set<String> allowed =
                Set.of(
                        "username",
                        "nickname",
                        "enabled",
                        "roleIds",
                        "permissionIds",
                        "menuIds",
                        "version",
                        "siteName",
                        "registrationEnabled",
                        "timeZone",
                        "pendingTimeZone",
                        "passwordChanged");
        if (!allowed.containsAll(summary.keySet())) {
            throw new IllegalArgumentException("审计摘要包含未经允许的字段");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal =
                authentication != null
                                && authentication.getPrincipal() instanceof AccountPrincipal value
                        ? value
                        : null;
        OperationLogEntity event = new OperationLogEntity();
        event.setActorId(principal == null ? null : principal.userId());
        event.setActorUsername(principal == null ? null : principal.username());
        event.setTargetUserId(targetUserId);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setActionCode(action);
        event.setOutcome("SUCCESS");
        event.setChangeSummary(json.write(summary));
        event.setIpAddress(RequestUtils.ipAddress());
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        mapper.insert(event);
    }

    @Override
    public PageResultVO<OperationLogVO> page(LogQueryDTO query) {
        var actor = SecurityUtils.requirePermission("identity:operation_log:list");
        int offset = query.offset();
        boolean regularOnly = !actor.superAdmin();
        List<OperationLogVO> rows =
                mapper
                        .findPage(
                                regularOnly,
                                actor.userId(),
                                query.getUserId(),
                                offset,
                                query.getSize())
                        .stream()
                        .map(this::toVO)
                        .toList();
        return new PageResultVO<>(
                rows,
                mapper.count(regularOnly, actor.userId(), query.getUserId()),
                query.getPage(),
                query.getSize());
    }

    private OperationLogVO toVO(OperationLogEntity event) {
        return new OperationLogVO(
                id(event.getId()),
                id(event.getActorId()),
                event.getActorUsername(),
                id(event.getTargetUserId()),
                event.getResourceType(),
                event.getResourceId(),
                event.getActionCode(),
                event.getOutcome(),
                event.getReasonCode(),
                json.object(event.getChangeSummary()),
                event.getIpAddress(),
                event.getTraceId(),
                event.getOccurredAt());
    }

    private String id(Long value) {
        return value == null ? null : value.toString();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(int status, String code) {
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            return;
        }
        var request = attributes.getRequest();
        if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) {
            return;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AccountPrincipal actor)) {
            return;
        }
        String path = request.getServletPath();
        if (path.startsWith("/asterway/executions/") && path.endsWith("/heartbeat")) {
            // 失效计时器可能重复发出旧段心跳，不把高频心跳写成安全操作日志。
            return;
        }
        Long target = path.startsWith("/asterway/me") ? actor.userId() : null;
        var match = Pattern.compile("^/asterway/users/([0-9]+)(?:/|$)").matcher(path);
        if (match.find()) {
            try {
                target = Long.parseLong(match.group(1));
            } catch (NumberFormatException error) {
                target = null;
            }
        }
        OperationLogEntity event = new OperationLogEntity();
        event.setActorId(actor.userId());
        event.setActorUsername(actor.username());
        event.setTargetUserId(target);
        event.setResourceType("REQUEST");
        event.setResourceId(path.substring(0, Math.min(100, path.length())));
        event.setActionCode("HTTP_" + request.getMethod());
        event.setOutcome("FAILURE");
        event.setReasonCode(code);
        event.setChangeSummary(json.write(Map.of("status", status)));
        event.setIpAddress(RequestUtils.ipAddress());
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        mapper.insert(event);
    }
}
