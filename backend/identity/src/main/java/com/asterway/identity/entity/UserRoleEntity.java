package com.asterway.identity.entity;

import java.time.Instant;

/** 用户角色关联实体，不单独暴露无意义的关联表增删接口。 */
public class UserRoleEntity {

    /** 逻辑引用账户，关联有效性由应用检查。 */
    private Long userId;

    /** 逻辑引用角色，关联有效性由应用检查。 */
    private Long roleId;

    /** 创建时刻。 */
    private Instant createdAt;

    /** 创建者账户标识；初始化时可空；无物理外键。 */
    private Long createdBy;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
