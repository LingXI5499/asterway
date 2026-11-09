/*
 * 声明统计查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

// 引入日期时间格式（DateTimeFormat），下面可以直接使用这个类型或静态成员。
import org.springframework.format.annotation.DateTimeFormat;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 多维统计条件；身份、时区与今天由服务端填写，不从客户端接收。 */
// 定义类 StatisticsQueryDTO；声明统计查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class StatisticsQueryDTO extends PageQueryDTO {
    // 准备统计周期（period），类型为 String；初始内容是固定状态或编码 MONTH（月）。
    private String period = "MONTH";

    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // anchor日期（anchorDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate anchorDate;

    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate dateFrom;

    // 使用 DateTimeFormat 注解配置下面的类、方法或参数。
    // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private LocalDate dateTo;

    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;
    // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long stageId;
    // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long taskId;

    // 读取 统计周期（Period） 的值。
    public String getPeriod() {
        // 把统计周期（period）交还给调用方，本方法到此结束。
        return period;
    }

    // 设置 统计周期（Period） 的值。
    // 统计周期（period），使用 String 类型保存；可为 null，表示尚未提供。
    public void setPeriod(String period) {
        // 把统计周期（period）保存到 this.period。
        this.period = period;
    }

    // 读取 Anchor日期（AnchorDate） 的值。
    public LocalDate getAnchorDate() {
        // 把anchor日期（anchorDate）交还给调用方，本方法到此结束。
        return anchorDate;
    }

    // 设置 Anchor日期（AnchorDate） 的值。
    // anchor日期（anchorDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void setAnchorDate(LocalDate anchorDate) {
        // 把anchor日期（anchorDate）保存到 this.anchorDate。
        this.anchorDate = anchorDate;
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

    // 读取 分页时跳过的记录数（Offset） 的值。
    public int getOffset() {
        // 把调用 offset 完成分页时跳过的记录数处理交还给调用方，本方法到此结束。
        return offset();
    }
}
