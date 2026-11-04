package com.asterway.identity.entity;

import java.time.Instant;

/** 角色持久化实体，内置角色本轮只维护展示信息和授权。 */
public class RoleEntity {

    /** 数据库自增主键；对外作为字符串传输。 */
    private Long id;

    /** 全历史唯一业务编码；内置编码由应用保护。 */
    private String code;

    /** 界面展示名称。 */
    private String name;

    /** 用途说明。 */
    private String description;

    /** 内置对象标志；核心对象不能由管理接口删除。 */
    private Boolean builtin;

    /** 是否启用；不等同于逻辑删除。 */
    private Boolean enabled;

    /** 乐观锁版本；应用更新时比较并递增。 */
    private Long version;

    /** 创建时刻。 */
    private Instant createdAt;

    /** 最近修改时刻；由应用写入，无自动更新时间触发器。 */
    private Instant updatedAt;

    /** 创建者账户标识；初始化时可空；无物理外键。 */
    private Long createdBy;

    /** 最近修改者账户标识；初始化时可空；无物理外键。 */
    private Long updatedBy;

    /** 逻辑删除时刻；为空表示未删除。 */
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Boolean builtin) {
        this.builtin = builtin;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
