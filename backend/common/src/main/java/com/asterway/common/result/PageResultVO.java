/*
 * 提供当前页结果相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.result;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 分页返回对象，页码从 1 开始；items 与 total 使用相同的数据可见范围。 */
// 定义保存一组固定字段的 record PageResultVO；提供当前页结果相关的共享实现或应用配置。
// 数据列表（items），使用 List<T> 类型保存。
// 总数（total），使用 long 类型保存。
// 当前页（page），使用 int 类型保存。
// 每页条数（size），使用 int 类型保存。
public record PageResultVO<T>(List<T> items, long total, int page, int size) {
}
