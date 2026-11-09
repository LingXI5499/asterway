/*
 * 用 Java 字段表示用户数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 账户持久化实体，密码哈希仅用于服务端校验，禁止直接返回。 */
// 定义类 UserEntity；用 Java 字段表示用户数据库记录，由 Mapper 负责读写。
public class UserEntity {

    /* 数据库自增主键；对外作为字符串传输。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 登录名；大小写无关且全历史唯一，保留展示形式。 */
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    private String username;

    /* Argon2id 编码摘要；数据库只做格式前缀检查，完整哈希与密码规则由应用验证。 */
    // 密码摘要值（passwordHash），使用 String 类型保存；可为 null，表示尚未提供。
    private String passwordHash;

    /* 显示昵称，可重复，注册未填时由应用取用户名。 */
    // 昵称（nickname），使用 String 类型保存；可为 null，表示尚未提供。
    private String nickname;

    /* 是否启用；不等同于逻辑删除。 */
    // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean enabled;

    /* 初始或重置密码后要求修改密码。 */
    // 首次登录是否必须改密码（mustChangePassword），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean mustChangePassword;

    /* 安全版本；密码与安全状态变化时递增。 */
    // 使旧登录失效的认证版本号（authVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long authVersion;

    /* 有效 IANA 时区；应用通过可用时区列表验证。 */
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    private String timeZone;

    /* 待生效时区；没有待切换时为空。 */
    // 等待处理时间时区（pendingTimeZone），使用 String 类型保存；可为 null，表示尚未提供。
    private String pendingTimeZone;

    /* 待切换时刻，与待生效时区同时设置或清空。 */
    // 时间时区生效At（timeZoneEffectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant timeZoneEffectiveAt;

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

    // 读取 用户名（Username） 的值。
    public String getUsername() {
        // 把用户名（username）交还给调用方，本方法到此结束。
        return username;
    }

    // 设置 用户名（Username） 的值。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    public void setUsername(String username) {
        // 把用户名（username）保存到 this.username。
        this.username = username;
    }

    // 读取 密码摘要值（PasswordHash） 的值。
    public String getPasswordHash() {
        // 把密码摘要值（passwordHash）交还给调用方，本方法到此结束。
        return passwordHash;
    }

    // 设置 密码摘要值（PasswordHash） 的值。
    // 密码摘要值（passwordHash），使用 String 类型保存；可为 null，表示尚未提供。
    public void setPasswordHash(String passwordHash) {
        // 把密码摘要值（passwordHash）保存到 this.passwordHash。
        this.passwordHash = passwordHash;
    }

    // 读取 昵称（Nickname） 的值。
    public String getNickname() {
        // 把昵称（nickname）交还给调用方，本方法到此结束。
        return nickname;
    }

    // 设置 昵称（Nickname） 的值。
    // 昵称（nickname），使用 String 类型保存；可为 null，表示尚未提供。
    public void setNickname(String nickname) {
        // 把昵称（nickname）保存到 this.nickname。
        this.nickname = nickname;
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

    // 读取 Must修改密码（MustChangePassword） 的值。
    public Boolean getMustChangePassword() {
        // 把首次登录是否必须改密码（mustChangePassword）交还给调用方，本方法到此结束。
        return mustChangePassword;
    }

    // 设置 Must修改密码（MustChangePassword） 的值。
    // 首次登录是否必须改密码（mustChangePassword），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setMustChangePassword(Boolean mustChangePassword) {
        // 把首次登录是否必须改密码（mustChangePassword）保存到 this.mustChangePassword。
        this.mustChangePassword = mustChangePassword;
    }

    // 读取 认证版本号（AuthVersion） 的值。
    public Long getAuthVersion() {
        // 把使旧登录失效的认证版本号（authVersion）交还给调用方，本方法到此结束。
        return authVersion;
    }

    // 设置 认证版本号（AuthVersion） 的值。
    // 使旧登录失效的认证版本号（authVersion），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setAuthVersion(Long authVersion) {
        // 把使旧登录失效的认证版本号（authVersion）保存到 this.authVersion。
        this.authVersion = authVersion;
    }

    // 读取 时间时区（TimeZone） 的值。
    public String getTimeZone() {
        // 把用户使用的时区（timeZone）交还给调用方，本方法到此结束。
        return timeZone;
    }

    // 设置 时间时区（TimeZone） 的值。
    // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTimeZone(String timeZone) {
        // 把用户使用的时区（timeZone）保存到 this.timeZone。
        this.timeZone = timeZone;
    }

    // 读取 等待处理时间时区（PendingTimeZone） 的值。
    public String getPendingTimeZone() {
        // 把等待处理时间时区（pendingTimeZone）交还给调用方，本方法到此结束。
        return pendingTimeZone;
    }

    // 设置 等待处理时间时区（PendingTimeZone） 的值。
    // 等待处理时间时区（pendingTimeZone），使用 String 类型保存；可为 null，表示尚未提供。
    public void setPendingTimeZone(String pendingTimeZone) {
        // 把等待处理时间时区（pendingTimeZone）保存到 this.pendingTimeZone。
        this.pendingTimeZone = pendingTimeZone;
    }

    // 读取 时间时区生效At（TimeZoneEffectiveAt） 的值。
    public Instant getTimeZoneEffectiveAt() {
        // 把时间时区生效At（timeZoneEffectiveAt）交还给调用方，本方法到此结束。
        return timeZoneEffectiveAt;
    }

    // 设置 时间时区生效At（TimeZoneEffectiveAt） 的值。
    // 时间时区生效At（timeZoneEffectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setTimeZoneEffectiveAt(Instant timeZoneEffectiveAt) {
        // 把时间时区生效At（timeZoneEffectiveAt）保存到 this.timeZoneEffectiveAt。
        this.timeZoneEffectiveAt = timeZoneEffectiveAt;
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
