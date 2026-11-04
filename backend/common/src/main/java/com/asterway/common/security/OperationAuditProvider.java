package com.asterway.common.security;

/** 业务失败审计的中立接入点；全局异常处理不引用具体账户 Mapper。 */
public interface OperationAuditProvider {

    /** 记录已认证写请求的拒绝结果，不读取请求体或凭据。 */
    void recordFailure(int status, String code);
}
