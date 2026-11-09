/*
 * 提供AsterwayApplication相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway;

// 引入SpringApplication，下面可以直接使用这个类型或静态成员。
import org.springframework.boot.SpringApplication;
// 引入SpringBootApplication，下面可以直接使用这个类型或静态成员。
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Asterway 应用启动入口。
 */
// 启用 Spring Boot 启动、配置装配与组件扫描。
// 定义类 AsterwayApplication；提供AsterwayApplication相关的共享实现或应用配置。
@SpringBootApplication
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AsterwayApplication {

    // 程序入口（main）方法：调用 main 完成程序入口处理；参数负责传入本次处理所需的数据。
    // 参数（args），使用 String[] 类型保存。
    public static void main(String[] args) {
        // 调用 SpringApplication.run 完成方案运行处理。
        SpringApplication.run(AsterwayApplication.class, args);
    }
}
