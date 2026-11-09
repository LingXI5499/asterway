/*
 * 用 Java 字段表示执行计时会话数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 任务的一次完整执行会话，只映射会话表，不作为接口返回对象。 */
// 定义类 ExecutionSessionEntity；用 Java 字段表示执行计时会话数据库记录，由 Mapper 负责读写。
public class ExecutionSessionEntity {

    /* 执行会话主键。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户，由认证上下文取得。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 所属任务，逻辑删除后仍保留引用。 */
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskId;

    /* RUNNING、PAUSED 或 FINISHED。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 首次开始时间，恢复时不覆盖。 */
    // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant startedAt;

    /* 最终结束时间，未结束时为空。 */
    // finishedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant finishedAt;

    /* 最近有效心跳，开始和恢复时初始化。 */
    // 最后一次心跳At（lastHeartbeatAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant lastHeartbeatAt;

    /* 控制端登录 SID，只保存在持久化对象中。 */
    // 请求入口会话编号（controllerSessionId），使用 String 类型保存；可为 null，表示尚未提供。
    private String controllerSessionId;

    /* 主动结束、任务删除或目标归档原因。 */
    // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
    private String finishReason;

    /* 状态控制版本；心跳不递增。 */
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

    // 读取 状态（Status） 的值。
    public String getStatus() {
        // 把状态（status）交还给调用方，本方法到此结束。
        return status;
    }

    // 设置 状态（Status） 的值。
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStatus(String status) {
        // 把状态（status）保存到 this.status。
        this.status = status;
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

    // 读取 FinishedAt 的值。
    public Instant getFinishedAt() {
        // 把finishedAt交还给调用方，本方法到此结束。
        return finishedAt;
    }

    // 设置 FinishedAt 的值。
    // finishedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setFinishedAt(Instant finishedAt) {
        // 把finishedAt保存到 this.finishedAt。
        this.finishedAt = finishedAt;
    }

    // 读取 最后一次心跳At（LastHeartbeatAt） 的值。
    public Instant getLastHeartbeatAt() {
        // 把最后一次心跳At（lastHeartbeatAt）交还给调用方，本方法到此结束。
        return lastHeartbeatAt;
    }

    // 设置 最后一次心跳At（LastHeartbeatAt） 的值。
    // 最后一次心跳At（lastHeartbeatAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        // 把最后一次心跳At（lastHeartbeatAt）保存到 this.lastHeartbeatAt。
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    // 读取 请求入口会话编号（ControllerSessionId） 的值。
    public String getControllerSessionId() {
        // 把请求入口会话编号（controllerSessionId）交还给调用方，本方法到此结束。
        return controllerSessionId;
    }

    // 设置 请求入口会话编号（ControllerSessionId） 的值。
    // 请求入口会话编号（controllerSessionId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setControllerSessionId(String controllerSessionId) {
        // 把请求入口会话编号（controllerSessionId）保存到 this.controllerSessionId。
        this.controllerSessionId = controllerSessionId;
    }

    // 读取 Finish原因（FinishReason） 的值。
    public String getFinishReason() {
        // 把finish原因（finishReason）交还给调用方，本方法到此结束。
        return finishReason;
    }

    // 设置 Finish原因（FinishReason） 的值。
    // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
    public void setFinishReason(String finishReason) {
        // 把finish原因（finishReason）保存到 this.finishReason。
        this.finishReason = finishReason;
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
