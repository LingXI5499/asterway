/*
 * 规定成长助手工作区业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service;

// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;

/* 记忆、知识、规划与复盘公开入口；身份始终来自当前登录会话。 */
// 定义接口 AgentWorkspaceService；规定成长助手工作区业务服务可供其他代码调用的业务方法。
public interface AgentWorkspaceService {
    // 请求（request）方法：调用 request 完成请求处理；参数负责传入本次处理所需的数据。
    // 方法（method），使用 String 类型保存；可为 null，表示尚未提供。
    // resource，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    // 请求正文（body），使用 JsonNode 类型保存。
    JsonNode request(String method, String resource, Map<String, String> query, JsonNode body);

    // 上传（upload）方法：调用 upload 完成上传处理；参数负责传入本次处理所需的数据。
    // resource，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 Map<String, String> 类型保存。
    // mime类型（mimeType），使用 String 类型保存；可为 null，表示尚未提供。
    // 内容（content），使用 byte[] 类型保存。
    JsonNode upload(String resource, Map<String, String> query, String mimeType, byte[] content);
}
