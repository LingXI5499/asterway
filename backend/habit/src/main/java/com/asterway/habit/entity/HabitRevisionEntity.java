/*
 * 用 Java 字段表示习惯修订版本数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;

/* 单表持久化对象；所有归属和写入时间由服务端维护。 */
// 定义类 HabitRevisionEntity；用 Java 字段表示习惯修订版本数据库记录，由 Mapper 负责读写。
public class HabitRevisionEntity {
    /* 修订标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 所属习惯。 */
    // 习惯编号（habitId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long habitId;

    /* 连续修订号。 */
    // 修订版本No（revisionNo），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long revisionNo;

    /* 规则或生命周期动作。 */
    // 操作类型（actionType），使用 String 类型保存；可为 null，表示尚未提供。
    private String actionType;

    /* 名称快照。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* 描述快照。 */
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    private String description;

    /* 完成标准快照。 */
    // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
    private String completionCriteria;

    /* 分类快照，可空。 */
    // 分类编号（categoryId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long categoryId;

    /* DAILY／WEEKLY_DAYS。 */
    // cycle类型（cycleType），使用 String 类型保存；可为 null，表示尚未提供。
    private String cycleType;

    /* 星期位图，周一为最低位。 */
    // weekdayMask，使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer weekdayMask;

    /* 本地开始日期。 */
    // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate startDate;

    /* 提示时间，仅 HH:mm。 */
    // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
    private LocalTime expectedTime;

    /* 动作后状态。 */
    // 状态确认时的数据快照（statusSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    private String statusSnapshot;

    /* 动作后删除标志。 */
    // 删除标记确认时的数据快照（deletedSnapshot），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean deletedSnapshot;

    /* 本次修订生效日。 */
    // 生效日期（effectiveDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate effectiveDate;

    /* 服务端动作时间。 */
    // 已修改At（changedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant changedAt;

    /* 动作时账号有效时区。 */
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    private String timeZone;

    /* 入库时间。 */
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

    // 读取 修订版本No（RevisionNo） 的值。
    public Long getRevisionNo() {
        // 把修订版本No（revisionNo）交还给调用方，本方法到此结束。
        return revisionNo;
    }

