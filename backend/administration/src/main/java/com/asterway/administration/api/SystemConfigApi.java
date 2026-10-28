package com.asterway.administration.api;



// 提供当前业务允许公开读取的两项系统配置。
public interface SystemConfigApi {

    String siteName();

    boolean registrationOpen();
}
