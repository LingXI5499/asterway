/*
 * 提供执行计时Config相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入值（Value），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.annotation.Value;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;
// 引入EnableScheduling，下面可以直接使用这个类型或静态成员。
import org.springframework.scheduling.annotation.EnableScheduling;

/* 本地执行计时参数。配置保持在唯一 application.yml，不引入调度平台。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 ExecutionConfig；提供执行计时Config相关的共享实现或应用配置。
@Configuration
// 允许执行项目声明的定时维护任务。
@EnableScheduling
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class ExecutionConfig {
    // 心跳间隔秒数（heartbeatIntervalSeconds），使用 int 类型保存。
    private final int heartbeatIntervalSeconds;
    // 心跳超时秒数（heartbeatTimeoutSeconds），使用 int 类型保存。
    private final int heartbeatTimeoutSeconds;
    // scan批量每页条数（scanBatchSize），使用 int 类型保存。
    private final int scanBatchSize;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public ExecutionConfig(
            // 使用 Value 注解配置下面的类、方法或参数。
            // 心跳间隔秒数（heartbeatIntervalSeconds），使用 int 类型保存。
            @Value("${asterway.execution.heartbeat-interval-seconds:20}")
                    // 继续传入或组合表达式 int heartbeatIntervalSeconds 的结果，完成上面尚未结束的调用或声明。
                    int heartbeatIntervalSeconds,
            // 使用 Value 注解配置下面的类、方法或参数。
            // 心跳超时秒数（heartbeatTimeoutSeconds），使用 int 类型保存。
            @Value("${asterway.execution.heartbeat-timeout-seconds:90}")
                    // 继续传入或组合表达式 int heartbeatTimeoutSeconds 的结果，完成上面尚未结束的调用或声明。
                    int heartbeatTimeoutSeconds,
            // 使用 Value 注解配置下面的类、方法或参数。
            // scanDelayMillis，使用 long 类型保存。
            @Value("${asterway.execution.scan-delay-ms:15000}") long scanDelayMillis,
            // 使用 Value 注解配置下面的类、方法或参数。
            // scan批量每页条数（scanBatchSize），使用 int 类型保存。
            @Value("${asterway.execution.scan-batch-size:100}") int scanBatchSize) {
        // 检查条件 heartbeatIntervalSeconds <= 0 || heartbeatTimeoutSeconds <= heartbeatIntervalSeconds || scanDelayMillis <= 0 || scanBatchSize < 1 || scanBatchSiz… 是否成立；成立时执行下面的分支。
        if (heartbeatIntervalSeconds <= 0
                // 继续补充同一个判断或备用结果：条件 || heartbeatTimeoutSeconds <= heartbeatIntervalSeconds 是否成立。
                || heartbeatTimeoutSeconds <= heartbeatIntervalSeconds
                // 继续补充同一个判断或备用结果：条件 || scanDelayMillis <= 0 是否成立。
                || scanDelayMillis <= 0
                // 继续补充同一个判断或备用结果：条件 || scanBatchSize < 1 是否成立。
                || scanBatchSize < 1
                // 继续补充同一个判断或备用结果：条件 || scanBatchSize > 1000) { 是否成立。
                || scanBatchSize > 1000) {
            // 中止正常处理并抛出异常：新建 Illegal参数Exception（IllegalArgumentException） 实例；由上层按失败处理。
            throw new IllegalArgumentException("执行心跳、超时和扫描参数不合法");
        }
        // 把心跳间隔秒数（heartbeatIntervalSeconds）保存到 this.heartbeatIntervalSeconds。
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        // 把心跳超时秒数（heartbeatTimeoutSeconds）保存到 this.heartbeatTimeoutSeconds。
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        // 把scan批量每页条数（scanBatchSize）保存到 this.scanBatchSize。
        this.scanBatchSize = scanBatchSize;
    }

    // 心跳间隔秒数（heartbeatIntervalSeconds）方法：调用 heartbeatIntervalSeconds 完成心跳间隔秒数处理；参数负责传入本次处理所需的数据。
    public int heartbeatIntervalSeconds() {
        // 把心跳间隔秒数（heartbeatIntervalSeconds）交还给调用方，本方法到此结束。
        return heartbeatIntervalSeconds;
    }

    // 心跳超时秒数（heartbeatTimeoutSeconds）方法：调用 heartbeatTimeoutSeconds 完成心跳超时秒数处理；参数负责传入本次处理所需的数据。
    public int heartbeatTimeoutSeconds() {
        // 把心跳超时秒数（heartbeatTimeoutSeconds）交还给调用方，本方法到此结束。
        return heartbeatTimeoutSeconds;
    }

    // scan批量每页条数（scanBatchSize）方法：调用 scanBatchSize 完成scan批量每页条数处理；参数负责传入本次处理所需的数据。
    public int scanBatchSize() {
        // 把scan批量每页条数（scanBatchSize）交还给调用方，本方法到此结束。
        return scanBatchSize;
    }
}
