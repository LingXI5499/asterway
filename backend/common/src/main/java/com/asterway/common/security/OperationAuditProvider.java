package com.asterway.common.security;

/** 业务失败审计的中立接入点；全局异常处理不引用具体账户 Mapper。 */
public interface OperationAuditProvider {

    /** 记录已认证写请求的拒绝结果，不读取请求体或凭据。 */
    void recordFailure(int status, String code);

    /** 在成长业务的成功事务中追加审计，不接收用户资料、密码或令牌。 */
    void recordGrowthSuccess(String action, long ownerId);
}
