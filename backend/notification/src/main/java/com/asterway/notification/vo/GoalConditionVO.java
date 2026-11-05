package com.asterway.notification.vo;

import java.time.Instant;

/** 通知 Service 的内部精确条件投影，不直接作为接口结果。 */
public class GoalConditionVO {
    /** 目标标识。 */
    private Long goalId;

    /** 目标名称。 */
    private String name;

    /** 当前状态。 */
    private String status;

    /** 归档时间。 */
    private Instant archivedAt;

    /** 逻辑删除时间。 */
    private Instant deletedAt;

    /** 当前达标缓存。 */
    private Boolean completionConditionMet;

    /** 当前轮次。 */
    private Long completionRound;

    /** 目标版本。 */
    private Long version;

    /** 精确业务条件及账号资格，不使用舍入百分比。 */
    private Boolean qualifies;

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Boolean getCompletionConditionMet() {
        return completionConditionMet;
    }

    public void setCompletionConditionMet(Boolean completionConditionMet) {
        this.completionConditionMet = completionConditionMet;
    }

    public Long getCompletionRound() {
        return completionRound;
    }

    public void setCompletionRound(Long completionRound) {
        this.completionRound = completionRound;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getQualifies() {
        return qualifies;
    }

    public void setQualifies(Boolean qualifies) {
        this.qualifies = qualifies;
    }
}
