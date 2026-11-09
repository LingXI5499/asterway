/*
 * 提供执行计时超时Scheduler相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.scheduling;

// 引入执行计时会话业务服务（ExecutionSessionService），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.service.ExecutionSessionService;

// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入定时执行（Scheduled），下面可以直接使用这个类型或静态成员。
import org.springframework.scheduling.annotation.Scheduled;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* 只负责触发到期扫描；所有状态变更及事务仍位于会话 Service。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 ExecutionTimeoutScheduler；提供执行计时超时Scheduler相关的共享实现或应用配置。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class ExecutionTimeoutScheduler {
    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionTimeoutScheduler.class);
    // 业务服务（service），使用 ExecutionSessionService 类型保存。
    private final ExecutionSessionService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 ExecutionSessionService 类型保存。
    public ExecutionTimeoutScheduler(ExecutionSessionService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 按这里指定的间隔或时间规则执行维护任务。
    // 过期（expire）方法：调用 expire 完成过期处理；参数负责传入本次处理所需的数据。
    @Scheduled(
            // 继续传入或组合表达式 fixedDelayString = "${asterway.execution.scan-delay-ms:15000}" 的结果，完成上面尚未结束的调用或声明。
            fixedDelayString = "${asterway.execution.scan-delay-ms:15000}",
            // 继续传入或组合表达式 initialDelayString = "${asterway.execution.scan-delay-ms:15000}") 的结果，完成上面尚未结束的调用或声明。
            initialDelayString = "${asterway.execution.scan-delay-ms:15000}")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void expire() {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 调用 service.scanExpired 完成scan已过期处理。
            service.scanExpired();
        // 错误（error），使用 RuntimeException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (RuntimeException error) {
            // 记录error级别的运行信息，供定位处理过程和失败原因。
            LOGGER.error("执行到期扫描未完成，将在下一轮重试", error);
        }
    }
}
