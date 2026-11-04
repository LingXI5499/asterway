package com.asterway.identity.service;

import com.asterway.identity.dto.SettingUpdateDTO;
import com.asterway.identity.vo.PublicSettingVO;
import com.asterway.identity.vo.SettingVO;

/** 两项固定账户设置，不提供任意配置项增删接口。 */
public interface SettingService {

    /** 超管读取配置及版本。 */
    SettingVO get();

    /** 公开读取站点名称和注册开关，不返回配置元数据。 */
    PublicSettingVO publicSettings();

    /** 在注册调用方事务内共享锁定开关，关闭时拒绝写入。 */
    void requireRegistrationOpen();

    /** 同一事务比较两项旧版本并更新，失败整体回滚。 */
    SettingVO update(SettingUpdateDTO dto);
}
