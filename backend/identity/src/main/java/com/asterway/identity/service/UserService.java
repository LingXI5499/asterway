package com.asterway.identity.service;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.CaptchaResultVO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.AccountSecurityProvider;
import com.asterway.identity.dto.CaptchaCreateDTO;
import com.asterway.identity.dto.CaptchaVerifyDTO;
import com.asterway.identity.dto.LoginDTO;
import com.asterway.identity.dto.PasswordResetDTO;
import com.asterway.identity.dto.PasswordUpdateDTO;
import com.asterway.identity.dto.RegistrationDTO;
import com.asterway.identity.dto.TimeZoneUpdateDTO;
import com.asterway.identity.dto.UserCreateDTO;
import com.asterway.identity.dto.UserQueryDTO;
import com.asterway.identity.dto.UserRolesDTO;
import com.asterway.identity.dto.UserStatusDTO;
import com.asterway.identity.dto.UserUpdateDTO;
import com.asterway.identity.vo.CaptchaTicketVO;
import com.asterway.identity.vo.CurrentUserVO;
import com.asterway.identity.vo.DeviceVO;
import com.asterway.identity.vo.LoginResultVO;
import com.asterway.identity.vo.TimeZoneVO;
import com.asterway.identity.vo.UserStatisticsVO;
import com.asterway.identity.vo.UserVO;
import java.util.List;

/** 用户表的主业务链：注册认证、本人账户、后台用户及其角色与登录会话。 */
public interface UserService extends AccountSecurityProvider {

    /** 创建场景绑定挑战，并限制来源请求频率。 */
    CaptchaResultVO createCaptcha(CaptchaCreateDTO dto);

    /** 验证后返回一次性票据，不提前进行账户认证。 */
    CaptchaTicketVO verifyCaptcha(CaptchaVerifyDTO dto);

    /** 注册事务中检查开关并原子建立 USER 账户及基础关联。 */
    UserVO register(RegistrationDTO dto);

    /** 验证码与凭据验证成功后创建独立 Redis 会话和 JWT。 */
    LoginResultVO login(LoginDTO dto);

    /** 读取当前资料和最新授权，应用已到期时区变更。 */
    CurrentUserVO me();

    /** 只更新本人资料，不接受角色字段。 */
    UserVO updateMe(UserUpdateDTO dto);

    /** 验证旧密码和版本，成功后递增安全版本并撤销全部旧会话。 */
    void changePassword(PasswordUpdateDTO dto);

    /** 读取当前与待生效时区。 */
    TimeZoneVO timeZone();

    /** 按旧时区次日午夜切换，重复目标保持原生效时刻。 */
    TimeZoneVO updateTimeZone(TimeZoneUpdateDTO dto);

    /** 分页返回有效 IANA 时区。 */
    PageResultVO<String> timeZones(String keyword, PageQueryDTO query);

    /** 按操作者范围查询未删除用户。 */
    PageResultVO<UserVO> page(UserQueryDTO query);

    /** 统计同一管理范围内用户状态。 */
    UserStatisticsVO statistics();

    /** 只读取管理范围内账户。 */
    UserVO get(long id);

    /** 管理员只能创建普通用户，要求初始密码首次修改。 */
    UserVO create(UserCreateDTO dto);

    /** 锁定并核验目标管理范围与旧版本后更新。 */
    UserVO update(long id, UserUpdateDTO dto);

    /** 逻辑删除、保护当前账户和最后一个超管，并使旧会话失效。 */
    void delete(long id, long version);

    /** 启停账户，停用时撤销全部会话。 */
    UserVO updateStatus(long id, UserStatusDTO dto);

    /** 重置管理范围内密码，不暴露原密码。 */
    void resetPassword(long id, PasswordResetDTO dto);

    /** 仅超管读取目标角色集合。 */
    List<String> roles(long id);

    /** 仅超管调整 ADMIN 附加身份，保留 USER，禁止改变 SUPER_ADMIN。 */
    UserVO assignRoles(long id, UserRolesDTO dto);

    /** 列出本人有效登录设备，不续期。 */
    List<DeviceVO> mySessions();

    /** 按管理范围列出有效会话，不续期。 */
    PageResultVO<DeviceVO> sessions(PageQueryDTO query);

    /** 仅退出本人指定设备。 */
    void revokeMySession(String id);

    /** 退出当前设备。 */
    void logout();

    /** 退出全部设备，同时提升数据库安全版本。 */
    void logoutAll();

    /** 按管理范围强退指定会话。 */
    void revokeSession(String id);
}
