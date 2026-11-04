package com.asterway.identity.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.JsonUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.identity.dto.SettingUpdateDTO;
import com.asterway.identity.entity.SettingEntity;
import com.asterway.identity.mapper.SettingMapper;
import com.asterway.identity.service.OperationLogService;
import com.asterway.identity.service.SettingService;
import com.asterway.identity.vo.PublicSettingVO;
import com.asterway.identity.vo.SettingVO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 只维护站点名称和注册开关，配置更新与注册共享锁协调。 */
@Service
public class SettingServiceImpl implements SettingService {

    private final SettingMapper mapper;

    private final OperationLogService logs;

    private final JsonUtils json;

    public SettingServiceImpl(
            SettingMapper mapper,
            OperationLogService logs,
            JsonUtils json) {
        this.mapper = mapper;
        this.logs = logs;
        this.json = json;
    }

    @Override
    public SettingVO get() {
        SecurityUtils.requireSuperAdmin("identity:setting:read");
        return values();
    }

    @Override
    public PublicSettingVO publicSettings() {
        SettingVO value = values();
        return new PublicSettingVO(value.siteName(), value.registrationEnabled());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireRegistrationOpen() {
        SettingEntity value = mapper.lockRegistration();
        if (value == null || !"true".equals(value.getSettingValue())) {
            throw new BusinessException(403, "REGISTRATION_CLOSED", "当前未开放注册");
        }
    }

    @Override
    @Transactional
    public SettingVO update(SettingUpdateDTO dto) {
        var actor = SecurityUtils.requireSuperAdmin("identity:setting:update");
        if (dto.siteName() == null || dto.siteName().isBlank() || dto.registrationEnabled() == null) {
            throw BusinessException.badRequest("账户配置无效");
        }
        // 两个 UPDATE 在同一事务内，第二项冲突会回滚第一项。
        SettingEntity site = new SettingEntity();
        site.setSettingKey("site_name");
        site.setSettingValue(json.write(dto.siteName()));
        site.setVersion(dto.siteNameVersion());
        site.setUpdatedBy(actor.userId());
        SettingEntity registration = new SettingEntity();
        registration.setSettingKey("registration_enabled");
        registration.setSettingValue(json.write(dto.registrationEnabled()));
        registration.setVersion(dto.registrationVersion());
        registration.setUpdatedBy(actor.userId());
        if (mapper.update(site) != 1 || mapper.update(registration) != 1) {
            throw BusinessException.conflict("配置已被修改，请刷新后重试");
        }
        logs.append("identity:setting:update", "SETTING", "account", null,
                Map.of("siteName", dto.siteName(), "registrationEnabled", dto.registrationEnabled()));
        return values();
    }

    private SettingVO values() {
        Map<String, SettingEntity> entries = new HashMap<>();
        for (SettingEntity entry : mapper.findAll()) {
            entries.put(entry.getSettingKey(), entry);
        }
        SettingEntity site = entries.get("site_name");
        SettingEntity registration = entries.get("registration_enabled");
        if (site == null || registration == null) {
            throw new BusinessException(503, "CONFIGURATION_MISSING", "账户配置未初始化");
        }
        return new SettingVO(json.text(site.getSettingValue()), "true".equals(registration.getSettingValue()),
                site.getVersion(), registration.getVersion());
    }

}
