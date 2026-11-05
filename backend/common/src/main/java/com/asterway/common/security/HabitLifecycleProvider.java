package com.asterway.common.security;

import java.time.Instant;

/** 账户生命周期对习惯的中立联动，必须加入父操作事务并先锁同一用户。 */
public interface HabitLifecycleProvider {
    /** 停用、删除账户或转为管理身份时暂停 ACTIVE 习惯，不自动恢复。 */
    void suspendPersonal(long userId);

    /** 清除已生效的待变时区前封存旧时区最后一天，防止回拨日期重开历史。 */
    void sealBeforeZoneSwitch(long userId, Instant effectiveAt, String previousZone);
}
