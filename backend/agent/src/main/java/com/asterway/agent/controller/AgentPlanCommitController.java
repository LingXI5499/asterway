/*
 * 提供成长助手方案提交回执请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.controller;

// 引入成长助手方案提交回执（AgentPlanCommitDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentPlanCommitDTO;
// 引入成长助手方案提交回执业务服务（AgentPlanCommitService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentPlanCommitService;
// 引入成长助手方案提交回执（AgentPlanCommitVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentPlanCommitVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;

// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;

// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 已确认规划方案的批量提交入口；只允许普通用户本人、需要 agent:workflow:manage。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 AgentPlanCommitController；提供成长助手方案提交回执请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/agent/plans")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentPlanCommitController {
    // 业务服务（service），使用 AgentPlanCommitService 类型保存。
    private final AgentPlanCommitService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 AgentPlanCommitService 类型保存。
    public AgentPlanCommitController(AgentPlanCommitService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 提交回执（commit）方法：提交本次数据库事务，使本次修改一起生效；参数负责传入本次处理所需的数据。
    @PostMapping("/commit")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 AgentPlanCommitDTO 类型保存。
    public ResultVO<AgentPlanCommitVO> commit(@Valid @RequestBody AgentPlanCommitDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.commit(dto));
    }
}
