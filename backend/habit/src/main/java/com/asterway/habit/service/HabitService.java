package com.asterway.habit.service;

import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.habit.dto.*;
import com.asterway.habit.vo.*;

/** 习惯主链，规则、生命周期和从属打卡在此统一维护。 */
public interface HabitService {
    /** 分页读取本人习惯，默认不含结束归档项。 */
    PageResultVO<HabitVO> page(HabitQueryDTO query);

    /** 读取当前资料与当日打卡状态。 */
    HabitVO get(long id);

    /** 创建独立习惯及首个完整规则快照。 */
    HabitVO create(HabitSaveDTO dto);

    /** 部分修改或完整替换规则；只对当前未封存日期生效。 */
    HabitVO update(long id, HabitSaveDTO dto, boolean replace);

    /** 暂停 ACTIVE 习惯，记录生命周期快照。 */
    HabitVO pause(long id, long version);

    /** 恢复 PAUSED 习惯，ENDED 须使用重新开启。 */
    HabitVO resume(long id, long version);

    /** 结束并归档习惯，保留已完成日和过去规则。 */
    HabitVO end(long id, long version);

    /** 重新开启 ENDED 习惯，不恢复逻辑删除项。 */
    HabitVO reopen(long id, long version);

    /** 逻辑删除习惯并追加删除快照，事实继续保留。 */
    void delete(long id, long version);

    /** 分页读取今日应做或已有打卡的习惯。 */
    PageResultVO<HabitTodayVO> today(PageQueryDTO query);

    /** 为今天打卡，版本指习惯版本；已有有效记录时幂等返回。 */
    HabitCheckinVO checkin(long id, long habitVersion);

    /** 撤销今天的指定打卡，版本指打卡版本，不允许改写历史。 */
    HabitCheckinVO cancel(long id, long checkinId, long version);

    /** 分页读取本人原始打卡和撤销事实，删除后仍可查。 */
    PageResultVO<HabitCheckinVO> checkins(long id, HabitCheckinQueryDTO query);

    /** 分页读取完整规则和生命周期修订，不提供修改历史接口。 */
    PageResultVO<HabitRevisionVO> revisions(long id, PageQueryDTO query);
}
