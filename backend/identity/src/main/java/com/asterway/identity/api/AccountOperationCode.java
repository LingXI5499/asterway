package com.asterway.identity.api;



// 操作标识属于公开契约，固定存储值保持兼容；仅部分操作允许登录后查询结果。
public enum AccountOperationCode {

    REGISTER("API-AUTH-004", false),
    LOGIN("API-AUTH-005", false),
    UPDATE_PROFILE("API-AUTH-007", true),
    CHANGE_PASSWORD("API-AUTH-008", true),
    LOGOUT("API-AUTH-009", true),
    SYNCHRONIZE_TIME_ZONE("API-AUTH-010", true);

    private final String code;
    private final boolean queryable;

    AccountOperationCode(String code, boolean queryable) {
        this.code = code;
        this.queryable = queryable;
    }

    public String code() {
        return code;
    }

    public static boolean supportsResultQuery(String code) {
        for (AccountOperationCode operation : values()) {
            if (operation.queryable && operation.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
