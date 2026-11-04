package com.asterway.identity.entity;

import java.time.Instant;

/** 账户设置实体，只允许已确认的两个配置键。 */
public class SettingEntity {

    /** 仅 site_name 与 registration_enabled，不允许任意扩展配置键。 */
    private String settingKey;

    /** 站点名称为 JSON 字符串，注册开关为 JSON 布尔值。 */
    private String settingValue;

    /** 乐观锁版本；应用更新时比较并递增。 */
    private Long version;

    /** 创建时刻。 */
    private Instant createdAt;

    /** 最近修改时刻；由应用写入，无自动更新时间触发器。 */
    private Instant updatedAt;

    /** 最近修改者账户标识；初始化时可空；无物理外键。 */
    private Long updatedBy;

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
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

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}
