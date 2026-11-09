/*
 * 处理权限Guard相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;

// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* 方法权限表达式入口；对象级管理范围继续由 Service 校验。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 PermissionGuard；处理权限Guard相关的当前身份、权限或安全数据。
@Component("access")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class PermissionGuard {

    // 是否存在权限（hasPermission）方法：判断是否有 权限（Permission）；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    public boolean hasPermission(String permission) {
        // 准备当前登录身份（principal），类型为 AccountPrincipal；初始内容是调用 SecurityUtils.current 完成当前处理。
        AccountPrincipal principal = SecurityUtils.current();
        // 把优先使用前面的有效值，没有时采用后面的备用值：!(com.asterway.common.utils.GrowthAccessUtils.permission(permission) && (com.asterway.common.utils.GrowthAcces…交还给调用方，本方法到此结束。
        return !(com.asterway.common.utils.GrowthAccessUtils.permission(permission)
                        // 继续补充同一个判断或备用结果：条件 && (com.asterway.common.utils.GrowthAccessUtils.manager(principal.roles()) 是否成立。
                        && (com.asterway.common.utils.GrowthAccessUtils.manager(principal.roles())
                                // 继续补充同一个判断或备用结果：条件 || !principal.roles().contains("USER"))) 是否成立。
                                || !principal.roles().contains("USER")))
                // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& (principal.superAdmin() || principal.permissions().contains(permission));。
                && (principal.superAdmin() || principal.permissions().contains(permission));
    }

    // 是否SuperAdmin（isSuperAdmin）方法：判断是否为 SuperAdmin；参数负责传入本次处理所需的数据。
    public boolean isSuperAdmin() {
        // 把调用 SecurityUtils.current 完成当前处理交还给调用方，本方法到此结束。
        return SecurityUtils.current().superAdmin();
    }
}
