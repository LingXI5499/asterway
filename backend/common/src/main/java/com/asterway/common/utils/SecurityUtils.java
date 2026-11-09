/*
 * 集中处理SecurityUtils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入账号当前登录身份（AccountPrincipal），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountPrincipal;

// 引入Security上下文Holder（SecurityContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.core.context.SecurityContextHolder;

/* 当前身份及 Service 边界的权限检查，不从请求体读取操作者。 */
// 定义类 SecurityUtils；集中处理SecurityUtils的重复规则，避免各业务模块分别实现。
public final class SecurityUtils {

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private SecurityUtils() {}

    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    public static AccountPrincipal current() {
        // 准备身份认证（authentication），类型为 null；初始内容是读取 上下文（Context） 的值。
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)，即对应内容是否不存在；成立时执行下面的分支。
        if (authentication == null
                // 当前登录身份（principal），使用 AccountPrincipal 类型保存。
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把当前登录身份（principal）交还给调用方，本方法到此结束。
        return principal;
    }

    // require权限（requirePermission）方法：要求满足 权限（Permission）；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    public static AccountPrincipal requirePermission(String permission) {
        // 准备当前登录身份（principal），类型为 AccountPrincipal；初始内容是调用 current 完成当前处理。
        AccountPrincipal principal = current();
        // 检查当前用户是否具有需要的权限：GrowthAccessUtils.permission(permission) && (GrowthAccessUtils.manager(principal.roles()) || !principal.roles().contains("USER")) || !principal.s…；成立时执行下面的分支。
        if (GrowthAccessUtils.permission(permission)
                        // 继续补充同一个判断或备用结果：条件 && (GrowthAccessUtils.manager(principal.roles()) 是否成立。
                        && (GrowthAccessUtils.manager(principal.roles())
                                // 继续补充同一个判断或备用结果：条件 || !principal.roles().contains("USER")) 是否成立。
                                || !principal.roles().contains("USER"))
                // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| !principal.superAdmin() && !principal.permissions().contains(permission)) {。
                || !principal.superAdmin() && !principal.permissions().contains(permission)) {
            // 中止正常处理并抛出异常：调用 BusinessException.forbidden 完成forbidden处理；由上层按失败处理。
            throw BusinessException.forbidden();
        }
        // 把当前登录身份（principal）交还给调用方，本方法到此结束。
        return principal;
    }

    // requireSuperAdmin方法：要求满足 SuperAdmin；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    public static AccountPrincipal requireSuperAdmin(String permission) {
        // 准备当前登录身份（principal），类型为 AccountPrincipal；初始内容是要求满足 权限（Permission）。
        AccountPrincipal principal = requirePermission(permission);
        // 检查条件 !principal.superAdmin() 是否成立；成立时执行下面的分支。
        if (!principal.superAdmin()) {
            // 中止正常处理并抛出异常：调用 BusinessException.forbidden 完成forbidden处理；由上层按失败处理。
            throw BusinessException.forbidden();
        }
        // 把当前登录身份（principal）交还给调用方，本方法到此结束。
        return principal;
    }
}
