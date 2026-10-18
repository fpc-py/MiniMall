package com.mall.ai.service;

import cn.hutool.json.JSONUtil;
import com.mall.ai.config.AiProperties;
import com.mall.ai.dto.AiChatContext;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.tool.AiProgressNotifier;
import com.mall.ai.tool.AiToolContextKeys;
import com.mall.ai.tool.FaqAgentTools;
import com.mall.ai.tool.HandoffAgentTools;
import com.mall.ai.tool.OrderAgentTools;
import com.mall.ai.tool.ProductAgentTools;
import com.mall.ai.tool.RefundAgentTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能客服编排：装配历史 → ChatClient 流式（框架托管工具循环）→ SSE 事件逐字回推 → 落库。
 * <p>工具执行循环由 Spring AI 框架托管，本类不自己写 Agent 循环。
 */
@Slf4j
@Service
public class AiChatService {

    private static final String EVENT_META = "meta";
    private static final String EVENT_DELTA = "delta";
    private static final String EVENT_DONE = "done";
    private static final String EVENT_ERROR = "error";
    private static final String TOOL_HANDOFF = "handoff_to_human";
    private static final String AI_UNAVAILABLE_MSG = "智能客服暂时不可用，请稍后再试。";

    private final ChatClient chatClient;
    private final AiConversationService conversationService;
    private final AiProperties aiProperties;
    private final ExecutorService aiExecutor;
    private final OrderAgentTools orderAgentTools;
    private final RefundAgentTools refundAgentTools;
    private final ProductAgentTools productAgentTools;
    private final FaqAgentTools faqAgentTools;
    private final HandoffAgentTools handoffAgentTools;

    public AiChatService(ChatClient chatClient,
                         AiConversationService conversationService,
                         AiProperties aiProperties,
                         @Qualifier("aiExecutor") ExecutorService aiExecutor,
                         OrderAgentTools orderAgentTools,
                         RefundAgentTools refundAgentTools,
                         ProductAgentTools productAgentTools,
                         FaqAgentTools faqAgentTools,
                         HandoffAgentTools handoffAgentTools) {
        this.chatClient = chatClient;
        this.conversationService = conversationService;
        this.aiProperties = aiProperties;
        this.aiExecutor = aiExecutor;
        this.orderAgentTools = orderAgentTools;
        this.refundAgentTools = refundAgentTools;
        this.productAgentTools = productAgentTools;
        this.faqAgentTools = faqAgentTools;
        this.handoffAgentTools = handoffAgentTools;
    }

    /**
     * 发起一轮流式对话，返回 SSE 连接。实际推流在独立线程池执行，避免阻塞 Servlet 线程。
     */
    public SseEmitter stream(Long memberId, AiChatRequest request) {
        SseEmitter emitter = new SseEmitter((long) aiProperties.getTimeoutSeconds() * 2000);
        aiExecutor.execute(() -> run(memberId, request, emitter));
        return emitter;
    }

    private void run(Long memberId, AiChatRequest request, SseEmitter emitter) {
        AtomicBoolean handoff = new AtomicBoolean(false);
        AiProgressNotifier notifier = (type, payload) -> {
            if ("tool".equals(type) && payload instanceof Map<?, ?> map
                    && TOOL_HANDOFF.equals(map.get("name")) && "running".equals(map.get("status"))) {
                handoff.set(true);
            }
            sendEvent(emitter, type, payload);
        };

        try {
            AiChatContext context = conversationService.prepare(memberId, request);
            sendEvent(emitter, EVENT_META, Map.of(
                    "conversationId", String.valueOf(context.conversationId()),
                    "messageId", String.valueOf(context.userMessageId())));

            StringBuilder answer = new StringBuilder();
            AtomicReference<Throwable> failure = new AtomicReference<>();

            chatClient.prompt()
                    .messages(context.history())
                    .tools(orderAgentTools, refundAgentTools, productAgentTools, faqAgentTools, handoffAgentTools)
                    .toolContext(Map.<String, Object>of(
                            AiToolContextKeys.USER_ID, memberId,
                            AiToolContextKeys.CONVERSATION_ID, context.conversationId(),
                            AiToolContextKeys.NOTIFIER, notifier))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            answer.append(chunk);
                            sendEvent(emitter, EVENT_DELTA, Map.of("content", chunk));
                        }
                    })
                    .doOnError(failure::set)
                    .onErrorResume(error -> Flux.empty())
                    .blockLast();

            if (failure.get() != null) {
                log.warn("智能客服流式回答失败: conversationId={}", context.conversationId(), failure.get());
                sendEvent(emitter, EVENT_ERROR, Map.of("msg", AI_UNAVAILABLE_MSG));
            } else {
                String text = answer.toString();
                Long assistantMessageId = conversationService.appendAssistant(
                        context.conversationId(), memberId, text);
                sendEvent(emitter, EVENT_DONE, Map.of(
                        "messageId", String.valueOf(assistantMessageId),
                        "handoff", handoff.get()));
            }
        } catch (Exception e) {
            log.warn("智能客服处理失败: {}", e.getMessage(), e);
            sendEvent(emitter, EVENT_ERROR, Map.of("msg", AI_UNAVAILABLE_MSG));
        } finally {
            complete(emitter);
        }
    }

    private void sendEvent(SseEmitter emitter, String type, Object payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        if (payload instanceof Map<?, ?> map) {
            map.forEach((key, value) -> event.put(String.valueOf(key), value));
        } else if (payload != null) {
            event.put("data", payload);
        }
        try {
            emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(event)));
        } catch (Exception e) {
            // 客户端主动断开时属正常情况，不刷错误日志
            log.debug("SSE 发送失败(type={}): {}", type, e.getMessage());
        }
    }

    private void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("SSE 关闭失败: {}", e.getMessage());
        }
    }
}
