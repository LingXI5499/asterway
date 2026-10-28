package com.asterway.app.service;

import com.asterway.administration.api.SystemConfigApi;
import com.asterway.identity.api.AccountConfigurationPort;
import org.springframework.stereotype.Service;

// 将管理模块的公开配置读取能力适配给账户业务。
@Service
public class AccountConfigurationService implements AccountConfigurationPort {

    private final SystemConfigApi systemConfig;

    public AccountConfigurationService(SystemConfigApi systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Override
    public String siteName() {
        return systemConfig.siteName();
    }

    @Override
    public boolean registrationOpen() {
        return systemConfig.registrationOpen();
    }
}
