package com.asterway.execution.vo;

import com.asterway.common.result.PageResultVO;

/** 会话详情及首屏时间段，可使用专属分页接口继续读取。 */
public record ExecutionDetailVO(
        ExecutionSessionVO execution, PageResultVO<ExecutionSegmentVO> segments) {}
