package com.asterway.app.constant;



// 既有账本的持久化阶段值，改包和拆类时不得改变这些编码。
public final class AccountLedgerConstants {

    public static final String BASELINE = "BASELINE";
    public static final String INITIALIZED = "INITIALIZED";
    public static final String PREPARE = "PREPARE";
    public static final String COMMIT = "COMMIT";
    public static final String ABORT = "ABORT";
    public static final String ROOT_MAC = "ROOT";
    public static final String SIGNATURE_PURPOSE = "ledger";

    private AccountLedgerConstants() {
    }
}
