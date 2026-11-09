/*
 * 提供成长业务Overview相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.result;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;

/* 目标和阶段详情使用的轻量子项，不引入业务工程依赖。 */
// 定义保存一组固定字段的 record GrowthOverviewVO；提供成长业务Overview相关的共享实现或应用配置。
// 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
// 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
// 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
// 进度（progress），使用 BigDecimal 类型保存。
public record GrowthOverviewVO(String id, String name, String status, BigDecimal progress) {}
