/*
 * 提供成长助手Config相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入值（Value），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.annotation.Value;
// 引入Bean，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Bean;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;
// 引入Thread连接池任务Executor（ThreadPoolTaskExecutor），下面可以直接使用这个类型或静态成员。
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
// 引入异步SupportConfigurer（AsyncSupportConfigurer），下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
// 引入WebMvcConfigurer，下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 引入URI，下面可以直接使用这个类型或静态成员。
import java.net.URI;
// 引入持续时间（Duration），下面可以直接使用这个类型或静态成员。
import java.time.Duration;

/* Agent 内部地址和超时；私密调用密钥仅由服务端读取，不进入任何返回结果。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 AgentConfig；提供成长助手Config相关的共享实现或应用配置。
@Configuration
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentConfig implements WebMvcConfigurer {

    // 基础地址（baseUrl），使用 URI 类型保存。
    private final URI baseUrl;
    // 内部键（internalKey），使用 String 类型保存；可为 null，表示尚未提供。
    private final String internalKey;
    // 连接超时（connectTimeout），使用 Duration 类型保存。
    private final Duration connectTimeout;
    // 请求超时（requestTimeout），使用 Duration 类型保存。
    private final Duration requestTimeout;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public AgentConfig(
            // 使用 Value 注解配置下面的类、方法或参数。
            // 基础地址（baseUrl），使用 String 类型保存；可为 null，表示尚未提供。
            @Value("${asterway.agent.base-url:http://127.0.0.1:8001}") String baseUrl,
            // 使用 Value 注解配置下面的类、方法或参数。
            // 内部键（internalKey），使用 String 类型保存；可为 null，表示尚未提供。
            @Value("${asterway.agent.internal-key:}") String internalKey,
            // 使用 Value 注解配置下面的类、方法或参数。
            // 连接秒数（connectSeconds），使用 int 类型保存。
            @Value("${asterway.agent.connect-timeout-seconds:5}") int connectSeconds,
            // 使用 Value 注解配置下面的类、方法或参数。
            // 请求秒数（requestSeconds），使用 int 类型保存。
            @Value("${asterway.agent.request-timeout-seconds:280}") int requestSeconds) {
        // 准备parsed，类型为 URI；初始内容是调用 URI.create 完成创建处理。
        URI parsed = URI.create(baseUrl);
        // 检查!("http".equals(parsed.getScheme()) || "https".equals(parsed.getScheme())) || parsed.getHost() == nu…，即对应内容是否不存在；成立时执行下面的分支。
        if (!("http".equals(parsed.getScheme()) || "https".equals(parsed.getScheme()))
                // 继续补充同一个判断或备用结果：|| parsed.getHost() == null，即对应内容是否不存在。
                || parsed.getHost() == null
                // 继续补充同一个判断或备用结果：条件 || parsed.getUserInfo() != null 是否成立。
                || parsed.getUserInfo() != null
                // 继续补充同一个判断或备用结果：条件 || parsed.getQuery() != null 是否成立。
                || parsed.getQuery() != null
                // 继续补充同一个判断或备用结果：条件 || parsed.getFragment() != null 是否成立。
                || parsed.getFragment() != null
                // 继续补充同一个判断或备用结果：输入或集合是否为空：|| !(parsed.getPath().isEmpty() || "/".equals(parsed.getPath()))。
                || !(parsed.getPath().isEmpty() || "/".equals(parsed.getPath()))
                // 继续补充同一个判断或备用结果：条件 || connectSeconds < 1 是否成立。
                || connectSeconds < 1
                // 继续补充同一个判断或备用结果：条件 || connectSeconds > 30 是否成立。
                || connectSeconds > 30
                // 继续补充同一个判断或备用结果：条件 || requestSeconds < 30 是否成立。
                || requestSeconds < 30
                // 继续补充同一个判断或备用结果：条件 || requestSeconds > 300) { 是否成立。
                || requestSeconds > 300) {
            // 中止正常处理并抛出异常：新建 Illegal参数Exception（IllegalArgumentException） 实例；由上层按失败处理。
            throw new IllegalArgumentException("Agent 内部地址或超时参数不合法");
        }
        // 检查输入或集合是否为空：!internalKey.isEmpty() && !internalKey.matches("[A-Za-z0-9_\\-]{32,128}")；成立时执行下面的分支。
        if (!internalKey.isEmpty() && !internalKey.matches("[A-Za-z0-9_\\-]{32,128}")) {
            // 中止正常处理并抛出异常：新建 Illegal参数Exception（IllegalArgumentException） 实例；由上层按失败处理。
            throw new IllegalArgumentException("Agent 内部调用密钥须为 32 至 128 位安全字符");
        }
        // 把parsed保存到 this.baseUrl。
        this.baseUrl = parsed;
        // 把内部键（internalKey）保存到 this.internalKey。
        this.internalKey = internalKey;
        // 把调用 Duration.ofSeconds 完成of秒数处理保存到 this.connectTimeout。
        this.connectTimeout = Duration.ofSeconds(connectSeconds);
        // 把调用 Duration.ofSeconds 完成of秒数处理保存到 this.requestTimeout。
        this.requestTimeout = Duration.ofSeconds(requestSeconds);
    }

    // endpoint方法：调用 endpoint 完成endpoint处理；参数负责传入本次处理所需的数据。
    // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
    public URI endpoint(String path) {
        // 把调用 baseUrl.resolve 完成resolve处理交还给调用方，本方法到此结束。
        return baseUrl.resolve(path);
    }

    // 内部键（internalKey）方法：调用 internalKey 完成内部键处理；参数负责传入本次处理所需的数据。
    public String internalKey() {
        // 把内部键（internalKey）交还给调用方，本方法到此结束。
        return internalKey;
    }

    // 连接超时（connectTimeout）方法：调用 connectTimeout 完成连接超时处理；参数负责传入本次处理所需的数据。
    public Duration connectTimeout() {
        // 把连接超时（connectTimeout）交还给调用方，本方法到此结束。
        return connectTimeout;
    }

    // 请求超时（requestTimeout）方法：调用 requestTimeout 完成请求超时处理；参数负责传入本次处理所需的数据。
    public Duration requestTimeout() {
        // 把请求超时（requestTimeout）交还给调用方，本方法到此结束。
        return requestTimeout;
    }

    // 把此方法返回的对象登记为可被注入的共享组件。
    // 成长助手分段回复流Executor（agentStreamExecutor）方法：调用 agentStreamExecutor 完成成长助手分段回复流Executor处理；参数负责传入本次处理所需的数据。
    @Bean
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ThreadPoolTaskExecutor agentStreamExecutor() {
        // 准备executor，类型为 ThreadPoolTaskExecutor；初始内容是新建 Thread连接池任务Executor（ThreadPoolTaskExecutor） 实例。
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置 Core连接池每页条数（CorePoolSize） 的值。
        executor.setCorePoolSize(8);
        // 设置 最大连接池每页条数（MaxPoolSize） 的值。
        executor.setMaxPoolSize(16);
        // 设置 QueueCapacity 的值。
        executor.setQueueCapacity(32);
        // 设置 Thread名称前缀（ThreadNamePrefix） 的值。
        executor.setThreadNamePrefix("agent-stream-");
        // 设置 等待For任务列表To完成事件发生时Shutdown（WaitForTasksToCompleteOnShutdown） 的值。
        executor.setWaitForTasksToCompleteOnShutdown(false);
        // 把executor交还给调用方，本方法到此结束。
        return executor;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // configure异步Support（configureAsyncSupport）方法：调用 configureAsyncSupport 完成configure异步Support处理；参数负责传入本次处理所需的数据。
    @Override
    // configurer，使用 AsyncSupportConfigurer 类型保存。
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置 任务Executor（TaskExecutor） 的值。
        configurer.setTaskExecutor(agentStreamExecutor());
        // 设置 默认值超时（DefaultTimeout） 的值。
        configurer.setDefaultTimeout(requestTimeout.plusSeconds(15).toMillis());
    }
}
