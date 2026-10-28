package com.asterway.app.service;

import com.asterway.app.mapper.AccountSessionMapper;
import com.asterway.identity.api.AccountSessionPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 在调用方已有事务中撤销会话，保持属性行先于主体行删除。
@Service
public class AccountSessionRevocationService implements AccountSessionPort {

    private final AccountSessionMapper mapper;

    public AccountSessionRevocationService(AccountSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void revokeCurrent(String sessionId) {
        mapper.deleteCurrentSessionAttributes(sessionId);
        mapper.deleteCurrentSession(sessionId);
    }

    @Override
    public void revokeAll(UUID userId) {
        mapper.deleteAccountSessionAttributes(userId);
        mapper.deleteAccountSessions(userId);
    }
}
