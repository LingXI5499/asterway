/*
 * 提供方案写入Port相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.integration;

// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;

/* 已确认方案的中立写入契约；只在 Java 父事务内调用，业务模块保留自身校验。 */
// 定义接口 PlanWritePort；提供方案写入Port相关的共享实现或应用配置。
public interface PlanWritePort {
    // 定义一组固定取值 Action；提供方案写入Port相关的共享实现或应用配置。
    // 准备创建（CREATE），类型为 Action；初始内容是新建 操作（Action） 实例。
    // 准备周（WEEK），类型为 Action；初始内容是新建 操作（Action） 实例。
    // 准备日（DAY），类型为 Action；初始内容是新建 操作（Action） 实例。
    // 准备当前任务安排（CURRENT_ASSIGNMENT），类型为 Action；初始内容是新建 操作（Action） 实例。
    // 准备安排（ASSIGN），类型为 Action；初始内容是新建 操作（Action） 实例。
    // 准备分类（CATEGORY），类型为 Action；初始内容是新建 操作（Action） 实例。
    enum Action { CREATE, WEEK, DAY, CURRENT_ASSIGNMENT, ASSIGN, CATEGORY }

    // domain方法：调用 domain 完成domain处理；参数负责传入本次处理所需的数据。
    String domain();

    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 操作（action），使用 Action 类型保存。
    // command，使用 JsonNode 类型保存。
    JsonNode write(Action action, JsonNode command);
}
