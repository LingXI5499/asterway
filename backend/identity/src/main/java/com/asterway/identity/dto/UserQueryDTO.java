package com.asterway.identity.dto;

import com.asterway.common.dto.PageQueryDTO;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 用户分页条件，后端始终附加操作者管理范围。 */
public class UserQueryDTO extends PageQueryDTO {

    @Size(max = 128)
    private String keyword;

    private Boolean enabled;

    @Pattern(regexp = "USER|ADMIN|SUPER_ADMIN")
    private String role;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
