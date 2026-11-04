package com.asterway.identity.entity;

import java.time.Instant;

/** 操作审计实体，变更摘要必须先脱敏。 */
public class OperationLogEntity {

    /** 数据库自增主键；对外作为字符串传输。 */
    private Long id;

    /** 操作者账户标识，匿名失败场景可空。 */
    private Long actorId;

    /** 操作时用户名快照。 */
    private String actorUsername;

    /** 被管理账户标识，用于管理员日志范围筛选。 */
    private Long targetUserId;

    /** 被操作资源类别。 */
    private String resourceType;

    /** 被操作资源标识快照。 */
    private String resourceId;

    /** 动作权限或审计动作编码。 */
    private String actionCode;

    /** SUCCESS 或 FAILURE。 */
    private String outcome;

    /** 结果或拒绝原因编码。 */
    private String reasonCode;

    /** 仅安全字段的 JSON 对象，应用负责拒绝密码、JWT 和验证码答案。 */
    private String changeSummary;

    /** 请求来源 IP；代理来源必须经过可信配置。 */
    private String ipAddress;

    /** 请求追踪标识，不存认证凭据。 */
    private String traceId;

    /** 事件发生时刻。 */
    private Instant occurredAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
