package com.asterway.identity.constant;



// 登录失败累计与锁定规则，重试提示由同一锁定时长推导。
public final class LoginConstants {

    public static final int MAX_FAILURES = 5;
    public static final int LOCK_MINUTES = 5;
    public static final int LOCK_SECONDS = LOCK_MINUTES * 60;

    private LoginConstants() {
    }
}
