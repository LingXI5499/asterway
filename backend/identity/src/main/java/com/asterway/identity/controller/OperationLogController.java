/*
 * 提供操作日志请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入日志查询条件（LogQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LogQueryDTO;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入操作日志（OperationLogVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.OperationLogVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入提前Authorize（PreAuthorize），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.prepost.PreAuthorize;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 操作审计只提供按范围查询。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 OperationLogController；提供操作日志请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/operation-logs")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class OperationLogController {

    // 业务服务（service），使用 OperationLogService 类型保存。
    private final OperationLogService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 OperationLogService 类型保存。
    public OperationLogController(OperationLogService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 分页查询可见审计记录。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:operation_log:list')")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 LogQueryDTO 类型保存。
    public ResultVO<PageResultVO<OperationLogVO>> page(@Valid @ModelAttribute LogQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }
}
