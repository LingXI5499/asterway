/*
 * 处理账号Security提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

/* 公共认证契约；实现放在账户 Service，common 不直接引用业务 Mapper。 */
// 定义接口 AccountSecurityProvider；处理账号Security提供器相关的当前身份、权限或安全数据。
public interface AccountSecurityProvider {

    /* 按稳定用户标识读取当前状态和授权；不存在时返回 null。 */
    // 加载Security账号（loadSecurityAccount）方法：加载 Security账号（SecurityAccount）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    SecurityAccountData loadSecurityAccount(long userId);
}
