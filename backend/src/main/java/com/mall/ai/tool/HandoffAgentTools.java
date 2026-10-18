package com.mall.ai.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiHandoff;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiHandoffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 转人工工具：写 ai_handoff 工单 + 会话置为「已转人工」，同一事务内完成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandoffAgentTools {

    /** 会话状态：1 已转人工 */
    private static final int CONVERSATION_STATUS_HANDOFF = 1;

    /** 工单状态：0 待处理 */
    private static final int HANDOFF_STATUS_PENDING = 0;

    private final AiHandoffMapper handoffMapper;
    private final AiConversationMapper conversationMapper;

    @Tool(name = "handoff_to_human",
            description = "转人工客服。当用户明确要求转人工，或问题涉及投诉、纠纷、账号安全等无法自动处理的事项时调用。",
            resultConverter = AiTextResultConverter.class)
    @Transactional(rollbackFor = Exception.class)
    public String handoffToHuman(
            @ToolParam(required = false, description = "转人工原因，可传用户原话") String reason,
            @ToolParam(required = false, description = "用户联系方式，用户主动提供时才传入") String contact,
            ToolContext toolContext) {
        Long memberId = AiToolSupport.userId(toolContext);
        if (memberId == null) {
            return "请先登录后再联系人工客服。";
        }
        Long conversationId = AiToolSupport.conversationId(toolContext);
        AiProgressNotifier notifier = AiToolSupport.notifier(toolContext);
        notifier.tool("handoff_to_human", "running");
        try {
            AiHandoff handoff = new AiHandoff();
            handoff.setConversationId(conversationId);
            handoff.setMemberId(memberId);
            handoff.setReason(reason);
            handoff.setContact(contact);
            handoff.setStatus(HANDOFF_STATUS_PENDING);
            handoffMapper.insert(handoff);

            if (conversationId != null) {
                conversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                        .set(AiConversation::getStatus, CONVERSATION_STATUS_HANDOFF)
                        .eq(AiConversation::getId, conversationId)
                        .eq(AiConversation::getMemberId, memberId));
            }
            log.info("转人工工单已创建: handoffId={}, memberId={}, conversationId={}",
                    handoff.getId(), memberId, conversationId);
            return "已为您转接人工客服，工作时间每日 9:00-21:00，客服会尽快与您联系。"
                    + "您也可以留下手机号或邮箱，方便我们主动联系您。";
        } finally {
            notifier.tool("handoff_to_human", "done");
        }
    }
}
