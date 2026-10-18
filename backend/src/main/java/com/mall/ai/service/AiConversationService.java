package com.mall.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.ai.config.AiProperties;
import com.mall.ai.dto.AiChatContext;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiMessage;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiMessageMapper;
import com.mall.ai.vo.AiConversationVO;
import com.mall.ai.vo.AiMessageVO;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话与消息的持久化服务。
 * <p>单独成类（而非并入 AiChatService）是为了保证「写操作 @Transactional」在流式回调线程中仍通过代理生效，
 * 避免同类内部调用导致事务失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    /** 会话状态：0 进行中 */
    private static final int CONVERSATION_STATUS_ACTIVE = 0;
    /** 消息角色：1 用户 */
    private static final int ROLE_USER = 1;
    /** 消息角色：2 客服 */
    private static final int ROLE_ASSISTANT = 2;

    private static final int CONVERSATION_LIST_LIMIT = 20;
    private static final int MESSAGE_LIST_LIMIT = 200;
    private static final int TITLE_MAX_LENGTH = 30;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiProperties aiProperties;

    /**
     * 开轮准备（同一事务）：校验会话归属 → 新建会话（如需）→ 落库用户消息 → 装配历史。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiChatContext prepare(Long memberId, AiChatRequest request) {
        Long conversationId = request.getConversationId();
        if (conversationId == null) {
            AiConversation conversation = new AiConversation();
            conversation.setMemberId(memberId);
            conversation.setTitle(summary(request.getContent()));
            conversation.setStatus(CONVERSATION_STATUS_ACTIVE);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setMessageCount(0);
            conversationMapper.insert(conversation);
            conversationId = conversation.getId();
        } else {
            requireOwned(memberId, conversationId);
        }

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setMemberId(memberId);
        userMessage.setRole(ROLE_USER);
        userMessage.setContent(request.getContent());
        messageMapper.insert(userMessage);

        touchConversation(conversationId, memberId);

        List<Message> history = loadHistory(conversationId);
        return new AiChatContext(conversationId, userMessage.getId(), history);
    }

    /**
     * 落库客服回答（同一事务），返回客服消息ID。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long appendAssistant(Long conversationId, Long memberId, String content) {
        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setMemberId(memberId);
        assistantMessage.setRole(ROLE_ASSISTANT);
        assistantMessage.setContent(content);
        messageMapper.insert(assistantMessage);

        touchConversation(conversationId, memberId);
        return assistantMessage.getId();
    }

    /** 我的最近会话 */
    public List<AiConversationVO> listMine(Long memberId) {
        List<AiConversation> conversations = conversationMapper.selectList(Wrappers.<AiConversation>lambdaQuery()
                .eq(AiConversation::getMemberId, memberId)
                .orderByDesc(AiConversation::getLastMessageTime)
                .orderByDesc(AiConversation::getId)
                .last("LIMIT " + CONVERSATION_LIST_LIMIT));
        return conversations.stream().map(this::toConversationVO).toList();
    }

    /** 会话消息列表（归属校验，非本人抛 3xxxx） */
    public List<AiMessageVO> listMessages(Long memberId, Long conversationId) {
        requireOwned(memberId, conversationId);
        List<AiMessage> messages = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getId)
                .last("LIMIT " + MESSAGE_LIST_LIMIT));
        return messages.stream().map(this::toMessageVO).toList();
    }

    /** 归属校验：不存在抛 AI_CONVERSATION_NOT_FOUND，非本人抛 AI_CONVERSATION_FORBIDDEN */
    private AiConversation requireOwned(Long memberId, Long conversationId) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new MallException(ErrorCode.Biz.AI_CONVERSATION_NOT_FOUND);
        }
        if (!conversation.getMemberId().equals(memberId)) {
            throw new MallException(ErrorCode.Biz.AI_CONVERSATION_FORBIDDEN);
        }
        return conversation;
    }

    private void touchConversation(Long conversationId, Long memberId) {
        conversationMapper.update(null, Wrappers.<AiConversation>lambdaUpdate()
                .setSql("message_count = message_count + 1")
                .set(AiConversation::getLastMessageTime, LocalDateTime.now())
                .eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getMemberId, memberId));
    }

    /** 取最近 historyLimit 条消息（含本轮用户消息），按时间正序转为 Spring AI 消息 */
    private List<Message> loadHistory(Long conversationId) {
        int limit = Math.max(aiProperties.getHistoryLimit(), 1);
        List<AiMessage> messages = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getId)
                .last("LIMIT " + limit));
        Collections.reverse(messages);
        List<Message> history = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            if (message.getRole() != null && message.getRole() == ROLE_ASSISTANT) {
                history.add(new AssistantMessage(message.getContent()));
            } else {
                history.add(new UserMessage(message.getContent()));
            }
        }
        return history;
    }

    private String summary(String content) {
        if (!StringUtils.hasText(content)) {
            return "新会话";
        }
        String trimmed = content.trim();
        return trimmed.length() <= TITLE_MAX_LENGTH ? trimmed : trimmed.substring(0, TITLE_MAX_LENGTH);
    }

    private AiConversationVO toConversationVO(AiConversation conversation) {
        AiConversationVO vo = new AiConversationVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setStatus(conversation.getStatus());
        vo.setLastMessageTime(conversation.getLastMessageTime());
        vo.setMessageCount(conversation.getMessageCount());
        return vo;
    }

    private AiMessageVO toMessageVO(AiMessage message) {
        AiMessageVO vo = new AiMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
