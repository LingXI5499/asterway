package com.asterway.stage.service.impl;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthReorderDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.ExecutionLifecycleProvider;
import com.asterway.common.security.GoalNotificationProvider;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.stage.dto.StageSaveDTO;
import com.asterway.stage.entity.StageEntity;
import com.asterway.stage.entity.StageStateHistoryEntity;
import com.asterway.stage.mapper.StageMapper;
import com.asterway.stage.mapper.StageStateHistoryMapper;
import com.asterway.stage.service.StageService;
import com.asterway.stage.vo.StageStateHistoryVO;
import com.asterway.stage.vo.StageVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

/** Stage 归属、状态和版本控制；所有写操作先获得统一用户锁。 */
@Service
public class StageServiceImpl implements StageService {
    private final StageMapper mapper;
    private final GrowthContextUtils ctx;
    private final ExecutionLifecycleProvider execution;
    private final StageStateHistoryMapper histories;
    private final GoalNotificationProvider notices;

    public StageServiceImpl(
            StageMapper mapper,
            GrowthContextUtils ctx,
            ExecutionLifecycleProvider execution,
            StageStateHistoryMapper histories,
            GoalNotificationProvider notices) {
        this.mapper = mapper;
        this.ctx = ctx;
        this.execution = execution;
        this.histories = histories;
        this.notices = notices;
    }

    @Override
    public PageResultVO<StageVO> page(GrowthQueryDTO query) {
        long userId = ctx.read("stage:stage:list");
        ctx.prepare(query, userId);
        if (query.getGoalId() == null) {
            throw BusinessException.badRequest("必须指定目标");
        }
        ctx.scope("goal", userId, query.getGoalId(), false);
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(row -> toVO(row, false)).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public StageVO get(long id) {
        long userId = ctx.read("stage:stage:read");
        ctx.scope("stage", userId, id, false);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public StageVO create(long parentId, StageSaveDTO dto) {
        long userId = ctx.write("stage:stage:create");
        ctx.scope("goal", userId, parentId, true);
        StageEntity row = new StageEntity();
        row.setUserId(userId);
        row.setStatus("NOT_STARTED");
        row.setGoalId(parentId);
        fill(row, dto);
        mapper.insert(row);
        appendHistory(row, "CREATED", null, 0L);
        notices.refreshGoal(userId, parentId);
        return toVO(required(userId, row.getId()), true);
    }

    @Override
    @Transactional
    public StageVO update(long id, StageSaveDTO dto) {
        long userId = ctx.write("stage:stage:update");
        ctx.scope("stage", userId, id, true);
        StageEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        dto =
                new StageSaveDTO(
                        dto.name() == null ? row.getName() : dto.name(),
                        dto.description() == null ? row.getDescription() : dto.description(),
                        dto.completionCriteria() == null
                                ? row.getCompletionCriteria()
                                : dto.completionCriteria(),
                        dto.startDate() == null ? row.getStartDate() : dto.startDate(),
                        dto.endDate() == null ? row.getEndDate() : dto.endDate(),
                        dto.sortOrder() == null ? row.getSortOrder() : dto.sortOrder(),
                        dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public StageVO replace(long id, StageSaveDTO dto) {
        long userId = ctx.write("stage:stage:update");
        ctx.scope("stage", userId, id, true);
        StageEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        fill(row, dto);
        ctx.changed(mapper.update(row));
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public StageVO status(long id, GrowthStatusDTO dto) {
        long userId = ctx.write("stage:stage:status");
        ctx.scope("stage", userId, id, true);
        StageEntity row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        String before = row.getStatus();
        String next = ctx.state(dto.status(), false);
        if (next.equals(before)) {
            return toVO(row, true);
        }
        row.setStatus(next);
        ctx.changed(mapper.update(row));
        execution.validateRunningEligibility(userId);
        appendHistory(row, "STATUS_CHANGED", before, row.getVersion() + 1);
        return toVO(required(userId, id), true);
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        long userId = ctx.write("stage:stage:delete");
        ctx.scope("stage", userId, id, true);
        StageEntity row = required(userId, id);
        ctx.version(row.getVersion(), version);
        if (ctx.tasks(userId, id) > 0) {
            throw BusinessException.conflict("请先处理阶段下的有效任务");
        }
        ctx.changed(mapper.softDelete(row));
        appendHistory(row, "DELETED", row.getStatus(), row.getVersion() + 1);
        notices.refreshGoal(userId, row.getGoalId());
    }

    @Override
    @Transactional
    public void reorder(long parentId, GrowthReorderDTO dto) {
        long userId = ctx.write("stage:stage:sort");
        ctx.scope("goal", userId, parentId, true);
        if (dto.items() == null || dto.items().isEmpty() || dto.items().size() > 100) {
            throw BusinessException.badRequest("排序数量须为 1～100");
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (var item : dto.items()) {
            if (!ids.add(item.id())) {
                throw BusinessException.badRequest("排序标识不能重复");
            }
            StageEntity row = required(userId, item.id());
            if (!java.util.Objects.equals(row.getGoalId(), parentId)) {
                throw BusinessException.badRequest("只能调整同一父资源的顺序");
            }
            ctx.version(row.getVersion(), item.version());
            row.setSortOrder(ctx.order(item.sortOrder()));
            ctx.changed(mapper.update(row));
        }
    }

    @Override
    public PageResultVO<StageStateHistoryVO> history(long id, PageQueryDTO query) {
        long userId = ctx.read("stage:stage:read");
        int offset = query.offset();
        if (histories.historicalSubject(userId, id) == null) {
            throw BusinessException.notFound();
        }
        return new PageResultVO<>(
                histories.page(userId, id, offset, query.getSize()),
                histories.count(userId, id),
                query.getPage(),
                query.getSize());
    }

    private void appendHistory(StageEntity row, String event, String before, long version) {
        var history = new StageStateHistoryEntity();
        history.setUserId(row.getUserId());
        history.setStageId(row.getId());
        history.setEventType(event);
        history.setBeforeStatus(before);
        history.setAfterStatus(row.getStatus());
        history.setNameSnapshot(row.getName());
        history.setEntityVersion(version);
        history.setOccurredAt(java.time.Instant.now().truncatedTo(ChronoUnit.MILLIS));
        history.setSourceType("USER_ACTION");
        ctx.changed(histories.insert(history));
    }

    private void fill(StageEntity row, StageSaveDTO dto) {
        ctx.dates(dto.startDate(), dto.endDate());
        row.setName(ctx.name(dto.name()));
        row.setDescription(ctx.text(dto.description()));
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        row.setStartDate(dto.startDate());
        row.setEndDate(dto.endDate());
        row.setSortOrder(ctx.order(dto.sortOrder()));
    }

    private StageEntity required(long userId, long id) {
        StageEntity row = mapper.findById(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private StageVO toVO(StageEntity row, boolean detail) {
        return new StageVO(
                ctx.id(row.getId()),
                ctx.id(row.getGoalId()),
                row.getName(),
                row.getDescription(),
                row.getCompletionCriteria(),
                row.getStartDate(),
                row.getEndDate(),
                row.getSortOrder(),
                row.getStatus(),
                row.getVersion(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                mapper.progress(row.getUserId(), row.getId()),
                detail ? ctx.taskOverview(row.getUserId(), row.getId()) : java.util.List.of());
    }
}
