/*
 * 声明规划任务返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 计划任务来源投影；所有关联同时限定用户 */
// 定义类 PlanningTaskVO；声明规划任务返回给页面的字段，不直接把数据库实体全部暴露出去。
public class PlanningTaskVO {

    /* 安排标识。 */
    // 任务安排编号（assignmentId），使用 String 类型保存；可为 null，表示尚未提供。
    private String assignmentId;

    /* 安排版本。 */
    // 任务安排版本号（assignmentVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long assignmentVersion;

    /* 任务标识。 */
    // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
    private String taskId;

    /* 任务名称。 */
    // 任务名称（taskName），使用 String 类型保存；可为 null，表示尚未提供。
    private String taskName;

    /* 任务状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 优先级。 */
    // priority，使用 String 类型保存；可为 null，表示尚未提供。
    private String priority;

    /* 权重。 */
    // 权重（weight），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer weight;

    /* 阶段标识。 */
    // 阶段编号（stageId），使用 String 类型保存；可为 null，表示尚未提供。
    private String stageId;

    /* 阶段名称。 */
    // 阶段名称（stageName），使用 String 类型保存；可为 null，表示尚未提供。
    private String stageName;

    /* 目标标识。 */
    // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
    private String goalId;

    /* 目标名称。 */
    // 目标名称（goalName），使用 String 类型保存；可为 null，表示尚未提供。
    private String goalName;

    /* 周计划。 */
    // 每周方案编号（weeklyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
    private String weeklyPlanId;

    /* 日计划。 */
    // 每日方案编号（dailyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
    private String dailyPlanId;

    /* 周一日期。 */
    // 所属周的周一日期（weekStartDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate weekStartDate;

    /* 安排日期。 */
    // 方案日期（planDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    private LocalDate planDate;

    /* 安排排序。 */
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer sortOrder;

    /* 来源目标归档时间。 */
    // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant archivedAt;

    // 读取 任务安排编号（AssignmentId） 的值。
    public String getAssignmentId() {
        // 把任务安排编号（assignmentId）交还给调用方，本方法到此结束。
        return assignmentId;
    }

    // 设置 任务安排编号（AssignmentId） 的值。
    // 任务安排编号（assignmentId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setAssignmentId(String assignmentId) {
        // 把任务安排编号（assignmentId）保存到 this.assignmentId。
        this.assignmentId = assignmentId;
    }

    // 读取 任务安排版本号（AssignmentVersion） 的值。
    public Long getAssignmentVersion() {
        // 把任务安排版本号（assignmentVersion）交还给调用方，本方法到此结束。
        return assignmentVersion;
    }

    // 设置 任务安排版本号（AssignmentVersion） 的值。
    // 任务安排版本号（assignmentVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setAssignmentVersion(Long assignmentVersion) {
        // 把任务安排版本号（assignmentVersion）保存到 this.assignmentVersion。
        this.assignmentVersion = assignmentVersion;
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

    // 读取 Priority 的值。
    public String getPriority() {
        // 把priority交还给调用方，本方法到此结束。
        return priority;
    }

    // 设置 Priority 的值。
    // priority，使用 String 类型保存；可为 null，表示尚未提供。
    public void setPriority(String priority) {
        // 把priority保存到 this.priority。
        this.priority = priority;
    }

    // 读取 权重（Weight） 的值。
    public Integer getWeight() {
        // 把权重（weight）交还给调用方，本方法到此结束。
        return weight;
    }

    // 设置 权重（Weight） 的值。
    // 权重（weight），使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setWeight(Integer weight) {
        // 把权重（weight）保存到 this.weight。
        this.weight = weight;
    }

    // 读取 阶段编号（StageId） 的值。
    public String getStageId() {
        // 把阶段编号（stageId）交还给调用方，本方法到此结束。
        return stageId;
    }

    // 设置 阶段编号（StageId） 的值。
    // 阶段编号（stageId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStageId(String stageId) {
        // 把阶段编号（stageId）保存到 this.stageId。
        this.stageId = stageId;
    }

    // 读取 阶段名称（StageName） 的值。
    public String getStageName() {
        // 把阶段名称（stageName）交还给调用方，本方法到此结束。
        return stageName;
    }

    // 设置 阶段名称（StageName） 的值。
    // 阶段名称（stageName），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStageName(String stageName) {
        // 把阶段名称（stageName）保存到 this.stageName。
        this.stageName = stageName;
    }

    // 读取 目标编号（GoalId） 的值。
    public String getGoalId() {
        // 把目标编号（goalId）交还给调用方，本方法到此结束。
        return goalId;
    }

    // 设置 目标编号（GoalId） 的值。
    // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setGoalId(String goalId) {
        // 把目标编号（goalId）保存到 this.goalId。
        this.goalId = goalId;
    }

    // 读取 目标名称（GoalName） 的值。
    public String getGoalName() {
        // 把目标名称（goalName）交还给调用方，本方法到此结束。
        return goalName;
    }

    // 设置 目标名称（GoalName） 的值。
    // 目标名称（goalName），使用 String 类型保存；可为 null，表示尚未提供。
    public void setGoalName(String goalName) {
        // 把目标名称（goalName）保存到 this.goalName。
        this.goalName = goalName;
    }

    // 读取 每周方案编号（WeeklyPlanId） 的值。
    public String getWeeklyPlanId() {
        // 把每周方案编号（weeklyPlanId）交还给调用方，本方法到此结束。
        return weeklyPlanId;
    }

    // 设置 每周方案编号（WeeklyPlanId） 的值。
    // 每周方案编号（weeklyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setWeeklyPlanId(String weeklyPlanId) {
        // 把每周方案编号（weeklyPlanId）保存到 this.weeklyPlanId。
        this.weeklyPlanId = weeklyPlanId;
    }

    // 读取 每日方案编号（DailyPlanId） 的值。
    public String getDailyPlanId() {
        // 把每日方案编号（dailyPlanId）交还给调用方，本方法到此结束。
        return dailyPlanId;
    }

    // 设置 每日方案编号（DailyPlanId） 的值。
    // 每日方案编号（dailyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setDailyPlanId(String dailyPlanId) {
        // 把每日方案编号（dailyPlanId）保存到 this.dailyPlanId。
        this.dailyPlanId = dailyPlanId;
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
    // 卡片展示的任务描述，内容仍取自原任务。
    private String description;

    // 预计耗时以分钟为单位，未填写时返回空值。
    private Integer estimatedMinutes;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
}
