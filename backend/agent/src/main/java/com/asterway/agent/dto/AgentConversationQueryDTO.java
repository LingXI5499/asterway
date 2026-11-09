/*
 * 声明成长助手对话查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Pattern;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 会话列表条件；排序由服务端固定，不接受前端字段。 */
// 定义类 AgentConversationQueryDTO；声明成长助手对话查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class AgentConversationQueryDTO extends PageQueryDTO {

    // 要求输入文字符合给定的正则规则。
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    @Pattern(regexp = "ACTIVE|CLOSED", message = "会话状态无效")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private String status;

    // 限制集合条数或文字长度，边界以注解中的数值为准。
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    @Size(max = 100, message = "关键词不能超过 100 字")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private String keyword;

    // 读取 状态（Status） 的值。
    public String getStatus() {
        // 把状态（status）交还给调用方，本方法到此结束。
        return status;
    }

    // 设置 状态（Status） 的值。
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStatus(String status) {
        // 把状态（status）保存到 this.status。
        this.status = status;
    }

    // 读取 Keyword 的值。
    public String getKeyword() {
        // 把keyword交还给调用方，本方法到此结束。
        return keyword;
    }

    // 设置 Keyword 的值。
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    public void setKeyword(String keyword) {
        // 把keyword保存到 this.keyword。
        this.keyword = keyword;
    }
}
