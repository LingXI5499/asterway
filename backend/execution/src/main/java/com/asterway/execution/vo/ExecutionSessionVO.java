/*
 * 声明执行计时会话返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 执行会话的接口视图，明确排除控制端 SID 等内部字段。 */
// 定义类 ExecutionSessionVO；声明执行计时会话返回给页面的字段，不直接把数据库实体全部暴露出去。
public class ExecutionSessionVO {

    /* 会话标识。 */
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    private String id;

    /* 任务标识。 */
    // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
    private String taskId;

    /* 任务当前名称，历史不会因逻辑删除消失。 */
    // 任务名称（taskName），使用 String 类型保存；可为 null，表示尚未提供。
    private String taskName;

    /* 会话状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 首次开始时间。 */
    // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant startedAt;

    /* 最终结束时间。 */
    // finishedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant finishedAt;

    /* 会话结束原因。 */
    // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
    private String finishReason;

    /* 会话控制版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 任务当前版本。 */
    // 任务版本号（taskVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskVersion;

    /* 累计实际秒数，汇总后取整。 */
    // 实际秒数（actualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long actualSeconds;

    /* 查询区间内实际秒数，未筛日期时为空。 */
    // 区间实际秒数（rangeActualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long rangeActualSeconds;

    /* 最后有效心跳时间。 */
    // 最后一次心跳At（lastHeartbeatAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant lastHeartbeatAt;

    /* 开放时间段标识。 */
    // 当前有效连续计时段编号（activeSegmentId），使用 String 类型保存；可为 null，表示尚未提供。
    private String activeSegmentId;

    /* 开放段开始时间。 */
    // 当前有效连续计时段开始时At（activeSegmentStartedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant activeSegmentStartedAt;

    /* 本次查询捕获的服务端时间。 */
    // server当前时间（serverNow），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant serverNow;

    /* 心跳超时边界。 */
    // 心跳截止时间（heartbeatDeadline），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant heartbeatDeadline;

    /* 当前段的计划或时区边界。 */
    // eligibleUntil，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant eligibleUntil;

    /* 当前登录端是否可以为此段发送心跳。 */
    // 是否可以心跳（canHeartbeat），使用 boolean 类型保存。
    private boolean canHeartbeat;

    /* 前端应采用的心跳间隔。 */
    // 心跳间隔秒数（heartbeatIntervalSeconds），使用 int 类型保存。
    private int heartbeatIntervalSeconds;

    // 读取 编号（Id） 的值。
    public String getId() {
        // 把编号（id）交还给调用方，本方法到此结束。
        return id;
    }

