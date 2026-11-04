package com.asterway.common.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/** 启动时检查必要的基础连接，连接失败就终止启动，不将不可用服务报告为就绪。 */
@Configuration
public class StartupConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfig.class);

    @Bean
    public ApplicationRunner checkInfrastructure(DataSource dataSource, RedisConnectionFactory redis) {
        return args -> {
            try (var connection = dataSource.getConnection()) {
                if (!connection.isValid(3)) {
                    throw new IllegalStateException("本地数据库连接不可用");
                }
            }
            try (var connection = redis.getConnection()) {
                if (!"PONG".equals(connection.ping())) {
                    throw new IllegalStateException("本地 Redis 连接不可用");
                }
            }
            LOGGER.info("本地数据库与 Redis 连接已就绪");
        };
    }
}
