package com.mall.ai.controller;

import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.service.AiChatService;
import com.mall.ai.service.AiConversationService;
import com.mall.ai.service.AiFaqService;
import com.mall.ai.vo.AiConversationVO;
import com.mall.ai.vo.AiMessageVO;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 买家端 - 智能客服。memberId 一律取自 Token，不接受前端传入。
 */
@Tag(name = "买家端 - 智能客服")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    /** 快捷提问展示条数 */
    private static final int HOT_FAQ_LIMIT = 6;

    private final AiChatService aiChatService;
    private final AiConversationService aiConversationService;
    private final AiFaqService aiFaqService;

    @Operation(summary = "流式对话", description = "SSE 逐字返回；事件类型 meta/tool/products/delta/done/error")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@CurrentUser LoginUser user, @Valid @RequestBody AiChatRequest request) {
        return aiChatService.stream(user.userId(), request);
    }

    @Operation(summary = "我的客服会话", description = "返回最近会话，供客服页历史列表使用")
    @GetMapping("/conversations")
    public R<List<AiConversationVO>> conversations(@CurrentUser LoginUser user) {
        return R.ok(aiConversationService.listMine(user.userId()));
    }

    @Operation(summary = "会话消息", description = "返回指定会话的消息列表，做归属校验")
    @GetMapping("/conversations/{conversationId}/messages")
    public R<List<AiMessageVO>> messages(@CurrentUser LoginUser user,
                                         @PathVariable("conversationId") Long conversationId) {
        return R.ok(aiConversationService.listMessages(user.userId(), conversationId));
    }

    @Operation(summary = "热门问题", description = "客服页快捷提问 chips")
    @GetMapping("/faq/hot")
    public R<List<String>> hotFaq() {
        return R.ok(aiFaqService.hotQuestions(HOT_FAQ_LIMIT));
    }
}
