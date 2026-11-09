/*
 * 实现Setting业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入JSON数据Utils（JsonUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.JsonUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入Setting更新（SettingUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.SettingUpdateDTO;
// 引入Setting（SettingEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.SettingEntity;
// 引入Setting数据库读写器（SettingMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.SettingMapper;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入Setting业务服务（SettingService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.SettingService;
// 引入PublicSetting（PublicSettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PublicSettingVO;
// 引入Setting（SettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.SettingVO;
// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Propagation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Propagation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

/* 只维护站点名称和注册开关，配置更新与注册共享锁协调。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 SettingServiceImpl；实现Setting业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class SettingServiceImpl implements SettingService {

    // 访问数据库的 Mapper（mapper），使用 SettingMapper 类型保存。
    private final SettingMapper mapper;

    // logs，使用 OperationLogService 类型保存。
    private final OperationLogService logs;

    // JSON 编码和解析库（json），使用 JsonUtils 类型保存。
    private final JsonUtils json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public SettingServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 SettingMapper 类型保存。
            SettingMapper mapper,
            // logs，使用 OperationLogService 类型保存。
            OperationLogService logs,
            // JSON 编码和解析库（json），使用 JsonUtils 类型保存。
            JsonUtils json) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把logs保存到 this.logs。
        this.logs = logs;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public SettingVO get() {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:setting:read");
        // 把调用 values 完成值列表处理交还给调用方，本方法到此结束。
        return values();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // public配置（publicSettings）方法：调用 publicSettings 完成public配置处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public PublicSettingVO publicSettings() {
        // 准备值（value），类型为 SettingVO；初始内容是调用 values 完成值列表处理。
        SettingVO value = values();
        // 把新建 PublicSetting（PublicSettingVO） 实例交还给调用方，本方法到此结束。
        return new PublicSettingVO(value.siteName(), value.registrationEnabled());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // requireRegistration是否展开（requireRegistrationOpen）方法：要求满足 Registration是否展开（RegistrationOpen）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional(propagation = Propagation.MANDATORY)
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void requireRegistrationOpen() {
        // 准备值（value），类型为 SettingEntity；初始内容是读取并锁定符合条件的记录，具体 SQL 对应 lockRegistration。
        SettingEntity value = mapper.lockRegistration();
        // 检查value == null || !"true".equals(value.getSettingValue())，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || !"true".equals(value.getSettingValue())) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(403, "REGISTRATION_CLOSED", "当前未开放注册");
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 SettingUpdateDTO 类型保存。
    public SettingVO update(SettingUpdateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:setting:update");
        // 检查dto.siteName() == null || dto.siteName().isBlank() || dto.registrationEnabled() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.siteName() == null || dto.siteName().isBlank() || dto.registrationEnabled() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("账户配置无效");
        }
        // 两个 UPDATE 在同一事务内，第二项冲突会回滚第一项。
        // 准备site，类型为 SettingEntity；初始内容是新建 Setting（SettingEntity） 实例。
        SettingEntity site = new SettingEntity();
        // 设置 Setting键（SettingKey） 的值。
        site.setSettingKey("site_name");
        // 设置 Setting值（SettingValue） 的值。
        site.setSettingValue(json.write(dto.siteName()));
        // 设置 版本号（Version） 的值。
        site.setVersion(dto.siteNameVersion());
        // 设置 更新时按（UpdatedBy） 的值。
        site.setUpdatedBy(actor.userId());
        // 准备registration，类型为 SettingEntity；初始内容是新建 Setting（SettingEntity） 实例。
        SettingEntity registration = new SettingEntity();
        // 设置 Setting键（SettingKey） 的值。
        registration.setSettingKey("registration_enabled");
        // 设置 Setting值（SettingValue） 的值。
        registration.setSettingValue(json.write(dto.registrationEnabled()));
        // 设置 版本号（Version） 的值。
        registration.setVersion(dto.registrationVersion());
        // 设置 更新时按（UpdatedBy） 的值。
        registration.setUpdatedBy(actor.userId());
        // 检查条件 mapper.update(site) != 1 || mapper.update(registration) != 1 是否成立；成立时执行下面的分支。
        if (mapper.update(site) != 1 || mapper.update(registration) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("配置已被修改，请刷新后重试");
        }
        // 把新条目放到列表末尾。
        logs.append("identity:setting:update", "SETTING", "account", null,
                // 调用 Map.of 完成of处理。
                Map.of("siteName", dto.siteName(), "registrationEnabled", dto.registrationEnabled()));
        // 把调用 values 完成值列表处理交还给调用方，本方法到此结束。
        return values();
    }

    // 值列表（values）方法：调用 values 完成值列表处理；参数负责传入本次处理所需的数据。
    private SettingVO values() {
        // 准备entries，类型为 Map<String, SettingEntity>；初始内容是新建 摘要值对应表（HashMap） 实例。
        Map<String, SettingEntity> entries = new HashMap<>();
        // entry，使用 SettingEntity 类型保存。
        // 逐次执行循环：for (SettingEntity entry : mapper.findAll())；每轮处理一条数据或推进一次检查。
        for (SettingEntity entry : mapper.findAll()) {
            // 保存指定键对应的值。
            entries.put(entry.getSettingKey(), entry);
        }
        // 准备site，类型为 SettingEntity；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        SettingEntity site = entries.get("site_name");
        // 准备registration，类型为 SettingEntity；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        SettingEntity registration = entries.get("registration_enabled");
        // 检查site == null || registration == null，即对应内容是否不存在；成立时执行下面的分支。
        if (site == null || registration == null) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(503, "CONFIGURATION_MISSING", "账户配置未初始化");
        }
        // 把新建 Setting（SettingVO） 实例交还给调用方，本方法到此结束。
        return new SettingVO(json.text(site.getSettingValue()), "true".equals(registration.getSettingValue()),
                // 读取 版本号（Version） 的值。
                site.getVersion(), registration.getVersion());
    }

}
