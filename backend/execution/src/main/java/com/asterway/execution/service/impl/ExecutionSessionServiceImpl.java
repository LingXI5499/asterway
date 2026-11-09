/*
 * 实现执行计时会话业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.service.impl;

// 引入*，下面可以直接使用这个类型或静态成员。
import static com.asterway.common.constant.ExecutionConstants.*;

// 引入执行计时Config（ExecutionConfig），下面可以直接使用这个类型或静态成员。
import com.asterway.common.config.ExecutionConfig;
// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入执行计时数据范围Enum（ExecutionScopeEnum），下面可以直接使用这个类型或静态成员。
import com.asterway.common.enums.ExecutionScopeEnum;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入账号当前登录身份（AccountPrincipal），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountPrincipal;
// 引入账号Security提供器（AccountSecurityProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountSecurityProvider;
// 引入执行计时账号数据（ExecutionAccountData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionAccountData;
// 引入执行计时Lifecycle提供器（ExecutionLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionLifecycleProvider;
// 引入操作Audit提供器（OperationAuditProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.OperationAuditProvider;
// 引入Redis缓存会话Store（RedisSessionStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisSessionStore;
// 引入任务当前状态历史提供器（TaskStateHistoryProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.TaskStateHistoryProvider;
// 引入成长业务访问权限Utils（GrowthAccessUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthAccessUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入执行计时Control（ExecutionControlDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionControlDTO;
// 引入执行计时心跳（ExecutionHeartbeatDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionHeartbeatDTO;
// 引入执行计时查询条件（ExecutionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionQueryDTO;
// 引入执行计时读取（ExecutionReadDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionReadDTO;
// 引入执行计时开始（ExecutionStartDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionStartDTO;
// 引入执行计时连续计时段（ExecutionSegmentEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.entity.ExecutionSegmentEntity;
// 引入执行计时会话（ExecutionSessionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.entity.ExecutionSessionEntity;
// 引入执行计时连续计时段数据库读写器（ExecutionSegmentMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.mapper.ExecutionSegmentMapper;
// 引入执行计时会话数据库读写器（ExecutionSessionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.mapper.ExecutionSessionMapper;
// 引入执行计时会话业务服务（ExecutionSessionService），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.service.ExecutionSessionService;
// 引入执行计时详情（ExecutionDetailVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionDetailVO;
// 引入执行计时连续计时段（ExecutionSegmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSegmentVO;
// 引入执行计时会话（ExecutionSessionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSessionVO;
// 引入执行计时开始结果（ExecutionStartResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionStartResultVO;
// 引入执行计时汇总（ExecutionSummaryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSummaryVO;
// 引入执行计时任务（ExecutionTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionTaskVO;

// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入对象提供器（ObjectProvider），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.ObjectProvider;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入平台事务管理员（PlatformTransactionManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.PlatformTransactionManager;
// 引入事务Definition（TransactionDefinition），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.TransactionDefinition;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;
// 引入事务Synchronization管理员（TransactionSynchronizationManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionSynchronizationManager;
// 引入事务Template（TransactionTemplate），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionTemplate;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入BiFunction，下面可以直接使用这个类型或静态成员。
import java.util.function.BiFunction;
// 引入Function，下面可以直接使用这个类型或静态成员。
import java.util.function.Function;

/* 执行状态、资格、版本与时间段的一致性入口；所有写操作按用户串行。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 ExecutionSessionServiceImpl；实现执行计时会话业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class ExecutionSessionServiceImpl
        // 继续传入或组合表达式 implements ExecutionSessionService, ExecutionLifecycleProvider { 的结果，完成上面尚未结束的调用或声明。
        implements ExecutionSessionService, ExecutionLifecycleProvider {
    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionSessionServiceImpl.class);
    // 访问数据库的 Mapper（mapper），使用 ExecutionSessionMapper 类型保存。
    private final ExecutionSessionMapper mapper;
    // 计时段列表（segments），使用 ExecutionSegmentMapper 类型保存。
    private final ExecutionSegmentMapper segments;
    // config，使用 ExecutionConfig 类型保存。
    private final ExecutionConfig config;
    // logins，使用 RedisSessionStore 类型保存。
    private final RedisSessionStore logins;
    // accounts，使用 ObjectProvider<AccountSecurityProvider> 类型保存。
    private final ObjectProvider<AccountSecurityProvider> accounts;
    // 任务历史记录列表（taskHistories），使用 ObjectProvider<TaskStateHistoryProvider> 类型保存。
    private final ObjectProvider<TaskStateHistoryProvider> taskHistories;
    // audit，使用 OperationAuditProvider 类型保存。
    private final OperationAuditProvider audit;
    // command，使用 TransactionTemplate 类型保存。
    private final TransactionTemplate command;
    // maintenance，使用 TransactionTemplate 类型保存。
    private final TransactionTemplate maintenance;
    // 确认时的数据快照（snapshot），使用 TransactionTemplate 类型保存。
    private final TransactionTemplate snapshot;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public ExecutionSessionServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 ExecutionSessionMapper 类型保存。
            ExecutionSessionMapper mapper,
            // 计时段列表（segments），使用 ExecutionSegmentMapper 类型保存。
            ExecutionSegmentMapper segments,
            // config，使用 ExecutionConfig 类型保存。
            ExecutionConfig config,
            // logins，使用 RedisSessionStore 类型保存。
            RedisSessionStore logins,
            // accounts，使用 ObjectProvider<AccountSecurityProvider> 类型保存。
            ObjectProvider<AccountSecurityProvider> accounts,
            // 任务历史记录列表（taskHistories），使用 ObjectProvider<TaskStateHistoryProvider> 类型保存。
            ObjectProvider<TaskStateHistoryProvider> taskHistories,
            // audit，使用 OperationAuditProvider 类型保存。
            OperationAuditProvider audit,
            // transactions，使用 PlatformTransactionManager 类型保存。
            PlatformTransactionManager transactions) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把计时段列表（segments）保存到 this.segments。
        this.segments = segments;
        // 把config保存到 this.config。
        this.config = config;
        // 把logins保存到 this.logins。
        this.logins = logins;
        // 延迟获取账户安全契约，避免账户联动接口与认证提供者构造时互相依赖。
        // 把accounts保存到 this.accounts。
        this.accounts = accounts;
        // 把任务历史记录列表（taskHistories）保存到 this.taskHistories。
        this.taskHistories = taskHistories;
        // 把audit保存到 this.audit。
        this.audit = audit;
        // 把新建 事务Template（TransactionTemplate） 实例保存到 this.command。
        this.command = new TransactionTemplate(transactions);
        // 把新建 事务Template（TransactionTemplate） 实例保存到 this.maintenance。
        this.maintenance = new TransactionTemplate(transactions);
        // 设置 PropagationBehavior 的值。
        this.maintenance.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // 把新建 事务Template（TransactionTemplate） 实例保存到 this.snapshot。
        this.snapshot = new TransactionTemplate(transactions);
        // 设置 读取仅（ReadOnly） 的值。
        this.snapshot.setReadOnly(true);
        // 设置 IsolationLevel 的值。
        this.snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 开始（start）方法：调用 start 完成开始处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 ExecutionStartDTO 类型保存。
    public ExecutionStartResultVO start(ExecutionStartDTO dto) {
        // 调用 positive(dto.taskId 完成任务编号处理。
        positive(dto.taskId());
        // 调用 expected(dto.taskVersion 完成任务版本号处理。
        expected(dto.taskVersion());
        // 把调用 write(START, (actor,now)->{ long userId = actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return write(
                // 继续传入或组合开始（START），完成上面尚未结束的调用或声明。
                START,
                // actor，使用 null 类型保存。
                // 当前时间（now），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                (actor, now) -> {
                    // 准备用户编号（userId），类型为 long；初始内容是调用 actor.userId 完成用户编号处理。
                    long userId = actor.userId();
                    // 调用 requireScope(userId, ExecutionScopeEnum.TASK, dto.taskId 完成任务编号处理。
                    requireScope(userId, ExecutionScopeEnum.TASK, dto.taskId());
                    // 准备账号（account），类型为 ExecutionAccountData；初始内容是调用 account 完成账号处理。
                    ExecutionAccountData account = account(userId);
                    // 准备任务（task），类型为 ExecutionTaskVO；初始内容是调用 eligible(userId, dto.taskId 完成任务编号处理。
                    ExecutionTaskVO task = eligible(userId, dto.taskId(), account, now);
                    // 准备运行中（running），类型为 ExecutionSessionEntity；初始内容是调用 mapper.running 完成运行中处理。
                    ExecutionSessionEntity running = mapper.running(userId);
                    // 检查条件 running != null 是否成立；成立时执行下面的分支。
                    if (running != null) {
                        // 检查条件 Objects.equals(running.getTaskId(), dto.taskId()) 是否成立；成立时执行下面的分支。
                        if (Objects.equals(running.getTaskId(), dto.taskId())) {
                            // 把新建 执行计时开始结果（ExecutionStartResultVO） 实例交还给调用方，本方法到此结束。
                            return new ExecutionStartResultVO(
                                    // 继续传入或组合读取 编号（Id） 的值，完成上面尚未结束的调用或声明。
                                    false, view(actor, running.getId(), now));
                        }
                        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                        throw BusinessException.conflict("其他任务正在执行，请先暂停当前执行");
                    }
                    // 调用 version(task.taskVersion 完成任务版本号处理。
                    version(task.taskVersion(), dto.taskVersion());
                    // 准备当前记录（row），类型为 ExecutionSessionEntity；初始内容是调用 mapper.unfinishedTask 完成unfinished任务处理。
                    ExecutionSessionEntity row = mapper.unfinishedTask(userId, dto.taskId());
                    // 准备创建时（created），类型为 boolean；初始内容是条件判断的结果：row == null。
                    boolean created = row == null;
                    // 检查条件 created 是否成立；成立时执行下面的分支。
                    if (created) {
                        // 把新建 执行计时会话（ExecutionSessionEntity） 实例保存到 row。
                        row = new ExecutionSessionEntity();
                        // 设置 用户编号（UserId） 的值。
                        row.setUserId(userId);
                        // 设置 任务编号（TaskId） 的值。
                        row.setTaskId(task.taskId());
                        // 设置 状态（Status） 的值。
                        row.setStatus(RUNNING);
                        // 设置 开始时At（StartedAt） 的值。
                        row.setStartedAt(now);
                        // 设置 最后一次心跳At（LastHeartbeatAt） 的值。
                        row.setLastHeartbeatAt(now);
                        // 设置 请求入口会话编号（ControllerSessionId） 的值。
                        row.setControllerSessionId(actor.sessionId());
                        // 设置 版本号（Version） 的值。
                        row.setVersion(0L);
                        // 设置 创建时At（CreatedAt） 的值。
                        row.setCreatedAt(now);
                        // 设置 更新时At（UpdatedAt） 的值。
                        row.setUpdatedAt(now);
                        // 向数据库插入新记录，具体 SQL 对应 insert。
                        changed(mapper.insert(row));
                    // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
                    } else {
                        // 调用 freshActor 完成freshActor处理。
                        freshActor(RESUME, actor);
                        // 调用 activate(row, actor.sessionId 完成会话编号处理。
                        activate(row, actor.sessionId(), now);
                    }
                    // 调用 changed(mapper.startTask 完成开始任务处理。
                    changed(mapper.startTask(userId, task.taskId(), task.taskVersion(), now));
                    // 读取 对象（Object） 的值。
                    taskHistories
                            // 继续对上一步结果执行读取 对象（Object） 的值。
                            .getObject()
                            // 继续对上一步结果执行调用 recordExecutionStart 完成记录执行计时开始处理。
                            .recordExecutionStart(
                                    // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
                                    userId,
                                    // 调用 task.taskId 完成任务编号处理。
                                    task.taskId(),
                                    // 调用 task.taskStatus 完成任务状态处理。
                                    task.taskStatus(),
                                    // 继续传入或组合调用 task.taskVersion 完成任务版本号处理，完成上面尚未结束的调用或声明。
                                    task.taskVersion() + 1,
                                    // 继续传入或组合表达式 now) 的结果，完成上面尚未结束的调用或声明。
                                    now);
                    // 调用 appendSegment 完成追加连续计时段处理。
                    appendSegment(row, task, account, now);
                    // 调用 audit.recordGrowthSuccess 完成记录成长业务成功处理。
                    audit.recordGrowthSuccess(created ? START : RESUME, userId);
                    // 把新建 执行计时开始结果（ExecutionStartResultVO） 实例交还给调用方，本方法到此结束。
                    return new ExecutionStartResultVO(created, view(actor, row.getId(), now));
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ExecutionSessionVO current() {
        // 把根据前面的条件，从两个候选结果中选择一个：read(READ, (actor)->{ /*missing* / row = mapper.running(actor.userId()); return row == null ? null : …交还给调用方，本方法到此结束。
        return read(
                // 继续传入或组合读取（READ），完成上面尚未结束的调用或声明。
                READ,
                // actor，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                actor -> {
                    // 准备当前记录（row），类型为 null；初始内容是调用 mapper.running 完成运行中处理。
                    var row = mapper.running(actor.userId());
                    // 把根据前面的条件，从两个候选结果中选择一个：row == null ? null : view(actor, row.getId(), now())交还给调用方，本方法到此结束。
                    return row == null ? null : view(actor, row.getId(), now());
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    public ExecutionSessionVO pause(long id, ExecutionControlDTO dto) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 调用 expected(dto.version 完成版本号处理。
        expected(dto.version());
        // 把调用 write(PAUSE, (actor,now)->{ /*missing* / row = required(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return write(
                // 继续传入或组合暂停（PAUSE），完成上面尚未结束的调用或声明。
                PAUSE,
                // actor，使用 null 类型保存。
                // 当前时间（now），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                (actor, now) -> {
                    // 准备当前记录（row），类型为 null；初始内容是调用 required(actor.userId 完成用户编号处理。
                    var row = required(actor.userId(), id);
                    // 检查条件 PAUSED.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
                    if (PAUSED.equals(row.getStatus())) {
                        // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                        return view(actor, id, now);
                    }
                    // 检查条件 FINISHED.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
                    if (FINISHED.equals(row.getStatus())) {
                        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                        throw BusinessException.conflict("执行已经结束，不能暂停");
                    }
                    // 读取 版本号（Version） 的值。
                    version(row.getVersion(), dto.version());
                    // 调用 pauseAt 完成暂停At处理。
                    pauseAt(row, open(row), now, USER_PAUSE, now);
                    // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                    return view(actor, id, now);
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    public ExecutionSessionVO resume(long id, ExecutionControlDTO dto) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 调用 expected(dto.version 完成版本号处理。
        expected(dto.version());
        // 把调用 write(RESUME, (actor,now)->{ /*missing* / row = required(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return write(
                // 继续传入或组合继续（RESUME），完成上面尚未结束的调用或声明。
                RESUME,
                // actor，使用 null 类型保存。
                // 当前时间（now），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                (actor, now) -> {
                    // 准备当前记录（row），类型为 null；初始内容是调用 required(actor.userId 完成用户编号处理。
                    var row = required(actor.userId(), id);
                    // 检查条件 RUNNING.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
                    if (RUNNING.equals(row.getStatus())) {
                        // 幂等返回，不让另一个登录端通过重复恢复接管 SID。
                        // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                        return view(actor, id, now);
                    }
                    // 检查条件 FINISHED.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
                    if (FINISHED.equals(row.getStatus())) {
                        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                        throw BusinessException.conflict("已结束的执行不能恢复");
                    }
                    // 读取 版本号（Version） 的值。
                    version(row.getVersion(), dto.version());
                    // 检查条件 mapper.running(actor.userId()) != null 是否成立；成立时执行下面的分支。
                    if (mapper.running(actor.userId()) != null) {
                        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                        throw BusinessException.conflict("其他任务正在执行，请先暂停");
                    }
                    // 准备账号（account），类型为 null；初始内容是调用 account(actor.userId 完成用户编号处理。
                    var account = account(actor.userId());
                    // 准备任务（task），类型为 null；初始内容是调用 eligible(actor.userId 完成用户编号处理。
                    var task = eligible(actor.userId(), row.getTaskId(), account, now);
                    // 调用 activate(row, actor.sessionId 完成会话编号处理。
                    activate(row, actor.sessionId(), now);
                    // 调用 changed(mapper.startTask 完成开始任务处理。
                    changed(
                            // 继续传入或组合调用 mapper.startTask 完成开始任务处理，完成上面尚未结束的调用或声明。
                            mapper.startTask(
                                    // 调用 actor.userId 完成用户编号处理。
                                    actor.userId(), task.taskId(), task.taskVersion(), now));
                    // 读取 对象（Object） 的值。
                    taskHistories
                            // 继续对上一步结果执行读取 对象（Object） 的值。
                            .getObject()
                            // 继续对上一步结果执行调用 recordExecutionStart 完成记录执行计时开始处理。
                            .recordExecutionStart(
                                    // 调用 actor.userId 完成用户编号处理。
                                    actor.userId(),
                                    // 调用 task.taskId 完成任务编号处理。
                                    task.taskId(),
                                    // 调用 task.taskStatus 完成任务状态处理。
                                    task.taskStatus(),
                                    // 继续传入或组合调用 task.taskVersion 完成任务版本号处理，完成上面尚未结束的调用或声明。
                                    task.taskVersion() + 1,
                                    // 继续传入或组合表达式 now) 的结果，完成上面尚未结束的调用或声明。
                                    now);
                    // 调用 appendSegment 完成追加连续计时段处理。
                    appendSegment(row, task, account, now);
                    // 调用 audit.recordGrowthSuccess 完成记录成长业务成功处理。
                    audit.recordGrowthSuccess(RESUME, actor.userId());
                    // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                    return view(actor, id, now);
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // finish方法：调用 finish 完成finish处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionControlDTO 类型保存。
    public ExecutionSessionVO finish(long id, ExecutionControlDTO dto) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 调用 expected(dto.version 完成版本号处理。
        expected(dto.version());
        // 把调用 write(END, (actor,now)->{ /*missing* / row = required(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return write(
                // 继续传入或组合结束（END），完成上面尚未结束的调用或声明。
                END,
                // actor，使用 null 类型保存。
                // 当前时间（now），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                (actor, now) -> {
                    // 准备当前记录（row），类型为 null；初始内容是调用 required(actor.userId 完成用户编号处理。
                    var row = required(actor.userId(), id);
                    // 检查条件 !FINISHED.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
                    if (!FINISHED.equals(row.getStatus())) {
                        // 读取 版本号（Version） 的值。
                        version(row.getVersion(), dto.version());
                        // 调用 finishAt 完成finishAt处理。
                        finishAt(row, USER_FINISH, now);
                    }
                    // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                    return view(actor, id, now);
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 心跳（heartbeat）方法：调用 heartbeat 完成心跳处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 ExecutionHeartbeatDTO 类型保存。
    public ExecutionSessionVO heartbeat(long id, ExecutionHeartbeatDTO dto) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 调用 positive(dto.segmentId 完成连续计时段编号处理。
        positive(dto.segmentId());
        // 把优先使用前面的有效值，没有时采用后面的备用值：write(HEARTBEAT, (actor,now)->{ /*missing* / row = required(actor.userId(), id); if (!RUNNING.equals(row.getSta…交还给调用方，本方法到此结束。
        return write(
                // 继续传入或组合心跳（HEARTBEAT），完成上面尚未结束的调用或声明。
                HEARTBEAT,
                // actor，使用 null 类型保存。
                // 当前时间（now），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                (actor, now) -> {
                    // 准备当前记录（row），类型为 null；初始内容是调用 required(actor.userId 完成用户编号处理。
                    var row = required(actor.userId(), id);
                    // 检查条件 !RUNNING.equals(row.getStatus()) || !Objects.equals(row.getControllerSessionId(), actor.sessionId()) || mapper.heartbeat(actor.userId(), id, dto.… 是否成立；成立时执行下面的分支。
                    if (!RUNNING.equals(row.getStatus())
                            // 继续补充同一个判断或备用结果：条件 || !Objects.equals(row.getControllerSessionId(), actor.sessionId()) 是否成立。
                            || !Objects.equals(row.getControllerSessionId(), actor.sessionId())
                            // 继续补充同一个判断或备用结果：条件 || mapper.heartbeat( 是否成立。
                            || mapper.heartbeat(
                                            // 调用 actor.userId 完成用户编号处理。
                                            actor.userId(),
                                            // 继续传入或组合编号（id），完成上面尚未结束的调用或声明。
                                            id,
                                            // 调用 dto.segmentId 完成连续计时段编号处理。
                                            dto.segmentId(),
                                            // 调用 actor.sessionId 完成会话编号处理。
                                            actor.sessionId(),
                                            // 继续传入或组合表达式 now) 的结果，完成上面尚未结束的调用或声明。
                                            now)
                                    // 继续传入或组合条件判断的结果：!= 1) {，完成上面尚未结束的调用或声明。
                                    != 1) {
                        // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                        throw BusinessException.conflict("执行段或控制端已变化，请刷新执行状态");
                    }
                    // 心跳不修改控制版本，也不逐次记录操作日志。
                    // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
                    return view(actor, id, now);
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    public PageResultVO<ExecutionSessionVO> page(ExecutionQueryDTO query) {
        // actor，使用 null 类型保存。
        // 把调用 read 完成读取处理交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return read(LIST, actor -> pageLocked(actor, query));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 任务执行记录（taskExecutions）方法：调用 taskExecutions 完成任务执行记录处理；参数负责传入本次处理所需的数据。
    @Override
    // 任务编号（taskId），使用 long 类型保存。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    public PageResultVO<ExecutionSessionVO> taskExecutions(long taskId, ExecutionQueryDTO query) {
        // 调用 positive 完成正数处理。
        positive(taskId);
        // 设置 任务编号（TaskId） 的值。
        query.setTaskId(taskId);
        // 把调用 page 完成当前页处理交还给调用方，本方法到此结束。
        return page(query);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public ExecutionDetailVO get(long id) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 把调用 read 完成读取处理交还给调用方，本方法到此结束。
        return read(
                // 继续传入或组合读取（READ），完成上面尚未结束的调用或声明。
                READ,
                // actor，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                actor -> {
                    // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
                    var ctx = context(actor, new ExecutionQueryDTO(), now(), false);
                    // 准备当前记录（row），类型为 null；初始内容是要求满足 d页面（dView）。
                    var row = requiredView(ctx, id);
                    // 把新建 执行计时详情（ExecutionDetailVO） 实例交还给调用方，本方法到此结束。
                    return new ExecutionDetailVO(row, segmentPage(ctx, id, new PageQueryDTO()));
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 计时段列表（segments）方法：调用 segments 完成计时段列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<ExecutionSegmentVO> segments(long id, PageQueryDTO query) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 把调用 read(READ, (actor)->{ /*missing* / ctx = context(actor, new ExecutionQueryDTO(), now(), false); required(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return read(
                // 继续传入或组合读取（READ），完成上面尚未结束的调用或声明。
                READ,
                // actor，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                actor -> {
                    // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
                    var ctx = context(actor, new ExecutionQueryDTO(), now(), false);
                    // 调用 required(actor.userId 完成用户编号处理。
                    required(actor.userId(), id);
                    // 把调用 segmentPage 完成连续计时段当前页处理交还给调用方，本方法到此结束。
                    return segmentPage(ctx, id, query);
                });
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    @Override
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    public ExecutionSummaryVO summary(ExecutionScopeEnum scope, Long id, ExecutionQueryDTO query) {
        // 把调用 read(READ, (actor)->{ if (scope != null) { positive(id); requireScope(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return read(
                // 继续传入或组合读取（READ），完成上面尚未结束的调用或声明。
                READ,
                // actor，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                actor -> {
                    // 检查条件 scope != null 是否成立；成立时执行下面的分支。
                    if (scope != null) {
                        // 调用 positive 完成正数处理。
                        positive(id);
                        // 调用 requireScope(actor.userId 完成用户编号处理。
                        requireScope(actor.userId(), scope, id);
                        // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
                        switch (scope) {
                            // 匹配这一种取值时使用本分支：case TASK。
                            // 设置 任务编号（TaskId） 的值。
                            case TASK -> query.setTaskId(id);
                            // 匹配这一种取值时使用本分支：case STAGE。
                            // 设置 阶段编号（StageId） 的值。
                            case STAGE -> query.setStageId(id);
                            // 匹配这一种取值时使用本分支：case GOAL。
                            // 设置 目标编号（GoalId） 的值。
                            case GOAL -> query.setGoalId(id);
                            // 匹配这一种取值时使用本分支：default。
                            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                            default -> throw BusinessException.badRequest("汇总范围无效");
                        }
                    }
                    // 把调用 mapper.summary 完成汇总处理交还给调用方，本方法到此结束。
                    return mapper.summary(context(actor, query, now(), true), query);
                });
    }

    // 当前页Locked（pageLocked）方法：调用 pageLocked 完成当前页Locked处理；参数负责传入本次处理所需的数据。
    private PageResultVO<ExecutionSessionVO> pageLocked(
            // actor，使用 AccountPrincipal 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            AccountPrincipal actor, ExecutionQueryDTO query) {
        // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
        var ctx = context(actor, query, now(), false);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 按分页条件读取记录，具体 SQL 对应 page。
                mapper.page(ctx, query, query.offset()),
                // 统计符合条件的记录数，具体 SQL 对应 count。
                mapper.count(ctx, query),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 连续计时段当前页（segmentPage）方法：调用 segmentPage 完成连续计时段当前页处理；参数负责传入本次处理所需的数据。
    private PageResultVO<ExecutionSegmentVO> segmentPage(
            // 当前用户、权限和数据范围检查器（ctx），使用 ExecutionReadDTO 类型保存。
            // 编号（id），使用 long 类型保存。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            ExecutionReadDTO ctx, long id, PageQueryDTO query) {
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 segments.page 完成当前页处理。
                segments.page(ctx, id, query.offset(), query.getSize()),
                // 调用 segments.count 完成数量处理。
                segments.count(ctx.userId(), id),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    /* 到期维护在业务命令事务之外提交，后续参数冲突不能把已到期记录恢复成运行。 */
    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    // 操作（action），使用 BiFunction<AccountPrincipal, Instant, T> 类型保存。
    private <T> T write(String permission, BiFunction<AccountPrincipal, Instant, T> action) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission(permission);
        // 调用 normalize(actor.userId 完成用户编号处理。
        normalize(actor.userId());
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把执行传入的数据库语句并取得结果交还给调用方，本方法到此结束。
            return command.execute(
                    // 状态（status），使用 null 类型保存。
                    // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                    status -> {
                        // 调用 lock(actor.userId 完成用户编号处理。
                        lock(actor.userId());
                        // 调用 freshActor 完成freshActor处理。
                        freshActor(permission, actor);
                        // 一次命令只捕获一个服务端时间，归一化、业务写入和返回视图使用同一时刻。
                        // 准备操作时间（operationTime），类型为 Instant；初始内容是调用 now 完成当前时间处理。
                        Instant operationTime = now();
                        // 调用 normalizeLocked(actor.userId 完成用户编号处理。
                        normalizeLocked(actor.userId(), operationTime, true);
                        // 把调用 action.apply 完成apply处理交还给调用方，本方法到此结束。
                        return action.apply(actor, operationTime);
                    });
        // 错误（error），使用 BusinessException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (BusinessException error) {
            // 等待命令锁期间也可能越界。失败事务释放锁后再次独立归一化，避免回滚到运行态。
            // 调用 normalize(actor.userId 完成用户编号处理。
            normalize(actor.userId());
            // 中止正常处理并抛出异常：错误（error）；由上层按失败处理。
            throw error;
        }
    }

    // 读取（read）方法：调用 read 完成读取处理；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    // 操作（action），使用 Function<AccountPrincipal, T> 类型保存。
    private <T> T read(String permission, Function<AccountPrincipal, T> action) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission(permission);
        // 调用 normalize(actor.userId 完成用户编号处理。
        normalize(actor.userId());
        // 把执行传入的数据库语句并取得结果交还给调用方，本方法到此结束。
        return snapshot.execute(
                // 状态（status），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                status -> {
                    // 调用 freshActor 完成freshActor处理。
                    freshActor(permission, actor);
                    // 把调用 action.apply 完成apply处理交还给调用方，本方法到此结束。
                    return action.apply(actor);
                });
    }

    // freshActor方法：调用 freshActor 完成freshActor处理；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    // actor，使用 AccountPrincipal 类型保存。
    private void freshActor(String permission, AccountPrincipal actor) {
        // 准备fresh，类型为 null；初始内容是读取 对象（Object） 的值。
        var fresh = accounts.getObject().loadSecurityAccount(actor.userId());
        // 检查当前记录版本是否符合请求要求：fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()；成立时执行下面的分支。
        if (fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 检查当前用户是否具有需要的权限：GrowthAccessUtils.manager(fresh.roles()) || !fresh.roles().contains("USER") || !fresh.permissions().contains(permission) || fresh.mustChangePassw…；成立时执行下面的分支。
        if (GrowthAccessUtils.manager(fresh.roles())
                // 继续补充同一个判断或备用结果：条件 || !fresh.roles().contains("USER") 是否成立。
                || !fresh.roles().contains("USER")
                // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| !fresh.permissions().contains(permission)。
                || !fresh.permissions().contains(permission)
                // 继续补充同一个判断或备用结果：条件 || fresh.mustChangePassword()) { 是否成立。
                || fresh.mustChangePassword()) {
            // 中止正常处理并抛出异常：调用 BusinessException.forbidden 完成forbidden处理；由上层按失败处理。
            throw BusinessException.forbidden();
        }
        // 准备登录（login），类型为 null；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        var login = logins.get(actor.sessionId());
        // 检查记录或凭据是否已经到期：login == null || login.expiresAt() <= System.currentTimeMillis() || !login.userId().equals(Long.toString(actor.userId())) || login.authVersion() …；成立时执行下面的分支。
        if (login == null
                // 继续补充同一个判断或备用结果：记录或凭据是否已经到期：|| login.expiresAt() <= System.currentTimeMillis()。
                || login.expiresAt() <= System.currentTimeMillis()
                // 继续补充同一个判断或备用结果：条件 || !login.userId().equals(Long.toString(actor.userId())) 是否成立。
                || !login.userId().equals(Long.toString(actor.userId()))
                // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| login.authVersion() != fresh.authVersion()) {。
                || login.authVersion() != fresh.authVersion()) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    private ExecutionReadDTO context(
            // actor，使用 AccountPrincipal 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            // 默认值今天（defaultToday），使用 boolean 类型保存。
            AccountPrincipal actor, ExecutionQueryDTO query, Instant now, boolean defaultToday) {
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 检查条件 query.getStatus() != null && !STATES.contains(query.getStatus()) 是否成立；成立时执行下面的分支。
        if (query.getStatus() != null && !STATES.contains(query.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("执行状态无效");
        }
        // 检查条件 query.getTaskId() != null 是否成立；成立时执行下面的分支。
        if (query.getTaskId() != null) {
            // 调用 requireScope(actor.userId 完成用户编号处理。
            requireScope(actor.userId(), ExecutionScopeEnum.TASK, query.getTaskId());
        }
        // 检查条件 query.getStageId() != null 是否成立；成立时执行下面的分支。
        if (query.getStageId() != null) {
            // 调用 requireScope(actor.userId 完成用户编号处理。
            requireScope(actor.userId(), ExecutionScopeEnum.STAGE, query.getStageId());
        }
        // 检查条件 query.getGoalId() != null 是否成立；成立时执行下面的分支。
        if (query.getGoalId() != null) {
            // 调用 requireScope(actor.userId 完成用户编号处理。
            requireScope(actor.userId(), ExecutionScopeEnum.GOAL, query.getGoalId());
        }
        // 准备来源（from），类型为 null；初始内容是读取 日期来源（DateFrom） 的值。
        var from = query.getDateFrom();
        // 准备to，类型为 null；初始内容是读取 日期To（DateTo） 的值。
        var to = query.getDateTo();
        // 检查from == null) != (to == null) || from != null && from.isAfter(to，即对应内容是否不存在；成立时执行下面的分支。
        if ((from == null) != (to == null) || from != null && from.isAfter(to)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("起止日期必须成对提供且顺序正确");
        }
        // 准备时区（zone），类型为 null；初始内容是调用 account(actor.userId 完成用户编号处理。
        var zone = account(actor.userId()).effectiveZone(now);
        // 检查from == null && defaultToday，即对应内容是否不存在；成立时执行下面的分支。
        if (from == null && defaultToday) {
            // 把调用 now.atZone 完成at时区处理保存到 from。
            from = now.atZone(zone).toLocalDate();
            // 把来源（from）保存到 to。
            to = from;
        }
        // 检查条件 to != null && to.getYear() >= 9999 || from != null && from.getYear() < 1 是否成立；成立时执行下面的分支。
        if (to != null && to.getYear() >= 9999 || from != null && from.getYear() < 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("查询日期超出支持范围");
        }
        // 把新建 执行计时读取（ExecutionReadDTO） 实例交还给调用方，本方法到此结束。
        return new ExecutionReadDTO(
                // 调用 actor.userId 完成用户编号处理。
                actor.userId(),
                // 调用 actor.sessionId 完成会话编号处理。
                actor.sessionId(),
                // 继续传入或组合当前时间（now），完成上面尚未结束的调用或声明。
                now,
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：from == null ? null : from.atStartOfDay(zone).toInstant()，完成上面尚未结束的调用或声明。
                from == null ? null : from.atStartOfDay(zone).toInstant(),
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant()，完成上面尚未结束的调用或声明。
                to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant(),
                // 调用 config.heartbeatIntervalSeconds 完成心跳间隔秒数处理。
                config.heartbeatIntervalSeconds());
    }

    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // actor，使用 AccountPrincipal 类型保存。
    // 编号（id），使用 long 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    private ExecutionSessionVO view(AccountPrincipal actor, long id, Instant now) {
        // 把调用 requiredView(new ExecutionReadDTO(actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return requiredView(
                // 继续传入或组合新建 执行计时读取（ExecutionReadDTO） 实例，完成上面尚未结束的调用或声明。
                new ExecutionReadDTO(
                        // 调用 actor.userId 完成用户编号处理。
                        actor.userId(),
                        // 调用 actor.sessionId 完成会话编号处理。
                        actor.sessionId(),
                        // 继续传入或组合当前时间（now），完成上面尚未结束的调用或声明。
                        now,
                        // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                        null,
                        // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                        null,
                        // 调用 config.heartbeatIntervalSeconds 完成心跳间隔秒数处理。
                        config.heartbeatIntervalSeconds()),
                // 继续传入或组合表达式 id) 的结果，完成上面尚未结束的调用或声明。
                id);
    }

    // 必填页面（requiredView）方法：要求满足 d页面（dView）；参数负责传入本次处理所需的数据。
    // 当前用户、权限和数据范围检查器（ctx），使用 ExecutionReadDTO 类型保存。
    // 编号（id），使用 long 类型保存。
    private ExecutionSessionVO requiredView(ExecutionReadDTO ctx, long id) {
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.view 完成页面处理。
        var row = mapper.view(ctx, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private ExecutionSessionEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 null；初始内容是取得第一条符合条件的数据。
        var row = mapper.find(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 账号（account）方法：调用 account 完成账号处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private ExecutionAccountData account(long userId) {
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.account 完成账号处理。
        var row = mapper.account(userId);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // eligible方法：调用 eligible 完成eligible处理；参数负责传入本次处理所需的数据。
    private ExecutionTaskVO eligible(
            // 用户编号（userId），使用 long 类型保存。
            // 任务编号（taskId），使用 long 类型保存。
            // 账号（account），使用 ExecutionAccountData 类型保存。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            long userId, long taskId, ExecutionAccountData account, Instant now) {
        // 准备任务（task），类型为 null；初始内容是调用 mapper.eligibleTask 完成eligible任务处理。
        var task =
                // 继续传入或组合调用 mapper.eligibleTask 完成eligible任务处理，完成上面尚未结束的调用或声明。
                mapper.eligibleTask(
                        // 继续传入或组合调用 userId, taskId, now.atZone 完成at时区处理，完成上面尚未结束的调用或声明。
                        userId, taskId, now.atZone(account.effectiveZone(now)).toLocalDate());
        // 检查task == null，即对应内容是否不存在；成立时执行下面的分支。
        if (task == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("任务当前不可执行，请检查父链状态、开始日期及本周／今日安排");
        }
        // 把任务（task）交还给调用方，本方法到此结束。
        return task;
    }

    // 追加连续计时段（appendSegment）方法：调用 appendSegment 完成追加连续计时段处理；参数负责传入本次处理所需的数据。
    private void appendSegment(
            // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
            ExecutionSessionEntity row,
            // 任务（task），使用 ExecutionTaskVO 类型保存。
            ExecutionTaskVO task,
            // 账号（account），使用 ExecutionAccountData 类型保存。
            ExecutionAccountData account,
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            Instant now) {
        // 检查条件 segments.open(row.getUserId(), row.getId()) != null 是否成立；成立时执行下面的分支。
        if (segments.open(row.getUserId(), row.getId()) != null) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("会话已有开放时间段");
        }
        // 准备时区（zone），类型为 null；初始内容是调用 account.effectiveZone 完成生效时区处理。
        var zone = account.effectiveZone(now);
        // 准备boundary日期（boundaryDate），类型为 LocalDate；初始内容是根据前面的条件，从两个候选结果中选择一个：task.planDate() == null ? task.weekStartDate().plusDays(7) : task.planDate().plusDays(1)。
        LocalDate boundaryDate =
                // 继续传入或组合调用 task.planDate 完成方案日期处理，完成上面尚未结束的调用或声明。
                task.planDate() == null
                        // 继续补充同一个判断或备用结果：条件 ? task.weekStartDate().plusDays(7) 是否成立。
                        ? task.weekStartDate().plusDays(7)
                        // 继续补充同一个判断或备用结果：条件 : task.planDate().plusDays(1); 是否成立。
                        : task.planDate().plusDays(1);
        // 准备截止时间（deadline），类型为 Instant；初始内容是调用 boundaryDate.atStartOfDay 完成at开始Of日处理。
        Instant deadline = boundaryDate.atStartOfDay(zone).toInstant();
        // 准备等待处理（pending），类型为 Instant；初始内容是调用 account.pendingBoundary 完成等待处理Boundary处理。
        Instant pending = account.pendingBoundary(now);
        // 检查条件 pending != null && pending.isBefore(deadline) 是否成立；成立时执行下面的分支。
        if (pending != null && pending.isBefore(deadline)) {
            // 把等待处理（pending）保存到 deadline。
            deadline = pending;
        }
        // 检查条件 !deadline.isAfter(now) 是否成立；成立时执行下面的分支。
        if (!deadline.isAfter(now)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("本次安排已到执行边界，请重新安排");
        }
        // 准备连续计时段（segment），类型为 null；初始内容是新建 执行计时连续计时段（ExecutionSegmentEntity） 实例。
        var segment = new ExecutionSegmentEntity();
        // 设置 执行计时编号（ExecutionId） 的值。
        segment.setExecutionId(row.getId());
        // 设置 任务安排编号（AssignmentId） 的值。
        segment.setAssignmentId(task.assignmentId());
        // 设置 连续计时段No（SegmentNo） 的值。
        segment.setSegmentNo(segments.nextNumber(row.getUserId(), row.getId()));
        // 设置 开始时At（StartedAt） 的值。
        segment.setStartedAt(now);
        // 设置 EligibleUntil 的值。
        segment.setEligibleUntil(deadline);
        // 设置 时间时区（TimeZone） 的值。
        segment.setTimeZone(zone.getId());
        // 设置 心跳超时秒数（HeartbeatTimeoutSeconds） 的值。
        segment.setHeartbeatTimeoutSeconds(config.heartbeatTimeoutSeconds());
        // 设置 创建时At（CreatedAt） 的值。
        segment.setCreatedAt(now);
        // 设置 更新时At（UpdatedAt） 的值。
        segment.setUpdatedAt(now);
        // 调用 changed(segments.insert 完成写入新记录处理。
        changed(segments.insert(segment));
    }

    // activate方法：调用 activate 完成activate处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    // 本次登录会话编号（sid），使用 String 类型保存；可为 null，表示尚未提供。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    private void activate(ExecutionSessionEntity row, String sid, Instant now) {
        // 检查条件 !PAUSED.equals(row.getStatus()) || segments.open(row.getUserId(), row.getId()) != null 是否成立；成立时执行下面的分支。
        if (!PAUSED.equals(row.getStatus())
                // 继续补充同一个判断或备用结果：条件 || segments.open(row.getUserId(), row.getId()) != null) { 是否成立。
                || segments.open(row.getUserId(), row.getId()) != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("会话状态已变化，不能继续");
        }
        // 设置 状态（Status） 的值。
        row.setStatus(RUNNING);
        // 设置 请求入口会话编号（ControllerSessionId） 的值。
        row.setControllerSessionId(sid);
        // 设置 最后一次心跳At（LastHeartbeatAt） 的值。
        row.setLastHeartbeatAt(now);
        // 调用 persist 完成持久保存处理。
        persist(row, now);
    }

    // 是否展开（open）方法：调用 open 完成是否展开处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    private ExecutionSegmentEntity open(ExecutionSessionEntity row) {
        // 准备连续计时段（segment），类型为 null；初始内容是调用 segments.open 完成是否展开处理。
        var segment = segments.open(row.getUserId(), row.getId());
        // 检查segment == null，即对应内容是否不存在；成立时执行下面的分支。
        if (segment == null) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("运行会话缺少开放时间段");
        }
        // 把连续计时段（segment）交还给调用方，本方法到此结束。
        return segment;
    }

    // 截止时间（deadline）方法：调用 deadline 完成截止时间处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    // 连续计时段（segment），使用 ExecutionSegmentEntity 类型保存。
    private Instant deadline(ExecutionSessionEntity row, ExecutionSegmentEntity segment) {
        // 准备心跳（heartbeat），类型为 Instant；初始内容是读取 最后一次心跳At（LastHeartbeatAt） 的值。
        Instant heartbeat =
                // 读取 最后一次心跳At（LastHeartbeatAt） 的值。
                row.getLastHeartbeatAt().plusSeconds(segment.getHeartbeatTimeoutSeconds());
        // 把根据前面的条件，从两个候选结果中选择一个：heartbeat.isBefore(segment.getEligibleUntil()) ? heartbeat : segment.getEligibleUntil()交还给调用方，本方法到此结束。
        return heartbeat.isBefore(segment.getEligibleUntil())
                // 继续补充同一个判断或备用结果：条件 ? heartbeat 是否成立。
                ? heartbeat
                // 继续补充同一个判断或备用结果：条件 : segment.getEligibleUntil(); 是否成立。
                : segment.getEligibleUntil();
    }

    // 暂停At（pauseAt）方法：调用 pauseAt 完成暂停At处理；参数负责传入本次处理所需的数据。
    private void pauseAt(
            // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
            ExecutionSessionEntity row,
            // 连续计时段（segment），使用 ExecutionSegmentEntity 类型保存。
            ExecutionSegmentEntity segment,
            // requested，使用 Instant 类型保存；可为 null，表示尚未提供。
            Instant requested,
            // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
            String reason,
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            Instant now) {
        // 准备结束（end），类型为 Instant；初始内容是根据前面的条件，从两个候选结果中选择一个：requested.isBefore(deadline(row, segment)) ? requested : deadline(row, segment)。
        Instant end =
                // 根据前面的条件，从两个候选结果中选择一个：requested.isBefore(deadline(row, segment)) ? requested : deadline(row, segment)。
                requested.isBefore(deadline(row, segment)) ? requested : deadline(row, segment);
        // 检查条件 end.isBefore(segment.getStartedAt()) 是否成立；成立时执行下面的分支。
        if (end.isBefore(segment.getStartedAt())) {
            // 把读取 开始时At（StartedAt） 的值保存到 end。
            end = segment.getStartedAt();
        }
        // 释放连接、文件或客户端占用的资源。
        changed(segments.close(row.getUserId(), segment.getId(), end, reason, now));
        // 设置 状态（Status） 的值。
        row.setStatus(PAUSED);
        // 设置 请求入口会话编号（ControllerSessionId） 的值。
        row.setControllerSessionId(null);
        // 调用 persist 完成持久保存处理。
        persist(row, now);
        // 调用 audit.recordGrowthSuccess 完成记录成长业务成功处理。
        audit.recordGrowthSuccess(
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：USER_PAUSE.equals(reason) ? PAUSE : "execution:session:auto_pause"，完成上面尚未结束的调用或声明。
                USER_PAUSE.equals(reason) ? PAUSE : "execution:session:auto_pause",
                // 读取 用户编号（UserId） 的值。
                row.getUserId());
    }

    // finishAt方法：调用 finishAt 完成finishAt处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    private void finishAt(ExecutionSessionEntity row, String reason, Instant now) {
        // 检查条件 RUNNING.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
        if (RUNNING.equals(row.getStatus())) {
            // 准备连续计时段（segment），类型为 null；初始内容是调用 open 完成是否展开处理。
            var segment = open(row);
            // 准备结束（end），类型为 Instant；初始内容是根据前面的条件，从两个候选结果中选择一个：now.isBefore(deadline(row, segment)) ? now : deadline(row, segment)。
            Instant end = now.isBefore(deadline(row, segment)) ? now : deadline(row, segment);
            // 释放连接、文件或客户端占用的资源。
            changed(segments.close(row.getUserId(), segment.getId(), end, FINISH, now));
        // 检查条件 segments.open(row.getUserId(), row.getId()) != null 是否成立；成立时执行下面的分支。
        } else if (segments.open(row.getUserId(), row.getId()) != null) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("非运行会话仍有开放时间段");
        }
        // 设置 状态（Status） 的值。
        row.setStatus(FINISHED);
        // 设置 FinishedAt 的值。
        row.setFinishedAt(now);
        // 设置 Finish原因（FinishReason） 的值。
        row.setFinishReason(reason);
        // 设置 请求入口会话编号（ControllerSessionId） 的值。
        row.setControllerSessionId(null);
        // 调用 persist 完成持久保存处理。
        persist(row, now);
        // 调用 audit.recordGrowthSuccess 完成记录成长业务成功处理。
        audit.recordGrowthSuccess(END, row.getUserId());
    }

    // 持久保存（persist）方法：调用 persist 完成持久保存处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    private void persist(ExecutionSessionEntity row, Instant now) {
        // 准备旧版本号（oldVersion），类型为 long；初始内容是读取 版本号（Version） 的值。
        long oldVersion = row.getVersion();
        // 调用 changed(mapper.changeState 完成修改当前状态处理。
        changed(mapper.changeState(row, oldVersion, now));
        // 设置 版本号（Version） 的值。
        row.setVersion(oldVersion + 1);
    }

    // normalize方法：调用 normalize 完成normalize处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private void normalize(long userId) {
        // 调用 maintenance.executeWithoutResult 完成执行Without结果处理。
        maintenance.executeWithoutResult(
                // 状态（status），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                status -> {
                    // 调用 lock 完成加锁处理。
                    lock(userId);
                    // 统一处理 Locked。
                    normalizeLocked(userId, now(), true);
                });
    }

    /* 已持有用户锁；父操作写入后可关闭资格自动暂停，只做截止／认证归一化，再显式拒绝无效父修改。 */
    // normalizeLocked方法：统一处理 Locked；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    // 核对Eligibility（checkEligibility），使用 boolean 类型保存。
    private void normalizeLocked(long userId, Instant now, boolean checkEligibility) {
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.running 完成运行中处理。
        var row = mapper.running(userId);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备连续计时段（segment），类型为 null；初始内容是调用 open 完成是否展开处理。
        var segment = open(row);
        // 准备结束（end），类型为 Instant；初始内容是调用 deadline 完成截止时间处理。
        Instant end = deadline(row, segment);
        // 检查条件 !end.isAfter(now) 是否成立；成立时执行下面的分支。
        if (!end.isAfter(now)) {
            // 准备原因（reason），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：segment.getEligibleUntil().isAfter(end) ? AUTO_PAUSE : PLAN_BOUNDARY。
            String reason = segment.getEligibleUntil().isAfter(end) ? AUTO_PAUSE : PLAN_BOUNDARY;
            // 调用 pauseAt 完成暂停At处理。
            pauseAt(row, segment, end, reason, now);
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备账号（account），类型为 null；初始内容是调用 account 完成账号处理。
        var account = account(userId);
        // 准备登录（login），类型为 null；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        var login = logins.get(row.getControllerSessionId());
        // 准备security，类型为 null；初始内容是读取 对象（Object） 的值。
        var security = accounts.getObject().loadSecurityAccount(userId);
        // 检查记录或凭据是否已经到期：!account.enabled() || security == null || GrowthAccessUtils.manager(security.roles()) || login == null || login.expiresAt() <= now.toEpochMilli()…；成立时执行下面的分支。
        if (!account.enabled()
                // 继续补充同一个判断或备用结果：|| security == null，即对应内容是否不存在。
                || security == null
                // 继续补充同一个判断或备用结果：条件 || GrowthAccessUtils.manager(security.roles()) 是否成立。
                || GrowthAccessUtils.manager(security.roles())
                // 继续补充同一个判断或备用结果：|| login == null，即对应内容是否不存在。
                || login == null
                // 继续补充同一个判断或备用结果：记录或凭据是否已经到期：|| login.expiresAt() <= now.toEpochMilli()。
                || login.expiresAt() <= now.toEpochMilli()
                // 继续补充同一个判断或备用结果：条件 || !login.userId().equals(Long.toString(userId)) 是否成立。
                || !login.userId().equals(Long.toString(userId))
                // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| login.authVersion() != account.authVersion()) {。
                || login.authVersion() != account.authVersion()) {
            // 调用 pauseAt 完成暂停At处理。
            pauseAt(row, segment, now, AUTH_REVOKED, now);
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 检查条件 checkEligibility 是否成立；成立时执行下面的分支。
        if (checkEligibility) {
            // 准备任务（task），类型为 null；初始内容是调用 mapper.eligibleTask 完成eligible任务处理。
            var task =
                    // 继续传入或组合调用 mapper.eligibleTask 完成eligible任务处理，完成上面尚未结束的调用或声明。
                    mapper.eligibleTask(
                            // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
                            userId,
                            // 读取 任务编号（TaskId） 的值。
                            row.getTaskId(),
                            // 调用 now.atZone 完成at时区处理。
                            now.atZone(account.effectiveZone(now)).toLocalDate());
            // 检查task == null || !"IN_PROGRESS".equals(task.taskStatus())，即对应内容是否不存在；成立时执行下面的分支。
            if (task == null || !"IN_PROGRESS".equals(task.taskStatus())) {
                // 调用 pauseAt 完成暂停At处理。
                pauseAt(row, segment, now, PLAN_BOUNDARY, now);
            }
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // scan已过期（scanExpired）方法：调用 scanExpired 完成scan已过期处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void scanExpired() {
        // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long userId : mapper.dueUsers(now(), config.scanBatchSize()))；每轮处理一条数据或推进一次检查。
        for (Long userId : mapper.dueUsers(now(), config.scanBatchSize())) {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 maintenance.executeWithoutResult 完成执行Without结果处理。
                maintenance.executeWithoutResult(
                        // 状态（status），使用 null 类型保存。
                        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                        status -> {
                            // 检查条件 mapper.tryLockUser(userId) != null 是否成立；成立时执行下面的分支。
                            if (mapper.tryLockUser(userId) != null) {
                                // 统一处理 Locked。
                                normalizeLocked(userId, now(), true);
                            }
                        });
            // 错误（error），使用 RuntimeException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (RuntimeException error) {
                // 记录error级别的运行信息，供定位处理过程和失败原因。
                LOGGER.error("执行到期处理失败，用户标识：{}", userId, error);
            }
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // require任务Finished（requireTaskFinished）方法：要求满足 任务Finished（TaskFinished）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 任务编号（taskId），使用 long 类型保存。
    public void requireTaskFinished(long userId, long taskId) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 统一处理 Locked。
        normalizeLocked(userId, now(), false);
        // 检查条件 mapper.unfinishedTask(userId, taskId) != null 是否成立；成立时执行下面的分支。
        if (mapper.unfinishedTask(userId, taskId) != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("请先结束该任务的运行或暂停执行，再完成任务");
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // requireNo运行中（requireNoRunning）方法：要求满足 No运行中（NoRunning）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 long 类型保存。
    public void requireNoRunning(long userId, ExecutionScopeEnum scope, long id) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 统一处理 Locked。
        normalizeLocked(userId, now(), false);
        // 检查条件 mapper.runningInScope(userId, scope.name(), id) 是否成立；成立时执行下面的分支。
        if (mapper.runningInScope(userId, scope.name(), id)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("请先暂停正在执行的任务，再调整安排或删除计划");
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 检查合法性运行中Eligibility（validateRunningEligibility）方法：重新核对正在运行的计时是否仍满足任务和计划条件；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    public void validateRunningEligibility(long userId) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 统一处理 Locked。
        normalizeLocked(userId, now, false);
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.running 完成运行中处理。
        var row = mapper.running(userId);
        // 检查条件 row != null 是否成立；成立时执行下面的分支。
        if (row != null) {
            // 准备账号（account），类型为 null；初始内容是调用 account 完成账号处理。
            var account = account(userId);
            // 准备任务（task），类型为 null；初始内容是调用 mapper.eligibleTask 完成eligible任务处理。
            var task =
                    // 继续传入或组合调用 mapper.eligibleTask 完成eligible任务处理，完成上面尚未结束的调用或声明。
                    mapper.eligibleTask(
                            // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
                            userId,
                            // 读取 任务编号（TaskId） 的值。
                            row.getTaskId(),
                            // 调用 now.atZone 完成at时区处理。
                            now.atZone(account.effectiveZone(now)).toLocalDate());
            // 检查task == null || !"IN_PROGRESS".equals(task.taskStatus())，即对应内容是否不存在；成立时执行下面的分支。
            if (task == null || !"IN_PROGRESS".equals(task.taskStatus())) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("本次修改会使正在执行的任务失效，请先暂停执行");
            }
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // finish任务（finishTask）方法：调用 finishTask 完成finish任务处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 任务编号（taskId），使用 long 类型保存。
    public void finishTask(long userId, long taskId) {
        // 调用 finishScope 完成finish数据范围处理。
        finishScope(userId, ExecutionScopeEnum.TASK, taskId, TASK_DELETED);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // finish目标（finishGoal）方法：调用 finishGoal 完成finish目标处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    public void finishGoal(long userId, long goalId) {
        // 调用 finishScope 完成finish数据范围处理。
        finishScope(userId, ExecutionScopeEnum.GOAL, goalId, GOAL_ARCHIVED);
    }

    // finish数据范围（finishScope）方法：调用 finishScope 完成finish数据范围处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 long 类型保存。
    // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
    private void finishScope(long userId, ExecutionScopeEnum scope, long id, String reason) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 统一处理 Locked。
        normalizeLocked(userId, now, false);
        // 逐次执行循环：while (true)；每轮处理一条数据或推进一次检查。
        while (true) {
            // 准备批量（batch），类型为 null；初始内容是调用 mapper.unfinished 完成unfinished处理。
            var batch = mapper.unfinished(userId, scope.name(), id, config.scanBatchSize());
            // 检查输入或集合是否为空：batch.isEmpty()；成立时执行下面的分支。
            if (batch.isEmpty()) {
                // 把返回（return）交还给调用方，本方法到此结束。
                return;
            }
            // 当前记录（row），使用 null 类型保存。
            // 逐次执行循环：for (/*missing* / row : batch)；每轮处理一条数据或推进一次检查。
            for (var row : batch) {
                // 调用 finishAt 完成finishAt处理。
                finishAt(row, reason, now);
            }
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 暂停登录（pauseLogin）方法：调用 pauseLogin 完成暂停登录处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    public void pauseLogin(long userId, String sessionId) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 统一处理 Locked。
        normalizeLocked(userId, now, false);
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.running 完成运行中处理。
        var row = mapper.running(userId);
        // 检查row != null && (sessionId == null || sessionId.equals(row.getControllerSessionId()))，即对应内容是否不存在；成立时执行下面的分支。
        if (row != null && (sessionId == null || sessionId.equals(row.getControllerSessionId()))) {
            // 调用 pauseAt 完成暂停At处理。
            pauseAt(row, open(row), now, AUTH_REVOKED, now);
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // shortenBoundary方法：调用 shortenBoundary 完成shortenBoundary处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 生效At（effectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    public void shortenBoundary(long userId, Instant effectiveAt) {
        // 调用 lifecycleLock 完成lifecycle加锁处理。
        lifecycleLock(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 统一处理 Locked。
        normalizeLocked(userId, now, false);
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.running 完成运行中处理。
        var row = mapper.running(userId);
        // 检查row == null || effectiveAt == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null || effectiveAt == null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备连续计时段（segment），类型为 null；初始内容是调用 open 完成是否展开处理。
        var segment = open(row);
        // 检查条件 effectiveAt.isAfter(segment.getStartedAt()) && effectiveAt.isBefore(segment.getEligibleUntil()) 是否成立；成立时执行下面的分支。
        if (effectiveAt.isAfter(segment.getStartedAt())
                // 继续补充同一个判断或备用结果：条件 && effectiveAt.isBefore(segment.getEligibleUntil())) { 是否成立。
                && effectiveAt.isBefore(segment.getEligibleUntil())) {
            // 调用 changed(segments.shorten 完成shorten处理。
            changed(segments.shorten(userId, row.getId(), effectiveAt, now));
            // 统一处理 Locked。
            normalizeLocked(userId, now, false);
        }
    }

    // lifecycle加锁（lifecycleLock）方法：调用 lifecycleLock 完成lifecycle加锁处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private void lifecycleLock(long userId) {
        // 检查条件 !TransactionSynchronizationManager.isActualTransactionActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("执行联动必须参加父业务事务");
        }
        // 调用 lock 完成加锁处理。
        lock(userId);
    }

    // 加锁（lock）方法：调用 lock 完成加锁处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private void lock(long userId) {
        // 检查mapper.lockUser(userId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (mapper.lockUser(userId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
    }

    // require数据范围（requireScope）方法：要求满足 数据范围（Scope）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 数据范围（scope），使用 ExecutionScopeEnum 类型保存。
    // 编号（id），使用 long 类型保存。
    private void requireScope(long userId, ExecutionScopeEnum scope, long id) {
        // 调用 positive 完成正数处理。
        positive(id);
        // 检查mapper.scopeExists(userId, scope.name(), id) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (mapper.scopeExists(userId, scope.name(), id) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
    }

    // 当前时间（now）方法：调用 now 完成当前时间处理；参数负责传入本次处理所需的数据。
    private static Instant now() {
        // 把当前真实时间，用于记录操作发生的时刻交还给调用方，本方法到此结束。
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    // 正数（positive）方法：调用 positive 完成正数处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 Long 类型保存；可为 null，表示尚未提供。
    private static void positive(Long value) {
        // 检查value == null || value <= 0，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value <= 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("资源标识必须为正数");
        }
    }

    // 预计（expected）方法：调用 expected 完成预计处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 Long 类型保存；可为 null，表示尚未提供。
    private static void expected(Long value) {
        // 检查value == null || value < 0，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value < 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("必须提供有效版本");
        }
    }

    // 版本号（version）方法：调用 version 完成版本号处理；参数负责传入本次处理所需的数据。
    // 实际（actual），使用 Long 类型保存；可为 null，表示尚未提供。
    // 预计（expected），使用 Long 类型保存；可为 null，表示尚未提供。
    private static void version(Long actual, Long expected) {
        // 检查条件 !Objects.equals(actual, expected) 是否成立；成立时执行下面的分支。
        if (!Objects.equals(actual, expected)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("数据版本已变化，请刷新后重试");
        }
    }

    // 已修改（changed）方法：调用 changed 完成已修改处理；参数负责传入本次处理所需的数据。
    // 数量（count），使用 int 类型保存。
    private static void changed(int count) {
        // 检查条件 count != 1 是否成立；成立时执行下面的分支。
        if (count != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("执行数据已变化，请刷新后重试");
        }
    }
}
