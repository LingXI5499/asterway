/*
 * 声明用户统计返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

/* 按操作者可见范围统计账户，禁用不等于删除。 */
// 定义保存一组固定字段的 record UserStatisticsVO；声明用户统计返回给页面的字段，不直接把数据库实体全部暴露出去。
public record UserStatisticsVO(
        // 用户列表（users），使用 long 类型保存。
        long users,
        // administrators，使用 long 类型保存。
        long administrators,
        // 已禁用（disabled），使用 long 类型保存。
        long disabled,
        // 删除标记（deleted），使用 long 类型保存。
        long deleted
) {
}
