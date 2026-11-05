package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.mapper.GrowthSupportMapper;
import com.asterway.common.security.AccountSecurityProvider;
import com.asterway.common.security.GrowthScopeData;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;

/** 跨模块公共校验：所有成长写入口先锁定同一个用户，避免检查后父对象失效。 */
@Component
public class GrowthContextUtils {
    private final GrowthSupportMapper mapper;
    private final com.asterway.common.security.OperationAuditProvider audit;
    private final ObjectProvider<AccountSecurityProvider> accounts;

    public GrowthContextUtils(
            GrowthSupportMapper mapper,
            com.asterway.common.security.OperationAuditProvider audit,
            ObjectProvider<AccountSecurityProvider> accounts) {
        this.mapper = mapper;
        this.audit = audit;
        this.accounts = accounts;
    }

    public long read(String permission) {
        return SecurityUtils.requirePermission(permission).userId();
    }

    /** 中立生命周期契约使用，必须加入既有父事务，锁顺序仍先用户后业务对象。 */
    public void lockLifecycleUser(long userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("生命周期联动必须参加父业务事务");
        }
        if (mapper.lockAnyUser(userId) == null) {
            throw BusinessException.notFound();
        }
    }

    public long write(String permission) {
        var actor = SecurityUtils.requirePermission(permission);
        long userId = actor.userId();
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("成长写操作必须在事务内");
        }
        if (mapper.lockUser(userId) == null) {
            throw BusinessException.unauthorized();
        }
        // 等待用户锁时可能发生账号停用、改密或角色变更，不能继续使用请求开始时的旧身份。
        var fresh = accounts.getObject().loadSecurityAccount(userId);
        if (fresh == null || !fresh.enabled() || fresh.authVersion() != actor.authVersion()) {
            throw BusinessException.unauthorized();
        }
        if (GrowthAccessUtils.permission(permission)
                        && (GrowthAccessUtils.manager(fresh.roles())
                                || !fresh.roles().contains("USER"))
                || !fresh.roles().contains("SUPER_ADMIN")
                        && !fresh.permissions().contains(permission)) {
            throw BusinessException.forbidden();
        }
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void beforeCommit(boolean readOnly) {
                                if (!readOnly) {
                                    audit.recordGrowthSuccess(permission, userId);
                                }
                            }
                        });
        return userId;
    }

    public GrowthScopeData scope(String type, long userId, long id, boolean writable) {
        GrowthScopeData row =
                switch (type) {
                    case "goal" -> mapper.goal(userId, id);
                    case "stage" -> mapper.stage(userId, id);
                    case "task" -> mapper.task(userId, id);
                    case "week" -> mapper.week(userId, id);
                    case "day" -> mapper.day(userId, id);
                    default -> throw new IllegalArgumentException("未知资源类型");
                };
        if (row == null) {
            throw BusinessException.notFound();
        }
        if (writable && row.getArchivedAt() != null) {
            throw BusinessException.conflict("目标已归档，请先恢复");
        }
        return row;
    }

    public LocalDate today(long userId) {
        String zone = mapper.timeZone(userId);
        if (zone == null) {
            throw BusinessException.unauthorized();
        }
        return LocalDate.now(ZoneId.of(zone));
    }

    public void version(Long actual, Long expected) {
        if (expected == null || !Objects.equals(actual, expected)) {
            throw BusinessException.conflict("数据版本已变化，请刷新后重试");
        }
    }

    public void changed(int count) {
        if (count != 1) {
            throw BusinessException.conflict("数据已变化，请刷新后重试");
        }
    }

    public void dates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw BusinessException.badRequest("结束日期不能早于开始日期");
        }
    }

    public String name(String value) {
        if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 100) {
            throw BusinessException.badRequest("名称须为 1～100 个字符且不能全为空白");
        }
        return value;
    }

    public String text(String value) {
        return value == null ? "" : value;
    }

    public int order(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw BusinessException.badRequest("排序不能小于零");
        }
        return value;
    }

    public String state(String value, boolean task) {
        if (value == null
                || !(task
                                ? Set.of("NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED")
                                : Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED"))
                        .contains(value)) {
            throw BusinessException.badRequest("状态无效");
        }
        return value;
    }

    public void endTask(long userId, long id) {
        mapper.endTask(userId, id);
    }

    public void endGoal(long userId, long id) {
        mapper.endGoal(userId, id);
    }

    public String id(Long value) {
        return value == null ? null : value.toString();
    }

    public java.util.List<com.asterway.common.result.GrowthOverviewVO> stageOverview(
            long userId, long id) {
        return mapper.stageOverview(userId, id);
    }

    public java.util.List<com.asterway.common.result.GrowthOverviewVO> taskOverview(
            long userId, long id) {
        return mapper.taskOverview(userId, id);
    }

    public long stages(long userId, long id) {
        return mapper.stages(userId, id);
    }

    public long tasks(long userId, long id) {
        return mapper.tasks(userId, id);
    }

    public void prepare(com.asterway.common.dto.GrowthQueryDTO q, long userId) {
        q.offset();
        q.setToday(today(userId));
        dates(q.getStartFrom(), q.getStartTo());
        dates(q.getDateFrom(), q.getDateTo());
        if (q.getStatus() != null && !q.getStatus().isBlank()) {
            state(q.getStatus(), true);
        }
        if (q.getPriority() != null
                && !q.getPriority().isBlank()
                && !Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(q.getPriority())) {
            throw BusinessException.badRequest("优先级无效");
        }
        if (q.getAssignmentStatus() != null
                && !q.getAssignmentStatus().isBlank()
                && !Set.of("POOL", "WEEK", "DAY").contains(q.getAssignmentStatus())) {
            throw BusinessException.badRequest("安排状态无效");
        }
        if (q.getTiming() != null
                && !q.getTiming().isBlank()
                && !Set.of("ALL", "CURRENT", "FUTURE").contains(q.getTiming())) {
            throw BusinessException.badRequest("时间筛选无效");
        }
        if (q.getKeyword() != null && q.getKeyword().length() > 100) {
            throw BusinessException.badRequest("关键字过长");
        }
        for (Long id :
                new Long[] {
                    q.getGoalId(),
                    q.getStageId(),
                    q.getTaskId(),
                    q.getWeeklyPlanId(),
                    q.getDailyPlanId()
                }) {
            if (id != null && id <= 0) {
                throw BusinessException.badRequest("资源标识无效");
            }
        }
    }
}
