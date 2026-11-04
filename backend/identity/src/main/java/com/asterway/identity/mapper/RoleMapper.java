package com.asterway.identity.mapper;

import com.asterway.identity.entity.RoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Role 数据访问接口；SQL 全部定义在同名 XML。 */
@Mapper
public interface RoleMapper {

    /** 查询本轮角色列表。 */
    List<RoleEntity> findAll();

    /** 读取角色。 */
    RoleEntity findById(@Param("id") long id);

    /** 读取仍有效的角色编码。 */
    List<String> findCodesByUserId(@Param("userId") long userId);

    /** 读取全部管理身份，用于目标范围保护，不因角色停用而放宽。 */
    List<String> findAssignedCodes(@Param("userId") long userId);

    /** 先锁内置超管角色行，统一串行化管理身份和授权变更。 */
    Long lockSecurityGate();

    /** 在持有安全门锁时统计可用超管。 */
    long countEnabledSuperAdmins();

    /** 角色展示信息更新并比较旧版本。 */
    int update(RoleEntity entity);

    /** 替换授权前递增角色版本。 */
    int advanceVersion(@Param("id") long id, @Param("version") long version, @Param("actorId") long actorId);
}
