/*
 * 声明执行计时开始结果返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

/* 仅供 Controller 选择 201/200，HTTP data 仍直接返回 execution。 */
// 定义保存一组固定字段的 record ExecutionStartResultVO；声明执行计时开始结果返回给页面的字段，不直接把数据库实体全部暴露出去。
// 创建时（created），使用 boolean 类型保存。
// 执行计时（execution），使用 ExecutionSessionVO 类型保存。
public record ExecutionStartResultVO(boolean created, ExecutionSessionVO execution) {}
