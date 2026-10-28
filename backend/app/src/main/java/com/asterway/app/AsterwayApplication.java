package com.asterway.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Asterway 唯一可执行应用入口（模块化单体：一个进程、一个数据库）。
// 通过 scanBasePackages 显式声明扫描范围，让 Spring 发现已实现业务及应用适配的 Bean；
// 通过 @MapperScan 扫描 identity 的 Mapper 接口，避免每个接口都写 @Mapper。
@SpringBootApplication(
        scanBasePackages = {
                "com.asterway.app",
                "com.asterway.identity",
                "com.asterway.administration",
                "com.asterway.execution",
                "com.asterway.growth"
        }
)
@MapperScan({
        "com.asterway.identity.mapper",
        "com.asterway.app.mapper",
        "com.asterway.administration.mapper",
        "com.asterway.execution.mapper",
        "com.asterway.growth.mapper"
})
public class AsterwayApplication {

    // 程序入口
    public static void main(String[] args) {
        SpringApplication.run(AsterwayApplication.class, args);
    }
}
