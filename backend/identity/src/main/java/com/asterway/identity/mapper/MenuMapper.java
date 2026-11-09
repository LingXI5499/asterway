/*
 * 声明菜单数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入菜单（MenuEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.MenuEntity;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* Menu 数据访问接口，同一表的查询和修改集中在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 MenuMapper；声明菜单数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface MenuMapper {

    /* 读取未删除菜单，管理树与有效菜单共用。 */
    // 查找全部（findAll）方法：查找 全部（All）；参数负责传入本次处理所需的数据。
    List<MenuEntity> findAll();

    /* 读取菜单详情。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找按编号（findById）方法：查找 按编号（ById）；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    MenuEntity findById(@Param("id") long id);

    /* 合并有效角色分配的菜单标识。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找Granted编号列表（findGrantedIds）方法：查找 Granted编号列表（GrantedIds）；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    List<Long> findGrantedIds(@Param("userId") long userId);

    /* 检查角色分配和未删除子菜单引用。 */
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量References（countReferences）方法：调用 countReferences 完成数量References处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    long countReferences(@Param("id") long id);

    /* 新增目录、页面或按钮。 */
    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // entity，使用 MenuEntity 类型保存。
    int insert(MenuEntity entity);

    /* 更新同一菜单并比较版本。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // entity，使用 MenuEntity 类型保存。
    int update(MenuEntity entity);

    /* 逻辑删除已解除引用的扩展菜单。 */
    // soft删除（softDelete）方法：调用 softDelete 完成soft删除处理；参数负责传入本次处理所需的数据。
    // entity，使用 MenuEntity 类型保存。
    int softDelete(MenuEntity entity);
}
