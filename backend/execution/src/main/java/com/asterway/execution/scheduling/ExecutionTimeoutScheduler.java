package com.asterway.execution.scheduling;

import com.asterway.execution.service.ExecutionSessionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 只负责触发到期扫描；所有状态变更及事务仍位于会话 Service。 */
@Component
public class ExecutionTimeoutScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTimeoutScheduler.class);
    private final ExecutionSessionService service;

    public ExecutionTimeoutScheduler(ExecutionSessionService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${asterway.execution.scan-delay-ms:15000}",
            initialDelayString = "${asterway.execution.scan-delay-ms:15000}")
    public void expire() {
        try {
            service.scanExpired();
        } catch (RuntimeException error) {
            LOGGER.error("执行到期扫描未完成，将在下一轮重试", error);
        }
    }
}
