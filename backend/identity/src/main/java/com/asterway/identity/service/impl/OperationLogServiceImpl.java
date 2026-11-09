/*
 * 实现操作日志业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入账号当前登录身份（AccountPrincipal），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountPrincipal;
// 引入JSON数据Utils（JsonUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.JsonUtils;
// 引入请求Utils（RequestUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.RequestUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入日志查询条件（LogQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LogQueryDTO;
// 引入操作日志（OperationLogEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.OperationLogEntity;
// 引入操作日志数据库读写器（OperationLogMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.OperationLogMapper;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入操作日志（OperationLogVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.OperationLogVO;

// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;
// 引入Security上下文Holder（SecurityContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.core.context.SecurityContextHolder;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;
// 引入请求上下文Holder（RequestContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.RequestContextHolder;
// 引入Servlet请求Attributes（ServletRequestAttributes），下面可以直接使用这个类型或静态成员。
import org.springframework.web.context.request.ServletRequestAttributes;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;
// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import java.util.regex.Pattern;

/* 操作审计只追加；与成功业务写入保持同一事务。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 OperationLogServiceImpl；实现操作日志业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class OperationLogServiceImpl implements OperationLogService {

    // 访问数据库的 Mapper（mapper），使用 OperationLogMapper 类型保存。
    private final OperationLogMapper mapper;

    // JSON 编码和解析库（json），使用 JsonUtils 类型保存。
    private final JsonUtils json;

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 记录成长业务成功（recordGrowthSuccess）方法：调用 recordGrowthSuccess 完成记录成长业务成功处理；参数负责传入本次处理所需的数据。
    @Override
    // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
    // 所属人编号（ownerId），使用 long 类型保存。
    public void recordGrowthSuccess(String action, long ownerId) {
        // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
        String resourceId = null;
        // 检查条件 RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes 是否成立；成立时执行下面的分支。
        if (RequestContextHolder.getRequestAttributes()
                // attributes，使用 ServletRequestAttributes 类型保存。
                instanceof ServletRequestAttributes attributes) {
            // 准备路径（path），类型为 String；初始内容是读取 本次 HTTP 请求（Request） 的值。
            String path = attributes.getRequest().getServletPath();
            // 把集合或文字的长度保存到 resourceId。
            resourceId = path.substring(0, Math.min(100, path.length()));
        }
        // 调用 append(action, "GROWTH", resourceId, ownerId, Map.of 完成of处理。
        append(action, "GROWTH", resourceId, ownerId, Map.of());
    }

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 访问数据库的 Mapper（mapper），使用 OperationLogMapper 类型保存。
    // JSON 编码和解析库（json），使用 JsonUtils 类型保存。
    public OperationLogServiceImpl(OperationLogMapper mapper, JsonUtils json) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 追加（append）方法：把新条目放到列表末尾；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void append(
            // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
            String action,
            // resource类型（resourceType），使用 String 类型保存；可为 null，表示尚未提供。
            String resourceType,
            // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
            String resourceId,
            // 目标值用户编号（targetUserId），使用 Long 类型保存；可为 null，表示尚未提供。
            Long targetUserId,
            // 汇总（summary），使用 Map<String, Object> 类型保存。
            Map<String, Object> summary) {
        // 准备allowed，类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
        Set<String> allowed =
                // 继续传入或组合调用 Set.of 完成of处理，完成上面尚未结束的调用或声明。
                Set.of(
                        // 继续传入或组合固定文字 'username'，完成上面尚未结束的调用或声明。
                        "username",
                        // 继续传入或组合固定文字 'nickname'，完成上面尚未结束的调用或声明。
                        "nickname",
                        // 继续传入或组合固定文字 'enabled'，完成上面尚未结束的调用或声明。
                        "enabled",
                        // 继续传入或组合固定文字 'roleIds'，完成上面尚未结束的调用或声明。
                        "roleIds",
                        // 继续传入或组合固定文字 'permissionIds'，完成上面尚未结束的调用或声明。
                        "permissionIds",
                        // 继续传入或组合固定文字 'menuIds'，完成上面尚未结束的调用或声明。
                        "menuIds",
                        // 继续传入或组合固定文字 'version'，完成上面尚未结束的调用或声明。
                        "version",
                        // 继续传入或组合固定文字 'siteName'，完成上面尚未结束的调用或声明。
                        "siteName",
                        // 继续传入或组合固定文字 'registrationEnabled'，完成上面尚未结束的调用或声明。
                        "registrationEnabled",
                        // 继续传入或组合固定文字 'timeZone'，完成上面尚未结束的调用或声明。
                        "timeZone",
                        // 继续传入或组合固定文字 'pendingTimeZone'，完成上面尚未结束的调用或声明。
                        "pendingTimeZone",
                        // 继续传入或组合表达式 "passwordChanged") 的结果，完成上面尚未结束的调用或声明。
                        "passwordChanged");
        // 检查条件 !allowed.containsAll(summary.keySet()) 是否成立；成立时执行下面的分支。
        if (!allowed.containsAll(summary.keySet())) {
            // 中止正常处理并抛出异常：新建 Illegal参数Exception（IllegalArgumentException） 实例；由上层按失败处理。
            throw new IllegalArgumentException("审计摘要包含未经允许的字段");
        }
        // 准备身份认证（authentication），类型为 null；初始内容是读取 上下文（Context） 的值。
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // 准备当前登录身份（principal），类型为 null；初始内容是根据前面的条件，从两个候选结果中选择一个：authentication != null && authentication.getPrincipal() instanceof AccountPrincipal value ? value : …。
        var principal =
                // 继续传入或组合条件判断的结果：authentication != null，完成上面尚未结束的调用或声明。
                authentication != null
                                // 值（value），使用 AccountPrincipal 类型保存。
                                && authentication.getPrincipal() instanceof AccountPrincipal value
                        // 继续补充同一个判断或备用结果：条件 ? value 是否成立。
                        ? value
                        // 继续补充同一个判断或备用结果：条件 : null; 是否成立。
                        : null;
        // 准备事件（event），类型为 OperationLogEntity；初始内容是新建 操作日志（OperationLogEntity） 实例。
        OperationLogEntity event = new OperationLogEntity();
        // 设置 Actor编号（ActorId） 的值。
        event.setActorId(principal == null ? null : principal.userId());
        // 设置 Actor用户名（ActorUsername） 的值。
        event.setActorUsername(principal == null ? null : principal.username());
        // 设置 目标值用户编号（TargetUserId） 的值。
        event.setTargetUserId(targetUserId);
        // 设置 Resource类型（ResourceType） 的值。
        event.setResourceType(resourceType);
        // 设置 Resource编号（ResourceId） 的值。
        event.setResourceId(resourceId);
        // 设置 操作编码（ActionCode） 的值。
        event.setActionCode(action);
        // 设置 Outcome 的值。
        event.setOutcome("SUCCESS");
        // 设置 修改汇总（ChangeSummary） 的值。
        event.setChangeSummary(json.write(summary));
        // 设置 IpAddress 的值。
        event.setIpAddress(RequestUtils.ipAddress());
        // 设置 追踪标识编号（TraceId） 的值。
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(event);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 LogQueryDTO 类型保存。
    public PageResultVO<OperationLogVO> page(LogQueryDTO query) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:operation_log:list");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备普通用户仅（regularOnly），类型为 boolean；初始内容是调用 !actor.superAdmin 完成superAdmin处理。
        boolean regularOnly = !actor.superAdmin();
        // 准备记录列表（rows），类型为 List<OperationLogVO>；初始内容是把前面筛选、转换后的结果汇成列表。
        List<OperationLogVO> rows =
                // 继续传入或组合访问数据库的 Mapper（mapper），完成上面尚未结束的调用或声明。
                mapper
                        // 继续对上一步结果执行查找 当前页（Page）。
                        .findPage(
                                // 继续传入或组合普通用户仅（regularOnly），完成上面尚未结束的调用或声明。
                                regularOnly,
                                // 调用 actor.userId 完成用户编号处理。
                                actor.userId(),
                                // 读取 用户编号（UserId） 的值。
                                query.getUserId(),
                                // 继续传入或组合分页时跳过的记录数（offset），完成上面尚未结束的调用或声明。
                                offset,
                                // 读取 长度或数量校验规则（Size） 的值。
                                query.getSize())
                        // 继续对上一步结果执行调用 stream 完成分段回复流处理。
                        .stream()
                        // 继续对上一步结果执行把每个条目转换成需要的形式。
                        .map(this::toVO)
                        // 继续对上一步结果执行调用 toList 完成to列表处理。
                        .toList();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 继续传入或组合记录列表（rows），完成上面尚未结束的调用或声明。
                rows,
                // 统计符合条件的记录数，具体 SQL 对应 count。
                mapper.count(regularOnly, actor.userId(), query.getUserId()),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 事件（event），使用 OperationLogEntity 类型保存。
    private OperationLogVO toVO(OperationLogEntity event) {
        // 把新建 操作日志（OperationLogVO） 实例交还给调用方，本方法到此结束。
        return new OperationLogVO(
                // 读取 编号（Id） 的值。
                id(event.getId()),
                // 读取 Actor编号（ActorId） 的值。
                id(event.getActorId()),
                // 读取 Actor用户名（ActorUsername） 的值。
                event.getActorUsername(),
                // 读取 目标值用户编号（TargetUserId） 的值。
                id(event.getTargetUserId()),
                // 读取 Resource类型（ResourceType） 的值。
                event.getResourceType(),
                // 读取 Resource编号（ResourceId） 的值。
                event.getResourceId(),
                // 读取 操作编码（ActionCode） 的值。
                event.getActionCode(),
                // 读取 Outcome 的值。
                event.getOutcome(),
                // 读取 原因编码（ReasonCode） 的值。
                event.getReasonCode(),
                // 调用 json.object 完成对象处理。
                json.object(event.getChangeSummary()),
                // 读取 IpAddress 的值。
                event.getIpAddress(),
                // 读取 追踪标识编号（TraceId） 的值。
                event.getTraceId(),
                // 读取 OccurredAt 的值。
                event.getOccurredAt());
    }

    // 编号（id）方法：调用 id 完成编号处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 Long 类型保存；可为 null，表示尚未提供。
    private String id(Long value) {
        // 把根据前面的条件，从两个候选结果中选择一个：value == null ? null : value.toString()交还给调用方，本方法到此结束。
        return value == null ? null : value.toString();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 记录失败（recordFailure）方法：调用 recordFailure 完成记录失败处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // 状态（status），使用 int 类型保存。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    public void recordFailure(int status, String code) {
        // 检查条件 !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) 是否成立；成立时执行下面的分支。
        if (!(RequestContextHolder.getRequestAttributes()
                // attributes，使用 ServletRequestAttributes 类型保存。
                instanceof ServletRequestAttributes attributes)) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备请求（request），类型为 null；初始内容是读取 本次 HTTP 请求（Request） 的值。
        var request = attributes.getRequest();
        // 检查条件 !Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod()) 是否成立；成立时执行下面的分支。
        if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备身份认证（authentication），类型为 null；初始内容是读取 上下文（Context） 的值。
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // 检查authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal actor)，即对应内容是否不存在；成立时执行下面的分支。
        if (authentication == null
                // actor，使用 AccountPrincipal 类型保存。
                || !(authentication.getPrincipal() instanceof AccountPrincipal actor)) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备路径（path），类型为 String；初始内容是读取 Servlet路径（ServletPath） 的值。
        String path = request.getServletPath();
        // 检查条件 path.startsWith("/asterway/executions/") && path.endsWith("/heartbeat") 是否成立；成立时执行下面的分支。
        if (path.startsWith("/asterway/executions/") && path.endsWith("/heartbeat")) {
            // 失效计时器可能重复发出旧段心跳，不把高频心跳写成安全操作日志。
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备目标值（target），类型为 Long；初始内容是根据前面的条件，从两个候选结果中选择一个：path.startsWith("/asterway/me") ? actor.userId() : null。
        Long target = path.startsWith("/asterway/me") ? actor.userId() : null;
        // 准备match，类型为 null；初始内容是调用 Pattern.compile 完成compile处理。
        var match = Pattern.compile("^/asterway/users/([0-9]+)(?:/|$)").matcher(path);
        // 检查条件 match.find() 是否成立；成立时执行下面的分支。
        if (match.find()) {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 把把输入转换成数值，供比较、计算或按编号查询保存到 target。
                target = Long.parseLong(match.group(1));
            // 错误（error），使用 NumberFormatException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (NumberFormatException error) {
                // 把空值，表示当前还没有对应内容保存到 target。
                target = null;
            }
        }
        // 准备事件（event），类型为 OperationLogEntity；初始内容是新建 操作日志（OperationLogEntity） 实例。
        OperationLogEntity event = new OperationLogEntity();
        // 设置 Actor编号（ActorId） 的值。
        event.setActorId(actor.userId());
        // 设置 Actor用户名（ActorUsername） 的值。
        event.setActorUsername(actor.username());
        // 设置 目标值用户编号（TargetUserId） 的值。
        event.setTargetUserId(target);
        // 设置 Resource类型（ResourceType） 的值。
        event.setResourceType("REQUEST");
        // 设置 Resource编号（ResourceId） 的值。
        event.setResourceId(path.substring(0, Math.min(100, path.length())));
        // 设置 操作编码（ActionCode） 的值。
        event.setActionCode("HTTP_" + request.getMethod());
        // 设置 Outcome 的值。
        event.setOutcome("FAILURE");
        // 设置 原因编码（ReasonCode） 的值。
        event.setReasonCode(code);
        // 设置 修改汇总（ChangeSummary） 的值。
        event.setChangeSummary(json.write(Map.of("status", status)));
        // 设置 IpAddress 的值。
        event.setIpAddress(RequestUtils.ipAddress());
        // 设置 追踪标识编号（TraceId） 的值。
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(event);
    }
}
