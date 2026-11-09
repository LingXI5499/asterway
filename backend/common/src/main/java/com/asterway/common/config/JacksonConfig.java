/*
 * 提供JacksonConfig相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入DeserializationFeature，下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.DeserializationFeature;
// 引入Jackson2对象数据库读写器BuilderCustomizer（Jackson2ObjectMapperBuilderCustomizer），下面可以直接使用这个类型或静态成员。
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
// 引入Bean，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Bean;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;

/* 拒绝未知输入字段，防止将 roleIds 等字段混入普通注册或资料更新。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 JacksonConfig；提供JacksonConfig相关的共享实现或应用配置。
@Configuration
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class JacksonConfig {

    // 把此方法返回的对象登记为可被注入的共享组件。
    // strictJSON数据（strictJson）方法：调用 strictJson 完成strictJSON数据处理；参数负责传入本次处理所需的数据。
    @Bean
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public Jackson2ObjectMapperBuilderCustomizer strictJson() {
        // builder，使用 null 类型保存。
        // 把调用 (builder)->builder.featuresToEnable 完成featuresToEnable处理交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
