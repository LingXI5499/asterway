/*
 * 实现阶段方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.stage.service.impl;

// 引入方案写入Port（PlanWritePort），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.PlanWritePort;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入方案写入提交数据Utils（PlanWritePayloadUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.PlanWritePayloadUtils;
// 引入阶段保存（StageSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.dto.StageSaveDTO;
// 引入阶段业务服务（StageService），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.service.StageService;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;

/* 已确认方案的stage写入适配，业务规则仍由现有 Service 执行。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 StagePlanWriteServiceImpl；实现阶段方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class StagePlanWriteServiceImpl implements PlanWritePort {
    // 业务服务（service），使用 StageService 类型保存。
    private final StageService service;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 StageService 类型保存。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public StagePlanWriteServiceImpl(StageService service, ObjectMapper json) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // domain方法：调用 domain 完成domain处理；参数负责传入本次处理所需的数据。
    @Override
    // 把固定文字 'stage'交还给调用方，本方法到此结束。
    public String domain() { return "stage"; }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    @Override
    // 操作（action），使用 Action 类型保存。
    // command，使用 JsonNode 类型保存。
    public JsonNode write(Action action, JsonNode command) {
        // 检查条件 action != Action.CREATE 是否成立；成立时执行下面的分支。
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        if (action != Action.CREATE) throw BusinessException.badRequest("不支持的方案操作");
        // 准备请求参数对象（dto），类型为 null；初始内容是保存 请求参数对象（DTO）。
        var dto = PlanWritePayloadUtils.saveDTO(command.path("data"), StageSaveDTO.class, json);
        // 把调用 json.valueToTree 完成值ToTree处理交还给调用方，本方法到此结束。
        return json.valueToTree(service.create(command.path("parentId").asLong(), dto));
    }
}
