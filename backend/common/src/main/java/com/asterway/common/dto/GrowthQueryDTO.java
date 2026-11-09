/*
 * 声明成长业务查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.dto;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 成长业务统一查询条件；用户身份不从请求参数读取 */
// 定义类 GrowthQueryDTO；声明成长业务查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class GrowthQueryDTO extends PageQueryDTO {

    // 读取 分页时跳过的记录数（Offset） 的值。
    public int getOffset() {
        // 把调用 offset 完成分页时跳过的记录数处理交还给调用方，本方法到此结束。
        return offset();
    }

    /* 关键字。 */
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    private String keyword;

    /* 状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 任务优先级。 */
    // priority，使用 String 类型保存；可为 null，表示尚未提供。
    private String priority;

    /* 任务位置 POOL/WEEK/DAY。 */
    // 任务安排状态（assignmentStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String assignmentStatus;

    /* 目标时间范围 ALL/CURRENT/FUTURE。 */
    // timing，使用 String 类型保存；可为 null，表示尚未提供。
    private String timing;

    /* 是否归档。 */
    // 已归档（archived），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean archived;

    /* 目标。 */
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;

    /* 阶段。 */
    // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long stageId;

    /* 任务。 */
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskId;

    /* 周计划。 */
    // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long weeklyPlanId;

    /* 日计划。 */
    // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long dailyPlanId;

    /* 开始日期下限。 */
    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 开始来源（startFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @org.springframework.format.annotation.DateTimeFormat(
            // 继续传入或组合表达式 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 的结果，完成上面尚未结束的调用或声明。
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate startFrom;

    /* 开始日期上限。 */
    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 开始To（startTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @org.springframework.format.annotation.DateTimeFormat(
            // 继续传入或组合表达式 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 的结果，完成上面尚未结束的调用或声明。
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate startTo;

    /* 计划日期下限。 */
    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @org.springframework.format.annotation.DateTimeFormat(
            // 继续传入或组合表达式 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 的结果，完成上面尚未结束的调用或声明。
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate dateFrom;

    /* 计划日期上限。 */
    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @org.springframework.format.annotation.DateTimeFormat(
            // 继续传入或组合表达式 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 的结果，完成上面尚未结束的调用或声明。
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate dateTo;

    /* 由 Service 按用户时区覆盖。 */
    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @org.springframework.format.annotation.DateTimeFormat(
            // 继续传入或组合表达式 iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) 的结果，完成上面尚未结束的调用或声明。
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate today;

    /* 是否仅当前安排。 */
    // 当前仅（currentOnly），使用 boolean 类型保存。
    private boolean currentOnly;

    // 读取 Keyword 的值。
    public String getKeyword() {
        // 把keyword交还给调用方，本方法到此结束。
        return keyword;
    }

    // 设置 Keyword 的值。
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    public void setKeyword(String keyword) {
        // 把keyword保存到 this.keyword。
        this.keyword = keyword;
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

    // 读取 任务安排状态（AssignmentStatus） 的值。
    public String getAssignmentStatus() {
        // 把任务安排状态（assignmentStatus）交还给调用方，本方法到此结束。
        return assignmentStatus;
    }

    // 设置 任务安排状态（AssignmentStatus） 的值。
    // 任务安排状态（assignmentStatus），使用 String 类型保存；可为 null，表示尚未提供。
    public void setAssignmentStatus(String assignmentStatus) {
        // 把任务安排状态（assignmentStatus）保存到 this.assignmentStatus。
        this.assignmentStatus = assignmentStatus;
    }

    // 读取 Timing 的值。
    public String getTiming() {
        // 把timing交还给调用方，本方法到此结束。
        return timing;
    }

    // 设置 Timing 的值。
    // timing，使用 String 类型保存；可为 null，表示尚未提供。
    public void setTiming(String timing) {
        // 把timing保存到 this.timing。
        this.timing = timing;
    }

    // 读取 已归档（Archived） 的值。
    public Boolean getArchived() {
        // 把已归档（archived）交还给调用方，本方法到此结束。
        return archived;
    }

    // 设置 已归档（Archived） 的值。
    // 已归档（archived），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setArchived(Boolean archived) {
        // 把已归档（archived）保存到 this.archived。
        this.archived = archived;
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

    // 读取 每日方案编号（DailyPlanId） 的值。
    public Long getDailyPlanId() {
        // 把每日方案编号（dailyPlanId）交还给调用方，本方法到此结束。
        return dailyPlanId;
    }

    // 设置 每日方案编号（DailyPlanId） 的值。
    // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setDailyPlanId(Long dailyPlanId) {
        // 把每日方案编号（dailyPlanId）保存到 this.dailyPlanId。
        this.dailyPlanId = dailyPlanId;
    }

    // 读取 开始来源（StartFrom） 的值。
    public LocalDate getStartFrom() {
        // 把开始来源（startFrom）交还给调用方，本方法到此结束。
        return startFrom;
    }

    // 设置 开始来源（StartFrom） 的值。
    // 开始来源（startFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setStartFrom(LocalDate startFrom) {
        // 把开始来源（startFrom）保存到 this.startFrom。
        this.startFrom = startFrom;
    }

    // 读取 开始To（StartTo） 的值。
    public LocalDate getStartTo() {
        // 把开始To（startTo）交还给调用方，本方法到此结束。
        return startTo;
    }

    // 设置 开始To（StartTo） 的值。
    // 开始To（startTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setStartTo(LocalDate startTo) {
        // 把开始To（startTo）保存到 this.startTo。
        this.startTo = startTo;
    }

    // 读取 日期来源（DateFrom） 的值。
    public LocalDate getDateFrom() {
        // 把日期来源（dateFrom）交还给调用方，本方法到此结束。
        return dateFrom;
    }

    // 设置 日期来源（DateFrom） 的值。
    // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setDateFrom(LocalDate dateFrom) {
        // 把日期来源（dateFrom）保存到 this.dateFrom。
        this.dateFrom = dateFrom;
    }

    // 读取 日期To（DateTo） 的值。
    public LocalDate getDateTo() {
        // 把日期To（dateTo）交还给调用方，本方法到此结束。
        return dateTo;
    }

    // 设置 日期To（DateTo） 的值。
    // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setDateTo(LocalDate dateTo) {
        // 把日期To（dateTo）保存到 this.dateTo。
        this.dateTo = dateTo;
    }

    // 读取 今天（Today） 的值。
    public LocalDate getToday() {
        // 把今天（today）交还给调用方，本方法到此结束。
        return today;
    }

    // 设置 今天（Today） 的值。
    // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setToday(LocalDate today) {
        // 把今天（today）保存到 this.today。
        this.today = today;
    }

    // 读取 当前仅（CurrentOnly） 的值。
    public boolean getCurrentOnly() {
        // 把当前仅（currentOnly）交还给调用方，本方法到此结束。
        return currentOnly;
    }

    // 设置 当前仅（CurrentOnly） 的值。
    // 当前仅（currentOnly），使用 boolean 类型保存。
    public void setCurrentOnly(boolean currentOnly) {
        // 把当前仅（currentOnly）保存到 this.currentOnly。
        this.currentOnly = currentOnly;
    }
}
