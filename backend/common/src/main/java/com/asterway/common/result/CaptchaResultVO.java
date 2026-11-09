/*
 * 提供Captcha结果相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.result;

/* 拼图展示数据；不返回正确横坐标。pieceTop 用于将透明拼图放在正确高度。 */
// 定义保存一组固定字段的 record CaptchaResultVO；提供Captcha结果相关的共享实现或应用配置。
public record CaptchaResultVO(
        // challenge编号（challengeId），使用 String 类型保存；可为 null，表示尚未提供。
        String challengeId,
        // 后台（background），使用 String 类型保存；可为 null，表示尚未提供。
        String background,
        // piece，使用 String 类型保存；可为 null，表示尚未提供。
        String piece,
        // pieceTop，使用 int 类型保存。
        int pieceTop,
        // 宽度（width），使用 int 类型保存。
        int width,
        // height，使用 int 类型保存。
        int height,
        // piece宽度（pieceWidth），使用 int 类型保存。
        int pieceWidth,
        // 到期In秒数（expiresInSeconds），使用 int 类型保存。
        int expiresInSeconds
) {
}
