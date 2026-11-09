/*
 * 规定登录日志业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入日志查询条件（LogQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LogQueryDTO;
// 引入登录日志（LoginLogVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.LoginLogVO;

/* 登录日志的追加和查询链路。 */
// 定义接口 LoginLogService；规定登录日志业务服务可供其他代码调用的业务方法。
public interface LoginLogService {

    /* 独立短事务记录一次认证结果，不记录任何凭据。 */
    // 追加（append）方法：把新条目放到列表末尾；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    // 成功（success），使用 boolean 类型保存。
    // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    void append(Long userId, String username, boolean success, String reason, String sessionId);

    /* 普通管理员仅查询已识别普通用户日志，匿名失败仅超管可见。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 LogQueryDTO 类型保存。
    PageResultVO<LoginLogVO> page(LogQueryDTO query);
}
