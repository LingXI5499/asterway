/*
 * 声明统计对象返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

/* 当前有效未归档对象概况，与历史完成动作分开。 */
// 定义保存一组固定字段的 record StatisticsObjectVO；声明统计对象返回给页面的字段，不直接把数据库实体全部暴露出去。
// 种类（kind），使用 String 类型保存；可为 null，表示尚未提供。
// 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
// 对象数量（objectCount），使用 long 类型保存。
public record StatisticsObjectVO(String kind, String status, long objectCount) {}
