/*
 * 声明统计习惯查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.dto;

/* 习惯统计分组；只支持 HABIT 或 CATEGORY，不接受任意 SQL 维度。 */
// 定义类 StatisticsHabitQueryDTO；声明统计习惯查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class StatisticsHabitQueryDTO extends StatisticsQueryDTO {
    // 准备分组按（groupBy），类型为 String；初始内容是固定状态或编码 HABIT（习惯）。
    private String groupBy = "HABIT";

    // 读取 分组按（GroupBy） 的值。
    public String getGroupBy() {
        // 把分组按（groupBy）交还给调用方，本方法到此结束。
        return groupBy;
    }

    // 设置 分组按（GroupBy） 的值。
    // 分组按（groupBy），使用 String 类型保存；可为 null，表示尚未提供。
    public void setGroupBy(String groupBy) {
        // 把分组按（groupBy）保存到 this.groupBy。
        this.groupBy = groupBy;
    }
}
