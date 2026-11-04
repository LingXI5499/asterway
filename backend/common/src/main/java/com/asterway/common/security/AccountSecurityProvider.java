package com.asterway.common.security;

/** 公共认证契约；实现放在账户 Service，common 不直接引用业务 Mapper。 */
public interface AccountSecurityProvider {

    /** 按稳定用户标识读取当前状态和授权；不存在时返回 null。 */
    SecurityAccountData loadSecurityAccount(long userId);
}
