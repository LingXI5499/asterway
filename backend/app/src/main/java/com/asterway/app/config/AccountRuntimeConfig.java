package com.asterway.app.config;

import com.asterway.identity.api.AccountSecurityPort;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 迁移完成后核对安全账本；严格区分 JSON 字符串、布尔和数值类型。
@Configuration
@EnableScheduling
public class AccountRuntimeConfig {

    @Bean
    public ApplicationRunner accountSecurityInitialization(AccountSecurityPort security) {
        ApplicationRunner runner = arguments -> security.initialize();
        return runner;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictAccountJson() {
        Jackson2ObjectMapperBuilderCustomizer customizer = builder -> builder.postConfigurer(mapper -> {
            mapper.coercionConfigFor(LogicalType.Textual)
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
            mapper.coercionConfigFor(LogicalType.Boolean)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
        });
        return customizer;
    }
}
