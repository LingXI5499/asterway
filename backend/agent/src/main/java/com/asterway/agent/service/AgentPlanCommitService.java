/*
 * 规定成长助手方案提交回执业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service;

// 引入成长助手方案提交回执（AgentPlanCommitDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentPlanCommitDTO;
// 引入成长助手方案提交回执（AgentPlanCommitVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentPlanCommitVO;

/* 已确认方案的 Java 原子批量提交；同一方案版本只提交一次。 */
// 定义接口 AgentPlanCommitService；规定成长助手方案提交回执业务服务可供其他代码调用的业务方法。
public interface AgentPlanCommitService {
    // 提交回执（commit）方法：提交本次数据库事务，使本次修改一起生效；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 AgentPlanCommitDTO 类型保存。
    AgentPlanCommitVO commit(AgentPlanCommitDTO dto);
}
