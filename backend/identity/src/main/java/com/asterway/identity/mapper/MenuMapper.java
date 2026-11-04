package com.asterway.identity.mapper;

import com.asterway.identity.entity.MenuEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Menu 数据访问接口，同一表的查询和修改集中在同名 XML。 */
@Mapper
public interface MenuMapper {

    /** 读取未删除菜单，管理树与有效菜单共用。 */
    List<MenuEntity> findAll();

    /** 读取菜单详情。 */
    MenuEntity findById(@Param("id") long id);

    /** 合并有效角色分配的菜单标识。 */
    List<Long> findGrantedIds(@Param("userId") long userId);

    /** 检查角色分配和未删除子菜单引用。 */
    long countReferences(@Param("id") long id);

    /** 新增目录、页面或按钮。 */
    int insert(MenuEntity entity);

    /** 更新同一菜单并比较版本。 */
    int update(MenuEntity entity);

    /** 逻辑删除已解除引用的扩展菜单。 */
    int softDelete(MenuEntity entity);
}
