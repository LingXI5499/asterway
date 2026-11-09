/*
 * 集中处理方案写入提交数据Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入对象节点（ObjectNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.node.ObjectNode;

/* 中立命令到模块 DTO 的受控转换；模型逻辑引用不会混入业务 DTO。 */
// 定义类 PlanWritePayloadUtils；集中处理方案写入提交数据Utils的重复规则，避免各业务模块分别实现。
public final class PlanWritePayloadUtils {
    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private PlanWritePayloadUtils() {}

    // 保存（saveDTO）方法：保存 请求参数对象（DTO）；参数负责传入本次处理所需的数据。
    // 来源（source），使用 JsonNode 类型保存。
    // 类型（type），使用 Class<T> 类型保存。
    // 访问数据库的 Mapper（mapper），使用 ObjectMapper 类型保存。
    public static <T> T saveDTO(JsonNode source, Class<T> type, ObjectMapper mapper) {
        // 准备fields，类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
        ObjectNode fields = mapper.createObjectNode();
        // component，使用 null 类型保存。
        // 逐次执行循环：for (/*missing* / component : type.getRecordComponents())；每轮处理一条数据或推进一次检查。
        for (var component : type.getRecordComponents()) {
            // 准备名称（name），类型为 String；初始内容是读取 名称（Name） 的值。
            String name = component.getName();
            // 准备输入（input），类型为 String；初始内容是名称（name）。
            String input = name;
            // 检查条件 !source.has(input) 是否成立；成立时执行下面的分支。
            if (!source.has(input)) {
                // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
                // 把表达式 switch (name) { case "name" -> yield "title"; case "expectedEndDate", "endDate" -> yield "dueDate"; default -> yield nam… 的结果保存到 input。
                input = switch (name) {
                    // 匹配这一种取值时使用本分支：case "name"。
                    case "name" -> "title";
                    // 匹配这一种取值时使用本分支：case "expectedEndDate", "endDate"。
                    case "expectedEndDate", "endDate" -> "dueDate";
                    // 匹配这一种取值时使用本分支：default。
                    default -> name;
                };
            }
            // 检查条件 source.has(input) 是否成立；成立时执行下面的分支。
            if (source.has(input)) {
                // 调用 fields.set 完成设置处理。
                fields.set(name, source.get(input));
            }
        }
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把调用 mapper.convertValue 完成convert值处理交还给调用方，本方法到此结束。
            return mapper.convertValue(fields, type);
        // 错误（error），使用 IllegalArgumentException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IllegalArgumentException error) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("方案中的业务字段格式不正确");
        }
    }
}
