/*
 * 声明目标Condition返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 通知 Service 的内部精确条件投影，不直接作为接口结果。 */
// 定义类 GoalConditionVO；声明目标Condition返回给页面的字段，不直接把数据库实体全部暴露出去。
public class GoalConditionVO {
    /* 目标标识。 */
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long goalId;

    /* 目标名称。 */
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    private String name;

    /* 当前状态。 */
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    private String status;

    /* 归档时间。 */
    // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant archivedAt;

    /* 逻辑删除时间。 */
    // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant deletedAt;

    /* 当前达标缓存。 */
    // 完成情况ConditionMet（completionConditionMet），使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean completionConditionMet;

    /* 当前轮次。 */
    // 完成情况轮（completionRound），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long completionRound;

    /* 目标版本。 */
    // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long version;

    /* 精确业务条件及账号资格，不使用舍入百分比。 */
    // qualifies，使用 Boolean 类型保存；可为 null，表示尚未提供。
    private Boolean qualifies;

    // 读取 目标编号（GoalId） 的值。
    public Long getGoalId() {
        // 把目标编号（goalId）交还给调用方，本方法到此结束。
        return goalId;
    }

    // 设置 目标编号（GoalId） 的值。
    // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setGoalId(Long goalId) {
        // 把目标编号（goalId）保存到 this.goalId。
        this.goalId = goalId;
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

    // 读取 状态（Status） 的值。
    public String getStatus() {
        // 把状态（status）交还给调用方，本方法到此结束。
        return status;
    }

    // 设置 状态（Status） 的值。
    // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
    public void setStatus(String status) {
        // 把状态（status）保存到 this.status。
        this.status = status;
    }

    // 读取 已归档At（ArchivedAt） 的值。
    public Instant getArchivedAt() {
        // 把已归档At（archivedAt）交还给调用方，本方法到此结束。
        return archivedAt;
    }

    // 设置 已归档At（ArchivedAt） 的值。
    // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setArchivedAt(Instant archivedAt) {
        // 把已归档At（archivedAt）保存到 this.archivedAt。
        this.archivedAt = archivedAt;
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

    // 读取 完成情况ConditionMet（CompletionConditionMet） 的值。
    public Boolean getCompletionConditionMet() {
        // 把完成情况ConditionMet（completionConditionMet）交还给调用方，本方法到此结束。
        return completionConditionMet;
    }

    // 设置 完成情况ConditionMet（CompletionConditionMet） 的值。
    // 完成情况ConditionMet（completionConditionMet），使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setCompletionConditionMet(Boolean completionConditionMet) {
        // 把完成情况ConditionMet（completionConditionMet）保存到 this.completionConditionMet。
        this.completionConditionMet = completionConditionMet;
    }

    // 读取 完成情况轮（CompletionRound） 的值。
    public Long getCompletionRound() {
        // 把完成情况轮（completionRound）交还给调用方，本方法到此结束。
        return completionRound;
    }

    // 设置 完成情况轮（CompletionRound） 的值。
    // 完成情况轮（completionRound），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setCompletionRound(Long completionRound) {
        // 把完成情况轮（completionRound）保存到 this.completionRound。
        this.completionRound = completionRound;
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

    // 读取 Qualifies 的值。
    public Boolean getQualifies() {
        // 把qualifies交还给调用方，本方法到此结束。
        return qualifies;
    }

    // 设置 Qualifies 的值。
    // qualifies，使用 Boolean 类型保存；可为 null，表示尚未提供。
    public void setQualifies(Boolean qualifies) {
        // 把qualifies保存到 this.qualifies。
        this.qualifies = qualifies;
    }
}
