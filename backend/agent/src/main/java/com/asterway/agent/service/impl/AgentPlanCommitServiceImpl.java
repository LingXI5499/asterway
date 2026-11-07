package com.asterway.agent.service.impl;

import com.asterway.agent.dto.AgentPlanCommitDTO;
import com.asterway.agent.mapper.PlanCommitMapper;
import com.asterway.agent.service.AgentPlanCommitService;
import com.asterway.agent.vo.AgentPlanCommitVO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.integration.PlanWritePort.Action;
import com.asterway.common.security.AccountPrincipal;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 已确认方案在单一事务内批量创建目标、阶段、任务、计划、安排与习惯；幂等键保证不重复创建。 */
@Service
public class AgentPlanCommitServiceImpl implements AgentPlanCommitService {
  private static final Set<String> INTENTS =
      Set.of(
          "GOAL_PLAN",
          "STAGE_PLAN",
          "TASK_PLAN",
          "WEEKLY_PLAN",
          "DAILY_PLAN",
          "HABIT_PLAN",
          "COMPREHENSIVE_PLAN");
  private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
  private static final int MAX_ATTEMPTS = 10;

  private final PlanCommitMapper mapper;
  private final PlanCommitFailureRecorder failureRecorder;
  private final ObjectMapper objectMapper;
  private final Map<String, PlanWritePort> ports;
  private final GrowthContextUtils context;
  private final TransactionTemplate transactions;

