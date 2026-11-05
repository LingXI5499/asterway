package com.asterway.stage.service;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.common.dto.GrowthReorderDTO;
import com.asterway.common.dto.GrowthStatusDTO;
import com.asterway.common.dto.PageQueryDTO;
import com.asterway.common.result.PageResultVO;
import com.asterway.stage.dto.StageSaveDTO;
import com.asterway.stage.vo.StageStateHistoryVO;
import com.asterway.stage.vo.StageVO;

/** Stage 的完整业务链路，不按增删改查拆分。 */
public interface StageService {
    /** 本人状态事实分页，归档或逻辑删除后仍保留历史读取。 */
    PageResultVO<StageStateHistoryVO> history(long id, PageQueryDTO query);

    /** 当前用户范围内的业务操作。 */
    PageResultVO<StageVO> page(GrowthQueryDTO query);

    /** 当前用户范围内的业务操作。 */
    StageVO get(long id);

    /** 当前用户范围内的业务操作。 */
    StageVO create(long parentId, StageSaveDTO dto);

    /** 当前用户范围内的业务操作。 */
    StageVO update(long id, StageSaveDTO dto);

    /** 当前用户范围内的业务操作。 */
    /** 完整替换资料，可清空可选日期。 */
    StageVO replace(long id, StageSaveDTO dto);

    StageVO status(long id, GrowthStatusDTO dto);

    /** 当前用户范围内的业务操作。 */
    void delete(long id, long version);

    /** 当前用户范围内的业务操作。 */
    void reorder(long parentId, GrowthReorderDTO dto);
}
