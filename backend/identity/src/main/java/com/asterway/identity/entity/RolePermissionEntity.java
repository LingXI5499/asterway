package com.asterway.identity.entity;

import java.time.Instant;

/** 角色动作权限关联实体，由角色授权事务维护。 */
public class RolePermissionEntity {

    /** 逻辑引用角色，关联有效性由应用检查。 */
    private Long roleId;

    /** 逻辑引用动作权限，关联有效性由应用检查。 */
    private Long permissionId;

    /** 创建时刻。 */
    private Instant createdAt;

    /** 创建者账户标识；初始化时可空；无物理外键。 */
    private Long createdBy;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
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
