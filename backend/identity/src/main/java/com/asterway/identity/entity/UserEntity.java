package com.asterway.identity.entity;

import java.time.Instant;

/** 账户持久化实体，密码哈希仅用于服务端校验，禁止直接返回。 */
public class UserEntity {

    /** 数据库自增主键；对外作为字符串传输。 */
    private Long id;

    /** 登录名；大小写无关且全历史唯一，保留展示形式。 */
    private String username;

    /** Argon2id 编码摘要；数据库只做格式前缀检查，完整哈希与密码规则由应用验证。 */
    private String passwordHash;

    /** 显示昵称，可重复，注册未填时由应用取用户名。 */
    private String nickname;

    /** 是否启用；不等同于逻辑删除。 */
    private Boolean enabled;

    /** 初始或重置密码后要求修改密码。 */
    private Boolean mustChangePassword;

    /** 安全版本；密码与安全状态变化时递增。 */
    private Long authVersion;

    /** 有效 IANA 时区；应用通过可用时区列表验证。 */
    private String timeZone;

    /** 待生效时区；没有待切换时为空。 */
    private String pendingTimeZone;

    /** 待切换时刻，与待生效时区同时设置或清空。 */
    private Instant timeZoneEffectiveAt;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Long authVersion) {
        this.authVersion = authVersion;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getPendingTimeZone() {
        return pendingTimeZone;
    }

    public void setPendingTimeZone(String pendingTimeZone) {
        this.pendingTimeZone = pendingTimeZone;
    }

    public Instant getTimeZoneEffectiveAt() {
        return timeZoneEffectiveAt;
    }

    public void setTimeZoneEffectiveAt(Instant timeZoneEffectiveAt) {
        this.timeZoneEffectiveAt = timeZoneEffectiveAt;
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
