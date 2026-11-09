/*
 * 声明操作日志数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入操作日志（OperationLogEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.OperationLogEntity;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* OperationLog 数据访问接口，同一表的查询和修改集中在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 OperationLogMapper；声明操作日志数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface OperationLogMapper {

    /* 追加经过脱敏的审计事件，不提供修改删除方法。 */
    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // entity，使用 OperationLogEntity 类型保存。
    int insert(OperationLogEntity entity);

    /* 在管理范围内按时间倒序查询日志。 */
    // 查找当前页（findPage）方法：查找 当前页（Page）；参数负责传入本次处理所需的数据。
    List<OperationLogEntity> findPage(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 普通用户仅（regularOnly），使用 boolean 类型保存。
            @Param("regularOnly") boolean regularOnly,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // actor编号（actorId），使用 long 类型保存。
            @Param("actorId") long actorId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
            @Param("userId") Long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size
    );

    /* 与分页使用相同的管理范围。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 普通用户仅（regularOnly），使用 boolean 类型保存。
    // actor编号（actorId），使用 long 类型保存。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    long count(@Param("regularOnly") boolean regularOnly, @Param("actorId") long actorId, @Param("userId") Long userId);
}
