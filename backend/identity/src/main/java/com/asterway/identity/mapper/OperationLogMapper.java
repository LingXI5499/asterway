package com.asterway.identity.mapper;

import com.asterway.identity.entity.OperationLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** OperationLog 数据访问接口，同一表的查询和修改集中在同名 XML。 */
@Mapper
public interface OperationLogMapper {

    /** 追加经过脱敏的审计事件，不提供修改删除方法。 */
    int insert(OperationLogEntity entity);

    /** 在管理范围内按时间倒序查询日志。 */
    List<OperationLogEntity> findPage(
            @Param("regularOnly") boolean regularOnly,
            @Param("actorId") long actorId,
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /** 与分页使用相同的管理范围。 */
    long count(@Param("regularOnly") boolean regularOnly, @Param("actorId") long actorId, @Param("userId") Long userId);
}
