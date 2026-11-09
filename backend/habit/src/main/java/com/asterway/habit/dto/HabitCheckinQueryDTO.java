/*
 * 声明习惯打卡查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

// 引入日期时间格式（DateTimeFormat），下面可以直接使用这个类型或静态成员。
import org.springframework.format.annotation.DateTimeFormat;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 本人列表筛选，不接收所属用户标识。 */
// 定义类 HabitCheckinQueryDTO；声明习惯打卡查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class HabitCheckinQueryDTO extends PageQueryDTO {
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

    // 已取消（cancelled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean cancelled;

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

    // 读取 已取消（Cancelled） 的值。
    public Boolean getCancelled() {
        // 把已取消（cancelled）交还给调用方，本方法到此结束。
        return cancelled;
    }

    // 设置 已取消（Cancelled） 的值。
    // 已取消（cancelled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setCancelled(Boolean cancelled) {
        // 把已取消（cancelled）保存到 this.cancelled。
        this.cancelled = cancelled;
    }

    // 读取 分页时跳过的记录数（Offset） 的值。
    public int getOffset() {
        // 把调用 offset 完成分页时跳过的记录数处理交还给调用方，本方法到此结束。
        return offset();
    }
}
