/*
 * 声明用户角色数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* UserRole 数据访问接口；SQL 全部定义在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 UserRoleMapper；声明用户角色数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface UserRoleMapper {

    /* 读取当前关联标识，用于回显和替换。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找编号列表（findIds）方法：查找 编号列表（Ids）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    List<Long> findIds(@Param("userId") long userId);

    /* 清理当前主体关联，只在完整事务内调用。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 删除按所属人（deleteByOwner）方法：删除 按所属人（ByOwner）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    int deleteByOwner(@Param("userId") long userId);

    /* 插入一个已验证关联，联合主键防止重复。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 角色编号（roleId），使用 long 类型保存。
    // actor编号（actorId），使用 Long 类型保存；可为 null，表示尚未提供。
    int insert(@Param("userId") long userId, @Param("roleId") long roleId, @Param("actorId") Long actorId);
}
