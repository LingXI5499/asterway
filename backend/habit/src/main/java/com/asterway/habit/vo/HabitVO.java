/*
 * 声明习惯返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;

/* 习惯当前资料；历史标准及分类以修订和打卡事实为准。 */
// 定义类 HabitVO；声明习惯返回给页面的字段，不直接把数据库实体全部暴露出去。
public class HabitVO {
    /* 习惯标识。 */
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    private String id;

    /* 生命周期状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 当前修订号。 */
    // 修订版本No（revisionNo），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long revisionNo;

    /* 结束归档时间。 */
    // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant endedAt;

    /* 已封存日。 */
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

    /* 当前修订标识。 */
    // 修订版本编号（revisionId），使用 String 类型保存；可为 null，表示尚未提供。
    private String revisionId;

    /* 当前名称。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* 描述。 */
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    private String description;

    /* 完成标准。 */
    // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
    private String completionCriteria;

    /* 可空分类标识。 */
    // 分类编号（categoryId），使用 String 类型保存；可为 null，表示尚未提供。
    private String categoryId;

    /* 分类名称。 */
    // 分类名称（categoryName），使用 String 类型保存；可为 null，表示尚未提供。
    private String categoryName;

    /* 分类已删除时仍保留当前引用。 */
    // 分类删除标记（categoryDeleted），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean categoryDeleted;

    /* 执行周期。 */
    // cycle类型（cycleType），使用 String 类型保存；可为 null，表示尚未提供。
    private String cycleType;

    /* 内部星期掩码，对外返回 weekdays。 */
    // weekdayMask，使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer weekdayMask;

    /* 开始日期。 */
    // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate startDate;

    /* 提示时间。 */
    // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
    private LocalTime expectedTime;

    /* 今日是否已有有效打卡。 */
    // 已核对今天（checkedToday），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean checkedToday;

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

    // 读取 修订版本编号（RevisionId） 的值。
    public String getRevisionId() {
        // 把修订版本编号（revisionId）交还给调用方，本方法到此结束。
        return revisionId;
    }

    // 设置 修订版本编号（RevisionId） 的值。
    // 修订版本编号（revisionId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setRevisionId(String revisionId) {
        // 把修订版本编号（revisionId）保存到 this.revisionId。
        this.revisionId = revisionId;
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
    public String getCategoryId() {
        // 把分类编号（categoryId）交还给调用方，本方法到此结束。
        return categoryId;
    }

    // 设置 分类编号（CategoryId） 的值。
    // 分类编号（categoryId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setCategoryId(String categoryId) {
        // 把分类编号（categoryId）保存到 this.categoryId。
        this.categoryId = categoryId;
    }

    // 读取 分类名称（CategoryName） 的值。
    public String getCategoryName() {
        // 把分类名称（categoryName）交还给调用方，本方法到此结束。
        return categoryName;
    }

    // 设置 分类名称（CategoryName） 的值。
    // 分类名称（categoryName），使用 String 类型保存；可为 null，表示尚未提供。
    public void setCategoryName(String categoryName) {
        // 把分类名称（categoryName）保存到 this.categoryName。
        this.categoryName = categoryName;
    }

    // 读取 分类删除标记（CategoryDeleted） 的值。
    public Boolean getCategoryDeleted() {
        // 把分类删除标记（categoryDeleted）交还给调用方，本方法到此结束。
        return categoryDeleted;
    }

    // 设置 分类删除标记（CategoryDeleted） 的值。
    // 分类删除标记（categoryDeleted），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setCategoryDeleted(Boolean categoryDeleted) {
        // 把分类删除标记（categoryDeleted）保存到 this.categoryDeleted。
        this.categoryDeleted = categoryDeleted;
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

    // 不把这个字段作为普通 JSON 内容输出。
    // 读取 WeekdayMask 的值。
    @com.fasterxml.jackson.annotation.JsonIgnore
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
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

    // 规定该字段编码成 JSON 时使用的格式。
    // 读取 预计时间（ExpectedTime） 的值。
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
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

    // 读取 已核对今天（CheckedToday） 的值。
    public Boolean getCheckedToday() {
        // 把已核对今天（checkedToday）交还给调用方，本方法到此结束。
        return checkedToday;
    }

    // 设置 已核对今天（CheckedToday） 的值。
    // 已核对今天（checkedToday），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setCheckedToday(Boolean checkedToday) {
        // 把已核对今天（checkedToday）保存到 this.checkedToday。
        this.checkedToday = checkedToday;
    }

    /* API 使用周一至周日的 1～7 数组，不向客户端暴露位运算存储。 */
    // 读取 Weekdays 的值。
    public java.util.List<Integer> getWeekdays() {
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return java.util.stream.IntStream.rangeClosed(1, 7)
                // 日（day），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(day -> weekdayMask != null && (weekdayMask & (1 << (day - 1))) != 0)
                // 继续对上一步结果执行调用 boxed 完成boxed处理。
                .boxed()
                // 继续对上一步结果执行调用 toList 完成to列表处理。
                .toList();
    }
}
