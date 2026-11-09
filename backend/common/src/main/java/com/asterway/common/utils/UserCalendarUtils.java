/*
 * 集中处理用户CalendarUtils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长业务Support数据库读写器（GrowthSupportMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.common.mapper.GrowthSupportMapper;
// 引入习惯Lifecycle提供器（HabitLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.HabitLifecycleProvider;
// 引入用户Calendar数据（UserCalendarData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.UserCalendarData;

// 引入对象提供器（ObjectProvider），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.ObjectProvider;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;
// 引入事务Synchronization管理员（TransactionSynchronizationManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;

/* 按账号延后生效时区生成业务日历；不使用服务器 CURRENT_DATE 推断用户日期。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 UserCalendarUtils；集中处理用户CalendarUtils的重复规则，避免各业务模块分别实现。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class UserCalendarUtils {
    // 访问数据库的 Mapper（mapper），使用 GrowthSupportMapper 类型保存。
    private final GrowthSupportMapper mapper;
    // 习惯列表（habits），使用 ObjectProvider<HabitLifecycleProvider> 类型保存。
    private final ObjectProvider<HabitLifecycleProvider> habits;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public UserCalendarUtils(
            // 访问数据库的 Mapper（mapper），使用 GrowthSupportMapper 类型保存。
            // 习惯列表（habits），使用 ObjectProvider<HabitLifecycleProvider> 类型保存。
            GrowthSupportMapper mapper, ObjectProvider<HabitLifecycleProvider> habits) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把习惯列表（habits）保存到 this.habits。
        this.habits = habits;
    }

    // 读取（read）方法：调用 read 完成读取处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    public UserCalendarData read(long userId) {
        // 把调用 context 完成上下文处理交还给调用方，本方法到此结束。
        return context(userId, false);
    }

    /* 调用方已持有用户锁，待生效时区尚未被账户接口落库时也先封存历史。 */
    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    public UserCalendarData write(long userId) {
        // 检查条件 !TransactionSynchronizationManager.isActualTransactionActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("日历写操作必须在用户锁事务内");
        }
        // 把调用 context 完成上下文处理交还给调用方，本方法到此结束。
        return context(userId, true);
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // writing，使用 boolean 类型保存。
    private UserCalendarData context(long userId, boolean writing) {
        // 准备账号（account），类型为 null；初始内容是调用 mapper.calendarAccount 完成calendar账号处理。
        var account = mapper.calendarAccount(userId);
        // 检查account == null || !account.enabled()，即对应内容是否不存在；成立时执行下面的分支。
        if (account == null || !account.enabled()) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 准备当前时间（now），类型为 Instant；初始内容是当前真实时间，用于记录操作发生的时刻。
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // 检查条件 writing && account.pendingTimeZone() != null && !account.timeZoneEffectiveAt().isAfter(now) 是否成立；成立时执行下面的分支。
        if (writing
                // 继续补充同一个判断或备用结果：条件 && account.pendingTimeZone() != null 是否成立。
                && account.pendingTimeZone() != null
                // 继续补充同一个判断或备用结果：条件 && !account.timeZoneEffectiveAt().isAfter(now)) { 是否成立。
                && !account.timeZoneEffectiveAt().isAfter(now)) {
            // 读取 对象（Object） 的值。
            habits.getObject()
                    // 继续对上一步结果执行调用 sealBeforeZoneSwitch 完成seal变化前时区Switch处理。
                    .sealBeforeZoneSwitch(
                            // 继续传入或组合调用 userId, account.timeZoneEffectiveAt 完成时间时区生效At处理，完成上面尚未结束的调用或声明。
                            userId, account.timeZoneEffectiveAt(), account.timeZone());
        }
        // 把新建 用户Calendar数据（UserCalendarData） 实例交还给调用方，本方法到此结束。
        return new UserCalendarData(userId, account.effectiveZone(now), now);
    }
}