  public AgentPlanCommitServiceImpl(
      PlanCommitMapper mapper,
      PlanCommitFailureRecorder failureRecorder,
      ObjectMapper objectMapper,
      List<PlanWritePort> ports,
      GrowthContextUtils context,
      PlatformTransactionManager transactionManager) {
    this.mapper = mapper;
    this.failureRecorder = failureRecorder;
    this.objectMapper = objectMapper;
    this.ports = new HashMap<>();
    for (PlanWritePort port : ports) this.ports.put(port.domain(), port);
    this.context = context;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @Override
  public AgentPlanCommitVO commit(AgentPlanCommitDTO dto) {
    AccountPrincipal principal = SecurityUtils.requirePermission("agent:workflow:manage");
    long userId = principal.userId();
    AtomicBoolean attempted = new AtomicBoolean();
    try {
      return transactions.execute(status -> commitInTransaction(dto, userId, attempted));
    } catch (RuntimeException error) {
      // execute 返回或抛出时原事务已经提交／回滚，记录失败不会等待自己持有的行锁。
      if (attempted.get()) {
        try {
          failureRecorder.record(
              dto,
              userId,
              error instanceof BusinessException business
                  ? business.getCode()
                  : "PLAN_COMMIT_FAILED",
              error instanceof BusinessException business ? business.getMessage() : "规划提交失败");
        } catch (RuntimeException recordingError) {
          error.addSuppressed(recordingError);
        }
      }
      throw error;
    }
  }

  private AgentPlanCommitVO commitInTransaction(
      AgentPlanCommitDTO dto, long userId, AtomicBoolean attempted) {
    context.write("agent:workflow:manage");
    validateSnapshot(dto.snapshot());
    if (!sha256(canonical(dto.snapshot()).toString()).equals(dto.requestHash())) {
      throw BusinessException.conflict("提交内容摘要不一致");
    }
    Map<String, Object> row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    if (row != null) {
      if (number(row.get("user_id")) != userId) {
        throw BusinessException.notFound();
      }
      if (number(row.get("run_id")) != dto.runId()
          || number(row.get("plan_version_id")) != dto.planVersionId()
          || !dto.requestHash().equals(row.get("request_hash"))) {
        throw BusinessException.conflict("幂等键对应的请求内容不一致");
      }
      if ("SUCCEEDED".equals(row.get("status"))) {
        return new AgentPlanCommitVO(
            Long.toString(number(row.get("id"))),
            "SUCCEEDED",
            parseResult(row.get("result_json")),
            List.of());
      }
    }
    Map<String, Object> confirmed =
        mapper.findConfirmedPlan(userId, dto.runId(), dto.planVersionId());
    if (confirmed == null) {
      throw BusinessException.notFound();
    }
    if (!"CONFIRMED".equals(confirmed.get("plan_status"))
        || !Set.of("CONFIRMED", "EXECUTING", "FAILED").contains(confirmed.get("run_status"))
        || number(confirmed.get("latest_plan_version_id")) != dto.planVersionId()
        || Boolean.TRUE.equals(confirmed.get("expired"))) {
      throw BusinessException.conflict("方案未确认、已取消、已过期或已经修订");
    }
    JsonNode saved = parseResult(confirmed.get("snapshot_json"));
    String hash = sha256(canonical(saved).toString());
    String key = sha256(dto.runId() + ":" + dto.planVersionId() + ":" + hash);
    if (!canonical(saved).toString().equals(canonical(dto.snapshot()).toString())
        || !hash.equals(dto.requestHash())
        || !key.equals(dto.idempotencyKey())) {
      throw BusinessException.conflict("提交内容与用户已确认方案不一致");
    }
    if (row == null) {
      Map<String, Object> insert = new HashMap<>();
      insert.put("userId", userId);
      insert.put("runId", dto.runId());
      insert.put("planVersionId", dto.planVersionId());
      insert.put("idempotencyKey", dto.idempotencyKey());
      insert.put("requestHash", dto.requestHash());
      mapper.insert(insert);
      row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    }
    if (row == null) {
      throw new BusinessException(500, "PLAN_COMMIT_FAILED", "提交记录创建失败");
    }
    long commitId = number(row.get("id"));
    if (number(row.get("user_id")) != userId) {
      throw BusinessException.conflict("提交记录不属于当前用户");
    }
    if (!dto.requestHash().equals(String.valueOf(row.get("request_hash")))) {
      throw BusinessException.conflict("幂等键对应的请求内容不一致");
    }
    if ("SUCCEEDED".equals(row.get("status"))) {
      return new AgentPlanCommitVO(
          Long.toString(commitId), "SUCCEEDED", parseResult(row.get("result_json")), List.of());
    }
    if ("RUNNING".equals(row.get("status"))) {
      throw BusinessException.conflict("方案正在提交中，请稍后重试");
    }
    if (number(row.get("attempts")) >= MAX_ATTEMPTS) {
      throw BusinessException.conflict("提交尝试次数已用完，请重新生成方案");
    }
    if (mapper.markRunning(commitId, MAX_ATTEMPTS) != 1) {
      throw BusinessException.conflict("方案提交状态已变化，请刷新后重试");
    }
    attempted.set(true);
    JsonNode result = createBusiness(saved);
    if (mapper.markSucceeded(commitId, result.toString()) != 1) {
      throw new BusinessException(500, "PLAN_COMMIT_FAILED", "提交结果保存失败");
    }
    return new AgentPlanCommitVO(Long.toString(commitId), "SUCCEEDED", result, List.of());
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  private JsonNode canonical(JsonNode value) {
    if (value.isObject()) {
      ObjectNode sorted = objectMapper.createObjectNode();
      var names = new java.util.ArrayList<String>();
      value.fieldNames().forEachRemaining(names::add);
      names.sort(String::compareTo);
      for (String name : names) sorted.set(name, canonical(value.get(name)));
      return sorted;
    }
    if (value.isArray()) {
      ArrayNode array = objectMapper.createArrayNode();
      value.forEach(item -> array.add(canonical(item)));
      return array;
    }
    return value;
  }

  private void validateSnapshot(JsonNode snapshot) {
    if (snapshot == null || !snapshot.isObject()) {
      throw BusinessException.badRequest("方案快照必须是对象");
    }
    String intent = text(snapshot, "intent");
    if (intent == null || !INTENTS.contains(intent)) {
      throw BusinessException.badRequest("方案意图无效");
    }
    requireArray(snapshot, "stages");
    requireArray(snapshot, "tasks");
    requireArray(snapshot, "weeklyPlans");
    requireArray(snapshot, "dailyPlans");
    requireArray(snapshot, "taskAssignments");
    requireArray(snapshot, "habits");
    JsonNode stages = snapshot.path("stages");
    Set<String> keys = new HashSet<>();
    if (snapshot.path("goal").isObject())
      keys.add(requiredText(snapshot.path("goal"), "clientKey", "目标 clientKey"));
    for (String group :
        List.of("stages", "tasks", "weeklyPlans", "dailyPlans", "taskAssignments", "habits")) {
      if (snapshot.path(group).size() > 700) throw BusinessException.badRequest("方案内容过多，请拆分规划");
      for (JsonNode item : snapshot.path(group)) {
        if (!item.isObject() || !keys.add(requiredText(item, "clientKey", "clientKey"))) {
          throw BusinessException.badRequest("方案 clientKey 重复或内容无效");
        }
      }
    }
    if (keys.isEmpty()) throw BusinessException.badRequest("方案不能为空");
    if ("GOAL_PLAN".equals(intent) && !snapshot.path("goal").isObject()) {
      throw BusinessException.badRequest("目标规划必须包含目标");
    }
    if (Set.of("WEEKLY_PLAN", "DAILY_PLAN").contains(intent)
        && (snapshot.path("goal").isObject()
            || snapshot.path("stages").size() > 0
            || snapshot.path("tasks").size() > 0
            || snapshot.path("habits").size() > 0)) {
      throw BusinessException.badRequest("计划应安排已有任务，不能复制成长对象");
    }
    for (JsonNode task : snapshot.path("tasks")) {
      if ((text(task, "stageKey") == null) == (text(task, "stageId") == null)) {
        throw BusinessException.badRequest("任务必须且只能引用新阶段或已有阶段");
      }
    }
    for (JsonNode daily : snapshot.path("dailyPlans")) {
      if (text(daily, "weekKey") == null || text(daily, "businessDate") == null) {
        throw BusinessException.badRequest("日计划必须引用周计划并给出日期");
      }
    }
    for (JsonNode assignment : snapshot.path("taskAssignments")) {
      if ((text(assignment, "taskKey") == null) == (text(assignment, "taskId") == null)
          || text(assignment, "weekKey") == null) {
        throw BusinessException.badRequest("任务安排必须引用任务与周计划");
      }
    }
    for (JsonNode habit : snapshot.path("habits")) {
      if (text(habit, "name") == null) {
        throw BusinessException.badRequest("习惯名称不能为空");
      }
    }
  }

  private void requireArray(JsonNode snapshot, String field) {
    JsonNode value = snapshot.path(field);
    if (!value.isMissingNode() && !value.isNull() && !value.isArray()) {
      throw BusinessException.badRequest("方案字段 " + field + " 必须是数组");
    }
  }

  private JsonNode createBusiness(JsonNode snapshot) {
    ObjectNode result = objectMapper.createObjectNode();
    for (String field : List.of("goalIds", "stageIds", "taskIds", "weeklyPlanIds", "dailyPlanIds", "assignmentIds", "habitIds")) {
      result.set(field, objectMapper.createArrayNode());
    }
    String goalId = null;
    JsonNode goal = snapshot.path("goal");
    if (goal.isObject()) {
      requiredText(goal, "title", "目标名称");
      JsonNode created = write("goal", Action.CREATE, goal, null);
      goalId = created.path("id").asText();
      ((ArrayNode) result.get("goalIds")).add(goalId);
    }
    Map<String, String> stageKeys = new HashMap<>();
    for (JsonNode stage : snapshot.path("stages")) {
      String parentId = goalId;
      String existing = text(stage, "goalId");
      if (existing != null) {
        if (goalId != null || text(stage, "goalKey") != null) throw BusinessException.badRequest("阶段只能引用一个目标");
        context.read("goal:goal:read");
        var parent = context.scope("goal", SecurityUtils.current().userId(), positiveId(existing), true);
        context.version(parent.getVersion(), version(stage, "goalVersion"));
        parentId = existing;
      }
      if (parentId == null) throw BusinessException.badRequest("阶段必须引用目标");
      requiredText(stage, "title", "阶段名称");
      JsonNode created = write("stage", Action.CREATE, stage, positiveId(parentId));
      String id = created.path("id").asText();
      stageKeys.put(requiredText(stage, "clientKey", "阶段 clientKey"), id);
      ((ArrayNode) result.get("stageIds")).add(id);
    }
    Map<String, String> taskKeys = new HashMap<>();
    for (JsonNode task : snapshot.path("tasks")) {
      String stageId = text(task, "stageId");
      if (stageId != null) {
        context.read("stage:stage:read");
        var parent = context.scope("stage", SecurityUtils.current().userId(), positiveId(stageId), true);
        context.version(parent.getVersion(), version(task, "stageVersion"));
      } else stageId = stageKeys.get(text(task, "stageKey"));
      if (stageId == null) throw BusinessException.badRequest("任务引用的阶段不存在");
      String priority = text(task, "priority");
      if (priority != null && !PRIORITIES.contains(priority)) throw BusinessException.badRequest("任务优先级无效");
      requiredText(task, "title", "任务名称");
      JsonNode created = write("task", Action.CREATE, task, positiveId(stageId));
      String id = created.path("id").asText();
      taskKeys.put(requiredText(task, "clientKey", "任务 clientKey"), id);
      ((ArrayNode) result.get("taskIds")).add(id);
    }
    Map<String, String> weekKeys = new HashMap<>();
    for (JsonNode week : snapshot.path("weeklyPlans")) {
      LocalDate date = localDate(week, "weekStart", "周计划起始日期");
      if (date == null || date.getDayOfWeek() != java.time.DayOfWeek.MONDAY) throw BusinessException.badRequest("周计划起始日期必须为周一");
      JsonNode created = planning(Action.WEEK, null, date);
      String id = created.path("id").asText();
      weekKeys.put(requiredText(week, "clientKey", "周计划 clientKey"), id);
      ((ArrayNode) result.get("weeklyPlanIds")).add(id);
    }
    for (JsonNode daily : snapshot.path("dailyPlans")) {
      String weeklyId = weekKeys.get(requiredText(daily, "weekKey", "日计划所属周"));
      LocalDate date = localDate(daily, "businessDate", "日计划日期");
      if (weeklyId == null || date == null) throw BusinessException.badRequest("日计划引用无效");
      JsonNode created = planning(Action.DAY, positiveId(weeklyId), date);
      ((ArrayNode) result.get("dailyPlanIds")).add(created.path("id").asText());
    }
    for (JsonNode assignment : snapshot.path("taskAssignments")) {
      String taskId = text(assignment, "taskId");
      if (taskId != null) {
        context.read("task:task:read");
        var task = context.scope("task", SecurityUtils.current().userId(), positiveId(taskId), true);
        context.version(task.getVersion(), version(assignment, "taskVersion"));
      } else taskId = taskKeys.get(text(assignment, "taskKey"));
      String weeklyId = weekKeys.get(text(assignment, "weekKey"));
      if (taskId == null || weeklyId == null) throw BusinessException.badRequest("任务安排引用无效");
      String dailyId = null;
      LocalDate date = localDate(assignment, "businessDate", "安排日期");
      if (date != null) dailyId = planning(Action.DAY, positiveId(weeklyId), date).path("id").asText();
      JsonNode current = planning(Action.CURRENT_ASSIGNMENT, positiveId(taskId), null);
      if (!current.isNull()) {
        if (Objects.equals(text(current, "weeklyPlanId"), weeklyId)
            && Objects.equals(text(current, "dailyPlanId"), dailyId)) {
          ((ArrayNode) result.get("assignmentIds")).add(current.path("id").asText());
          continue;
        }
        if (!current.path("id").asText().equals(text(assignment, "assignmentId"))) {
          throw BusinessException.conflict("任务原安排已变化，请重新核对并接受方案");
        }
        context.version(current.path("version").asLong(), version(assignment, "assignmentVersion"));
      } else if (text(assignment, "assignmentId") != null) {
        throw BusinessException.conflict("原任务安排已变化，请重新确认方案");
      }
      ObjectNode data = objectMapper.createObjectNode();
      data.put("weeklyPlanId", positiveId(weeklyId));
      if (dailyId != null) data.put("dailyPlanId", positiveId(dailyId));
      if (!current.isNull()) {
        data.put("assignmentId", current.path("id").asLong());
        data.put("version", current.path("version").asLong());
      }
      if (assignment.hasNonNull("sortOrder")) data.set("sortOrder", assignment.get("sortOrder"));
      JsonNode created = write("planning", Action.ASSIGN, data, positiveId(taskId));
      ((ArrayNode) result.get("assignmentIds")).add(created.path("id").asText());
    }
    for (JsonNode habit : snapshot.path("habits")) {
      ObjectNode data = habit.deepCopy();
      requiredText(data, "name", "习惯名称");
      if (!data.hasNonNull("cycleType")) data.put("cycleType", "DAILY");
      String category = text(habit, "categoryName");
      if (category != null) {
        ObjectNode command = objectMapper.createObjectNode().put("name", category);
        data.put("categoryId", port("habit").write(Action.CATEGORY, command).path("id").asLong());
      }
      JsonNode created = write("habit", Action.CREATE, data, null);
      ((ArrayNode) result.get("habitIds")).add(created.path("id").asText());
    }
    return result;
  }

  private PlanWritePort port(String domain) {
    PlanWritePort port = ports.get(domain);
    if (port == null) throw new BusinessException(503, "PLAN_MODULE_UNAVAILABLE", "方案涉及的业务模块尚未就绪");
    return port;
  }

  private JsonNode write(String domain, Action action, JsonNode data, Long parentId) {
    ObjectNode command = objectMapper.createObjectNode();
    command.set("data", data);
    if (parentId != null) command.put("parentId", parentId);
    return port(domain).write(action, command);
  }

  private JsonNode planning(Action action, Long parentId, LocalDate date) {
    ObjectNode command = objectMapper.createObjectNode();
    if (parentId != null) command.put("parentId", parentId);
    if (date != null) command.put("date", date.toString());
    return port("planning").write(action, command);
  }

  private long positiveId(String value) {
    try {
      long parsed = Long.parseLong(value);
      if (parsed <= 0) throw new NumberFormatException();
      return parsed;
    } catch (NumberFormatException error) {
      throw BusinessException.badRequest("业务标识必须是正整数");
    }
  }

  private Long version(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
      throw BusinessException.conflict("已有业务对象必须提供有效版本");
    }
    return value.longValue();
  }

  private JsonNode parseResult(Object value) {
    try {
      if (value == null) {
        return objectMapper.createObjectNode();
      }
      return objectMapper.readTree(String.valueOf(value));
    } catch (Exception error) {
      return objectMapper.createObjectNode();
    }
  }

  private long number(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText().trim();
    return text.isEmpty() ? null : text;
  }

  private String requiredText(JsonNode node, String field, String label) {
    String value = text(node, field);
    if (value == null) {
      throw BusinessException.badRequest(label + "不能为空");
    }
    return value;
  }

  private LocalDate localDate(JsonNode node, String field, String label) {
    String value = text(node, field);
    if (value == null) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (RuntimeException error) {
      throw BusinessException.badRequest(label + "格式必须是 YYYY-MM-DD");
    }
  }

  private LocalTime localTime(JsonNode node, String field, String label) {
    String value = text(node, field);
    if (value == null) {
      return null;
    }
    try {
      return LocalTime.parse(value);
    } catch (RuntimeException error) {
      throw BusinessException.badRequest(label + "格式必须是 HH:mm");
    }
  }

  private Integer integer(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isInt() || value.isLong() ? value.asInt() : null;
  }

  private Integer positiveInteger(JsonNode node, String field) {
    Integer value = integer(node, field);
    return value != null && value > 0 ? value : null;
  }
}
