package com.asterway.app.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.UUID;

// 框架会话撤销的数据访问契约。
public interface AccountSessionMapper {

    void deleteCurrentSessionAttributes(@Param("sessionId") String sessionId);

    void deleteCurrentSession(@Param("sessionId") String sessionId);

    void deleteAccountSessionAttributes(@Param("userId") UUID userId);

    void deleteAccountSessions(@Param("userId") UUID userId);
}
