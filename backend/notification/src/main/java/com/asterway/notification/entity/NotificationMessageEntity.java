/*
 * 用 Java 字段表示通知消息数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 单表持久化对象；所有归属和写入时间由服务端维护。 */
// 定义类 NotificationMessageEntity；用 Java 字段表示通知消息数据库记录，由 Mapper 负责读写。
public class NotificationMessageEntity {
    /* 消息标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 收件人。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 来源目标。 */
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;

    /* 达标轮次。 */
    // condition轮（conditionRound），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long conditionRound;

    /* 本期仅 GOAL_COMPLETION_CONFIRM。 */
    // 消息类型（messageType），使用 String 类型保存；可为 null，表示尚未提供。
    private String messageType;

    /* 标题快照。 */
    // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
    private String title;

    /* 正文快照。 */
    // 内容（content），使用 String 类型保存；可为 null，表示尚未提供。
    private String content;

    /* 首次已读时间。 */
    // 读取At（readAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant readAt;

    /* 处理状态，与已读独立。 */
    // handle状态（handleStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String handleStatus;

    /* 处理或失效时间。 */
    // handledAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant handledAt;

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

    // 读取 目标编号（GoalId） 的值。
    public Long getGoalId() {
        // 把目标编号（goalId）交还给调用方，本方法到此结束。
        return goalId;
    }

    // 设置 目标编号（GoalId） 的值。
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setGoalId(Long goalId) {
        // 把目标编号（goalId）保存到 this.goalId。
        this.goalId = goalId;
    }

    // 读取 Condition轮（ConditionRound） 的值。
    public Long getConditionRound() {
        // 把condition轮（conditionRound）交还给调用方，本方法到此结束。
        return conditionRound;
    }

    // 设置 Condition轮（ConditionRound） 的值。
    // condition轮（conditionRound），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setConditionRound(Long conditionRound) {
        // 把condition轮（conditionRound）保存到 this.conditionRound。
        this.conditionRound = conditionRound;
    }

    // 读取 消息类型（MessageType） 的值。
    public String getMessageType() {
        // 把消息类型（messageType）交还给调用方，本方法到此结束。
        return messageType;
    }

    // 设置 消息类型（MessageType） 的值。
    // 消息类型（messageType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setMessageType(String messageType) {
        // 把消息类型（messageType）保存到 this.messageType。
        this.messageType = messageType;
    }

    // 读取 标题（Title） 的值。
    public String getTitle() {
        // 把标题（title）交还给调用方，本方法到此结束。
        return title;
    }

    // 设置 标题（Title） 的值。
    // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTitle(String title) {
        // 把标题（title）保存到 this.title。
        this.title = title;
    }

    // 读取 内容（Content） 的值。
    public String getContent() {
        // 把内容（content）交还给调用方，本方法到此结束。
        return content;
    }

    // 设置 内容（Content） 的值。
    // 内容（content），使用 String 类型保存；可为 null，表示尚未提供。
    public void setContent(String content) {
        // 把内容（content）保存到 this.content。
        this.content = content;
    }

    // 读取 读取At（ReadAt） 的值。
    public Instant getReadAt() {
        // 把读取At（readAt）交还给调用方，本方法到此结束。
        return readAt;
    }

    // 设置 读取At（ReadAt） 的值。
    // 读取At（readAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setReadAt(Instant readAt) {
        // 把读取At（readAt）保存到 this.readAt。
        this.readAt = readAt;
    }

    // 读取 Handle状态（HandleStatus） 的值。
    public String getHandleStatus() {
        // 把handle状态（handleStatus）交还给调用方，本方法到此结束。
        return handleStatus;
    }

    // 设置 Handle状态（HandleStatus） 的值。
    // handle状态（handleStatus），使用 String 类型保存；可为 null，表示尚未提供。
    public void setHandleStatus(String handleStatus) {
        // 把handle状态（handleStatus）保存到 this.handleStatus。
        this.handleStatus = handleStatus;
    }

    // 读取 HandledAt 的值。
    public Instant getHandledAt() {
        // 把handledAt交还给调用方，本方法到此结束。
        return handledAt;
    }

    // 设置 HandledAt 的值。
    // handledAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setHandledAt(Instant handledAt) {
        // 把handledAt保存到 this.handledAt。
        this.handledAt = handledAt;
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
