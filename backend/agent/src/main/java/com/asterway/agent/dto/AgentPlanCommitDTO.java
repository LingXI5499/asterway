/*
 * 声明成长助手方案提交回执的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Pattern;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;

/* Python 规划引擎提交的已确认方案；只接受普通用户本人范围。 */
// 定义保存一组固定字段的 record AgentPlanCommitDTO；声明成长助手方案提交回执的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AgentPlanCommitDTO(
    // 要求该值必须存在，不能为 null。
    // 要求数值大于零。
    // 工作流运行编号（runId），使用 Long 类型保存；可为 null，表示尚未提供。
    @NotNull @Positive Long runId,
    // 要求该值必须存在，不能为 null。
    // 要求数值大于零。
    // 方案版本号编号（planVersionId），使用 Long 类型保存；可为 null，表示尚未提供。
    @NotNull @Positive Long planVersionId,
    // 要求文字既不为空，也不能只有空白。
    // 要求输入文字符合给定的正则规则。
    // 防止重复提交的唯一键（idempotencyKey），使用 String 类型保存；可为 null，表示尚未提供。
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "幂等键必须是 sha256 十六进制")
        // 继续传入或组合表达式 String idempotencyKey 的结果，完成上面尚未结束的调用或声明。
        String idempotencyKey,
    // 要求文字既不为空，也不能只有空白。
    // 要求输入文字符合给定的正则规则。
    // 请求内容的校验摘要（requestHash），使用 String 类型保存；可为 null，表示尚未提供。
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "请求摘要必须是 sha256 十六进制")
        // 继续传入或组合表达式 String requestHash 的结果，完成上面尚未结束的调用或声明。
        String requestHash,
    // 要求该值必须存在，不能为 null。
    // 确认时的数据快照（snapshot），使用 JsonNode 类型保存。
    @NotNull JsonNode snapshot) {}
