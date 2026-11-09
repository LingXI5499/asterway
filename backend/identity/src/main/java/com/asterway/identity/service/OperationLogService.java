/*
 * 规定操作日志业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入操作Audit提供器（OperationAuditProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.OperationAuditProvider;
// 引入日志查询条件（LogQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LogQueryDTO;
// 引入操作日志（OperationLogVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.OperationLogVO;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;

/* 操作日志只追加和按范围查询，不额外拆分新增、查询服务。 */
// 定义接口 OperationLogService；规定操作日志业务服务可供其他代码调用的业务方法。
public interface OperationLogService extends OperationAuditProvider {

    /* 在业务事务内追加白名单摘要；失败回滚同一业务写入。 */
    // 追加（append）方法：把新条目放到列表末尾；参数负责传入本次处理所需的数据。
    // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
    // resource类型（resourceType），使用 String 类型保存；可为 null，表示尚未提供。
    // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
    // 目标值用户编号（targetUserId），使用 Long 类型保存；可为 null，表示尚未提供。
    // 汇总（summary），使用 Map<String, Object> 类型保存。
    void append(String action, String resourceType, String resourceId, Long targetUserId, Map<String, Object> summary);

    /* 管理员只能查本人对普通账户的操作；超管查询完整审计。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 LogQueryDTO 类型保存。
    PageResultVO<OperationLogVO> page(LogQueryDTO query);
}
