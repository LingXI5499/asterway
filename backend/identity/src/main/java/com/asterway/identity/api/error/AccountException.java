package com.asterway.identity.api.error;

import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;

import java.util.Map;

// 仅携带允许向页面披露的附加信息，不接受密码、凭据或完整请求体。
public class AccountException extends BusinessException {

    private final Map<String, Object> data;

    public AccountException(ErrorCode errorCode, Map<String, Object> data) {
        super(errorCode);
        this.data = Map.copyOf(data);
    }

    public Map<String, Object> getData() {
        return data;
    }
}
