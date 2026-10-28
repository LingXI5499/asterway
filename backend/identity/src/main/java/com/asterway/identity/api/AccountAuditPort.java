package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;

import java.util.UUID;

// 记录账户业务给出的安全审计事实。
public interface AccountAuditPort {

    void record(AccountContext context, UUID targetId, String action, String outcome);
}
