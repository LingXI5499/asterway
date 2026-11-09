/*
 * 实现方案提交回执失败Recorder业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service.impl;

// 引入成长助手方案提交回执（AgentPlanCommitDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentPlanCommitDTO;
// 引入方案提交回执数据库读写器（PlanCommitMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.mapper.PlanCommitMapper;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

/* 失败记录使用独立事务，保证业务回滚后仍留下可重试的失败结果。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 PlanCommitFailureRecorder；实现方案提交回执失败Recorder业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class PlanCommitFailureRecorder {
  // 访问数据库的 Mapper（mapper），使用 PlanCommitMapper 类型保存。
  private final PlanCommitMapper mapper;
  // 上下文（context），使用 GrowthContextUtils 类型保存。
  private final GrowthContextUtils context;

  // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
  // 访问数据库的 Mapper（mapper），使用 PlanCommitMapper 类型保存。
  // 上下文（context），使用 GrowthContextUtils 类型保存。
  public PlanCommitFailureRecorder(PlanCommitMapper mapper, GrowthContextUtils context) {
    // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
    this.mapper = mapper;
    // 把上下文（context）保存到 this.context。
    this.context = context;
  }

  // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
  // 记录（record）方法：调用 record 完成记录处理；参数负责传入本次处理所需的数据。
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  // 请求参数对象（dto），使用 AgentPlanCommitDTO 类型保存。
  // 用户编号（userId），使用 long 类型保存。
  // 错误编码（errorCode），使用 String 类型保存；可为 null，表示尚未提供。
  // 错误消息（errorMessage），使用 String 类型保存；可为 null，表示尚未提供。
  public void record(AgentPlanCommitDTO dto, long userId, String errorCode, String errorMessage) {
    // 调用方业务事务必须已经结束，再以相同锁顺序记录失败。
    // 调用 context.lockLifecycleUser 完成加锁Lifecycle用户处理。
    context.lockLifecycleUser(userId);
    // 准备当前记录（row），类型为 null；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findByKeyForUpdate。
    var row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
    if (row == null) {
      // 准备值列表（values），类型为 null；初始内容是新建 摘要值对应表（HashMap） 实例。
      var values = new HashMap<String, Object>();
      // 保存指定键对应的值。
      values.put("userId", userId);
      // 保存指定键对应的值。
      values.put("runId", dto.runId());
      // 保存指定键对应的值。
      values.put("planVersionId", dto.planVersionId());
      // 保存指定键对应的值。
      values.put("idempotencyKey", dto.idempotencyKey());
      // 保存指定键对应的值。
      values.put("requestHash", dto.requestHash());
      // 向数据库插入新记录，具体 SQL 对应 insert。
      mapper.insert(values);
      // 把从数据库读取符合条件的记录，具体 SQL 对应 findByKeyForUpdate保存到 row。
      row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    }
    // 检查row == null || ((Number)row.get("user_id")).longValue() != userId || !dto.requestHash().equals(row.g…，即对应内容是否不存在；成立时执行下面的分支。
    if (row == null
        // 继续补充同一个判断或备用结果：条件 || ((Number) row.get("user_id")).longValue() != userId 是否成立。
        || ((Number) row.get("user_id")).longValue() != userId
        // 继续补充同一个判断或备用结果：条件 || !dto.requestHash().equals(row.get("request_hash")) 是否成立。
        || !dto.requestHash().equals(row.get("request_hash"))
        // 继续补充同一个判断或备用结果：条件 || "SUCCEEDED".equals(row.get("status"))) { 是否成立。
        || "SUCCEEDED".equals(row.get("status"))) {
      // 把返回（return）交还给调用方，本方法到此结束。
      return;
    }
    // 准备编码（code），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：errorCode == null || errorCode.isBlank() ? "PLAN_COMMIT_FAILED" : errorCode。
    String code = errorCode == null || errorCode.isBlank() ? "PLAN_COMMIT_FAILED" : errorCode;
    // 准备消息（message），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：errorMessage == null || errorMessage.isBlank() ? "U+89c4U+5212U+63d0U+4ea4U+5931U+8d25" : errorMessa…。
    String message = errorMessage == null || errorMessage.isBlank() ? "规划提交失败" : errorMessage;
    // 调用 mapper.markFailed 完成mark失败处理。
    mapper.markFailed(
        // 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的调用或声明。
        ((Number) row.get("id")).longValue(),
        // 继续传入或组合编码（code），完成上面尚未结束的调用或声明。
        code,
        // 根据前面的条件，从两个候选结果中选择一个：message.length() > 1000 ? message.substring(0, 1000) : message)。
        message.length() > 1000 ? message.substring(0, 1000) : message);
  }
}
