package com.asterway.identity.api;

import com.asterway.identity.api.model.PublicBootstrap;
import com.asterway.identity.api.model.TimeZonePage;

// 账户页面初始化信息及合法时区查询能力。
public interface AccountPublicApi {

    PublicBootstrap bootstrap();

    TimeZonePage timeZones(String query, int limit, String cursor);
}
