package com.asterway.identity.entity;

import java.time.Instant;

/** 目录、页面与按钮实体，父节点为逻辑引用。 */
public class MenuEntity {

    /** 数据库自增主键；对外作为字符串传输。 */
    private Long id;

    /** 全历史唯一业务编码；内置编码由应用保护。 */
    private String code;

    /** 父菜单逻辑标识；根节点为空；更长循环及悬空父节点由应用拒绝。 */
    private Long parentId;

    /** 界面展示名称。 */
    private String name;

    /** DIRECTORY 目录、PAGE 页面、BUTTON 按钮。 */
    private String menuType;

    /** 目录或页面路径，必须以斜杠开头；按钮为空。 */
    private String routePath;

    /** 页面组件白名单键，不存任意文件路径。 */
    private String componentKey;

    /** 逻辑引用动作权限，关联有效性由应用检查。 */
    private Long permissionId;

    /** 图标键，由前端白名单解释。 */
    private String icon;

    /** 同级非负排序值。 */
    private Integer sortOrder;

    /** 导航展示状态，不代替接口授权。 */
    private Boolean visible;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getComponentKey() {
        return componentKey;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
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
