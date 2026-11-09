/*
 * 用 Java 字段表示习惯打卡数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 单表持久化对象；所有归属和写入时间由服务端维护。 */
// 定义类 HabitCheckinEntity；用 Java 字段表示习惯打卡数据库记录，由 Mapper 负责读写。
public class HabitCheckinEntity {
    /* 打卡标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 所属习惯。 */
    // 习惯编号（habitId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long habitId;

    /* 打卡当时的修订。 */
    // 修订版本编号（revisionId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long revisionId;

    /* 原始本地日期，不随时区改变。 */
    // 按用户时区计算的业务日期（businessDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate businessDate;

    /* 打卡时有效时区。 */
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    private String timeZone;

    /* 首次打卡时间。 */
    // 已核对At（checkedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant checkedAt;

    /* 撤销时间。 */
    // 已取消At（cancelledAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant cancelledAt;

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

    // 读取 习惯编号（HabitId） 的值。
    public Long getHabitId() {
        // 把习惯编号（habitId）交还给调用方，本方法到此结束。
        return habitId;
    }

    // 设置 习惯编号（HabitId） 的值。
    // 习惯编号（habitId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setHabitId(Long habitId) {
        // 把习惯编号（habitId）保存到 this.habitId。
        this.habitId = habitId;
    }

    // 读取 修订版本编号（RevisionId） 的值。
    public Long getRevisionId() {
        // 把修订版本编号（revisionId）交还给调用方，本方法到此结束。
        return revisionId;
    }

    // 设置 修订版本编号（RevisionId） 的值。
    // 修订版本编号（revisionId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setRevisionId(Long revisionId) {
        // 把修订版本编号（revisionId）保存到 this.revisionId。
        this.revisionId = revisionId;
    }

    // 读取 Business日期（BusinessDate） 的值。
    public LocalDate getBusinessDate() {
        // 把按用户时区计算的业务日期（businessDate）交还给调用方，本方法到此结束。
        return businessDate;
    }

    // 设置 Business日期（BusinessDate） 的值。
    // 按用户时区计算的业务日期（businessDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setBusinessDate(LocalDate businessDate) {
        // 把按用户时区计算的业务日期（businessDate）保存到 this.businessDate。
        this.businessDate = businessDate;
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

    // 读取 已核对At（CheckedAt） 的值。
    public Instant getCheckedAt() {
        // 把已核对At（checkedAt）交还给调用方，本方法到此结束。
        return checkedAt;
    }

    // 设置 已核对At（CheckedAt） 的值。
    // 已核对At（checkedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setCheckedAt(Instant checkedAt) {
        // 把已核对At（checkedAt）保存到 this.checkedAt。
        this.checkedAt = checkedAt;
    }

    // 读取 已取消At（CancelledAt） 的值。
    public Instant getCancelledAt() {
        // 把已取消At（cancelledAt）交还给调用方，本方法到此结束。
        return cancelledAt;
    }

    // 设置 已取消At（CancelledAt） 的值。
    // 已取消At（cancelledAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setCancelledAt(Instant cancelledAt) {
        // 把已取消At（cancelledAt）保存到 this.cancelledAt。
        this.cancelledAt = cancelledAt;
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
