package com.asterway.agent.vo;

import java.util.List;

/** 历史消息游标页；items 按消息序号升序返回。 */
public record AgentMessagePageVO(
        List<AgentMessageVO> items, boolean hasMore, Long nextBeforeMessageNo) {}
