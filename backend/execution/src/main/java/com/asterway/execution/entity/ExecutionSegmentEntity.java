/*
 * 用 Java 字段表示执行计时连续计时段数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 一次连续实际执行，只映射时间段表，暂停间隔不产生记录。 */
// 定义类 ExecutionSegmentEntity；用 Java 字段表示执行计时连续计时段数据库记录，由 Mapper 负责读写。
public class ExecutionSegmentEntity {

    /* 连续执行时间段主键。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属执行会话。 */
    // 执行计时编号（executionId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long executionId;

    /* 本段开始时的任务安排，历史不改写。 */
    // 任务安排编号（assignmentId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long assignmentId;

    /* 会话内从一开始的时间段序号。 */
    // 连续计时段No（segmentNo），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer segmentNo;

    /* 本次开始或继续时间。 */
    // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant startedAt;

    /* 本次暂停或结束的有效截止时间。 */
    // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant endedAt;

    /* 停止本段的原因。 */
    // 结束原因（endReason），使用 String 类型保存；可为 null，表示尚未提供。
    private String endReason;

    /* 本段的计划及待生效时区边界。 */
    // eligibleUntil，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant eligibleUntil;

    /* 本段开始时的 IANA 时区快照。 */
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    private String timeZone;

    /* 本段采用的超时秒数快照。 */
    // 心跳超时秒数（heartbeatTimeoutSeconds），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer heartbeatTimeoutSeconds;

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

    // 读取 执行计时编号（ExecutionId） 的值。
    public Long getExecutionId() {
        // 把执行计时编号（executionId）交还给调用方，本方法到此结束。
        return executionId;
    }

    // 设置 执行计时编号（ExecutionId） 的值。
    // 执行计时编号（executionId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setExecutionId(Long executionId) {
        // 把执行计时编号（executionId）保存到 this.executionId。
        this.executionId = executionId;
    }

    // 读取 任务安排编号（AssignmentId） 的值。
    public Long getAssignmentId() {
        // 把任务安排编号（assignmentId）交还给调用方，本方法到此结束。
        return assignmentId;
    }

    // 设置 任务安排编号（AssignmentId） 的值。
    // 任务安排编号（assignmentId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setAssignmentId(Long assignmentId) {
        // 把任务安排编号（assignmentId）保存到 this.assignmentId。
        this.assignmentId = assignmentId;
    }

    // 读取 连续计时段No（SegmentNo） 的值。
    public Integer getSegmentNo() {
        // 把连续计时段No（segmentNo）交还给调用方，本方法到此结束。
        return segmentNo;
    }

    // 设置 连续计时段No（SegmentNo） 的值。
    // 连续计时段No（segmentNo），使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setSegmentNo(Integer segmentNo) {
        // 把连续计时段No（segmentNo）保存到 this.segmentNo。
        this.segmentNo = segmentNo;
    }

    // 读取 开始时At（StartedAt） 的值。
    public Instant getStartedAt() {
        // 把开始时At（startedAt）交还给调用方，本方法到此结束。
        return startedAt;
    }

    // 设置 开始时At（StartedAt） 的值。
    // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setStartedAt(Instant startedAt) {
        // 把开始时At（startedAt）保存到 this.startedAt。
        this.startedAt = startedAt;
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

    // 读取 EligibleUntil 的值。
    public Instant getEligibleUntil() {
        // 把eligibleUntil交还给调用方，本方法到此结束。
        return eligibleUntil;
    }

    // 设置 EligibleUntil 的值。
    // eligibleUntil，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setEligibleUntil(Instant eligibleUntil) {
        // 把eligibleUntil保存到 this.eligibleUntil。
        this.eligibleUntil = eligibleUntil;
    }

    // 读取 时间时区（TimeZone） 的值。
    public String getTimeZone() {
        // 把用户使用的时区（timeZone）交还给调用方，本方法到此结束。
        return timeZone;
    }

    // 设置 时间时区（TimeZone） 的值。
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTimeZone(String timeZone) {
        // 把用户使用的时区（timeZone）保存到 this.timeZone。
        this.timeZone = timeZone;
    }

    // 读取 心跳超时秒数（HeartbeatTimeoutSeconds） 的值。
    public Integer getHeartbeatTimeoutSeconds() {
        // 把心跳超时秒数（heartbeatTimeoutSeconds）交还给调用方，本方法到此结束。
        return heartbeatTimeoutSeconds;
    }

    // 设置 心跳超时秒数（HeartbeatTimeoutSeconds） 的值。
    // 心跳超时秒数（heartbeatTimeoutSeconds），使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setHeartbeatTimeoutSeconds(Integer heartbeatTimeoutSeconds) {
        // 把心跳超时秒数（heartbeatTimeoutSeconds）保存到 this.heartbeatTimeoutSeconds。
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
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
