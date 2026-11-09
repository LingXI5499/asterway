/*
 * 处理操作Audit提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

/* 业务失败审计的中立接入点；全局异常处理不引用具体账户 Mapper。 */
// 定义接口 OperationAuditProvider；处理操作Audit提供器相关的当前身份、权限或安全数据。
public interface OperationAuditProvider {

    /* 记录已认证写请求的拒绝结果，不读取请求体或凭据。 */
    // 记录失败（recordFailure）方法：调用 recordFailure 完成记录失败处理；参数负责传入本次处理所需的数据。
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    void recordFailure(int status, String code);

    /* 在成长业务的成功事务中追加审计，不接收用户资料、密码或令牌。 */
    // 记录成长业务成功（recordGrowthSuccess）方法：调用 recordGrowthSuccess 完成记录成长业务成功处理；参数负责传入本次处理所需的数据。
    // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
    // 所属人编号（ownerId），使用 long 类型保存。
    void recordGrowthSuccess(String action, long ownerId);
}
