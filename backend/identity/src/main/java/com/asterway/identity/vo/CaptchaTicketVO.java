package com.asterway.identity.vo;

/** 一次性验证码票据，绑定创建时的场景与客户端随机标识。 */
public record CaptchaTicketVO(
        String captchaTicket,
        int expiresInSeconds
) {
}
