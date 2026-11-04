package com.asterway.identity.dto;

import com.asterway.common.dto.PageQueryDTO;
import jakarta.validation.constraints.Positive;

/** 审计分页条件，不接受客户端指定操作者。 */
public class LogQueryDTO extends PageQueryDTO {

    @Positive
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
