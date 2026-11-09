/*
 * 用 Java 字段表示菜单数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 目录、页面与按钮实体，父节点为逻辑引用。 */
// 定义类 MenuEntity；用 Java 字段表示菜单数据库记录，由 Mapper 负责读写。
public class MenuEntity {

    /* 数据库自增主键；对外作为字符串传输。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 全历史唯一业务编码；内置编码由应用保护。 */
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private String code;

    /* 父菜单逻辑标识；根节点为空；更长循环及悬空父节点由应用拒绝。 */
    // 上级编号（parentId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long parentId;

    /* 界面展示名称。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* DIRECTORY 目录、PAGE 页面、BUTTON 按钮。 */
    // 菜单类型（menuType），使用 String 类型保存；可为 null，表示尚未提供。
    private String menuType;

    /* 目录或页面路径，必须以斜杠开头；按钮为空。 */
    // route路径（routePath），使用 String 类型保存；可为 null，表示尚未提供。
    private String routePath;

    /* 页面组件白名单键，不存任意文件路径。 */
    // component键（componentKey），使用 String 类型保存；可为 null，表示尚未提供。
    private String componentKey;

    /* 逻辑引用动作权限，关联有效性由应用检查。 */
    // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long permissionId;

    /* 图标键，由前端白名单解释。 */
    // icon，使用 String 类型保存；可为 null，表示尚未提供。
    private String icon;

    /* 同级非负排序值。 */
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    private Integer sortOrder;

    /* 导航展示状态，不代替接口授权。 */
    // 是否显示（visible），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean visible;

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

    // 读取 上级编号（ParentId） 的值。
    public Long getParentId() {
        // 把上级编号（parentId）交还给调用方，本方法到此结束。
        return parentId;
    }

    // 设置 上级编号（ParentId） 的值。
    // 上级编号（parentId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setParentId(Long parentId) {
        // 把上级编号（parentId）保存到 this.parentId。
        this.parentId = parentId;
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

    // 读取 菜单类型（MenuType） 的值。
    public String getMenuType() {
        // 把菜单类型（menuType）交还给调用方，本方法到此结束。
        return menuType;
    }

    // 设置 菜单类型（MenuType） 的值。
    // 菜单类型（menuType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setMenuType(String menuType) {
        // 把菜单类型（menuType）保存到 this.menuType。
        this.menuType = menuType;
    }

    // 读取 Route路径（RoutePath） 的值。
    public String getRoutePath() {
        // 把route路径（routePath）交还给调用方，本方法到此结束。
        return routePath;
    }

    // 设置 Route路径（RoutePath） 的值。
    // route路径（routePath），使用 String 类型保存；可为 null，表示尚未提供。
    public void setRoutePath(String routePath) {
        // 把route路径（routePath）保存到 this.routePath。
        this.routePath = routePath;
    }

    // 读取 Component键（ComponentKey） 的值。
    public String getComponentKey() {
        // 把component键（componentKey）交还给调用方，本方法到此结束。
        return componentKey;
    }

    // 设置 Component键（ComponentKey） 的值。
    // component键（componentKey），使用 String 类型保存；可为 null，表示尚未提供。
    public void setComponentKey(String componentKey) {
        // 把component键（componentKey）保存到 this.componentKey。
        this.componentKey = componentKey;
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

    // 读取 Icon 的值。
    public String getIcon() {
        // 把icon交还给调用方，本方法到此结束。
        return icon;
    }

    // 设置 Icon 的值。
    // icon，使用 String 类型保存；可为 null，表示尚未提供。
    public void setIcon(String icon) {
        // 把icon保存到 this.icon。
        this.icon = icon;
    }

    // 读取 排序顺序（SortOrder） 的值。
    public Integer getSortOrder() {
        // 把排序顺序（sortOrder）交还给调用方，本方法到此结束。
        return sortOrder;
    }

    // 设置 排序顺序（SortOrder） 的值。
    // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
    public void setSortOrder(Integer sortOrder) {
        // 把排序顺序（sortOrder）保存到 this.sortOrder。
        this.sortOrder = sortOrder;
    }

    // 读取 是否显示（Visible） 的值。
    public Boolean getVisible() {
        // 把是否显示（visible）交还给调用方，本方法到此结束。
        return visible;
    }

    // 设置 是否显示（Visible） 的值。
    // 是否显示（visible），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setVisible(Boolean visible) {
        // 把是否显示（visible）保存到 this.visible。
        this.visible = visible;
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
