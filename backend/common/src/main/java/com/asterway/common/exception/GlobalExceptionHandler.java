/*
 * 提供GlobalExceptionHandler相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.exception;

// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入操作Audit提供器（OperationAuditProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.OperationAuditProvider;
// 引入约束ViolationException（ConstraintViolationException），下面可以直接使用这个类型或静态成员。
import jakarta.validation.ConstraintViolationException;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入对象提供器（ObjectProvider），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.ObjectProvider;
// 引入数据访问权限Exception（DataAccessException），下面可以直接使用这个类型或静态成员。
import org.springframework.dao.DataAccessException;
// 引入数据IntegrityViolationException（DataIntegrityViolationException），下面可以直接使用这个类型或静态成员。
import org.springframework.dao.DataIntegrityViolationException;
// 引入响应（ResponseEntity），下面可以直接使用这个类型或静态成员。
import org.springframework.http.ResponseEntity;
// 引入HTTP请求消息NotReadableException（HttpMessageNotReadableException），下面可以直接使用这个类型或静态成员。
import org.springframework.http.converter.HttpMessageNotReadableException;
// 引入访问权限DeniedException（AccessDeniedException），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.AccessDeniedException;
// 引入绑定Exception（BindException），下面可以直接使用这个类型或静态成员。
import org.springframework.validation.BindException;
// 引入HTTP请求Media类型NotSupportedException（HttpMediaTypeNotSupportedException），下面可以直接使用这个类型或静态成员。
import org.springframework.web.HttpMediaTypeNotSupportedException;
// 引入HTTP请求请求方法NotSupportedException（HttpRequestMethodNotSupportedException），下面可以直接使用这个类型或静态成员。
import org.springframework.web.HttpRequestMethodNotSupportedException;
// 引入方法参数Not有效Exception（MethodArgumentNotValidException），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.MethodArgumentNotValidException;
// 引入MissingServlet请求参数Exception（MissingServletRequestParameterException），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.MissingServletRequestParameterException;
// 引入ExceptionHandler，下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ExceptionHandler;
// 引入Rest请求入口Advice（RestControllerAdvice），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestControllerAdvice;
// 引入方法参数类型MismatchException（MethodArgumentTypeMismatchException），下面可以直接使用这个类型或静态成员。
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
// 引入NoResourceFoundException，下面可以直接使用这个类型或静态成员。
import org.springframework.web.servlet.resource.NoResourceFoundException;

/* Controller 异常出口；安全过滤器中的异常使用同一响应格式单独写出。 */
// 为 HTTP 接口集中提供异常处理与返回规则。
// 定义类 GlobalExceptionHandler；提供GlobalExceptionHandler相关的共享实现或应用配置。
@RestControllerAdvice
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class GlobalExceptionHandler {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // audits，使用 ObjectProvider<OperationAuditProvider> 类型保存。
    private final ObjectProvider<OperationAuditProvider> audits;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // audits，使用 ObjectProvider<OperationAuditProvider> 类型保存。
    public GlobalExceptionHandler(ObjectProvider<OperationAuditProvider> audits) {
        // 把audits保存到 this.audits。
        this.audits = audits;
    }

    // audit方法：调用 audit 完成audit处理；参数负责传入本次处理所需的数据。
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private void audit(int status, String code) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备recorder，类型为 null；初始内容是读取 If是否可用（IfAvailable） 的值。
            var recorder = audits.getIfAvailable();
            // 检查条件 recorder != null 是否成立；成立时执行下面的分支。
            if (recorder != null) {
                // 调用 recorder.recordFailure 完成记录失败处理。
                recorder.recordFailure(status, code);
            }
        // 错误（error），使用 RuntimeException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (RuntimeException error) {
            // 记录warn级别的运行信息，供定位处理过程和失败原因。
            LOGGER.warn("拒绝请求的审计写入失败，类型：{}", error.getClass().getSimpleName());
        }
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // business方法：调用 business 完成business处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(BusinessException.class)
    // 错误（error），使用 BusinessException 类型保存。
    public ResponseEntity<ResultVO<Object>> business(BusinessException error) {
        // 读取 状态（Status） 的值。
        audit(error.getStatus(), error.getCode());
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(error.getStatus())
                // 继续对上一步结果执行调用 body(ResultVO.failure 完成失败处理。
                .body(ResultVO.failure(error.getCode(), error.getMessage(), error.getData()));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // validation方法：调用 validation 完成validation处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    // 错误（error），使用 BindException 类型保存。
    public ResponseEntity<ResultVO<Object>> validation(BindException error) {
        // 准备fields，类型为 null；初始内容是逐项处理集合中的数据，继续做筛选、转换或汇总。
        var fields = error.getBindingResult().getFieldErrors().stream()
                // 字段（field），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .map(field -> Map.of("field", field.getField(), "message",
                        // 根据前面的条件，从两个候选结果中选择一个：field.getDefaultMessage() == null ? "字段无效" : field.getDefaultMessage()))。
                        field.getDefaultMessage() == null ? "字段无效" : field.getDefaultMessage()))
                // 继续对上一步结果执行调用 toList 完成to列表处理。
                .toList();
        // 把调用 ResponseEntity.badRequest 完成bad请求处理交还给调用方，本方法到此结束。
        return ResponseEntity.badRequest()
                // 继续对上一步结果执行调用 body(ResultVO.failure 完成失败处理。
                .body(ResultVO.failure("INVALID_ARGUMENT", "请检查提交的字段", fields));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // 不合法（invalid）方法：调用 invalid 完成不合法处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
            // 继续传入或组合表达式 MethodArgumentTypeMismatchException.class, IllegalArgumentException.class 的结果，完成上面尚未结束的调用或声明。
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class,
            // 继续传入或组合表达式 MissingServletRequestParameterException.class}) 的结果，完成上面尚未结束的调用或声明。
            MissingServletRequestParameterException.class})
    // 错误（error），使用 Exception 类型保存。
    public ResponseEntity<ResultVO<Object>> invalid(Exception error) {
        // 把调用 ResponseEntity.badRequest 完成bad请求处理交还给调用方，本方法到此结束。
        return ResponseEntity.badRequest().body(ResultVO.failure("INVALID_ARGUMENT", "请求参数格式无效", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // denied方法：调用 denied 完成denied处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(AccessDeniedException.class)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<Object>> denied() {
        // 调用 audit 完成audit处理。
        audit(403, "FORBIDDEN");
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(403).body(ResultVO.failure("FORBIDDEN", "没有执行此操作的权限", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // missing方法：调用 missing 完成missing处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(NoResourceFoundException.class)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<Object>> missing() {
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(404).body(ResultVO.failure("NOT_FOUND", "请求地址不存在", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // integrity方法：调用 integrity 完成integrity处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(DataIntegrityViolationException.class)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<Object>> integrity() {
        // 调用 audit 完成audit处理。
        audit(409, "DATA_CONFLICT");
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(409).body(ResultVO.failure("DATA_CONFLICT", "数据重复或不满足保存条件", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // 数据库（database）方法：调用 database 完成数据库处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(DataAccessException.class)
    // 错误（error），使用 DataAccessException 类型保存。
    public ResponseEntity<ResultVO<Object>> database(DataAccessException error) {
        // 记录error级别的运行信息，供定位处理过程和失败原因。
        LOGGER.error("数据服务不可用，异常类型：{}", error.getClass().getSimpleName());
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(503).body(ResultVO.failure("SERVICE_UNAVAILABLE", "数据服务暂不可用，请稍后重试", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // 方法NotAllowed（methodNotAllowed）方法：调用 methodNotAllowed 完成方法NotAllowed处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<Object>> methodNotAllowed() {
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(405).body(ResultVO.failure("METHOD_NOT_ALLOWED", "当前地址不支持此请求方法", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // media类型（mediaType）方法：调用 mediaType 完成media类型处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResponseEntity<ResultVO<Object>> mediaType() {
        // 把调用 ResponseEntity.status 完成状态处理交还给调用方，本方法到此结束。
        return ResponseEntity.status(415).body(ResultVO.failure("UNSUPPORTED_MEDIA_TYPE", "请使用 JSON 请求体", null));
    }

    // 把指定异常交给下面的方法转换成统一错误响应。
    // unexpected方法：调用 unexpected 完成unexpected处理；参数负责传入本次处理所需的数据。
    @ExceptionHandler(Exception.class)
    // 错误（error），使用 Exception 类型保存。
    public ResponseEntity<ResultVO<Object>> unexpected(Exception error) {
        // 只记录异常类型，避免异常消息包含数据库绑定参数或凭据。
        // 记录error级别的运行信息，供定位处理过程和失败原因。
        LOGGER.error("未处理异常，类型：{}", error.getClass().getName());
        // 把调用 ResponseEntity.internalServerError 完成内部Server错误处理交还给调用方，本方法到此结束。
        return ResponseEntity.internalServerError().body(ResultVO.failure("INTERNAL_ERROR", "服务处理失败，请稍后重试", null));
    }
}
