package com.asterway.identity.mapper;

import com.asterway.identity.entity.LoginLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** LoginLog 数据访问接口，同一表的查询和修改集中在同名 XML。 */
@Mapper
public interface LoginLogMapper {

    /** 追加经过脱敏的审计事件，不提供修改删除方法。 */
    int insert(LoginLogEntity entity);

    /** 在管理范围内按时间倒序查询日志。 */
    List<LoginLogEntity> findPage(
            @Param("regularOnly") boolean regularOnly,
            @Param("actorId") long actorId,
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /** 与分页使用相同的管理范围。 */
    long count(@Param("regularOnly") boolean regularOnly, @Param("actorId") long actorId, @Param("userId") Long userId);
}
