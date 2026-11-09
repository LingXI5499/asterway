/*
 * 用 Java 字段表示任务任务安排数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 单表持久化对象；不直接作为 HTTP 返回值 */
// 定义类 TaskAssignmentEntity；用 Java 字段表示任务任务安排数据库记录，由 Mapper 负责读写。
public class TaskAssignmentEntity {

    /* 标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 任务。 */
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskId;

    /* 周计划。 */
    // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long weeklyPlanId;

    /* 日计划，可空。 */
    // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long dailyPlanId;

    /* 排序。 */
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer sortOrder;

    /* 安排建立时间。 */
    // assignedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant assignedAt;

    /* 安排结束时间。 */
    // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant endedAt;

    /* 结束原因。 */
    // 结束原因（endReason），使用 String 类型保存；可为 null，表示尚未提供。
    private String endReason;

    /* 乐观锁版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 创建时间。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 更新时间。 */
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant updatedAt;

    // 读取 编号（Id） 的值。
    public Long getId() {
        // 把编号（id）交还给调用方，本方法到此结束。
        return id;
    }

    // 设置 编号（Id） 的值。
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setId(Long id) {
        // 把编号（id）保存到 this.id。
        this.id = id;
    }

    // 读取 用户编号（UserId） 的值。
    public Long getUserId() {
        // 把用户编号（userId）交还给调用方，本方法到此结束。
        return userId;
    }

    // 设置 用户编号（UserId） 的值。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setUserId(Long userId) {
        // 把用户编号（userId）保存到 this.userId。
        this.userId = userId;
    }

    // 读取 任务编号（TaskId） 的值。
    public Long getTaskId() {
        // 把任务编号（taskId）交还给调用方，本方法到此结束。
        return taskId;
    }

    // 设置 任务编号（TaskId） 的值。
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setTaskId(Long taskId) {
        // 把任务编号（taskId）保存到 this.taskId。
        this.taskId = taskId;
    }

    // 读取 每周方案编号（WeeklyPlanId） 的值。
    public Long getWeeklyPlanId() {
        // 把每周方案编号（weeklyPlanId）交还给调用方，本方法到此结束。
        return weeklyPlanId;
    }

    // 设置 每周方案编号（WeeklyPlanId） 的值。
    // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setWeeklyPlanId(Long weeklyPlanId) {
        // 把每周方案编号（weeklyPlanId）保存到 this.weeklyPlanId。
        this.weeklyPlanId = weeklyPlanId;
    }

    // 读取 每日方案编号（DailyPlanId） 的值。
    public Long getDailyPlanId() {
        // 把每日方案编号（dailyPlanId）交还给调用方，本方法到此结束。
        return dailyPlanId;
    }

    // 设置 每日方案编号（DailyPlanId） 的值。
    // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setDailyPlanId(Long dailyPlanId) {
        // 把每日方案编号（dailyPlanId）保存到 this.dailyPlanId。
        this.dailyPlanId = dailyPlanId;
    }

    // 读取 排序顺序（SortOrder） 的值。
    public Integer getSortOrder() {
        // 把排序顺序（sortOrder）交还给调用方，本方法到此结束。
        return sortOrder;
    }

    // 设置 排序顺序（SortOrder） 的值。
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setSortOrder(Integer sortOrder) {
        // 把排序顺序（sortOrder）保存到 this.sortOrder。
        this.sortOrder = sortOrder;
    }

    // 读取 AssignedAt 的值。
    public Instant getAssignedAt() {
        // 把assignedAt交还给调用方，本方法到此结束。
        return assignedAt;
    }

    // 设置 AssignedAt 的值。
    // assignedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setAssignedAt(Instant assignedAt) {
        // 把assignedAt保存到 this.assignedAt。
        this.assignedAt = assignedAt;
    }

    // 读取 已结束At（EndedAt） 的值。
    public Instant getEndedAt() {
        // 把已结束At（endedAt）交还给调用方，本方法到此结束。
        return endedAt;
    }

    // 设置 已结束At（EndedAt） 的值。
    // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setEndedAt(Instant endedAt) {
        // 把已结束At（endedAt）保存到 this.endedAt。
        this.endedAt = endedAt;
    }

    // 读取 结束原因（EndReason） 的值。
    public String getEndReason() {
        // 把结束原因（endReason）交还给调用方，本方法到此结束。
        return endReason;
    }

    // 设置 结束原因（EndReason） 的值。
    // 结束原因（endReason），使用 String 类型保存；可为 null，表示尚未提供。
    public void setEndReason(String endReason) {
        // 把结束原因（endReason）保存到 this.endReason。
        this.endReason = endReason;
    }

    // 读取 版本号（Version） 的值。
    public Long getVersion() {
        // 把版本号（version）交还给调用方，本方法到此结束。
        return version;
    }

    // 设置 版本号（Version） 的值。
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setVersion(Long version) {
        // 把版本号（version）保存到 this.version。
        this.version = version;
    }

    // 读取 创建时At（CreatedAt） 的值。
    public Instant getCreatedAt() {
        // 把创建时At（createdAt）交还给调用方，本方法到此结束。
        return createdAt;
    }

    // 设置 创建时At（CreatedAt） 的值。
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setCreatedAt(Instant createdAt) {
        // 把创建时At（createdAt）保存到 this.createdAt。
        this.createdAt = createdAt;
    }

    // 读取 更新时At（UpdatedAt） 的值。
    public Instant getUpdatedAt() {
        // 把更新时At（updatedAt）交还给调用方，本方法到此结束。
        return updatedAt;
    }

    // 设置 更新时At（UpdatedAt） 的值。
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setUpdatedAt(Instant updatedAt) {
        // 把更新时At（updatedAt）保存到 this.updatedAt。
        this.updatedAt = updatedAt;
    }
}
