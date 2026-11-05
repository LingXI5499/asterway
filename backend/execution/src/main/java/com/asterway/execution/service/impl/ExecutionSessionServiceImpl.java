package com.asterway.execution.service.impl;

import static com.asterway.common.constant.ExecutionConstants.*;

import com.asterway.common.config.ExecutionConfig;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.enums.ExecutionScopeEnum;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.AccountPrincipal;
import com.asterway.common.security.AccountSecurityProvider;
import com.asterway.common.security.ExecutionAccountData;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.security.OperationAuditProvider;
import com.asterway.common.security.RedisSessionStore;
import com.asterway.common.security.TaskStateHistoryProvider;
import com.asterway.common.utils.GrowthAccessUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.execution.dto.ExecutionControlDTO;
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
import com.asterway.execution.dto.ExecutionQueryDTO;
import com.asterway.execution.dto.ExecutionReadDTO;
import com.asterway.execution.dto.ExecutionStartDTO;
import com.asterway.execution.entity.ExecutionSegmentEntity;
import com.asterway.execution.entity.ExecutionSessionEntity;
import com.asterway.execution.mapper.ExecutionSegmentMapper;
import com.asterway.execution.mapper.ExecutionSessionMapper;
import com.asterway.execution.service.ExecutionSessionService;
import com.asterway.execution.vo.ExecutionDetailVO;
import com.asterway.execution.vo.ExecutionSegmentVO;
import com.asterway.execution.vo.ExecutionSessionVO;
import com.asterway.execution.vo.ExecutionStartResultVO;
import com.asterway.execution.vo.ExecutionSummaryVO;
import com.asterway.execution.vo.ExecutionTaskVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** 执行状态、资格、版本与时间段的一致性入口；所有写操作按用户串行。 */
@Service
public class ExecutionSessionServiceImpl
        implements ExecutionSessionService, ExecutionLifecycleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionSessionServiceImpl.class);
    private final ExecutionSessionMapper mapper;
    private final ExecutionSegmentMapper segments;
    private final ExecutionConfig config;
    private final RedisSessionStore logins;
    private final ObjectProvider<AccountSecurityProvider> accounts;
    private final ObjectProvider<TaskStateHistoryProvider> taskHistories;
    private final OperationAuditProvider audit;
    private final TransactionTemplate command;
    private final TransactionTemplate maintenance;
    private final TransactionTemplate snapshot;

    public ExecutionSessionServiceImpl(
            ExecutionSessionMapper mapper,
            ExecutionSegmentMapper segments,
            ExecutionConfig config,
            RedisSessionStore logins,
            ObjectProvider<AccountSecurityProvider> accounts,
            ObjectProvider<TaskStateHistoryProvider> taskHistories,
            OperationAuditProvider audit,
            PlatformTransactionManager transactions) {
        this.mapper = mapper;
        this.segments = segments;
        this.config = config;
        this.logins = logins;
        // 延迟获取账户安全契约，避免账户联动接口与认证提供者构造时互相依赖。
        this.accounts = accounts;
        this.taskHistories = taskHistories;
        this.audit = audit;
        this.command = new TransactionTemplate(transactions);
        this.maintenance = new TransactionTemplate(transactions);
        this.maintenance.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.snapshot = new TransactionTemplate(transactions);
        this.snapshot.setReadOnly(true);
        this.snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public ExecutionStartResultVO start(ExecutionStartDTO dto) {
        positive(dto.taskId());
        expected(dto.taskVersion());
        return write(
                START,
                (actor, now) -> {
                    long userId = actor.userId();
                    requireScope(userId, ExecutionScopeEnum.TASK, dto.taskId());
                    ExecutionAccountData account = account(userId);
                    ExecutionTaskVO task = eligible(userId, dto.taskId(), account, now);
                    ExecutionSessionEntity running = mapper.running(userId);
                    if (running != null) {
                        if (Objects.equals(running.getTaskId(), dto.taskId())) {
                            return new ExecutionStartResultVO(
                                    false, view(actor, running.getId(), now));
                        }
                        throw BusinessException.conflict("其他任务正在执行，请先暂停当前执行");
                    }
                    version(task.taskVersion(), dto.taskVersion());
                    ExecutionSessionEntity row = mapper.unfinishedTask(userId, dto.taskId());
                    boolean created = row == null;
                    if (created) {
                        row = new ExecutionSessionEntity();
                        row.setUserId(userId);
                        row.setTaskId(task.taskId());
                        row.setStatus(RUNNING);
                        row.setStartedAt(now);
                        row.setLastHeartbeatAt(now);
                        row.setControllerSessionId(actor.sessionId());
                        row.setVersion(0L);
                        row.setCreatedAt(now);
                        row.setUpdatedAt(now);
                        changed(mapper.insert(row));
                    } else {
                        freshActor(RESUME, actor);
                        activate(row, actor.sessionId(), now);
                    }
                    changed(mapper.startTask(userId, task.taskId(), task.taskVersion(), now));
                    taskHistories
                            .getObject()
                            .recordExecutionStart(
                                    userId,
                                    task.taskId(),
                                    task.taskStatus(),
                                    task.taskVersion() + 1,
                                    now);
                    appendSegment(row, task, account, now);
                    audit.recordGrowthSuccess(created ? START : RESUME, userId);
                    return new ExecutionStartResultVO(created, view(actor, row.getId(), now));
                });
    }

    @Override
    public ExecutionSessionVO current() {
        return read(
                READ,
                actor -> {
                    var row = mapper.running(actor.userId());
                    return row == null ? null : view(actor, row.getId(), now());
                });
    }

    @Override
    public ExecutionSessionVO pause(long id, ExecutionControlDTO dto) {
        positive(id);
        expected(dto.version());
        return write(
                PAUSE,
                (actor, now) -> {
                    var row = required(actor.userId(), id);
                    if (PAUSED.equals(row.getStatus())) {
                        return view(actor, id, now);
                    }
                    if (FINISHED.equals(row.getStatus())) {
                        throw BusinessException.conflict("执行已经结束，不能暂停");
                    }
                    version(row.getVersion(), dto.version());
                    pauseAt(row, open(row), now, USER_PAUSE, now);
                    return view(actor, id, now);
                });
    }

    @Override
    public ExecutionSessionVO resume(long id, ExecutionControlDTO dto) {
        positive(id);
        expected(dto.version());
        return write(
                RESUME,
                (actor, now) -> {
                    var row = required(actor.userId(), id);
                    if (RUNNING.equals(row.getStatus())) {
                        // 幂等返回，不让另一个登录端通过重复恢复接管 SID。
                        return view(actor, id, now);
                    }
                    if (FINISHED.equals(row.getStatus())) {
                        throw BusinessException.conflict("已结束的执行不能恢复");
                    }
                    version(row.getVersion(), dto.version());
                    if (mapper.running(actor.userId()) != null) {
                        throw BusinessException.conflict("其他任务正在执行，请先暂停");
                    }
                    var account = account(actor.userId());
                    var task = eligible(actor.userId(), row.getTaskId(), account, now);
                    activate(row, actor.sessionId(), now);
                    changed(
                            mapper.startTask(
                                    actor.userId(), task.taskId(), task.taskVersion(), now));
                    taskHistories
                            .getObject()
                            .recordExecutionStart(
                                    actor.userId(),
                                    task.taskId(),
                                    task.taskStatus(),
                                    task.taskVersion() + 1,
                                    now);
                    appendSegment(row, task, account, now);
                    audit.recordGrowthSuccess(RESUME, actor.userId());
                    return view(actor, id, now);
                });
    }

    @Override
    public ExecutionSessionVO finish(long id, ExecutionControlDTO dto) {
        positive(id);
        expected(dto.version());
        return write(
                END,
                (actor, now) -> {
                    var row = required(actor.userId(), id);
                    if (!FINISHED.equals(row.getStatus())) {
                        version(row.getVersion(), dto.version());
                        finishAt(row, USER_FINISH, now);
                    }
                    return view(actor, id, now);
                });
    }

    @Override
    public ExecutionSessionVO heartbeat(long id, ExecutionHeartbeatDTO dto) {
        positive(id);
        positive(dto.segmentId());
        return write(
                HEARTBEAT,
                (actor, now) -> {
                    var row = required(actor.userId(), id);
                    if (!RUNNING.equals(row.getStatus())
                            || !Objects.equals(row.getControllerSessionId(), actor.sessionId())
                            || mapper.heartbeat(
                                            actor.userId(),
                                            id,
                                            dto.segmentId(),
                                            actor.sessionId(),
                                            now)
                                    != 1) {
                        throw BusinessException.conflict("执行段或控制端已变化，请刷新执行状态");
                    }
                    // 心跳不修改控制版本，也不逐次记录操作日志。
                    return view(actor, id, now);
                });
    }

    @Override
    public PageResultVO<ExecutionSessionVO> page(ExecutionQueryDTO query) {
        return read(LIST, actor -> pageLocked(actor, query));
    }

    @Override
    public PageResultVO<ExecutionSessionVO> taskExecutions(long taskId, ExecutionQueryDTO query) {
        positive(taskId);
        query.setTaskId(taskId);
        return page(query);
    }

    @Override
    public ExecutionDetailVO get(long id) {
        positive(id);
        return read(
                READ,
                actor -> {
                    var ctx = context(actor, new ExecutionQueryDTO(), now(), false);
                    var row = requiredView(ctx, id);
                    return new ExecutionDetailVO(row, segmentPage(ctx, id, new PageQueryDTO()));
                });
    }

    @Override
    public PageResultVO<ExecutionSegmentVO> segments(long id, PageQueryDTO query) {
        positive(id);
        return read(
                READ,
                actor -> {
                    var ctx = context(actor, new ExecutionQueryDTO(), now(), false);
                    required(actor.userId(), id);
                    return segmentPage(ctx, id, query);
                });
    }

    @Override
    public ExecutionSummaryVO summary(ExecutionScopeEnum scope, Long id, ExecutionQueryDTO query) {
        return read(
                READ,
                actor -> {
                    if (scope != null) {
                        positive(id);
                        requireScope(actor.userId(), scope, id);
                        switch (scope) {
                            case TASK -> query.setTaskId(id);
                            case STAGE -> query.setStageId(id);
                            case GOAL -> query.setGoalId(id);
                            default -> throw BusinessException.badRequest("汇总范围无效");
                        }
                    }
                    return mapper.summary(context(actor, query, now(), true), query);
                });
    }

    private PageResultVO<ExecutionSessionVO> pageLocked(
            AccountPrincipal actor, ExecutionQueryDTO query) {
        var ctx = context(actor, query, now(), false);
        return new PageResultVO<>(
                mapper.page(ctx, query, query.offset()),
                mapper.count(ctx, query),
                query.getPage(),
                query.getSize());
    }

    private PageResultVO<ExecutionSegmentVO> segmentPage(
            ExecutionReadDTO ctx, long id, PageQueryDTO query) {
        return new PageResultVO<>(
                segments.page(ctx, id, query.offset(), query.getSize()),
                segments.count(ctx.userId(), id),
                query.getPage(),
                query.getSize());
    }

    /** 到期维护在业务命令事务之外提交，后续参数冲突不能把已到期记录恢复成运行。 */
    private <T> T write(String permission, BiFunction<AccountPrincipal, Instant, T> action) {
        var actor = SecurityUtils.requirePermission(permission);
        normalize(actor.userId());
        try {
            return command.execute(
                    status -> {
                        lock(actor.userId());
                        freshActor(permission, actor);
                        // 一次命令只捕获一个服务端时间，归一化、业务写入和返回视图使用同一时刻。
                        Instant operationTime = now();
                        normalizeLocked(actor.userId(), operationTime, true);
                        return action.apply(actor, operationTime);
                    });
        } catch (BusinessException error) {
            // 等待命令锁期间也可能越界。失败事务释放锁后再次独立归一化，避免回滚到运行态。
            normalize(actor.userId());
            throw error;
        }
    }

    private <T> T read(String permission, Function<AccountPrincipal, T> action) {
        var actor = SecurityUtils.requirePermission(permission);
        normalize(actor.userId());
        return snapshot.execute(
                status -> {
                    freshActor(permission, actor);
                    return action.apply(actor);
                });
    }

    private void freshActor(String permission, AccountPrincipal actor) {
        var fresh = accounts.getObject().loadSecurityAccount(actor.userId());
        if (fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()) {
            throw BusinessException.unauthorized();
        }
        if (GrowthAccessUtils.manager(fresh.roles())
                || !fresh.roles().contains("USER")
                || !fresh.permissions().contains(permission)
                || fresh.mustChangePassword()) {
            throw BusinessException.forbidden();
        }
        var login = logins.get(actor.sessionId());
        if (login == null
                || login.expiresAt() <= System.currentTimeMillis()
                || !login.userId().equals(Long.toString(actor.userId()))
                || login.authVersion() != fresh.authVersion()) {
            throw BusinessException.unauthorized();
        }
    }

    private ExecutionReadDTO context(
            AccountPrincipal actor, ExecutionQueryDTO query, Instant now, boolean defaultToday) {
        query.offset();
        if (query.getStatus() != null && !STATES.contains(query.getStatus())) {
            throw BusinessException.badRequest("执行状态无效");
        }
        if (query.getTaskId() != null) {
            requireScope(actor.userId(), ExecutionScopeEnum.TASK, query.getTaskId());
        }
        if (query.getStageId() != null) {
            requireScope(actor.userId(), ExecutionScopeEnum.STAGE, query.getStageId());
        }
        if (query.getGoalId() != null) {
            requireScope(actor.userId(), ExecutionScopeEnum.GOAL, query.getGoalId());
        }
        var from = query.getDateFrom();
        var to = query.getDateTo();
        if ((from == null) != (to == null) || from != null && from.isAfter(to)) {
            throw BusinessException.badRequest("起止日期必须成对提供且顺序正确");
        }
        var zone = account(actor.userId()).effectiveZone(now);
        if (from == null && defaultToday) {
            from = now.atZone(zone).toLocalDate();
            to = from;
        }
        if (to != null && to.getYear() >= 9999 || from != null && from.getYear() < 1) {
            throw BusinessException.badRequest("查询日期超出支持范围");
        }
        return new ExecutionReadDTO(
                actor.userId(),
                actor.sessionId(),
                now,
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant(),
                config.heartbeatIntervalSeconds());
    }

    private ExecutionSessionVO view(AccountPrincipal actor, long id, Instant now) {
        return requiredView(
                new ExecutionReadDTO(
                        actor.userId(),
                        actor.sessionId(),
                        now,
                        null,
                        null,
                        config.heartbeatIntervalSeconds()),
                id);
    }

    private ExecutionSessionVO requiredView(ExecutionReadDTO ctx, long id) {
        var row = mapper.view(ctx, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private ExecutionSessionEntity required(long userId, long id) {
        var row = mapper.find(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private ExecutionAccountData account(long userId) {
        var row = mapper.account(userId);
        if (row == null) {
            throw BusinessException.unauthorized();
        }
        return row;
    }

    private ExecutionTaskVO eligible(
            long userId, long taskId, ExecutionAccountData account, Instant now) {
        var task =
                mapper.eligibleTask(
                        userId, taskId, now.atZone(account.effectiveZone(now)).toLocalDate());
        if (task == null) {
            throw BusinessException.conflict("任务当前不可执行，请检查父链状态、开始日期及本周／今日安排");
        }
        return task;
    }

    private void appendSegment(
            ExecutionSessionEntity row,
            ExecutionTaskVO task,
            ExecutionAccountData account,
            Instant now) {
        if (segments.open(row.getUserId(), row.getId()) != null) {
            throw new IllegalStateException("会话已有开放时间段");
        }
        var zone = account.effectiveZone(now);
        LocalDate boundaryDate =
                task.planDate() == null
                        ? task.weekStartDate().plusDays(7)
                        : task.planDate().plusDays(1);
        Instant deadline = boundaryDate.atStartOfDay(zone).toInstant();
        Instant pending = account.pendingBoundary(now);
        if (pending != null && pending.isBefore(deadline)) {
            deadline = pending;
        }
        if (!deadline.isAfter(now)) {
            throw BusinessException.conflict("本次安排已到执行边界，请重新安排");
        }
        var segment = new ExecutionSegmentEntity();
        segment.setExecutionId(row.getId());
        segment.setAssignmentId(task.assignmentId());
        segment.setSegmentNo(segments.nextNumber(row.getUserId(), row.getId()));
        segment.setStartedAt(now);
        segment.setEligibleUntil(deadline);
        segment.setTimeZone(zone.getId());
        segment.setHeartbeatTimeoutSeconds(config.heartbeatTimeoutSeconds());
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        changed(segments.insert(segment));
    }

    private void activate(ExecutionSessionEntity row, String sid, Instant now) {
        if (!PAUSED.equals(row.getStatus())
                || segments.open(row.getUserId(), row.getId()) != null) {
            throw BusinessException.conflict("会话状态已变化，不能继续");
        }
        row.setStatus(RUNNING);
        row.setControllerSessionId(sid);
        row.setLastHeartbeatAt(now);
        persist(row, now);
    }

    private ExecutionSegmentEntity open(ExecutionSessionEntity row) {
        var segment = segments.open(row.getUserId(), row.getId());
        if (segment == null) {
            throw new IllegalStateException("运行会话缺少开放时间段");
        }
        return segment;
    }

    private Instant deadline(ExecutionSessionEntity row, ExecutionSegmentEntity segment) {
        Instant heartbeat =
                row.getLastHeartbeatAt().plusSeconds(segment.getHeartbeatTimeoutSeconds());
        return heartbeat.isBefore(segment.getEligibleUntil())
                ? heartbeat
                : segment.getEligibleUntil();
    }

    private void pauseAt(
            ExecutionSessionEntity row,
            ExecutionSegmentEntity segment,
            Instant requested,
            String reason,
            Instant now) {
        Instant end =
                requested.isBefore(deadline(row, segment)) ? requested : deadline(row, segment);
        if (end.isBefore(segment.getStartedAt())) {
            end = segment.getStartedAt();
        }
        changed(segments.close(row.getUserId(), segment.getId(), end, reason, now));
        row.setStatus(PAUSED);
        row.setControllerSessionId(null);
        persist(row, now);
        audit.recordGrowthSuccess(
                USER_PAUSE.equals(reason) ? PAUSE : "execution:session:auto_pause",
                row.getUserId());
    }

    private void finishAt(ExecutionSessionEntity row, String reason, Instant now) {
        if (RUNNING.equals(row.getStatus())) {
            var segment = open(row);
            Instant end = now.isBefore(deadline(row, segment)) ? now : deadline(row, segment);
            changed(segments.close(row.getUserId(), segment.getId(), end, FINISH, now));
        } else if (segments.open(row.getUserId(), row.getId()) != null) {
            throw new IllegalStateException("非运行会话仍有开放时间段");
        }
        row.setStatus(FINISHED);
        row.setFinishedAt(now);
        row.setFinishReason(reason);
        row.setControllerSessionId(null);
        persist(row, now);
        audit.recordGrowthSuccess(END, row.getUserId());
    }

    private void persist(ExecutionSessionEntity row, Instant now) {
        long oldVersion = row.getVersion();
        changed(mapper.changeState(row, oldVersion, now));
        row.setVersion(oldVersion + 1);
    }

    private void normalize(long userId) {
        maintenance.executeWithoutResult(
                status -> {
                    lock(userId);
                    normalizeLocked(userId, now(), true);
                });
    }

    /** 已持有用户锁；父操作写入后可关闭资格自动暂停，只做截止／认证归一化，再显式拒绝无效父修改。 */
    private void normalizeLocked(long userId, Instant now, boolean checkEligibility) {
        var row = mapper.running(userId);
        if (row == null) {
            return;
        }
        var segment = open(row);
        Instant end = deadline(row, segment);
        if (!end.isAfter(now)) {
            String reason = segment.getEligibleUntil().isAfter(end) ? AUTO_PAUSE : PLAN_BOUNDARY;
            pauseAt(row, segment, end, reason, now);
            return;
        }
        var account = account(userId);
        var login = logins.get(row.getControllerSessionId());
        var security = accounts.getObject().loadSecurityAccount(userId);
        if (!account.enabled()
                || security == null
                || GrowthAccessUtils.manager(security.roles())
                || login == null
                || login.expiresAt() <= now.toEpochMilli()
                || !login.userId().equals(Long.toString(userId))
                || login.authVersion() != account.authVersion()) {
            pauseAt(row, segment, now, AUTH_REVOKED, now);
            return;
        }
        if (checkEligibility) {
            var task =
                    mapper.eligibleTask(
                            userId,
                            row.getTaskId(),
                            now.atZone(account.effectiveZone(now)).toLocalDate());
            if (task == null || !"IN_PROGRESS".equals(task.taskStatus())) {
                pauseAt(row, segment, now, PLAN_BOUNDARY, now);
            }
        }
    }

    @Override
    public void scanExpired() {
        for (Long userId : mapper.dueUsers(now(), config.scanBatchSize())) {
            try {
                maintenance.executeWithoutResult(
                        status -> {
                            if (mapper.tryLockUser(userId) != null) {
                                normalizeLocked(userId, now(), true);
                            }
                        });
            } catch (RuntimeException error) {
                LOGGER.error("执行到期处理失败，用户标识：{}", userId, error);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireTaskFinished(long userId, long taskId) {
        lifecycleLock(userId);
        normalizeLocked(userId, now(), false);
        if (mapper.unfinishedTask(userId, taskId) != null) {
            throw BusinessException.conflict("请先结束该任务的运行或暂停执行，再完成任务");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireNoRunning(long userId, ExecutionScopeEnum scope, long id) {
        lifecycleLock(userId);
        normalizeLocked(userId, now(), false);
        if (mapper.runningInScope(userId, scope.name(), id)) {
            throw BusinessException.conflict("请先暂停正在执行的任务，再调整安排或删除计划");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void validateRunningEligibility(long userId) {
        lifecycleLock(userId);
        Instant now = now();
        normalizeLocked(userId, now, false);
        var row = mapper.running(userId);
        if (row != null) {
            var account = account(userId);
            var task =
                    mapper.eligibleTask(
                            userId,
                            row.getTaskId(),
                            now.atZone(account.effectiveZone(now)).toLocalDate());
            if (task == null || !"IN_PROGRESS".equals(task.taskStatus())) {
                throw BusinessException.conflict("本次修改会使正在执行的任务失效，请先暂停执行");
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void finishTask(long userId, long taskId) {
        finishScope(userId, ExecutionScopeEnum.TASK, taskId, TASK_DELETED);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void finishGoal(long userId, long goalId) {
        finishScope(userId, ExecutionScopeEnum.GOAL, goalId, GOAL_ARCHIVED);
    }

    private void finishScope(long userId, ExecutionScopeEnum scope, long id, String reason) {
        lifecycleLock(userId);
        Instant now = now();
        normalizeLocked(userId, now, false);
        while (true) {
            var batch = mapper.unfinished(userId, scope.name(), id, config.scanBatchSize());
            if (batch.isEmpty()) {
                return;
            }
            for (var row : batch) {
                finishAt(row, reason, now);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void pauseLogin(long userId, String sessionId) {
        lifecycleLock(userId);
        Instant now = now();
        normalizeLocked(userId, now, false);
        var row = mapper.running(userId);
        if (row != null && (sessionId == null || sessionId.equals(row.getControllerSessionId()))) {
            pauseAt(row, open(row), now, AUTH_REVOKED, now);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void shortenBoundary(long userId, Instant effectiveAt) {
        lifecycleLock(userId);
        Instant now = now();
        normalizeLocked(userId, now, false);
        var row = mapper.running(userId);
        if (row == null || effectiveAt == null) {
            return;
        }
        var segment = open(row);
        if (effectiveAt.isAfter(segment.getStartedAt())
                && effectiveAt.isBefore(segment.getEligibleUntil())) {
            changed(segments.shorten(userId, row.getId(), effectiveAt, now));
            normalizeLocked(userId, now, false);
        }
    }

    private void lifecycleLock(long userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("执行联动必须参加父业务事务");
        }
        lock(userId);
    }

    private void lock(long userId) {
        if (mapper.lockUser(userId) == null) {
            throw BusinessException.notFound();
        }
    }

    private void requireScope(long userId, ExecutionScopeEnum scope, long id) {
        positive(id);
        if (mapper.scopeExists(userId, scope.name(), id) == null) {
            throw BusinessException.notFound();
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static void positive(Long value) {
        if (value == null || value <= 0) {
            throw BusinessException.badRequest("资源标识必须为正数");
        }
    }

    private static void expected(Long value) {
        if (value == null || value < 0) {
            throw BusinessException.badRequest("必须提供有效版本");
        }
    }

    private static void version(Long actual, Long expected) {
        if (!Objects.equals(actual, expected)) {
            throw BusinessException.conflict("数据版本已变化，请刷新后重试");
        }
    }

    private static void changed(int count) {
        if (count != 1) {
            throw BusinessException.conflict("执行数据已变化，请刷新后重试");
        }
    }
}
