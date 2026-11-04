package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** 账户字段规则；Controller 校验格式，Service 再验证业务含义。 */
public final class AccountValidationUtils {

    private AccountValidationUtils() {
    }

    public static void username(String value) {
        if (value == null || value.codePointCount(0, value.length()) < 1
                || value.codePointCount(0, value.length()) > 30
                || !value.codePoints().allMatch(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                        || c >= '0' && c <= '9' || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) {
            throw BusinessException.badRequest("用户名须为1到30个汉字、英文字母或数字");
        }
    }

    public static String nickname(String value, String defaultValue) {
        String actual = value == null ? defaultValue : value;
        if (actual == null || actual.isBlank() || actual.codePointCount(0, actual.length()) > 30) {
            throw BusinessException.badRequest("昵称须为1到30个非全空白字符");
        }
        return actual;
    }

    public static void password(String password, String confirmation) {
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
                || password.codePointCount(0, password.length()) > 50 || !Objects.equals(password, confirmation)) {
            throw BusinessException.badRequest("密码须为8到50个字符，且两次输入一致");
        }
    }

    public static String timeZone(String zone) {
        if (zone == null || !ZoneId.getAvailableZoneIds().contains(zone) || zone.length() > 64) {
            throw BusinessException.badRequest("请选择有效的 IANA 时区");
        }
        return zone;
    }

    public static void version(Long expected, Long actual) {
        if (expected == null || !expected.equals(actual)) {
            throw BusinessException.conflict("数据已被修改，请刷新后重试");
        }
    }

    public static void distinctIds(List<Long> ids) {
        if (ids == null || ids.size() > 200 || ids.stream().anyMatch(id -> id == null || id < 1)
                || new HashSet<>(ids).size() != ids.size()) {
            throw BusinessException.badRequest("关联标识无效、重复或超过200项");
        }
    }
}
