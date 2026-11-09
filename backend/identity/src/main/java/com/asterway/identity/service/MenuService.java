/*
 * 规定菜单业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入菜单保存（MenuSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuSaveDTO;
// 引入菜单（MenuVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.MenuVO;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 目录、页面和按钮的同一管理链路。 */
// 定义接口 MenuService；规定菜单业务服务可供其他代码调用的业务方法。
public interface MenuService {

    /* 超管读取平铺菜单。 */
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    List<MenuVO> list();

    /* 超管读取管理菜单树。 */
    // tree方法：调用 tree 完成tree处理；参数负责传入本次处理所需的数据。
    List<MenuVO> tree();

    /* 根据当前身份过滤菜单、权限及完整父链。 */
    // 当前Tree（currentTree）方法：调用 currentTree 完成当前Tree处理；参数负责传入本次处理所需的数据。
    List<MenuVO> currentTree();

    /* 超管读取节点。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    MenuVO get(long id);

    /* 校验父链与组件白名单后新增。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    MenuVO create(MenuSaveDTO dto);

    /* 比较版本并拒绝循环父链。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    MenuVO update(long id, MenuSaveDTO dto);

    /* 只删除未引用、无有效子节点的扩展菜单。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);
}
