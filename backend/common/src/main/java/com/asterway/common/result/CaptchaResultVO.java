package com.asterway.common.result;

/** 拼图展示数据；不返回正确横坐标。pieceTop 用于将透明拼图放在正确高度。 */
public record CaptchaResultVO(
        String challengeId,
        String background,
        String piece,
        int pieceTop,
        int width,
        int height,
        int pieceWidth,
        int expiresInSeconds
) {
}
