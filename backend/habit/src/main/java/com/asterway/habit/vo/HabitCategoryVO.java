/*
 * 声明习惯分类返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.vo;

/* 分类返回，标识使用字符串，允许同名的已删除历史分类继续存在。 */
// 定义保存一组固定字段的 record HabitCategoryVO；声明习惯分类返回给页面的字段，不直接把数据库实体全部暴露出去。
// 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
// 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
// 排序顺序（sortOrder），使用 int 类型保存。
// 版本号（version），使用 long 类型保存。
public record HabitCategoryVO(String id, String name, int sortOrder, long version) {}
