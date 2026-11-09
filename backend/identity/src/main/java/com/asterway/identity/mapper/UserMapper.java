/*
 * 声明用户数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入用户（UserEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.UserEntity;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* User 数据访问接口；SQL 全部定义在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 UserMapper；声明用户数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface UserMapper {

    /* 按稳定标识读取，包含停用和删除状态供认证判断。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找按编号（findById）方法：查找 按编号（ById）；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    UserEntity findById(@Param("id") long id);

    /* 按大小写无关登录名读取。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找按用户名（findByUsername）方法：查找 按用户名（ByUsername）；参数负责传入本次处理所需的数据。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    UserEntity findByUsername(@Param("username") String username);

    /* 修改前锁定账户行，事务结束释放。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 加锁按编号（lockById）方法：按编号锁住数据库记录，防止并发修改互相覆盖；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    UserEntity lockById(@Param("id") long id);

    /* 按操作者范围分页查询未删除账户。 */
    // 查找当前页（findPage）方法：查找 当前页（Page）；参数负责传入本次处理所需的数据。
    List<UserEntity> findPage(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // keyword，使用 String 类型保存；可为 null，表示尚未提供。
            @Param("keyword") String keyword,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
            @Param("enabled") Boolean enabled,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 角色（role），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("role") String role,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 普通用户仅（regularOnly），使用 boolean 类型保存。
            @Param("regularOnly") boolean regularOnly,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size
    );

    /* 使用与分页相同的条件计数。 */
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    long count(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // keyword，使用 String 类型保存；可为 null，表示尚未提供。
            @Param("keyword") String keyword,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
            @Param("enabled") Boolean enabled,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 角色（role），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("role") String role,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 普通用户仅（regularOnly），使用 boolean 类型保存。
            @Param("regularOnly") boolean regularOnly
    );

    /* 在同一管理范围内统计账户状态。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 统计（statistics）方法：调用 statistics 完成统计处理；参数负责传入本次处理所需的数据。
    // 普通用户仅（regularOnly），使用 boolean 类型保存。
    Map<String, Object> statistics(@Param("regularOnly") boolean regularOnly);

    /* 新增账户并回填主键，不包含角色写入。 */
    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // entity，使用 UserEntity 类型保存。
    int insert(UserEntity entity);

    /* 比较 version 后更新用户名与昵称。 */
    // 更新个人资料（updateProfile）方法：更新 个人资料（Profile）；参数负责传入本次处理所需的数据。
    // entity，使用 UserEntity 类型保存。
    int updateProfile(UserEntity entity);

    /* 更新密码、安全版本及强制改密状态。 */
    // 更新密码（updatePassword）方法：更新 密码（Password）；参数负责传入本次处理所需的数据。
    // entity，使用 UserEntity 类型保存。
    int updatePassword(UserEntity entity);

    /* 启停账户或逻辑删除，并递增安全版本。 */
    // 更新状态（updateStatus）方法：更新 状态（Status）；参数负责传入本次处理所需的数据。
    // entity，使用 UserEntity 类型保存。
    int updateStatus(UserEntity entity);

    /* 保存待生效或已生效时区。 */
    // 更新时间时区（updateTimeZone）方法：更新 时间时区（TimeZone）；参数负责传入本次处理所需的数据。
    // entity，使用 UserEntity 类型保存。
    int updateTimeZone(UserEntity entity);
    /* 仅提升安全版本并比较旧版本，用于退出全部设备。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // invalidate会话列表（invalidateSessions）方法：调用 invalidateSessions 完成invalidate会话列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    // actor编号（actorId），使用 long 类型保存。
    int invalidateSessions(@Param("id") long id, @Param("version") long version, @Param("actorId") long actorId);
}
