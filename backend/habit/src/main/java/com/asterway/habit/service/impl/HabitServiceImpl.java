/*
 * 实现习惯业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.service.impl;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长业务Support数据库读写器（GrowthSupportMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.common.mapper.GrowthSupportMapper;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入习惯Lifecycle提供器（HabitLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.HabitLifecycleProvider;
// 引入用户Calendar数据（UserCalendarData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.UserCalendarData;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入用户CalendarUtils（UserCalendarUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.UserCalendarUtils;
// 引入习惯打卡查询条件（HabitCheckinQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitCheckinQueryDTO;
// 引入习惯查询条件（HabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitQueryDTO;
// 引入习惯保存（HabitSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitSaveDTO;
// 引入习惯打卡（HabitCheckinEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitCheckinEntity;
// 引入习惯（HabitEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitEntity;
// 引入习惯修订版本（HabitRevisionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitRevisionEntity;
// 引入习惯分类数据库读写器（HabitCategoryMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.mapper.HabitCategoryMapper;
// 引入习惯打卡数据库读写器（HabitCheckinMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.mapper.HabitCheckinMapper;
// 引入习惯数据库读写器（HabitMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.mapper.HabitMapper;
// 引入习惯修订版本数据库读写器（HabitRevisionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.mapper.HabitRevisionMapper;
// 引入习惯业务服务（HabitService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitService;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.*;

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
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;
// 引入ChronoUnit，下面可以直接使用这个类型或静态成员。
import java.time.temporal.ChronoUnit;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入Int分段回复流（IntStream），下面可以直接使用这个类型或静态成员。
import java.util.stream.IntStream;

/* 每个日期最多一次有效打卡；修订追加，不覆盖过去规则和完成事实。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 HabitServiceImpl；实现习惯业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 把下面的修改放进事务，成功一起提交，异常按配置回滚。
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class HabitServiceImpl implements HabitService, HabitLifecycleProvider {
    // 访问数据库的 Mapper（mapper），使用 HabitMapper 类型保存。
    private final HabitMapper mapper;
    // 修订记录（revisions），使用 HabitRevisionMapper 类型保存。
    private final HabitRevisionMapper revisions;
    // 打卡记录（checkins），使用 HabitCheckinMapper 类型保存。
    private final HabitCheckinMapper checkins;
    // 分类列表（categories），使用 HabitCategoryMapper 类型保存。
    private final HabitCategoryMapper categories;
    // support，使用 GrowthSupportMapper 类型保存。
    private final GrowthSupportMapper support;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;
    // calendar，使用 UserCalendarUtils 类型保存。
    private final UserCalendarUtils calendar;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public HabitServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 HabitMapper 类型保存。
            HabitMapper mapper,
            // 修订记录（revisions），使用 HabitRevisionMapper 类型保存。
            HabitRevisionMapper revisions,
            // 打卡记录（checkins），使用 HabitCheckinMapper 类型保存。
            HabitCheckinMapper checkins,
            // 分类列表（categories），使用 HabitCategoryMapper 类型保存。
            HabitCategoryMapper categories,
            // support，使用 GrowthSupportMapper 类型保存。
            GrowthSupportMapper support,
            // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
            GrowthContextUtils ctx,
            // calendar，使用 UserCalendarUtils 类型保存。
            UserCalendarUtils calendar) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把修订记录（revisions）保存到 this.revisions。
        this.revisions = revisions;
        // 把打卡记录（checkins）保存到 this.checkins。
        this.checkins = checkins;
        // 把分类列表（categories）保存到 this.categories。
        this.categories = categories;
        // 把support保存到 this.support。
        this.support = support;
        // 把当前用户、权限和数据范围检查器（ctx）保存到 this.ctx。
        this.ctx = ctx;
        // 把calendar保存到 this.calendar。
        this.calendar = calendar;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    public PageResultVO<HabitVO> page(HabitQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:habit:list");
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 检查条件 query.getKeyword() != null && query.getKeyword().length() > 100 是否成立；成立时执行下面的分支。
        if (query.getKeyword() != null && query.getKeyword().length() > 100) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("关键字过长");
        }
        // 检查条件 query.getCategoryId() != null && query.getCategoryId() <= 0 || query.getStatus() != null && !Set.of("ACTIVE", "PAUSED", "ENDED").contains(query.g… 是否成立；成立时执行下面的分支。
        if (query.getCategoryId() != null && query.getCategoryId() <= 0
                // 继续补充同一个判断或备用结果：条件 || query.getStatus() != null 是否成立。
                || query.getStatus() != null
                        // 继续补充同一个判断或备用结果：条件 && !Set.of("ACTIVE", "PAUSED", "ENDED").contains(query.getStatus())) { 是否成立。
                        && !Set.of("ACTIVE", "PAUSED", "ENDED").contains(query.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("分类或状态筛选无效");
        }
        // 准备日（day），类型为 null；初始内容是调用 calendar.read 完成读取处理。
        var day = calendar.read(userId);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 按分页条件读取记录，具体 SQL 对应 page。
                mapper.page(userId, query, day.today()),
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
    public HabitVO get(long id) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:habit:read");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(userId, id, false);
        // 把调用 view(userId, id, calendar.read 完成读取处理交还给调用方，本方法到此结束。
        return view(userId, id, calendar.read(userId));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    public HabitVO create(HabitSaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:habit:create");
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备rule，类型为 null；初始内容是新建 习惯修订版本（HabitRevisionEntity） 实例。
        var rule = new HabitRevisionEntity();
        // 把已校验的请求字段填入待保存的记录。
        fill(rule, dto, day);
        // 读取 分类编号（CategoryId） 的值。
        validateCategory(userId, rule.getCategoryId(), null);
        // 准备当前记录（row），类型为 null；初始内容是新建 习惯（HabitEntity） 实例。
        var row = new HabitEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 状态（Status） 的值。
        row.setStatus("ACTIVE");
        // 设置 当前修订版本No（CurrentRevisionNo） 的值。
        row.setCurrentRevisionNo(1L);
        // 设置 版本号（Version） 的值。
        row.setVersion(0L);
        // 设置 创建时At（CreatedAt） 的值。
        row.setCreatedAt(day.now());
        // 设置 更新时At（UpdatedAt） 的值。
        row.setUpdatedAt(day.now());
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.insert(row));
        // 调用 snapshot(row, rule, "CREATED", day, day.today 完成今天处理。
        snapshot(row, rule, "CREATED", day, day.today());
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(revisions.insert(rule));
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return view(userId, row.getId(), day);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    // 整体替换（replace），使用 boolean 类型保存。
    public HabitVO update(long id, HabitSaveDTO dto, boolean replace) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:habit:update");
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id, false);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 检查条件 "ENDED".equals(row.getStatus()) 是否成立；成立时执行下面的分支。
        if ("ENDED".equals(row.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("习惯已结束，请先重新开启");
        }
        // 准备上一次（previous），类型为 null；初始内容是调用 current 完成当前处理。
        var previous = current(row);
        // 准备值列表（values），类型为 HabitSaveDTO；初始内容是根据前面的条件，从两个候选结果中选择一个：replace ? dto : new HabitSaveDTO(dto.name() == null ? previous.getName() : dto.name(), dto.descripti…。
        HabitSaveDTO values =
                // 继续传入或组合整体替换（replace），完成上面尚未结束的调用或声明。
                replace
                        // 继续补充同一个判断或备用结果：条件 ? dto 是否成立。
                        ? dto
                        // 继续补充同一个判断或备用结果：条件 : new HabitSaveDTO( 是否成立。
                        : new HabitSaveDTO(
                                // 根据前面的条件，从两个候选结果中选择一个：dto.name() == null ? previous.getName() : dto.name()。
                                dto.name() == null ? previous.getName() : dto.name(),
                                // 继续传入或组合调用 dto.description 完成说明处理，完成上面尚未结束的调用或声明。
                                dto.description() == null
                                        // 继续补充同一个判断或备用结果：条件 ? previous.getDescription() 是否成立。
                                        ? previous.getDescription()
                                        // 继续补充同一个判断或备用结果：条件 : dto.description(), 是否成立。
                                        : dto.description(),
                                // 继续传入或组合调用 dto.completionCriteria 完成完成情况判断标准处理，完成上面尚未结束的调用或声明。
                                dto.completionCriteria() == null
                                        // 继续补充同一个判断或备用结果：条件 ? previous.getCompletionCriteria() 是否成立。
                                        ? previous.getCompletionCriteria()
                                        // 继续补充同一个判断或备用结果：条件 : dto.completionCriteria(), 是否成立。
                                        : dto.completionCriteria(),
                                // 继续传入或组合调用 dto.categoryId 完成分类编号处理，完成上面尚未结束的调用或声明。
                                dto.categoryId() == null
                                        // 继续补充同一个判断或备用结果：条件 ? previous.getCategoryId() 是否成立。
                                        ? previous.getCategoryId()
                                        // 继续补充同一个判断或备用结果：条件 : dto.categoryId(), 是否成立。
                                        : dto.categoryId(),
                                // 根据前面的条件，从两个候选结果中选择一个：dto.cycleType() == null ? previous.getCycleType() : dto.cycleType()。
                                dto.cycleType() == null ? previous.getCycleType() : dto.cycleType(),
                                // 继续传入或组合调用 dto.weekdays 完成weekdays处理，完成上面尚未结束的调用或声明。
                                dto.weekdays() == null
                                        // 继续补充同一个判断或备用结果：条件 ? ("DAILY".equals(dto.cycleType()) 是否成立。
                                        ? ("DAILY".equals(dto.cycleType())
                                                // 继续补充同一个判断或备用结果：条件 ? null 是否成立。
                                                ? null
                                                // 继续补充同一个判断或备用结果：条件 : weekdays(previous.getWeekdayMask())) 是否成立。
                                                : weekdays(previous.getWeekdayMask()))
                                        // 继续补充同一个判断或备用结果：条件 : dto.weekdays(), 是否成立。
                                        : dto.weekdays(),
                                // 根据前面的条件，从两个候选结果中选择一个：dto.startDate() == null ? previous.getStartDate() : dto.startDate()。
                                dto.startDate() == null ? previous.getStartDate() : dto.startDate(),
                                // 继续传入或组合调用 dto.expectedTime 完成预计时间处理，完成上面尚未结束的调用或声明。
                                dto.expectedTime() == null
                                        // 继续补充同一个判断或备用结果：条件 ? previous.getExpectedTime() 是否成立。
                                        ? previous.getExpectedTime()
                                        // 继续补充同一个判断或备用结果：条件 : dto.expectedTime(), 是否成立。
                                        : dto.expectedTime(),
                                // 调用 dto.version 完成版本号处理。
                                dto.version());
        // 准备下一个（next），类型为 null；初始内容是新建 习惯修订版本（HabitRevisionEntity） 实例。
        var next = new HabitRevisionEntity();
        // 把已校验的请求字段填入待保存的记录。
        fill(next, values, day);
        // 读取 分类编号（CategoryId） 的值。
        validateCategory(userId, next.getCategoryId(), previous.getCategoryId());
        // 检查条件 sameRule(previous, next) 是否成立；成立时执行下面的分支。
        if (sameRule(previous, next)) {
            // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
            return view(userId, id, day);
        }
        // 调用 editable 完成editable处理。
        editable(row, previous, day);
        // 调用 append(row, next, "EDITED", day, day.today 完成今天处理。
        append(row, next, "EDITED", day, day.today());
        // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
        return view(userId, id, day);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public HabitVO pause(long id, long version) {
        // 把调用 transition(id, version, "habit:habit:pause", "PAUSED", "PAUSED", Set.of 完成of处理交还给调用方，本方法到此结束。
        return transition(id, version, "habit:habit:pause", "PAUSED", "PAUSED", Set.of("ACTIVE"));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public HabitVO resume(long id, long version) {
        // 把调用 transition(id, version, "habit:habit:resume", "ACTIVE", "RESUMED", Set.of 完成of处理交还给调用方，本方法到此结束。
        return transition(id, version, "habit:habit:resume", "ACTIVE", "RESUMED", Set.of("PAUSED"));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 结束（end）方法：调用 end 完成结束处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public HabitVO end(long id, long version) {
        // 把调用 transition(id, version, "habit:habit:end", "ENDED", "ENDED", Set.of 完成of处理交还给调用方，本方法到此结束。
        return transition(
                // 继续传入或组合调用 id, version, "habit:habit:end", "ENDED", "ENDED", Set.of 完成of处理，完成上面尚未结束的调用或声明。
                id, version, "habit:habit:end", "ENDED", "ENDED", Set.of("ACTIVE", "PAUSED"));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // reopen方法：调用 reopen 完成reopen处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public HabitVO reopen(long id, long version) {
        // 把调用 transition(id, version, "habit:habit:reopen", "ACTIVE", "REOPENED", Set.of 完成of处理交还给调用方，本方法到此结束。
        return transition(id, version, "habit:habit:reopen", "ACTIVE", "REOPENED", Set.of("ENDED"));
    }

    // transition方法：调用 transition 完成transition处理；参数负责传入本次处理所需的数据。
    private HabitVO transition(
            // 编号（id），使用 long 类型保存。
            long id,
            // 版本号（version），使用 long 类型保存。
            long version,
            // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
            String permission,
            // 目标值（target），使用 String 类型保存；可为 null，表示尚未提供。
            String target,
            // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
            String action,
            // allowed，使用 Set<String> 类型保存。
            Set<String> allowed) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write(permission);
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id, false);
        // 检查条件 target.equals(row.getStatus()) 是否成立；成立时执行下面的分支。
        if (target.equals(row.getStatus())) {
            // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
            return view(userId, id, day);
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 检查条件 !allowed.contains(row.getStatus()) 是否成立；成立时执行下面的分支。
        if (!allowed.contains(row.getStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("当前状态不允许该操作，请选择恢复或重新开启");
        }
        // 准备rule，类型为 null；初始内容是调用 current 完成当前处理。
        var rule = current(row);
        // 调用 editable 完成editable处理。
        editable(row, rule, day);
        // 设置 状态（Status） 的值。
        row.setStatus(target);
        // 设置 已结束At（EndedAt） 的值。
        row.setEndedAt("ENDED".equals(target) ? day.now() : null);
        // 调用 append(row, copy(rule), action, day, day.today 完成今天处理。
        append(row, copy(rule), action, day, day.today());
        // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
        return view(userId, id, day);
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
        long userId = ctx.write("habit:habit:delete");
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id, true);
        // 检查条件 row.getDeletedAt() != null 是否成立；成立时执行下面的分支。
        if (row.getDeletedAt() != null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 准备rule，类型为 null；初始内容是调用 current 完成当前处理。
        var rule = current(row);
        // 调用 editable 完成editable处理。
        editable(row, rule, day);
        // 设置 删除标记At（DeletedAt） 的值。
        row.setDeletedAt(day.now());
        // 调用 append(row, copy(rule), "DELETED", day, day.today 完成今天处理。
        append(row, copy(rule), "DELETED", day, day.today());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 今天（today）方法：调用 today 完成今天处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<HabitTodayVO> today(PageQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:habit:list");
        // 准备日（day），类型为 null；初始内容是调用 calendar.read 完成读取处理。
        var day = calendar.read(userId);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 mapper.today 完成今天处理。
                mapper.today(userId, day.today(), query.offset(), query.getSize()),
                // 调用 mapper.todayCount 完成今天数量处理。
                mapper.todayCount(userId, day.today()),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 打卡（checkin）方法：核对 in；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 习惯版本号（habitVersion），使用 long 类型保存。
    public HabitCheckinVO checkin(long id, long habitVersion) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:checkin:create");
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id, false);
        // 准备existing，类型为 null；初始内容是调用 checkins.valid 完成有效处理。
        var existing = checkins.valid(userId, id, day.today());
        // 检查条件 existing != null 是否成立；成立时执行下面的分支。
        if (existing != null) {
            // 把调用 checkins.view 完成页面处理交还给调用方，本方法到此结束。
            return checkins.view(userId, existing.getId());
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), habitVersion);
        // 准备rule，类型为 null；初始内容是调用 current 完成当前处理。
        var rule = current(row);
        // 调用 editable 完成editable处理。
        editable(row, rule, day);
        // 检查条件 !"ACTIVE".equals(row.getStatus()) || !"ACTIVE".equals(rule.getStatusSnapshot()) || Boolean.TRUE.equals(rule.getDeletedSnapshot()) || rule.getStar… 是否成立；成立时执行下面的分支。
        if (!"ACTIVE".equals(row.getStatus())
                // 继续补充同一个判断或备用结果：条件 || !"ACTIVE".equals(rule.getStatusSnapshot()) 是否成立。
                || !"ACTIVE".equals(rule.getStatusSnapshot())
                // 继续补充同一个判断或备用结果：条件 || Boolean.TRUE.equals(rule.getDeletedSnapshot()) 是否成立。
                || Boolean.TRUE.equals(rule.getDeletedSnapshot())
                // 继续补充同一个判断或备用结果：条件 || rule.getStartDate().isAfter(day.today()) 是否成立。
                || rule.getStartDate().isAfter(day.today())
                // 继续补充同一个判断或备用结果：条件 || (rule.getWeekdayMask() & (1 << (day.today().getDayOfWeek().getValue() - 1))) 是否成立。
                || (rule.getWeekdayMask() & (1 << (day.today().getDayOfWeek().getValue() - 1)))
                        // 继续传入或组合条件判断的结果：== 0) {，完成上面尚未结束的调用或声明。
                        == 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("今天不是该习惯可打卡的日期");
        }
        // 准备打卡（checkin），类型为 null；初始内容是新建 习惯打卡（HabitCheckinEntity） 实例。
        var checkin = new HabitCheckinEntity();
        // 设置 用户编号（UserId） 的值。
        checkin.setUserId(userId);
        // 设置 习惯编号（HabitId） 的值。
        checkin.setHabitId(id);
        // 设置 修订版本编号（RevisionId） 的值。
        checkin.setRevisionId(rule.getId());
        // 设置 Business日期（BusinessDate） 的值。
        checkin.setBusinessDate(day.today());
        // 设置 时间时区（TimeZone） 的值。
        checkin.setTimeZone(day.zone().getId());
        // 设置 已核对At（CheckedAt） 的值。
        checkin.setCheckedAt(day.now());
        // 设置 创建时At（CreatedAt） 的值。
        checkin.setCreatedAt(day.now());
        // 设置 更新时At（UpdatedAt） 的值。
        checkin.setUpdatedAt(day.now());
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(checkins.insert(checkin));
        // 把调用 checkins.view 完成页面处理交还给调用方，本方法到此结束。
        return checkins.view(userId, checkin.getId());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 打卡编号（checkinId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public HabitCheckinVO cancel(long id, long checkinId, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:checkin:cancel");
        // 准备日（day），类型为 null；初始内容是把内容写入当前输出或响应。
        var day = calendar.write(userId);
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id, false);
        // 准备打卡（checkin），类型为 null；初始内容是取得第一条符合条件的数据。
        var checkin = checkins.find(userId, checkinId);
        // 检查checkin == null || checkin.getHabitId() != id，即对应内容是否不存在；成立时执行下面的分支。
        if (checkin == null || checkin.getHabitId() != id) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 调用 editable 完成editable处理。
        editable(row, current(row), day);
        // 检查条件 !day.today().equals(checkin.getBusinessDate()) 是否成立；成立时执行下面的分支。
        if (!day.today().equals(checkin.getBusinessDate())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("只能撤销今天的打卡，不允许修改历史");
        }
        // 检查checkin.getCancelledAt() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (checkin.getCancelledAt() == null) {
            // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
            ctx.version(checkin.getVersion(), version);
            // 设置 已取消At（CancelledAt） 的值。
            checkin.setCancelledAt(day.now());
            // 设置 更新时At（UpdatedAt） 的值。
            checkin.setUpdatedAt(day.now());
            // 核对数据库实际更新条数，失败时按冲突处理。
            ctx.changed(checkins.cancel(checkin));
        }
        // 把调用 checkins.view 完成页面处理交还给调用方，本方法到此结束。
        return checkins.view(userId, checkinId);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 打卡记录（checkins）方法：核对 ins；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 HabitCheckinQueryDTO 类型保存。
    public PageResultVO<HabitCheckinVO> checkins(long id, HabitCheckinQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:checkin:read");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(userId, id, true);
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 调用 ctx.dates 完成dates处理。
        ctx.dates(query.getDateFrom(), query.getDateTo());
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 调用 checkins.page 完成当前页处理。
                checkins.page(userId, id, query),
                // 调用 checkins.count 完成数量处理。
                checkins.count(userId, id, query),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 修订记录（revisions）方法：调用 revisions 完成修订记录处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<HabitRevisionVO> revisions(long id, PageQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:habit:history");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(userId, id, true);
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                revisions.page(userId, id, query.offset(), query.getSize()).stream()
                        // 继续对上一步结果执行把每个条目转换成需要的形式。
                        .map(this::revisionView)
                        // 继续对上一步结果执行调用 toList 完成to列表处理。
                        .toList(),
                // 调用 revisions.count 完成数量处理。
                revisions.count(userId, id),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    /* 封存只推进边界，不生成每日占位行，也不改写已有规则或打卡。 */
    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // seal变化前时区Switch（sealBeforeZoneSwitch）方法：调用 sealBeforeZoneSwitch 完成seal变化前时区Switch处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    // 生效At（effectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
    // 上一次时区（previousZone），使用 String 类型保存；可为 null，表示尚未提供。
    public void sealBeforeZoneSwitch(long userId, Instant effectiveAt, String previousZone) {
        // 检查effectiveAt == null，即对应内容是否不存在；成立时执行下面的分支。
        if (effectiveAt == null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 调用 lockAny 完成加锁Any处理。
        lockAny(userId);
        // 准备through，类型为 LocalDate；初始内容是调用 effectiveAt.minusMillis 完成minusMillis处理。
        LocalDate through =
                // 调用 effectiveAt.minusMillis 完成minusMillis处理。
                effectiveAt.minusMillis(1).atZone(ZoneId.of(previousZone)).toLocalDate();
        // 调用 mapper.sealHistory 完成seal历史处理。
        mapper.sealHistory(userId, through, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // suspend个人（suspendPersonal）方法：调用 suspendPersonal 完成suspend个人处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 用户编号（userId），使用 long 类型保存。
    public void suspendPersonal(long userId) {
        // 调用 lockAny 完成加锁Any处理。
        lockAny(userId);
        // 准备账号（account），类型为 null；初始内容是调用 support.calendarAccount 完成calendar账号处理。
        var account = support.calendarAccount(userId);
        // 准备当前时间（now），类型为 Instant；初始内容是当前真实时间，用于记录操作发生的时刻。
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        // 检查条件 account.pendingTimeZone() != null && !account.timeZoneEffectiveAt().isAfter(now) 是否成立；成立时执行下面的分支。
        if (account.pendingTimeZone() != null && !account.timeZoneEffectiveAt().isAfter(now)) {
            // 调用 sealBeforeZoneSwitch(userId, account.timeZoneEffectiveAt 完成时间时区生效At处理。
            sealBeforeZoneSwitch(userId, account.timeZoneEffectiveAt(), account.timeZone());
        }
        // 准备日（day），类型为 null；初始内容是新建 用户Calendar数据（UserCalendarData） 实例。
        var day = new UserCalendarData(userId, account.effectiveZone(now), now);
        // 准备变化后（after），类型为 long；初始内容是固定数值 0。
        long after = 0;
        // 逐次执行循环：while (true)；每轮处理一条数据或推进一次检查。
        while (true) {
            // 准备批量（batch），类型为 null；初始内容是调用 mapper.activeBatch 完成当前有效批量处理。
            var batch = mapper.activeBatch(userId, after);
            // 检查输入或集合是否为空：batch.isEmpty()；成立时执行下面的分支。
            if (batch.isEmpty()) {
                // 把返回（return）交还给调用方，本方法到此结束。
                return;
            }
            // 当前记录（row），使用 null 类型保存。
            // 逐次执行循环：for (/*missing* / row : batch)；每轮处理一条数据或推进一次检查。
            for (var row : batch) {
                // 准备rule，类型为 null；初始内容是调用 current 完成当前处理。
                var rule = current(row);
                // 准备生效（effective），类型为 LocalDate；初始内容是调用 day.today 完成今天处理。
                LocalDate effective = day.today();
                // 时区回拨时，系统暂停不能重写已封存日期；主体立即暂停，规则从首个未封存日生效。
                // 检查条件 row.getHistoryLockedThrough() != null && !effective.isAfter(row.getHistoryLockedThrough()) 是否成立；成立时执行下面的分支。
                if (row.getHistoryLockedThrough() != null
                        // 继续补充同一个判断或备用结果：条件 && !effective.isAfter(row.getHistoryLockedThrough())) { 是否成立。
                        && !effective.isAfter(row.getHistoryLockedThrough())) {
                    // 把读取 历史LockedThrough（HistoryLockedThrough） 的值保存到 effective。
                    effective = row.getHistoryLockedThrough().plusDays(1);
                }
                // 检查条件 effective.isBefore(rule.getEffectiveDate()) 是否成立；成立时执行下面的分支。
                if (effective.isBefore(rule.getEffectiveDate())) {
                    // 把读取 生效日期（EffectiveDate） 的值保存到 effective。
                    effective = rule.getEffectiveDate();
                }
                // 设置 状态（Status） 的值。
                row.setStatus("PAUSED");
                // 把新条目放到列表末尾。
                append(row, copy(rule), "SYSTEM_PAUSED", day, effective);
                // 把读取 编号（Id） 的值保存到 after。
                after = row.getId();
            }
        }
    }

    // 加锁Any（lockAny）方法：调用 lockAny 完成加锁Any处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    private void lockAny(long userId) {
        // 检查support.lockAnyUser(userId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (support.lockAnyUser(userId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    // include删除标记（includeDeleted），使用 boolean 类型保存。
    private HabitEntity required(long userId, long id, boolean includeDeleted) {
        // 准备当前记录（row），类型为 null；初始内容是取得第一条符合条件的数据。
        var row = mapper.find(userId, id);
        // 检查row == null || !includeDeleted && row.getDeletedAt() != null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null || !includeDeleted && row.getDeletedAt() != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 当前（current）方法：调用 current 完成当前处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitEntity 类型保存。
    private HabitRevisionEntity current(HabitEntity row) {
        // 准备rule，类型为 null；初始内容是调用 revisions.current 完成当前处理。
        var rule = revisions.current(row.getUserId(), row.getId(), row.getCurrentRevisionNo());
        // 检查rule == null，即对应内容是否不存在；成立时执行下面的分支。
        if (rule == null) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("习惯缺少当前完整修订");
        }
        // 把rule交还给调用方，本方法到此结束。
        return rule;
    }

    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    // 日（day），使用 UserCalendarData 类型保存。
    private HabitVO view(long userId, long id, UserCalendarData day) {
        // 准备值（value），类型为 null；初始内容是调用 mapper.view 完成页面处理。
        var value = mapper.view(userId, id, day.today());
        // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把值（value）交还给调用方，本方法到此结束。
        return value;
    }

    // editable方法：调用 editable 完成editable处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitEntity 类型保存。
    // rule，使用 HabitRevisionEntity 类型保存。
    // 日（day），使用 UserCalendarData 类型保存。
    private void editable(HabitEntity row, HabitRevisionEntity rule, UserCalendarData day) {
        // 检查条件 row.getHistoryLockedThrough() != null && !day.today().isAfter(row.getHistoryLockedThrough()) || rule.getEffectiveDate().isAfter(day.today()) 是否成立；成立时执行下面的分支。
        if (row.getHistoryLockedThrough() != null
                        // 继续补充同一个判断或备用结果：条件 && !day.today().isAfter(row.getHistoryLockedThrough()) 是否成立。
                        && !day.today().isAfter(row.getHistoryLockedThrough())
                // 继续补充同一个判断或备用结果：条件 || rule.getEffectiveDate().isAfter(day.today())) { 是否成立。
                || rule.getEffectiveDate().isAfter(day.today())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("该本地日期已封存，请在新的业务日期操作");
        }
    }

    // 填充（fill）方法：把已校验的请求字段填入待保存的记录；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitRevisionEntity 类型保存。
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    // 日（day），使用 UserCalendarData 类型保存。
    private void fill(HabitRevisionEntity row, HabitSaveDTO dto, UserCalendarData day) {
        // 设置 名称（Name） 的值。
        row.setName(ctx.name(dto.name()));
        // 设置 说明（Description） 的值。
        row.setDescription(ctx.text(dto.description()));
        // 设置 完成情况判断标准（CompletionCriteria） 的值。
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        // 检查条件 row.getDescription().length() > 10000 || row.getCompletionCriteria().length() > 10000 是否成立；成立时执行下面的分支。
        if (row.getDescription().length() > 10000 || row.getCompletionCriteria().length() > 10000) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("描述或完成标准过长");
        }
        // 设置 分类编号（CategoryId） 的值。
        row.setCategoryId(dto.categoryId());
        // 准备cycle，类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：dto.cycleType() == null ? "DAILY" : dto.cycleType()。
        String cycle = dto.cycleType() == null ? "DAILY" : dto.cycleType();
        // 检查条件 !Set.of("DAILY", "WEEKLY_DAYS").contains(cycle) 是否成立；成立时执行下面的分支。
        if (!Set.of("DAILY", "WEEKLY_DAYS").contains(cycle)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("周期仅支持每天或指定星期");
        }
        // 准备mask，类型为 int；初始内容是固定数值 0。
        int mask = 0;
        // 检查条件 dto.weekdays() != null 是否成立；成立时执行下面的分支。
        if (dto.weekdays() != null) {
            // 日Of周（dayOfWeek），使用 Integer 类型保存；可为 null，表示尚未提供。
            // 逐次执行循环：for (Integer dayOfWeek : dto.weekdays())；每轮处理一条数据或推进一次检查。
            for (Integer dayOfWeek : dto.weekdays()) {
                // 检查dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7 || (mask & (1 << (dayOfWeek - 1))) != 0，即对应内容是否不存在；成立时执行下面的分支。
                if (dayOfWeek == null
                        // 继续补充同一个判断或备用结果：条件 || dayOfWeek < 1 是否成立。
                        || dayOfWeek < 1
                        // 继续补充同一个判断或备用结果：条件 || dayOfWeek > 7 是否成立。
                        || dayOfWeek > 7
                        // 继续补充同一个判断或备用结果：条件 || (mask & (1 << (dayOfWeek - 1))) != 0) { 是否成立。
                        || (mask & (1 << (dayOfWeek - 1))) != 0) {
                    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                    throw BusinessException.badRequest("星期须为不重复的 1～7");
                }
                // 完成本次状态更新或调用：mask |= 1 << (dayOfWeek - 1);。
                mask |= 1 << (dayOfWeek - 1);
            }
        }
        // 检查条件 "DAILY".equals(cycle) 是否成立；成立时执行下面的分支。
        if ("DAILY".equals(cycle)) {
            // 检查条件 dto.weekdays() != null && mask != 127 是否成立；成立时执行下面的分支。
            if (dto.weekdays() != null && mask != 127) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("每天执行不能只选择部分星期");
            }
            // 把固定数值 127保存到 mask。
            mask = 127;
        // 检查条件 mask == 0 是否成立；成立时执行下面的分支。
        } else if (mask == 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("请至少选择一个执行星期");
        }
        // 设置 Cycle类型（CycleType） 的值。
        row.setCycleType(cycle);
        // 设置 WeekdayMask 的值。
        row.setWeekdayMask(mask);
        // 设置 开始日期（StartDate） 的值。
        row.setStartDate(dto.startDate() == null ? day.today() : dto.startDate());
        // 检查条件 dto.expectedTime() != null && (dto.expectedTime().getSecond() != 0 || dto.expectedTime().getNano() != 0) 是否成立；成立时执行下面的分支。
        if (dto.expectedTime() != null
                // 继续补充同一个判断或备用结果：条件 && (dto.expectedTime().getSecond() != 0 || dto.expectedTime().getNano() != 0)) { 是否成立。
                && (dto.expectedTime().getSecond() != 0 || dto.expectedTime().getNano() != 0)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("提示时间仅支持 HH:mm");
        }
        // 设置 预计时间（ExpectedTime） 的值。
        row.setExpectedTime(dto.expectedTime());
    }

    // 检查合法性分类（validateCategory）方法：检查 分类（Category）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 下一个（next），使用 Long 类型保存；可为 null，表示尚未提供。
    // 上一次（previous），使用 Long 类型保存；可为 null，表示尚未提供。
    private void validateCategory(long userId, Long next, Long previous) {
        // 检查next == null，即对应内容是否不存在；成立时执行下面的分支。
        if (next == null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备分类（category），类型为 null；初始内容是取得第一条符合条件的数据。
        var category = categories.find(userId, next);
        // 检查next <= 0 || category == null || category.getDeletedAt() != null && !Objects.equals(next, previous)，即对应内容是否不存在；成立时执行下面的分支。
        if (next <= 0
                // 继续补充同一个判断或备用结果：|| category == null，即对应内容是否不存在。
                || category == null
                // 继续补充同一个判断或备用结果：条件 || category.getDeletedAt() != null && !Objects.equals(next, previous)) { 是否成立。
                || category.getDeletedAt() != null && !Objects.equals(next, previous)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("只能选择本人有效分类");
        }
    }

    // sameRule方法：调用 sameRule 完成sameRule处理；参数负责传入本次处理所需的数据。
    // a，使用 HabitRevisionEntity 类型保存。
    // b，使用 HabitRevisionEntity 类型保存。
    private boolean sameRule(HabitRevisionEntity a, HabitRevisionEntity b) {
        // 把比较两个值是否相同交还给调用方，本方法到此结束。
        return Objects.equals(a.getName(), b.getName())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getDescription(), b.getDescription()) 是否成立。
                && Objects.equals(a.getDescription(), b.getDescription())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getCompletionCriteria(), b.getCompletionCriteria()) 是否成立。
                && Objects.equals(a.getCompletionCriteria(), b.getCompletionCriteria())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getCategoryId(), b.getCategoryId()) 是否成立。
                && Objects.equals(a.getCategoryId(), b.getCategoryId())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getCycleType(), b.getCycleType()) 是否成立。
                && Objects.equals(a.getCycleType(), b.getCycleType())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getWeekdayMask(), b.getWeekdayMask()) 是否成立。
                && Objects.equals(a.getWeekdayMask(), b.getWeekdayMask())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getStartDate(), b.getStartDate()) 是否成立。
                && Objects.equals(a.getStartDate(), b.getStartDate())
                // 继续补充同一个判断或备用结果：条件 && Objects.equals(a.getExpectedTime(), b.getExpectedTime()); 是否成立。
                && Objects.equals(a.getExpectedTime(), b.getExpectedTime());
    }

    // copy方法：调用 copy 完成copy处理；参数负责传入本次处理所需的数据。
    // rule，使用 HabitRevisionEntity 类型保存。
    private HabitRevisionEntity copy(HabitRevisionEntity rule) {
        // 准备结果（result），类型为 null；初始内容是新建 习惯修订版本（HabitRevisionEntity） 实例。
        var result = new HabitRevisionEntity();
        // 设置 名称（Name） 的值。
        result.setName(rule.getName());
        // 设置 说明（Description） 的值。
        result.setDescription(rule.getDescription());
        // 设置 完成情况判断标准（CompletionCriteria） 的值。
        result.setCompletionCriteria(rule.getCompletionCriteria());
        // 设置 分类编号（CategoryId） 的值。
        result.setCategoryId(rule.getCategoryId());
        // 设置 Cycle类型（CycleType） 的值。
        result.setCycleType(rule.getCycleType());
        // 设置 WeekdayMask 的值。
        result.setWeekdayMask(rule.getWeekdayMask());
        // 设置 开始日期（StartDate） 的值。
        result.setStartDate(rule.getStartDate());
        // 设置 预计时间（ExpectedTime） 的值。
        result.setExpectedTime(rule.getExpectedTime());
        // 把结果（result）交还给调用方，本方法到此结束。
        return result;
    }

    // 确认时的数据快照（snapshot）方法：调用 snapshot 完成确认时的数据快照处理；参数负责传入本次处理所需的数据。
    private void snapshot(
            // 当前记录（row），使用 HabitEntity 类型保存。
            HabitEntity row,
            // rule，使用 HabitRevisionEntity 类型保存。
            HabitRevisionEntity rule,
            // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
            String action,
            // 日（day），使用 UserCalendarData 类型保存。
            UserCalendarData day,
            // 生效日期（effectiveDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            LocalDate effectiveDate) {
        // 设置 用户编号（UserId） 的值。
        rule.setUserId(row.getUserId());
        // 设置 习惯编号（HabitId） 的值。
        rule.setHabitId(row.getId());
        // 设置 修订版本No（RevisionNo） 的值。
        rule.setRevisionNo(row.getCurrentRevisionNo());
        // 设置 操作类型（ActionType） 的值。
        rule.setActionType(action);
        // 设置 状态确认时的数据快照（StatusSnapshot） 的值。
        rule.setStatusSnapshot(row.getStatus());
        // 设置 删除标记确认时的数据快照（DeletedSnapshot） 的值。
        rule.setDeletedSnapshot(row.getDeletedAt() != null);
        // 设置 生效日期（EffectiveDate） 的值。
        rule.setEffectiveDate(effectiveDate);
        // 设置 已修改At（ChangedAt） 的值。
        rule.setChangedAt(day.now());
        // 设置 时间时区（TimeZone） 的值。
        rule.setTimeZone(day.zone().getId());
        // 设置 创建时At（CreatedAt） 的值。
        rule.setCreatedAt(day.now());
    }

    // 追加（append）方法：把新条目放到列表末尾；参数负责传入本次处理所需的数据。
    private void append(
            // 当前记录（row），使用 HabitEntity 类型保存。
            HabitEntity row,
            // rule，使用 HabitRevisionEntity 类型保存。
            HabitRevisionEntity rule,
            // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
            String action,
            // 日（day），使用 UserCalendarData 类型保存。
            UserCalendarData day,
            // 生效日期（effectiveDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            LocalDate effectiveDate) {
        // 设置 当前修订版本No（CurrentRevisionNo） 的值。
        row.setCurrentRevisionNo(row.getCurrentRevisionNo() + 1);
        // 设置 更新时At（UpdatedAt） 的值。
        row.setUpdatedAt(day.now());
        // 调用 snapshot 完成确认时的数据快照处理。
        snapshot(row, rule, action, day, effectiveDate);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(revisions.insert(rule));
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 设置 版本号（Version） 的值。
        row.setVersion(row.getVersion() + 1);
    }

    // weekdays方法：调用 weekdays 完成weekdays处理；参数负责传入本次处理所需的数据。
    // mask，使用 int 类型保存。
    private List<Integer> weekdays(int mask) {
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return IntStream.rangeClosed(1, 7)
                // 日（day），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(day -> (mask & (1 << (day - 1))) != 0)
                // 继续对上一步结果执行调用 boxed 完成boxed处理。
                .boxed()
                // 继续对上一步结果执行调用 toList 完成to列表处理。
                .toList();
    }

    // 修订版本页面（revisionView）方法：调用 revisionView 完成修订版本页面处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitRevisionEntity 类型保存。
    private HabitRevisionVO revisionView(HabitRevisionEntity row) {
        // 把新建 习惯修订版本（HabitRevisionVO） 实例交还给调用方，本方法到此结束。
        return new HabitRevisionVO(
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 修订版本No（RevisionNo） 的值。
                row.getRevisionNo(),
                // 读取 操作类型（ActionType） 的值。
                row.getActionType(),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 说明（Description） 的值。
                row.getDescription(),
                // 读取 完成情况判断标准（CompletionCriteria） 的值。
                row.getCompletionCriteria(),
                // 调用 ctx.id 完成编号处理。
                ctx.id(row.getCategoryId()),
                // 读取 Cycle类型（CycleType） 的值。
                row.getCycleType(),
                // 读取 WeekdayMask 的值。
                weekdays(row.getWeekdayMask()),
                // 读取 开始日期（StartDate） 的值。
                row.getStartDate(),
                // 读取 预计时间（ExpectedTime） 的值。
                row.getExpectedTime(),
                // 读取 状态确认时的数据快照（StatusSnapshot） 的值。
                row.getStatusSnapshot(),
                // 读取 删除标记确认时的数据快照（DeletedSnapshot） 的值。
                row.getDeletedSnapshot(),
                // 读取 生效日期（EffectiveDate） 的值。
                row.getEffectiveDate(),
                // 读取 已修改At（ChangedAt） 的值。
                row.getChangedAt(),
                // 读取 时间时区（TimeZone） 的值。
                row.getTimeZone());
    }
}
