/*
 * 用 Java 字段表示角色数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 角色持久化实体，内置角色本轮只维护展示信息和授权。 */
// 定义类 RoleEntity；用 Java 字段表示角色数据库记录，由 Mapper 负责读写。
public class RoleEntity {

    /* 数据库自增主键；对外作为字符串传输。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 全历史唯一业务编码；内置编码由应用保护。 */
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private String code;

    /* 界面展示名称。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* 用途说明。 */
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    private String description;

    /* 内置对象标志；核心对象不能由管理接口删除。 */
    // builtin，使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean builtin;

    /* 是否启用；不等同于逻辑删除。 */
    // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean enabled;

    /* 乐观锁版本；应用更新时比较并递增。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 创建时刻。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 最近修改时刻；由应用写入，无自动更新时间触发器。 */
    // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant updatedAt;

    /* 创建者账户标识；初始化时可空；无物理外键。 */
    // 创建时按（createdBy），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long createdBy;

    /* 最近修改者账户标识；初始化时可空；无物理外键。 */
    // 更新时按（updatedBy），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long updatedBy;

    /* 逻辑删除时刻；为空表示未删除。 */
    // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant deletedAt;

    // 读取 编号（Id） 的值。
    public Long getId() {
        // 把编号（id）交还给调用方，本方法到此结束。
        return id;
    }

    // 设置 编号（Id） 的值。
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setId(Long id) {
        // 把编号（id）保存到 this.id。
        this.id = id;
    }

    // 读取 编码（Code） 的值。
    public String getCode() {
        // 把编码（code）交还给调用方，本方法到此结束。
        return code;
    }

    // 设置 编码（Code） 的值。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    public void setCode(String code) {
        // 把编码（code）保存到 this.code。
        this.code = code;
    }

    // 读取 名称（Name） 的值。
    public String getName() {
        // 把名称（name）交还给调用方，本方法到此结束。
        return name;
    }

    // 设置 名称（Name） 的值。
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    public void setName(String name) {
        // 把名称（name）保存到 this.name。
        this.name = name;
    }

    // 读取 说明（Description） 的值。
    public String getDescription() {
        // 把说明（description）交还给调用方，本方法到此结束。
        return description;
    }

    // 设置 说明（Description） 的值。
    // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
    public void setDescription(String description) {
        // 把说明（description）保存到 this.description。
        this.description = description;
    }

    // 读取 Builtin 的值。
    public Boolean getBuiltin() {
        // 把builtin交还给调用方，本方法到此结束。
        return builtin;
    }

    // 设置 Builtin 的值。
    // builtin，使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setBuiltin(Boolean builtin) {
        // 把builtin保存到 this.builtin。
        this.builtin = builtin;
    }

    // 读取 是否启用（Enabled） 的值。
    public Boolean getEnabled() {
        // 把是否启用（enabled）交还给调用方，本方法到此结束。
        return enabled;
    }

    // 设置 是否启用（Enabled） 的值。
    // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setEnabled(Boolean enabled) {
        // 把是否启用（enabled）保存到 this.enabled。
        this.enabled = enabled;
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

    // 读取 创建时按（CreatedBy） 的值。
    public Long getCreatedBy() {
        // 把创建时按（createdBy）交还给调用方，本方法到此结束。
        return createdBy;
    }

    // 设置 创建时按（CreatedBy） 的值。
    // 创建时按（createdBy），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setCreatedBy(Long createdBy) {
        // 把创建时按（createdBy）保存到 this.createdBy。
        this.createdBy = createdBy;
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

    // 读取 删除标记At（DeletedAt） 的值。
    public Instant getDeletedAt() {
        // 把删除标记At（deletedAt）交还给调用方，本方法到此结束。
        return deletedAt;
    }

    // 设置 删除标记At（DeletedAt） 的值。
    // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setDeletedAt(Instant deletedAt) {
        // 把删除标记At（deletedAt）保存到 this.deletedAt。
        this.deletedAt = deletedAt;
    }
}
