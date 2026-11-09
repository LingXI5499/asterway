/*
 * 声明PublicSetting返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

/* 公开配置只包含站点名称和注册开关。 */
// 定义保存一组固定字段的 record PublicSettingVO；声明PublicSetting返回给页面的字段，不直接把数据库实体全部暴露出去。
public record PublicSettingVO(
        // site名称（siteName），使用 String 类型保存；可为 null，表示尚未提供。
        String siteName,
        // registration是否启用（registrationEnabled），使用 boolean 类型保存。
        boolean registrationEnabled
) {
}
