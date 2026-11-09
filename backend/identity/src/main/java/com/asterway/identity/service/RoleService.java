/*
 * 规定角色业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入菜单Grant（MenuGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuGrantDTO;
// 引入权限Grant（PermissionGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionGrantDTO;
// 引入角色更新（RoleUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.RoleUpdateDTO;
// 引入角色（RoleVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.RoleVO;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 三角色查询、展示信息和关系授权集中在同一链路。 */
// 定义接口 RoleService；规定角色业务服务可供其他代码调用的业务方法。
public interface RoleService {

    /* 超管查询三个内置角色及授权回显。 */
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    List<RoleVO> list();

    /* 超管查询角色详情。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    RoleVO get(long id);

    /* 只修改展示信息，不改变角色身份或编码。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 RoleUpdateDTO 类型保存。
    RoleVO update(long id, RoleUpdateDTO dto);

    /* 读取角色动作权限标识。 */
    // 权限列表（permissions）方法：调用 permissions 完成权限列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    List<String> permissions(long id);

    /* 读取角色菜单标识。 */
    // 菜单列表（menus）方法：调用 menus 完成菜单列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    List<String> menus(long id);

    /* 持有统一安全门锁，检查引用和角色边界后原子替换权限。 */
    // grant权限列表（grantPermissions）方法：调用 grantPermissions 完成grant权限列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PermissionGrantDTO 类型保存。
    RoleVO grantPermissions(long id, PermissionGrantDTO dto);

    /* 校验并补齐祖先目录，原子替换菜单授权。 */
    // grant菜单列表（grantMenus）方法：调用 grantMenus 完成grant菜单列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuGrantDTO 类型保存。
    RoleVO grantMenus(long id, MenuGrantDTO dto);
}
