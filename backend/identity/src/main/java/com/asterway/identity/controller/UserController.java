package com.asterway.identity.controller;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.CaptchaResultVO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.result.ResultVO;
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
import com.asterway.identity.service.UserService;
import com.asterway.identity.vo.CaptchaTicketVO;
import com.asterway.identity.vo.CurrentUserVO;
import com.asterway.identity.vo.DeviceVO;
import com.asterway.identity.vo.LoginResultVO;
import com.asterway.identity.vo.TimeZoneVO;
import com.asterway.identity.vo.UserStatisticsVO;
import com.asterway.identity.vo.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 用户主链的 HTTP 入口；Controller 只校验接入参数并调用 Service 接口。 */
@RestController
@Validated
@RequestMapping("/asterway")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    /** 创建场景绑定拼图。 */
    @PostMapping("/captchas")
    public ResultVO<CaptchaResultVO> createCaptcha(@Valid @RequestBody CaptchaCreateDTO dto) {
        return ResultVO.success(service.createCaptcha(dto));
    }

    /** 验证拼图并取得一次性凭据。 */
    @PostMapping("/captcha-verifications")
    public ResultVO<CaptchaTicketVO> verifyCaptcha(@Valid @RequestBody CaptchaVerifyDTO dto) {
        return ResultVO.success(service.verifyCaptcha(dto));
    }

    /** 公开注册普通用户，成功后仍需登录。 */
    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<UserVO> register(@Valid @RequestBody RegistrationDTO dto) {
        return ResultVO.success(service.register(dto));
    }

    /** 创建独立登录会话。 */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultVO<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return ResultVO.success(service.login(dto));
    }

    /** 读取当前账户与授权菜单。 */
    @GetMapping("/me")
    @PreAuthorize("@access.hasPermission('identity:profile:read')")
    public ResultVO<CurrentUserVO> me() {
        return ResultVO.success(service.me());
    }

    /** 修改本人用户名或昵称。 */
    @PatchMapping("/me")
    @PreAuthorize("@access.hasPermission('identity:profile:update')")
    public ResultVO<UserVO> updateMe(@Valid @RequestBody UserUpdateDTO dto) {
        return ResultVO.success(service.updateMe(dto));
    }

    /** 校验原密码后修改密码，成功退出全部旧设备。 */
    @PutMapping("/me/password")
    @PreAuthorize("@access.hasPermission('identity:password:update')")
    public ResultVO<Void> changePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        service.changePassword(dto);
        return ResultVO.success(null);
    }

    /** 读取本人时区状态。 */
    @GetMapping("/me/time-zone")
    @PreAuthorize("@access.hasPermission('identity:timezone:read')")
    public ResultVO<TimeZoneVO> timeZone() {
        return ResultVO.success(service.timeZone());
    }

    /** 保存设备时区，按旧时区日边界生效。 */
    @PutMapping("/me/time-zone")
    @PreAuthorize("@access.hasPermission('identity:timezone:update')")
    public ResultVO<TimeZoneVO> updateTimeZone(@Valid @RequestBody TimeZoneUpdateDTO dto) {
        return ResultVO.success(service.updateTimeZone(dto));
    }

    /** 查询可用 IANA 时区。 */
    @GetMapping("/time-zones")
    public ResultVO<PageResultVO<String>> timeZones(
            @RequestParam(required = false) @Size(max = 128) String keyword,
            @Valid @ModelAttribute PageQueryDTO query
    ) {
        return ResultVO.success(service.timeZones(keyword, query));
    }

    /** 分页查询管理范围内用户。 */
    @GetMapping("/users")
    @PreAuthorize("@access.hasPermission('identity:user:list')")
    public ResultVO<PageResultVO<UserVO>> page(@Valid @ModelAttribute UserQueryDTO query) {
        return ResultVO.success(service.page(query));
    }

    /** 查询管理范围内账户数量。 */
    @GetMapping("/user-statistics")
    @PreAuthorize("@access.hasPermission('identity:user:count')")
    public ResultVO<UserStatisticsVO> statistics() {
        return ResultVO.success(service.statistics());
    }

    /** 查询管理范围内用户详情。 */
    @GetMapping("/users/{id}")
    @PreAuthorize("@access.hasPermission('identity:user:read')")
    public ResultVO<UserVO> get(@PathVariable @Positive long id) {
        return ResultVO.success(service.get(id));
    }

    /** 新增普通账户，不允许直接指定管理身份。 */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.hasPermission('identity:user:create')")
    public ResultVO<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        return ResultVO.success(service.create(dto));
    }

    /** 修改同一用户资料。 */
    @PatchMapping("/users/{id}")
    @PreAuthorize("@access.hasPermission('identity:user:update')")
    public ResultVO<UserVO> update(@PathVariable @Positive long id, @Valid @RequestBody UserUpdateDTO dto) {
        return ResultVO.success(service.update(id, dto));
    }

    /** 逻辑删除账户，保留审计与历史身份。 */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("@access.hasPermission('identity:user:delete')")
    public ResultVO<Void> delete(@PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        service.delete(id, version);
        return ResultVO.success(null);
    }

    /** 启停账户。 */
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("@access.hasPermission('identity:user:status')")
    public ResultVO<UserVO> updateStatus(@PathVariable @Positive long id, @Valid @RequestBody UserStatusDTO dto) {
        return ResultVO.success(service.updateStatus(id, dto));
    }

    /** 重置密码并要求首次修改。 */
    @PutMapping("/users/{id}/password")
    @PreAuthorize("@access.hasPermission('identity:user:reset_password')")
    public ResultVO<Void> resetPassword(@PathVariable @Positive long id, @Valid @RequestBody PasswordResetDTO dto) {
        service.resetPassword(id, dto);
        return ResultVO.success(null);
    }

    /** 超管读取目标角色回显。 */
    @GetMapping("/users/{id}/roles")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:user:assign_role')")
    public ResultVO<List<String>> roles(@PathVariable @Positive long id) {
        return ResultVO.success(service.roles(id));
    }

    /** 超管授予或撤销 ADMIN 附加身份。 */
    @PutMapping("/users/{id}/roles")
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:user:assign_role')")
    public ResultVO<UserVO> assignRoles(@PathVariable @Positive long id, @Valid @RequestBody UserRolesDTO dto) {
        return ResultVO.success(service.assignRoles(id, dto));
    }

    /** 读取本人有效设备，不进行会话保活。 */
    @GetMapping("/me/sessions")
    @PreAuthorize("@access.hasPermission('identity:device:list')")
    public ResultVO<List<DeviceVO>> mySessions() {
        return ResultVO.success(service.mySessions());
    }

    /** 退出当前设备。 */
    @DeleteMapping("/me/sessions/current")
    public ResultVO<Void> logout() {
        service.logout();
        return ResultVO.success(null);
    }

    /** 退出本人指定设备。 */
    @DeleteMapping("/me/sessions/{id}")
    @PreAuthorize("@access.hasPermission('identity:device:revoke')")
    public ResultVO<Void> revokeMySession(@PathVariable @Size(max = 64) String id) {
        service.revokeMySession(id);
        return ResultVO.success(null);
    }

    /** 退出全部设备并提升数据库安全版本。 */
    @DeleteMapping("/me/sessions")
    @PreAuthorize("@access.hasPermission('identity:device:revoke')")
    public ResultVO<Void> logoutAll() {
        service.logoutAll();
        return ResultVO.success(null);
    }

    /** 查询管理范围内的有效会话。 */
    @GetMapping("/sessions")
    @PreAuthorize("@access.hasPermission('identity:session:list')")
    public ResultVO<PageResultVO<DeviceVO>> sessions(@Valid @ModelAttribute PageQueryDTO query) {
        return ResultVO.success(service.sessions(query));
    }

    /** 强退管理范围内的指定会话。 */
    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("@access.hasPermission('identity:session:revoke')")
    public ResultVO<Void> revokeSession(@PathVariable @Size(max = 64) String id) {
        service.revokeSession(id);
        return ResultVO.success(null);
    }
}
