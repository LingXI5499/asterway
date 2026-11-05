package com.asterway.habit.service.impl;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.mapper.GrowthSupportMapper;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.security.HabitLifecycleProvider;
import com.asterway.common.security.UserCalendarData;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.common.utils.UserCalendarUtils;
import com.asterway.habit.dto.HabitCheckinQueryDTO;
import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.dto.HabitSaveDTO;
import com.asterway.habit.entity.HabitCheckinEntity;
import com.asterway.habit.entity.HabitEntity;
import com.asterway.habit.entity.HabitRevisionEntity;
import com.asterway.habit.mapper.HabitCategoryMapper;
import com.asterway.habit.mapper.HabitCheckinMapper;
import com.asterway.habit.mapper.HabitMapper;
import com.asterway.habit.mapper.HabitRevisionMapper;
import com.asterway.habit.service.HabitService;
import com.asterway.habit.vo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/** 每个日期最多一次有效打卡；修订追加，不覆盖过去规则和完成事实。 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class HabitServiceImpl implements HabitService, HabitLifecycleProvider {
    private final HabitMapper mapper;
    private final HabitRevisionMapper revisions;
    private final HabitCheckinMapper checkins;
    private final HabitCategoryMapper categories;
    private final GrowthSupportMapper support;
    private final GrowthContextUtils ctx;
    private final UserCalendarUtils calendar;

    public HabitServiceImpl(
            HabitMapper mapper,
            HabitRevisionMapper revisions,
            HabitCheckinMapper checkins,
            HabitCategoryMapper categories,
            GrowthSupportMapper support,
            GrowthContextUtils ctx,
            UserCalendarUtils calendar) {
        this.mapper = mapper;
        this.revisions = revisions;
        this.checkins = checkins;
        this.categories = categories;
        this.support = support;
        this.ctx = ctx;
        this.calendar = calendar;
    }

    @Override
    public PageResultVO<HabitVO> page(HabitQueryDTO query) {
        long userId = ctx.read("habit:habit:list");
        query.offset();
        if (query.getKeyword() != null && query.getKeyword().length() > 100) {
            throw BusinessException.badRequest("关键字过长");
        }
        if (query.getCategoryId() != null && query.getCategoryId() <= 0
                || query.getStatus() != null
                        && !Set.of("ACTIVE", "PAUSED", "ENDED").contains(query.getStatus())) {
            throw BusinessException.badRequest("分类或状态筛选无效");
        }
        var day = calendar.read(userId);
        return new PageResultVO<>(
                mapper.page(userId, query, day.today()),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public HabitVO get(long id) {
        long userId = ctx.read("habit:habit:read");
        required(userId, id, false);
        return view(userId, id, calendar.read(userId));
    }

    @Override
    @Transactional
    public HabitVO create(HabitSaveDTO dto) {
        long userId = ctx.write("habit:habit:create");
        var day = calendar.write(userId);
        var rule = new HabitRevisionEntity();
        fill(rule, dto, day);
        validateCategory(userId, rule.getCategoryId(), null);
        var row = new HabitEntity();
        row.setUserId(userId);
        row.setStatus("ACTIVE");
        row.setCurrentRevisionNo(1L);
        row.setVersion(0L);
        row.setCreatedAt(day.now());
        row.setUpdatedAt(day.now());
        ctx.changed(mapper.insert(row));
        snapshot(row, rule, "CREATED", day, day.today());
        ctx.changed(revisions.insert(rule));
        return view(userId, row.getId(), day);
    }

    @Override
    @Transactional
    public HabitVO update(long id, HabitSaveDTO dto, boolean replace) {
        long userId = ctx.write("habit:habit:update");
        var day = calendar.write(userId);
        var row = required(userId, id, false);
        ctx.version(row.getVersion(), dto.version());
        if ("ENDED".equals(row.getStatus())) {
            throw BusinessException.conflict("习惯已结束，请先重新开启");
        }
        var previous = current(row);
        HabitSaveDTO values =
                replace
                        ? dto
                        : new HabitSaveDTO(
                                dto.name() == null ? previous.getName() : dto.name(),
                                dto.description() == null
                                        ? previous.getDescription()
                                        : dto.description(),
                                dto.completionCriteria() == null
                                        ? previous.getCompletionCriteria()
                                        : dto.completionCriteria(),
                                dto.categoryId() == null
                                        ? previous.getCategoryId()
                                        : dto.categoryId(),
                                dto.cycleType() == null ? previous.getCycleType() : dto.cycleType(),
                                dto.weekdays() == null
                                        ? ("DAILY".equals(dto.cycleType())
                                                ? null
                                                : weekdays(previous.getWeekdayMask()))
                                        : dto.weekdays(),
                                dto.startDate() == null ? previous.getStartDate() : dto.startDate(),
                                dto.expectedTime() == null
                                        ? previous.getExpectedTime()
                                        : dto.expectedTime(),
                                dto.version());
        var next = new HabitRevisionEntity();
        fill(next, values, day);
        validateCategory(userId, next.getCategoryId(), previous.getCategoryId());
        if (sameRule(previous, next)) {
            return view(userId, id, day);
        }
        editable(row, previous, day);
        append(row, next, "EDITED", day, day.today());
        return view(userId, id, day);
    }

    @Override
    @Transactional
    public HabitVO pause(long id, long version) {
        return transition(id, version, "habit:habit:pause", "PAUSED", "PAUSED", Set.of("ACTIVE"));
    }

    @Override
    @Transactional
    public HabitVO resume(long id, long version) {
        return transition(id, version, "habit:habit:resume", "ACTIVE", "RESUMED", Set.of("PAUSED"));
    }

    @Override
    @Transactional
    public HabitVO end(long id, long version) {
        return transition(
                id, version, "habit:habit:end", "ENDED", "ENDED", Set.of("ACTIVE", "PAUSED"));
    }

    @Override
    @Transactional
    public HabitVO reopen(long id, long version) {
        return transition(id, version, "habit:habit:reopen", "ACTIVE", "REOPENED", Set.of("ENDED"));
    }

    private HabitVO transition(
            long id,
            long version,
            String permission,
            String target,
            String action,
            Set<String> allowed) {
        long userId = ctx.write(permission);
        var day = calendar.write(userId);
        var row = required(userId, id, false);
        if (target.equals(row.getStatus())) {
            return view(userId, id, day);
        }
        ctx.version(row.getVersion(), version);
        if (!allowed.contains(row.getStatus())) {
            throw BusinessException.conflict("当前状态不允许该操作，请选择恢复或重新开启");
        }
        var rule = current(row);
        editable(row, rule, day);
        row.setStatus(target);
        row.setEndedAt("ENDED".equals(target) ? day.now() : null);
        append(row, copy(rule), action, day, day.today());
        return view(userId, id, day);
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        long userId = ctx.write("habit:habit:delete");
        var day = calendar.write(userId);
        var row = required(userId, id, true);
        if (row.getDeletedAt() != null) {
            return;
        }
        ctx.version(row.getVersion(), version);
        var rule = current(row);
        editable(row, rule, day);
        row.setDeletedAt(day.now());
        append(row, copy(rule), "DELETED", day, day.today());
    }

    @Override
    public PageResultVO<HabitTodayVO> today(PageQueryDTO query) {
        long userId = ctx.read("habit:habit:list");
        var day = calendar.read(userId);
        return new PageResultVO<>(
                mapper.today(userId, day.today(), query.offset(), query.getSize()),
                mapper.todayCount(userId, day.today()),
                query.getPage(),
                query.getSize());
    }

    @Override
    @Transactional
    public HabitCheckinVO checkin(long id, long habitVersion) {
        long userId = ctx.write("habit:checkin:create");
        var day = calendar.write(userId);
        var row = required(userId, id, false);
        var existing = checkins.valid(userId, id, day.today());
        if (existing != null) {
            return checkins.view(userId, existing.getId());
        }
        ctx.version(row.getVersion(), habitVersion);
        var rule = current(row);
        editable(row, rule, day);
        if (!"ACTIVE".equals(row.getStatus())
                || !"ACTIVE".equals(rule.getStatusSnapshot())
                || Boolean.TRUE.equals(rule.getDeletedSnapshot())
                || rule.getStartDate().isAfter(day.today())
                || (rule.getWeekdayMask() & (1 << (day.today().getDayOfWeek().getValue() - 1)))
                        == 0) {
            throw BusinessException.conflict("今天不是该习惯可打卡的日期");
        }
        var checkin = new HabitCheckinEntity();
        checkin.setUserId(userId);
        checkin.setHabitId(id);
        checkin.setRevisionId(rule.getId());
        checkin.setBusinessDate(day.today());
        checkin.setTimeZone(day.zone().getId());
        checkin.setCheckedAt(day.now());
        checkin.setCreatedAt(day.now());
        checkin.setUpdatedAt(day.now());
        ctx.changed(checkins.insert(checkin));
        return checkins.view(userId, checkin.getId());
    }

    @Override
    @Transactional
    public HabitCheckinVO cancel(long id, long checkinId, long version) {
        long userId = ctx.write("habit:checkin:cancel");
        var day = calendar.write(userId);
        var row = required(userId, id, false);
        var checkin = checkins.find(userId, checkinId);
        if (checkin == null || checkin.getHabitId() != id) {
            throw BusinessException.notFound();
        }
        editable(row, current(row), day);
        if (!day.today().equals(checkin.getBusinessDate())) {
            throw BusinessException.conflict("只能撤销今天的打卡，不允许修改历史");
        }
        if (checkin.getCancelledAt() == null) {
            ctx.version(checkin.getVersion(), version);
            checkin.setCancelledAt(day.now());
            checkin.setUpdatedAt(day.now());
            ctx.changed(checkins.cancel(checkin));
        }
        return checkins.view(userId, checkinId);
    }

    @Override
    public PageResultVO<HabitCheckinVO> checkins(long id, HabitCheckinQueryDTO query) {
        long userId = ctx.read("habit:checkin:read");
        required(userId, id, true);
        query.offset();
        ctx.dates(query.getDateFrom(), query.getDateTo());
        return new PageResultVO<>(
                checkins.page(userId, id, query),
                checkins.count(userId, id, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    public PageResultVO<HabitRevisionVO> revisions(long id, PageQueryDTO query) {
        long userId = ctx.read("habit:habit:history");
        required(userId, id, true);
        return new PageResultVO<>(
                revisions.page(userId, id, query.offset(), query.getSize()).stream()
                        .map(this::revisionView)
                        .toList(),
                revisions.count(userId, id),
                query.getPage(),
                query.getSize());
    }

    /** 封存只推进边界，不生成每日占位行，也不改写已有规则或打卡。 */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void sealBeforeZoneSwitch(long userId, Instant effectiveAt, String previousZone) {
        if (effectiveAt == null) {
            return;
        }
        lockAny(userId);
        LocalDate through =
                effectiveAt.minusMillis(1).atZone(ZoneId.of(previousZone)).toLocalDate();
        mapper.sealHistory(userId, through, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void suspendPersonal(long userId) {
        lockAny(userId);
        var account = support.calendarAccount(userId);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        if (account.pendingTimeZone() != null && !account.timeZoneEffectiveAt().isAfter(now)) {
            sealBeforeZoneSwitch(userId, account.timeZoneEffectiveAt(), account.timeZone());
        }
        var day = new UserCalendarData(userId, account.effectiveZone(now), now);
        long after = 0;
        while (true) {
            var batch = mapper.activeBatch(userId, after);
            if (batch.isEmpty()) {
                return;
            }
            for (var row : batch) {
                var rule = current(row);
                LocalDate effective = day.today();
                // 时区回拨时，系统暂停不能重写已封存日期；主体立即暂停，规则从首个未封存日生效。
                if (row.getHistoryLockedThrough() != null
                        && !effective.isAfter(row.getHistoryLockedThrough())) {
                    effective = row.getHistoryLockedThrough().plusDays(1);
                }
                if (effective.isBefore(rule.getEffectiveDate())) {
                    effective = rule.getEffectiveDate();
                }
                row.setStatus("PAUSED");
                append(row, copy(rule), "SYSTEM_PAUSED", day, effective);
                after = row.getId();
            }
        }
    }

    private void lockAny(long userId) {
        if (support.lockAnyUser(userId) == null) {
            throw BusinessException.notFound();
        }
    }

    private HabitEntity required(long userId, long id, boolean includeDeleted) {
        var row = mapper.find(userId, id);
        if (row == null || !includeDeleted && row.getDeletedAt() != null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private HabitRevisionEntity current(HabitEntity row) {
        var rule = revisions.current(row.getUserId(), row.getId(), row.getCurrentRevisionNo());
        if (rule == null) {
            throw new IllegalStateException("习惯缺少当前完整修订");
        }
        return rule;
    }

    private HabitVO view(long userId, long id, UserCalendarData day) {
        var value = mapper.view(userId, id, day.today());
        if (value == null) {
            throw BusinessException.notFound();
        }
        return value;
    }

    private void editable(HabitEntity row, HabitRevisionEntity rule, UserCalendarData day) {
        if (row.getHistoryLockedThrough() != null
                        && !day.today().isAfter(row.getHistoryLockedThrough())
                || rule.getEffectiveDate().isAfter(day.today())) {
            throw BusinessException.conflict("该本地日期已封存，请在新的业务日期操作");
        }
    }

    private void fill(HabitRevisionEntity row, HabitSaveDTO dto, UserCalendarData day) {
        row.setName(ctx.name(dto.name()));
        row.setDescription(ctx.text(dto.description()));
        row.setCompletionCriteria(ctx.text(dto.completionCriteria()));
        if (row.getDescription().length() > 10000 || row.getCompletionCriteria().length() > 10000) {
            throw BusinessException.badRequest("描述或完成标准过长");
        }
        row.setCategoryId(dto.categoryId());
        String cycle = dto.cycleType() == null ? "DAILY" : dto.cycleType();
        if (!Set.of("DAILY", "WEEKLY_DAYS").contains(cycle)) {
            throw BusinessException.badRequest("周期仅支持每天或指定星期");
        }
        int mask = 0;
        if (dto.weekdays() != null) {
            for (Integer dayOfWeek : dto.weekdays()) {
                if (dayOfWeek == null
                        || dayOfWeek < 1
                        || dayOfWeek > 7
                        || (mask & (1 << (dayOfWeek - 1))) != 0) {
                    throw BusinessException.badRequest("星期须为不重复的 1～7");
                }
                mask |= 1 << (dayOfWeek - 1);
            }
        }
        if ("DAILY".equals(cycle)) {
            if (dto.weekdays() != null && mask != 127) {
                throw BusinessException.badRequest("每天执行不能只选择部分星期");
            }
            mask = 127;
        } else if (mask == 0) {
            throw BusinessException.badRequest("请至少选择一个执行星期");
        }
        row.setCycleType(cycle);
        row.setWeekdayMask(mask);
        row.setStartDate(dto.startDate() == null ? day.today() : dto.startDate());
        if (dto.expectedTime() != null
                && (dto.expectedTime().getSecond() != 0 || dto.expectedTime().getNano() != 0)) {
            throw BusinessException.badRequest("提示时间仅支持 HH:mm");
        }
        row.setExpectedTime(dto.expectedTime());
    }

    private void validateCategory(long userId, Long next, Long previous) {
        if (next == null) {
            return;
        }
        var category = categories.find(userId, next);
        if (next <= 0
                || category == null
                || category.getDeletedAt() != null && !Objects.equals(next, previous)) {
            throw BusinessException.badRequest("只能选择本人有效分类");
        }
    }

    private boolean sameRule(HabitRevisionEntity a, HabitRevisionEntity b) {
        return Objects.equals(a.getName(), b.getName())
                && Objects.equals(a.getDescription(), b.getDescription())
                && Objects.equals(a.getCompletionCriteria(), b.getCompletionCriteria())
                && Objects.equals(a.getCategoryId(), b.getCategoryId())
                && Objects.equals(a.getCycleType(), b.getCycleType())
                && Objects.equals(a.getWeekdayMask(), b.getWeekdayMask())
                && Objects.equals(a.getStartDate(), b.getStartDate())
                && Objects.equals(a.getExpectedTime(), b.getExpectedTime());
    }

    private HabitRevisionEntity copy(HabitRevisionEntity rule) {
        var result = new HabitRevisionEntity();
        result.setName(rule.getName());
        result.setDescription(rule.getDescription());
        result.setCompletionCriteria(rule.getCompletionCriteria());
        result.setCategoryId(rule.getCategoryId());
        result.setCycleType(rule.getCycleType());
        result.setWeekdayMask(rule.getWeekdayMask());
        result.setStartDate(rule.getStartDate());
        result.setExpectedTime(rule.getExpectedTime());
        return result;
    }

    private void snapshot(
            HabitEntity row,
            HabitRevisionEntity rule,
            String action,
            UserCalendarData day,
            LocalDate effectiveDate) {
        rule.setUserId(row.getUserId());
        rule.setHabitId(row.getId());
        rule.setRevisionNo(row.getCurrentRevisionNo());
        rule.setActionType(action);
        rule.setStatusSnapshot(row.getStatus());
        rule.setDeletedSnapshot(row.getDeletedAt() != null);
        rule.setEffectiveDate(effectiveDate);
        rule.setChangedAt(day.now());
        rule.setTimeZone(day.zone().getId());
        rule.setCreatedAt(day.now());
    }

    private void append(
            HabitEntity row,
            HabitRevisionEntity rule,
            String action,
            UserCalendarData day,
            LocalDate effectiveDate) {
        row.setCurrentRevisionNo(row.getCurrentRevisionNo() + 1);
        row.setUpdatedAt(day.now());
        snapshot(row, rule, action, day, effectiveDate);
        ctx.changed(revisions.insert(rule));
        ctx.changed(mapper.update(row));
        row.setVersion(row.getVersion() + 1);
    }

    private List<Integer> weekdays(int mask) {
        return IntStream.rangeClosed(1, 7)
                .filter(day -> (mask & (1 << (day - 1))) != 0)
                .boxed()
                .toList();
    }

    private HabitRevisionVO revisionView(HabitRevisionEntity row) {
        return new HabitRevisionVO(
                row.getId().toString(),
                row.getRevisionNo(),
                row.getActionType(),
                row.getName(),
                row.getDescription(),
                row.getCompletionCriteria(),
                ctx.id(row.getCategoryId()),
                row.getCycleType(),
                weekdays(row.getWeekdayMask()),
                row.getStartDate(),
                row.getExpectedTime(),
                row.getStatusSnapshot(),
                row.getDeletedSnapshot(),
                row.getEffectiveDate(),
                row.getChangedAt(),
                row.getTimeZone());
    }
}
