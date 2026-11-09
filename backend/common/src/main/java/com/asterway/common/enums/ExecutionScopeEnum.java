/*
 * 提供执行计时数据范围Enum相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.enums;

/* 执行联动的固定对象范围，只用于服务端调用，不接受任意表名或列名。 */
// 定义一组固定取值 ExecutionScopeEnum；提供执行计时数据范围Enum相关的共享实现或应用配置。
public enum ExecutionScopeEnum {
    // 准备任务（TASK），类型为 ExecutionScopeEnum；初始内容是新建 执行计时数据范围Enum（ExecutionScopeEnum） 实例。
    TASK,
    // 准备阶段（STAGE），类型为 ExecutionScopeEnum；初始内容是新建 执行计时数据范围Enum（ExecutionScopeEnum） 实例。
    STAGE,
    // 准备目标（GOAL），类型为 ExecutionScopeEnum；初始内容是新建 执行计时数据范围Enum（ExecutionScopeEnum） 实例。
    GOAL,
    // 准备周（WEEK），类型为 ExecutionScopeEnum；初始内容是新建 执行计时数据范围Enum（ExecutionScopeEnum） 实例。
    WEEK,
    // 准备日（DAY），类型为 ExecutionScopeEnum；初始内容是新建 执行计时数据范围Enum（ExecutionScopeEnum） 实例。
    DAY
}
