/*
 * 实现登录日志业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入请求Utils（RequestUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.RequestUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入日志查询条件（LogQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LogQueryDTO;
// 引入登录日志（LoginLogEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.LoginLogEntity;
// 引入登录日志数据库读写器（LoginLogMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.LoginLogMapper;
// 引入登录日志业务服务（LoginLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.LoginLogService;
// 引入登录日志（LoginLogVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.LoginLogVO;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;
// 引入MDC，下面可以直接使用这个类型或静态成员。
import org.slf4j.MDC;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

/* 每次登录结果独立记录；不提供修改和删除审计记录的方法。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 LoginLogServiceImpl；实现登录日志业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class LoginLogServiceImpl implements LoginLogService {

    // 访问数据库的 Mapper（mapper），使用 LoginLogMapper 类型保存。
    private final LoginLogMapper mapper;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public LoginLogServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 LoginLogMapper 类型保存。
            LoginLogMapper mapper) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 追加（append）方法：把新条目放到列表末尾；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    // 成功（success），使用 boolean 类型保存。
    // 原因（reason），使用 String 类型保存；可为 null，表示尚未提供。
    // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
    public void append(Long userId, String username, boolean success, String reason, String sessionId) {
        // 准备事件（event），类型为 LoginLogEntity；初始内容是新建 登录日志（LoginLogEntity） 实例。
        LoginLogEntity event = new LoginLogEntity();
        // 设置 用户编号（UserId） 的值。
        event.setUserId(userId);
        // 设置 用户名确认时的数据快照（UsernameSnapshot） 的值。
        event.setUsernameSnapshot(username);
        // 设置 Outcome 的值。
        event.setOutcome(success ? "SUCCESS" : "FAILURE");
        // 设置 原因编码（ReasonCode） 的值。
        event.setReasonCode(reason);
        // 设置 会话编号（SessionId） 的值。
        event.setSessionId(sessionId);
        // 设置 IpAddress 的值。
        event.setIpAddress(RequestUtils.ipAddress());
        // 设置 用户成长助手（UserAgent） 的值。
        event.setUserAgent(RequestUtils.userAgent());
        // 准备成长助手（agent），类型为 String；初始内容是调用 RequestUtils.userAgent 完成用户成长助手处理。
        String agent = RequestUtils.userAgent();
        // 设置 设备显示名称（DeviceLabel） 的值。
        event.setDeviceLabel(agent.substring(0, Math.min(128, agent.length())));
        // 设置 追踪标识编号（TraceId） 的值。
        event.setTraceId(Objects.toString(MDC.get("traceId"), UUID.randomUUID().toString()));
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(event);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 LogQueryDTO 类型保存。
    public PageResultVO<LoginLogVO> page(LogQueryDTO query) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:login_log:list");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备普通用户仅（regularOnly），类型为 boolean；初始内容是调用 !actor.superAdmin 完成superAdmin处理。
        boolean regularOnly = !actor.superAdmin();
        // 准备记录列表（rows），类型为 List<LoginLogVO>；初始内容是把前面筛选、转换后的结果汇成列表。
        List<LoginLogVO> rows = mapper.findPage(regularOnly, actor.userId(), query.getUserId(), offset, query.getSize())
                // 继续对上一步结果执行把前面筛选、转换后的结果汇成列表。
                .stream().map(this::toVO).toList();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(rows, mapper.count(regularOnly, actor.userId(), query.getUserId()),
                // 读取 当前页（Page） 的值。
                query.getPage(), query.getSize());
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 事件（event），使用 LoginLogEntity 类型保存。
    private LoginLogVO toVO(LoginLogEntity event) {
        // 把新建 登录日志（LoginLogVO） 实例交还给调用方，本方法到此结束。
        return new LoginLogVO(event.getId().toString(), event.getUserId() == null ? null : event.getUserId().toString(),
                // 读取 用户名确认时的数据快照（UsernameSnapshot） 的值。
                event.getUsernameSnapshot(), event.getOutcome(), event.getReasonCode(), event.getSessionId(),
                // 读取 IpAddress 的值。
                event.getIpAddress(), event.getUserAgent(), event.getDeviceLabel(), event.getTraceId(), event.getOccurredAt());
    }

}
