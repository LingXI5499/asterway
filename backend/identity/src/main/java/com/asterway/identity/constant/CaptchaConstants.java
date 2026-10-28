package com.asterway.identity.constant;



// 拼图生成和验证共享尺寸、期限及状态，防止调整图片后遗漏位移校验。
public final class CaptchaConstants {

    public static final int IMAGE_WIDTH = 320;
    public static final int IMAGE_HEIGHT = 160;
    public static final int PIECE_SIZE = 40;
    public static final int MAX_OFFSET = IMAGE_WIDTH - PIECE_SIZE;
    public static final int LIFETIME_SECONDS = 120;
    public static final int MINIMUM_ELAPSED_MILLIS = 300;
    public static final int OFFSET_TOLERANCE = 4;
    public static final int CHALLENGES_PER_MINUTE = 20;
    public static final int RETRY_SECONDS = 60;
    public static final String REGISTER_PURPOSE = "REGISTER";
    public static final String LOGIN_PURPOSE = "LOGIN";
    public static final String ISSUED = "ISSUED";
    public static final String VERIFIED = "VERIFIED";
    public static final String FAILED = "FAILED";
    public static final String CONSUMED = "CONSUMED";

    private CaptchaConstants() {
    }
}
