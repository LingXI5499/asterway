package com.asterway.identity.service.impl;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.CaptchaResultVO;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.LoginSessionData;
import com.asterway.common.security.RedisAttemptStore;
import com.asterway.common.security.RedisCaptchaStore;
import com.asterway.common.security.RedisSessionStore;
import com.asterway.common.security.SecurityAccountData;
import com.asterway.common.utils.AccountValidationUtils;
import com.asterway.common.utils.JwtTokenUtils;
import com.asterway.common.utils.RequestUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.common.utils.TransactionUtils;
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
import com.asterway.identity.entity.RoleEntity;
import com.asterway.identity.entity.UserEntity;
import com.asterway.identity.mapper.PermissionMapper;
import com.asterway.identity.mapper.RoleMapper;
import com.asterway.identity.mapper.UserMapper;
import com.asterway.identity.mapper.UserRoleMapper;
import com.asterway.identity.service.LoginLogService;
import com.asterway.identity.service.MenuService;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.service.SettingService;
import com.asterway.identity.service.UserService;
import com.asterway.identity.vo.CaptchaTicketVO;
import com.asterway.identity.vo.CurrentUserVO;
import com.asterway.identity.vo.DeviceVO;
import com.asterway.identity.vo.LoginResultVO;
import com.asterway.identity.vo.TimeZoneVO;
import com.asterway.identity.vo.UserStatisticsVO;
import com.asterway.identity.vo.UserVO;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 账户主链；注册认证、本人资料、后台管理及用户关联在同一 Service 内完成。 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper mapper;

    private final RoleMapper roles;

    private final UserRoleMapper userRoles;

    private final PermissionMapper permissions;

    private final SettingService settings;

    private final MenuService menus;

    private final LoginLogService loginLogs;

    private final OperationLogService logs;

    private final PasswordEncoder passwordEncoder;

    private final RedisSessionStore sessions;

    private final RedisCaptchaStore captchas;

    private final RedisAttemptStore attempts;

    private final JwtTokenUtils tokens;

    public UserServiceImpl(
            UserMapper mapper,
            RoleMapper roles,
            UserRoleMapper userRoles,
            PermissionMapper permissions,
            SettingService settings,
            MenuService menus,
            LoginLogService loginLogs,
            OperationLogService logs,
            PasswordEncoder passwordEncoder,
            RedisSessionStore sessions,
            RedisCaptchaStore captchas,
            RedisAttemptStore attempts,
            JwtTokenUtils tokens) {
        this.mapper = mapper;
        this.roles = roles;
        this.userRoles = userRoles;
        this.permissions = permissions;
        this.settings = settings;
        this.menus = menus;
        this.loginLogs = loginLogs;
        this.logs = logs;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        this.sessions = sessions;
        this.captchas = captchas;
        this.attempts = attempts;
        this.tokens = tokens;
    }

    private final String dummyPasswordHash;

    @Override
    public SecurityAccountData loadSecurityAccount(long userId) {
        UserEntity row = mapper.findById(userId);
        if (row == null) {
            return null;
        }
        boolean enabled = Boolean.TRUE.equals(row.getEnabled()) && row.getDeletedAt() == null;
        return new SecurityAccountData(row.getId(), row.getUsername(), row.getPasswordHash(),
                enabled, Boolean.TRUE.equals(row.getMustChangePassword()), row.getAuthVersion(),
                new HashSet<>(roles.findCodesByUserId(userId)), new HashSet<>(permissions.findCodesByUserId(userId)));
    }

    @Override
    public CaptchaResultVO createCaptcha(CaptchaCreateDTO dto) {
        attempts.limit("captcha-create", RequestUtils.ipAddress(), 30, 60);
        return captchas.create(dto.scene(), dto.clientNonce());
    }

    @Override
    public CaptchaTicketVO verifyCaptcha(CaptchaVerifyDTO dto) {
        attempts.limit("captcha-verify", RequestUtils.ipAddress(), 60, 60);
        return new CaptchaTicketVO(captchas.verify(dto.challengeId(), dto.clientNonce(), dto.offset()), 60);
    }

    @Override
    @Transactional
    public UserVO register(RegistrationDTO dto) {
        attempts.limit("register", RequestUtils.ipAddress(), 10, 60);
        AccountValidationUtils.username(dto.username());
        AccountValidationUtils.password(dto.password(), dto.confirmPassword());
        // 注册不采集个人资料；昵称先使用用户名，时区暂用 UTC，登录后可自行完善。
        String nickname = AccountValidationUtils.nickname(null, dto.username());
        String zone = "UTC";
        captchas.consume(dto.captchaTicket(), "REGISTER", dto.clientNonce());
        String hash = passwordEncoder.encode(dto.password());
        // 编码后再持有开关共享锁，避免把昂贵计算放在配置锁持有期间。
        settings.requireRegistrationOpen();
        UserEntity row = newUser(dto.username(), nickname, hash, zone, false, null);
        mapper.insert(row);
        userRoles.insert(row.getId(), userRole().getId(), null);
        logs.append("USER_REGISTERED", "USER", row.getId().toString(), row.getId(), Map.of("username", dto.username()));
        return toVO(mapper.findById(row.getId()));
    }

    @Override
    @Transactional
    public LoginResultVO login(LoginDTO dto) {
        AccountValidationUtils.username(dto.username());
        try {
            attempts.limit("login", RequestUtils.ipAddress(), 30, 60);
            attempts.requireUnlocked(dto.username());
            captchas.consume(dto.captchaTicket(), "LOGIN", dto.clientNonce());
        } catch (BusinessException error) {
            loginLogs.append(null, dto.username(), false, error.getCode(), null);
            throw error;
        }
        UserEntity found = mapper.findByUsername(dto.username());
        boolean matches = passwordEncoder.matches(dto.password(), found == null ? dummyPasswordHash : found.getPasswordHash());
        if (!matches || found == null) {
            attempts.failedCredentials(dto.username());
            loginLogs.append(found == null ? null : found.getId(), dto.username(), false, "INVALID_CREDENTIALS", null);
            throw new BusinessException(401, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        UserEntity row = mapper.lockById(found.getId());
        if (row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled())
                || !Objects.equals(row.getPasswordHash(), found.getPasswordHash())) {
            loginLogs.append(found.getId(), dto.username(), false, "ACCOUNT_UNAVAILABLE", null);
            throw new BusinessException(401, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        var account = loadSecurityAccount(row.getId());
        var session = sessions.create(account, RequestUtils.ipAddress(), RequestUtils.userAgent());
        try {
            String token = tokens.create(session);
            loginLogs.append(row.getId(), dto.username(), true, "SUCCESS", session.id());
            attempts.clearFailures(dto.username());
            return new LoginResultVO(token, session.id(), Instant.ofEpochMilli(session.expiresAt()), toVO(row));
        } catch (RuntimeException error) {
            try {
                sessions.revoke(session.id());
            } catch (RuntimeException cleanupError) {
                error.addSuppressed(cleanupError);
            }
            throw error;
        }
    }

    @Override
    @Transactional
    public CurrentUserVO me() {
        var actor = SecurityUtils.requirePermission("identity:profile:read");
        UserEntity row = lockOwn();
        applyDueTimeZone(row);
        return new CurrentUserVO(toVO(row), actor.roles(), actor.permissions(), menus.currentTree());
    }

    @Override
    @Transactional
    public UserVO updateMe(UserUpdateDTO dto) {
        SecurityUtils.requirePermission("identity:profile:update");
        UserEntity row = lockOwn();
        return saveProfile(row, dto, row.getId());
    }

    @Override
    @Transactional
    public void changePassword(PasswordUpdateDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:password:update");
        AccountValidationUtils.password(dto.newPassword(), dto.confirmPassword());
        UserEntity row = lockOwn();
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (!passwordEncoder.matches(dto.currentPassword(), row.getPasswordHash())) {
            throw BusinessException.badRequest("原密码错误");
        }
        if (passwordEncoder.matches(dto.newPassword(), row.getPasswordHash())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        row.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        row.setMustChangePassword(false);
        row.setUpdatedBy(actor.userId());
        changed(mapper.updatePassword(row));
        logs.append("identity:password:update", "USER", row.getId().toString(), row.getId(), Map.of("passwordChanged", true));
        TransactionUtils.afterCommit(() -> sessions.revokeUser(row.getId()));
    }

    @Override
    @Transactional
    public TimeZoneVO timeZone() {
        SecurityUtils.requirePermission("identity:timezone:read");
        UserEntity row = lockOwn();
        applyDueTimeZone(row);
        return timeZoneVO(row);
    }

    @Override
    @Transactional
    public TimeZoneVO updateTimeZone(TimeZoneUpdateDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:timezone:update");
        String zone = AccountValidationUtils.timeZone(dto.timeZone());
        UserEntity row = lockOwn();
        applyDueTimeZone(row);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (zone.equals(row.getPendingTimeZone()) || zone.equals(row.getTimeZone()) && row.getPendingTimeZone() == null) {
            return timeZoneVO(row);
        }
        if (zone.equals(row.getTimeZone())) {
            row.setPendingTimeZone(null);
            row.setTimeZoneEffectiveAt(null);
        } else {
            row.setPendingTimeZone(zone);
            ZoneId currentZone = ZoneId.of(row.getTimeZone());
            row.setTimeZoneEffectiveAt(LocalDate.now(currentZone).plusDays(1).atStartOfDay(currentZone).toInstant());
        }
        row.setUpdatedBy(actor.userId());
        changed(mapper.updateTimeZone(row));
        logs.append("identity:timezone:update", "USER", row.getId().toString(), row.getId(), Map.of("timeZone", zone));
        return timeZoneVO(mapper.findById(row.getId()));
    }

    @Override
    public PageResultVO<String> timeZones(String keyword, PageQueryDTO query) {
        int offset = query.offset();
        String filter = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<String> available = ZoneId.getAvailableZoneIds().stream()
                .filter(zone -> zone.toLowerCase(Locale.ROOT).contains(filter)).sorted().toList();
        return new PageResultVO<>(available.stream().skip(offset).limit(query.getSize()).toList(),
                available.size(), query.getPage(), query.getSize());
    }

    @Override
    public PageResultVO<UserVO> page(UserQueryDTO query) {
        var actor = SecurityUtils.requirePermission("identity:user:list");
        int offset = query.offset();
        String keyword = literalKeyword(query.getKeyword());
        boolean regularOnly = !actor.superAdmin();
        List<UserVO> rows = mapper.findPage(keyword, query.getEnabled(), query.getRole(), regularOnly, offset, query.getSize())
                .stream().map(this::toVO).toList();
        return new PageResultVO<>(rows, mapper.count(keyword, query.getEnabled(), query.getRole(), regularOnly),
                query.getPage(), query.getSize());
    }

    @Override
    public UserStatisticsVO statistics() {
        var actor = SecurityUtils.requirePermission("identity:user:count");
        Map<String, Object> values = mapper.statistics(!actor.superAdmin());
        return new UserStatisticsVO(number(values, "users"), number(values, "administrators"),
                number(values, "disabled"), number(values, "deleted"));
    }

    @Override
    public UserVO get(long id) {
        SecurityUtils.requirePermission("identity:user:read");
        return toVO(managed(id, false));
    }

    @Override
    @Transactional
    public UserVO create(UserCreateDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:user:create");
        AccountValidationUtils.username(dto.username());
        AccountValidationUtils.password(dto.initialPassword(), dto.confirmPassword());
        String nickname = AccountValidationUtils.nickname(dto.nickname(), dto.username());
        String zone = AccountValidationUtils.timeZone(dto.timeZone());
        String hash = passwordEncoder.encode(dto.initialPassword());
        roles.lockSecurityGate();
        UserEntity row = newUser(dto.username(), nickname, hash, zone, true, actor.userId());
        mapper.insert(row);
        userRoles.insert(row.getId(), userRole().getId(), actor.userId());
        logs.append("identity:user:create", "USER", row.getId().toString(), row.getId(), Map.of("username", dto.username()));
        return toVO(mapper.findById(row.getId()));
    }

    @Override
    @Transactional
    public UserVO update(long id, UserUpdateDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:user:update");
        roles.lockSecurityGate();
        return saveProfile(managed(id, true), dto, actor.userId());
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        var actor = SecurityUtils.requirePermission("identity:user:delete");
        roles.lockSecurityGate();
        UserEntity row = managed(id, true);
        AccountValidationUtils.version(version, row.getVersion());
        protectUser(row);
        row.setEnabled(false);
        row.setDeletedAt(Instant.now());
        row.setUpdatedBy(actor.userId());
        changed(mapper.updateStatus(row));
        logs.append("identity:user:delete", "USER", Long.toString(id), id, Map.of("enabled", false));
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
    }

    @Override
    @Transactional
    public UserVO updateStatus(long id, UserStatusDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:user:status");
        roles.lockSecurityGate();
        UserEntity row = managed(id, true);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (dto.enabled() == null) {
            throw BusinessException.badRequest("请选择账户状态");
        }
        if (!dto.enabled()) {
            protectUser(row);
        }
        row.setEnabled(dto.enabled());
        row.setUpdatedBy(actor.userId());
        changed(mapper.updateStatus(row));
        logs.append("identity:user:status", "USER", Long.toString(id), id, Map.of("enabled", dto.enabled()));
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
        return toVO(mapper.findById(id));
    }

    @Override
    @Transactional
    public void resetPassword(long id, PasswordResetDTO dto) {
        var actor = SecurityUtils.requirePermission("identity:user:reset_password");
        AccountValidationUtils.password(dto.newPassword(), dto.confirmPassword());
        String hash = passwordEncoder.encode(dto.newPassword());
        roles.lockSecurityGate();
        UserEntity row = managed(id, true);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (roles.findAssignedCodes(id).contains("SUPER_ADMIN")) {
            throw BusinessException.conflict("超级管理员请通过本人改密入口修改密码");
        }
        row.setPasswordHash(hash);
        row.setMustChangePassword(true);
        row.setUpdatedBy(actor.userId());
        changed(mapper.updatePassword(row));
        logs.append("identity:user:reset_password", "USER", Long.toString(id), id, Map.of("passwordChanged", true));
        TransactionUtils.afterCommit(() -> sessions.revokeUser(id));
    }

    @Override
    public List<String> roles(long id) {
        SecurityUtils.requireSuperAdmin("identity:user:assign_role");
        managed(id, false);
        return userRoles.findIds(id).stream().map(Object::toString).toList();
    }

    @Override
    @Transactional
    public UserVO assignRoles(long id, UserRolesDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:user:assign_role");
        roles.lockSecurityGate();
        UserEntity row = managed(id, true);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        AccountValidationUtils.distinctIds(dto.roleIds());
        if (roles.findAssignedCodes(id).contains("SUPER_ADMIN")) {
            throw BusinessException.conflict("不能通过管理接口修改超级管理员身份");
        }
        Set<Long> allowed = new HashSet<>();
        for (RoleEntity role : roles.findAll()) {
            if (Set.of("USER", "ADMIN").contains(role.getCode()) && Boolean.TRUE.equals(role.getEnabled())) {
                allowed.add(role.getId());
            }
        }
        if (!allowed.containsAll(dto.roleIds()) || !dto.roleIds().contains(userRole().getId())) {
            throw BusinessException.badRequest("仅允许保留 USER 并授予或撤销 ADMIN");
        }
        row.setUpdatedBy(actor.userId());
        changed(mapper.updateProfile(row));
        userRoles.deleteByOwner(id);
        for (Long roleId : dto.roleIds()) {
            userRoles.insert(id, roleId, actor.userId());
        }
        logs.append("identity:user:assign_role", "USER", Long.toString(id), id, Map.of("roleIds", dto.roleIds()));
        return toVO(mapper.findById(id));
    }

    @Override
    public List<DeviceVO> mySessions() {
        var actor = SecurityUtils.requirePermission("identity:device:list");
        return devices(sessions.list(actor.userId()), false);
    }

    @Override
    public PageResultVO<DeviceVO> sessions(PageQueryDTO query) {
        var actor = SecurityUtils.requirePermission("identity:session:list");
        int offset = query.offset();
        List<DeviceVO> rows = devices(sessions.list(null), !actor.superAdmin());
        return new PageResultVO<>(rows.stream().skip(offset).limit(query.getSize()).toList(),
                rows.size(), query.getPage(), query.getSize());
    }

    @Override
    public void revokeMySession(String id) {
        var actor = SecurityUtils.requirePermission("identity:device:revoke");
        var session = sessions.get(id);
        if (session == null || !session.userId().equals(Long.toString(actor.userId()))) {
            throw BusinessException.notFound();
        }
        sessions.revoke(id);
        logs.append("identity:device:revoke", "SESSION", id, actor.userId(), Map.of());
    }

    @Override
    public void logout() {
        var actor = SecurityUtils.current();
        sessions.revoke(actor.sessionId());
        logs.append("USER_LOGOUT", "SESSION", actor.sessionId(), actor.userId(), Map.of());
    }

    @Override
    @Transactional
    public void logoutAll() {
        var actor = SecurityUtils.requirePermission("identity:device:revoke");
        UserEntity row = lockOwn();
        changed(mapper.invalidateSessions(row.getId(), row.getVersion(), actor.userId()));
        logs.append("identity:device:revoke", "USER", row.getId().toString(), row.getId(), Map.of("version", row.getVersion()));
        TransactionUtils.afterCommit(() -> sessions.revokeUser(actor.userId()));
    }

    @Override
    @Transactional
    public void revokeSession(String id) {
        var actor = SecurityUtils.requirePermission("identity:session:revoke");
        var session = sessions.get(id);
        if (session == null) {
            throw BusinessException.notFound();
        }
        roles.lockSecurityGate();
        UserEntity row = managed(Long.parseLong(session.userId()), true);
        sessions.revoke(id);
        logs.append("identity:session:revoke", "SESSION", id, row.getId(), Map.of("version", row.getVersion()));
    }

    private UserEntity managed(long id, boolean lock) {
        UserEntity row = lock ? mapper.lockById(id) : mapper.findById(id);
        if (row == null || row.getDeletedAt() != null) {
            throw BusinessException.notFound();
        }
        if (!SecurityUtils.current().superAdmin() && roles.findAssignedCodes(id).stream()
                .anyMatch(code -> code.equals("ADMIN") || code.equals("SUPER_ADMIN"))) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private UserEntity lockOwn() {
        UserEntity row = mapper.lockById(SecurityUtils.current().userId());
        if (row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled())
                || row.getAuthVersion() != SecurityUtils.current().authVersion()) {
            throw BusinessException.unauthorized();
        }
        return row;
    }

    private void protectUser(UserEntity row) {
        if (row.getId().equals(SecurityUtils.current().userId())) {
            throw BusinessException.conflict("不能禁用或删除当前登录账户");
        }
        if (roles.findAssignedCodes(row.getId()).contains("SUPER_ADMIN")
                && Boolean.TRUE.equals(row.getEnabled()) && roles.countEnabledSuperAdmins() <= 1) {
            throw BusinessException.conflict("必须保留至少一个可用超级管理员");
        }
    }

    private UserVO saveProfile(UserEntity row, UserUpdateDTO dto, long actorId) {
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (dto.username() == null && dto.nickname() == null) {
            throw BusinessException.badRequest("至少提供一个需要修改的资料字段");
        }
        if (dto.username() != null) {
            AccountValidationUtils.username(dto.username());
            row.setUsername(dto.username());
        }
        if (dto.nickname() != null) {
            row.setNickname(AccountValidationUtils.nickname(dto.nickname(), null));
        }
        row.setUpdatedBy(actorId);
        changed(mapper.updateProfile(row));
        logs.append("identity:profile:update", "USER", row.getId().toString(), row.getId(),
                Map.of("username", row.getUsername(), "nickname", row.getNickname()));
        return toVO(mapper.findById(row.getId()));
    }

    private void applyDueTimeZone(UserEntity row) {
        if (row.getPendingTimeZone() != null && !row.getTimeZoneEffectiveAt().isAfter(Instant.now())) {
            row.setTimeZone(row.getPendingTimeZone());
            row.setPendingTimeZone(null);
            row.setTimeZoneEffectiveAt(null);
            row.setUpdatedBy(row.getId());
            changed(mapper.updateTimeZone(row));
            row.setVersion(row.getVersion() + 1);
        }
    }

    private RoleEntity userRole() {
        return roles.findAll().stream().filter(role -> role.getCode().equals("USER") && Boolean.TRUE.equals(role.getEnabled()))
                .findFirst().orElseThrow(() -> new BusinessException(503, "ROLE_MISSING", "基础角色未初始化"));
    }

    private UserEntity newUser(String username, String nickname, String hash, String zone,
            boolean mustChange, Long creator) {
        UserEntity row = new UserEntity();
        row.setUsername(username);
        row.setNickname(nickname);
        row.setPasswordHash(hash);
        row.setTimeZone(zone);
        row.setMustChangePassword(mustChange);
        row.setCreatedBy(creator);
        row.setUpdatedBy(creator);
        return row;
    }

    private List<DeviceVO> devices(List<LoginSessionData> active, boolean regularOnly) {
        var actor = SecurityUtils.current();
        Map<Long, UserEntity> cache = new HashMap<>();
        List<DeviceVO> result = new ArrayList<>();
        for (var session : active) {
            long userId = Long.parseLong(session.userId());
            UserEntity row = cache.computeIfAbsent(userId, mapper::findById);
            if (row == null || row.getDeletedAt() != null || !Boolean.TRUE.equals(row.getEnabled())
                    || row.getAuthVersion() != session.authVersion()) {
                continue;
            }
            if (regularOnly && roles.findAssignedCodes(userId).stream()
                    .anyMatch(code -> code.equals("ADMIN") || code.equals("SUPER_ADMIN"))) {
                continue;
            }
            result.add(new DeviceVO(session.id(), session.userId(), row.getUsername(), session.deviceLabel(),
                    session.ipAddress(), session.userAgent(), Instant.ofEpochMilli(session.createdAt()),
                    Instant.ofEpochMilli(session.lastActiveAt()), Instant.ofEpochMilli(session.expiresAt()),
                    actor.sessionId().equals(session.id())));
        }
        return result;
    }

    private UserVO toVO(UserEntity row) {
        return new UserVO(row.getId().toString(), row.getUsername(), row.getNickname(), Boolean.TRUE.equals(row.getEnabled()),
                Boolean.TRUE.equals(row.getMustChangePassword()), row.getVersion(), row.getTimeZone(), row.getPendingTimeZone(),
                row.getTimeZoneEffectiveAt(), row.getCreatedAt(), roles.findAssignedCodes(row.getId()),
                userRoles.findIds(row.getId()).stream().map(Object::toString).toList());
    }

    private TimeZoneVO timeZoneVO(UserEntity row) {
        return new TimeZoneVO(
            row.getTimeZone(),
            row.getPendingTimeZone(),
            row.getTimeZoneEffectiveAt(),
            row.getVersion()
    );
    }

    private void changed(int affected) {
        if (affected != 1) {
            throw BusinessException.conflict("数据已变化，请刷新后重试");
        }
    }

    private long number(Map<String, Object> values, String key) {
        return ((Number) values.get(key)).longValue();
    }

    private String literalKeyword(String value) {
        return value == null ? null : value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

}
