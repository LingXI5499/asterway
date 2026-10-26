package com.asterway.identity.validation;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 所有字符串按 Unicode 码点校验，不 trim、不截断、不增加密码强度门槛。
@Component
public class AccountValidator {

    public void username(String value) {
        text(value, "username", 1, 30);
        boolean allowed = value.codePoints().allMatch(codePoint ->
                codePoint >= 'A' && codePoint <= 'Z'
                        || codePoint >= 'a' && codePoint <= 'z'
                        || codePoint >= '0' && codePoint <= '9'
                        || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
        if (!allowed) {
            invalid("username", "用户名只能包含英文字母、数字和汉字");
        }
    }

    public void nickname(String value) {
        text(value, "nickname", 1, 30);
    }

    public void password(String value, String field, boolean newPassword) {
        int minimum = newPassword ? 8 : 1;
        text(value, field, minimum, 50);
    }

    public void matchingPasswords(String password, String confirmation) {
        password(confirmation, "confirmPassword", true);
        if (!password.equals(confirmation)) {
            throw new AccountException(IdentityErrorCode.PASSWORD_MISMATCH, Map.of());
        }
    }

    public long version(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) {
            invalid("expectedVersion", "版本必须是非负整数的十进制字符串");
        }
        try {
            long version = Long.parseLong(value);
            return version;
        } catch (NumberFormatException exception) {
            invalid("expectedVersion", "版本超出 bigint 范围");
            throw new IllegalStateException("版本校验未终止", exception);
        }
    }

    public void zone(String value) {
        if (value == null || value.length() > 64 || !ZoneId.getAvailableZoneIds().contains(value)) {
            invalid("zoneId", "请选择有效的 IANA 时区");
        }
    }

    public void captcha(UUID challengeId, String token) {
        if (challengeId == null || token == null || !token.matches("[0-9a-f]{64}")) {
            invalid("captchaVerificationToken", "请先完成滑动验证");
        }
    }

    public void confirmed(Boolean value) {
        if (!Boolean.TRUE.equals(value)) {
            invalid("confirmed", "请确认本次操作");
        }
    }

    public void text(String value, String field, int minimum, int maximum) {
        if (value == null) {
            invalid(field, "此字段不能为空");
        }
        int length = value.codePointCount(0, value.length());
        boolean blank = value.codePoints().allMatch(codePoint ->
                Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint));
        boolean invalidEncoding = value.codePoints().anyMatch(codePoint ->
                codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE);
        if (length < minimum || length > maximum || blank || invalidEncoding) {
            invalid(field, "长度必须为" + minimum + "到" + maximum + "个字符，且不能全为空白");
        }
    }

    public void invalid(String field, String message) {
        Map<String, Object> violation = Map.of("field", field, "message", message);
        Map<String, Object> data = Map.of("violations", List.of(violation));
        throw new AccountException(CommonErrorCode.VALIDATION_ERROR, data);
    }
}
