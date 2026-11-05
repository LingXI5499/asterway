package com.asterway.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 本地执行计时参数。配置保持在唯一 application.yml，不引入调度平台。 */
@Configuration
@EnableScheduling
public class ExecutionConfig {
    private final int heartbeatIntervalSeconds;
    private final int heartbeatTimeoutSeconds;
    private final int scanBatchSize;

    public ExecutionConfig(
            @Value("${asterway.execution.heartbeat-interval-seconds:20}")
                    int heartbeatIntervalSeconds,
            @Value("${asterway.execution.heartbeat-timeout-seconds:90}")
                    int heartbeatTimeoutSeconds,
            @Value("${asterway.execution.scan-delay-ms:15000}") long scanDelayMillis,
            @Value("${asterway.execution.scan-batch-size:100}") int scanBatchSize) {
        if (heartbeatIntervalSeconds <= 0
                || heartbeatTimeoutSeconds <= heartbeatIntervalSeconds
                || scanDelayMillis <= 0
                || scanBatchSize < 1
                || scanBatchSize > 1000) {
            throw new IllegalArgumentException("执行心跳、超时和扫描参数不合法");
        }
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        this.scanBatchSize = scanBatchSize;
    }

    public int heartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public int heartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public int scanBatchSize() {
        return scanBatchSize;
    }
}
