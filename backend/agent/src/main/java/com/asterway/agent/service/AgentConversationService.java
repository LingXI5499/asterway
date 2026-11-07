package com.asterway.agent.service;

import com.asterway.agent.dto.AgentConversationCreateDTO;
import com.asterway.agent.dto.AgentConversationMessageDTO;
import com.asterway.agent.dto.AgentConversationQueryDTO;
import com.asterway.agent.dto.AgentConversationTitleDTO;
import com.asterway.agent.vo.AgentConversationDetailVO;
import com.asterway.agent.vo.AgentConversationVO;
import com.asterway.agent.vo.AgentMessagePageVO;
import com.asterway.agent.vo.AgentSendResultVO;
import com.asterway.agent.vo.AgentTurnVO;
import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.result.PageResultVO;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 会话持久化代理；Java 只做认证、权限和转发，不持有 Agent 表。 */
public interface AgentConversationService {

    AgentConversationDetailVO create(AgentConversationCreateDTO dto);

    PageResultVO<AgentConversationVO> page(AgentConversationQueryDTO query);

    AgentConversationDetailVO detail(long conversationId);

    AgentConversationDetailVO rename(long conversationId, AgentConversationTitleDTO dto);

    AgentConversationDetailVO close(long conversationId, GrowthVersionDTO dto);

    void delete(long conversationId, long version);

    AgentMessagePageVO messages(long conversationId, Long beforeMessageNo, int size);

    AgentTurnVO turn(long conversationId, long turnId);

    AgentTurnVO cancel(long conversationId, long turnId, GrowthVersionDTO dto);

    AgentSendResultVO send(long conversationId, AgentConversationMessageDTO dto);

    StreamingResponseBody stream(long conversationId, AgentConversationMessageDTO dto);
}
