package com.asterway.identity.mapper;

import com.asterway.identity.model.CaptchaState;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

// 验证码挑战及其到期清理的数据访问契约。
public interface AccountCaptchaMapper {

    CaptchaState findCaptcha(@Param("id") UUID id);

    CaptchaState findCreatedCaptcha(@Param("binding") byte[] binding, @Param("key") UUID key);

    long countRecentCaptchas(@Param("binding") byte[] binding);

    void insertCaptcha(CaptchaState state);

    void verifyCaptcha(
            @Param("id") UUID id,
            @Param("key") UUID key,
            @Param("fingerprint") byte[] fingerprint,
            @Param("state") String state,
            @Param("material") byte[] material
    );

    void consumeCaptcha(
            @Param("id") UUID id,
            @Param("key") UUID key,
            @Param("fingerprint") byte[] fingerprint
    );

    void cleanExpiredCaptchas();
}
