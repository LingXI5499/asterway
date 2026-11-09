/*
 * 规定通知消息业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入通知Handle（NotificationHandleDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.NotificationHandleDTO;
// 引入通知查询条件（NotificationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.NotificationQueryDTO;
// 引入通知消息（NotificationMessageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.NotificationMessageVO;
// 引入通知Unread（NotificationUnreadVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.NotificationUnreadVO;

/* 本人站内消息主链，不提供任意发送或后台群发接口。 */
// 定义接口 NotificationMessageService；规定通知消息业务服务可供其他代码调用的业务方法。
public interface NotificationMessageService {
    /* 分页读取本人站内消息，已读和处理状态分开筛选。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 NotificationQueryDTO 类型保存。
    PageResultVO<NotificationMessageVO> page(NotificationQueryDTO query);

    /* 读取本人消息详情及来源当前版本，不隐式标记已读。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    NotificationMessageVO get(long id);

    /* 统计未读消息，排除失效来源。 */
    // unread方法：调用 unread 完成unread处理；参数负责传入本次处理所需的数据。
    NotificationUnreadVO unread();

    /* 按消息版本标记首次已读，不改变业务处理状态。 */
    // mark读取（markRead）方法：调用 markRead 完成mark读取处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    NotificationMessageVO markRead(long id, long version);

    /* 在用户锁事务中将当前有效未读消息标为已读。 */
    // mark全部读取（markAllRead）方法：调用 markAllRead 完成mark全部读取处理；参数负责传入本次处理所需的数据。
    NotificationUnreadVO markAllRead();

    /* 确认目标完成或选择继续；确认须同时核对消息和目标版本。 */
    // handle方法：调用 handle 完成handle处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 确认（confirm），使用 boolean 类型保存。
    // 请求参数对象（dto），使用 NotificationHandleDTO 类型保存。
    NotificationMessageVO handle(long id, boolean confirm, NotificationHandleDTO dto);
}
