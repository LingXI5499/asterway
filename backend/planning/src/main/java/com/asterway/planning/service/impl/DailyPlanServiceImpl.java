/*
 * 实现每日方案业务服务业务规则，处理权限、状态、数据读写和必要的事务。
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
// 引入每日方案（DailyPlanEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.entity.DailyPlanEntity;
// 引入任务任务安排（TaskAssignmentEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.entity.TaskAssignmentEntity;
// 引入每日方案数据库读写器（DailyPlanMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.mapper.DailyPlanMapper;
// 引入规划任务数据库读写器（PlanningTaskMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.mapper.PlanningTaskMapper;
// 引入任务任务安排数据库读写器（TaskAssignmentMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.mapper.TaskAssignmentMapper;
// 引入每日方案业务服务（DailyPlanService），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.service.DailyPlanService;
// 引入每日方案（DailyPlanVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.DailyPlanVO;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;

/* 日计划按需创建，日期、所属周和用户必须一致。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 DailyPlanServiceImpl；实现每日方案业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class DailyPlanServiceImpl implements DailyPlanService {
    // 访问数据库的 Mapper（mapper），使用 DailyPlanMapper 类型保存。
    private final DailyPlanMapper mapper;
    // 任务安排列表（assignments），使用 TaskAssignmentMapper 类型保存。
    private final TaskAssignmentMapper assignments;
    // 任务列表（tasks），使用 PlanningTaskMapper 类型保存。
    private final PlanningTaskMapper tasks;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public DailyPlanServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 DailyPlanMapper 类型保存。
            DailyPlanMapper mapper,
            // 任务安排列表（assignments），使用 TaskAssignmentMapper 类型保存。
            TaskAssignmentMapper assignments,
            // 任务列表（tasks），使用 PlanningTaskMapper 类型保存。
            PlanningTaskMapper tasks,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            GrowthContextUtils ctx, ExecutionLifecycleProvider execution) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把任务安排列表（assignments）保存到 this.assignments。
        this.assignments = assignments;
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
    // 编号（id），使用 long 类型保存。
    public DailyPlanVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:daily:read");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("day", userId, id, false);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 查找按日期（findByDate）方法：查找 按日期（ByDate）；参数负责传入本次处理所需的数据。
    @Override
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public DailyPlanVO findByDate(long weeklyPlanId, LocalDate date) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:daily:read");
        // 准备周（week），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var week = ctx.scope("week", userId, weeklyPlanId, false);
        // 检查条件 date.isBefore(week.getWeekStartDate()) || date.isAfter(week.getWeekStartDate().plusDays(6)) 是否成立；成立时执行下面的分支。
        if (date.isBefore(week.getWeekStartDate())
                // 继续补充同一个判断或备用结果：条件 || date.isAfter(week.getWeekStartDate().plusDays(6))) { 是否成立。
                || date.isAfter(week.getWeekStartDate().plusDays(6))) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("日期不属于对应周计划");
        }
        // 准备当前记录（row），类型为 null；初始内容是调用 mapper.byDate 完成按日期处理。
        var row = mapper.byDate(userId, date);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
            return null;
        }
        // 检查条件 !Objects.equals(row.getWeeklyPlanId(), weeklyPlanId) 是否成立；成立时执行下面的分支。
        if (!Objects.equals(row.getWeeklyPlanId(), weeklyPlanId)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("日计划所属周不一致");
        }
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(row);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 按日期（byDate）方法：调用 byDate 完成按日期处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public DailyPlanVO byDate(long weeklyPlanId, LocalDate date) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("planning:daily:manage");
        // 准备周（week），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var week = ctx.scope("week", userId, weeklyPlanId, false);
        // 检查条件 date.isBefore(week.getWeekStartDate()) || date.isAfter(week.getWeekStartDate().plusDays(6)) 是否成立；成立时执行下面的分支。
        if (date.isBefore(week.getWeekStartDate())
                // 继续补充同一个判断或备用结果：条件 || date.isAfter(week.getWeekStartDate().plusDays(6))) { 是否成立。
                || date.isAfter(week.getWeekStartDate().plusDays(6))) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("日计划日期必须属于对应自然周");
        }
        // 准备当前记录（row），类型为 DailyPlanEntity；初始内容是调用 mapper.byDate 完成按日期处理。
        DailyPlanEntity row = mapper.byDate(userId, date);
        // 检查条件 row != null 是否成立；成立时执行下面的分支。
        if (row != null) {
            // 检查条件 !Objects.equals(row.getWeeklyPlanId(), weeklyPlanId) 是否成立；成立时执行下面的分支。
            if (!Objects.equals(row.getWeeklyPlanId(), weeklyPlanId)) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("该日期已属于其他有效周计划");
            }
            // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
            return toVO(row);
        }
        // 检查条件 date.isBefore(ctx.today(userId)) 是否成立；成立时执行下面的分支。
        if (date.isBefore(ctx.today(userId))) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("过去的日计划只读，不能新建");
        }
        // 把新建 每日方案（DailyPlanEntity） 实例保存到 row。
        row = new DailyPlanEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 每周方案编号（WeeklyPlanId） 的值。
        row.setWeeklyPlanId(weeklyPlanId);
        // 设置 方案日期（PlanDate） 的值。
        row.setPlanDate(date);
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(userId, row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 每周方案编号（weeklyPlanId），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public PageResultVO<DailyPlanVO> page(long weeklyPlanId, GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:daily:list");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("week", userId, weeklyPlanId, false);
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 设置 每周方案编号（WeeklyPlanId） 的值。
        query.setWeeklyPlanId(weeklyPlanId);
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
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public PageResultVO<PlanningTaskVO> tasks(long id, GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("planning:daily:read");
        // 准备日（day），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var day = ctx.scope("day", userId, id, false);
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 设置 每日方案编号（DailyPlanId） 的值。
        query.setDailyPlanId(id);
        // 设置 每周方案编号（WeeklyPlanId） 的值。
        query.setWeeklyPlanId(day.getWeeklyPlanId());
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 tasks.page 完成当前页处理。
                tasks.page(userId, query, false),
                // 调用 tasks.count 完成数量处理。
                tasks.count(userId, query, false),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    // handling，使用 String 类型保存；可为 null，表示尚未提供。
    public void delete(long id, long version, String handling) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("planning:daily:manage");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("day", userId, id, false);
        // 准备当前记录（row），类型为 DailyPlanEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        DailyPlanEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 要求满足 No运行中（NoRunning）。
        execution.requireNoRunning(userId, ExecutionScopeEnum.DAY, id);
        // 检查条件 row.getPlanDate().isBefore(ctx.today(userId)) 是否成立；成立时执行下面的分支。
        if (row.getPlanDate().isBefore(ctx.today(userId))) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("过去的日计划只读，请通过明确的任务改期处理");
        }
        // 准备q，类型为 GrowthQueryDTO；初始内容是新建 成长业务查询条件（GrowthQueryDTO） 实例。
        GrowthQueryDTO q = new GrowthQueryDTO();
        // 设置 每日方案编号（DailyPlanId） 的值。
        q.setDailyPlanId(id);
        // 设置 当前仅（CurrentOnly） 的值。
        q.setCurrentOnly(true);
        // 设置 长度或数量校验规则（Size） 的值。
        q.setSize(100);
        // 检查assignments.count(userId, q) > 0 && !java.util.Set.of("RETURN_TO_POOL", "KEEP_IN_WEEK").contains(han…，即对应内容是否不存在；成立时执行下面的分支。
        if (assignments.count(userId, q) > 0
                // 继续补充同一个判断或备用结果：条件 && !java.util.Set.of("RETURN_TO_POOL", "KEEP_IN_WEEK") 是否成立。
                && !java.util.Set.of("RETURN_TO_POOL", "KEEP_IN_WEEK")
                        // 继续对上一步结果执行根据前面的条件，从两个候选结果中选择一个：contains(handling == null ? "" : handling)) {。
                        .contains(handling == null ? "" : handling)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("请明确选择退回任务池或保留在本周队列");
        }
        // 逐次执行循环：while (true)；每轮处理一条数据或推进一次检查。
        while (true) {
            // 准备批量（batch），类型为 null；初始内容是调用 assignments.page 完成当前页处理。
            var batch = assignments.page(userId, q);
            // 检查输入或集合是否为空：batch.isEmpty()；成立时执行下面的分支。
            if (batch.isEmpty()) {
                // 继续传入或组合break，完成上面尚未结束的调用或声明。
                break;
            }
            // 任务安排（assignment），使用 null 类型保存。
            // 逐次执行循环：for (/*missing* / assignment : batch)；每轮处理一条数据或推进一次检查。
            for (var assignment : batch) {
                // 准备任务（task），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
                var task = ctx.scope("task", userId, assignment.getTaskId(), true);
                // 检查条件 "KEEP_IN_WEEK".equals(handling) && "COMPLETED".equals(task.getStatus()) 是否成立；成立时执行下面的分支。
                if ("KEEP_IN_WEEK".equals(handling) && "COMPLETED".equals(task.getStatus())) {
                    // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                    throw BusinessException.conflict("已完成任务不能重新安排到周队列");
                }
                // 设置 已结束At（EndedAt） 的值。
                assignment.setEndedAt(Instant.now());
                // 设置 结束原因（EndReason） 的值。
                assignment.setEndReason("DAILY_PLAN_DELETED");
                // 核对数据库实际更新条数，失败时按冲突处理。
                ctx.changed(assignments.update(assignment));
                // 检查条件 "KEEP_IN_WEEK".equals(handling) 是否成立；成立时执行下面的分支。
                if ("KEEP_IN_WEEK".equals(handling)) {
                    // 准备replacement，类型为 TaskAssignmentEntity；初始内容是新建 任务任务安排（TaskAssignmentEntity） 实例。
                    TaskAssignmentEntity replacement = new TaskAssignmentEntity();
                    // 设置 用户编号（UserId） 的值。
                    replacement.setUserId(userId);
                    // 设置 任务编号（TaskId） 的值。
                    replacement.setTaskId(assignment.getTaskId());
                    // 设置 每周方案编号（WeeklyPlanId） 的值。
                    replacement.setWeeklyPlanId(assignment.getWeeklyPlanId());
                    // 设置 排序顺序（SortOrder） 的值。
                    replacement.setSortOrder(assignment.getSortOrder());
                    // 设置 AssignedAt 的值。
                    replacement.setAssignedAt(Instant.now());
                    // 调用 assignments.insert 完成写入新记录处理。
                    assignments.insert(replacement);
                }
            }
        }
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.softDelete(row));
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private DailyPlanEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 DailyPlanEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        DailyPlanEntity row = mapper.findById(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 DailyPlanEntity 类型保存。
    private DailyPlanVO toVO(DailyPlanEntity row) {
        // 把新建 每日方案（DailyPlanVO） 实例交还给调用方，本方法到此结束。
        return new DailyPlanVO(
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getWeeklyPlanId()),
                // 读取 方案日期（PlanDate） 的值。
                row.getPlanDate(),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 读取 更新时At（UpdatedAt） 的值。
                row.getUpdatedAt());
    }
}
