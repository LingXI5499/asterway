/*
 * 声明权限查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 权限列表条件，数量较少时统一在 Service 过滤分页。 */
// 定义类 PermissionQueryDTO；声明权限查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class PermissionQueryDTO extends PageQueryDTO {

    // 限制集合条数或文字长度，边界以注解中的数值为准。
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    @Size(max = 128)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private String keyword;

    // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean enabled;

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

    // 读取 是否启用（Enabled） 的值。
    public Boolean getEnabled() {
        // 把是否启用（enabled）交还给调用方，本方法到此结束。
        return enabled;
    }

    // 设置 是否启用（Enabled） 的值。
    // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setEnabled(Boolean enabled) {
        // 把是否启用（enabled）保存到 this.enabled。
        this.enabled = enabled;
    }
}
