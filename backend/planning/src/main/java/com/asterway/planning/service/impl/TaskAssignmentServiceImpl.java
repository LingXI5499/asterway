/*
 * 实现任务任务安排业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.service.impl;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入执行计时数据范围Enum（ExecutionScopeEnum），下面可以直接使用这个类型或静态成员。
import com.asterway.common.enums.ExecutionScopeEnum;
// 引入执行计时Lifecycle提供器（ExecutionLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionLifecycleProvider;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入任务安排保存（AssignmentSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.dto.AssignmentSaveDTO;
// 引入任务任务安排（TaskAssignmentEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.entity.TaskAssignmentEntity;
// 引入规划任务数据库读写器（PlanningTaskMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.mapper.PlanningTaskMapper;
// 引入任务任务安排数据库读写器（TaskAssignmentMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.mapper.TaskAssignmentMapper;
// 引入任务任务安排业务服务（TaskAssignmentService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.TaskAssignmentService;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;
// 引入任务任务安排（TaskAssignmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.TaskAssignmentVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;

/* 任务安排唯一写入口：保留安排历史，不复制任务，不自动延期。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 TaskAssignmentServiceImpl；实现任务任务安排业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class TaskAssignmentServiceImpl implements TaskAssignmentService {
    // 访问数据库的 Mapper（mapper），使用 TaskAssignmentMapper 类型保存。
    private final TaskAssignmentMapper mapper;
    // 任务列表（tasks），使用 PlanningTaskMapper 类型保存。
    private final PlanningTaskMapper tasks;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public TaskAssignmentServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 TaskAssignmentMapper 类型保存。
            // 任务列表（tasks），使用 PlanningTaskMapper 类型保存。
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            TaskAssignmentMapper mapper, PlanningTaskMapper tasks, GrowthContextUtils ctx,
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            ExecutionLifecycleProvider execution) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把任务列表（tasks）保存到 this.tasks。
        this.tasks = tasks;
        // 把当前用户、权限和数据范围检查器（ctx）保存到 this.ctx。
        this.ctx = ctx;
        // 把执行计时（execution）保存到 this.execution。
        this.execution = execution;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 任务编号（taskId），使用 long 类型保存。
    public TaskAssignmentVO get(long taskId) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:assignment:read");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, taskId, false);
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.current 完成当前处理。
        var row = mapper.current(userId, taskId);
        // 把根据前面的条件，从两个候选结果中选择一个：row == null ? null : toVO(row)交还给调用方，本方法到此结束。
        return row == null ? null : toVO(row);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 安排（assign）方法：调用 assign 完成安排处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 任务编号（taskId），使用 long 类型保存。
    // 请求参数对象（dto），使用 AssignmentSaveDTO 类型保存。
    public TaskAssignmentVO assign(long taskId, AssignmentSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("planning:assignment:manage");
        // 准备任务（task），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var task = ctx.scope("task", userId, taskId, true);
        // 检查条件 "COMPLETED".equals(task.getStatus()) 是否成立；成立时执行下面的分支。
        if ("COMPLETED".equals(task.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("已完成任务不能重新安排");
        }
        // 准备周（week），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var week = ctx.scope("week", userId, dto.weeklyPlanId(), false);
        // 检查条件 week.getWeekStartDate().plusDays(6).isBefore(ctx.today(userId)) 是否成立；成立时执行下面的分支。
        if (week.getWeekStartDate().plusDays(6).isBefore(ctx.today(userId))) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("不能安排到已结束的周");
        }
        // 检查条件 dto.dailyPlanId() != null 是否成立；成立时执行下面的分支。
        if (dto.dailyPlanId() != null) {
            // 准备日（day），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
            var day = ctx.scope("day", userId, dto.dailyPlanId(), false);
            // 检查条件 !Objects.equals(day.getWeeklyPlanId(), dto.weeklyPlanId()) || day.getPlanDate().isBefore(week.getWeekStartDate()) || day.getPlanDate().isAfter(we… 是否成立；成立时执行下面的分支。
            if (!Objects.equals(day.getWeeklyPlanId(), dto.weeklyPlanId())
                    // 继续补充同一个判断或备用结果：条件 || day.getPlanDate().isBefore(week.getWeekStartDate()) 是否成立。
                    || day.getPlanDate().isBefore(week.getWeekStartDate())
                    // 继续补充同一个判断或备用结果：条件 || day.getPlanDate().isAfter(week.getWeekStartDate().plusDays(6))) { 是否成立。
                    || day.getPlanDate().isAfter(week.getWeekStartDate().plusDays(6))) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("日计划与周计划不一致");
            }
            // 检查条件 day.getPlanDate().isBefore(ctx.today(userId)) 是否成立；成立时执行下面的分支。
            if (day.getPlanDate().isBefore(ctx.today(userId))) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("不能安排到过去日期");
            }
        }
        // 准备旧（old），类型为 TaskAssignmentEntity；初始内容是调用 mapper.current 完成当前处理。
        TaskAssignmentEntity old = mapper.current(userId, taskId);
        // 准备顺序（order），类型为 int；初始内容是调用 ctx.order 完成顺序处理。
        int order = ctx.order(dto.sortOrder());
        // 检查old == null，即对应内容是否不存在；成立时执行下面的分支。
        if (old == null) {
            // 检查当前记录版本是否符合请求要求：dto.assignmentId() != null || dto.version() != null；成立时执行下面的分支。
            if (dto.assignmentId() != null || dto.version() != null) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("原安排已变化，请刷新");
            }
        // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
        } else {
            // 检查条件 !Objects.equals(old.getId(), dto.assignmentId()) 是否成立；成立时执行下面的分支。
            if (!Objects.equals(old.getId(), dto.assignmentId())) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("当前安排已变化，请刷新");
            }
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(old.getVersion(), dto.version());
            // 检查条件 Objects.equals(old.getWeeklyPlanId(), dto.weeklyPlanId()) && Objects.equals(old.getDailyPlanId(), dto.dailyPlanId()) 是否成立；成立时执行下面的分支。
            if (Objects.equals(old.getWeeklyPlanId(), dto.weeklyPlanId())
                    // 继续补充同一个判断或备用结果：条件 && Objects.equals(old.getDailyPlanId(), dto.dailyPlanId())) { 是否成立。
                    && Objects.equals(old.getDailyPlanId(), dto.dailyPlanId())) {
                // 检查条件 !Objects.equals(old.getSortOrder(), order) 是否成立；成立时执行下面的分支。
                if (!Objects.equals(old.getSortOrder(), order)) {
                    // 设置 排序顺序（SortOrder） 的值。
                    old.setSortOrder(order);
                    // 核对数据库实际更新条数，失败时按冲突处理。
                    ctx.changed(mapper.update(old));
                }
                // 把调用 toVO(mapper.current 完成当前处理交还给调用方，本方法到此结束。
                return toVO(mapper.current(userId, taskId));
            }
            // 要求满足 No运行中（NoRunning）。
            execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
            // 设置 已结束At（EndedAt） 的值。
            old.setEndedAt(Instant.now());
            // 设置 结束原因（EndReason） 的值。
            old.setEndReason("RESCHEDULED");
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(mapper.update(old));
        }
        // 准备当前记录（row），类型为 TaskAssignmentEntity；初始内容是新建 任务任务安排（TaskAssignmentEntity） 实例。
        TaskAssignmentEntity row = new TaskAssignmentEntity();
        // 检查old == null，即对应内容是否不存在；成立时执行下面的分支。
        if (old == null) {
            // 要求满足 No运行中（NoRunning）。
            execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
        }
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 任务编号（TaskId） 的值。
        row.setTaskId(taskId);
        // 设置 每周方案编号（WeeklyPlanId） 的值。
        row.setWeeklyPlanId(dto.weeklyPlanId());
        // 设置 每日方案编号（DailyPlanId） 的值。
        row.setDailyPlanId(dto.dailyPlanId());
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(order);
        // 设置 AssignedAt 的值。
        row.setAssignedAt(Instant.now());
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 把调用 toVO(mapper.current 完成当前处理交还给调用方，本方法到此结束。
        return toVO(mapper.current(userId, taskId));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 移除（remove）方法：调用 remove 完成移除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 任务编号（taskId），使用 long 类型保存。
    // 任务安排编号（assignmentId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void remove(long taskId, long assignmentId, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("planning:assignment:remove");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, taskId, true);
        // 准备当前记录（row），类型为 TaskAssignmentEntity；初始内容是调用 mapper.current 完成当前处理。
        TaskAssignmentEntity row = mapper.current(userId, taskId);
        // 检查row == null || !Objects.equals(row.getId(), assignmentId)，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null || !Objects.equals(row.getId(), assignmentId)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("当前安排已变化，请刷新");
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 要求满足 No运行中（NoRunning）。
        execution.requireNoRunning(userId, ExecutionScopeEnum.TASK, taskId);
        // 设置 已结束At（EndedAt） 的值。
        row.setEndedAt(Instant.now());
        // 设置 结束原因（EndReason） 的值。
        row.setEndReason("RETURNED_TO_POOL");
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    @Override
    // 任务编号（taskId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public PageResultVO<TaskAssignmentVO> history(long taskId, GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:assignment:read");
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 设置 任务编号（TaskId） 的值。
        query.setTaskId(taskId);
        // 设置 当前仅（CurrentOnly） 的值。
        query.setCurrentOnly(false);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 把前面筛选、转换后的结果汇成列表。
                mapper.page(userId, query).stream().map(this::toVO).toList(),
                // 统计符合条件的记录数，具体 SQL 对应 count。
                mapper.count(userId, query),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 等待处理（pending）方法：调用 pending 完成等待处理处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public PageResultVO<PlanningTaskVO> pending(GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:pending:list");
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 tasks.page 完成当前页处理。
                tasks.page(userId, query, true),
                // 调用 tasks.count 完成数量处理。
                tasks.count(userId, query, true),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 TaskAssignmentEntity 类型保存。
    private TaskAssignmentVO toVO(TaskAssignmentEntity row) {
        // 把新建 任务任务安排（TaskAssignmentVO） 实例交还给调用方，本方法到此结束。
        return new TaskAssignmentVO(
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getTaskId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getWeeklyPlanId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getDailyPlanId()),
                // 读取 排序顺序（SortOrder） 的值。
                row.getSortOrder(),
                // 读取 AssignedAt 的值。
                row.getAssignedAt(),
                // 读取 已结束At（EndedAt） 的值。
                row.getEndedAt(),
                // 读取 结束原因（EndReason） 的值。
                row.getEndReason(),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 读取 更新时At（UpdatedAt） 的值。
                row.getUpdatedAt());
    }
}
