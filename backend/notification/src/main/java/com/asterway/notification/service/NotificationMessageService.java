package com.asterway.notification.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.notification.dto.NotificationHandleDTO;
import com.asterway.notification.dto.NotificationQueryDTO;
import com.asterway.notification.vo.NotificationMessageVO;
import com.asterway.notification.vo.NotificationUnreadVO;

/** 本人站内消息主链，不提供任意发送或后台群发接口。 */
public interface NotificationMessageService {
    /** 分页读取本人站内消息，已读和处理状态分开筛选。 */
    PageResultVO<NotificationMessageVO> page(NotificationQueryDTO query);

    /** 读取本人消息详情及来源当前版本，不隐式标记已读。 */
    NotificationMessageVO get(long id);

    /** 统计未读消息，排除失效来源。 */
    NotificationUnreadVO unread();

    /** 按消息版本标记首次已读，不改变业务处理状态。 */
    NotificationMessageVO markRead(long id, long version);

    /** 在用户锁事务中将当前有效未读消息标为已读。 */
    NotificationUnreadVO markAllRead();

    /** 确认目标完成或选择继续；确认须同时核对消息和目标版本。 */
    NotificationMessageVO handle(long id, boolean confirm, NotificationHandleDTO dto);
}
