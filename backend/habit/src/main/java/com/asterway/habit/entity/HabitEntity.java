/*
 * 用 Java 字段表示习惯数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 单表持久化对象；所有归属和写入时间由服务端维护。 */
// 定义类 HabitEntity；用 Java 字段表示习惯数据库记录，由 Mapper 负责读写。
public class HabitEntity {
    /* 习惯标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* ACTIVE／PAUSED／ENDED。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 当前完整修订号。 */
    // 当前修订版本No（currentRevisionNo），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long currentRevisionNo;

    /* 结束归档时间。 */
    // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant endedAt;

    /* 已封存到的日期，只能前进。 */
    // 历史LockedThrough（historyLockedThrough），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate historyLockedThrough;

    /* 乐观锁版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 创建时间。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 更新时间。 */
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant updatedAt;

    /* 逻辑删除时间。 */
    // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant deletedAt;

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

    // 读取 当前修订版本No（CurrentRevisionNo） 的值。
    public Long getCurrentRevisionNo() {
        // 把当前修订版本No（currentRevisionNo）交还给调用方，本方法到此结束。
        return currentRevisionNo;
    }

    // 设置 当前修订版本No（CurrentRevisionNo） 的值。
    // 当前修订版本No（currentRevisionNo），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setCurrentRevisionNo(Long currentRevisionNo) {
        // 把当前修订版本No（currentRevisionNo）保存到 this.currentRevisionNo。
        this.currentRevisionNo = currentRevisionNo;
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

    // 读取 历史LockedThrough（HistoryLockedThrough） 的值。
    public LocalDate getHistoryLockedThrough() {
        // 把历史LockedThrough（historyLockedThrough）交还给调用方，本方法到此结束。
        return historyLockedThrough;
    }

    // 设置 历史LockedThrough（HistoryLockedThrough） 的值。
    // 历史LockedThrough（historyLockedThrough），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setHistoryLockedThrough(LocalDate historyLockedThrough) {
        // 把历史LockedThrough（historyLockedThrough）保存到 this.historyLockedThrough。
        this.historyLockedThrough = historyLockedThrough;
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

    // 读取 删除标记At（DeletedAt） 的值。
    public Instant getDeletedAt() {
        // 把删除标记At（deletedAt）交还给调用方，本方法到此结束。
        return deletedAt;
    }

    // 设置 删除标记At（DeletedAt） 的值。
    // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setDeletedAt(Instant deletedAt) {
        // 把删除标记At（deletedAt）保存到 this.deletedAt。
        this.deletedAt = deletedAt;
    }
}
