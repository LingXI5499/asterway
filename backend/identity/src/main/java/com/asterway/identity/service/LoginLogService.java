package com.asterway.identity.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.identity.dto.LogQueryDTO;
import com.asterway.identity.vo.LoginLogVO;

/** 登录日志的追加和查询链路。 */
public interface LoginLogService {

    /** 独立短事务记录一次认证结果，不记录任何凭据。 */
    void append(Long userId, String username, boolean success, String reason, String sessionId);

    /** 普通管理员仅查询已识别普通用户日志，匿名失败仅超管可见。 */
    PageResultVO<LoginLogVO> page(LogQueryDTO query);
}
