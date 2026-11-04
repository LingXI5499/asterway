package com.asterway.identity.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.identity.dto.PermissionQueryDTO;
import com.asterway.identity.dto.PermissionSaveDTO;
import com.asterway.identity.vo.PermissionVO;

/** 动作权限的完整单表管理链路。 */
public interface PermissionService {

    /** 超管查询并分页筛选权限。 */
    PageResultVO<PermissionVO> page(PermissionQueryDTO query);

    /** 超管读取单条权限。 */
    PermissionVO get(long id);

    /** 新增扩展权限；不会因此自动建立接口。 */
    PermissionVO create(PermissionSaveDTO dto);

    /** 比较版本后修改，内置编码不允许改变。 */
    PermissionVO update(long id, PermissionSaveDTO dto);

    /** 只删除未引用的扩展权限；内置权限受保护。 */
    void delete(long id, long version);
}
