/*
 * 实现阶段业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.stage.service.impl;

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
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入阶段保存（StageSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.dto.StageSaveDTO;
// 引入阶段（StageEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.entity.StageEntity;
// 引入阶段当前状态历史（StageStateHistoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.entity.StageStateHistoryEntity;
// 引入阶段数据库读写器（StageMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.mapper.StageMapper;
// 引入阶段当前状态历史数据库读写器（StageStateHistoryMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.mapper.StageStateHistoryMapper;
// 引入阶段业务服务（StageService），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.service.StageService;
// 引入阶段当前状态历史（StageStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.vo.StageStateHistoryVO;
// 引入阶段（StageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.stage.vo.StageVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;

/* Stage 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 StageServiceImpl；实现阶段业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class StageServiceImpl implements StageService {
    // 访问数据库的 Mapper（mapper），使用 StageMapper 类型保存。
    private final StageMapper mapper;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;
    // 历史记录列表（histories），使用 StageStateHistoryMapper 类型保存。
    private final StageStateHistoryMapper histories;
    // 达标提醒处理器（notices），使用 GoalNotificationProvider 类型保存。
    private final GoalNotificationProvider notices;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public StageServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 StageMapper 类型保存。
            StageMapper mapper,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            GrowthContextUtils ctx,
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            ExecutionLifecycleProvider execution,
            // 历史记录列表（histories），使用 StageStateHistoryMapper 类型保存。
            StageStateHistoryMapper histories,
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
    public PageResultVO<StageVO> page(GrowthQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("stage:stage:list");
        // 补齐查询所属用户、分页和筛选条件。
        ctx.prepare(query, userId);
        // 检查query.getGoalId() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (query.getGoalId() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("必须指定目标");
        }
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, query.getGoalId(), false);
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
    public StageVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("stage:stage:read");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, id, false);
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 上级编号（parentId），使用 long 类型保存。
    // 请求参数对象（dto），使用 StageSaveDTO 类型保存。
    public StageVO create(long parentId, StageSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("stage:stage:create");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, parentId, true);
        // 准备当前记录（row），类型为 StageEntity；初始内容是新建 阶段（StageEntity） 实例。
        StageEntity row = new StageEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 状态（Status） 的值。
        row.setStatus("NOT_STARTED");
        // 设置 目标编号（GoalId） 的值。
        row.setGoalId(parentId);
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 追加一条状态变化历史，保留当时的业务事实。
        appendHistory(row, "CREATED", null, 0L);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, parentId);
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(userId, row.getId()), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 StageSaveDTO 类型保存。
    public StageVO update(long id, StageSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("stage:stage:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, id, true);
        // 准备当前记录（row），类型为 StageEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        StageEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把新建 阶段保存（StageSaveDTO） 实例保存到 dto。
        dto =
                // 继续传入或组合新建 阶段保存（StageSaveDTO） 实例，完成上面尚未结束的调用或声明。
                new StageSaveDTO(
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
                        // 根据前面的条件，从两个候选结果中选择一个：dto.endDate() == null ? row.getEndDate() : dto.endDate()。
                        dto.endDate() == null ? row.getEndDate() : dto.endDate(),
                        // 根据前面的条件，从两个候选结果中选择一个：dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder()。
                        dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder(),
                        // 调用 dto.version 完成版本号处理。
                        dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(userId, id), true);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 StageSaveDTO 类型保存。
    public StageVO replace(long id, StageSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("stage:stage:update");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, id, true);
        // 准备当前记录（row），类型为 StageEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        StageEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
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
    public StageVO status(long id, GrowthStatusDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("stage:stage:status");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, id, true);
        // 准备当前记录（row），类型为 StageEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        StageEntity row = required(userId, id);
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
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 重新核对正在运行的计时是否仍满足任务和计划条件。
        execution.validateRunningEligibility(userId);
        // 读取 版本号（Version） 的值。
        appendHistory(row, "STATUS_CHANGED", before, row.getVersion() + 1);
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
        long userId = ctx.write("stage:stage:delete");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("stage", userId, id, true);
        // 准备当前记录（row），类型为 StageEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        StageEntity row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 检查条件 ctx.tasks(userId, id) > 0 是否成立；成立时执行下面的分支。
        if (ctx.tasks(userId, id) > 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("请先处理阶段下的有效任务");
        }
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.softDelete(row));
        // 读取 状态（Status） 的值。
        appendHistory(row, "DELETED", row.getStatus(), row.getVersion() + 1);
        // 按目标最新状态刷新达标提醒。
        notices.refreshGoal(userId, row.getGoalId());
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
        long userId = ctx.write("stage:stage:sort");
        // 核对记录归属；需要修改时同时锁定记录。
        ctx.scope("goal", userId, parentId, true);
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
            // 准备当前记录（row），类型为 StageEntity；初始内容是调用 required(userId, item.id 完成编号处理。
            StageEntity row = required(userId, item.id());
            // 检查条件 !java.util.Objects.equals(row.getGoalId(), parentId) 是否成立；成立时执行下面的分支。
            if (!java.util.Objects.equals(row.getGoalId(), parentId)) {
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
    public PageResultVO<StageStateHistoryVO> history(long id, PageQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("stage:stage:read");
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

    // 追加历史（appendHistory）方法：追加一条状态变化历史，保留当时的业务事实；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 StageEntity 类型保存。
    // 事件（event），使用 String 类型保存；可为 null，表示尚未提供。
    // 变化前（before），使用 String 类型保存；可为 null，表示尚未提供。
    // 版本号（version），使用 long 类型保存。
    private void appendHistory(StageEntity row, String event, String before, long version) {
        // 准备历史（history），类型为 null；初始内容是新建 阶段当前状态历史（StageStateHistoryEntity） 实例。
        var history = new StageStateHistoryEntity();
        // 设置 用户编号（UserId） 的值。
        history.setUserId(row.getUserId());
        // 设置 阶段编号（StageId） 的值。
        history.setStageId(row.getId());
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
        history.setOccurredAt(java.time.Instant.now().truncatedTo(ChronoUnit.MILLIS));
        // 设置 来源类型（SourceType） 的值。
        history.setSourceType("USER_ACTION");
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(histories.insert(history));
    }

    // 填充（fill）方法：把已校验的请求字段填入待保存的记录；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 StageEntity 类型保存。
    // 请求参数对象（dto），使用 StageSaveDTO 类型保存。
    private void fill(StageEntity row, StageSaveDTO dto) {
        // 调用 ctx.dates 完成dates处理。
        ctx.dates(dto.startDate(), dto.endDate());
        // 设置 名称（Name） 的值。
        row.setName(ctx.name(dto.name()));
        // 设置 说明（Description） 的值。
        row.setDescription(ctx.text(dto.description()));
        // 设置 完成情况判断标准（CompletionCriteria） 的值。
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        // 设置 开始日期（StartDate） 的值。
        row.setStartDate(dto.startDate());
        // 设置 结束日期（EndDate） 的值。
        row.setEndDate(dto.endDate());
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(ctx.order(dto.sortOrder()));
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private StageEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 StageEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        StageEntity row = mapper.findById(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 StageEntity 类型保存。
    // 详情（detail），使用 boolean 类型保存。
    private StageVO toVO(StageEntity row, boolean detail) {
        // 把新建 阶段（StageVO） 实例交还给调用方，本方法到此结束。
        return new StageVO(
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getId()),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getGoalId()),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 说明（Description） 的值。
                row.getDescription(),
                // 读取 完成情况判断标准（CompletionCriteria） 的值。
                row.getCompletionCriteria(),
                // 读取 开始日期（StartDate） 的值。
                row.getStartDate(),
                // 读取 结束日期（EndDate） 的值。
                row.getEndDate(),
                // 读取 排序顺序（SortOrder） 的值。
                row.getSortOrder(),
                // 读取 状态（Status） 的值。
                row.getStatus(),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 读取 更新时At（UpdatedAt） 的值。
                row.getUpdatedAt(),
                // 调用 mapper.progress 完成进度处理。
                mapper.progress(row.getUserId(), row.getId()),
                // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：detail ? ctx.taskOverview(row.getUserId(), row.getId()) : java.util.List.of())，完成上面尚未结束的调用或声明。
                detail ? ctx.taskOverview(row.getUserId(), row.getId()) : java.util.List.of());
    }
}
