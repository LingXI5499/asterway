/*
 * 用 Java 字段表示阶段数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.stage.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 单表持久化对象；不直接作为 HTTP 返回值 */
// 定义类 StageEntity；用 Java 字段表示阶段数据库记录，由 Mapper 负责读写。
public class StageEntity {

    /* 标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 所属用户。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 所属目标。 */
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;

    /* 名称。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* 描述。 */
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    private String description;

    /* 完成条件。 */
    // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
    private String completionCriteria;

    /* 开始日期。 */
    // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate startDate;

    /* 结束日期。 */
    // 结束日期（endDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate endDate;

    /* 排序。 */
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer sortOrder;

    /* 状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 乐观锁版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 创建时间。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 更新时间。 */
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant updatedAt;

    /* 删除时间。 */
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

    // 读取 结束日期（EndDate） 的值。
    public LocalDate getEndDate() {
        // 把结束日期（endDate）交还给调用方，本方法到此结束。
        return endDate;
    }

    // 设置 结束日期（EndDate） 的值。
    // 结束日期（endDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setEndDate(LocalDate endDate) {
        // 把结束日期（endDate）保存到 this.endDate。
        this.endDate = endDate;
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
