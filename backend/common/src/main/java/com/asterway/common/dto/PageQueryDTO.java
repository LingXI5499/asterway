/*
 * 声明当前页查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.dto;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入最大（Max），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Max;
// 引入最小（Min），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Min;

/* 通用分页请求；不接受前端拼接排序字段。 */
// 定义类 PageQueryDTO；声明当前页查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class PageQueryDTO {

    // 要求数值不小于注解给出的下限。
    // 准备当前页（page），类型为 int；初始内容是固定数值 1。
    @Min(1)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private int page = 1;

    // 要求数值不小于注解给出的下限。
    // 准备每页条数（size），类型为 int；初始内容是固定数值 20。
    @Min(1)
    // 要求数值不大于注解给出的上限。
    @Max(100)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    private int size = 20;

    // 读取 当前页（Page） 的值。
    public int getPage() {
        // 把当前页（page）交还给调用方，本方法到此结束。
        return page;
    }

    // 设置 当前页（Page） 的值。
    // 当前页（page），使用 int 类型保存。
    public void setPage(int page) {
        // 把当前页（page）保存到 this.page。
        this.page = page;
    }

    // 读取 长度或数量校验规则（Size） 的值。
    public int getSize() {
        // 把每页条数（size）交还给调用方，本方法到此结束。
        return size;
    }

    // 设置 长度或数量校验规则（Size） 的值。
    // 每页条数（size），使用 int 类型保存。
    public void setSize(int size) {
        // 把每页条数（size）保存到 this.size。
        this.size = size;
    }

    /* 在 Service 边界也限制分页，避免绕过 Controller 校验。 */
    // 分页时跳过的记录数（offset）方法：调用 offset 完成分页时跳过的记录数处理；参数负责传入本次处理所需的数据。
    public int offset() {
        // 检查条件 page < 1 || size < 1 || size > 100 || (long)(page - 1) * size > Integer.MAX_VALUE 是否成立；成立时执行下面的分支。
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("分页参数无效");
        }
        // 把按表达式计算得到的值：(page - 1) * size交还给调用方，本方法到此结束。
        return (page - 1) * size;
    }
}
