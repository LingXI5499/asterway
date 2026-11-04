package com.asterway.identity.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** RoleMenu 数据访问接口；SQL 全部定义在同名 XML。 */
@Mapper
public interface RoleMenuMapper {

    /** 读取当前关联标识，用于回显和替换。 */
    List<Long> findIds(@Param("roleId") long roleId);

    /** 清理当前主体关联，只在完整事务内调用。 */
    int deleteByOwner(@Param("roleId") long roleId);

    /** 插入一个已验证关联，联合主键防止重复。 */
    int insert(@Param("roleId") long roleId, @Param("menuId") long menuId, @Param("actorId") Long actorId);
}
