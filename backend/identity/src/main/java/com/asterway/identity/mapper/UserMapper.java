package com.asterway.identity.mapper;

import com.asterway.identity.entity.UserEntity;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** User 数据访问接口；SQL 全部定义在同名 XML。 */
@Mapper
public interface UserMapper {

    /** 按稳定标识读取，包含停用和删除状态供认证判断。 */
    UserEntity findById(@Param("id") long id);

    /** 按大小写无关登录名读取。 */
    UserEntity findByUsername(@Param("username") String username);

    /** 修改前锁定账户行，事务结束释放。 */
    UserEntity lockById(@Param("id") long id);

    /** 按操作者范围分页查询未删除账户。 */
    List<UserEntity> findPage(
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            @Param("role") String role,
            @Param("regularOnly") boolean regularOnly,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /** 使用与分页相同的条件计数。 */
    long count(
            @Param("keyword") String keyword,
            @Param("enabled") Boolean enabled,
            @Param("role") String role,
            @Param("regularOnly") boolean regularOnly
    );

    /** 在同一管理范围内统计账户状态。 */
    Map<String, Object> statistics(@Param("regularOnly") boolean regularOnly);

    /** 新增账户并回填主键，不包含角色写入。 */
    int insert(UserEntity entity);

    /** 比较 version 后更新用户名与昵称。 */
    int updateProfile(UserEntity entity);

    /** 更新密码、安全版本及强制改密状态。 */
    int updatePassword(UserEntity entity);

    /** 启停账户或逻辑删除，并递增安全版本。 */
    int updateStatus(UserEntity entity);

    /** 保存待生效或已生效时区。 */
    int updateTimeZone(UserEntity entity);
    /** 仅提升安全版本并比较旧版本，用于退出全部设备。 */
    int invalidateSessions(@Param("id") long id, @Param("version") long version, @Param("actorId") long actorId);
}
