/*
 * 用 Java 字段表示Setting数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 账户设置实体，只允许已确认的两个配置键。 */
// 定义类 SettingEntity；用 Java 字段表示Setting数据库记录，由 Mapper 负责读写。
public class SettingEntity {

    /* 仅 site_name 与 registration_enabled，不允许任意扩展配置键。 */
    // setting键（settingKey），使用 String 类型保存；可为 null，表示尚未提供。
    private String settingKey;

    /* 站点名称为 JSON 字符串，注册开关为 JSON 布尔值。 */
    // setting值（settingValue），使用 String 类型保存；可为 null，表示尚未提供。
    private String settingValue;

    /* 乐观锁版本；应用更新时比较并递增。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 创建时刻。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 最近修改时刻；由应用写入，无自动更新时间触发器。 */
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant updatedAt;

    /* 最近修改者账户标识；初始化时可空；无物理外键。 */
    // 更新时按（updatedBy），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long updatedBy;

    // 读取 Setting键（SettingKey） 的值。
    public String getSettingKey() {
        // 把setting键（settingKey）交还给调用方，本方法到此结束。
        return settingKey;
    }

    // 设置 Setting键（SettingKey） 的值。
    // setting键（settingKey），使用 String 类型保存；可为 null，表示尚未提供。
    public void setSettingKey(String settingKey) {
        // 把setting键（settingKey）保存到 this.settingKey。
        this.settingKey = settingKey;
    }

    // 读取 Setting值（SettingValue） 的值。
    public String getSettingValue() {
        // 把setting值（settingValue）交还给调用方，本方法到此结束。
        return settingValue;
    }

    // 设置 Setting值（SettingValue） 的值。
    // setting值（settingValue），使用 String 类型保存；可为 null，表示尚未提供。
    public void setSettingValue(String settingValue) {
        // 把setting值（settingValue）保存到 this.settingValue。
        this.settingValue = settingValue;
    }

    // 读取 版本号（Version） 的值。
    public Long getVersion() {
        // 把版本号（version）交还给调用方，本方法到此结束。
        return version;
    }

    // 设置 版本号（Version） 的值。
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setVersion(Long version) {
        // 把版本号（version）保存到 this.version。
        this.version = version;
    }

    // 读取 创建时At（CreatedAt） 的值。
    public Instant getCreatedAt() {
        // 把创建时At（createdAt）交还给调用方，本方法到此结束。
        return createdAt;
    }

    // 设置 创建时At（CreatedAt） 的值。
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setCreatedAt(Instant createdAt) {
        // 把创建时At（createdAt）保存到 this.createdAt。
        this.createdAt = createdAt;
    }

    // 读取 更新时At（UpdatedAt） 的值。
    public Instant getUpdatedAt() {
        // 把更新时At（updatedAt）交还给调用方，本方法到此结束。
        return updatedAt;
    }

    // 设置 更新时At（UpdatedAt） 的值。
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setUpdatedAt(Instant updatedAt) {
        // 把更新时At（updatedAt）保存到 this.updatedAt。
        this.updatedAt = updatedAt;
    }

    // 读取 更新时按（UpdatedBy） 的值。
    public Long getUpdatedBy() {
        // 把更新时按（updatedBy）交还给调用方，本方法到此结束。
        return updatedBy;
    }

    // 设置 更新时按（UpdatedBy） 的值。
    // 更新时按（updatedBy），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setUpdatedBy(Long updatedBy) {
        // 把更新时按（updatedBy）保存到 this.updatedBy。
        this.updatedBy = updatedBy;
    }
}
