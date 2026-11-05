package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.mapper.GrowthSupportMapper;
import com.asterway.common.security.HabitLifecycleProvider;
import com.asterway.common.security.UserCalendarData;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 按账号延后生效时区生成业务日历；不使用服务器 CURRENT_DATE 推断用户日期。 */
@Component
public class UserCalendarUtils {
    private final GrowthSupportMapper mapper;
    private final ObjectProvider<HabitLifecycleProvider> habits;

    public UserCalendarUtils(
            GrowthSupportMapper mapper, ObjectProvider<HabitLifecycleProvider> habits) {
        this.mapper = mapper;
        this.habits = habits;
    }

    public UserCalendarData read(long userId) {
        return context(userId, false);
    }

    /** 调用方已持有用户锁，待生效时区尚未被账户接口落库时也先封存历史。 */
    public UserCalendarData write(long userId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("日历写操作必须在用户锁事务内");
        }
        return context(userId, true);
    }

    private UserCalendarData context(long userId, boolean writing) {
        var account = mapper.calendarAccount(userId);
        if (account == null || !account.enabled()) {
            throw BusinessException.unauthorized();
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        if (writing
                && account.pendingTimeZone() != null
                && !account.timeZoneEffectiveAt().isAfter(now)) {
            habits.getObject()
                    .sealBeforeZoneSwitch(
                            userId, account.timeZoneEffectiveAt(), account.timeZone());
        }
        return new UserCalendarData(userId, account.effectiveZone(now), now);
    }
}
