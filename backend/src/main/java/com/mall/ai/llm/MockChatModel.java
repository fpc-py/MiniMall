package com.mall.ai.llm;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无真实模型 key 时的降级实现（mall.ai.mock-enabled=true）。
 * <p>不发起任何外部调用：按关键词做意图路由，直接调用 ChatClient 传入的同一批 {@link ToolCallback}
 * （即真实的 @Tool 方法），把工具返回的文本作为答案按字符切片产出，模拟逐字流式输出。
 * <p>与真实模型路径共用同一批工具实例与同一套 SSE 协议，保证两条路径行为一致。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mall.ai.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockChatModel implements ChatModel {

    /** 逐字输出节奏（毫秒/字） */
    private static final long CHUNK_DELAY_MILLIS = 20L;

    /** 订单号形态：14 位时间 + 6 位序号 + 2 位随机 = 22 位纯数字 */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\d{16,24}");

    private static final String FALLBACK_REPLY = "抱歉，我暂时没有理解您的问题。您可以换个说法，或回复「转人工」由人工客服为您服务。";

    private static final String[] HANDOFF_WORDS = {"转人工", "人工客服", "人工", "客服电话", "投诉", "找客服"};

    private static final String[] FAQ_ONLY_WORDS = {"政策", "怎么", "如何", "支持", "可以吗", "规定", "多久", "多少"};

    private static final String[] REFUND_WORDS = {"退款", "退货", "售后", "退钱", "退换"};

    private static final String[] ORDER_WORDS = {"订单", "物流", "快递", "发货", "到哪", "签收", "收货", "运单", "揽收"};

    private static final String[] PRODUCT_WORDS = {"商品", "买", "多少钱", "价格", "推荐", "有货", "手机", "电脑", "笔记本",
            "耳机", "电视", "空调", "显示器", "手表", "精华", "洗面奶", "跑鞋", "零食", "夹克", "红枣"};

    /** 商品检索前需要剔除的口语化填充词，避免整句当关键词导致 LIKE 匹配不到任何商品 */
    private static final String[] PRODUCT_FILLER = {"推荐", "一款", "我想买", "想买", "帮我找", "帮我", "给我", "我", "你",
            "请", "有没有", "有卖", "有货吗", "有货", "什么牌子", "多少钱", "价格", "的", "吗", "呢", "吧",
            "？", "?", "！", "!", "，", ",", "。", "."};

    /**
     * 必须返回 {@link ToolCallingChatOptions}：DefaultChatClientUtils 仅在请求选项是该类型时，
     * 才会把 {@code .tools(...)} / {@code .toolContext(...)} 合并进 Prompt，否则工具回调被静默丢弃。
     */
    @Override
    public ChatOptions getDefaultOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return toResponse(buildReply(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String reply = buildReply(prompt);
        return Flux.fromIterable(splitToChunks(reply))
                .delayElements(Duration.ofMillis(CHUNK_DELAY_MILLIS))
                .map(this::toResponse);
    }

    private ChatResponse toResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    // ==================== 意图路由 ====================

    private String buildReply(Prompt prompt) {
        String question = lastUserText(prompt);
        List<ToolCallback> callbacks = new ArrayList<>();
        Map<String, Object> context = Map.of();
        if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
            if (options.getToolCallbacks() != null) {
                callbacks = options.getToolCallbacks();
            }
            if (options.getToolContext() != null) {
                context = options.getToolContext();
            }
        }

        String orderNo = extractOrderNo(question);
        if (containsAny(question, HANDOFF_WORDS)) {
            return invoke("handoff_to_human", Map.of("reason", question), callbacks, context, FALLBACK_REPLY);
        }
        if (containsAny(question, REFUND_WORDS)) {
            // 纯政策咨询（无订单号）走 FAQ，其余查退款进度
            if (orderNo == null && containsAny(question, FAQ_ONLY_WORDS)) {
                return invoke("search_faq", Map.of("keyword", question), callbacks, context, FALLBACK_REPLY);
            }
            return invoke("query_refund", orderNo == null ? Map.of() : Map.of("orderNo", orderNo),
                    callbacks, context, FALLBACK_REPLY);
        }
        if (containsAny(question, ORDER_WORDS)) {
            return invoke("query_order", orderNo == null ? Map.of() : Map.of("orderNo", orderNo),
                    callbacks, context, FALLBACK_REPLY);
        }
        if (containsAny(question, PRODUCT_WORDS)) {
            return invoke("search_product", Map.of("keyword", extractKeyword(question)), callbacks, context, FALLBACK_REPLY);
        }
        return invoke("search_faq", Map.of("keyword", question), callbacks, context, FALLBACK_REPLY);
    }

    /**
     * 调用真实工具并把结果作为答案返回。工具缺失或执行异常时回退到兜底话术，不向外抛错。
     */
    private String invoke(String toolName, Map<String, Object> args, List<ToolCallback> callbacks,
                          Map<String, Object> context, String fallback) {
        ToolCallback callback = callbacks.stream()
                .filter(c -> toolName.equals(c.getToolDefinition().name()))
                .findFirst()
                .orElse(null);
        if (callback == null) {
            log.warn("Mock 模式未找到工具: {}", toolName);
            return fallback;
        }
        try {
            String result = callback.call(JSONUtil.toJsonStr(args), new ToolContext(context));
            return result == null || result.isBlank() ? fallback : result;
        } catch (Exception e) {
            log.warn("Mock 模式调用工具 {} 失败: {}", toolName, e.getMessage());
            return "抱歉，查询时出了点小问题，请稍后再试，或回复「转人工」由人工客服为您处理。";
        }
    }

    // ==================== 工具方法 ====================

    private String lastUserText(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();
        for (int i = instructions.size() - 1; i >= 0; i--) {
            Message message = instructions.get(i);
            if (message.getMessageType() == MessageType.USER) {
                String text = message.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return "";
    }

    private String extractOrderNo(String question) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(question);
        return matcher.find() ? matcher.group() : null;
    }

    /** 剔除口语化填充词得到检索关键词；剔除后过短则回退为原句，避免误伤 */
    private String extractKeyword(String question) {
        String keyword = question;
        for (String filler : PRODUCT_FILLER) {
            keyword = keyword.replace(filler, "");
        }
        keyword = keyword.trim();
        return keyword.length() >= 2 ? keyword : question;
    }

    private boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** 按字符切片，保持中文逐字观感（按码点切分，兼容 emoji 等代理对） */
    private List<String> splitToChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int count = Character.charCount(codePoint);
            chunks.add(text.substring(index, index + count));
            index += count;
        }
        return chunks;
    }
}
