package com.asterway.identity.service;

import com.asterway.identity.api.AccountSecurityQueryApi;
import com.asterway.identity.api.model.AccountSecurityState;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.AccountState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// 在账户数据所属模块读取安全事实；不查询护栏、账本或框架会话表。
@Service
public class AccountSecurityQueryService implements AccountSecurityQueryApi {

    private final AccountMapper mapper;

    public AccountSecurityQueryService(AccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AccountSecurityState find(UUID userId) {
        AccountState account = mapper.findState(userId);
        AccountSecurityState state = account == null ? null : account.securityState();
        return state;
    }

    @Override
    public List<AccountSecurityState> list() {
        List<AccountSecurityState> states = mapper.listSecurityStates().stream()
                .map(AccountState::securityState)
                .toList();
        return states;
    }
}
