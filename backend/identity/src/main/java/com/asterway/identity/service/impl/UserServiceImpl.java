/*
 * 实现用户业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入Captcha结果（CaptchaResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.CaptchaResultVO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入执行计时Lifecycle提供器（ExecutionLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.ExecutionLifecycleProvider;
// 引入目标通知提供器（GoalNotificationProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.GoalNotificationProvider;
// 引入习惯Lifecycle提供器（HabitLifecycleProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.HabitLifecycleProvider;
// 引入登录会话数据（LoginSessionData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.LoginSessionData;
// 引入Redis缓存尝试Store（RedisAttemptStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisAttemptStore;
// 引入Redis缓存CaptchaStore（RedisCaptchaStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisCaptchaStore;
// 引入Redis缓存会话Store（RedisSessionStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisSessionStore;
// 引入Security账号数据（SecurityAccountData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.SecurityAccountData;
// 引入账号ValidationUtils（AccountValidationUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.AccountValidationUtils;
// 引入Jwt身份凭据Utils（JwtTokenUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.JwtTokenUtils;
// 引入请求Utils（RequestUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.RequestUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入事务Utils（TransactionUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.TransactionUtils;
// 引入Captcha创建（CaptchaCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.CaptchaCreateDTO;
// 引入Captcha校验（CaptchaVerifyDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.CaptchaVerifyDTO;
// 引入登录（LoginDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LoginDTO;
// 引入密码Reset（PasswordResetDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PasswordResetDTO;
// 引入密码更新（PasswordUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PasswordUpdateDTO;
// 引入Registration（RegistrationDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.RegistrationDTO;
// 引入时间时区更新（TimeZoneUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.TimeZoneUpdateDTO;
// 引入用户创建（UserCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserCreateDTO;
// 引入用户查询条件（UserQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserQueryDTO;
// 引入用户角色列表（UserRolesDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserRolesDTO;
// 引入用户状态（UserStatusDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserStatusDTO;
// 引入用户更新（UserUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserUpdateDTO;
// 引入角色（RoleEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.RoleEntity;
// 引入用户（UserEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.UserEntity;
// 引入权限数据库读写器（PermissionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.PermissionMapper;
// 引入角色数据库读写器（RoleMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RoleMapper;
// 引入用户数据库读写器（UserMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.UserMapper;
// 引入用户角色数据库读写器（UserRoleMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.UserRoleMapper;
// 引入登录日志业务服务（LoginLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.LoginLogService;
// 引入菜单业务服务（MenuService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.MenuService;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入Setting业务服务（SettingService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.SettingService;
// 引入用户业务服务（UserService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.UserService;
// 引入Captcha本次请求序号（CaptchaTicketVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.CaptchaTicketVO;
// 引入当前用户（CurrentUserVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.CurrentUserVO;
// 引入设备（DeviceVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.DeviceVO;
// 引入登录结果（LoginResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.LoginResultVO;
// 引入时间时区（TimeZoneVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.TimeZoneVO;
// 引入用户统计（UserStatisticsVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.UserStatisticsVO;
// 引入用户（UserVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.UserVO;

// 引入密码Encoder（PasswordEncoder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.crypto.password.PasswordEncoder;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;
// 引入Array列表（ArrayList），下面可以直接使用这个类型或静态成员。
import java.util.ArrayList;
// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入摘要值设置（HashSet），下面可以直接使用这个类型或静态成员。
import java.util.HashSet;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Locale，下面可以直接使用这个类型或静态成员。
import java.util.Locale;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入Objects，下面可以直接使用这个类型或静态成员。
import java.util.Objects;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;

/* 账户主链；注册认证、本人资料、后台管理及用户关联在同一 Service 内完成。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 UserServiceImpl；实现用户业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class UserServiceImpl implements UserService {

    // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
    private final ExecutionLifecycleProvider execution;
    // 习惯列表（habits），使用 HabitLifecycleProvider 类型保存。
    private final HabitLifecycleProvider habits;
    // 目标达标提醒处理器（goalNotices），使用 GoalNotificationProvider 类型保存。
    private final GoalNotificationProvider goalNotices;

    // 访问数据库的 Mapper（mapper），使用 UserMapper 类型保存。
    private final UserMapper mapper;

    // 角色列表（roles），使用 RoleMapper 类型保存。
    private final RoleMapper roles;

    // 用户角色列表（userRoles），使用 UserRoleMapper 类型保存。
    private final UserRoleMapper userRoles;

    // 权限列表（permissions），使用 PermissionMapper 类型保存。
    private final PermissionMapper permissions;

    // 配置（settings），使用 SettingService 类型保存。
    private final SettingService settings;

    // 菜单列表（menus），使用 MenuService 类型保存。
    private final MenuService menus;

    // 登录Logs（loginLogs），使用 LoginLogService 类型保存。
    private final LoginLogService loginLogs;

    // logs，使用 OperationLogService 类型保存。
    private final OperationLogService logs;

    // 密码Encoder（passwordEncoder），使用 PasswordEncoder 类型保存。
    private final PasswordEncoder passwordEncoder;

    // 会话列表（sessions），使用 RedisSessionStore 类型保存。
    private final RedisSessionStore sessions;

    // captchas，使用 RedisCaptchaStore 类型保存。
    private final RedisCaptchaStore captchas;

    // 尝试次数（attempts），使用 RedisAttemptStore 类型保存。
    private final RedisAttemptStore attempts;

    // 凭据处理器（tokens），使用 JwtTokenUtils 类型保存。
    private final JwtTokenUtils tokens;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public UserServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 UserMapper 类型保存。
            UserMapper mapper,
            // 角色列表（roles），使用 RoleMapper 类型保存。
            RoleMapper roles,
            // 用户角色列表（userRoles），使用 UserRoleMapper 类型保存。
            UserRoleMapper userRoles,
            // 权限列表（permissions），使用 PermissionMapper 类型保存。
            PermissionMapper permissions,
            // 配置（settings），使用 SettingService 类型保存。
            SettingService settings,
            // 菜单列表（menus），使用 MenuService 类型保存。
            MenuService menus,
            // 登录Logs（loginLogs），使用 LoginLogService 类型保存。
            LoginLogService loginLogs,
            // logs，使用 OperationLogService 类型保存。
            OperationLogService logs,
            // 密码Encoder（passwordEncoder），使用 PasswordEncoder 类型保存。
            PasswordEncoder passwordEncoder,
            // 会话列表（sessions），使用 RedisSessionStore 类型保存。
            RedisSessionStore sessions,
            // captchas，使用 RedisCaptchaStore 类型保存。
            RedisCaptchaStore captchas,
            // 尝试次数（attempts），使用 RedisAttemptStore 类型保存。
            RedisAttemptStore attempts,
            // 凭据处理器（tokens），使用 JwtTokenUtils 类型保存。
            JwtTokenUtils tokens,
            // 执行计时（execution），使用 ExecutionLifecycleProvider 类型保存。
            ExecutionLifecycleProvider execution,
            // 习惯列表（habits），使用 HabitLifecycleProvider 类型保存。
            HabitLifecycleProvider habits,
            // 目标达标提醒处理器（goalNotices），使用 GoalNotificationProvider 类型保存。
            GoalNotificationProvider goalNotices) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把角色列表（roles）保存到 this.roles。
        this.roles = roles;
        // 把用户角色列表（userRoles）保存到 this.userRoles。
        this.userRoles = userRoles;
        // 把权限列表（permissions）保存到 this.permissions。
        this.permissions = permissions;
        // 把配置（settings）保存到 this.settings。
        this.settings = settings;
        // 把菜单列表（menus）保存到 this.menus。
        this.menus = menus;
        // 把登录Logs（loginLogs）保存到 this.loginLogs。
        this.loginLogs = loginLogs;
        // 把logs保存到 this.logs。
        this.logs = logs;
        // 把密码Encoder（passwordEncoder）保存到 this.passwordEncoder。
        this.passwordEncoder = passwordEncoder;
        // 把把原始密码编码成不可直接还原的密码摘要保存到 this.dummyPasswordHash。
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        // 把会话列表（sessions）保存到 this.sessions。
        this.sessions = sessions;
        // 把captchas保存到 this.captchas。
        this.captchas = captchas;
        // 把尝试次数（attempts）保存到 this.attempts。
        this.attempts = attempts;
        // 把凭据处理器（tokens）保存到 this.tokens。
        this.tokens = tokens;
        // 把执行计时（execution）保存到 this.execution。
        this.execution = execution;
        // 把习惯列表（habits）保存到 this.habits。
        this.habits = habits;
        // 把目标达标提醒处理器（goalNotices）保存到 this.goalNotices。
        this.goalNotices = goalNotices;
    }

    // dummy密码摘要值（dummyPasswordHash），使用 String 类型保存；可为 null，表示尚未提供。
    private final String dummyPasswordHash;

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 加载Security账号（loadSecurityAccount）方法：加载 Security账号（SecurityAccount）；参数负责传入本次处理所需的数据。
    @Override
    // 用户编号（userId），使用 long 类型保存。
    public SecurityAccountData loadSecurityAccount(long userId) {
        // 准备当前记录（row），类型为 UserEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        UserEntity row = mapper.findById(userId);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
            return null;
        }
        // 准备是否启用（enabled），类型为 boolean；初始内容是比较两个值是否相同。
        boolean enabled = Boolean.TRUE.equals(row.getEnabled()) && row.getDeletedAt() == null;
        // 准备角色Codes（roleCodes），类型为 Set<String>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<String> roleCodes = new HashSet<>(roles.findCodesByUserId(userId));
        // 准备生效权限列表（effectivePermissions），类型为 Set<String>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<String> effectivePermissions = new HashSet<>(permissions.findCodesByUserId(userId));
        // 检查条件 com.asterway.common.utils.GrowthAccessUtils.manager(roleCodes) 是否成立；成立时执行下面的分支。
        if (com.asterway.common.utils.GrowthAccessUtils.manager(roleCodes)) {
            // 移除 If。
            effectivePermissions.removeIf(com.asterway.common.utils.GrowthAccessUtils::permission);
        }
        // 把新建 Security账号数据（SecurityAccountData） 实例交还给调用方，本方法到此结束。
        return new SecurityAccountData(
                // 读取 编号（Id） 的值。
                row.getId(),
                // 读取 用户名（Username） 的值。
                row.getUsername(),
                // 读取 密码摘要值（PasswordHash） 的值。
                row.getPasswordHash(),
                // 继续传入或组合是否启用（enabled），完成上面尚未结束的调用或声明。
                enabled,
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getMustChangePassword()),
                // 读取 认证版本号（AuthVersion） 的值。
                row.getAuthVersion(),
                // 继续传入或组合角色Codes（roleCodes），完成上面尚未结束的调用或声明。
                roleCodes,
                // 继续传入或组合表达式 effectivePermissions) 的结果，完成上面尚未结束的调用或声明。
                effectivePermissions);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建Captcha（createCaptcha）方法：创建 Captcha；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 CaptchaCreateDTO 类型保存。
    public CaptchaResultVO createCaptcha(CaptchaCreateDTO dto) {
        // 调用 attempts.limit 完成上限处理。
        attempts.limit("captcha-create", RequestUtils.ipAddress(), 30, 60);
        // 把调用 captchas.create 完成创建处理交还给调用方，本方法到此结束。
        return captchas.create(dto.scene(), dto.clientNonce());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 校验Captcha（verifyCaptcha）方法：调用 verifyCaptcha 完成校验Captcha处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求参数对象（dto），使用 CaptchaVerifyDTO 类型保存。
    public CaptchaTicketVO verifyCaptcha(CaptchaVerifyDTO dto) {
        // 调用 attempts.limit 完成上限处理。
        attempts.limit("captcha-verify", RequestUtils.ipAddress(), 60, 60);
        // 把新建 Captcha本次请求序号（CaptchaTicketVO） 实例交还给调用方，本方法到此结束。
        return new CaptchaTicketVO(
                // 调用 captchas.verify 完成校验处理。
                captchas.verify(dto.challengeId(), dto.clientNonce(), dto.offset()), 60);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // register方法：调用 register 完成register处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 RegistrationDTO 类型保存。
    public UserVO register(RegistrationDTO dto) {
        // 调用 attempts.limit 完成上限处理。
        attempts.limit("register", RequestUtils.ipAddress(), 10, 60);
        // 调用 AccountValidationUtils.username 完成用户名处理。
        AccountValidationUtils.username(dto.username());
        // 调用 AccountValidationUtils.password 完成密码处理。
        AccountValidationUtils.password(dto.password(), dto.confirmPassword());
        // 注册不采集个人资料；昵称先使用用户名，时区暂用 UTC，登录后可自行完善。
        // 准备昵称（nickname），类型为 String；初始内容是调用 AccountValidationUtils.nickname 完成昵称处理。
        String nickname = AccountValidationUtils.nickname(null, dto.username());
        // 准备时区（zone），类型为 String；初始内容是固定状态或编码 UTC（协调世界时）。
        String zone = "UTC";
        // 调用 captchas.consume 完成consume处理。
        captchas.consume(dto.captchaTicket(), "REGISTER", dto.clientNonce());
        // 准备摘要值（hash），类型为 String；初始内容是把原始密码编码成不可直接还原的密码摘要。
        String hash = passwordEncoder.encode(dto.password());
        // 编码后再持有开关共享锁，避免把昂贵计算放在配置锁持有期间。
        // 要求满足 Registration是否展开（RegistrationOpen）。
        settings.requireRegistrationOpen();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 newUser(dto.username 完成用户名处理。
        UserEntity row = newUser(dto.username(), nickname, hash, zone, false, null);
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 调用 userRoles.insert 完成写入新记录处理。
        userRoles.insert(row.getId(), userRole().getId(), null);
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定状态或编码 USER_REGISTERED（用户REGISTERED），完成上面尚未结束的调用或声明。
                "USER_REGISTERED",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("username", dto.username()));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return toVO(mapper.findById(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 登录（login）方法：调用 login 完成登录处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 LoginDTO 类型保存。
    public LoginResultVO login(LoginDTO dto) {
        // 调用 AccountValidationUtils.username 完成用户名处理。
        AccountValidationUtils.username(dto.username());
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 调用 attempts.limit 完成上限处理。
            attempts.limit("login", RequestUtils.ipAddress(), 30, 60);
            // 要求满足 Unlocked。
            attempts.requireUnlocked(dto.username());
            // 调用 captchas.consume 完成consume处理。
            captchas.consume(dto.captchaTicket(), "LOGIN", dto.clientNonce());
        // 错误（error），使用 BusinessException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (BusinessException error) {
            // 把新条目放到列表末尾。
            loginLogs.append(null, dto.username(), false, error.getCode(), null);
            // 中止正常处理并抛出异常：错误（error）；由上层按失败处理。
            throw error;
        }
        // 准备found，类型为 UserEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findByUsername。
        UserEntity found = mapper.findByUsername(dto.username());
        // 准备matches，类型为 boolean；初始内容是根据前面的条件，从两个候选结果中选择一个：passwordEncoder.matches(dto.password(), found == null ? dummyPasswordHash : found.getPasswordHash())。
        boolean matches =
                // 继续传入或组合核对输入与保存的摘要或匹配规则是否一致，完成上面尚未结束的调用或声明。
                passwordEncoder.matches(
                        // 调用 dto.password 完成密码处理。
                        dto.password(),
                        // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：found == null ? dummyPasswordHash : found.getPasswordHash())，完成上面尚未结束的调用或声明。
                        found == null ? dummyPasswordHash : found.getPasswordHash());
        // 检查!matches || found == null，即对应内容是否不存在；成立时执行下面的分支。
        if (!matches || found == null) {
            // 调用 attempts.failedCredentials 完成失败Credentials处理。
            attempts.failedCredentials(dto.username());
            // 把新条目放到列表末尾。
            loginLogs.append(
                    // 继续传入或组合根据前面的条件，从两个候选结果中选择一个：found == null ? null : found.getId()，完成上面尚未结束的调用或声明。
                    found == null ? null : found.getId(),
                    // 调用 dto.username 完成用户名处理。
                    dto.username(),
                    // 继续传入或组合假，表示条件不成立或开关关闭，完成上面尚未结束的调用或声明。
                    false,
                    // 继续传入或组合固定状态或编码 INVALID_CREDENTIALS（不合法CREDENTIALS），完成上面尚未结束的调用或声明。
                    "INVALID_CREDENTIALS",
                    // 继续传入或组合表达式 null) 的结果，完成上面尚未结束的调用或声明。
                    null);
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(401, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        // 准备当前记录（row），类型为 UserEntity；初始内容是按编号锁住数据库记录，防止并发修改互相覆盖。
        UserEntity row = mapper.lockById(found.getId());
        // 检查row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled()) || !Objects.equa…，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null
                // 继续补充同一个判断或备用结果：条件 || row.getDeletedAt() != null 是否成立。
                || row.getDeletedAt() != null
                // 继续补充同一个判断或备用结果：条件 || !Boolean.TRUE.equals(row.getEnabled()) 是否成立。
                || !Boolean.TRUE.equals(row.getEnabled())
                // 继续补充同一个判断或备用结果：条件 || !Objects.equals(row.getPasswordHash(), found.getPasswordHash())) { 是否成立。
                || !Objects.equals(row.getPasswordHash(), found.getPasswordHash())) {
            // 把新条目放到列表末尾。
            loginLogs.append(found.getId(), dto.username(), false, "ACCOUNT_UNAVAILABLE", null);
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(401, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        // 准备账号（account），类型为 null；初始内容是读取 编号（Id） 的值。
        var account = loadSecurityAccount(row.getId());
        // 准备当前会话（session），类型为 null；初始内容是调用 sessions.create 完成创建处理。
        var session = sessions.create(account, RequestUtils.ipAddress(), RequestUtils.userAgent());
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备身份凭据（token），类型为 String；初始内容是调用 tokens.create 完成创建处理。
            String token = tokens.create(session);
            // 把新条目放到列表末尾。
            loginLogs.append(row.getId(), dto.username(), true, "SUCCESS", session.id());
            // 调用 attempts.clearFailures 完成清除Failures处理。
            attempts.clearFailures(dto.username());
            // 把新建 登录结果（LoginResultVO） 实例交还给调用方，本方法到此结束。
            return new LoginResultVO(
                    // 继续传入或组合调用 token, session.id 完成编号处理，完成上面尚未结束的调用或声明。
                    token, session.id(), Instant.ofEpochMilli(session.expiresAt()), toVO(row));
        // 错误（error），使用 RuntimeException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (RuntimeException error) {
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 调用 sessions.revoke 完成revoke处理。
                sessions.revoke(session.id());
            // cleanup错误（cleanupError），使用 RuntimeException 类型保存。
            // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
            } catch (RuntimeException cleanupError) {
                // 调用 error.addSuppressed 完成加入Suppressed处理。
                error.addSuppressed(cleanupError);
            }
            // 中止正常处理并抛出异常：错误（error）；由上层按失败处理。
            throw error;
        }
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前登录用户（me）方法：调用 me 完成当前登录用户处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public CurrentUserVO me() {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:profile:read");
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 调用 applyDueTimeZone 完成apply应完成时间时区处理。
        applyDueTimeZone(row);
        // 把新建 当前用户（CurrentUserVO） 实例交还给调用方，本方法到此结束。
        return new CurrentUserVO(
                // 调用 toVO(row), actor.roles 完成角色列表处理。
                toVO(row), actor.roles(), actor.permissions(), menus.currentTree());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新Me（updateMe）方法：更新 当前登录用户（Me）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    public UserVO updateMe(UserUpdateDTO dto) {
        // 要求满足 权限（Permission）。
        SecurityUtils.requirePermission("identity:profile:update");
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return saveProfile(row, dto, row.getId());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 修改密码（changePassword）方法：调用 changePassword 完成修改密码处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 PasswordUpdateDTO 类型保存。
    public void changePassword(PasswordUpdateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:password:update");
        // 调用 AccountValidationUtils.password 完成密码处理。
        AccountValidationUtils.password(dto.newPassword(), dto.confirmPassword());
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查条件 !passwordEncoder.matches(dto.currentPassword(), row.getPasswordHash()) 是否成立；成立时执行下面的分支。
        if (!passwordEncoder.matches(dto.currentPassword(), row.getPasswordHash())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("原密码错误");
        }
        // 检查条件 passwordEncoder.matches(dto.newPassword(), row.getPasswordHash()) 是否成立；成立时执行下面的分支。
        if (passwordEncoder.matches(dto.newPassword(), row.getPasswordHash())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        // 设置 密码摘要值（PasswordHash） 的值。
        row.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        // 设置 Must修改密码（MustChangePassword） 的值。
        row.setMustChangePassword(false);
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(row.getId(), null);
        // 按条件更新数据库记录，具体 SQL 对应 updatePassword。
        changed(mapper.updatePassword(row));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:password:update'，完成上面尚未结束的调用或声明。
                "identity:password:update",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("passwordChanged", true));
        // 调用 TransactionUtils.afterCommit 完成变化后提交回执处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommit(() -> sessions.revokeUser(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 用户使用的时区（timeZone）方法：调用 timeZone 完成用户使用的时区处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public TimeZoneVO timeZone() {
        // 要求满足 权限（Permission）。
        SecurityUtils.requirePermission("identity:timezone:read");
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 调用 applyDueTimeZone 完成apply应完成时间时区处理。
        applyDueTimeZone(row);
        // 把调用 timeZoneVO 完成时间时区处理交还给调用方，本方法到此结束。
        return timeZoneVO(row);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新时间时区（updateTimeZone）方法：更新 时间时区（TimeZone）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 TimeZoneUpdateDTO 类型保存。
    public TimeZoneVO updateTimeZone(TimeZoneUpdateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:timezone:update");
        // 准备时区（zone），类型为 String；初始内容是调用 AccountValidationUtils.timeZone 完成用户使用的时区处理。
        String zone = AccountValidationUtils.timeZone(dto.timeZone());
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 调用 applyDueTimeZone 完成apply应完成时间时区处理。
        applyDueTimeZone(row);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查zone.equals(row.getPendingTimeZone()) || zone.equals(row.getTimeZone()) && row.getPendingTimeZone() …，即对应内容是否不存在；成立时执行下面的分支。
        if (zone.equals(row.getPendingTimeZone())
                // 继续补充同一个判断或备用结果：|| zone.equals(row.getTimeZone()) && row.getPendingTimeZone() == null) {，即对应内容是否不存在。
                || zone.equals(row.getTimeZone()) && row.getPendingTimeZone() == null) {
            // 把调用 timeZoneVO 完成时间时区处理交还给调用方，本方法到此结束。
            return timeZoneVO(row);
        }
        // 检查条件 zone.equals(row.getTimeZone()) 是否成立；成立时执行下面的分支。
        if (zone.equals(row.getTimeZone())) {
            // 设置 等待处理时间时区（PendingTimeZone） 的值。
            row.setPendingTimeZone(null);
            // 设置 时间时区生效At（TimeZoneEffectiveAt） 的值。
            row.setTimeZoneEffectiveAt(null);
        // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
        } else {
            // 设置 等待处理时间时区（PendingTimeZone） 的值。
            row.setPendingTimeZone(zone);
            // 准备当前时区（currentZone），类型为 ZoneId；初始内容是调用 ZoneId.of 完成of处理。
            ZoneId currentZone = ZoneId.of(row.getTimeZone());
            // 设置 时间时区生效At（TimeZoneEffectiveAt） 的值。
            row.setTimeZoneEffectiveAt(
                    // 调用 LocalDate.now 完成当前时间处理。
                    LocalDate.now(currentZone).plusDays(1).atStartOfDay(currentZone).toInstant());
        }
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 按条件更新数据库记录，具体 SQL 对应 updateTimeZone。
        changed(mapper.updateTimeZone(row));
        // 调用 execution.shortenBoundary 完成shortenBoundary处理。
        execution.shortenBoundary(row.getId(), row.getTimeZoneEffectiveAt());
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:timezone:update'，完成上面尚未结束的调用或声明。
                "identity:timezone:update",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("timeZone", zone));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return timeZoneVO(mapper.findById(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 时间Zones（timeZones）方法：调用 timeZones 完成时间Zones处理；参数负责传入本次处理所需的数据。
    @Override
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<String> timeZones(String keyword, PageQueryDTO query) {
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备筛选（filter），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：keyword == null ? "" : keyword.toLowerCase(Locale.ROOT)。
        String filter = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        // 准备是否可用（available），类型为 List<String>；初始内容是把前面筛选、转换后的结果汇成列表。
        List<String> available =
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                ZoneId.getAvailableZoneIds().stream()
                        // 时区（zone），使用 null 类型保存。
                        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                        .filter(zone -> zone.toLowerCase(Locale.ROOT).contains(filter))
                        // 继续对上一步结果执行调用 sorted 完成sorted处理。
                        .sorted()
                        // 继续对上一步结果执行调用 toList 完成to列表处理。
                        .toList();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 把前面筛选、转换后的结果汇成列表。
                available.stream().skip(offset).limit(query.getSize()).toList(),
                // 调用 available.size 完成每页条数处理。
                available.size(),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 UserQueryDTO 类型保存。
    public PageResultVO<UserVO> page(UserQueryDTO query) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:list");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备keyword，类型为 String；初始内容是读取 Keyword 的值。
        String keyword = literalKeyword(query.getKeyword());
        // 准备普通用户仅（regularOnly），类型为 boolean；初始内容是调用 !actor.superAdmin 完成superAdmin处理。
        boolean regularOnly = !actor.superAdmin();
        // 准备记录列表（rows），类型为 List<UserVO>；初始内容是把前面筛选、转换后的结果汇成列表。
        List<UserVO> rows =
                // 继续传入或组合访问数据库的 Mapper（mapper），完成上面尚未结束的调用或声明。
                mapper
                        // 继续对上一步结果执行查找 当前页（Page）。
                        .findPage(
                                // 继续传入或组合keyword，完成上面尚未结束的调用或声明。
                                keyword,
                                // 读取 是否启用（Enabled） 的值。
                                query.getEnabled(),
                                // 读取 角色（Role） 的值。
                                query.getRole(),
                                // 继续传入或组合普通用户仅（regularOnly），完成上面尚未结束的调用或声明。
                                regularOnly,
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
                mapper.count(keyword, query.getEnabled(), query.getRole(), regularOnly),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 统计（statistics）方法：调用 statistics 完成统计处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public UserStatisticsVO statistics() {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:count");
        // 准备值列表（values），类型为 Map<String, Object>；初始内容是调用 mapper.statistics 完成统计处理。
        Map<String, Object> values = mapper.statistics(!actor.superAdmin());
        // 把新建 用户统计（UserStatisticsVO） 实例交还给调用方，本方法到此结束。
        return new UserStatisticsVO(
                // 调用 number 完成数值处理。
                number(values, "users"),
                // 调用 number 完成数值处理。
                number(values, "administrators"),
                // 调用 number 完成数值处理。
                number(values, "disabled"),
                // 调用 number 完成数值处理。
                number(values, "deleted"));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public UserVO get(long id) {
        // 要求满足 权限（Permission）。
        SecurityUtils.requirePermission("identity:user:read");
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(managed(id, false));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 UserCreateDTO 类型保存。
    public UserVO create(UserCreateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:create");
        // 调用 AccountValidationUtils.username 完成用户名处理。
        AccountValidationUtils.username(dto.username());
        // 调用 AccountValidationUtils.password 完成密码处理。
        AccountValidationUtils.password(dto.initialPassword(), dto.confirmPassword());
        // 准备昵称（nickname），类型为 String；初始内容是调用 AccountValidationUtils.nickname 完成昵称处理。
        String nickname = AccountValidationUtils.nickname(dto.nickname(), dto.username());
        // 准备时区（zone），类型为 String；初始内容是调用 AccountValidationUtils.timeZone 完成用户使用的时区处理。
        String zone = AccountValidationUtils.timeZone(dto.timeZone());
        // 准备摘要值（hash），类型为 String；初始内容是把原始密码编码成不可直接还原的密码摘要。
        String hash = passwordEncoder.encode(dto.initialPassword());
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 newUser(dto.username 完成用户名处理。
        UserEntity row = newUser(dto.username(), nickname, hash, zone, true, actor.userId());
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 调用 userRoles.insert 完成写入新记录处理。
        userRoles.insert(row.getId(), userRole().getId(), actor.userId());
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:user:create'，完成上面尚未结束的调用或声明。
                "identity:user:create",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("username", dto.username()));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return toVO(mapper.findById(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    public UserVO update(long id, UserUpdateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:update");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 把调用 saveProfile(managed(id, true), dto, actor.userId 完成用户编号处理交还给调用方，本方法到此结束。
        return saveProfile(managed(id, true), dto, actor.userId());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long id, long version) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:delete");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 managed 完成managed处理。
        UserEntity row = managed(id, true);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(version, row.getVersion());
        // 调用 protectUser 完成protect用户处理。
        protectUser(row);
        // 设置 是否启用（Enabled） 的值。
        row.setEnabled(false);
        // 设置 删除标记At（DeletedAt） 的值。
        row.setDeletedAt(Instant.now());
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 调用 habits.suspendPersonal 完成suspend个人处理。
        habits.suspendPersonal(id);
        // 调用 goalNotices.suspendNotifications 完成suspend通知列表处理。
        goalNotices.suspendNotifications(id);
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(id, null);
        // 按条件更新数据库记录，具体 SQL 对应 updateStatus。
        changed(mapper.updateStatus(row));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合调用 "identity:user:delete", "USER", Long.toString 完成to文字处理，完成上面尚未结束的调用或声明。
                "identity:user:delete", "USER", Long.toString(id), id, Map.of("enabled", false));
        // 调用 TransactionUtils.afterCommit 完成变化后提交回执处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新状态（updateStatus）方法：更新 状态（Status）；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserStatusDTO 类型保存。
    public UserVO updateStatus(long id, UserStatusDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:status");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 managed 完成managed处理。
        UserEntity row = managed(id, true);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查dto.enabled() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.enabled() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("请选择账户状态");
        }
        // 检查条件 !dto.enabled() 是否成立；成立时执行下面的分支。
        if (!dto.enabled()) {
            // 调用 protectUser 完成protect用户处理。
            protectUser(row);
        }
        // 设置 是否启用（Enabled） 的值。
        row.setEnabled(dto.enabled());
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 !dto.enabled() 是否成立；成立时执行下面的分支。
        if (!dto.enabled()) {
            // 调用 habits.suspendPersonal 完成suspend个人处理。
            habits.suspendPersonal(id);
            // 调用 goalNotices.suspendNotifications 完成suspend通知列表处理。
            goalNotices.suspendNotifications(id);
        }
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(id, null);
        // 按条件更新数据库记录，具体 SQL 对应 updateStatus。
        changed(mapper.updateStatus(row));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:user:status'，完成上面尚未结束的调用或声明。
                "identity:user:status",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合编号（id），完成上面尚未结束的调用或声明。
                id,
                // 调用 Map.of 完成of处理。
                Map.of("enabled", dto.enabled()));
        // 调用 TransactionUtils.afterCommit 完成变化后提交回执处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return toVO(mapper.findById(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // reset密码（resetPassword）方法：调用 resetPassword 完成reset密码处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PasswordResetDTO 类型保存。
    public void resetPassword(long id, PasswordResetDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:user:reset_password");
        // 调用 AccountValidationUtils.password 完成密码处理。
        AccountValidationUtils.password(dto.newPassword(), dto.confirmPassword());
        // 准备摘要值（hash），类型为 String；初始内容是把原始密码编码成不可直接还原的密码摘要。
        String hash = passwordEncoder.encode(dto.newPassword());
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 managed 完成managed处理。
        UserEntity row = managed(id, true);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查条件 roles.findAssignedCodes(id).contains("SUPER_ADMIN") 是否成立；成立时执行下面的分支。
        if (roles.findAssignedCodes(id).contains("SUPER_ADMIN")) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("超级管理员请通过本人改密入口修改密码");
        }
        // 设置 密码摘要值（PasswordHash） 的值。
        row.setPasswordHash(hash);
        // 设置 Must修改密码（MustChangePassword） 的值。
        row.setMustChangePassword(true);
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(id, null);
        // 按条件更新数据库记录，具体 SQL 对应 updatePassword。
        changed(mapper.updatePassword(row));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:user:reset_password'，完成上面尚未结束的调用或声明。
                "identity:user:reset_password",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合编号（id），完成上面尚未结束的调用或声明。
                id,
                // 调用 Map.of 完成of处理。
                Map.of("passwordChanged", true));
        // 调用 TransactionUtils.afterCommit 完成变化后提交回执处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 角色列表（roles）方法：调用 roles 完成角色列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public List<String> roles(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:user:assign_role");
        // 调用 managed 完成managed处理。
        managed(id, false);
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return userRoles.findIds(id).stream().map(Object::toString).toList();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 安排角色列表（assignRoles）方法：调用 assignRoles 完成安排角色列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserRolesDTO 类型保存。
    public UserVO assignRoles(long id, UserRolesDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:user:assign_role");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 managed 完成managed处理。
        UserEntity row = managed(id, true);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 调用 AccountValidationUtils.distinctIds 完成distinct编号列表处理。
        AccountValidationUtils.distinctIds(dto.roleIds());
        // 检查条件 roles.findAssignedCodes(id).contains("SUPER_ADMIN") 是否成立；成立时执行下面的分支。
        if (roles.findAssignedCodes(id).contains("SUPER_ADMIN")) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("不能通过管理接口修改超级管理员身份");
        }
        // 准备allowed，类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<Long> allowed = new HashSet<>();
        // 角色（role），使用 RoleEntity 类型保存。
        // 逐次执行循环：for (RoleEntity role : roles.findAll())；每轮处理一条数据或推进一次检查。
        for (RoleEntity role : roles.findAll()) {
            // 检查条件 Set.of("USER", "ADMIN").contains(role.getCode()) && Boolean.TRUE.equals(role.getEnabled()) 是否成立；成立时执行下面的分支。
            if (Set.of("USER", "ADMIN").contains(role.getCode())
                    // 继续补充同一个判断或备用结果：条件 && Boolean.TRUE.equals(role.getEnabled())) { 是否成立。
                    && Boolean.TRUE.equals(role.getEnabled())) {
                // 加入集合或登记一条待保存记录。
                allowed.add(role.getId());
            }
        }
        // 检查条件 !allowed.containsAll(dto.roleIds()) || !dto.roleIds().contains(userRole().getId()) 是否成立；成立时执行下面的分支。
        if (!allowed.containsAll(dto.roleIds()) || !dto.roleIds().contains(userRole().getId())) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("仅允许保留 USER 并授予或撤销 ADMIN");
        }
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 按条件更新数据库记录，具体 SQL 对应 updateProfile。
        changed(mapper.updateProfile(row));
        // 删除 按所属人（ByOwner）。
        userRoles.deleteByOwner(id);
        // 角色编号（roleId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long roleId : dto.roleIds())；每轮处理一条数据或推进一次检查。
        for (Long roleId : dto.roleIds()) {
            // 调用 userRoles.insert 完成写入新记录处理。
            userRoles.insert(id, roleId, actor.userId());
        }
        // 检查条件 com.asterway.common.utils.GrowthAccessUtils.manager(new HashSet<>(roles.findAssignedCodes(id))) 是否成立；成立时执行下面的分支。
        if (com.asterway.common.utils.GrowthAccessUtils.manager(
                // 继续传入或组合新建 摘要值设置（HashSet） 实例，完成上面尚未结束的调用或声明。
                new HashSet<>(roles.findAssignedCodes(id)))) {
            // 调用 habits.suspendPersonal 完成suspend个人处理。
            habits.suspendPersonal(id);
            // 调用 goalNotices.suspendNotifications 完成suspend通知列表处理。
            goalNotices.suspendNotifications(id);
        }
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(id, null);
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:user:assign_role'，完成上面尚未结束的调用或声明。
                "identity:user:assign_role",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合编号（id），完成上面尚未结束的调用或声明。
                id,
                // 调用 Map.of 完成of处理。
                Map.of("roleIds", dto.roleIds()));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return toVO(mapper.findById(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // my会话列表（mySessions）方法：调用 mySessions 完成my会话列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public List<DeviceVO> mySessions() {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:device:list");
        // 把调用 devices(sessions.list 完成列表处理交还给调用方，本方法到此结束。
        return devices(sessions.list(actor.userId()), false);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 会话列表（sessions）方法：调用 sessions 完成会话列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public PageResultVO<DeviceVO> sessions(PageQueryDTO query) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:session:list");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备记录列表（rows），类型为 List<DeviceVO>；初始内容是调用 devices(sessions.list 完成列表处理。
        List<DeviceVO> rows = devices(sessions.list(null), !actor.superAdmin());
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 把前面筛选、转换后的结果汇成列表。
                rows.stream().skip(offset).limit(query.getSize()).toList(),
                // 调用 rows.size 完成每页条数处理。
                rows.size(),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // revokeMy会话（revokeMySession）方法：调用 revokeMySession 完成revokeMy会话处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public void revokeMySession(String id) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:device:revoke");
        // 调用 lockOwn 完成加锁Own处理。
        lockOwn();
        // 准备当前会话（session），类型为 null；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        var session = sessions.get(id);
        // 检查session == null || !session.userId().equals(Long.toString(actor.userId()))，即对应内容是否不存在；成立时执行下面的分支。
        if (session == null || !session.userId().equals(Long.toString(actor.userId()))) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(actor.userId(), id);
        // 调用 TransactionUtils.afterCommitRequired 完成变化后提交回执必填处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommitRequired(() -> sessions.revoke(id));
        // 把新条目放到列表末尾。
        logs.append("identity:device:revoke", "SESSION", id, actor.userId(), Map.of());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 退出登录（logout）方法：调用 logout 完成退出登录处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void logout() {
        // 准备actor，类型为 null；初始内容是调用 SecurityUtils.current 完成当前处理。
        var actor = SecurityUtils.current();
        // 调用 lockOwn 完成加锁Own处理。
        lockOwn();
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(actor.userId(), actor.sessionId());
        // 调用 TransactionUtils.afterCommitRequired 完成变化后提交回执必填处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommitRequired(() -> sessions.revoke(actor.sessionId()));
        // 把新条目放到列表末尾。
        logs.append("USER_LOGOUT", "SESSION", actor.sessionId(), actor.userId(), Map.of());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 退出登录全部（logoutAll）方法：调用 logoutAll 完成退出登录全部处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public void logoutAll() {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:device:revoke");
        // 准备当前记录（row），类型为 UserEntity；初始内容是调用 lockOwn 完成加锁Own处理。
        UserEntity row = lockOwn();
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(row.getId(), null);
        // 调用 changed(mapper.invalidateSessions 完成invalidate会话列表处理。
        changed(mapper.invalidateSessions(row.getId(), row.getVersion(), actor.userId()));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:device:revoke'，完成上面尚未结束的调用或声明。
                "identity:device:revoke",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("version", row.getVersion()));
        // 调用 TransactionUtils.afterCommit 完成变化后提交回执处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommit(() -> sessions.revokeUser(actor.userId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // revoke会话（revokeSession）方法：调用 revokeSession 完成revoke会话处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public void revokeSession(String id) {
        // 准备actor，类型为 null；初始内容是要求满足 权限（Permission）。
        var actor = SecurityUtils.requirePermission("identity:session:revoke");
        // 准备当前会话（session），类型为 null；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        var session = sessions.get(id);
        // 检查session == null，即对应内容是否不存在；成立时执行下面的分支。
        if (session == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 UserEntity；初始内容是解析 可为空的大整数（Long）。
        UserEntity row = managed(Long.parseLong(session.userId()), true);
        // 调用 execution.pauseLogin 完成暂停登录处理。
        execution.pauseLogin(row.getId(), id);
        // 调用 TransactionUtils.afterCommitRequired 完成变化后提交回执必填处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        TransactionUtils.afterCommitRequired(() -> sessions.revoke(id));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:session:revoke'，完成上面尚未结束的调用或声明。
                "identity:session:revoke",
                // 继续传入或组合固定状态或编码 SESSION（当前会话），完成上面尚未结束的调用或声明。
                "SESSION",
                // 继续传入或组合编号（id），完成上面尚未结束的调用或声明。
                id,
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("version", row.getVersion()));
    }

    // managed方法：调用 managed 完成managed处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 加锁（lock），使用 boolean 类型保存。
    private UserEntity managed(long id, boolean lock) {
        // 准备当前记录（row），类型为 UserEntity；初始内容是根据前面的条件，从两个候选结果中选择一个：lock ? mapper.lockById(id) : mapper.findById(id)。
        UserEntity row = lock ? mapper.lockById(id) : mapper.findById(id);
        // 检查row == null || row.getDeletedAt() != null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null || row.getDeletedAt() != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 检查条件 !SecurityUtils.current().superAdmin() && roles.findAssignedCodes(id).stream().anyMatch((code)->code.equals("ADMIN") || code.equals("SUPER_ADMIN")… 是否成立；成立时执行下面的分支。
        if (!SecurityUtils.current().superAdmin()
                // 继续补充同一个判断或备用结果：条件 && roles.findAssignedCodes(id).stream() 是否成立。
                && roles.findAssignedCodes(id).stream()
                        // 编码（code），使用 null 类型保存。
                        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                        .anyMatch(code -> code.equals("ADMIN") || code.equals("SUPER_ADMIN"))) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 加锁Own（lockOwn）方法：调用 lockOwn 完成加锁Own处理；参数负责传入本次处理所需的数据。
    private UserEntity lockOwn() {
        // 准备当前记录（row），类型为 UserEntity；初始内容是按编号锁住数据库记录，防止并发修改互相覆盖。
        UserEntity row = mapper.lockById(SecurityUtils.current().userId());
        // 检查当前记录版本是否符合请求要求：row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled()) || row.getAuthVersion() != SecurityUtils.current().authVersio…；成立时执行下面的分支。
        if (row == null
                // 继续补充同一个判断或备用结果：条件 || row.getDeletedAt() != null 是否成立。
                || row.getDeletedAt() != null
                // 继续补充同一个判断或备用结果：条件 || !Boolean.TRUE.equals(row.getEnabled()) 是否成立。
                || !Boolean.TRUE.equals(row.getEnabled())
                // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| row.getAuthVersion() != SecurityUtils.current().authVersion()) {。
                || row.getAuthVersion() != SecurityUtils.current().authVersion()) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // protect用户（protectUser）方法：调用 protectUser 完成protect用户处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 UserEntity 类型保存。
    private void protectUser(UserEntity row) {
        // 检查条件 row.getId().equals(SecurityUtils.current().userId()) 是否成立；成立时执行下面的分支。
        if (row.getId().equals(SecurityUtils.current().userId())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("不能禁用或删除当前登录账户");
        }
        // 检查条件 roles.findAssignedCodes(row.getId()).contains("SUPER_ADMIN") && Boolean.TRUE.equals(row.getEnabled()) && roles.countEnabledSuperAdmins() <= 1 是否成立；成立时执行下面的分支。
        if (roles.findAssignedCodes(row.getId()).contains("SUPER_ADMIN")
                // 继续补充同一个判断或备用结果：条件 && Boolean.TRUE.equals(row.getEnabled()) 是否成立。
                && Boolean.TRUE.equals(row.getEnabled())
                // 继续补充同一个判断或备用结果：条件 && roles.countEnabledSuperAdmins() <= 1) { 是否成立。
                && roles.countEnabledSuperAdmins() <= 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("必须保留至少一个可用超级管理员");
        }
    }

    // 保存个人资料（saveProfile）方法：保存 个人资料（Profile）；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 UserEntity 类型保存。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    // actor编号（actorId），使用 long 类型保存。
    private UserVO saveProfile(UserEntity row, UserUpdateDTO dto, long actorId) {
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查dto.username() == null && dto.nickname() == null，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.username() == null && dto.nickname() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("至少提供一个需要修改的资料字段");
        }
        // 检查条件 dto.username() != null 是否成立；成立时执行下面的分支。
        if (dto.username() != null) {
            // 调用 AccountValidationUtils.username 完成用户名处理。
            AccountValidationUtils.username(dto.username());
            // 设置 用户名（Username） 的值。
            row.setUsername(dto.username());
        }
        // 检查条件 dto.nickname() != null 是否成立；成立时执行下面的分支。
        if (dto.nickname() != null) {
            // 设置 昵称（Nickname） 的值。
            row.setNickname(AccountValidationUtils.nickname(dto.nickname(), null));
        }
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actorId);
        // 按条件更新数据库记录，具体 SQL 对应 updateProfile。
        changed(mapper.updateProfile(row));
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:profile:update'，完成上面尚未结束的调用或声明。
                "identity:profile:update",
                // 继续传入或组合固定状态或编码 USER（用户），完成上面尚未结束的调用或声明。
                "USER",
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编号（Id） 的值。
                row.getId(),
                // 调用 Map.of 完成of处理。
                Map.of("username", row.getUsername(), "nickname", row.getNickname()));
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findById交还给调用方，本方法到此结束。
        return toVO(mapper.findById(row.getId()));
    }

    // apply应完成时间时区（applyDueTimeZone）方法：调用 applyDueTimeZone 完成apply应完成时间时区处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 UserEntity 类型保存。
    private void applyDueTimeZone(UserEntity row) {
        // 检查条件 row.getPendingTimeZone() != null && !row.getTimeZoneEffectiveAt().isAfter(Instant.now()) 是否成立；成立时执行下面的分支。
        if (row.getPendingTimeZone() != null
                // 继续补充同一个判断或备用结果：条件 && !row.getTimeZoneEffectiveAt().isAfter(Instant.now())) { 是否成立。
                && !row.getTimeZoneEffectiveAt().isAfter(Instant.now())) {
            // 调用 execution.shortenBoundary 完成shortenBoundary处理。
            execution.shortenBoundary(row.getId(), row.getTimeZoneEffectiveAt());
            // 调用 habits.sealBeforeZoneSwitch 完成seal变化前时区Switch处理。
            habits.sealBeforeZoneSwitch(
                    // 读取 编号（Id） 的值。
                    row.getId(), row.getTimeZoneEffectiveAt(), row.getTimeZone());
            // 设置 时间时区（TimeZone） 的值。
            row.setTimeZone(row.getPendingTimeZone());
            // 设置 等待处理时间时区（PendingTimeZone） 的值。
            row.setPendingTimeZone(null);
            // 设置 时间时区生效At（TimeZoneEffectiveAt） 的值。
            row.setTimeZoneEffectiveAt(null);
            // 设置 更新时按（UpdatedBy） 的值。
            row.setUpdatedBy(row.getId());
            // 按条件更新数据库记录，具体 SQL 对应 updateTimeZone。
            changed(mapper.updateTimeZone(row));
            // 设置 版本号（Version） 的值。
            row.setVersion(row.getVersion() + 1);
        }
    }

    // 用户角色（userRole）方法：调用 userRole 完成用户角色处理；参数负责传入本次处理所需的数据。
    private RoleEntity userRole() {
        // 把逐项处理集合中的数据，继续做筛选、转换或汇总交还给调用方，本方法到此结束。
        return roles.findAll().stream()
                // 继续对上一步结果执行只保留符合条件的条目。
                .filter(
                        // 角色（role），使用 null 类型保存。
                        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                        role ->
                                // 读取 编码（Code） 的值。
                                role.getCode().equals("USER")
                                        // 继续补充同一个判断或备用结果：条件 && Boolean.TRUE.equals(role.getEnabled())) 是否成立。
                                        && Boolean.TRUE.equals(role.getEnabled()))
                // 继续对上一步结果执行查找 首次（First）。
                .findFirst()
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .orElseThrow(() -> new BusinessException(503, "ROLE_MISSING", "基础角色未初始化"));
    }

    // 新用户（newUser）方法：调用 newUser 完成新用户处理；参数负责传入本次处理所需的数据。
    private UserEntity newUser(
            // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
            String username,
            // 昵称（nickname），使用 String 类型保存；可为 null，表示尚未提供。
            String nickname,
            // 摘要值（hash），使用 String 类型保存；可为 null，表示尚未提供。
            String hash,
            // 时区（zone），使用 String 类型保存；可为 null，表示尚未提供。
            String zone,
            // must修改（mustChange），使用 boolean 类型保存。
            boolean mustChange,
            // creator，使用 Long 类型保存；可为 null，表示尚未提供。
            Long creator) {
        // 准备当前记录（row），类型为 UserEntity；初始内容是新建 用户（UserEntity） 实例。
        UserEntity row = new UserEntity();
        // 设置 用户名（Username） 的值。
        row.setUsername(username);
        // 设置 昵称（Nickname） 的值。
        row.setNickname(nickname);
        // 设置 密码摘要值（PasswordHash） 的值。
        row.setPasswordHash(hash);
        // 设置 时间时区（TimeZone） 的值。
        row.setTimeZone(zone);
        // 设置 Must修改密码（MustChangePassword） 的值。
        row.setMustChangePassword(mustChange);
        // 设置 创建时按（CreatedBy） 的值。
        row.setCreatedBy(creator);
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(creator);
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // devices方法：调用 devices 完成devices处理；参数负责传入本次处理所需的数据。
    // 页面是否仍处于活动状态（active），使用 List<LoginSessionData> 类型保存。
    // 普通用户仅（regularOnly），使用 boolean 类型保存。
    private List<DeviceVO> devices(List<LoginSessionData> active, boolean regularOnly) {
        // 准备actor，类型为 null；初始内容是调用 SecurityUtils.current 完成当前处理。
        var actor = SecurityUtils.current();
        // 准备缓存（cache），类型为 Map<Long, UserEntity>；初始内容是新建 摘要值对应表（HashMap） 实例。
        Map<Long, UserEntity> cache = new HashMap<>();
        // 准备结果（result），类型为 List<DeviceVO>；初始内容是一个空列表，用来保存后续多条数据。
        List<DeviceVO> result = new ArrayList<>();
        // 当前会话（session），使用 null 类型保存。
        // 逐次执行循环：for (/*missing* / session : active)；每轮处理一条数据或推进一次检查。
        for (var session : active) {
            // 准备用户编号（userId），类型为 long；初始内容是把输入转换成数值，供比较、计算或按编号查询。
            long userId = Long.parseLong(session.userId());
            // 准备当前记录（row），类型为 UserEntity；初始内容是调用 cache.computeIfAbsent 完成computeIfAbsent处理。
            UserEntity row = cache.computeIfAbsent(userId, mapper::findById);
            // 检查当前记录版本是否符合请求要求：row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled()) || row.getAuthVersion() != session.authVersion()；成立时执行下面的分支。
            if (row == null
                    // 继续补充同一个判断或备用结果：条件 || row.getDeletedAt() != null 是否成立。
                    || row.getDeletedAt() != null
                    // 继续补充同一个判断或备用结果：条件 || !Boolean.TRUE.equals(row.getEnabled()) 是否成立。
                    || !Boolean.TRUE.equals(row.getEnabled())
                    // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| row.getAuthVersion() != session.authVersion()) {。
                    || row.getAuthVersion() != session.authVersion()) {
                // 继续传入或组合continue，完成上面尚未结束的调用或声明。
                continue;
            }
            // 检查条件 regularOnly && roles.findAssignedCodes(userId).stream().anyMatch((code)->code.equals("ADMIN") || code.equals("SUPER_ADMIN")) 是否成立；成立时执行下面的分支。
            if (regularOnly
                    // 继续补充同一个判断或备用结果：条件 && roles.findAssignedCodes(userId).stream() 是否成立。
                    && roles.findAssignedCodes(userId).stream()
                            // 编码（code），使用 null 类型保存。
                            // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                            .anyMatch(code -> code.equals("ADMIN") || code.equals("SUPER_ADMIN"))) {
                // 继续传入或组合continue，完成上面尚未结束的调用或声明。
                continue;
            }
            // 加入集合或登记一条待保存记录。
            result.add(
                    // 继续传入或组合新建 设备（DeviceVO） 实例，完成上面尚未结束的调用或声明。
                    new DeviceVO(
                            // 调用 session.id 完成编号处理。
                            session.id(),
                            // 调用 session.userId 完成用户编号处理。
                            session.userId(),
                            // 读取 用户名（Username） 的值。
                            row.getUsername(),
                            // 调用 session.deviceLabel 完成设备显示名称处理。
                            session.deviceLabel(),
                            // 调用 session.ipAddress 完成ipAddress处理。
                            session.ipAddress(),
                            // 调用 session.userAgent 完成用户成长助手处理。
                            session.userAgent(),
                            // 调用 Instant.ofEpochMilli 完成ofEpochMilli处理。
                            Instant.ofEpochMilli(session.createdAt()),
                            // 调用 Instant.ofEpochMilli 完成ofEpochMilli处理。
                            Instant.ofEpochMilli(session.lastActiveAt()),
                            // 调用 Instant.ofEpochMilli 完成ofEpochMilli处理。
                            Instant.ofEpochMilli(session.expiresAt()),
                            // 调用 actor.sessionId 完成会话编号处理。
                            actor.sessionId().equals(session.id())));
        }
        // 把结果（result）交还给调用方，本方法到此结束。
        return result;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 UserEntity 类型保存。
    private UserVO toVO(UserEntity row) {
        // 把新建 用户（UserVO） 实例交还给调用方，本方法到此结束。
        return new UserVO(
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 用户名（Username） 的值。
                row.getUsername(),
                // 读取 昵称（Nickname） 的值。
                row.getNickname(),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getEnabled()),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getMustChangePassword()),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 读取 时间时区（TimeZone） 的值。
                row.getTimeZone(),
                // 读取 等待处理时间时区（PendingTimeZone） 的值。
                row.getPendingTimeZone(),
                // 读取 时间时区生效At（TimeZoneEffectiveAt） 的值。
                row.getTimeZoneEffectiveAt(),
                // 读取 创建时At（CreatedAt） 的值。
                row.getCreatedAt(),
                // 查找 AssignedCodes。
                roles.findAssignedCodes(row.getId()),
                // 把前面筛选、转换后的结果汇成列表。
                userRoles.findIds(row.getId()).stream().map(Object::toString).toList());
    }

    // 时间时区（timeZoneVO）方法：调用 timeZoneVO 完成时间时区处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 UserEntity 类型保存。
    private TimeZoneVO timeZoneVO(UserEntity row) {
        // 把新建 时间时区（TimeZoneVO） 实例交还给调用方，本方法到此结束。
        return new TimeZoneVO(
                // 读取 时间时区（TimeZone） 的值。
                row.getTimeZone(),
                // 读取 等待处理时间时区（PendingTimeZone） 的值。
                row.getPendingTimeZone(),
                // 读取 时间时区生效At（TimeZoneEffectiveAt） 的值。
                row.getTimeZoneEffectiveAt(),
                // 读取 版本号（Version） 的值。
                row.getVersion());
    }

    // 已修改（changed）方法：调用 changed 完成已修改处理；参数负责传入本次处理所需的数据。
    // affected，使用 int 类型保存。
    private void changed(int affected) {
        // 检查条件 affected != 1 是否成立；成立时执行下面的分支。
        if (affected != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("数据已变化，请刷新后重试");
        }
    }

    // 数值（number）方法：调用 number 完成数值处理；参数负责传入本次处理所需的数据。
    // 值列表（values），使用 Map<String, Object> 类型保存。
    // 键（key），使用 String 类型保存；可为 null，表示尚未提供。
    private long number(Map<String, Object> values, String key) {
        // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值交还给调用方，本方法到此结束。
        return ((Number) values.get(key)).longValue();
    }

    // literalKeyword方法：调用 literalKeyword 完成literalKeyword处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    private String literalKeyword(String value) {
        // 把根据前面的条件，从两个候选结果中选择一个：value == null ? null : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")交还给调用方，本方法到此结束。
        return value == null
                // 继续补充同一个判断或备用结果：条件 ? null 是否成立。
                ? null
                // 继续补充同一个判断或备用结果：条件 : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); 是否成立。
                : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
