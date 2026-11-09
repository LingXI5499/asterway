/*
 * 集中处理JSON数据Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入JSON数据ProcessingException（JsonProcessingException），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.core.JsonProcessingException;
// 引入类型Reference（TypeReference），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.core.type.TypeReference;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* JSONB 字符串与明确类型之间的转换，不提供未知类型的动态反序列化。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 JsonUtils；集中处理JSON数据Utils的重复规则，避免各业务模块分别实现。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class JsonUtils {

    // 访问数据库的 Mapper（mapper），使用 ObjectMapper 类型保存。
    private final ObjectMapper mapper;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 访问数据库的 Mapper（mapper），使用 ObjectMapper 类型保存。
    public JsonUtils(ObjectMapper mapper) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
    }

    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 值（value），使用 Object 类型保存。
    public String write(Object value) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把把对象编码为 JSON 文字交还给调用方，本方法到此结束。
            return mapper.writeValueAsString(value);
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("JSON 数据转换失败");
        }
    }

    // 对象（object）方法：调用 object 完成对象处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public Map<String, Object> object(String value) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 定义类 ；集中处理JSON数据Utils的重复规则，避免各业务模块分别实现。
            // 把读取 值（Value）交还给调用方，本方法到此结束。
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() { });
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("JSON 对象格式无效");
        }
    }

    // 文字（text）方法：调用 text 完成文字处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public String text(String value) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把读取 值（Value）交还给调用方，本方法到此结束。
            return mapper.readValue(value, String.class);
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("JSON 字符串格式无效");
        }
    }
}
