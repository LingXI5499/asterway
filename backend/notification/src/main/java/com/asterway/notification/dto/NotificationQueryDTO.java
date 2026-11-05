package com.asterway.notification.dto;

import com.asterway.common.dto.PageQueryDTO;

/** 本人的消息查询，已读状态与处理状态独立筛选。 */
public class NotificationQueryDTO extends PageQueryDTO {
    private Boolean unread;
    private String handleStatus;

    public Boolean getUnread() {
        return unread;
    }

    public void setUnread(Boolean unread) {
        this.unread = unread;
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus;
    }

    public int getOffset() {
        return offset();
    }
}
