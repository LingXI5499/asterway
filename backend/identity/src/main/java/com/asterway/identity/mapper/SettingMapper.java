package com.asterway.identity.mapper;

import com.asterway.identity.entity.SettingEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** Setting 数据访问接口，同一表的查询和修改集中在同名 XML。 */
@Mapper
public interface SettingMapper {

    /** 读取账户配置。 */
    List<SettingEntity> findAll();

    /** 注册写入时共享锁定开关，和关闭注册事务协调。 */
    SettingEntity lockRegistration();

    /** 按版本修改固定配置键。 */
    int update(SettingEntity entity);
}
