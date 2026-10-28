package com.asterway.identity.api;

import java.util.UUID;

// 撤销账户会话，框架存储细节由应用层负责。
public interface AccountSessionPort {

    void revokeCurrent(String sessionId);

    void revokeAll(UUID userId);
}
