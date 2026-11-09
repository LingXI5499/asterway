/*
 * 集中处理账号ValidationUtils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;
// 引入摘要值设置（HashSet），下面可以直接使用这个类型或静态成员。
import java.util.HashSet;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;

/* 账户字段规则；Controller 校验格式，Service 再验证业务含义。 */
// 定义类 AccountValidationUtils；集中处理账号ValidationUtils的重复规则，避免各业务模块分别实现。
public final class AccountValidationUtils {

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private AccountValidationUtils() {
    }

    // 用户名（username）方法：调用 username 完成用户名处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public static void username(String value) {
        // 检查value == null || value.codePointCount(0, value.length()) < 1 || value.codePointCount(0, value.length…，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value.codePointCount(0, value.length()) < 1
                // 继续补充同一个判断或备用结果：条件 || value.codePointCount(0, value.length()) > 30 是否成立。
                || value.codePointCount(0, value.length()) > 30
                // c，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                || !value.codePoints().allMatch(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                        // 继续补充同一个判断或备用结果：条件 || c >= '0' && c <= '9' || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) { 是否成立。
                        || c >= '0' && c <= '9' || Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("用户名须为1到30个汉字、英文字母或数字");
        }
    }

    // 昵称（nickname）方法：调用 nickname 完成昵称处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    // 默认值值（defaultValue），使用 String 类型保存；可为 null，表示尚未提供。
    public static String nickname(String value, String defaultValue) {
        // 准备实际（actual），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：value == null ? defaultValue : value。
        String actual = value == null ? defaultValue : value;
        // 检查actual == null || actual.isBlank() || actual.codePointCount(0, actual.length()) > 30，即对应内容是否不存在；成立时执行下面的分支。
        if (actual == null || actual.isBlank() || actual.codePointCount(0, actual.length()) > 30) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("昵称须为1到30个非全空白字符");
        }
        // 把实际（actual）交还给调用方，本方法到此结束。
        return actual;
    }

    // 密码（password）方法：调用 password 完成密码处理；参数负责传入本次处理所需的数据。
    // 密码（password），使用 String 类型保存；可为 null，表示尚未提供。
    // confirmation，使用 String 类型保存；可为 null，表示尚未提供。
    public static void password(String password, String confirmation) {
        // 检查password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8 || passw…，即对应内容是否不存在；成立时执行下面的分支。
        if (password == null || password.isBlank() || password.codePointCount(0, password.length()) < 8
                // 继续补充同一个判断或备用结果：条件 || password.codePointCount(0, password.length()) > 50 || !Objects.equals(password, confirmation)) { 是否成立。
                || password.codePointCount(0, password.length()) > 50 || !Objects.equals(password, confirmation)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("密码须为8到50个字符，且两次输入一致");
        }
    }

    // 用户使用的时区（timeZone）方法：调用 timeZone 完成用户使用的时区处理；参数负责传入本次处理所需的数据。
    // 时区（zone），使用 String 类型保存；可为 null，表示尚未提供。
    public static String timeZone(String zone) {
        // 检查zone == null || !ZoneId.getAvailableZoneIds().contains(zone) || zone.length() > 64，即对应内容是否不存在；成立时执行下面的分支。
        if (zone == null || !ZoneId.getAvailableZoneIds().contains(zone) || zone.length() > 64) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("请选择有效的 IANA 时区");
        }
        // 把时区（zone）交还给调用方，本方法到此结束。
        return zone;
    }

    // 版本号（version）方法：调用 version 完成版本号处理；参数负责传入本次处理所需的数据。
    // 预计（expected），使用 Long 类型保存；可为 null，表示尚未提供。
    // 实际（actual），使用 Long 类型保存；可为 null，表示尚未提供。
    public static void version(Long expected, Long actual) {
        // 检查expected == null || !expected.equals(actual)，即对应内容是否不存在；成立时执行下面的分支。
        if (expected == null || !expected.equals(actual)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("数据已被修改，请刷新后重试");
        }
    }

    // distinct编号列表（distinctIds）方法：调用 distinctIds 完成distinct编号列表处理；参数负责传入本次处理所需的数据。
    // 编号列表（ids），使用 List<Long> 类型保存。
    public static void distinctIds(List<Long> ids) {
        // 编号（id），使用 null 类型保存。
        // 检查ids == null || ids.size() > 200 || ids.stream().anyMatch((id)->id == null || id < 1) || new HashSet<…，即对应内容是否不存在；成立时执行下面的分支。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        if (ids == null || ids.size() > 200 || ids.stream().anyMatch(id -> id == null || id < 1)
                // 继续补充同一个判断或备用结果：条件 || new HashSet<>(ids).size() != ids.size()) { 是否成立。
                || new HashSet<>(ids).size() != ids.size()) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("关联标识无效、重复或超过200项");
        }
    }
}
