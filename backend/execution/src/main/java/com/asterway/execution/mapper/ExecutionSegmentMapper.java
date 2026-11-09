/*
 * 声明执行计时连续计时段数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.mapper;

// 引入执行计时读取（ExecutionReadDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.dto.ExecutionReadDTO;
// 引入执行计时连续计时段（ExecutionSegmentEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.entity.ExecutionSegmentEntity;
// 引入执行计时连续计时段（ExecutionSegmentVO），下面可以直接使用这个类型或静态成员。
import com.asterway.execution.vo.ExecutionSegmentVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 时间段从属于会话，只由会话 Service 在同一事务内写入。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 ExecutionSegmentMapper；声明执行计时连续计时段数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface ExecutionSegmentMapper {
    // 是否展开（open）方法：调用 open 完成是否展开处理；参数负责传入本次处理所需的数据。
    ExecutionSegmentEntity open(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 执行计时编号（executionId），使用 long 类型保存。
            @Param("userId") long userId, @Param("executionId") long executionId);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 下一个数值（nextNumber）方法：调用 nextNumber 完成下一个数值处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 执行计时编号（executionId），使用 long 类型保存。
    int nextNumber(@Param("userId") long userId, @Param("executionId") long executionId);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 ExecutionSegmentEntity 类型保存。
    int insert(ExecutionSegmentEntity row);

    // 关闭（close）方法：释放连接、文件或客户端占用的资源；参数负责传入本次处理所需的数据。
    int close(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("endedAt") Instant endedAt,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("reason") String reason,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // shorten方法：调用 shorten 完成shorten处理；参数负责传入本次处理所需的数据。
    int shorten(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 执行计时编号（executionId），使用 long 类型保存。
            @Param("executionId") long executionId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 截止时间（deadline），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("deadline") Instant deadline,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<ExecutionSegmentVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 上下文（context），使用 ExecutionReadDTO 类型保存。
            @Param("ctx") ExecutionReadDTO context,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 执行计时编号（executionId），使用 long 类型保存。
            @Param("executionId") long executionId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 执行计时编号（executionId），使用 long 类型保存。
    long count(@Param("userId") long userId, @Param("executionId") long executionId);
}
