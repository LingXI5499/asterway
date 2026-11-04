package com.asterway.identity.service.impl;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.RequestUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.entity.LoginLogEntity;
import com.asterway.identity.mapper.LoginLogMapper;
import com.asterway.identity.service.LoginLogService;
import com.asterway.identity.vo.LoginLogVO;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 每次登录结果独立记录；不提供修改和删除审计记录的方法。 */
@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper mapper;

    public LoginLogServiceImpl(
            LoginLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(Long userId, String username, boolean success, String reason, String sessionId) {
        LoginLogEntity event = new LoginLogEntity();
        event.setUserId(userId);
        event.setUsernameSnapshot(username);
        event.setOutcome(success ? "SUCCESS" : "FAILURE");
        event.setReasonCode(reason);
        event.setSessionId(sessionId);
        event.setIpAddress(RequestUtils.ipAddress());
        event.setUserAgent(RequestUtils.userAgent());
        String agent = RequestUtils.userAgent();
        event.setDeviceLabel(agent.substring(0, Math.min(128, agent.length())));
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        mapper.insert(event);
    }

    @Override
    public PageResultVO<LoginLogVO> page(LogQueryDTO query) {
        var actor = SecurityUtils.requirePermission("identity:login_log:list");
        int offset = query.offset();
        boolean regularOnly = !actor.superAdmin();
        List<LoginLogVO> rows = mapper.findPage(regularOnly, actor.userId(), query.getUserId(), offset, query.getSize())
                .stream().map(this::toVO).toList();
        return new PageResultVO<>(rows, mapper.count(regularOnly, actor.userId(), query.getUserId()),
                query.getPage(), query.getSize());
    }

    private LoginLogVO toVO(LoginLogEntity event) {
        return new LoginLogVO(event.getId().toString(), event.getUserId() == null ? null : event.getUserId().toString(),
                event.getUsernameSnapshot(), event.getOutcome(), event.getReasonCode(), event.getSessionId(),
                event.getIpAddress(), event.getUserAgent(), event.getDeviceLabel(), event.getTraceId(), event.getOccurredAt());
    }

}
