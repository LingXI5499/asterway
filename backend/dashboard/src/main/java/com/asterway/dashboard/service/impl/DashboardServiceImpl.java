/*
 * 实现Dashboard业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.service.impl;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入用户CalendarUtils（UserCalendarUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.UserCalendarUtils;
// 引入Dashboard读取（DashboardReadDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.dto.DashboardReadDTO;
// 引入Dashboard任务查询条件（DashboardTaskQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
// 引入Dashboard数据库读写器（DashboardMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.mapper.DashboardMapper;
// 引入Dashboard业务服务（DashboardService），下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.service.DashboardService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.dashboard.vo.*;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Isolation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Isolation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

/* 同一只读快照下组合今日事实，各业务数据仍由原模块持有。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 DashboardServiceImpl；实现Dashboard业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 把下面的修改放进事务，成功一起提交，异常按配置回滚。
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class DashboardServiceImpl implements DashboardService {
    // 访问数据库的 Mapper（mapper），使用 DashboardMapper 类型保存。
    private final DashboardMapper mapper;
    // 访问权限（access），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils access;
    // calendar，使用 UserCalendarUtils 类型保存。
    private final UserCalendarUtils calendar;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public DashboardServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 DashboardMapper 类型保存。
            // 访问权限（access），使用 GrowthContextUtils 类型保存。
            // calendar，使用 UserCalendarUtils 类型保存。
            DashboardMapper mapper, GrowthContextUtils access, UserCalendarUtils calendar) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把访问权限（access）保存到 this.access。
        this.access = access;
        // 把calendar保存到 this.calendar。
        this.calendar = calendar;
    }

    // 上下文（context）方法：调用 context 完成上下文处理；参数负责传入本次处理所需的数据。
    private DashboardReadDTO context() {
        // 准备用户编号（userId），类型为 long；初始内容是调用 access.read 完成读取处理。
        long userId = access.read("dashboard:board:read");
        // 准备日（day），类型为 null；初始内容是调用 calendar.read 完成读取处理。
        var day = calendar.read(userId);
        // 把新建 Dashboard读取（DashboardReadDTO） 实例交还给调用方，本方法到此结束。
        return new DashboardReadDTO(
                // 继续传入或组合用户编号（userId），完成上面尚未结束的调用或声明。
                userId,
                // 调用 day.today 完成今天处理。
                day.today(),
                // 调用 day.zone 完成时区处理。
                day.zone().getId(),
                // 调用 day.now 完成当前时间处理。
                day.now(),
                // 调用 SecurityUtils.current 完成当前处理。
                SecurityUtils.current().sessionId());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // overview方法：调用 overview 完成overview处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public DashboardOverviewVO overview() {
        // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
        var ctx = context();
        // 把新建 DashboardOverview（DashboardOverviewVO） 实例交还给调用方，本方法到此结束。
        return new DashboardOverviewVO(
                // 调用 ctx.today 完成今天处理。
                ctx.today(), ctx.timeZone(), ctx.now(), mapper.overview(ctx), mapper.current(ctx));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 DashboardTaskQueryDTO 类型保存。
    public PageResultVO<DashboardTaskVO> tasks(DashboardTaskQueryDTO query) {
        // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
        var ctx = context();
        // 准备section，类型为 String；初始内容是读取 Section 的值。
        String section = query.getSection();
        // 检查条件 section != null && !"TODAY".equals(section) && !"WEEK_UNDATED".equals(section) 是否成立；成立时执行下面的分支。
        if (section != null && !"TODAY".equals(section) && !"WEEK_UNDATED".equals(section)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("任务分组必须为 TODAY 或 WEEK_UNDATED");
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 mapper.tasks 完成任务列表处理。
                mapper.tasks(ctx, section, query.offset(), query.getSize()),
                // 调用 mapper.tasksCount 完成任务列表数量处理。
                mapper.tasksCount(ctx, section),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 目标列表（goals）方法：调用 goals 完成目标列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<DashboardGoalVO> goals(PageQueryDTO query) {
        // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
        var ctx = context();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 mapper.goals 完成目标列表处理。
                mapper.goals(ctx, query.offset(), query.getSize()),
                // 调用 mapper.goalsCount 完成目标列表数量处理。
                mapper.goalsCount(ctx),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 习惯列表（habits）方法：调用 habits 完成习惯列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<DashboardHabitVO> habits(PageQueryDTO query) {
        // 准备当前用户、权限和数据范围检查器（ctx），类型为 null；初始内容是调用 context 完成上下文处理。
        var ctx = context();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 mapper.habits 完成习惯列表处理。
                mapper.habits(ctx, query.offset(), query.getSize()),
                // 调用 mapper.habitsCount 完成习惯列表数量处理。
                mapper.habitsCount(ctx),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }
}
