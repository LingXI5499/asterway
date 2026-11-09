/*
 * 规定权限业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入权限查询条件（PermissionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionQueryDTO;
// 引入权限保存（PermissionSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionSaveDTO;
// 引入权限（PermissionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PermissionVO;

/* 动作权限的完整单表管理链路。 */
// 定义接口 PermissionService；规定权限业务服务可供其他代码调用的业务方法。
public interface PermissionService {

    /* 超管查询并分页筛选权限。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 PermissionQueryDTO 类型保存。
    PageResultVO<PermissionVO> page(PermissionQueryDTO query);

    /* 超管读取单条权限。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    PermissionVO get(long id);

    /* 新增扩展权限；不会因此自动建立接口。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    PermissionVO create(PermissionSaveDTO dto);

    /* 比较版本后修改，内置编码不允许改变。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    PermissionVO update(long id, PermissionSaveDTO dto);

    /* 只删除未引用的扩展权限；内置权限受保护。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);
}
