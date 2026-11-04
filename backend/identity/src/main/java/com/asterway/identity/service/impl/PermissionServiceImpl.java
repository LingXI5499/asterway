package com.asterway.identity.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.AccountValidationUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.PermissionQueryDTO;
import com.asterway.identity.dto.PermissionSaveDTO;
import com.asterway.identity.entity.PermissionEntity;
import com.asterway.identity.mapper.PermissionMapper;
import com.asterway.identity.mapper.RoleMapper;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.service.PermissionService;
import com.asterway.identity.vo.PermissionVO;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 权限的查询、保存与删除放在同一服务，所有写操作与授权变更共用安全门锁。 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper mapper;

    private final RoleMapper roles;

    private final OperationLogService logs;

    public PermissionServiceImpl(
            PermissionMapper mapper,
            RoleMapper roles,
            OperationLogService logs) {
        this.mapper = mapper;
        this.roles = roles;
        this.logs = logs;
    }

    @Override
    public PageResultVO<PermissionVO> page(PermissionQueryDTO query) {
        SecurityUtils.requireSuperAdmin("identity:permission:list");
        int offset = query.offset();
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().toLowerCase(Locale.ROOT);
        List<PermissionEntity> matches = mapper.findAll().stream()
                .filter(row -> query.getEnabled() == null || query.getEnabled().equals(row.getEnabled()))
                .filter(row -> row.getCode().toLowerCase(Locale.ROOT).contains(keyword)
                        || row.getName().toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
        return new PageResultVO<>(matches.stream().skip(offset).limit(query.getSize()).map(this::toVO).toList(),
                matches.size(), query.getPage(), query.getSize());
    }

    @Override
    public PermissionVO get(long id) {
        SecurityUtils.requireSuperAdmin("identity:permission:read");
        return toVO(required(id));
    }

    @Override
    @Transactional
    public PermissionVO create(PermissionSaveDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:create");
        roles.lockSecurityGate();
        PermissionEntity row = new PermissionEntity();
        fill(row, dto);
        row.setCreatedBy(actor.userId());
        row.setUpdatedBy(actor.userId());
        mapper.insert(row);
        logs.append("identity:permission:create", "PERMISSION", row.getId().toString(), null, Map.of("version", 0));
        return toVO(required(row.getId()));
    }

    @Override
    @Transactional
    public PermissionVO update(long id, PermissionSaveDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:update");
        roles.lockSecurityGate();
        PermissionEntity row = required(id);
        AccountValidationUtils.version(dto.version(), row.getVersion());
        if (Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code())) {
            throw BusinessException.conflict("内置权限编码不能修改");
        }
        fill(row, dto);
        row.setUpdatedBy(actor.userId());
        if (mapper.update(row) != 1) {
            throw BusinessException.conflict("权限已被修改，请刷新");
        }
        logs.append("identity:permission:update", "PERMISSION", Long.toString(id), null, Map.of("version", dto.version()));
        return toVO(required(id));
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:delete");
        roles.lockSecurityGate();
        PermissionEntity row = required(id);
        AccountValidationUtils.version(version, row.getVersion());
        if (Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0) {
            throw BusinessException.conflict("内置权限或仍被引用的权限不能删除");
        }
        row.setUpdatedBy(actor.userId());
        if (mapper.softDelete(row) != 1) {
            throw BusinessException.conflict("权限已被修改，请刷新");
        }
        logs.append("identity:permission:delete", "PERMISSION", Long.toString(id), null, Map.of("version", version));
    }

    private PermissionEntity required(long id) {
        PermissionEntity row = mapper.findById(id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private void fill(PermissionEntity row, PermissionSaveDTO dto) {
        if (dto.code() == null || !dto.code().matches("^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$")
                || dto.name() == null || dto.name().isBlank() || dto.enabled() == null) {
            throw BusinessException.badRequest("权限字段无效");
        }
        row.setCode(dto.code());
        row.setName(dto.name());
        row.setDescription(dto.description() == null ? "" : dto.description());
        row.setEnabled(dto.enabled());
    }

    private PermissionVO toVO(PermissionEntity row) {
        return new PermissionVO(row.getId().toString(), row.getCode(), row.getName(), row.getDescription(),
                Boolean.TRUE.equals(row.getBuiltin()), Boolean.TRUE.equals(row.getEnabled()), row.getVersion());
    }

}
