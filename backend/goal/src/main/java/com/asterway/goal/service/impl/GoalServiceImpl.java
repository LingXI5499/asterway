/*
 * 实现目标业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.service.impl;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
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
// 引入目标完成情况提供器（GoalCompletionProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalCompletionProvider;
// 引入目标通知提供器（GoalNotificationProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalNotificationProvider;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入目标归档（GoalArchiveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.dto.GoalArchiveDTO;
// 引入目标保存（GoalSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.dto.GoalSaveDTO;
// 引入目标（GoalEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.entity.GoalEntity;
// 引入目标当前状态历史（GoalStateHistoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.entity.GoalStateHistoryEntity;
// 引入目标数据库读写器（GoalMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.mapper.GoalMapper;
// 引入目标当前状态历史数据库读写器（GoalStateHistoryMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.mapper.GoalStateHistoryMapper;
// 引入目标业务服务（GoalService），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.service.GoalService;
// 引入目标当前状态历史（GoalStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.vo.GoalStateHistoryVO;
// 引入目标（GoalVO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.vo.GoalVO;

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

/* Goal 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 GoalServiceImpl；实现目标业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class GoalServiceImpl implements GoalService, GoalCompletionProvider {
    // 访问数据库的 Mapper（mapper），使用 GoalMapper 类型保存。
    private final GoalMapper mapper;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;
    // 历史记录列表（histories），使用 GoalStateHistoryMapper 类型保存。
    private final GoalStateHistoryMapper histories;
    // 达标提醒处理器（notices），使用 GoalNotificationProvider 类型保存。
    private final GoalNotificationProvider notices;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public GoalServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 GoalMapper 类型保存。
            GoalMapper mapper,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            GrowthContextUtils ctx,
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            ExecutionLifecycleProvider execution,
            // 历史记录列表（histories），使用 GoalStateHistoryMapper 类型保存。
            GoalStateHistoryMapper histories,
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
    public PageResultVO<GoalVO> page(GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("goal:goal:list");
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 检查query.getArchived() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (query.getArchived() == null) {
            // 设置 已归档（Archived） 的值。
            query.setArchived(false);
        }
        // 检查query.getTiming() == null || query.getTiming().isBlank()，即对应内容是否不存在；成立时执行下面的分支。
        if (query.getTiming() == null || query.getTiming().isBlank()) {
            // 默认工作区排除未来目标；明确筛选状态或归档时展示对应的完整结果。
            // 设置 Timing 的值。
            query.setTiming(
                    // 比较两个值是否相同。
                    Boolean.TRUE.equals(query.getArchived())
                                    // 继续补充同一个判断或备用结果：输入或集合是否为空：|| query.getStatus() != null && !query.getStatus().isBlank()。
                                    || query.getStatus() != null && !query.getStatus().isBlank()
                            // 继续补充同一个判断或备用结果：条件 ? "ALL" 是否成立。
                            ? "ALL"
                            // 继续补充同一个判断或备用结果：条件 : "CURRENT"); 是否成立。
                            : "CURRENT");
        }
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
    public GoalVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("goal:goal:read");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, id, false);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    public GoalVO create(GoalSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:create");
        // 准备当前记录（row），类型为 GoalEntity；初始内容是新建 目标（GoalEntity） 实例。
        GoalEntity row = new GoalEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 状态（Status） 的值。
        row.setStatus("NOT_STARTED");
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 调用 appendHistory(row, "CREATED", null, null, "USER_ACTION", 0L, Instant.now 完成当前时间处理。
        appendHistory(row, "CREATED", null, null, "USER_ACTION", 0L, Instant.now());
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(userId, row.getId()), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    public GoalVO update(long id, GoalSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, id, true);
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把新建 目标保存（GoalSaveDTO） 实例保存到 dto。
        dto =
                // 继续传入或组合新建 目标保存（GoalSaveDTO） 实例，完成上面尚未结束的调用或声明。
                new GoalSaveDTO(
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
                        // 根据前面的条件，从两个候选结果中选择一个：dto.startDate() == null ? row.getStartDate() : dto.startDate()。
                        dto.startDate() == null ? row.getStartDate() : dto.startDate(),
                        // 继续传入或组合调用 dto.expectedEndDate 完成预计结束日期处理，完成上面尚未结束的调用或声明。
                        dto.expectedEndDate() == null
                                // 继续补充同一个判断或备用结果：条件 ? row.getExpectedEndDate() 是否成立。
                                ? row.getExpectedEndDate()
                                // 继续补充同一个判断或备用结果：条件 : dto.expectedEndDate(), 是否成立。
                                : dto.expectedEndDate(),
                        // 调用 dto.version 完成版本号处理。
                        dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, id);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    public GoalVO replace(long id, GoalSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, id, true);
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, id);
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
    public GoalVO status(long id, GrowthStatusDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:status");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, id, true);
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 准备变化前（before），类型为 String；初始内容是读取 状态（Status） 的值。
        String before = row.getStatus();
        // 准备下一个（next），类型为 String；初始内容是调用 ctx.state 完成当前状态处理。
        String next = ctx.state(dto.status(), false);
        // 检查条件 next.equals(before) 是否成立；成立时执行下面的分支。
        if (next.equals(before)) {
            // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
            return toVO(row, true);
        }
        // 设置 状态（Status） 的值。
        row.setStatus(next);
        // 检查条件 "COMPLETED".equals(next) 是否成立；成立时执行下面的分支。
        if ("COMPLETED".equals(next)) {
            // 设置 完成情况ConditionMet（CompletionConditionMet） 的值。
            row.setCompletionConditionMet(false);
        }
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 读取 已归档At（ArchivedAt） 的值。
        appendHistory(
                // 继续传入或组合当前记录（row），完成上面尚未结束的调用或声明。
                row,
                // 继续传入或组合固定状态或编码 STATUS_CHANGED（状态已修改），完成上面尚未结束的调用或声明。
                "STATUS_CHANGED",
                // 继续传入或组合变化前（before），完成上面尚未结束的调用或声明。
                before,
                // 继续传入或组合读取 已归档At（ArchivedAt） 的值，完成上面尚未结束的调用或声明。
                row.getArchivedAt() != null,
                // 继续传入或组合固定状态或编码 USER_ACTION（用户操作），完成上面尚未结束的调用或声明。
                "USER_ACTION",
                // 继续传入或组合读取 版本号（Version） 的值，完成上面尚未结束的调用或声明。
                row.getVersion() + 1,
                // 当前真实时间，用于记录操作发生的时刻。
                Instant.now());
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, id);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long id, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("goal:goal:archive");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, id, false);
        // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
        throw new BusinessException(410, "GOAL_DELETE_RETIRED", "目标仅支持归档，请使用归档操作");
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 归档（archive）方法：调用 archive 完成归档处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalArchiveDTO 类型保存。
    public GoalVO archive(long id, GoalArchiveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:archive");
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 检查条件 !Boolean.TRUE.equals(dto.confirmed()) 是否成立；成立时执行下面的分支。
        if (!Boolean.TRUE.equals(dto.confirmed())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("请确认结束目标全部未结束执行并移出计划");
        }
        // 已归档目标也执行联动，可按新规则处理旧 KEEP 策略遗留的安排，历史不删除。
        // 调用 execution.finishGoal 完成finish目标处理。
        execution.finishGoal(userId, id);
        // 调用 ctx.endGoal 完成结束目标处理。
        ctx.endGoal(userId, id);
        // 检查row.getArchivedAt() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row.getArchivedAt() == null) {
            // 设置 已归档At（ArchivedAt） 的值。
            row.setArchivedAt(Instant.now());
            // 设置 完成情况ConditionMet（CompletionConditionMet） 的值。
            row.setCompletionConditionMet(false);
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(mapper.update(row));
            // 读取 状态（Status） 的值。
            appendHistory(
                    // 继续传入或组合当前记录（row），完成上面尚未结束的调用或声明。
                    row,
                    // 继续传入或组合固定状态或编码 ARCHIVED（已归档），完成上面尚未结束的调用或声明。
                    "ARCHIVED",
                    // 读取 状态（Status） 的值。
                    row.getStatus(),
                    // 继续传入或组合假，表示条件不成立或开关关闭，完成上面尚未结束的调用或声明。
                    false,
                    // 继续传入或组合固定状态或编码 USER_ACTION（用户操作），完成上面尚未结束的调用或声明。
                    "USER_ACTION",
                    // 继续传入或组合读取 版本号（Version） 的值，完成上面尚未结束的调用或声明。
                    row.getVersion() + 1,
                    // 读取 已归档At（ArchivedAt） 的值。
                    row.getArchivedAt());
        }
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, id);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 恢复（restore）方法：调用 restore 完成恢复处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public GoalVO restore(long id, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("goal:goal:restore");
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 检查row.getArchivedAt() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row.getArchivedAt() == null) {
            // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
            return toVO(row, true);
        }
        // 设置 已归档At（ArchivedAt） 的值。
        row.setArchivedAt(null);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 读取 状态（Status） 的值。
        appendHistory(
                // 继续传入或组合当前记录（row），完成上面尚未结束的调用或声明。
                row,
                // 继续传入或组合固定状态或编码 RESTORED（RESTORED），完成上面尚未结束的调用或声明。
                "RESTORED",
                // 读取 状态（Status） 的值。
                row.getStatus(),
                // 继续传入或组合真，表示条件成立或开关打开，完成上面尚未结束的调用或声明。
                true,
                // 继续传入或组合固定状态或编码 USER_ACTION（用户操作），完成上面尚未结束的调用或声明。
                "USER_ACTION",
                // 继续传入或组合读取 版本号（Version） 的值，完成上面尚未结束的调用或声明。
                row.getVersion() + 1,
                // 当前真实时间，用于记录操作发生的时刻。
                Instant.now());
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, id);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<GoalStateHistoryVO> history(long id, PageQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("goal:goal:read");
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

    /* 通知主链已经校验精确条件和轮次；目标只负责自己的状态、历史与执行联动。 */
    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 确认来源通知（confirmFromNotification）方法：确认 来源通知（FromNotification）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    // 预计版本号（expectedVersion），使用 long 类型保存。
    public void confirmFromNotification(long userId, long goalId, long expectedVersion) {
        // 调用 ctx.lockLifecycleUser 完成加锁Lifecycle用户处理。
        ctx.lockLifecycleUser(userId);
        // 准备当前记录（row），类型为 GoalEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        GoalEntity row = required(userId, goalId);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), expectedVersion);
        // 检查条件 row.getArchivedAt() != null || "COMPLETED".equals(row.getStatus()) 是否成立；成立时执行下面的分支。
        if (row.getArchivedAt() != null || "COMPLETED".equals(row.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("目标已归档或完成，不能确认旧提醒");
        }
        // 准备变化前（before），类型为 String；初始内容是读取 状态（Status） 的值。
        String before = row.getStatus();
        // 设置 状态（Status） 的值。
        row.setStatus("COMPLETED");
        // 设置 完成情况ConditionMet（CompletionConditionMet） 的值。
        row.setCompletionConditionMet(false);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 调用 appendHistory(row, "STATUS_CHANGED", before, false, "NOTIFICATION", expectedVersion + 1, Instant.now 完成当前时间处理。
        appendHistory(
                // 继续传入或组合当前记录（row），完成上面尚未结束的调用或声明。
                row,
                // 继续传入或组合固定状态或编码 STATUS_CHANGED（状态已修改），完成上面尚未结束的调用或声明。
                "STATUS_CHANGED",
                // 继续传入或组合变化前（before），完成上面尚未结束的调用或声明。
                before,
                // 继续传入或组合假，表示条件不成立或开关关闭，完成上面尚未结束的调用或声明。
                false,
                // 继续传入或组合固定状态或编码 NOTIFICATION（通知），完成上面尚未结束的调用或声明。
                "NOTIFICATION",
                // 继续传入或组合按表达式计算得到的值：expectedVersion + 1，完成上面尚未结束的调用或声明。
                expectedVersion + 1,
                // 当前真实时间，用于记录操作发生的时刻。
                Instant.now());
        // 当前消息由调用方先标记 CONFIRMED，再统一撤回其他待处理消息，避免误使自身失效。
    }

    // 追加历史（appendHistory）方法：追加一条状态变化历史，保留当时的业务事实；参数负责传入本次处理所需的数据。
    private void appendHistory(
            // 当前记录（row），使用 GoalEntity 类型保存。
            GoalEntity row,
            // 事件（event），使用 String 类型保存；可为 null，表示尚未提供。
            String event,
            // 变化前（before），使用 String 类型保存；可为 null，表示尚未提供。
            String before,
            // 变化前已归档（beforeArchived），使用 Boolean 类型保存；可为 null，表示尚未提供。
            Boolean beforeArchived,
            // 来源（source），使用 String 类型保存；可为 null，表示尚未提供。
            String source,
            // 版本号（version），使用 long 类型保存。
            long version,
            // at，使用 Instant 类型保存；可为 null，表示尚未提供。
            Instant at) {
        // 准备历史（history），类型为 null；初始内容是新建 目标当前状态历史（GoalStateHistoryEntity） 实例。
        var history = new GoalStateHistoryEntity();
        // 设置 用户编号（UserId） 的值。
        history.setUserId(row.getUserId());
        // 设置 目标编号（GoalId） 的值。
        history.setGoalId(row.getId());
        // 设置 事件类型（EventType） 的值。
        history.setEventType(event);
        // 设置 变化前状态（BeforeStatus） 的值。
        history.setBeforeStatus(before);
        // 设置 变化后状态（AfterStatus） 的值。
        history.setAfterStatus(row.getStatus());
        // 设置 变化前已归档（BeforeArchived） 的值。
        history.setBeforeArchived(beforeArchived);
        // 设置 变化后已归档（AfterArchived） 的值。
        history.setAfterArchived(row.getArchivedAt() != null);
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
    // 当前记录（row），使用 GoalEntity 类型保存。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    private void fill(GoalEntity row, GoalSaveDTO dto) {
        // 调用 ctx.dates 完成dates处理。
        ctx.dates(dto.startDate(), dto.expectedEndDate());
        // 设置 名称（Name） 的值。
        row.setName(ctx.name(dto.name()));
        // 设置 说明（Description） 的值。
        row.setDescription(ctx.text(dto.description()));
        // 设置 完成情况判断标准（CompletionCriteria） 的值。
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        // 设置 开始日期（StartDate） 的值。
        row.setStartDate(dto.startDate());
        // 设置 预计结束日期（ExpectedEndDate） 的值。
        row.setExpectedEndDate(dto.expectedEndDate());
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private GoalEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 GoalEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        GoalEntity row = mapper.findById(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 GoalEntity 类型保存。
    // 详情（detail），使用 boolean 类型保存。
    private GoalVO toVO(GoalEntity row, boolean detail) {
        // 把新建 目标（GoalVO） 实例交还给调用方，本方法到此结束。
        return new GoalVO(
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getId()),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 说明（Description） 的值。
                row.getDescription(),
                // 读取 完成情况判断标准（CompletionCriteria） 的值。
                row.getCompletionCriteria(),
                // 读取 开始日期（StartDate） 的值。
                row.getStartDate(),
                // 读取 预计结束日期（ExpectedEndDate） 的值。
                row.getExpectedEndDate(),
                // 读取 状态（Status） 的值。
                row.getStatus(),
                // 读取 已归档At（ArchivedAt） 的值。
                row.getArchivedAt(),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 读取 更新时At（UpdatedAt） 的值。
                row.getUpdatedAt(),
                // 调用 mapper.progress 完成进度处理。
                mapper.progress(row.getUserId(), row.getId()),
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：detail ? ctx.stageOverview(row.getUserId(), row.getId()) : java.util.List.of()，完成上面尚未结束的调用或声明。
                detail ? ctx.stageOverview(row.getUserId(), row.getId()) : java.util.List.of(),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getCompletionConditionMet()),
                // 读取 完成情况轮（CompletionRound） 的值。
                row.getCompletionRound());
    }
}