    // 设置 编号（Id） 的值。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public void setId(String id) {
        // 把编号（id）保存到 this.id。
        this.id = id;
    }

    // 读取 任务编号（TaskId） 的值。
    public String getTaskId() {
        // 把任务编号（taskId）交还给调用方，本方法到此结束。
        return taskId;
    }

    // 设置 任务编号（TaskId） 的值。
    // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTaskId(String taskId) {
        // 把任务编号（taskId）保存到 this.taskId。
        this.taskId = taskId;
    }

    // 读取 任务名称（TaskName） 的值。
    public String getTaskName() {
        // 把任务名称（taskName）交还给调用方，本方法到此结束。
        return taskName;
    }

    // 设置 任务名称（TaskName） 的值。
    // 任务名称（taskName），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTaskName(String taskName) {
        // 把任务名称（taskName）保存到 this.taskName。
        this.taskName = taskName;
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

    // 读取 任务版本号（TaskVersion） 的值。
    public Long getTaskVersion() {
        // 把任务版本号（taskVersion）交还给调用方，本方法到此结束。
        return taskVersion;
    }

    // 设置 任务版本号（TaskVersion） 的值。
    // 任务版本号（taskVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setTaskVersion(Long taskVersion) {
        // 把任务版本号（taskVersion）保存到 this.taskVersion。
        this.taskVersion = taskVersion;
    }

    // 读取 实际秒数（ActualSeconds） 的值。
    public Long getActualSeconds() {
        // 把实际秒数（actualSeconds）交还给调用方，本方法到此结束。
        return actualSeconds;
    }

    // 设置 实际秒数（ActualSeconds） 的值。
    // 实际秒数（actualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setActualSeconds(Long actualSeconds) {
        // 把实际秒数（actualSeconds）保存到 this.actualSeconds。
        this.actualSeconds = actualSeconds;
    }

    // 读取 区间实际秒数（RangeActualSeconds） 的值。
    public Long getRangeActualSeconds() {
        // 把区间实际秒数（rangeActualSeconds）交还给调用方，本方法到此结束。
        return rangeActualSeconds;
    }

    // 设置 区间实际秒数（RangeActualSeconds） 的值。
    // 区间实际秒数（rangeActualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setRangeActualSeconds(Long rangeActualSeconds) {
        // 把区间实际秒数（rangeActualSeconds）保存到 this.rangeActualSeconds。
        this.rangeActualSeconds = rangeActualSeconds;
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

    // 读取 当前有效连续计时段编号（ActiveSegmentId） 的值。
    public String getActiveSegmentId() {
        // 把当前有效连续计时段编号（activeSegmentId）交还给调用方，本方法到此结束。
        return activeSegmentId;
    }

    // 设置 当前有效连续计时段编号（ActiveSegmentId） 的值。
    // 当前有效连续计时段编号（activeSegmentId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setActiveSegmentId(String activeSegmentId) {
        // 把当前有效连续计时段编号（activeSegmentId）保存到 this.activeSegmentId。
        this.activeSegmentId = activeSegmentId;
    }

    // 读取 当前有效连续计时段开始时At（ActiveSegmentStartedAt） 的值。
    public Instant getActiveSegmentStartedAt() {
        // 把当前有效连续计时段开始时At（activeSegmentStartedAt）交还给调用方，本方法到此结束。
        return activeSegmentStartedAt;
    }

    // 设置 当前有效连续计时段开始时At（ActiveSegmentStartedAt） 的值。
    // 当前有效连续计时段开始时At（activeSegmentStartedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setActiveSegmentStartedAt(Instant activeSegmentStartedAt) {
        // 把当前有效连续计时段开始时At（activeSegmentStartedAt）保存到 this.activeSegmentStartedAt。
        this.activeSegmentStartedAt = activeSegmentStartedAt;
    }

    // 读取 Server当前时间（ServerNow） 的值。
    public Instant getServerNow() {
        // 把server当前时间（serverNow）交还给调用方，本方法到此结束。
        return serverNow;
    }

    // 设置 Server当前时间（ServerNow） 的值。
    // server当前时间（serverNow），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setServerNow(Instant serverNow) {
        // 把server当前时间（serverNow）保存到 this.serverNow。
        this.serverNow = serverNow;
    }

    // 读取 心跳截止时间（HeartbeatDeadline） 的值。
    public Instant getHeartbeatDeadline() {
        // 把心跳截止时间（heartbeatDeadline）交还给调用方，本方法到此结束。
        return heartbeatDeadline;
    }

    // 设置 心跳截止时间（HeartbeatDeadline） 的值。
    // 心跳截止时间（heartbeatDeadline），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setHeartbeatDeadline(Instant heartbeatDeadline) {
        // 把心跳截止时间（heartbeatDeadline）保存到 this.heartbeatDeadline。
        this.heartbeatDeadline = heartbeatDeadline;
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

    // 读取 是否可以心跳（CanHeartbeat） 的值。
    public boolean getCanHeartbeat() {
        // 把是否可以心跳（canHeartbeat）交还给调用方，本方法到此结束。
        return canHeartbeat;
    }

    // 设置 是否可以心跳（CanHeartbeat） 的值。
    // 是否可以心跳（canHeartbeat），使用 boolean 类型保存。
    public void setCanHeartbeat(boolean canHeartbeat) {
        // 把是否可以心跳（canHeartbeat）保存到 this.canHeartbeat。
        this.canHeartbeat = canHeartbeat;
    }

    // 读取 心跳间隔秒数（HeartbeatIntervalSeconds） 的值。
    public int getHeartbeatIntervalSeconds() {
        // 把心跳间隔秒数（heartbeatIntervalSeconds）交还给调用方，本方法到此结束。
        return heartbeatIntervalSeconds;
    }

    // 设置 心跳间隔秒数（HeartbeatIntervalSeconds） 的值。
    // 心跳间隔秒数（heartbeatIntervalSeconds），使用 int 类型保存。
    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        // 把心跳间隔秒数（heartbeatIntervalSeconds）保存到 this.heartbeatIntervalSeconds。
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
}
