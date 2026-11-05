package com.asterway.habit.service;

import com.asterway.common.result.PageResultVO;
import com.asterway.habit.dto.HabitCategorySaveDTO;
import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.vo.HabitCategoryVO;

/** 用户自有分类的一条完整业务链路。 */
public interface HabitCategoryService {
    /** 分页读取本人仍有效的分类。 */
    PageResultVO<HabitCategoryVO> page(HabitQueryDTO query);

    /** 创建本人分类，同用户有效名称不重复。 */
    HabitCategoryVO create(HabitCategorySaveDTO dto);

    /** 按版本修改分类名称和顺序；无变化不推进版本。 */
    HabitCategoryVO update(long id, HabitCategorySaveDTO dto);

    /** 逻辑删除分类，保留既有习惯和历史引用。 */
    void delete(long id, long version);
}