    // 设置 修订版本No（RevisionNo） 的值。
    // 修订版本No（revisionNo），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setRevisionNo(Long revisionNo) {
        // 把修订版本No（revisionNo）保存到 this.revisionNo。
        this.revisionNo = revisionNo;
    }

    // 读取 操作类型（ActionType） 的值。
    public String getActionType() {
        // 把操作类型（actionType）交还给调用方，本方法到此结束。
        return actionType;
    }

    // 设置 操作类型（ActionType） 的值。
    // 操作类型（actionType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setActionType(String actionType) {
        // 把操作类型（actionType）保存到 this.actionType。
        this.actionType = actionType;
    }

    // 读取 名称（Name） 的值。
    public String getName() {
        // 把名称（name）交还给调用方，本方法到此结束。
        return name;
    }

    // 设置 名称（Name） 的值。
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    public void setName(String name) {
        // 把名称（name）保存到 this.name。
        this.name = name;
    }

    // 读取 说明（Description） 的值。
    public String getDescription() {
        // 把说明（description）交还给调用方，本方法到此结束。
        return description;
    }

    // 设置 说明（Description） 的值。
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    public void setDescription(String description) {
        // 把说明（description）保存到 this.description。
        this.description = description;
    }

    // 读取 完成情况判断标准（CompletionCriteria） 的值。
    public String getCompletionCriteria() {
        // 把完成情况判断标准（completionCriteria）交还给调用方，本方法到此结束。
        return completionCriteria;
    }

    // 设置 完成情况判断标准（CompletionCriteria） 的值。
    // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
    public void setCompletionCriteria(String completionCriteria) {
        // 把完成情况判断标准（completionCriteria）保存到 this.completionCriteria。
        this.completionCriteria = completionCriteria;
    }

    // 读取 分类编号（CategoryId） 的值。
    public Long getCategoryId() {
        // 把分类编号（categoryId）交还给调用方，本方法到此结束。
        return categoryId;
    }

    // 设置 分类编号（CategoryId） 的值。
    // 分类编号（categoryId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setCategoryId(Long categoryId) {
        // 把分类编号（categoryId）保存到 this.categoryId。
        this.categoryId = categoryId;
    }

    // 读取 Cycle类型（CycleType） 的值。
    public String getCycleType() {
        // 把cycle类型（cycleType）交还给调用方，本方法到此结束。
        return cycleType;
    }

    // 设置 Cycle类型（CycleType） 的值。
    // cycle类型（cycleType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setCycleType(String cycleType) {
        // 把cycle类型（cycleType）保存到 this.cycleType。
        this.cycleType = cycleType;
    }

    // 读取 WeekdayMask 的值。
    public Integer getWeekdayMask() {
        // 把weekdayMask交还给调用方，本方法到此结束。
        return weekdayMask;
    }

    // 设置 WeekdayMask 的值。
    // weekdayMask，使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setWeekdayMask(Integer weekdayMask) {
        // 把weekdayMask保存到 this.weekdayMask。
        this.weekdayMask = weekdayMask;
    }

    // 读取 开始日期（StartDate） 的值。
    public LocalDate getStartDate() {
        // 把开始日期（startDate）交还给调用方，本方法到此结束。
        return startDate;
    }

    // 设置 开始日期（StartDate） 的值。
    // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setStartDate(LocalDate startDate) {
        // 把开始日期（startDate）保存到 this.startDate。
        this.startDate = startDate;
    }

    // 读取 预计时间（ExpectedTime） 的值。
    public LocalTime getExpectedTime() {
        // 把预计时间（expectedTime）交还给调用方，本方法到此结束。
        return expectedTime;
    }

    // 设置 预计时间（ExpectedTime） 的值。
    // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
    public void setExpectedTime(LocalTime expectedTime) {
        // 把预计时间（expectedTime）保存到 this.expectedTime。
        this.expectedTime = expectedTime;
    }

    // 读取 状态确认时的数据快照（StatusSnapshot） 的值。
    public String getStatusSnapshot() {
        // 把状态确认时的数据快照（statusSnapshot）交还给调用方，本方法到此结束。
        return statusSnapshot;
    }

    // 设置 状态确认时的数据快照（StatusSnapshot） 的值。
    // 状态确认时的数据快照（statusSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStatusSnapshot(String statusSnapshot) {
        // 把状态确认时的数据快照（statusSnapshot）保存到 this.statusSnapshot。
        this.statusSnapshot = statusSnapshot;
    }

    // 读取 删除标记确认时的数据快照（DeletedSnapshot） 的值。
    public Boolean getDeletedSnapshot() {
        // 把删除标记确认时的数据快照（deletedSnapshot）交还给调用方，本方法到此结束。
        return deletedSnapshot;
    }

    // 设置 删除标记确认时的数据快照（DeletedSnapshot） 的值。
    // 删除标记确认时的数据快照（deletedSnapshot），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setDeletedSnapshot(Boolean deletedSnapshot) {
        // 把删除标记确认时的数据快照（deletedSnapshot）保存到 this.deletedSnapshot。
        this.deletedSnapshot = deletedSnapshot;
    }

    // 读取 生效日期（EffectiveDate） 的值。
    public LocalDate getEffectiveDate() {
        // 把生效日期（effectiveDate）交还给调用方，本方法到此结束。
        return effectiveDate;
    }

    // 设置 生效日期（EffectiveDate） 的值。
    // 生效日期（effectiveDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setEffectiveDate(LocalDate effectiveDate) {
        // 把生效日期（effectiveDate）保存到 this.effectiveDate。
        this.effectiveDate = effectiveDate;
    }

    // 读取 已修改At（ChangedAt） 的值。
    public Instant getChangedAt() {
        // 把已修改At（changedAt）交还给调用方，本方法到此结束。
        return changedAt;
    }

    // 设置 已修改At（ChangedAt） 的值。
    // 已修改At（changedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setChangedAt(Instant changedAt) {
        // 把已修改At（changedAt）保存到 this.changedAt。
        this.changedAt = changedAt;
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
