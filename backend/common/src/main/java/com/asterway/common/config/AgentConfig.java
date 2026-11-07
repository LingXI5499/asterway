package com.asterway.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.time.Duration;

/** Agent 内部地址和超时；私密调用密钥仅由服务端读取，不进入任何返回结果。 */
@Configuration
public class AgentConfig implements WebMvcConfigurer {

    private final URI baseUrl;
    private final String internalKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public AgentConfig(
            @Value("${asterway.agent.base-url:http://127.0.0.1:8001}") String baseUrl,
            @Value("${asterway.agent.internal-key:}") String internalKey,
            @Value("${asterway.agent.connect-timeout-seconds:5}") int connectSeconds,
            @Value("${asterway.agent.request-timeout-seconds:280}") int requestSeconds) {
        URI parsed = URI.create(baseUrl);
        if (!("http".equals(parsed.getScheme()) || "https".equals(parsed.getScheme()))
                || parsed.getHost() == null
                || parsed.getUserInfo() != null
                || parsed.getQuery() != null
                || parsed.getFragment() != null
                || !(parsed.getPath().isEmpty() || "/".equals(parsed.getPath()))
                || connectSeconds < 1
                || connectSeconds > 30
                || requestSeconds < 30
                || requestSeconds > 300) {
            throw new IllegalArgumentException("Agent 内部地址或超时参数不合法");
        }
        if (!internalKey.isEmpty() && !internalKey.matches("[A-Za-z0-9_\\-]{32,128}")) {
            throw new IllegalArgumentException("Agent 内部调用密钥须为 32 至 128 位安全字符");
        }
        this.baseUrl = parsed;
        this.internalKey = internalKey;
        this.connectTimeout = Duration.ofSeconds(connectSeconds);
        this.requestTimeout = Duration.ofSeconds(requestSeconds);
    }

    public URI endpoint(String path) {
        return baseUrl.resolve(path);
    }

    public String internalKey() {
        return internalKey;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    @Bean
    public ThreadPoolTaskExecutor agentStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(32);
        executor.setThreadNamePrefix("agent-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(agentStreamExecutor());
        configurer.setDefaultTimeout(requestTimeout.plusSeconds(15).toMillis());
    }
}
