/*
 * 启动时验证 PostgreSQL 与 Redis 可用，依赖失败时不报告启动成功。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入数据来源（DataSource），下面可以直接使用这个类型或静态成员。
import javax.sql.DataSource;
// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入ApplicationRunner，下面可以直接使用这个类型或静态成员。
import org.springframework.boot.ApplicationRunner;
// 引入Bean，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Bean;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;
// 引入Redis缓存数据库连接创建器（RedisConnectionFactory），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.connection.RedisConnectionFactory;

/* 启动时检查必要的基础连接，连接失败就终止启动，不将不可用服务报告为就绪。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 StartupConfig；启动时验证 PostgreSQL 与 Redis 可用，依赖失败时不报告启动成功。
@Configuration
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class StartupConfig {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfig.class);

    // 把此方法返回的对象登记为可被注入的共享组件。
    // 核对Infrastructure（checkInfrastructure）方法：核对 Infrastructure；参数负责传入本次处理所需的数据。
    @Bean
    // 数据来源（dataSource），使用 DataSource 类型保存。
    // Redis缓存（redis），使用 RedisConnectionFactory 类型保存。
    public ApplicationRunner checkInfrastructure(DataSource dataSource, RedisConnectionFactory redis) {
        // 参数（args），使用 null 类型保存。
        // 把读取 数据库连接（Connection） 的值交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return args -> {
            // 准备数据库连接（connection），类型为 null；初始内容是读取 数据库连接（Connection） 的值。
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try (var connection = dataSource.getConnection()) {
                // 检查条件 !connection.isValid(3) 是否成立；成立时执行下面的分支。
                if (!connection.isValid(3)) {
                    // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
                    throw new IllegalStateException("本地数据库连接不可用");
                }
            }
            // 准备数据库连接（connection），类型为 null；初始内容是读取 数据库连接（Connection） 的值。
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try (var connection = redis.getConnection()) {
                // 检查条件 !"PONG".equals(connection.ping()) 是否成立；成立时执行下面的分支。
                if (!"PONG".equals(connection.ping())) {
                    // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
                    throw new IllegalStateException("本地 Redis 连接不可用");
                }
            }
            // 记录info级别的运行信息，供定位处理过程和失败原因。
            LOGGER.info("本地数据库与 Redis 连接已就绪");
        };
    }
}
