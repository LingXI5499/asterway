/*
 * 用 Java 字段表示任务当前状态历史数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.task.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 追加式状态事实；不提供修改或删除业务操作。 */
// 定义类 TaskStateHistoryEntity；用 Java 字段表示任务当前状态历史数据库记录，由 Mapper 负责读写。
public class TaskStateHistoryEntity {
    /* 历史事实标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 所属业务对象。 */
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskId;

    /* 真实事件类型。 */
    // 事件类型（eventType），使用 String 类型保存；可为 null，表示尚未提供。
    private String eventType;

    /* 变化前状态。 */
    // 变化前状态（beforeStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String beforeStatus;

    /* 变化后状态。 */
    // 变化后状态（afterStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String afterStatus;

    /* 事件发生时的名称。 */
    // 名称确认时的数据快照（nameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    private String nameSnapshot;

    /* 该事件对应的主体版本。 */
    // 版本号（entityVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long entityVersion;

    /* 业务发生时间。 */
    // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant occurredAt;

    /* 用户、联动或初始化来源。 */
    // 来源类型（sourceType），使用 String 类型保存；可为 null，表示尚未提供。
    private String sourceType;

    /* 记录入库时间。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

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

    // 读取 事件类型（EventType） 的值。
    public String getEventType() {
        // 把事件类型（eventType）交还给调用方，本方法到此结束。
        return eventType;
    }

    // 设置 事件类型（EventType） 的值。
    // 事件类型（eventType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setEventType(String eventType) {
        // 把事件类型（eventType）保存到 this.eventType。
        this.eventType = eventType;
    }

    // 读取 变化前状态（BeforeStatus） 的值。
    public String getBeforeStatus() {
        // 把变化前状态（beforeStatus）交还给调用方，本方法到此结束。
        return beforeStatus;
    }

    // 设置 变化前状态（BeforeStatus） 的值。
    // 变化前状态（beforeStatus），使用 String 类型保存；可为 null，表示尚未提供。
    public void setBeforeStatus(String beforeStatus) {
        // 把变化前状态（beforeStatus）保存到 this.beforeStatus。
        this.beforeStatus = beforeStatus;
    }

    // 读取 变化后状态（AfterStatus） 的值。
    public String getAfterStatus() {
        // 把变化后状态（afterStatus）交还给调用方，本方法到此结束。
        return afterStatus;
    }

    // 设置 变化后状态（AfterStatus） 的值。
    // 变化后状态（afterStatus），使用 String 类型保存；可为 null，表示尚未提供。
    public void setAfterStatus(String afterStatus) {
        // 把变化后状态（afterStatus）保存到 this.afterStatus。
        this.afterStatus = afterStatus;
    }

    // 读取 名称确认时的数据快照（NameSnapshot） 的值。
    public String getNameSnapshot() {
        // 把名称确认时的数据快照（nameSnapshot）交还给调用方，本方法到此结束。
        return nameSnapshot;
    }

    // 设置 名称确认时的数据快照（NameSnapshot） 的值。
    // 名称确认时的数据快照（nameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    public void setNameSnapshot(String nameSnapshot) {
        // 把名称确认时的数据快照（nameSnapshot）保存到 this.nameSnapshot。
        this.nameSnapshot = nameSnapshot;
    }

    // 读取 版本号（EntityVersion） 的值。
    public Long getEntityVersion() {
        // 把版本号（entityVersion）交还给调用方，本方法到此结束。
        return entityVersion;
    }

    // 设置 版本号（EntityVersion） 的值。
    // 版本号（entityVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setEntityVersion(Long entityVersion) {
        // 把版本号（entityVersion）保存到 this.entityVersion。
        this.entityVersion = entityVersion;
    }

    // 读取 OccurredAt 的值。
    public Instant getOccurredAt() {
        // 把occurredAt交还给调用方，本方法到此结束。
        return occurredAt;
    }

    // 设置 OccurredAt 的值。
    // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setOccurredAt(Instant occurredAt) {
        // 把occurredAt保存到 this.occurredAt。
        this.occurredAt = occurredAt;
    }

    // 读取 来源类型（SourceType） 的值。
    public String getSourceType() {
        // 把来源类型（sourceType）交还给调用方，本方法到此结束。
        return sourceType;
    }

    // 设置 来源类型（SourceType） 的值。
    // 来源类型（sourceType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setSourceType(String sourceType) {
        // 把来源类型（sourceType）保存到 this.sourceType。
        this.sourceType = sourceType;
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
}
