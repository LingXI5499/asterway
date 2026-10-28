package com.asterway.app.constant;



// 账户入口保护策略，窗口、重试提示和并发上限在同一作用域维护。
public final class AccountThrottleConstants {

    public static final int MAX_CONCURRENT_REQUESTS = 8;
    public static final long WINDOW_MILLIS = 60_000L;
    public static final int MAX_TRACKED_ADDRESSES = 2048;
    public static final int MAX_REQUESTS_PER_WINDOW = 120;
    public static final int RETRY_AFTER_SECONDS = 60;

    private AccountThrottleConstants() {
    }
}
