/*
 * 规定用户业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入Captcha结果（CaptchaResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.CaptchaResultVO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入账号Security提供器（AccountSecurityProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountSecurityProvider;
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
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 用户表的主业务链：注册认证、本人账户、后台用户及其角色与登录会话。 */
// 定义接口 UserService；规定用户业务服务可供其他代码调用的业务方法。
public interface UserService extends AccountSecurityProvider {

    /* 创建场景绑定挑战，并限制来源请求频率。 */
    // 创建Captcha（createCaptcha）方法：创建 Captcha；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 CaptchaCreateDTO 类型保存。
    CaptchaResultVO createCaptcha(CaptchaCreateDTO dto);

    /* 验证后返回一次性票据，不提前进行账户认证。 */
    // 校验Captcha（verifyCaptcha）方法：调用 verifyCaptcha 完成校验Captcha处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 CaptchaVerifyDTO 类型保存。
    CaptchaTicketVO verifyCaptcha(CaptchaVerifyDTO dto);

    /* 注册事务中检查开关并原子建立 USER 账户及基础关联。 */
    // register方法：调用 register 完成register处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 RegistrationDTO 类型保存。
    UserVO register(RegistrationDTO dto);

    /* 验证码与凭据验证成功后创建独立 Redis 会话和 JWT。 */
    // 登录（login）方法：调用 login 完成登录处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 LoginDTO 类型保存。
    LoginResultVO login(LoginDTO dto);

    /* 读取当前资料和最新授权，应用已到期时区变更。 */
    // 当前登录用户（me）方法：调用 me 完成当前登录用户处理；参数负责传入本次处理所需的数据。
    CurrentUserVO me();

    /* 只更新本人资料，不接受角色字段。 */
    // 更新Me（updateMe）方法：更新 当前登录用户（Me）；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    UserVO updateMe(UserUpdateDTO dto);

    /* 验证旧密码和版本，成功后递增安全版本并撤销全部旧会话。 */
    // 修改密码（changePassword）方法：调用 changePassword 完成修改密码处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 PasswordUpdateDTO 类型保存。
    void changePassword(PasswordUpdateDTO dto);

    /* 读取当前与待生效时区。 */
    // 用户使用的时区（timeZone）方法：调用 timeZone 完成用户使用的时区处理；参数负责传入本次处理所需的数据。
    TimeZoneVO timeZone();

    /* 按旧时区次日午夜切换，重复目标保持原生效时刻。 */
    // 更新时间时区（updateTimeZone）方法：更新 时间时区（TimeZone）；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 TimeZoneUpdateDTO 类型保存。
    TimeZoneVO updateTimeZone(TimeZoneUpdateDTO dto);

    /* 分页返回有效 IANA 时区。 */
    // 时间Zones（timeZones）方法：调用 timeZones 完成时间Zones处理；参数负责传入本次处理所需的数据。
    // keyword，使用 String 类型保存；可为 null，表示尚未提供。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<String> timeZones(String keyword, PageQueryDTO query);

    /* 按操作者范围查询未删除用户。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 UserQueryDTO 类型保存。
    PageResultVO<UserVO> page(UserQueryDTO query);

    /* 统计同一管理范围内用户状态。 */
    // 统计（statistics）方法：调用 statistics 完成统计处理；参数负责传入本次处理所需的数据。
    UserStatisticsVO statistics();

    /* 只读取管理范围内账户。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    UserVO get(long id);

    /* 管理员只能创建普通用户，要求初始密码首次修改。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 UserCreateDTO 类型保存。
    UserVO create(UserCreateDTO dto);

    /* 锁定并核验目标管理范围与旧版本后更新。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    UserVO update(long id, UserUpdateDTO dto);

    /* 逻辑删除、保护当前账户和最后一个超管，并使旧会话失效。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);

    /* 启停账户，停用时撤销全部会话。 */
    // 更新状态（updateStatus）方法：更新 状态（Status）；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserStatusDTO 类型保存。
    UserVO updateStatus(long id, UserStatusDTO dto);

    /* 重置管理范围内密码，不暴露原密码。 */
    // reset密码（resetPassword）方法：调用 resetPassword 完成reset密码处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PasswordResetDTO 类型保存。
    void resetPassword(long id, PasswordResetDTO dto);

    /* 仅超管读取目标角色集合。 */
    // 角色列表（roles）方法：调用 roles 完成角色列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    List<String> roles(long id);

    /* 仅超管调整 ADMIN 附加身份，保留 USER，禁止改变 SUPER_ADMIN。 */
    // 安排角色列表（assignRoles）方法：调用 assignRoles 完成安排角色列表处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserRolesDTO 类型保存。
    UserVO assignRoles(long id, UserRolesDTO dto);

    /* 列出本人有效登录设备，不续期。 */
    // my会话列表（mySessions）方法：调用 mySessions 完成my会话列表处理；参数负责传入本次处理所需的数据。
    List<DeviceVO> mySessions();

    /* 按管理范围列出有效会话，不续期。 */
    // 会话列表（sessions）方法：调用 sessions 完成会话列表处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<DeviceVO> sessions(PageQueryDTO query);

    /* 仅退出本人指定设备。 */
    // revokeMy会话（revokeMySession）方法：调用 revokeMySession 完成revokeMy会话处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    void revokeMySession(String id);

    /* 退出当前设备。 */
    // 退出登录（logout）方法：调用 logout 完成退出登录处理；参数负责传入本次处理所需的数据。
    void logout();

    /* 退出全部设备，同时提升数据库安全版本。 */
    // 退出登录全部（logoutAll）方法：调用 logoutAll 完成退出登录全部处理；参数负责传入本次处理所需的数据。
    void logoutAll();

    /* 按管理范围强退指定会话。 */
    // revoke会话（revokeSession）方法：调用 revokeSession 完成revoke会话处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    void revokeSession(String id);
}
