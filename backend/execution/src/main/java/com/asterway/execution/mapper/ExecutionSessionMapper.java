/*
 * 声明执行计时会话数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.mapper;

// 引入执行计时账号数据（ExecutionAccountData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionAccountData;
// 引入执行计时查询条件（ExecutionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionQueryDTO;
// 引入执行计时读取（ExecutionReadDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionReadDTO;
// 引入执行计时会话（ExecutionSessionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.entity.ExecutionSessionEntity;
// 引入执行计时会话（ExecutionSessionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSessionVO;
// 引入执行计时汇总（ExecutionSummaryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSummaryVO;
// 引入执行计时任务（ExecutionTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionTaskVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 会话持久化与执行范围查询，全部 SQL 位于 XML，不依赖其他业务 Mapper。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 ExecutionSessionMapper；声明执行计时会话数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface ExecutionSessionMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 加锁用户（lockUser）方法：调用 lockUser 完成加锁用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    Long lockUser(@Param("userId") long userId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // try加锁用户（tryLockUser）方法：调用 tryLockUser 完成try加锁用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    Long tryLockUser(@Param("userId") long userId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 账号（account）方法：调用 account 完成账号处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    ExecutionAccountData account(@Param("userId") long userId);

    // eligible任务（eligibleTask）方法：调用 eligibleTask 完成eligible任务处理；参数负责传入本次处理所需的数据。
    ExecutionTaskVO eligibleTask(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 任务编号（taskId），使用 long 类型保存。
            @Param("taskId") long taskId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            @Param("today") LocalDate today);

    // 数据范围是否存在（scopeExists）方法：调用 scopeExists 完成数据范围是否存在处理；参数负责传入本次处理所需的数据。
    Long scopeExists(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 数据范围（scope），使用 String 类型保存；可为 null，表示尚未提供。
            // 编号（id），使用 long 类型保存。
            @Param("userId") long userId, @Param("scope") String scope, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找（find）方法：取得第一条符合条件的数据；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    ExecutionSessionEntity find(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 运行中（running）方法：调用 running 完成运行中处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    ExecutionSessionEntity running(@Param("userId") long userId);

    // unfinished任务（unfinishedTask）方法：调用 unfinishedTask 完成unfinished任务处理；参数负责传入本次处理所需的数据。
    ExecutionSessionEntity unfinishedTask(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 任务编号（taskId），使用 long 类型保存。
            @Param("userId") long userId, @Param("taskId") long taskId);

    // unfinished方法：调用 unfinished 完成unfinished处理；参数负责传入本次处理所需的数据。
    List<ExecutionSessionEntity> unfinished(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 数据范围（scope），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("scope") String scope,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上限（limit），使用 int 类型保存。
            @Param("limit") int limit);

    // 运行中In数据范围（runningInScope）方法：调用 runningInScope 完成运行中In数据范围处理；参数负责传入本次处理所需的数据。
    boolean runningInScope(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 数据范围（scope），使用 String 类型保存；可为 null，表示尚未提供。
            // 编号（id），使用 long 类型保存。
            @Param("userId") long userId, @Param("scope") String scope, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 应完成用户列表（dueUsers）方法：调用 dueUsers 完成应完成用户列表处理；参数负责传入本次处理所需的数据。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    // 上限（limit），使用 int 类型保存。
    List<Long> dueUsers(@Param("now") Instant now, @Param("limit") int limit);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
    int insert(ExecutionSessionEntity row);

    // 修改当前状态（changeState）方法：调用 changeState 完成修改当前状态处理；参数负责传入本次处理所需的数据。
    int changeState(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前记录（row），使用 ExecutionSessionEntity 类型保存。
            @Param("row") ExecutionSessionEntity row,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 预计（expected），使用 long 类型保存。
            @Param("expected") long expected,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 心跳（heartbeat）方法：调用 heartbeat 完成心跳处理；参数负责传入本次处理所需的数据。
    int heartbeat(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 连续计时段编号（segmentId），使用 long 类型保存。
            @Param("segmentId") long segmentId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("sessionId") String sessionId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 开始任务（startTask）方法：调用 startTask 完成开始任务处理；参数负责传入本次处理所需的数据。
    int startTask(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 任务编号（taskId），使用 long 类型保存。
            @Param("taskId") long taskId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 版本号（version），使用 long 类型保存。
            @Param("version") long version,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 ExecutionReadDTO 类型保存。
    // 编号（id），使用 long 类型保存。
    ExecutionSessionVO view(@Param("ctx") ExecutionReadDTO context, @Param("id") long id);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<ExecutionSessionVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 ExecutionReadDTO 类型保存。
            @Param("ctx") ExecutionReadDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @Param("q") ExecutionQueryDTO query,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 上下文（context），使用 ExecutionReadDTO 类型保存。
    // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
    long count(@Param("ctx") ExecutionReadDTO context, @Param("q") ExecutionQueryDTO query);

    // 汇总（summary）方法：调用 summary 完成汇总处理；参数负责传入本次处理所需的数据。
    ExecutionSummaryVO summary(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 ExecutionReadDTO 类型保存。
            // 查询条件（query），使用 ExecutionQueryDTO 类型保存。
            @Param("ctx") ExecutionReadDTO context, @Param("q") ExecutionQueryDTO query);
}
