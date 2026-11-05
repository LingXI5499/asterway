package com.asterway.common.security;

import java.time.Instant;

/** 执行开始对任务状态事实的中立联动，不将安全日志当作业务历史。 */
public interface TaskStateHistoryProvider {
    /** 任务已成功推进版本后调用；只有状态实际变化才追加事件。 */
    void recordExecutionStart(
            long userId, long taskId, String beforeStatus, long entityVersion, Instant occurredAt);
}
