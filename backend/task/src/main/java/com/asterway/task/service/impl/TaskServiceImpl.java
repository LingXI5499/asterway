/*
 * 实现任务业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.task.service.impl;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入成长业务调整顺序（GrowthReorderDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthReorderDTO;
// 引入成长业务状态（GrowthStatusDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthStatusDTO;
// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入执行计时Lifecycle提供器（ExecutionLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionLifecycleProvider;
// 引入目标通知提供器（GoalNotificationProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalNotificationProvider;
// 引入任务当前状态历史提供器（TaskStateHistoryProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.TaskStateHistoryProvider;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入任务保存（TaskSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.dto.TaskSaveDTO;
// 引入任务（TaskEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.task.entity.TaskEntity;
// 引入任务当前状态历史（TaskStateHistoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.task.entity.TaskStateHistoryEntity;
// 引入任务数据库读写器（TaskMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.task.mapper.TaskMapper;
// 引入任务当前状态历史数据库读写器（TaskStateHistoryMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.task.mapper.TaskStateHistoryMapper;
// 引入任务业务服务（TaskService），下面可以直接使用这个类型或静态成员。
import com.asterway.task.service.TaskService;
// 引入任务当前状态历史（TaskStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.vo.TaskStateHistoryVO;
// 引入任务（TaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.task.vo.TaskVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;

/* Task 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 TaskServiceImpl；实现任务业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class TaskServiceImpl implements TaskService, TaskStateHistoryProvider {
    // 访问数据库的 Mapper（mapper），使用 TaskMapper 类型保存。
    private final TaskMapper mapper;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;
    // 历史记录列表（histories），使用 TaskStateHistoryMapper 类型保存。
    private final TaskStateHistoryMapper histories;
    // 达标提醒处理器（notices），使用 GoalNotificationProvider 类型保存。
    private final GoalNotificationProvider notices;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public TaskServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 TaskMapper 类型保存。
            TaskMapper mapper,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            GrowthContextUtils ctx,
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            ExecutionLifecycleProvider execution,
            // 历史记录列表（histories），使用 TaskStateHistoryMapper 类型保存。
            TaskStateHistoryMapper histories,
            // 达标提醒处理器（notices），使用 GoalNotificationProvider 类型保存。
            GoalNotificationProvider notices) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把当前用户、权限和数据范围检查器（ctx）保存到 this.ctx。
        this.ctx = ctx;
        // 把执行计时（execution）保存到 this.execution。
        this.execution = execution;
        // 把历史记录列表（histories）保存到 this.histories。
        this.histories = histories;
        // 把达标提醒处理器（notices）保存到 this.notices。
        this.notices = notices;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    public PageResultVO<TaskVO> page(GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("task:task:list");
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 当前记录（row），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                mapper.page(userId, query).stream().map(row -> toVO(row, false)).toList(),
                // 统计符合条件的记录数，具体 SQL 对应 count。
                mapper.count(userId, query),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public TaskVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("task:task:read");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, id, false);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 上级编号（parentId），使用 long 类型保存。
    // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
    public TaskVO create(long parentId, TaskSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:create");
        // 准备上级（parent），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var parent = ctx.scope("stage", userId, parentId, true);
        // 准备当前记录（row），类型为 TaskEntity；初始内容是新建 任务（TaskEntity） 实例。
        TaskEntity row = new TaskEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 状态（Status） 的值。
        row.setStatus("NOT_STARTED");
        // 设置 阶段编号（StageId） 的值。
        row.setStageId(parentId);
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 调用 appendHistory(row, "CREATED", null, "USER_ACTION", 0L, Instant.now 完成当前时间处理。
        appendHistory(row, "CREATED", null, "USER_ACTION", 0L, Instant.now());
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, parent.getGoalId());
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(userId, row.getId()), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
    public TaskVO update(long id, TaskSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, id, true);
        // 准备当前记录（row），类型为 TaskEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        TaskEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把新建 任务保存（TaskSaveDTO） 实例保存到 dto。
        dto =
                // 继续传入或组合新建 任务保存（TaskSaveDTO） 实例，完成上面尚未结束的调用或声明。
                new TaskSaveDTO(
                        // 根据前面的条件，从两个候选结果中选择一个：dto.name() == null ? row.getName() : dto.name()。
                        dto.name() == null ? row.getName() : dto.name(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.description() == null ? row.getDescription() : dto.description()。
                        dto.description() == null ? row.getDescription() : dto.description(),
                        // 继续传入或组合调用 dto.completionCriteria 完成完成情况判断标准处理，完成上面尚未结束的调用或声明。
                        dto.completionCriteria() == null
                                // 继续补充同一个判断或备用结果：条件 ? row.getCompletionCriteria() 是否成立。
                                ? row.getCompletionCriteria()
                                // 继续补充同一个判断或备用结果：条件 : dto.completionCriteria(), 是否成立。
                                : dto.completionCriteria(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.priority() == null ? row.getPriority() : dto.priority()。
                        dto.priority() == null ? row.getPriority() : dto.priority(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.weight() == null ? row.getWeight() : dto.weight()。
                        dto.weight() == null ? row.getWeight() : dto.weight(),
                        // 继续传入或组合调用 dto.estimatedMinutes 完成estimated分钟数处理，完成上面尚未结束的调用或声明。
                        dto.estimatedMinutes() == null
                                // 继续补充同一个判断或备用结果：条件 ? row.getEstimatedMinutes() 是否成立。
                                ? row.getEstimatedMinutes()
                                // 继续补充同一个判断或备用结果：条件 : dto.estimatedMinutes(), 是否成立。
                                : dto.estimatedMinutes(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.startDate() == null ? row.getStartDate() : dto.startDate()。
                        dto.startDate() == null ? row.getStartDate() : dto.startDate(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.dueDate() == null ? row.getDueDate() : dto.dueDate()。
                        dto.dueDate() == null ? row.getDueDate() : dto.dueDate(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder()。
                        dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder(),
                        // 调用 dto.version 完成版本号处理。
                        dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
    public TaskVO replace(long id, TaskSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, id, true);
        // 准备当前记录（row），类型为 TaskEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        TaskEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 状态（status）方法：调用 status 完成状态处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthStatusDTO 类型保存。
    public TaskVO status(long id, GrowthStatusDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:status");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("task", userId, id, true);
        // 准备当前记录（row），类型为 TaskEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        TaskEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 准备变化前（before），类型为 String；初始内容是读取 状态（Status） 的值。
        String before = row.getStatus();
        // 准备下一个（next），类型为 String；初始内容是调用 ctx.state 完成当前状态处理。
        String next = ctx.state(dto.status(), true);
        // 检查条件 next.equals(before) 是否成立；成立时执行下面的分支。
        if (next.equals(before)) {
            // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
            return toVO(row, true);
        }
        // 准备已修改At（changedAt），类型为 Instant；初始内容是当前真实时间，用于记录操作发生的时刻。
        Instant changedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // 设置 状态（Status） 的值。
        row.setStatus(next);
        // 检查条件 "COMPLETED".equals(row.getStatus()) 是否成立；成立时执行下面的分支。
        if ("COMPLETED".equals(row.getStatus())) {
            // 要求满足 任务Finished（TaskFinished）。
            execution.requireTaskFinished(userId, id);
            // 检查row.getCompletedAt() == null，即对应内容是否不存在；成立时执行下面的分支。
            if (row.getCompletedAt() == null) {
                // 设置 已完成At（CompletedAt） 的值。
                row.setCompletedAt(changedAt);
            }
        // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
        } else {
            // 设置 已完成At（CompletedAt） 的值。
            row.setCompletedAt(null);
        }
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 读取 版本号（Version） 的值。
        appendHistory(
                // 继续传入或组合读取 版本号（Version） 的值，完成上面尚未结束的调用或声明。
                row, "STATUS_CHANGED", before, "USER_ACTION", row.getVersion() + 1, changedAt);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, ctx.scope("task", userId, id, false).getGoalId());
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long id, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:delete");
        // 准备数据范围（scope），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var scope = ctx.scope("task", userId, id, true);
        // 准备当前记录（row），类型为 TaskEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        TaskEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 调用 execution.finishTask 完成finish任务处理。
        execution.finishTask(userId, id);
        // 调用 ctx.endTask 完成结束任务处理。
        ctx.endTask(userId, id);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.softDelete(row));
        // 读取 状态（Status） 的值。
        appendHistory(
                // 继续传入或组合当前记录（row），完成上面尚未结束的调用或声明。
                row,
                // 继续传入或组合固定状态或编码 DELETED（删除标记），完成上面尚未结束的调用或声明。
                "DELETED",
                // 读取 状态（Status） 的值。
                row.getStatus(),
                // 继续传入或组合固定状态或编码 USER_ACTION（用户操作），完成上面尚未结束的调用或声明。
                "USER_ACTION",
                // 继续传入或组合读取 版本号（Version） 的值，完成上面尚未结束的调用或声明。
                row.getVersion() + 1,
                // 当前真实时间，用于记录操作发生的时刻。
                Instant.now());
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, scope.getGoalId());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 调整顺序（reorder）方法：调用 reorder 完成调整顺序处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 上级编号（parentId），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthReorderDTO 类型保存。
    public void reorder(long parentId, GrowthReorderDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("task:task:sort");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, parentId, true);
        // 检查dto.items() == null || dto.items().isEmpty() || dto.items().size() > 100，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.items() == null || dto.items().isEmpty() || dto.items().size() > 100) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("排序数量须为 1～100");
        }
        // 准备编号列表（ids），类型为 java.util.Set<Long>；初始内容是新建 Java后端中的util中的摘要值设置（java.util.HashSet） 实例。
        java.util.Set<Long> ids = new java.util.HashSet<>();
        // 单条数据（item），使用 null 类型保存。
        // 逐次执行循环：for (/*missing* / item : dto.items())；每轮处理一条数据或推进一次检查。
        for (var item : dto.items()) {
            // 检查条件 !ids.add(item.id()) 是否成立；成立时执行下面的分支。
            if (!ids.add(item.id())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("排序标识不能重复");
            }
            // 准备当前记录（row），类型为 TaskEntity；初始内容是调用 required(userId, item.id 完成编号处理。
            TaskEntity row = required(userId, item.id());
            // 检查条件 !java.util.Objects.equals(row.getStageId(), parentId) 是否成立；成立时执行下面的分支。
            if (!java.util.Objects.equals(row.getStageId(), parentId)) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("只能调整同一父资源的顺序");
            }
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(row.getVersion(), item.version());
            // 设置 排序顺序（SortOrder） 的值。
            row.setSortOrder(ctx.order(item.sortOrder()));
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(mapper.update(row));
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<TaskStateHistoryVO> history(long id, PageQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("task:task:read");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 检查histories.historicalSubject(userId, id) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (histories.historicalSubject(userId, id) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 histories.page 完成当前页处理。
                histories.page(userId, id, offset, query.getSize()),
                // 调用 histories.count 完成数量处理。
                histories.count(userId, id),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 记录执行计时开始（recordExecutionStart）方法：调用 recordExecutionStart 完成记录执行计时开始处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void recordExecutionStart(
            // 用户编号（userId），使用 long 类型保存。
            // 任务编号（taskId），使用 long 类型保存。
            // 变化前（before），使用 String 类型保存；可为 null，表示尚未提供。
            // 版本号（version），使用 long 类型保存。
            // at，使用 Instant 类型保存；可为 null，表示尚未提供。
            long userId, long taskId, String before, long version, Instant at) {
        // 调用 ctx.lockLifecycleUser 完成加锁Lifecycle用户处理。
        ctx.lockLifecycleUser(userId);
        // 检查条件 "IN_PROGRESS".equals(before) 是否成立；成立时执行下面的分支。
        if ("IN_PROGRESS".equals(before)) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, taskId);
        // 检查当前记录版本是否符合请求要求：!"IN_PROGRESS".equals(row.getStatus()) || row.getVersion() != version；成立时执行下面的分支。
        if (!"IN_PROGRESS".equals(row.getStatus()) || row.getVersion() != version) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("执行开始与任务状态事实不一致");
        }
        // 追加一条状态变化历史，保留当时的业务事实。
        appendHistory(row, "STATUS_CHANGED", before, "EXECUTION_START", version, at);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, ctx.scope("task", userId, taskId, false).getGoalId());
    }

    // 追加历史（appendHistory）方法：追加一条状态变化历史，保留当时的业务事实；参数负责传入本次处理所需的数据。
    private void appendHistory(
            // 当前记录（row），使用 TaskEntity 类型保存。
            // 事件（event），使用 String 类型保存；可为 null，表示尚未提供。
            // 变化前（before），使用 String 类型保存；可为 null，表示尚未提供。
            // 来源（source），使用 String 类型保存；可为 null，表示尚未提供。
            // 版本号（version），使用 long 类型保存。
            // at，使用 Instant 类型保存；可为 null，表示尚未提供。
            TaskEntity row, String event, String before, String source, long version, Instant at) {
        // 准备历史（history），类型为 null；初始内容是新建 任务当前状态历史（TaskStateHistoryEntity） 实例。
        var history = new TaskStateHistoryEntity();
        // 设置 用户编号（UserId） 的值。
        history.setUserId(row.getUserId());
        // 设置 任务编号（TaskId） 的值。
        history.setTaskId(row.getId());
        // 设置 事件类型（EventType） 的值。
        history.setEventType(event);
        // 设置 变化前状态（BeforeStatus） 的值。
        history.setBeforeStatus(before);
        // 设置 变化后状态（AfterStatus） 的值。
        history.setAfterStatus(row.getStatus());
        // 设置 名称确认时的数据快照（NameSnapshot） 的值。
        history.setNameSnapshot(row.getName());
        // 设置 版本号（EntityVersion） 的值。
        history.setEntityVersion(version);
        // 设置 OccurredAt 的值。
        history.setOccurredAt(at.truncatedTo(ChronoUnit.MILLIS));
        // 设置 来源类型（SourceType） 的值。
        history.setSourceType(source);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(histories.insert(history));
    }

    // 填充（fill）方法：把已校验的请求字段填入待保存的记录；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 TaskEntity 类型保存。
    // 请求参数对象（dto），使用 TaskSaveDTO 类型保存。
    private void fill(TaskEntity row, TaskSaveDTO dto) {
        // 调用 ctx.dates 完成dates处理。
        ctx.dates(dto.startDate(), dto.dueDate());
        // 设置 名称（Name） 的值。
        row.setName(ctx.name(dto.name()));
        // 设置 说明（Description） 的值。
        row.setDescription(ctx.text(dto.description()));
        // 设置 完成情况判断标准（CompletionCriteria） 的值。
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        // 设置 开始日期（StartDate） 的值。
        row.setStartDate(dto.startDate());
        // 设置 应完成日期（DueDate） 的值。
        row.setDueDate(dto.dueDate());
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(ctx.order(dto.sortOrder()));
        // 准备priority，类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：dto.priority() == null ? "MEDIUM" : dto.priority()。
        String priority = dto.priority() == null ? "MEDIUM" : dto.priority();
        // 检查条件 !java.util.Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority) 是否成立；成立时执行下面的分支。
        if (!java.util.Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("优先级无效");
        }
        // 准备权重（weight），类型为 int；初始内容是根据前面的条件，从两个候选结果中选择一个：dto.weight() == null ? 1 : dto.weight()。
        int weight = dto.weight() == null ? 1 : dto.weight();
        // 检查条件 weight <= 0 || dto.estimatedMinutes() != null && dto.estimatedMinutes() <= 0 是否成立；成立时执行下面的分支。
        if (weight <= 0 || dto.estimatedMinutes() != null && dto.estimatedMinutes() <= 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("权重及预计耗时必须为正数");
        }
        // 设置 Priority 的值。
        row.setPriority(priority);
        // 设置 权重（Weight） 的值。
        row.setWeight(weight);
        // 设置 Estimated分钟数（EstimatedMinutes） 的值。
        row.setEstimatedMinutes(dto.estimatedMinutes());
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private TaskEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 TaskEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        TaskEntity row = mapper.findById(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 TaskEntity 类型保存。
    // 详情（detail），使用 boolean 类型保存。
    private TaskVO toVO(TaskEntity row, boolean detail) {
        // 准备数据范围（scope），类型为 null；初始内容是核对记录归属；需要修改时同时锁定记录。
        var scope = ctx.scope("task", row.getUserId(), row.getId(), false);
        // 把新建 任务（TaskVO） 实例交还给调用方，本方法到此结束。
        return new TaskVO(
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getStageId()),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 说明（Description） 的值。
                row.getDescription(),
                // 读取 完成情况判断标准（CompletionCriteria） 的值。
                row.getCompletionCriteria(),
                // 读取 Priority 的值。
                row.getPriority(),
                // 读取 权重（Weight） 的值。
                row.getWeight(),
                // 读取 Estimated分钟数（EstimatedMinutes） 的值。
                row.getEstimatedMinutes(),
                // 读取 开始日期（StartDate） 的值。
                row.getStartDate(),
                // 读取 应完成日期（DueDate） 的值。
                row.getDueDate(),
                // 读取 状态（Status） 的值。
                row.getStatus(),
                // 读取 排序顺序（SortOrder） 的值。
                row.getSortOrder(),
                // 读取 已完成At（CompletedAt） 的值。
                row.getCompletedAt(),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 读取 更新时At（UpdatedAt） 的值。
                row.getUpdatedAt(),
                // 调用 ctx.id 完成编号处理。
                ctx.id(scope.getGoalId()),
                // 读取 目标名称（GoalName） 的值。
                scope.getGoalName(),
                // 读取 阶段名称（StageName） 的值。
                scope.getStageName(),
                // 读取 任务安排状态（AssignmentStatus） 的值。
                scope.getAssignmentStatus(),
                // 调用 ctx.id 完成编号处理。
                ctx.id(scope.getWeeklyPlanId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(scope.getDailyPlanId()),
                // 读取 已归档At（ArchivedAt） 的值。
                scope.getArchivedAt());
    }
}
