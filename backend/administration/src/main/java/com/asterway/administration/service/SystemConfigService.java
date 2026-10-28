package com.asterway.administration.service;

import com.asterway.administration.api.SystemConfigApi;
import com.asterway.administration.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;

// 系统配置读取能力，暂不提供管理写入接口。
@Service
public class SystemConfigService implements SystemConfigApi {

    private final SystemConfigMapper mapper;

    public SystemConfigService(SystemConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String siteName() {
        return mapper.siteName();
    }

    @Override
    public boolean registrationOpen() {
        return mapper.registrationOpen();
    }
}
