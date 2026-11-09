/*
 * 声明Captcha本次请求序号返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

/* 一次性验证码票据，绑定创建时的场景与客户端随机标识。 */
// 定义保存一组固定字段的 record CaptchaTicketVO；声明Captcha本次请求序号返回给页面的字段，不直接把数据库实体全部暴露出去。
public record CaptchaTicketVO(
        // captcha本次请求序号（captchaTicket），使用 String 类型保存；可为 null，表示尚未提供。
        String captchaTicket,
        // 到期In秒数（expiresInSeconds），使用 int 类型保存。
        int expiresInSeconds
) {
}
