/*
 * 用 Java 字段表示操作日志数据库记录，由 Mapper 负责读写。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.entity;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 操作审计实体，变更摘要必须先脱敏。 */
// 定义类 OperationLogEntity；用 Java 字段表示操作日志数据库记录，由 Mapper 负责读写。
public class OperationLogEntity {

    /* 数据库自增主键；对外作为字符串传输。 */
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long id;

    /* 操作者账户标识，匿名失败场景可空。 */
    // actor编号（actorId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long actorId;

    /* 操作时用户名快照。 */
    // actor用户名（actorUsername），使用 String 类型保存；可为 null，表示尚未提供。
    private String actorUsername;

    /* 被管理账户标识，用于管理员日志范围筛选。 */
    // 目标值用户编号（targetUserId），使用 Long 类型保存；可为 null，表示尚未提供。
    private Long targetUserId;

    /* 被操作资源类别。 */
    // resource类型（resourceType），使用 String 类型保存；可为 null，表示尚未提供。
    private String resourceType;

    /* 被操作资源标识快照。 */
    // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
    private String resourceId;

    /* 动作权限或审计动作编码。 */
    // 操作编码（actionCode），使用 String 类型保存；可为 null，表示尚未提供。
    private String actionCode;

    /* SUCCESS 或 FAILURE。 */
    // outcome，使用 String 类型保存；可为 null，表示尚未提供。
    private String outcome;

    /* 结果或拒绝原因编码。 */
    // 原因编码（reasonCode），使用 String 类型保存；可为 null，表示尚未提供。
    private String reasonCode;

    /* 仅安全字段的 JSON 对象，应用负责拒绝密码、JWT 和验证码答案。 */
    // 修改汇总（changeSummary），使用 String 类型保存；可为 null，表示尚未提供。
    private String changeSummary;

    /* 请求来源 IP；代理来源必须经过可信配置。 */
    // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
    private String ipAddress;

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

    // 读取 Actor编号（ActorId） 的值。
    public Long getActorId() {
        // 把actor编号（actorId）交还给调用方，本方法到此结束。
        return actorId;
    }

    // 设置 Actor编号（ActorId） 的值。
    // actor编号（actorId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setActorId(Long actorId) {
        // 把actor编号（actorId）保存到 this.actorId。
        this.actorId = actorId;
    }

    // 读取 Actor用户名（ActorUsername） 的值。
    public String getActorUsername() {
        // 把actor用户名（actorUsername）交还给调用方，本方法到此结束。
        return actorUsername;
    }

    // 设置 Actor用户名（ActorUsername） 的值。
    // actor用户名（actorUsername），使用 String 类型保存；可为 null，表示尚未提供。
    public void setActorUsername(String actorUsername) {
        // 把actor用户名（actorUsername）保存到 this.actorUsername。
        this.actorUsername = actorUsername;
    }

    // 读取 目标值用户编号（TargetUserId） 的值。
    public Long getTargetUserId() {
        // 把目标值用户编号（targetUserId）交还给调用方，本方法到此结束。
        return targetUserId;
    }

    // 设置 目标值用户编号（TargetUserId） 的值。
    // 目标值用户编号（targetUserId），使用 Long 类型保存；可为 null，表示尚未提供。
    public void setTargetUserId(Long targetUserId) {
        // 把目标值用户编号（targetUserId）保存到 this.targetUserId。
        this.targetUserId = targetUserId;
    }

    // 读取 Resource类型（ResourceType） 的值。
    public String getResourceType() {
        // 把resource类型（resourceType）交还给调用方，本方法到此结束。
        return resourceType;
    }

    // 设置 Resource类型（ResourceType） 的值。
    // resource类型（resourceType），使用 String 类型保存；可为 null，表示尚未提供。
    public void setResourceType(String resourceType) {
        // 把resource类型（resourceType）保存到 this.resourceType。
        this.resourceType = resourceType;
    }

    // 读取 Resource编号（ResourceId） 的值。
    public String getResourceId() {
        // 把resource编号（resourceId）交还给调用方，本方法到此结束。
        return resourceId;
    }

    // 设置 Resource编号（ResourceId） 的值。
    // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
    public void setResourceId(String resourceId) {
        // 把resource编号（resourceId）保存到 this.resourceId。
        this.resourceId = resourceId;
    }

    // 读取 操作编码（ActionCode） 的值。
    public String getActionCode() {
        // 把操作编码（actionCode）交还给调用方，本方法到此结束。
        return actionCode;
    }

    // 设置 操作编码（ActionCode） 的值。
    // 操作编码（actionCode），使用 String 类型保存；可为 null，表示尚未提供。
    public void setActionCode(String actionCode) {
        // 把操作编码（actionCode）保存到 this.actionCode。
        this.actionCode = actionCode;
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

    // 读取 修改汇总（ChangeSummary） 的值。
    public String getChangeSummary() {
        // 把修改汇总（changeSummary）交还给调用方，本方法到此结束。
        return changeSummary;
    }

    // 设置 修改汇总（ChangeSummary） 的值。
    // 修改汇总（changeSummary），使用 String 类型保存；可为 null，表示尚未提供。
    public void setChangeSummary(String changeSummary) {
        // 把修改汇总（changeSummary）保存到 this.changeSummary。
        this.changeSummary = changeSummary;
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
