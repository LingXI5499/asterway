/*
 * 声明角色数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入角色（RoleEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.RoleEntity;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* Role 数据访问接口；SQL 全部定义在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 RoleMapper；声明角色数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface RoleMapper {

    /* 查询本轮角色列表。 */
    // 查找全部（findAll）方法：查找 全部（All）；参数负责传入本次处理所需的数据。
    List<RoleEntity> findAll();

    /* 读取角色。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找按编号（findById）方法：查找 按编号（ById）；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    RoleEntity findById(@Param("id") long id);

    /* 读取仍有效的角色编码。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找Codes按用户编号（findCodesByUserId）方法：查找 Codes按用户编号（CodesByUserId）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    List<String> findCodesByUserId(@Param("userId") long userId);

    /* 读取全部管理身份，用于目标范围保护，不因角色停用而放宽。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找AssignedCodes（findAssignedCodes）方法：查找 AssignedCodes；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    List<String> findAssignedCodes(@Param("userId") long userId);

    /* 先锁内置超管角色行，统一串行化管理身份和授权变更。 */
    // 加锁SecurityGate（lockSecurityGate）方法：调用 lockSecurityGate 完成加锁SecurityGate处理；参数负责传入本次处理所需的数据。
    Long lockSecurityGate();

    /* 在持有安全门锁时统计可用超管。 */
    // 数量是否启用SuperAdmins（countEnabledSuperAdmins）方法：调用 countEnabledSuperAdmins 完成数量是否启用SuperAdmins处理；参数负责传入本次处理所需的数据。
    long countEnabledSuperAdmins();

    /* 角色展示信息更新并比较旧版本。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // entity，使用 RoleEntity 类型保存。
    int update(RoleEntity entity);

    /* 替换授权前递增角色版本。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // advance版本号（advanceVersion）方法：调用 advanceVersion 完成advance版本号处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    // actor编号（actorId），使用 long 类型保存。
    int advanceVersion(@Param("id") long id, @Param("version") long version, @Param("actorId") long actorId);
}
