/*
 * 用 Java 字段表示角色权限数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 角色动作权限关联实体，由角色授权事务维护。 */
// 定义类 RolePermissionEntity；用 Java 字段表示角色权限数据库记录，由 Mapper 负责读写。
public class RolePermissionEntity {

    /* 逻辑引用角色，关联有效性由应用检查。 */
    // 角色编号（roleId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long roleId;

    /* 逻辑引用动作权限，关联有效性由应用检查。 */
    // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long permissionId;

    /* 创建时刻。 */
    // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant createdAt;

    /* 创建者账户标识；初始化时可空；无物理外键。 */
    // 创建时按（createdBy），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long createdBy;

    // 读取 角色编号（RoleId） 的值。
    public Long getRoleId() {
        // 把角色编号（roleId）交还给调用方，本方法到此结束。
        return roleId;
    }

    // 设置 角色编号（RoleId） 的值。
    // 角色编号（roleId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setRoleId(Long roleId) {
        // 把角色编号（roleId）保存到 this.roleId。
        this.roleId = roleId;
    }

    // 读取 权限编号（PermissionId） 的值。
    public Long getPermissionId() {
        // 把权限编号（permissionId）交还给调用方，本方法到此结束。
        return permissionId;
    }

    // 设置 权限编号（PermissionId） 的值。
    // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setPermissionId(Long permissionId) {
        // 把权限编号（permissionId）保存到 this.permissionId。
        this.permissionId = permissionId;
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
}
