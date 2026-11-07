package com.asterway.agent.dto;

import com.asterway.common.dto.PageQueryDTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 会话列表条件；排序由服务端固定，不接受前端字段。 */
public class AgentConversationQueryDTO extends PageQueryDTO {

    @Pattern(regexp = "ACTIVE|CLOSED", message = "会话状态无效")
    private String status;

    @Size(max = 100, message = "关键词不能超过 100 字")
    private String keyword;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
