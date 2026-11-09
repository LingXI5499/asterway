/*
 * 声明Dashboard任务查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Pattern;

/* 看板任务按分组独立分页；不指定分组时仍返回全部今日及本周未定日任务。 */
// 定义类 DashboardTaskQueryDTO；声明Dashboard任务查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class DashboardTaskQueryDTO extends PageQueryDTO {

    // 要求输入文字符合给定的正则规则。
    // section，使用 String 类型保存；可为 null，表示尚未提供。
    @Pattern(regexp = "TODAY|WEEK_UNDATED", message = "任务分组必须为 TODAY 或 WEEK_UNDATED")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private String section;

    // 读取 Section 的值。
    public String getSection() {
        // 把section交还给调用方，本方法到此结束。
        return section;
    }

    // 设置 Section 的值。
    // section，使用 String 类型保存；可为 null，表示尚未提供。
    public void setSection(String section) {
        // 把section保存到 this.section。
        this.section = section;
    }
}
