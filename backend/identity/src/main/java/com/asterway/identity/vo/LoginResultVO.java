/*
 * 声明登录结果返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 登录成功结果，令牌只能保存在客户端登录状态中，不写日志。 */
// 定义保存一组固定字段的 record LoginResultVO；声明登录结果返回给页面的字段，不直接把数据库实体全部暴露出去。
public record LoginResultVO(
        // 身份凭据（token），使用 String 类型保存；可为 null，表示尚未提供。
        String token,
        // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
        String sessionId,
        // 到期At（expiresAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant expiresAt,
        // 用户（user），使用 UserVO 类型保存。
        UserVO user
) {
}
