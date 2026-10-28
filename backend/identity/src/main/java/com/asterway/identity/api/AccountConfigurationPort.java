package com.asterway.identity.api;



// 账户业务需要的公开系统配置。
public interface AccountConfigurationPort {

    String siteName();

    boolean registrationOpen();
}
