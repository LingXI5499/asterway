/*
 * 声明成长助手方案提交回执返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 方案提交结果；重复请求返回同一份已保存结果。 */
// 定义保存一组固定字段的 record AgentPlanCommitVO；声明成长助手方案提交回执返回给页面的字段，不直接把数据库实体全部暴露出去。
// 提交回执编号（commitId），使用 String 类型保存；可为 null，表示尚未提供。
// 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
// 结果（result），使用 JsonNode 类型保存。
// warnings，使用 List<String> 类型保存。
public record AgentPlanCommitVO(String commitId, String status, JsonNode result, List<String> warnings) {}
