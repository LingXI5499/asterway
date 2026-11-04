package com.asterway.identity.dto;

import com.asterway.common.dto.PageQueryDTO;
import jakarta.validation.constraints.Size;

/** 权限列表条件，数量较少时统一在 Service 过滤分页。 */
public class PermissionQueryDTO extends PageQueryDTO {

    @Size(max = 128)
    private String keyword;

    private Boolean enabled;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
