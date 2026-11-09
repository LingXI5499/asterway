/*
 * 实现通知消息业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长业务Support数据库读写器（GrowthSupportMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.common.mapper.GrowthSupportMapper;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入目标完成情况提供器（GoalCompletionProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalCompletionProvider;
// 引入目标通知提供器（GoalNotificationProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalNotificationProvider;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入通知Handle（NotificationHandleDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.NotificationHandleDTO;
// 引入通知查询条件（NotificationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.NotificationQueryDTO;
// 引入通知消息（NotificationMessageEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.entity.NotificationMessageEntity;
// 引入通知消息数据库读写器（NotificationMessageMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.mapper.NotificationMessageMapper;
// 引入通知消息业务服务（NotificationMessageService），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.service.NotificationMessageService;
// 引入通知消息（NotificationMessageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.NotificationMessageVO;
// 引入通知Unread（NotificationUnreadVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.NotificationUnreadVO;

// 引入对象提供器（ObjectProvider），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.ObjectProvider;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Isolation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Isolation;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 目标达标、轮次和消息一致提交；消息已读与目标完成分别处理。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 NotificationMessageServiceImpl；实现通知消息业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 把下面的修改放进事务，成功一起提交，异常按配置回滚。
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class NotificationMessageServiceImpl
        // 继续传入或组合表达式 implements NotificationMessageService, GoalNotificationProvider { 的结果，完成上面尚未结束的调用或声明。
        implements NotificationMessageService, GoalNotificationProvider {
    // 访问数据库的 Mapper（mapper），使用 NotificationMessageMapper 类型保存。
    private final NotificationMessageMapper mapper;
    // support，使用 GrowthSupportMapper 类型保存。
    private final GrowthSupportMapper support;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // 目标列表（goals），使用 ObjectProvider<GoalCompletionProvider> 类型保存。
    private final ObjectProvider<GoalCompletionProvider> goals;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public NotificationMessageServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 NotificationMessageMapper 类型保存。
            NotificationMessageMapper mapper,
            // support，使用 GrowthSupportMapper 类型保存。
            GrowthSupportMapper support,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            GrowthContextUtils ctx,
            // 目标列表（goals），使用 ObjectProvider<GoalCompletionProvider> 类型保存。
            ObjectProvider<GoalCompletionProvider> goals) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把support保存到 this.support。
        this.support = support;
        // 把当前用户、权限和数据范围检查器（ctx）保存到 this.ctx。
        this.ctx = ctx;
        // 把目标列表（goals）保存到 this.goals。
        this.goals = goals;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 NotificationQueryDTO 类型保存。
    public PageResultVO<NotificationMessageVO> page(NotificationQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("notification:message:list");
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 检查条件 query.getHandleStatus() != null && !Set.of("PENDING", "CONFIRMED", "CONTINUED", "INVALIDATED").contains(query.getHandleStatus()) 是否成立；成立时执行下面的分支。
        if (query.getHandleStatus() != null
                // 继续补充同一个判断或备用结果：条件 && !Set.of("PENDING", "CONFIRMED", "CONTINUED", "INVALIDATED") 是否成立。
                && !Set.of("PENDING", "CONFIRMED", "CONTINUED", "INVALIDATED")
                        // 继续对上一步结果执行读取 Handle状态（HandleStatus） 的值。
                        .contains(query.getHandleStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("消息处理状态无效");
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 按分页条件读取记录，具体 SQL 对应 page。
                mapper.page(userId, query),
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
    public NotificationMessageVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("notification:message:read");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(userId, id);
        // 把调用 mapper.view 完成页面处理交还给调用方，本方法到此结束。
        return mapper.view(userId, id);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // unread方法：调用 unread 完成unread处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public NotificationUnreadVO unread() {
        // 把新建 通知Unread（NotificationUnreadVO） 实例交还给调用方，本方法到此结束。
        return new NotificationUnreadVO(mapper.unread(ctx.read("notification:message:list")));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // mark读取（markRead）方法：调用 markRead 完成mark读取处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public NotificationMessageVO markRead(long id, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("notification:message:mark_read");
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id);
        // 检查row.getReadAt() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row.getReadAt() == null) {
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(row.getVersion(), version);
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(mapper.markRead(userId, id, version, now()));
        }
        // 把调用 mapper.view 完成页面处理交还给调用方，本方法到此结束。
        return mapper.view(userId, id);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // mark全部读取（markAllRead）方法：调用 markAllRead 完成mark全部读取处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public NotificationUnreadVO markAllRead() {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("notification:message:mark_read");
        // 调用 mapper.markAllRead 完成mark全部读取处理。
        mapper.markAllRead(userId, now());
        // 把新建 通知Unread（NotificationUnreadVO） 实例交还给调用方，本方法到此结束。
        return new NotificationUnreadVO(mapper.unread(userId));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // handle方法：调用 handle 完成handle处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 确认（confirm），使用 boolean 类型保存。
    // 请求参数对象（dto），使用 NotificationHandleDTO 类型保存。
    public NotificationMessageVO handle(long id, boolean confirm, NotificationHandleDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("notification:message:handle");
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id);
        // 准备目标值（target），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：confirm ? "CONFIRMED" : "CONTINUED"。
        String target = confirm ? "CONFIRMED" : "CONTINUED";
        // 检查条件 target.equals(row.getHandleStatus()) 是否成立；成立时执行下面的分支。
        if (target.equals(row.getHandleStatus())) {
            // 把调用 mapper.view 完成页面处理交还给调用方，本方法到此结束。
            return mapper.view(userId, id);
        }
        // 检查条件 !"PENDING".equals(row.getHandleStatus()) 是否成立；成立时执行下面的分支。
        if (!"PENDING".equals(row.getHandleStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("该提醒已经处理或失效");
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 准备目标（goal），类型为 null；初始内容是调用 mapper.condition 完成condition处理。
        var goal = mapper.condition(userId, row.getGoalId());
        // 检查goal == null || !Boolean.TRUE.equals(goal.getQualifies()) || !Boolean.TRUE.equals(goal.getCompletion…，即对应内容是否不存在；成立时执行下面的分支。
        if (goal == null
                // 继续补充同一个判断或备用结果：条件 || !Boolean.TRUE.equals(goal.getQualifies()) 是否成立。
                || !Boolean.TRUE.equals(goal.getQualifies())
                // 继续补充同一个判断或备用结果：条件 || !Boolean.TRUE.equals(goal.getCompletionConditionMet()) 是否成立。
                || !Boolean.TRUE.equals(goal.getCompletionConditionMet())
                // 继续补充同一个判断或备用结果：条件 || !Objects.equals(goal.getCompletionRound(), row.getConditionRound())) { 是否成立。
                || !Objects.equals(goal.getCompletionRound(), row.getConditionRound())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("目标或达标条件已变化，不能处理旧提醒");
        }
        // 检查条件 confirm 是否成立；成立时执行下面的分支。
        if (confirm) {
            // 检查当前记录版本是否符合请求要求：dto.goalVersion() == null；成立时执行下面的分支。
            if (dto.goalVersion() == null) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("确认完成必须提供目标版本");
            }
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(goal.getVersion(), dto.goalVersion());
            // 延迟获取目标主链，避免通知与目标实现构造循环；状态历史由目标主链追加。
            // 读取 对象（Object） 的值。
            goals.getObject().confirmFromNotification(userId, row.getGoalId(), dto.goalVersion());
        // 检查当前记录版本是否符合请求要求：dto.goalVersion() != null；成立时执行下面的分支。
        } else if (dto.goalVersion() != null) {
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(goal.getVersion(), dto.goalVersion());
        }
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.handle(userId, id, row.getVersion(), target, now()));
        // 读取 目标编号（GoalId） 的值。
        refreshGoal(userId, row.getGoalId());
        // 把调用 mapper.view 完成页面处理交还给调用方，本方法到此结束。
        return mapper.view(userId, id);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 刷新目标（refreshGoal）方法：按目标最新状态刷新达标提醒；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    public void refreshGoal(long userId, long goalId) {
        // 调用 lock 完成加锁处理。
        lock(userId);
        // 准备目标（goal），类型为 null；初始内容是调用 mapper.condition 完成condition处理。
        var goal = mapper.condition(userId, goalId);
        // 检查goal == null，即对应内容是否不存在；成立时执行下面的分支。
        if (goal == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 准备met，类型为 boolean；初始内容是比较两个值是否相同。
        boolean met = Boolean.TRUE.equals(goal.getQualifies());
        // 准备轮（round），类型为 long；初始内容是读取 完成情况轮（CompletionRound） 的值。
        long round = goal.getCompletionRound();
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 检查条件 met != Boolean.TRUE.equals(goal.getCompletionConditionMet()) 是否成立；成立时执行下面的分支。
        if (met != Boolean.TRUE.equals(goal.getCompletionConditionMet())) {
            // 检查条件 met 是否成立；成立时执行下面的分支。
            if (met) {
                // 把调用 Math.addExact 完成加入Exact处理保存到 round。
                round = Math.addExact(round, 1);
            }
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(mapper.updateCondition(userId, goalId, goal.getVersion(), met, round, now));
        }
        // 调用 mapper.invalidateGoal 完成invalidate目标处理。
        mapper.invalidateGoal(userId, goalId, met ? round : 0, now);
        // 检查条件 met 是否成立；成立时执行下面的分支。
        if (met) {
            // 准备达标提醒（notice），类型为 null；初始内容是新建 通知消息（NotificationMessageEntity） 实例。
            var notice = new NotificationMessageEntity();
            // 设置 用户编号（UserId） 的值。
            notice.setUserId(userId);
            // 设置 目标编号（GoalId） 的值。
            notice.setGoalId(goalId);
            // 设置 Condition轮（ConditionRound） 的值。
            notice.setConditionRound(round);
            // 设置 标题（Title） 的值。
            notice.setTitle("目标已达标，请确认完成");
            // 设置 内容（Content） 的值。
            notice.setContent("目标「" + goal.getName() + "」的有效任务已全部完成，请确认目标完成或选择继续保持当前状态。");
            // 设置 创建时At（CreatedAt） 的值。
            notice.setCreatedAt(now);
            // 设置 更新时At（UpdatedAt） 的值。
            notice.setUpdatedAt(now);
            // 唯一索引同时覆盖已处理和已失效消息，同轮永远不重发。
            // 向数据库插入新记录，具体 SQL 对应 insert。
            mapper.insert(notice);
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // suspend通知列表（suspendNotifications）方法：调用 suspendNotifications 完成suspend通知列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    public void suspendNotifications(long userId) {
        // 调用 lock 完成加锁处理。
        lock(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是调用 now 完成当前时间处理。
        Instant now = now();
        // 调用 mapper.clearUserConditions 完成清除用户Conditions处理。
        mapper.clearUserConditions(userId, now);
        // 调用 mapper.invalidateUser 完成invalidate用户处理。
        mapper.invalidateUser(userId, now);
    }

    // 加锁（lock）方法：调用 lock 完成加锁处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private void lock(long userId) {
        // 检查support.lockAnyUser(userId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (support.lockAnyUser(userId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private NotificationMessageEntity required(long userId, long id) {
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

    // 当前时间（now）方法：调用 now 完成当前时间处理；参数负责传入本次处理所需的数据。
    private Instant now() {
        // 把当前真实时间，用于记录操作发生的时刻交还给调用方，本方法到此结束。
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
