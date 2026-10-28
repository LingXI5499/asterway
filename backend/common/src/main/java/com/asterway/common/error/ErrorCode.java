package com.asterway.common.error;



// 错误码契约：所有业务错误码（无论哪个模块）都必须实现这个接口。
// code() 返回机器可读的稳定错误码字符串（例如 "USERNAME_TAKEN"），供前端判断；
// message() 返回给用户看的错误提示文案。
// 设计成接口是为了让 common 只定义"形状"，各业务模块再用自己的枚举实现具体错误码。
public interface ErrorCode {

    // 机器可读的错误码
    String code();

    // 面向用户的错误提示文案
    String message();
}
