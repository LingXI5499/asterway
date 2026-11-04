package com.asterway.identity.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.OperationAuditProvider;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.vo.OperationLogVO;
import java.util.Map;

/** 操作日志只追加和按范围查询，不额外拆分新增、查询服务。 */
public interface OperationLogService extends OperationAuditProvider {

    /** 在业务事务内追加白名单摘要；失败回滚同一业务写入。 */
    void append(String action, String resourceType, String resourceId, Long targetUserId, Map<String, Object> summary);

    /** 管理员只能查本人对普通账户的操作；超管查询完整审计。 */
    PageResultVO<OperationLogVO> page(LogQueryDTO query);
}
