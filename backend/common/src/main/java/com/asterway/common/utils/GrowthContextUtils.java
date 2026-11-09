/*
 * 统一个人业务权限、归属检查、并发锁、乐观版本和业务日期规则。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入成长业务Support数据库读写器（GrowthSupportMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.common.mapper.GrowthSupportMapper;
// 引入账号Security提供器（AccountSecurityProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountSecurityProvider;
// 引入成长业务数据范围数据（GrowthScopeData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GrowthScopeData;

// 引入对象提供器（ObjectProvider），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.ObjectProvider;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;
// 引入事务Synchronization管理员（TransactionSynchronizationManager），下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 跨模块公共校验：所有成长写入口先锁定同一个用户，避免检查后父对象失效。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 GrowthContextUtils；统一个人业务权限、归属检查、并发锁、乐观版本和业务日期规则。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class GrowthContextUtils {
    // 访问数据库的 Mapper（mapper），使用 GrowthSupportMapper 类型保存。
    private final GrowthSupportMapper mapper;
    // audit，使用 com.asterway.common.security.OperationAuditProvider 类型保存。
    private final com.asterway.common.security.OperationAuditProvider audit;
    // accounts，使用 ObjectProvider<AccountSecurityProvider> 类型保存。
    private final ObjectProvider<AccountSecurityProvider> accounts;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public GrowthContextUtils(
            // 访问数据库的 Mapper（mapper），使用 GrowthSupportMapper 类型保存。
            GrowthSupportMapper mapper,
            // audit，使用 com.asterway.common.security.OperationAuditProvider 类型保存。
            com.asterway.common.security.OperationAuditProvider audit,
            // accounts，使用 ObjectProvider<AccountSecurityProvider> 类型保存。
            ObjectProvider<AccountSecurityProvider> accounts) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把audit保存到 this.audit。
        this.audit = audit;
        // 把accounts保存到 this.accounts。
        this.accounts = accounts;
    }

    // 读取（read）方法：调用 read 完成读取处理；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    public long read(String permission) {
        // 把要求满足 权限（Permission）交还给调用方，本方法到此结束。
        return SecurityUtils.requirePermission(permission).userId();
    }

    /* 中立生命周期契约使用，必须加入既有父事务，锁顺序仍先用户后业务对象。 */
    // 加锁Lifecycle用户（lockLifecycleUser）方法：调用 lockLifecycleUser 完成加锁Lifecycle用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    public void lockLifecycleUser(long userId) {
        // 检查条件 !TransactionSynchronizationManager.isActualTransactionActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("生命周期联动必须参加父业务事务");
        }
        // 检查mapper.lockAnyUser(userId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (mapper.lockAnyUser(userId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
    }

    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    // 权限（permission），使用 String 类型保存；可为 null，表示尚未提供。
    public long write(String permission) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission(permission);
        // 准备用户编号（userId），类型为 long；初始内容是调用 actor.userId 完成用户编号处理。
        long userId = actor.userId();
        // 检查条件 !TransactionSynchronizationManager.isActualTransactionActive() 是否成立；成立时执行下面的分支。
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("成长写操作必须在事务内");
        }
        // 检查mapper.lockUser(userId) == null，即对应内容是否不存在；成立时执行下面的分支。
        if (mapper.lockUser(userId) == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 等待用户锁时可能发生账号停用、改密或角色变更，不能继续使用请求开始时的旧身份。
        // 准备fresh，类型为 null；初始内容是读取 对象（Object） 的值。
        var fresh = accounts.getObject().loadSecurityAccount(userId);
        // 检查当前记录版本是否符合请求要求：fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()；成立时执行下面的分支。
        if (fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 检查当前用户是否具有需要的权限：GrowthAccessUtils.permission(permission) && (GrowthAccessUtils.manager(fresh.roles()) || !fresh.roles().contains("USER")) || !fresh.roles().conta…；成立时执行下面的分支。
        if (GrowthAccessUtils.permission(permission)
                        // 继续补充同一个判断或备用结果：条件 && (GrowthAccessUtils.manager(fresh.roles()) 是否成立。
                        && (GrowthAccessUtils.manager(fresh.roles())
                                // 继续补充同一个判断或备用结果：条件 || !fresh.roles().contains("USER")) 是否成立。
                                || !fresh.roles().contains("USER"))
                // 继续补充同一个判断或备用结果：条件 || !fresh.roles().contains("SUPER_ADMIN") 是否成立。
                || !fresh.roles().contains("SUPER_ADMIN")
                        // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& !fresh.permissions().contains(permission)) {。
                        && !fresh.permissions().contains(permission)) {
            // 中止正常处理并抛出异常：调用 BusinessException.forbidden 完成forbidden处理；由上层按失败处理。
            throw BusinessException.forbidden();
        }
        // 调用 org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization 完成registerSynchronization处理。
        org.springframework.transaction.support.TransactionSynchronizationManager
                // 继续对上一步结果执行调用 registerSynchronization 完成registerSynchronization处理。
                .registerSynchronization(
                        // 定义类 ；统一个人业务权限、归属检查、并发锁、乐观版本和业务日期规则。
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            // 说明此方法实现或改写了父类、接口中已经声明的方法。
                            // 变化前提交回执（beforeCommit）方法：调用 beforeCommit 完成变化前提交回执处理；参数负责传入本次处理所需的数据。
                            @Override
                            // 是否只读（readOnly），使用 boolean 类型保存。
                            public void beforeCommit(boolean readOnly) {
                                // 检查条件 !readOnly 是否成立；成立时执行下面的分支。
                                if (!readOnly) {
                                    // 调用 audit.recordGrowthSuccess 完成记录成长业务成功处理。
                                    audit.recordGrowthSuccess(permission, userId);
                                }
                            }
                        });
        // 把用户编号（userId）交还给调用方，本方法到此结束。
        return userId;
    }

    // 数据范围（scope）方法：调用 scope 完成数据范围处理；参数负责传入本次处理所需的数据。
    // 类型（type），使用 String 类型保存；可为 null，表示尚未提供。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    // writable，使用 boolean 类型保存。
    public GrowthScopeData scope(String type, long userId, long id, boolean writable) {
        // 准备当前记录（row），类型为 GrowthScopeData；初始内容是调用 switch (type) { case "goal" -> yield mapper.goal 完成目标处理。
        GrowthScopeData row =
                // 根据同一个值选择对应分支，避免把不同状态混在一起处理。
                switch (type) {
                    // 匹配这一种取值时使用本分支：case "goal"。
                    case "goal" -> mapper.goal(userId, id);
                    // 匹配这一种取值时使用本分支：case "stage"。
                    case "stage" -> mapper.stage(userId, id);
                    // 匹配这一种取值时使用本分支：case "task"。
                    case "task" -> mapper.task(userId, id);
                    // 匹配这一种取值时使用本分支：case "week"。
                    case "week" -> mapper.week(userId, id);
                    // 匹配这一种取值时使用本分支：case "day"。
                    case "day" -> mapper.day(userId, id);
                    // 匹配这一种取值时使用本分支：default。
                    // 中止正常处理并抛出异常：新建 Illegal参数Exception（IllegalArgumentException） 实例；由上层按失败处理。
                    default -> throw new IllegalArgumentException("未知资源类型");
                };
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 检查条件 writable && row.getArchivedAt() != null 是否成立；成立时执行下面的分支。
        if (writable && row.getArchivedAt() != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("目标已归档，请先恢复");
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 今天（today）方法：调用 today 完成今天处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    public LocalDate today(long userId) {
        // 准备时区（zone），类型为 String；初始内容是调用 mapper.timeZone 完成用户使用的时区处理。
        String zone = mapper.timeZone(userId);
        // 检查zone == null，即对应内容是否不存在；成立时执行下面的分支。
        if (zone == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把调用 LocalDate.now 完成当前时间处理交还给调用方，本方法到此结束。
        return LocalDate.now(ZoneId.of(zone));
    }

    // 版本号（version）方法：调用 version 完成版本号处理；参数负责传入本次处理所需的数据。
    // 实际（actual），使用 Long 类型保存；可为 null，表示尚未提供。
    // 预计（expected），使用 Long 类型保存；可为 null，表示尚未提供。
    public void version(Long actual, Long expected) {
        // 检查expected == null || !Objects.equals(actual, expected)，即对应内容是否不存在；成立时执行下面的分支。
        if (expected == null || !Objects.equals(actual, expected)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("数据版本已变化，请刷新后重试");
        }
    }

    // 已修改（changed）方法：调用 changed 完成已修改处理；参数负责传入本次处理所需的数据。
    // 数量（count），使用 int 类型保存。
    public void changed(int count) {
        // 检查条件 count != 1 是否成立；成立时执行下面的分支。
        if (count != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("数据已变化，请刷新后重试");
        }
    }

    // dates方法：调用 dates 完成dates处理；参数负责传入本次处理所需的数据。
    // 开始（start），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    // 结束（end），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public void dates(LocalDate start, LocalDate end) {
        // 检查条件 start != null && end != null && end.isBefore(start) 是否成立；成立时执行下面的分支。
        if (start != null && end != null && end.isBefore(start)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
    }

    // 名称（name）方法：调用 name 完成名称处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public String name(String value) {
        // 检查value == null || value.isBlank() || value.codePointCount(0, value.length()) > 100，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 100) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("名称须为 1～100 个字符且不能全为空白");
        }
        // 把值（value）交还给调用方，本方法到此结束。
        return value;
    }

    // 文字（text）方法：调用 text 完成文字处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    public String text(String value) {
        // 把根据前面的条件，从两个候选结果中选择一个：value == null ? "" : value交还给调用方，本方法到此结束。
        return value == null ? "" : value;
    }

    // 顺序（order）方法：调用 order 完成顺序处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 Integer 类型保存；可为 null，表示尚未提供。
    public int order(Integer value) {
        // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null) {
            // 把固定数值 0交还给调用方，本方法到此结束。
            return 0;
        }
        // 检查条件 value < 0 是否成立；成立时执行下面的分支。
        if (value < 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("排序不能小于零");
        }
        // 把值（value）交还给调用方，本方法到此结束。
        return value;
    }

    // 当前状态（state）方法：调用 state 完成当前状态处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    // 任务（task），使用 boolean 类型保存。
    public String state(String value, boolean task) {
        // 检查value == null || !(task ? Set.of("NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED") : Set.of("NOT_…，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null
                // 继续补充同一个判断或备用结果：条件 || !(task 是否成立。
                || !(task
                                // 继续补充同一个判断或备用结果：条件 ? Set.of("NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED") 是否成立。
                                ? Set.of("NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED")
                                // 继续补充同一个判断或备用结果：条件 : Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED")) 是否成立。
                                : Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED"))
                        // 继续对上一步结果执行判断集合是否包含指定内容。
                        .contains(value)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("状态无效");
        }
        // 把值（value）交还给调用方，本方法到此结束。
        return value;
    }

    // 结束任务（endTask）方法：调用 endTask 完成结束任务处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    public void endTask(long userId, long id) {
        // 调用 mapper.endTask 完成结束任务处理。
        mapper.endTask(userId, id);
    }

    // 结束目标（endGoal）方法：调用 endGoal 完成结束目标处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    public void endGoal(long userId, long id) {
        // 调用 mapper.endGoal 完成结束目标处理。
        mapper.endGoal(userId, id);
    }

    // 编号（id）方法：调用 id 完成编号处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 Long 类型保存；可为 null，表示尚未提供。
    public String id(Long value) {
        // 把根据前面的条件，从两个候选结果中选择一个：value == null ? null : value.toString()交还给调用方，本方法到此结束。
        return value == null ? null : value.toString();
    }

    // 阶段Overview（stageOverview）方法：调用 stageOverview 完成阶段Overview处理；参数负责传入本次处理所需的数据。
    public java.util.List<com.asterway.common.result.GrowthOverviewVO> stageOverview(
            // 用户编号（userId），使用 long 类型保存。
            // 编号（id），使用 long 类型保存。
            long userId, long id) {
        // 把调用 mapper.stageOverview 完成阶段Overview处理交还给调用方，本方法到此结束。
        return mapper.stageOverview(userId, id);
    }

    // 任务Overview（taskOverview）方法：调用 taskOverview 完成任务Overview处理；参数负责传入本次处理所需的数据。
    public java.util.List<com.asterway.common.result.GrowthOverviewVO> taskOverview(
            // 用户编号（userId），使用 long 类型保存。
            // 编号（id），使用 long 类型保存。
            long userId, long id) {
        // 把调用 mapper.taskOverview 完成任务Overview处理交还给调用方，本方法到此结束。
        return mapper.taskOverview(userId, id);
    }

    // 阶段列表（stages）方法：调用 stages 完成阶段列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    public long stages(long userId, long id) {
        // 把调用 mapper.stages 完成阶段列表处理交还给调用方，本方法到此结束。
        return mapper.stages(userId, id);
    }

    // 任务列表（tasks）方法：调用 tasks 完成任务列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    public long tasks(long userId, long id) {
        // 把调用 mapper.tasks 完成任务列表处理交还给调用方，本方法到此结束。
        return mapper.tasks(userId, id);
    }

    // prepare方法：调用 prepare 完成prepare处理；参数负责传入本次处理所需的数据。
    // q，使用 com.asterway.common.dto.GrowthQueryDTO 类型保存。
    // 用户编号（userId），使用 long 类型保存。
    public void prepare(com.asterway.common.dto.GrowthQueryDTO q, long userId) {
        // 调用 q.offset 完成分页时跳过的记录数处理。
        q.offset();
        // 设置 今天（Today） 的值。
        q.setToday(today(userId));
        // 读取 开始来源（StartFrom） 的值。
        dates(q.getStartFrom(), q.getStartTo());
        // 读取 日期来源（DateFrom） 的值。
        dates(q.getDateFrom(), q.getDateTo());
        // 检查输入或集合是否为空：q.getStatus() != null && !q.getStatus().isBlank()；成立时执行下面的分支。
        if (q.getStatus() != null && !q.getStatus().isBlank()) {
            // 读取 状态（Status） 的值。
            state(q.getStatus(), true);
        }
        // 检查输入或集合是否为空：q.getPriority() != null && !q.getPriority().isBlank() && !Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(q.getPriority())；成立时执行下面的分支。
        if (q.getPriority() != null
                // 继续补充同一个判断或备用结果：输入或集合是否为空：&& !q.getPriority().isBlank()。
                && !q.getPriority().isBlank()
                // 继续补充同一个判断或备用结果：条件 && !Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(q.getPriority())) { 是否成立。
                && !Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(q.getPriority())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("优先级无效");
        }
        // 检查输入或集合是否为空：q.getAssignmentStatus() != null && !q.getAssignmentStatus().isBlank() && !Set.of("POOL", "WEEK", "DAY").contains(q.getAssignmentStatus())；成立时执行下面的分支。
        if (q.getAssignmentStatus() != null
                // 继续补充同一个判断或备用结果：输入或集合是否为空：&& !q.getAssignmentStatus().isBlank()。
                && !q.getAssignmentStatus().isBlank()
                // 继续补充同一个判断或备用结果：条件 && !Set.of("POOL", "WEEK", "DAY").contains(q.getAssignmentStatus())) { 是否成立。
                && !Set.of("POOL", "WEEK", "DAY").contains(q.getAssignmentStatus())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("安排状态无效");
        }
        // 检查输入或集合是否为空：q.getTiming() != null && !q.getTiming().isBlank() && !Set.of("ALL", "CURRENT", "FUTURE").contains(q.getTiming())；成立时执行下面的分支。
        if (q.getTiming() != null
                // 继续补充同一个判断或备用结果：输入或集合是否为空：&& !q.getTiming().isBlank()。
                && !q.getTiming().isBlank()
                // 继续补充同一个判断或备用结果：条件 && !Set.of("ALL", "CURRENT", "FUTURE").contains(q.getTiming())) { 是否成立。
                && !Set.of("ALL", "CURRENT", "FUTURE").contains(q.getTiming())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("时间筛选无效");
        }
        // 检查条件 q.getKeyword() != null && q.getKeyword().length() > 100 是否成立；成立时执行下面的分支。
        if (q.getKeyword() != null && q.getKeyword().length() > 100) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("关键字过长");
        }
        // 编号（id），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long id : new Long[]；每轮处理一条数据或推进一次检查。
        for (Long id :
                // 继续传入或组合新建 长整数（Long[]） 实例，完成上面尚未结束的调用或声明。
                new Long[] {
                    // 读取 目标编号（GoalId） 的值。
                    q.getGoalId(),
                    // 读取 阶段编号（StageId） 的值。
                    q.getStageId(),
                    // 读取 任务编号（TaskId） 的值。
                    q.getTaskId(),
                    // 读取 每周方案编号（WeeklyPlanId） 的值。
                    q.getWeeklyPlanId(),
                    // 读取 每日方案编号（DailyPlanId） 的值。
                    q.getDailyPlanId()
                }) {
            // 检查条件 id != null && id <= 0 是否成立；成立时执行下面的分支。
            if (id != null && id <= 0) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("资源标识无效");
            }
        }
    }
}
