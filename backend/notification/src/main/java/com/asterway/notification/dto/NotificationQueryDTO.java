/*
 * 声明通知查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.dto;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;

/* 本人的消息查询，已读状态与处理状态独立筛选。 */
// 定义类 NotificationQueryDTO；声明通知查询条件的请求字段和校验条件；接口收到数据后按这些限制检查。
public class NotificationQueryDTO extends PageQueryDTO {
    // unread，使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean unread;
    // handle状态（handleStatus），使用 String 类型保存；可为 null，表示尚未提供。
    private String handleStatus;

    // 读取 Unread 的值。
    public Boolean getUnread() {
        // 把unread交还给调用方，本方法到此结束。
        return unread;
    }

    // 设置 Unread 的值。
    // unread，使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setUnread(Boolean unread) {
        // 把unread保存到 this.unread。
        this.unread = unread;
    }

    // 读取 Handle状态（HandleStatus） 的值。
    public String getHandleStatus() {
        // 把handle状态（handleStatus）交还给调用方，本方法到此结束。
        return handleStatus;
    }

    // 设置 Handle状态（HandleStatus） 的值。
    // handle状态（handleStatus），使用 String 类型保存；可为 null，表示尚未提供。
    public void setHandleStatus(String handleStatus) {
        // 把handle状态（handleStatus）保存到 this.handleStatus。
        this.handleStatus = handleStatus;
    }

    // 读取 分页时跳过的记录数（Offset） 的值。
    public int getOffset() {
        // 把调用 offset 完成分页时跳过的记录数处理交还给调用方，本方法到此结束。
        return offset();
    }
}
