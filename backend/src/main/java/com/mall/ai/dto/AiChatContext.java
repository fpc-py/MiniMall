package com.mall.ai.dto;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 单轮对话的上下文（服务端内部使用，不出参）。
 *
 * @param conversationId 会话ID
 * @param userMessageId  本轮用户消息ID（meta 事件回传给前端）
 * @param history        装配给模型的历史消息（含本轮用户消息）
 */
public record AiChatContext(Long conversationId, Long userMessageId, List<Message> history) {
}
