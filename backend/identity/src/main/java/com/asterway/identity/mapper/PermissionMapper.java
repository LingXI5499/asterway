package com.asterway.identity.mapper;

import com.asterway.identity.entity.PermissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Permission 数据访问接口，同一表的查询和修改集中在同名 XML。 */
@Mapper
public interface PermissionMapper {

    /** 读取所有未删除权限，供授权选项和安全快照使用。 */
    List<PermissionEntity> findAll();

    /** 读取权限详情。 */
    PermissionEntity findById(@Param("id") long id);

    /** 读取有效角色的当前权限并集。 */
    List<String> findCodesByUserId(@Param("userId") long userId);

    /** 新增扩展权限并回填标识。 */
    int insert(PermissionEntity entity);

    /** 比较版本后更新。 */
    int update(PermissionEntity entity);

    /** 检查角色授权与未删除菜单引用。 */
    long countReferences(@Param("id") long id);

    /** 逻辑删除未引用的扩展权限。 */
    int softDelete(PermissionEntity entity);
}
