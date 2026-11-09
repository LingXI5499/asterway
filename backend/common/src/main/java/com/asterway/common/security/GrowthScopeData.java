/*
 * 处理成长业务数据范围数据相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 成长业务归属校验的中立投影，不依赖业务 Entity */
// 定义类 GrowthScopeData；处理成长业务数据范围数据相关的当前身份、权限或安全数据。
public class GrowthScopeData {

    /* 资源标识。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 目标标识。 */
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;

    /* 阶段标识。 */
    // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long stageId;

    /* 周计划标识。 */
    // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long weeklyPlanId;

    /* 状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 所属目标归档时间。 */
    // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant archivedAt;

    /* 日计划日期。 */
    // 方案日期（planDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate planDate;

    /* 周一日期。 */
    // 所属周的周一日期（weekStartDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate weekStartDate;

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

    // 读取 阶段编号（StageId） 的值。
    public Long getStageId() {
        // 把阶段编号（stageId）交还给调用方，本方法到此结束。
        return stageId;
    }

    // 设置 阶段编号（StageId） 的值。
    // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setStageId(Long stageId) {
        // 把阶段编号（stageId）保存到 this.stageId。
        this.stageId = stageId;
    }

    // 读取 每周方案编号（WeeklyPlanId） 的值。
    public Long getWeeklyPlanId() {
        // 把每周方案编号（weeklyPlanId）交还给调用方，本方法到此结束。
        return weeklyPlanId;
    }

    // 设置 每周方案编号（WeeklyPlanId） 的值。
    // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setWeeklyPlanId(Long weeklyPlanId) {
        // 把每周方案编号（weeklyPlanId）保存到 this.weeklyPlanId。
        this.weeklyPlanId = weeklyPlanId;
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

    // 读取 已归档At（ArchivedAt） 的值。
    public Instant getArchivedAt() {
        // 把已归档At（archivedAt）交还给调用方，本方法到此结束。
        return archivedAt;
    }

    // 设置 已归档At（ArchivedAt） 的值。
    // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setArchivedAt(Instant archivedAt) {
        // 把已归档At（archivedAt）保存到 this.archivedAt。
        this.archivedAt = archivedAt;
    }

    // 读取 方案日期（PlanDate） 的值。
    public LocalDate getPlanDate() {
        // 把方案日期（planDate）交还给调用方，本方法到此结束。
        return planDate;
    }

    // 设置 方案日期（PlanDate） 的值。
    // 方案日期（planDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setPlanDate(LocalDate planDate) {
        // 把方案日期（planDate）保存到 this.planDate。
        this.planDate = planDate;
    }

    // 读取 周开始日期（WeekStartDate） 的值。
    public LocalDate getWeekStartDate() {
        // 把所属周的周一日期（weekStartDate）交还给调用方，本方法到此结束。
        return weekStartDate;
    }

    // 设置 周开始日期（WeekStartDate） 的值。
    // 所属周的周一日期（weekStartDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setWeekStartDate(LocalDate weekStartDate) {
        // 把所属周的周一日期（weekStartDate）保存到 this.weekStartDate。
        this.weekStartDate = weekStartDate;
    }

    // 目标名称（goalName），使用 String 类型保存；可为 null，表示尚未提供。
    private String goalName;

    // 读取 目标名称（GoalName） 的值。
    public String getGoalName() {
        // 把目标名称（goalName）交还给调用方，本方法到此结束。
        return goalName;
    }

    // 设置 目标名称（GoalName） 的值。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public void setGoalName(String value) {
        // 把值（value）保存到 this.goalName。
        this.goalName = value;
    }

    // 阶段名称（stageName），使用 String 类型保存；可为 null，表示尚未提供。
    private String stageName;

    // 读取 阶段名称（StageName） 的值。
    public String getStageName() {
        // 把阶段名称（stageName）交还给调用方，本方法到此结束。
        return stageName;
    }

    // 设置 阶段名称（StageName） 的值。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStageName(String value) {
        // 把值（value）保存到 this.stageName。
        this.stageName = value;
    }

    // 任务安排状态（assignmentStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String assignmentStatus;

    // 读取 任务安排状态（AssignmentStatus） 的值。
    public String getAssignmentStatus() {
        // 把任务安排状态（assignmentStatus）交还给调用方，本方法到此结束。
        return assignmentStatus;
    }

    // 设置 任务安排状态（AssignmentStatus） 的值。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public void setAssignmentStatus(String value) {
        // 把值（value）保存到 this.assignmentStatus。
        this.assignmentStatus = value;
    }

    // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long dailyPlanId;

    // 读取 每日方案编号（DailyPlanId） 的值。
    public Long getDailyPlanId() {
        // 把每日方案编号（dailyPlanId）交还给调用方，本方法到此结束。
        return dailyPlanId;
    }

    // 设置 每日方案编号（DailyPlanId） 的值。
    // 值（value），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setDailyPlanId(Long value) {
        // 把值（value）保存到 this.dailyPlanId。
        this.dailyPlanId = value;
    }
}
