package com.asterway.identity.entity;

import java.time.Instant;

/** 登录审计实体，只允许追加。 */
public class LoginLogEntity {

    /** 数据库自增主键；对外作为字符串传输。 */
    private Long id;

    /** 逻辑引用账户，关联有效性由应用检查。 */
    private Long userId;

    /** 尝试登录时的用户名快照；未知用户也能记录。 */
    private String usernameSnapshot;

    /** SUCCESS 或 FAILURE。 */
    private String outcome;

    /** 结果或拒绝原因编码。 */
    private String reasonCode;

    /** 登录成功关联的 UUID 会话标识，不是 JWT。 */
    private String sessionId;

    /** 请求来源 IP；代理来源必须经过可信配置。 */
    private String ipAddress;

    /** 截断后的客户端描述，不参与授权。 */
    private String userAgent;

    /** 设备展示描述，不参与授权。 */
    private String deviceLabel;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsernameSnapshot() {
        return usernameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
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
