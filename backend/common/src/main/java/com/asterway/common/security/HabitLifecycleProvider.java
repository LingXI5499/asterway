/*
 * 处理习惯Lifecycle提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 账户生命周期对习惯的中立联动，必须加入父操作事务并先锁同一用户。 */
// 定义接口 HabitLifecycleProvider；处理习惯Lifecycle提供器相关的当前身份、权限或安全数据。
public interface HabitLifecycleProvider {
    /* 停用、删除账户或转为管理身份时暂停 ACTIVE 习惯，不自动恢复。 */
    // suspend个人（suspendPersonal）方法：调用 suspendPersonal 完成suspend个人处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    void suspendPersonal(long userId);

    /* 清除已生效的待变时区前封存旧时区最后一天，防止回拨日期重开历史。 */
    // seal变化前时区Switch（sealBeforeZoneSwitch）方法：调用 sealBeforeZoneSwitch 完成seal变化前时区Switch处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 生效At（effectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    // 上一次时区（previousZone），使用 String 类型保存；可为 null，表示尚未提供。
    void sealBeforeZoneSwitch(long userId, Instant effectiveAt, String previousZone);
}
