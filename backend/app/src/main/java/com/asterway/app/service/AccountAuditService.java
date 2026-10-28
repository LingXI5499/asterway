package com.asterway.app.service;

import com.asterway.app.mapper.AccountAuditMapper;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.model.AccountContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

// 在调用方已有事务中保存账户业务审计事实。
@Service
public class AccountAuditService implements AccountAuditPort {

    private final AccountAuditMapper mapper;

    public AccountAuditService(AccountAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(AccountContext context, UUID targetId, String action, String outcome) {
        mapper.audit(context, targetId, action, outcome);
    }
}
