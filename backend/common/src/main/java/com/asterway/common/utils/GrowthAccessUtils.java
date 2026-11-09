/*
 * 集中处理成长业务访问权限Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 成长业务只属于普通用户，管理身份优先于账户保留的 USER 基础角色。 */
// 定义类 GrowthAccessUtils；集中处理成长业务访问权限Utils的重复规则，避免各业务模块分别实现。
public final class GrowthAccessUtils {
    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private GrowthAccessUtils() {}

    // 管理员（manager）方法：调用 manager 完成管理员处理；参数负责传入本次处理所需的数据。
    // 角色列表（roles），使用 Set<String> 类型保存。
    public static boolean manager(Set<String> roles) {
        // 把优先使用前面的有效值，没有时采用后面的备用值：roles.contains("ADMIN") || roles.contains("SUPER_ADMIN")交还给调用方，本方法到此结束。
        return roles.contains("ADMIN") || roles.contains("SUPER_ADMIN");
    }

    // 权限（permission）方法：调用 permission 完成权限处理；参数负责传入本次处理所需的数据。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    public static boolean permission(String code) {
        // 把优先使用前面的有效值，没有时采用后面的备用值：code != null && (code.startsWith("goal:") || code.startsWith("stage:") || code.startsWith("task:") || code.sta…交还给调用方，本方法到此结束。
        return code != null
                // 继续补充同一个判断或备用结果：条件 && (code.startsWith("goal:") 是否成立。
                && (code.startsWith("goal:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("stage:") 是否成立。
                        || code.startsWith("stage:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("task:") 是否成立。
                        || code.startsWith("task:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("planning:") 是否成立。
                        || code.startsWith("planning:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("execution:") 是否成立。
                        || code.startsWith("execution:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("habit:") 是否成立。
                        || code.startsWith("habit:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("notification:") 是否成立。
                        || code.startsWith("notification:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("dashboard:") 是否成立。
                        || code.startsWith("dashboard:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("statistics:") 是否成立。
                        || code.startsWith("statistics:")
                        // 继续补充同一个判断或备用结果：条件 || code.startsWith("agent:")); 是否成立。
                        || code.startsWith("agent:"));
    }

    // 菜单（menu）方法：调用 menu 完成菜单处理；参数负责传入本次处理所需的数据。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    // component，使用 String 类型保存；可为 null，表示尚未提供。
    public static boolean menu(String code, String component) {
        // 把优先使用前面的有效值，没有时采用后面的备用值：code != null && code.startsWith("growth_") || component != null && (component.startsWith("growth/") || compone…交还给调用方，本方法到此结束。
        return code != null && code.startsWith("growth_")
                // 继续补充同一个判断或备用结果：条件 || component != null 是否成立。
                || component != null
                        // 继续补充同一个判断或备用结果：条件 && (component.startsWith("growth/") 是否成立。
                        && (component.startsWith("growth/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("planning/") 是否成立。
                                || component.startsWith("planning/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("execution/") 是否成立。
                                || component.startsWith("execution/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("habit/") 是否成立。
                                || component.startsWith("habit/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("notification/") 是否成立。
                                || component.startsWith("notification/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("dashboard/") 是否成立。
                                || component.startsWith("dashboard/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("statistics/") 是否成立。
                                || component.startsWith("statistics/")
                                // 继续补充同一个判断或备用结果：条件 || component.startsWith("agent/")); 是否成立。
                                || component.startsWith("agent/"));
    }
}
