package com.asterway.app.web.identity.vo;



// 令牌只在 app 的会话接入层构造，不传入业务模块。
public record CsrfVO(String headerName, String token) {

    @Override
    public String toString() {
        return "CsrfVO[protected]";
    }
}
