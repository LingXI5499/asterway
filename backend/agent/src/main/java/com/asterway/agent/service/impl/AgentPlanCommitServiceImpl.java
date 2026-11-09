/*
 * 再次检查用户确认、归属和快照版本，在同一个事务里原子提交整份方案。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.service.impl;

// 引入成长助手方案提交回执（AgentPlanCommitDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.dto.AgentPlanCommitDTO;
// 引入方案提交回执数据库读写器（PlanCommitMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.mapper.PlanCommitMapper;
// 引入成长助手方案提交回执业务服务（AgentPlanCommitService），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.service.AgentPlanCommitService;
// 引入成长助手方案提交回执（AgentPlanCommitVO），下面可以直接使用这个类型或静态成员。
import com.asterway.agent.vo.AgentPlanCommitVO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入方案写入Port（PlanWritePort），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.PlanWritePort;
// 引入操作（Action），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.PlanWritePort.Action;
// 引入账号当前登录身份（AccountPrincipal），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountPrincipal;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入Array节点（ArrayNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.node.ArrayNode;
// 引入对象节点（ObjectNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.node.ObjectNode;
// 引入StandardCharsets，下面可以直接使用这个类型或静态成员。
import java.nio.charset.StandardCharsets;
// 引入消息Digest（MessageDigest），下面可以直接使用这个类型或静态成员。
import java.security.MessageDigest;
// 引入NoSuchAlgorithmException，下面可以直接使用这个类型或静态成员。
import java.security.NoSuchAlgorithmException;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;
// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入摘要值设置（HashSet），下面可以直接使用这个类型或静态成员。
import java.util.HashSet;
// 引入Hex格式（HexFormat），下面可以直接使用这个类型或静态成员。
import java.util.HexFormat;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入Atomic真假值（AtomicBoolean），下面可以直接使用这个类型或静态成员。
import java.util.concurrent.atomic.AtomicBoolean;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入平台事务管理员（PlatformTransactionManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.PlatformTransactionManager;
// 引入事务Template（TransactionTemplate），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionTemplate;

/* 已确认方案在单一事务内批量创建目标、阶段、任务、计划、安排与习惯；幂等键保证不重复创建。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 AgentPlanCommitServiceImpl；再次检查用户确认、归属和快照版本，在同一个事务里原子提交整份方案。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class AgentPlanCommitServiceImpl implements AgentPlanCommitService {
  // 准备INTENTS，类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
  private static final Set<String> INTENTS =
      // 继续传入或组合调用 Set.of 完成of处理，完成上面尚未结束的调用或声明。
      Set.of(
          // 继续传入或组合固定状态或编码 GOAL_PLAN（目标方案），完成上面尚未结束的调用或声明。
          "GOAL_PLAN",
          // 继续传入或组合固定状态或编码 STAGE_PLAN（阶段方案），完成上面尚未结束的调用或声明。
          "STAGE_PLAN",
          // 继续传入或组合固定状态或编码 TASK_PLAN（任务方案），完成上面尚未结束的调用或声明。
          "TASK_PLAN",
          // 继续传入或组合固定状态或编码 WEEKLY_PLAN（每周方案），完成上面尚未结束的调用或声明。
          "WEEKLY_PLAN",
          // 继续传入或组合固定状态或编码 DAILY_PLAN（每日方案），完成上面尚未结束的调用或声明。
          "DAILY_PLAN",
          // 继续传入或组合固定状态或编码 HABIT_PLAN（习惯方案），完成上面尚未结束的调用或声明。
          "HABIT_PLAN",
          // 继续传入或组合表达式 "COMPREHENSIVE_PLAN") 的结果，完成上面尚未结束的调用或声明。
          "COMPREHENSIVE_PLAN");
  // 准备PRIORITIES，类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
  private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
  // 准备最大尝试次数（MAX_ATTEMPTS），类型为 int；初始内容是固定数值 10。
  private static final int MAX_ATTEMPTS = 10;

  // 访问数据库的 Mapper（mapper），使用 PlanCommitMapper 类型保存。
  private final PlanCommitMapper mapper;
  // 失败Recorder（failureRecorder），使用 PlanCommitFailureRecorder 类型保存。
  private final PlanCommitFailureRecorder failureRecorder;
  // 对象数据库读写器（objectMapper），使用 ObjectMapper 类型保存。
  private final ObjectMapper objectMapper;
  // ports，使用 Map<String, PlanWritePort> 类型保存。
  private final Map<String, PlanWritePort> ports;
  // 上下文（context），使用 GrowthContextUtils 类型保存。
  private final GrowthContextUtils context;
  // transactions，使用 TransactionTemplate 类型保存。
  private final TransactionTemplate transactions;

  // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
  public AgentPlanCommitServiceImpl(
      // 访问数据库的 Mapper（mapper），使用 PlanCommitMapper 类型保存。
      PlanCommitMapper mapper,
      // 失败Recorder（failureRecorder），使用 PlanCommitFailureRecorder 类型保存。
      PlanCommitFailureRecorder failureRecorder,
      // 对象数据库读写器（objectMapper），使用 ObjectMapper 类型保存。
      ObjectMapper objectMapper,
      // ports，使用 List<PlanWritePort> 类型保存。
      List<PlanWritePort> ports,
      // 上下文（context），使用 GrowthContextUtils 类型保存。
      GrowthContextUtils context,
      // 事务管理员（transactionManager），使用 PlatformTransactionManager 类型保存。
      PlatformTransactionManager transactionManager) {
    // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
    this.mapper = mapper;
    // 把失败Recorder（failureRecorder）保存到 this.failureRecorder。
    this.failureRecorder = failureRecorder;
    // 把对象数据库读写器（objectMapper）保存到 this.objectMapper。
    this.objectMapper = objectMapper;
    // 把新建 摘要值对应表（HashMap） 实例保存到 this.ports。
    this.ports = new HashMap<>();
    // port，使用 PlanWritePort 类型保存。
    // 逐次执行循环：for (PlanWritePort port : ports) this.ports.put(port.domain(), port);；每轮处理一条数据或推进一次检查。
    // 保存指定键对应的值。
    for (PlanWritePort port : ports) this.ports.put(port.domain(), port);
    // 把上下文（context）保存到 this.context。
    this.context = context;
    // 把新建 事务Template（TransactionTemplate） 实例保存到 this.transactions。
    this.transactions = new TransactionTemplate(transactionManager);
  }

  // 说明此方法实现或改写了父类、接口中已经声明的方法。
  // 提交回执（commit）方法：提交本次数据库事务，使本次修改一起生效；参数负责传入本次处理所需的数据。
  @Override
  // 请求参数对象（dto），使用 AgentPlanCommitDTO 类型保存。
  public AgentPlanCommitVO commit(AgentPlanCommitDTO dto) {
    // 准备当前登录身份（principal），类型为 AccountPrincipal；初始内容是要求满足 权限（Permission）。
    AccountPrincipal principal = SecurityUtils.requirePermission("agent:workflow:manage");
    // 准备用户编号（userId），类型为 long；初始内容是调用 principal.userId 完成用户编号处理。
    long userId = principal.userId();
    // 准备attempted，类型为 AtomicBoolean；初始内容是新建 Atomic真假值（AtomicBoolean） 实例。
    AtomicBoolean attempted = new AtomicBoolean();
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 状态（status），使用 null 类型保存。
      // 把执行传入的数据库语句并取得结果交还给调用方，本方法到此结束。
      // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
      return transactions.execute(status -> commitInTransaction(dto, userId, attempted));
    // 错误（error），使用 RuntimeException 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (RuntimeException error) {
      // execute 返回或抛出时原事务已经提交／回滚，记录失败不会等待自己持有的行锁。
      // 检查条件 attempted.get() 是否成立；成立时执行下面的分支。
      if (attempted.get()) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
          // 调用 failureRecorder.record 完成记录处理。
          failureRecorder.record(
              // 继续传入或组合请求参数对象（dto），完成上面尚未结束的调用或声明。
              dto,
              // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
              userId,
              // business，使用 BusinessException 类型保存。
              error instanceof BusinessException business
                  // 继续补充同一个判断或备用结果：条件 ? business.getCode() 是否成立。
                  ? business.getCode()
                  // 继续补充同一个判断或备用结果：条件 : "PLAN_COMMIT_FAILED", 是否成立。
                  : "PLAN_COMMIT_FAILED",
              // business，使用 BusinessException 类型保存。
              error instanceof BusinessException business ? business.getMessage() : "规划提交失败");
        // recording错误（recordingError），使用 RuntimeException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (RuntimeException recordingError) {
          // 调用 error.addSuppressed 完成加入Suppressed处理。
          error.addSuppressed(recordingError);
        }
      }
      // 中止正常处理并抛出异常：错误（error）；由上层按失败处理。
      throw error;
    }
  }

  // 提交回执In事务（commitInTransaction）方法：调用 commitInTransaction 完成提交回执In事务处理；参数负责传入本次处理所需的数据。
  private AgentPlanCommitVO commitInTransaction(
      // 请求参数对象（dto），使用 AgentPlanCommitDTO 类型保存。
      // 用户编号（userId），使用 long 类型保存。
      // attempted，使用 AtomicBoolean 类型保存。
      AgentPlanCommitDTO dto, long userId, AtomicBoolean attempted) {
    // 把内容写入当前输出或响应。
    context.write("agent:workflow:manage");
    // 调用 validateSnapshot(dto.snapshot 完成确认时的数据快照处理。
    validateSnapshot(dto.snapshot());
    // 检查条件 !sha256(canonical(dto.snapshot()).toString()).equals(dto.requestHash()) 是否成立；成立时执行下面的分支。
    if (!sha256(canonical(dto.snapshot()).toString()).equals(dto.requestHash())) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("提交内容摘要不一致");
    }
    // 准备当前记录（row），类型为 Map<String, Object>；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findByKeyForUpdate。
    Map<String, Object> row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    // 检查条件 row != null 是否成立；成立时执行下面的分支。
    if (row != null) {
      // 检查条件 number(row.get("user_id")) != userId 是否成立；成立时执行下面的分支。
      if (number(row.get("user_id")) != userId) {
        // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
        throw BusinessException.notFound();
      }
      // 检查当前记录版本是否符合请求要求：number(row.get("run_id")) != dto.runId() || number(row.get("plan_version_id")) != dto.planVersionId() || !dto.requestHash().equals(row.get("reque…；成立时执行下面的分支。
      if (number(row.get("run_id")) != dto.runId()
          // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| number(row.get("plan_version_id")) != dto.planVersionId()。
          || number(row.get("plan_version_id")) != dto.planVersionId()
          // 继续补充同一个判断或备用结果：条件 || !dto.requestHash().equals(row.get("request_hash"))) { 是否成立。
          || !dto.requestHash().equals(row.get("request_hash"))) {
        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
        throw BusinessException.conflict("幂等键对应的请求内容不一致");
      }
      // 检查条件 "SUCCEEDED".equals(row.get("status")) 是否成立；成立时执行下面的分支。
      if ("SUCCEEDED".equals(row.get("status"))) {
        // 把新建 成长助手方案提交回执（AgentPlanCommitVO） 实例交还给调用方，本方法到此结束。
        return new AgentPlanCommitVO(
            // 调用 Long.toString 完成to文字处理。
            Long.toString(number(row.get("id"))),
            // 继续传入或组合固定状态或编码 SUCCEEDED（已成功），完成上面尚未结束的调用或声明。
            "SUCCEEDED",
            // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            parseResult(row.get("result_json")),
            // 调用 List.of 完成of处理。
            List.of());
      }
    }
    // 准备已确认（confirmed），类型为 Map<String, Object>；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findConfirmedPlan。
    Map<String, Object> confirmed =
        // 从数据库读取符合条件的记录，具体 SQL 对应 findConfirmedPlan。
        mapper.findConfirmedPlan(userId, dto.runId(), dto.planVersionId());
    // 检查confirmed == null，即对应内容是否不存在；成立时执行下面的分支。
    if (confirmed == null) {
      // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
      throw BusinessException.notFound();
    }
    // 检查条件 !"CONFIRMED".equals(confirmed.get("plan_status")) || !Set.of("CONFIRMED", "EXECUTING", "FAILED").contains(confirmed.get("run_status")) || number(… 是否成立；成立时执行下面的分支。
    if (!"CONFIRMED".equals(confirmed.get("plan_status"))
        // 继续补充同一个判断或备用结果：条件 || !Set.of("CONFIRMED", "EXECUTING", "FAILED").contains(confirmed.get("run_status")) 是否成立。
        || !Set.of("CONFIRMED", "EXECUTING", "FAILED").contains(confirmed.get("run_status"))
        // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| number(confirmed.get("latest_plan_version_id")) != dto.planVersionId()。
        || number(confirmed.get("latest_plan_version_id")) != dto.planVersionId()
        // 继续补充同一个判断或备用结果：记录或凭据是否已经到期：|| Boolean.TRUE.equals(confirmed.get("expired"))) {。
        || Boolean.TRUE.equals(confirmed.get("expired"))) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("方案未确认、已取消、已过期或已经修订");
    }
    // 准备已保存（saved），类型为 JsonNode；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
    JsonNode saved = parseResult(confirmed.get("snapshot_json"));
    // 准备摘要值（hash），类型为 String；初始内容是内容的 SHA-256 摘要，用于核对内容或构造幂等键。
    String hash = sha256(canonical(saved).toString());
    // 准备键（key），类型为 String；初始内容是内容的 SHA-256 摘要，用于核对内容或构造幂等键。
    String key = sha256(dto.runId() + ":" + dto.planVersionId() + ":" + hash);
    // 检查条件 !canonical(saved).toString().equals(canonical(dto.snapshot()).toString()) || !hash.equals(dto.requestHash()) || !key.equals(dto.idempotencyKey()) 是否成立；成立时执行下面的分支。
    if (!canonical(saved).toString().equals(canonical(dto.snapshot()).toString())
        // 继续补充同一个判断或备用结果：条件 || !hash.equals(dto.requestHash()) 是否成立。
        || !hash.equals(dto.requestHash())
        // 继续补充同一个判断或备用结果：条件 || !key.equals(dto.idempotencyKey())) { 是否成立。
        || !key.equals(dto.idempotencyKey())) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("提交内容与用户已确认方案不一致");
    }
    // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
    if (row == null) {
      // 准备写入新记录（insert），类型为 Map<String, Object>；初始内容是新建 摘要值对应表（HashMap） 实例。
      Map<String, Object> insert = new HashMap<>();
      // 保存指定键对应的值。
      insert.put("userId", userId);
      // 保存指定键对应的值。
      insert.put("runId", dto.runId());
      // 保存指定键对应的值。
      insert.put("planVersionId", dto.planVersionId());
      // 保存指定键对应的值。
      insert.put("idempotencyKey", dto.idempotencyKey());
      // 保存指定键对应的值。
      insert.put("requestHash", dto.requestHash());
      // 向数据库插入新记录，具体 SQL 对应 insert。
      mapper.insert(insert);
      // 把从数据库读取符合条件的记录，具体 SQL 对应 findByKeyForUpdate保存到 row。
      row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    }
    // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
    if (row == null) {
      // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
      throw new BusinessException(500, "PLAN_COMMIT_FAILED", "提交记录创建失败");
    }
    // 准备提交回执编号（commitId），类型为 long；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
    long commitId = number(row.get("id"));
    // 检查条件 number(row.get("user_id")) != userId 是否成立；成立时执行下面的分支。
    if (number(row.get("user_id")) != userId) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("提交记录不属于当前用户");
    }
    // 检查条件 !dto.requestHash().equals(String.valueOf(row.get("request_hash"))) 是否成立；成立时执行下面的分支。
    if (!dto.requestHash().equals(String.valueOf(row.get("request_hash")))) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("幂等键对应的请求内容不一致");
    }
    // 检查条件 "SUCCEEDED".equals(row.get("status")) 是否成立；成立时执行下面的分支。
    if ("SUCCEEDED".equals(row.get("status"))) {
      // 把新建 成长助手方案提交回执（AgentPlanCommitVO） 实例交还给调用方，本方法到此结束。
      return new AgentPlanCommitVO(
          // 调用 Long.toString 完成to文字处理。
          Long.toString(commitId), "SUCCEEDED", parseResult(row.get("result_json")), List.of());
    }
    // 检查条件 "RUNNING".equals(row.get("status")) 是否成立；成立时执行下面的分支。
    if ("RUNNING".equals(row.get("status"))) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("方案正在提交中，请稍后重试");
    }
    // 检查条件 number(row.get("attempts")) >= MAX_ATTEMPTS 是否成立；成立时执行下面的分支。
    if (number(row.get("attempts")) >= MAX_ATTEMPTS) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("提交尝试次数已用完，请重新生成方案");
    }
    // 检查条件 mapper.markRunning(commitId, MAX_ATTEMPTS) != 1 是否成立；成立时执行下面的分支。
    if (mapper.markRunning(commitId, MAX_ATTEMPTS) != 1) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("方案提交状态已变化，请刷新后重试");
    }
    // 调用 attempted.set 完成设置处理。
    attempted.set(true);
    // 准备结果（result），类型为 JsonNode；初始内容是创建 Business。
    JsonNode result = createBusiness(saved);
    // 检查条件 mapper.markSucceeded(commitId, result.toString()) != 1 是否成立；成立时执行下面的分支。
    if (mapper.markSucceeded(commitId, result.toString()) != 1) {
      // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
      throw new BusinessException(500, "PLAN_COMMIT_FAILED", "提交结果保存失败");
    }
    // 把新建 成长助手方案提交回执（AgentPlanCommitVO） 实例交还给调用方，本方法到此结束。
    return new AgentPlanCommitVO(Long.toString(commitId), "SUCCEEDED", result, List.of());
  }

  // sha256方法：调用 sha256 完成sha256处理；参数负责传入本次处理所需的数据。
  // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
  private String sha256(String value) {
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 把内容的 SHA-256 摘要，用于核对内容或构造幂等键交还给调用方，本方法到此结束。
      return HexFormat.of()
          // 继续对上一步结果执行格式化 Hex。
          .formatHex(
              // 内容的 SHA-256 摘要，用于核对内容或构造幂等键。
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    // 错误（error），使用 NoSuchAlgorithmException 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (NoSuchAlgorithmException error) {
      // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  // canonical方法：调用 canonical 完成canonical处理；参数负责传入本次处理所需的数据。
  // 值（value），使用 JsonNode 类型保存。
  private JsonNode canonical(JsonNode value) {
    // 检查条件 value.isObject() 是否成立；成立时执行下面的分支。
    if (value.isObject()) {
      // 准备sorted，类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
      ObjectNode sorted = objectMapper.createObjectNode();
      // 准备names，类型为 null；初始内容是新建 Java后端中的util中的Array列表（java.util.ArrayList） 实例。
      var names = new java.util.ArrayList<String>();
      // 调用 value.fieldNames 完成字段Names处理。
      value.fieldNames().forEachRemaining(names::add);
      // 按指定规则排列数据。
      names.sort(String::compareTo);
      // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
      // 逐次执行循环：for (String name : names) sorted.set(name, canonical(value.get(name)));；每轮处理一条数据或推进一次检查。
      // 调用 sorted.set 完成设置处理。
      for (String name : names) sorted.set(name, canonical(value.get(name)));
      // 把sorted交还给调用方，本方法到此结束。
      return sorted;
    }
    // 检查条件 value.isArray() 是否成立；成立时执行下面的分支。
    if (value.isArray()) {
      // 准备array，类型为 ArrayNode；初始内容是创建 Array节点（ArrayNode）。
      ArrayNode array = objectMapper.createArrayNode();
      // 单条数据（item），使用 null 类型保存。
      // 调用 value.forEach 完成forEach处理。
      // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
      value.forEach(item -> array.add(canonical(item)));
      // 把array交还给调用方，本方法到此结束。
      return array;
    }
    // 把值（value）交还给调用方，本方法到此结束。
    return value;
  }

  // 检查合法性确认时的数据快照（validateSnapshot）方法：检查 确认时的数据快照（Snapshot）；参数负责传入本次处理所需的数据。
  // 确认时的数据快照（snapshot），使用 JsonNode 类型保存。
  private void validateSnapshot(JsonNode snapshot) {
    // 检查snapshot == null || !snapshot.isObject()，即对应内容是否不存在；成立时执行下面的分支。
    if (snapshot == null || !snapshot.isObject()) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("方案快照必须是对象");
    }
    // 准备intent，类型为 String；初始内容是调用 text 完成文字处理。
    String intent = text(snapshot, "intent");
    // 检查intent == null || !INTENTS.contains(intent)，即对应内容是否不存在；成立时执行下面的分支。
    if (intent == null || !INTENTS.contains(intent)) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("方案意图无效");
    }
    // 要求满足 Array。
    requireArray(snapshot, "stages");
    // 要求满足 Array。
    requireArray(snapshot, "tasks");
    // 要求满足 Array。
    requireArray(snapshot, "weeklyPlans");
    // 要求满足 Array。
    requireArray(snapshot, "dailyPlans");
    // 要求满足 Array。
    requireArray(snapshot, "taskAssignments");
    // 要求满足 Array。
    requireArray(snapshot, "habits");
    // 准备阶段列表（stages），类型为 JsonNode；初始内容是调用 snapshot.path 完成路径处理。
    JsonNode stages = snapshot.path("stages");
    // 准备键列表（keys），类型为 Set<String>；初始内容是新建 摘要值设置（HashSet） 实例。
    Set<String> keys = new HashSet<>();
    // 检查条件 snapshot.path("goal").isObject() 是否成立；成立时执行下面的分支。
    if (snapshot.path("goal").isObject())
      // 加入集合或登记一条待保存记录。
      keys.add(requiredText(snapshot.path("goal"), "clientKey", "目标 clientKey"));
    // 分组（group），使用 String 类型保存；可为 null，表示尚未提供。
    // 逐次执行循环：for (String group : List.of("stages", "tasks", "weeklyPlans", "dailyPlans", "taskAssignments", "habits"))；每轮处理一条数据或推进一次检查。
    for (String group :
        // 继续传入或组合调用 List.of 完成of处理，完成上面尚未结束的调用或声明。
        List.of("stages", "tasks", "weeklyPlans", "dailyPlans", "taskAssignments", "habits")) {
      // 检查条件 snapshot.path(group).size() > 700 是否成立；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (snapshot.path(group).size() > 700) throw BusinessException.badRequest("方案内容过多，请拆分规划");
      // 单条数据（item），使用 JsonNode 类型保存。
      // 逐次执行循环：for (JsonNode item : snapshot.path(group))；每轮处理一条数据或推进一次检查。
      for (JsonNode item : snapshot.path(group)) {
        // 检查条件 !item.isObject() || !keys.add(requiredText(item, "clientKey", "clientKey")) 是否成立；成立时执行下面的分支。
        if (!item.isObject() || !keys.add(requiredText(item, "clientKey", "clientKey"))) {
          // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
          throw BusinessException.badRequest("方案 clientKey 重复或内容无效");
        }
      }
    }
    // 检查输入或集合是否为空：keys.isEmpty()；成立时执行下面的分支。
    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
    if (keys.isEmpty()) throw BusinessException.badRequest("方案不能为空");
    // 检查条件 "GOAL_PLAN".equals(intent) && !snapshot.path("goal").isObject() 是否成立；成立时执行下面的分支。
    if ("GOAL_PLAN".equals(intent) && !snapshot.path("goal").isObject()) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("目标规划必须包含目标");
    }
    // 检查条件 Set.of("WEEKLY_PLAN", "DAILY_PLAN").contains(intent) && (snapshot.path("goal").isObject() || snapshot.path("stages").size() > 0 || snapshot.path(… 是否成立；成立时执行下面的分支。
    if (Set.of("WEEKLY_PLAN", "DAILY_PLAN").contains(intent)
        // 继续补充同一个判断或备用结果：条件 && (snapshot.path("goal").isObject() 是否成立。
        && (snapshot.path("goal").isObject()
            // 继续补充同一个判断或备用结果：条件 || snapshot.path("stages").size() > 0 是否成立。
            || snapshot.path("stages").size() > 0
            // 继续补充同一个判断或备用结果：条件 || snapshot.path("tasks").size() > 0 是否成立。
            || snapshot.path("tasks").size() > 0
            // 继续补充同一个判断或备用结果：条件 || snapshot.path("habits").size() > 0)) { 是否成立。
            || snapshot.path("habits").size() > 0)) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("计划应安排已有任务，不能复制成长对象");
    }
    // 任务（task），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode task : snapshot.path("tasks"))；每轮处理一条数据或推进一次检查。
    for (JsonNode task : snapshot.path("tasks")) {
      // 检查text(task, "stageKey") == null) == (text(task, "stageId") == null，即对应内容是否不存在；成立时执行下面的分支。
      if ((text(task, "stageKey") == null) == (text(task, "stageId") == null)) {
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        throw BusinessException.badRequest("任务必须且只能引用新阶段或已有阶段");
      }
    }
    // 每日（daily），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode daily : snapshot.path("dailyPlans"))；每轮处理一条数据或推进一次检查。
    for (JsonNode daily : snapshot.path("dailyPlans")) {
      // 检查text(daily, "weekKey") == null || text(daily, "businessDate") == null，即对应内容是否不存在；成立时执行下面的分支。
      if (text(daily, "weekKey") == null || text(daily, "businessDate") == null) {
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        throw BusinessException.badRequest("日计划必须引用周计划并给出日期");
      }
    }
    // 任务安排（assignment），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode assignment : snapshot.path("taskAssignments"))；每轮处理一条数据或推进一次检查。
    for (JsonNode assignment : snapshot.path("taskAssignments")) {
      // 检查(text(assignment, "taskKey") == null) == (text(assignment, "taskId") == null) || text(assignment, "w…，即对应内容是否不存在；成立时执行下面的分支。
      if ((text(assignment, "taskKey") == null) == (text(assignment, "taskId") == null)
          // 继续补充同一个判断或备用结果：|| text(assignment, "weekKey") == null) {，即对应内容是否不存在。
          || text(assignment, "weekKey") == null) {
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        throw BusinessException.badRequest("任务安排必须引用任务与周计划");
      }
    }
    // 习惯（habit），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode habit : snapshot.path("habits"))；每轮处理一条数据或推进一次检查。
    for (JsonNode habit : snapshot.path("habits")) {
      // 检查text(habit, "name") == null，即对应内容是否不存在；成立时执行下面的分支。
      if (text(habit, "name") == null) {
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        throw BusinessException.badRequest("习惯名称不能为空");
      }
    }
  }

  // requireArray方法：要求满足 Array；参数负责传入本次处理所需的数据。
  // 确认时的数据快照（snapshot），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  private void requireArray(JsonNode snapshot, String field) {
    // 准备值（value），类型为 JsonNode；初始内容是调用 snapshot.path 完成路径处理。
    JsonNode value = snapshot.path(field);
    // 检查条件 !value.isMissingNode() && !value.isNull() && !value.isArray() 是否成立；成立时执行下面的分支。
    if (!value.isMissingNode() && !value.isNull() && !value.isArray()) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("方案字段 " + field + " 必须是数组");
    }
  }

  // 创建Business（createBusiness）方法：创建 Business；参数负责传入本次处理所需的数据。
  // 确认时的数据快照（snapshot），使用 JsonNode 类型保存。
  private JsonNode createBusiness(JsonNode snapshot) {
    // 准备结果（result），类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
    ObjectNode result = objectMapper.createObjectNode();
    // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
    // 逐次执行循环：for (String field : List.of("goalIds", "stageIds", "taskIds", "weeklyPlanIds", "dailyPlanIds", "assignmentIds", "habitIds"))；每轮处理一条数据或推进一次检查。
    for (String field : List.of("goalIds", "stageIds", "taskIds", "weeklyPlanIds", "dailyPlanIds", "assignmentIds", "habitIds")) {
      // 调用 result.set 完成设置处理。
      result.set(field, objectMapper.createArrayNode());
    }
    // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
    String goalId = null;
    // 准备目标（goal），类型为 JsonNode；初始内容是调用 snapshot.path 完成路径处理。
    JsonNode goal = snapshot.path("goal");
    // 检查条件 goal.isObject() 是否成立；成立时执行下面的分支。
    if (goal.isObject()) {
      // 要求满足 d文字（dText）。
      requiredText(goal, "title", "目标名称");
      // 准备创建时（created），类型为 JsonNode；初始内容是把内容写入当前输出或响应。
      JsonNode created = write("goal", Action.CREATE, goal, null);
      // 把调用 created.path 完成路径处理保存到 goalId。
      goalId = created.path("id").asText();
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("goalIds")).add(goalId);
    }
    // 准备阶段键列表（stageKeys），类型为 Map<String, String>；初始内容是新建 摘要值对应表（HashMap） 实例。
    Map<String, String> stageKeys = new HashMap<>();
    // 阶段（stage），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode stage : snapshot.path("stages"))；每轮处理一条数据或推进一次检查。
    for (JsonNode stage : snapshot.path("stages")) {
      // 准备上级编号（parentId），类型为 String；初始内容是目标编号（goalId）。
      String parentId = goalId;
      // 准备existing，类型为 String；初始内容是调用 text 完成文字处理。
      String existing = text(stage, "goalId");
      // 检查条件 existing != null 是否成立；成立时执行下面的分支。
      if (existing != null) {
        // 检查条件 goalId != null || text(stage, "goalKey") != null 是否成立；成立时执行下面的分支。
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        if (goalId != null || text(stage, "goalKey") != null) throw BusinessException.badRequest("阶段只能引用一个目标");
        // 调用 context.read 完成读取处理。
        context.read("goal:goal:read");
        // 准备上级（parent），类型为 null；初始内容是调用 context.scope 完成数据范围处理。
        var parent = context.scope("goal", SecurityUtils.current().userId(), positiveId(existing), true);
        // 调用 context.version 完成版本号处理。
        context.version(parent.getVersion(), version(stage, "goalVersion"));
        // 把existing保存到 parentId。
        parentId = existing;
      }
      // 检查parentId == null，即对应内容是否不存在；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (parentId == null) throw BusinessException.badRequest("阶段必须引用目标");
      // 要求满足 d文字（dText）。
      requiredText(stage, "title", "阶段名称");
      // 准备创建时（created），类型为 JsonNode；初始内容是把内容写入当前输出或响应。
      JsonNode created = write("stage", Action.CREATE, stage, positiveId(parentId));
      // 准备编号（id），类型为 String；初始内容是调用 created.path 完成路径处理。
      String id = created.path("id").asText();
      // 保存指定键对应的值。
      stageKeys.put(requiredText(stage, "clientKey", "阶段 clientKey"), id);
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("stageIds")).add(id);
    }
    // 准备任务键列表（taskKeys），类型为 Map<String, String>；初始内容是新建 摘要值对应表（HashMap） 实例。
    Map<String, String> taskKeys = new HashMap<>();
    // 任务（task），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode task : snapshot.path("tasks"))；每轮处理一条数据或推进一次检查。
    for (JsonNode task : snapshot.path("tasks")) {
      // 准备阶段编号（stageId），类型为 String；初始内容是调用 text 完成文字处理。
      String stageId = text(task, "stageId");
      // 检查条件 stageId != null 是否成立；成立时执行下面的分支。
      if (stageId != null) {
        // 调用 context.read 完成读取处理。
        context.read("stage:stage:read");
        // 准备上级（parent），类型为 null；初始内容是调用 context.scope 完成数据范围处理。
        var parent = context.scope("stage", SecurityUtils.current().userId(), positiveId(stageId), true);
        // 调用 context.version 完成版本号处理。
        context.version(parent.getVersion(), version(task, "stageVersion"));
      // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值保存到 stageId。
      } else stageId = stageKeys.get(text(task, "stageKey"));
      // 检查stageId == null，即对应内容是否不存在；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (stageId == null) throw BusinessException.badRequest("任务引用的阶段不存在");
      // 准备priority，类型为 String；初始内容是调用 text 完成文字处理。
      String priority = text(task, "priority");
      // 检查条件 priority != null && !PRIORITIES.contains(priority) 是否成立；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (priority != null && !PRIORITIES.contains(priority)) throw BusinessException.badRequest("任务优先级无效");
      // 要求满足 d文字（dText）。
      requiredText(task, "title", "任务名称");
      // 准备创建时（created），类型为 JsonNode；初始内容是把内容写入当前输出或响应。
      JsonNode created = write("task", Action.CREATE, task, positiveId(stageId));
      // 准备编号（id），类型为 String；初始内容是调用 created.path 完成路径处理。
      String id = created.path("id").asText();
      // 保存指定键对应的值。
      taskKeys.put(requiredText(task, "clientKey", "任务 clientKey"), id);
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("taskIds")).add(id);
    }
    // 准备周键列表（weekKeys），类型为 Map<String, String>；初始内容是新建 摘要值对应表（HashMap） 实例。
    Map<String, String> weekKeys = new HashMap<>();
    // 周（week），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode week : snapshot.path("weeklyPlans"))；每轮处理一条数据或推进一次检查。
    for (JsonNode week : snapshot.path("weeklyPlans")) {
      // 准备日期（date），类型为 LocalDate；初始内容是调用 localDate 完成本地日期处理。
      LocalDate date = localDate(week, "weekStart", "周计划起始日期");
      // 检查date == null || date.getDayOfWeek() != java.time.DayOfWeek.MONDAY，即对应内容是否不存在；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (date == null || date.getDayOfWeek() != java.time.DayOfWeek.MONDAY) throw BusinessException.badRequest("周计划起始日期必须为周一");
      // 准备创建时（created），类型为 JsonNode；初始内容是调用 planning 完成规划处理。
      JsonNode created = planning(Action.WEEK, null, date);
      // 准备编号（id），类型为 String；初始内容是调用 created.path 完成路径处理。
      String id = created.path("id").asText();
      // 保存指定键对应的值。
      weekKeys.put(requiredText(week, "clientKey", "周计划 clientKey"), id);
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("weeklyPlanIds")).add(id);
    }
    // 每日（daily），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode daily : snapshot.path("dailyPlans"))；每轮处理一条数据或推进一次检查。
    for (JsonNode daily : snapshot.path("dailyPlans")) {
      // 准备每周编号（weeklyId），类型为 String；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      String weeklyId = weekKeys.get(requiredText(daily, "weekKey", "日计划所属周"));
      // 准备日期（date），类型为 LocalDate；初始内容是调用 localDate 完成本地日期处理。
      LocalDate date = localDate(daily, "businessDate", "日计划日期");
      // 检查weeklyId == null || date == null，即对应内容是否不存在；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (weeklyId == null || date == null) throw BusinessException.badRequest("日计划引用无效");
      // 准备创建时（created），类型为 JsonNode；初始内容是调用 planning 完成规划处理。
      JsonNode created = planning(Action.DAY, positiveId(weeklyId), date);
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("dailyPlanIds")).add(created.path("id").asText());
    }
    // 任务安排（assignment），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode assignment : snapshot.path("taskAssignments"))；每轮处理一条数据或推进一次检查。
    for (JsonNode assignment : snapshot.path("taskAssignments")) {
      // 准备任务编号（taskId），类型为 String；初始内容是调用 text 完成文字处理。
      String taskId = text(assignment, "taskId");
      // 检查条件 taskId != null 是否成立；成立时执行下面的分支。
      if (taskId != null) {
        // 调用 context.read 完成读取处理。
        context.read("task:task:read");
        // 准备任务（task），类型为 null；初始内容是调用 context.scope 完成数据范围处理。
        var task = context.scope("task", SecurityUtils.current().userId(), positiveId(taskId), true);
        // 调用 context.version 完成版本号处理。
        context.version(task.getVersion(), version(assignment, "taskVersion"));
      // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值保存到 taskId。
      } else taskId = taskKeys.get(text(assignment, "taskKey"));
      // 准备每周编号（weeklyId），类型为 String；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      String weeklyId = weekKeys.get(text(assignment, "weekKey"));
      // 检查taskId == null || weeklyId == null，即对应内容是否不存在；成立时执行下面的分支。
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      if (taskId == null || weeklyId == null) throw BusinessException.badRequest("任务安排引用无效");
      // 每日编号（dailyId），使用 String 类型保存；可为 null，表示尚未提供。
      String dailyId = null;
      // 准备日期（date），类型为 LocalDate；初始内容是调用 localDate 完成本地日期处理。
      LocalDate date = localDate(assignment, "businessDate", "安排日期");
      // 检查条件 date != null 是否成立；成立时执行下面的分支。
      // 把调用 planning(Action.DAY, positiveId(weeklyId), date).path 完成路径处理保存到 dailyId。
      if (date != null) dailyId = planning(Action.DAY, positiveId(weeklyId), date).path("id").asText();
      // 准备当前（current），类型为 JsonNode；初始内容是调用 planning 完成规划处理。
      JsonNode current = planning(Action.CURRENT_ASSIGNMENT, positiveId(taskId), null);
      // 检查条件 !current.isNull() 是否成立；成立时执行下面的分支。
      if (!current.isNull()) {
        // 检查条件 Objects.equals(text(current, "weeklyPlanId"), weeklyId) && Objects.equals(text(current, "dailyPlanId"), dailyId) 是否成立；成立时执行下面的分支。
        if (Objects.equals(text(current, "weeklyPlanId"), weeklyId)
            // 继续补充同一个判断或备用结果：条件 && Objects.equals(text(current, "dailyPlanId"), dailyId)) { 是否成立。
            && Objects.equals(text(current, "dailyPlanId"), dailyId)) {
          // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
          ((ArrayNode) result.get("assignmentIds")).add(current.path("id").asText());
          // 继续传入或组合continue，完成上面尚未结束的调用或声明。
          continue;
        }
        // 检查条件 !current.path("id").asText().equals(text(assignment, "assignmentId")) 是否成立；成立时执行下面的分支。
        if (!current.path("id").asText().equals(text(assignment, "assignmentId"))) {
          // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
          throw BusinessException.conflict("任务原安排已变化，请重新核对并接受方案");
        }
        // 调用 context.version 完成版本号处理。
        context.version(current.path("version").asLong(), version(assignment, "assignmentVersion"));
      // 检查条件 text(assignment, "assignmentId") != null 是否成立；成立时执行下面的分支。
      } else if (text(assignment, "assignmentId") != null) {
        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
        throw BusinessException.conflict("原任务安排已变化，请重新确认方案");
      }
      // 准备数据（data），类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
      ObjectNode data = objectMapper.createObjectNode();
      // 保存指定键对应的值。
      data.put("weeklyPlanId", positiveId(weeklyId));
      // 检查条件 dailyId != null 是否成立；成立时执行下面的分支。
      // 保存指定键对应的值。
      if (dailyId != null) data.put("dailyPlanId", positiveId(dailyId));
      // 检查条件 !current.isNull() 是否成立；成立时执行下面的分支。
      if (!current.isNull()) {
        // 保存指定键对应的值。
        data.put("assignmentId", current.path("id").asLong());
        // 保存指定键对应的值。
        data.put("version", current.path("version").asLong());
      }
      // 检查条件 assignment.hasNonNull("sortOrder") 是否成立；成立时执行下面的分支。
      // 调用 data.set 完成设置处理。
      if (assignment.hasNonNull("sortOrder")) data.set("sortOrder", assignment.get("sortOrder"));
      // 准备创建时（created），类型为 JsonNode；初始内容是把内容写入当前输出或响应。
      JsonNode created = write("planning", Action.ASSIGN, data, positiveId(taskId));
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("assignmentIds")).add(created.path("id").asText());
    }
    // 习惯（habit），使用 JsonNode 类型保存。
    // 逐次执行循环：for (JsonNode habit : snapshot.path("habits"))；每轮处理一条数据或推进一次检查。
    for (JsonNode habit : snapshot.path("habits")) {
      // 准备数据（data），类型为 ObjectNode；初始内容是调用 habit.deepCopy 完成deepCopy处理。
      ObjectNode data = habit.deepCopy();
      // 要求满足 d文字（dText）。
      requiredText(data, "name", "习惯名称");
      // 检查条件 !data.hasNonNull("cycleType") 是否成立；成立时执行下面的分支。
      // 保存指定键对应的值。
      if (!data.hasNonNull("cycleType")) data.put("cycleType", "DAILY");
      // 准备分类（category），类型为 String；初始内容是调用 text 完成文字处理。
      String category = text(habit, "categoryName");
      // 检查条件 category != null 是否成立；成立时执行下面的分支。
      if (category != null) {
        // 准备command，类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
        ObjectNode command = objectMapper.createObjectNode().put("name", category);
        // 保存指定键对应的值。
        data.put("categoryId", port("habit").write(Action.CATEGORY, command).path("id").asLong());
      }
      // 准备创建时（created），类型为 JsonNode；初始内容是把内容写入当前输出或响应。
      JsonNode created = write("habit", Action.CREATE, data, null);
      // 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
      ((ArrayNode) result.get("habitIds")).add(created.path("id").asText());
    }
    // 把结果（result）交还给调用方，本方法到此结束。
    return result;
  }

  // port方法：调用 port 完成port处理；参数负责传入本次处理所需的数据。
  // domain，使用 String 类型保存；可为 null，表示尚未提供。
  private PlanWritePort port(String domain) {
    // 准备port，类型为 PlanWritePort；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
    PlanWritePort port = ports.get(domain);
    // 检查port == null，即对应内容是否不存在；成立时执行下面的分支。
    // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
    if (port == null) throw new BusinessException(503, "PLAN_MODULE_UNAVAILABLE", "方案涉及的业务模块尚未就绪");
    // 把port交还给调用方，本方法到此结束。
    return port;
  }

  // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
  // domain，使用 String 类型保存；可为 null，表示尚未提供。
  // 操作（action），使用 Action 类型保存。
  // 数据（data），使用 JsonNode 类型保存。
  // 上级编号（parentId），使用 Long 类型保存；可为 null，表示尚未提供。
  private JsonNode write(String domain, Action action, JsonNode data, Long parentId) {
    // 准备command，类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
    ObjectNode command = objectMapper.createObjectNode();
    // 调用 command.set 完成设置处理。
    command.set("data", data);
    // 检查条件 parentId != null 是否成立；成立时执行下面的分支。
    // 保存指定键对应的值。
    if (parentId != null) command.put("parentId", parentId);
    // 把把内容写入当前输出或响应交还给调用方，本方法到此结束。
    return port(domain).write(action, command);
  }

  // 规划（planning）方法：调用 planning 完成规划处理；参数负责传入本次处理所需的数据。
  // 操作（action），使用 Action 类型保存。
  // 上级编号（parentId），使用 Long 类型保存；可为 null，表示尚未提供。
  // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
  private JsonNode planning(Action action, Long parentId, LocalDate date) {
    // 准备command，类型为 ObjectNode；初始内容是创建 对象节点（ObjectNode）。
    ObjectNode command = objectMapper.createObjectNode();
    // 检查条件 parentId != null 是否成立；成立时执行下面的分支。
    // 保存指定键对应的值。
    if (parentId != null) command.put("parentId", parentId);
    // 检查条件 date != null 是否成立；成立时执行下面的分支。
    // 保存指定键对应的值。
    if (date != null) command.put("date", date.toString());
    // 把把内容写入当前输出或响应交还给调用方，本方法到此结束。
    return port("planning").write(action, command);
  }

  // 正数编号（positiveId）方法：调用 positiveId 完成正数编号处理；参数负责传入本次处理所需的数据。
  // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
  private long positiveId(String value) {
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 准备parsed，类型为 long；初始内容是把输入转换成数值，供比较、计算或按编号查询。
      long parsed = Long.parseLong(value);
      // 检查条件 parsed <= 0 是否成立；成立时执行下面的分支。
      // 中止正常处理并抛出异常：新建 数值格式Exception（NumberFormatException） 实例；由上层按失败处理。
      if (parsed <= 0) throw new NumberFormatException();
      // 把parsed交还给调用方，本方法到此结束。
      return parsed;
    // 错误（error），使用 NumberFormatException 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (NumberFormatException error) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest("业务标识必须是正整数");
    }
  }

  // 版本号（version）方法：调用 version 完成版本号处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  private Long version(JsonNode node, String field) {
    // 准备值（value），类型为 JsonNode；初始内容是调用 node.path 完成路径处理。
    JsonNode value = node.path(field);
    // 检查条件 !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0 是否成立；成立时执行下面的分支。
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
      // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
      throw BusinessException.conflict("已有业务对象必须提供有效版本");
    }
    // 把调用 value.longValue 完成长整数值处理交还给调用方，本方法到此结束。
    return value.longValue();
  }

  // 解析结果（parseResult）方法：解析 结果（Result）；参数负责传入本次处理所需的数据。
  // 值（value），使用 Object 类型保存。
  private JsonNode parseResult(Object value) {
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
      if (value == null) {
        // 把创建 对象节点（ObjectNode）交还给调用方，本方法到此结束。
        return objectMapper.createObjectNode();
      }
      // 把把 JSON 文字解析为可按字段读取的节点交还给调用方，本方法到此结束。
      return objectMapper.readTree(String.valueOf(value));
    // 错误（error），使用 Exception 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (Exception error) {
      // 把创建 对象节点（ObjectNode）交还给调用方，本方法到此结束。
      return objectMapper.createObjectNode();
    }
  }

  // 数值（number）方法：调用 number 完成数值处理；参数负责传入本次处理所需的数据。
  // 值（value），使用 Object 类型保存。
  private long number(Object value) {
    // 数值（number），使用 Number 类型保存。
    // 检查条件 value instanceof Number number 是否成立；成立时执行下面的分支。
    if (value instanceof Number number) {
      // 把调用 number.longValue 完成长整数值处理交还给调用方，本方法到此结束。
      return number.longValue();
    }
    // 把把输入转换成数值，供比较、计算或按编号查询交还给调用方，本方法到此结束。
    return Long.parseLong(String.valueOf(value));
  }

  // 文字（text）方法：调用 text 完成文字处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  private String text(JsonNode node, String field) {
    // 准备值（value），类型为 JsonNode；初始内容是调用 node.path 完成路径处理。
    JsonNode value = node.path(field);
    // 检查条件 value.isMissingNode() || value.isNull() 是否成立；成立时执行下面的分支。
    if (value.isMissingNode() || value.isNull()) {
      // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
      return null;
    }
    // 准备文字（text），类型为 String；初始内容是调用 value.asText 完成作为文字处理。
    String text = value.asText().trim();
    // 把根据前面的条件，从两个候选结果中选择一个：text.isEmpty() ? null : text交还给调用方，本方法到此结束。
    return text.isEmpty() ? null : text;
  }

  // 必填文字（requiredText）方法：要求满足 d文字（dText）；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  // 显示名称（label），使用 String 类型保存；可为 null，表示尚未提供。
  private String requiredText(JsonNode node, String field, String label) {
    // 准备值（value），类型为 String；初始内容是调用 text 完成文字处理。
    String value = text(node, field);
    // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
    if (value == null) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest(label + "不能为空");
    }
    // 把值（value）交还给调用方，本方法到此结束。
    return value;
  }

  // 本地日期（localDate）方法：调用 localDate 完成本地日期处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  // 显示名称（label），使用 String 类型保存；可为 null，表示尚未提供。
  private LocalDate localDate(JsonNode node, String field, String label) {
    // 准备值（value），类型为 String；初始内容是调用 text 完成文字处理。
    String value = text(node, field);
    // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
    if (value == null) {
      // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
      return null;
    }
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 把调用 LocalDate.parse 完成解析处理交还给调用方，本方法到此结束。
      return LocalDate.parse(value);
    // 错误（error），使用 RuntimeException 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (RuntimeException error) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest(label + "格式必须是 YYYY-MM-DD");
    }
  }

  // 本地时间（localTime）方法：调用 localTime 完成本地时间处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  // 显示名称（label），使用 String 类型保存；可为 null，表示尚未提供。
  private LocalTime localTime(JsonNode node, String field, String label) {
    // 准备值（value），类型为 String；初始内容是调用 text 完成文字处理。
    String value = text(node, field);
    // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
    if (value == null) {
      // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
      return null;
    }
    // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
    try {
      // 把调用 LocalTime.parse 完成解析处理交还给调用方，本方法到此结束。
      return LocalTime.parse(value);
    // 错误（error），使用 RuntimeException 类型保存。
    // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
    } catch (RuntimeException error) {
      // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
      throw BusinessException.badRequest(label + "格式必须是 HH:mm");
    }
  }

  // 整数（integer）方法：调用 integer 完成整数处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  private Integer integer(JsonNode node, String field) {
    // 准备值（value），类型为 JsonNode；初始内容是调用 node.path 完成路径处理。
    JsonNode value = node.path(field);
    // 把根据前面的条件，从两个候选结果中选择一个：value.isInt() || value.isLong() ? value.asInt() : null交还给调用方，本方法到此结束。
    return value.isInt() || value.isLong() ? value.asInt() : null;
  }

  // 正数整数（positiveInteger）方法：调用 positiveInteger 完成正数整数处理；参数负责传入本次处理所需的数据。
  // 节点（node），使用 JsonNode 类型保存。
  // 字段（field），使用 String 类型保存；可为 null，表示尚未提供。
  private Integer positiveInteger(JsonNode node, String field) {
    // 准备值（value），类型为 Integer；初始内容是调用 integer 完成整数处理。
    Integer value = integer(node, field);
    // 把根据前面的条件，从两个候选结果中选择一个：value != null && value > 0 ? value : null交还给调用方，本方法到此结束。
    return value != null && value > 0 ? value : null;
  }
}
