package com.asterway.identity.mapper;

import com.asterway.identity.model.LoginAttempt;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

// 登录失败计数与到期清理的数据访问契约。
public interface AccountLoginAttemptMapper {

    LoginAttempt loginAttempt(@Param("usernameKey") String usernameKey);

    void saveLoginAttempt(
            @Param("usernameKey") String usernameKey,
            @Param("failures") int failures,
            @Param("lockedUntil") OffsetDateTime lockedUntil
    );

    void cleanExpiredAttempts();
}
