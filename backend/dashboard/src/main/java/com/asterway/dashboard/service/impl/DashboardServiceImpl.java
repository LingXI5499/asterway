package com.asterway.dashboard.service.impl;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.common.utils.SecurityUtils;
import com.asterway.common.utils.UserCalendarUtils;
import com.asterway.dashboard.dto.DashboardReadDTO;
import com.asterway.dashboard.dto.DashboardTaskQueryDTO;
import com.asterway.dashboard.mapper.DashboardMapper;
import com.asterway.dashboard.service.DashboardService;
import com.asterway.dashboard.vo.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 同一只读快照下组合今日事实，各业务数据仍由原模块持有。 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper mapper;
    private final GrowthContextUtils access;
    private final UserCalendarUtils calendar;

    public DashboardServiceImpl(
            DashboardMapper mapper, GrowthContextUtils access, UserCalendarUtils calendar) {
        this.mapper = mapper;
        this.access = access;
        this.calendar = calendar;
    }

    private DashboardReadDTO context() {
        long userId = access.read("dashboard:board:read");
        var day = calendar.read(userId);
        return new DashboardReadDTO(
                userId,
                day.today(),
                day.zone().getId(),
                day.now(),
                SecurityUtils.current().sessionId());
    }

    @Override
    public DashboardOverviewVO overview() {
        var ctx = context();
        return new DashboardOverviewVO(
                ctx.today(), ctx.timeZone(), ctx.now(), mapper.overview(ctx), mapper.current(ctx));
    }

    @Override
    public PageResultVO<DashboardTaskVO> tasks(DashboardTaskQueryDTO query) {
        var ctx = context();
        String section = query.getSection();
        if (section != null && !"TODAY".equals(section) && !"WEEK_UNDATED".equals(section)) {
            throw BusinessException.badRequest("任务分组必须为 TODAY 或 WEEK_UNDATED");
        }
        return new PageResultVO<>(
                mapper.tasks(ctx, section, query.offset(), query.getSize()),
                mapper.tasksCount(ctx, section),
                query.getPage(),
                query.getSize());
    }

    @Override
    public PageResultVO<DashboardGoalVO> goals(PageQueryDTO query) {
        var ctx = context();
        return new PageResultVO<>(
                mapper.goals(ctx, query.offset(), query.getSize()),
                mapper.goalsCount(ctx),
                query.getPage(),
                query.getSize());
    }

    @Override
    public PageResultVO<DashboardHabitVO> habits(PageQueryDTO query) {
        var ctx = context();
        return new PageResultVO<>(
                mapper.habits(ctx, query.offset(), query.getSize()),
                mapper.habitsCount(ctx),
                query.getPage(),
                query.getSize());
    }
}
