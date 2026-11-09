/*
 * 提供是否展开接口Config相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入Components，下面可以直接使用这个类型或静态成员。
import io.swagger.v3.oas.models.Components;
// 引入是否展开接口（OpenAPI），下面可以直接使用这个类型或静态成员。
import io.swagger.v3.oas.models.OpenAPI;
// 引入信息（Info），下面可以直接使用这个类型或静态成员。
import io.swagger.v3.oas.models.info.Info;
// 引入SecurityRequirement，下面可以直接使用这个类型或静态成员。
import io.swagger.v3.oas.models.security.SecurityRequirement;
// 引入SecurityScheme，下面可以直接使用这个类型或静态成员。
import io.swagger.v3.oas.models.security.SecurityScheme;
// 引入Bean，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Bean;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;

/* 本地接口文档，提供 Bearer 输入入口，不改变实际接口前缀。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 OpenApiConfig；提供是否展开接口Config相关的共享实现或应用配置。
@Configuration
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class OpenApiConfig {

    // 把此方法返回的对象登记为可被注入的共享组件。
    // 账号是否展开接口（accountOpenApi）方法：调用 accountOpenApi 完成账号是否展开接口处理；参数负责传入本次处理所需的数据。
    @Bean
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public OpenAPI accountOpenApi() {
        // 把新建 是否展开接口（OpenAPI） 实例交还给调用方，本方法到此结束。
        return new OpenAPI().info(new Info().title("Asterway 账户接口").description("账户与 RBAC 后端接口"))
                // 继续对上一步结果执行调用 components(new Components().addSecuritySchemes 完成加入SecuritySchemes处理。
                .components(new Components().addSecuritySchemes("bearerAuth",
                        // 继续传入或组合新建 SecurityScheme 实例，完成上面尚未结束的调用或声明。
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // 继续对上一步结果执行调用 addSecurityItem(new SecurityRequirement().addList 完成加入列表处理。
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
