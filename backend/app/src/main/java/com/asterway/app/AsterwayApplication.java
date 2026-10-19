package com.asterway.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Asterway 唯一可执行应用入口（模块化单体：一个进程、一个数据库）。
 * 全局统一注册各业务模块 Mapper，不逐接口加 @Mapper。
 */
@SpringBootApplication(scanBasePackages = "com.asterway")
@MapperScan("com.asterway.**.mapper")
public class AsterwayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsterwayApplication.class, args);
    }
}
