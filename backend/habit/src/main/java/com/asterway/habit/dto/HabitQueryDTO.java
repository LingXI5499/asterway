/*
 * 声明习惯查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

/* 本人列表筛选，不接收所属用户标识。 */
// 定义类 HabitQueryDTO；声明习惯查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class HabitQueryDTO extends PageQueryDTO {
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    private String keyword;
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;
    // 分类编号（categoryId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long categoryId;
    // 已归档（archived），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean archived;

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

    // 读取 分类编号（CategoryId） 的值。
    public Long getCategoryId() {
        // 把分类编号（categoryId）交还给调用方，本方法到此结束。
        return categoryId;
    }

    // 设置 分类编号（CategoryId） 的值。
    // 分类编号（categoryId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setCategoryId(Long categoryId) {
        // 把分类编号（categoryId）保存到 this.categoryId。
        this.categoryId = categoryId;
    }

    // 读取 已归档（Archived） 的值。
    public Boolean getArchived() {
        // 把已归档（archived）交还给调用方，本方法到此结束。
        return archived;
    }

    // 设置 已归档（Archived） 的值。
    // 已归档（archived），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setArchived(Boolean archived) {
        // 把已归档（archived）保存到 this.archived。
        this.archived = archived;
    }

    // 读取 分页时跳过的记录数（Offset） 的值。
    public int getOffset() {
        // 把调用 offset 完成分页时跳过的记录数处理交还给调用方，本方法到此结束。
        return offset();
    }
}
