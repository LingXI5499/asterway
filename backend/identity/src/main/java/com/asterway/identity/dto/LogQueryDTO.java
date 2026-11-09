/*
 * 声明日志查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;

/* 审计分页条件，不接受客户端指定操作者。 */
// 定义类 LogQueryDTO；声明日志查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class LogQueryDTO extends PageQueryDTO {

    // 要求数值大于零。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    @Positive
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private Long userId;

    // 读取 用户编号（UserId） 的值。
    public Long getUserId() {
        // 把用户编号（userId）交还给调用方，本方法到此结束。
        return userId;
    }

    // 设置 用户编号（UserId） 的值。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setUserId(Long userId) {
        // 把用户编号（userId）保存到 this.userId。
        this.userId = userId;
    }
}
