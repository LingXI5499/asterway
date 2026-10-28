package com.asterway.identity.api;

import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.DeviceTimeZoneResult;
import com.asterway.identity.api.model.ZoneInput;

// 设备时区同步及到期处理能力。
public interface AccountTimeZoneApi {

    DeviceTimeZoneResult synchronize(AccountContext context, ZoneInput input);

    void applyPending();
}
