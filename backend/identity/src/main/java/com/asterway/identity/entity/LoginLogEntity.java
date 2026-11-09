/*
 * 用 Java 字段表示登录日志数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 登录审计实体，只允许追加。 */
// 定义类 LoginLogEntity；用 Java 字段表示登录日志数据库记录，由 Mapper 负责读写。
public class LoginLogEntity {

    /* 数据库自增主键；对外作为字符串传输。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 逻辑引用账户，关联有效性由应用检查。 */
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long userId;

    /* 尝试登录时的用户名快照；未知用户也能记录。 */
    // 用户名确认时的数据快照（usernameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    private String usernameSnapshot;

    /* SUCCESS 或 FAILURE。 */
    // outcome，使用 String 类型保存；可为 null，表示尚未提供。
    private String outcome;

    /* 结果或拒绝原因编码。 */
    // 原因编码（reasonCode），使用 String 类型保存；可为 null，表示尚未提供。
    private String reasonCode;

    /* 登录成功关联的 UUID 会话标识，不是 JWT。 */
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    private String sessionId;

    /* 请求来源 IP；代理来源必须经过可信配置。 */
    // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
    private String ipAddress;

    /* 截断后的客户端描述，不参与授权。 */
    // 用户成长助手（userAgent），使用 String 类型保存；可为 null，表示尚未提供。
    private String userAgent;

    /* 设备展示描述，不参与授权。 */
    // 设备显示名称（deviceLabel），使用 String 类型保存；可为 null，表示尚未提供。
    private String deviceLabel;

    /* 请求追踪标识，不存认证凭据。 */
    // 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
    private String traceId;

    /* 事件发生时刻。 */
    // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    private Instant occurredAt;

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

    // 读取 用户编号（UserId） 的值。
    public Long getUserId() {
        // 把用户编号（userId）交还给调用方，本方法到此结束。
        return userId;
    }

    // 设置 用户编号（UserId） 的值。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setUserId(Long userId) {
        // 把用户编号（userId）保存到 this.userId。
        this.userId = userId;
    }

    // 读取 用户名确认时的数据快照（UsernameSnapshot） 的值。
    public String getUsernameSnapshot() {
        // 把用户名确认时的数据快照（usernameSnapshot）交还给调用方，本方法到此结束。
        return usernameSnapshot;
    }

    // 设置 用户名确认时的数据快照（UsernameSnapshot） 的值。
    // 用户名确认时的数据快照（usernameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
    public void setUsernameSnapshot(String usernameSnapshot) {
        // 把用户名确认时的数据快照（usernameSnapshot）保存到 this.usernameSnapshot。
        this.usernameSnapshot = usernameSnapshot;
    }

    // 读取 Outcome 的值。
    public String getOutcome() {
        // 把outcome交还给调用方，本方法到此结束。
        return outcome;
    }

    // 设置 Outcome 的值。
    // outcome，使用 String 类型保存；可为 null，表示尚未提供。
    public void setOutcome(String outcome) {
        // 把outcome保存到 this.outcome。
        this.outcome = outcome;
    }

    // 读取 原因编码（ReasonCode） 的值。
    public String getReasonCode() {
        // 把原因编码（reasonCode）交还给调用方，本方法到此结束。
        return reasonCode;
    }

    // 设置 原因编码（ReasonCode） 的值。
    // 原因编码（reasonCode），使用 String 类型保存；可为 null，表示尚未提供。
    public void setReasonCode(String reasonCode) {
        // 把原因编码（reasonCode）保存到 this.reasonCode。
        this.reasonCode = reasonCode;
    }

    // 读取 会话编号（SessionId） 的值。
    public String getSessionId() {
        // 把会话编号（sessionId）交还给调用方，本方法到此结束。
        return sessionId;
    }

    // 设置 会话编号（SessionId） 的值。
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setSessionId(String sessionId) {
        // 把会话编号（sessionId）保存到 this.sessionId。
        this.sessionId = sessionId;
    }

    // 读取 IpAddress 的值。
    public String getIpAddress() {
        // 把ipAddress交还给调用方，本方法到此结束。
        return ipAddress;
    }

    // 设置 IpAddress 的值。
    // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
    public void setIpAddress(String ipAddress) {
        // 把ipAddress保存到 this.ipAddress。
        this.ipAddress = ipAddress;
    }

    // 读取 用户成长助手（UserAgent） 的值。
    public String getUserAgent() {
        // 把用户成长助手（userAgent）交还给调用方，本方法到此结束。
        return userAgent;
    }

    // 设置 用户成长助手（UserAgent） 的值。
    // 用户成长助手（userAgent），使用 String 类型保存；可为 null，表示尚未提供。
    public void setUserAgent(String userAgent) {
        // 把用户成长助手（userAgent）保存到 this.userAgent。
        this.userAgent = userAgent;
    }

    // 读取 设备显示名称（DeviceLabel） 的值。
    public String getDeviceLabel() {
        // 把设备显示名称（deviceLabel）交还给调用方，本方法到此结束。
        return deviceLabel;
    }

    // 设置 设备显示名称（DeviceLabel） 的值。
    // 设备显示名称（deviceLabel），使用 String 类型保存；可为 null，表示尚未提供。
    public void setDeviceLabel(String deviceLabel) {
        // 把设备显示名称（deviceLabel）保存到 this.deviceLabel。
        this.deviceLabel = deviceLabel;
    }

    // 读取 追踪标识编号（TraceId） 的值。
    public String getTraceId() {
        // 把本次请求的追踪编号（traceId）交还给调用方，本方法到此结束。
        return traceId;
    }

    // 设置 追踪标识编号（TraceId） 的值。
    // 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setTraceId(String traceId) {
        // 把本次请求的追踪编号（traceId）保存到 this.traceId。
        this.traceId = traceId;
    }

    // 读取 OccurredAt 的值。
    public Instant getOccurredAt() {
        // 把occurredAt交还给调用方，本方法到此结束。
        return occurredAt;
    }

    // 设置 OccurredAt 的值。
    // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
    public void setOccurredAt(Instant occurredAt) {
        // 把occurredAt保存到 this.occurredAt。
        this.occurredAt = occurredAt;
    }
}
