package com.asterway.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 本地接口文档，提供 Bearer 输入入口，不改变实际接口前缀。 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accountOpenApi() {
        return new OpenAPI().info(new Info().title("Asterway 账户接口").description("账户与 RBAC 后端接口"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
