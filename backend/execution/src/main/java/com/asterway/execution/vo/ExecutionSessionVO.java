package com.asterway.execution.vo;

import java.time.Instant;

/** 执行会话的接口视图，明确排除控制端 SID 等内部字段。 */
public class ExecutionSessionVO {

    /** 会话标识。 */
    private String id;

    /** 任务标识。 */
    private String taskId;

    /** 任务当前名称，历史不会因逻辑删除消失。 */
    private String taskName;

    /** 会话状态。 */
    private String status;

    /** 首次开始时间。 */
    private Instant startedAt;

    /** 最终结束时间。 */
    private Instant finishedAt;

    /** 会话结束原因。 */
    private String finishReason;

    /** 会话控制版本。 */
    private Long version;

    /** 任务当前版本。 */
    private Long taskVersion;

    /** 累计实际秒数，汇总后取整。 */
    private Long actualSeconds;

    /** 查询区间内实际秒数，未筛日期时为空。 */
    private Long rangeActualSeconds;

    /** 最后有效心跳时间。 */
    private Instant lastHeartbeatAt;

    /** 开放时间段标识。 */
    private String activeSegmentId;

    /** 开放段开始时间。 */
    private Instant activeSegmentStartedAt;

    /** 本次查询捕获的服务端时间。 */
    private Instant serverNow;

    /** 心跳超时边界。 */
    private Instant heartbeatDeadline;

    /** 当前段的计划或时区边界。 */
    private Instant eligibleUntil;

    /** 当前登录端是否可以为此段发送心跳。 */
    private boolean canHeartbeat;

    /** 前端应采用的心跳间隔。 */
    private int heartbeatIntervalSeconds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getTaskVersion() {
        return taskVersion;
    }

    public void setTaskVersion(Long taskVersion) {
        this.taskVersion = taskVersion;
    }

    public Long getActualSeconds() {
        return actualSeconds;
    }

    public void setActualSeconds(Long actualSeconds) {
        this.actualSeconds = actualSeconds;
    }

    public Long getRangeActualSeconds() {
        return rangeActualSeconds;
    }

    public void setRangeActualSeconds(Long rangeActualSeconds) {
        this.rangeActualSeconds = rangeActualSeconds;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public String getActiveSegmentId() {
        return activeSegmentId;
    }

    public void setActiveSegmentId(String activeSegmentId) {
        this.activeSegmentId = activeSegmentId;
    }

    public Instant getActiveSegmentStartedAt() {
        return activeSegmentStartedAt;
    }

    public void setActiveSegmentStartedAt(Instant activeSegmentStartedAt) {
        this.activeSegmentStartedAt = activeSegmentStartedAt;
    }

    public Instant getServerNow() {
        return serverNow;
    }

    public void setServerNow(Instant serverNow) {
        this.serverNow = serverNow;
    }

    public Instant getHeartbeatDeadline() {
        return heartbeatDeadline;
    }

    public void setHeartbeatDeadline(Instant heartbeatDeadline) {
        this.heartbeatDeadline = heartbeatDeadline;
    }

    public Instant getEligibleUntil() {
        return eligibleUntil;
    }

    public void setEligibleUntil(Instant eligibleUntil) {
        this.eligibleUntil = eligibleUntil;
    }

    public boolean getCanHeartbeat() {
        return canHeartbeat;
    }

    public void setCanHeartbeat(boolean canHeartbeat) {
        this.canHeartbeat = canHeartbeat;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }
}
