/*
 * 处理任务当前状态历史提供器相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 执行开始对任务状态事实的中立联动，不将安全日志当作业务历史。 */
// 定义接口 TaskStateHistoryProvider；处理任务当前状态历史提供器相关的当前身份、权限或安全数据。
public interface TaskStateHistoryProvider {
    /* 任务已成功推进版本后调用；只有状态实际变化才追加事件。 */
    // 记录执行计时开始（recordExecutionStart）方法：调用 recordExecutionStart 完成记录执行计时开始处理；参数负责传入本次处理所需的数据。
    void recordExecutionStart(
            // 用户编号（userId），使用 long 类型保存。
            // 任务编号（taskId），使用 long 类型保存。
            // 变化前状态（beforeStatus），使用 String 类型保存；可为 null，表示尚未提供。
            // 版本号（entityVersion），使用 long 类型保存。
            // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
            long userId, long taskId, String beforeStatus, long entityVersion, Instant occurredAt);
}
