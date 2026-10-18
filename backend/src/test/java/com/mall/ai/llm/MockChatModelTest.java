package com.mall.ai.llm;

import com.mall.ai.tool.AiTextResultConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mock 模型测试：意图路由到正确的工具、工具缺失时兜底、逐字流式切片。
 * <p>其中 {@link #defaultOptionsSupportsToolCalling()} 是回归守卫：必须返回 {@link ToolCallingChatOptions}，
 * 否则 ChatClient 会因为选项类型不匹配而静默丢弃工具回调（历史缺陷）。
 */
class MockChatModelTest {

    private static final String FALLBACK_REPLY = "抱歉，我暂时没有理解您的问题。您可以换个说法，或回复「转人工」由人工客服为您服务。";

    private MockChatModel chatModel;
    private List<ToolCallback> callbacks;

    @BeforeEach
    void setUp() {
        chatModel = new MockChatModel();
        callbacks = List.of(MethodToolCallbackProvider.builder()
                .toolObjects(new StubTools())
                .build()
                .getToolCallbacks());
    }

    private Prompt prompt(String question, boolean withCallbacks) {
        ToolCallingChatOptions.Builder builder = ToolCallingChatOptions.builder()
                .toolContext(Map.of("userId", 1L));
        if (withCallbacks) {
            builder.toolCallbacks(callbacks);
        }
        return new Prompt(List.of(new UserMessage(question)), builder.build());
    }

    private String reply(String question) {
        ChatResponse response = chatModel.call(prompt(question, true));
        return response.getResult().getOutput().getText();
    }

    @Test
    @DisplayName("默认选项必须是 ToolCallingChatOptions，否则工具回调会被静默丢弃")
    void defaultOptionsSupportsToolCalling() {
        ChatOptions defaultOptions = chatModel.getDefaultOptions();

        assertThat(defaultOptions).isInstanceOf(ToolCallingChatOptions.class);
    }

    @Test
    @DisplayName("问订单物流：路由到 query_order，并带上提取出的订单号")
    void routesOrderQuestionToQueryOrder() {
        assertThat(reply("订单12345678901234567890到哪了？"))
                .isEqualTo("订单物流:12345678901234567890");
    }

    @Test
    @DisplayName("要求人工：路由到 handoff_to_human")
    void routesHandoffQuestionToHandoffTool() {
        assertThat(reply("我要转人工")).isEqualTo("已转人工");
    }

    @Test
    @DisplayName("推荐商品：剥离口语填充词后再检索")
    void routesProductQuestionWithFillerStripped() {
        assertThat(reply("推荐一款手机")).isEqualTo("商品:手机");
    }

    @Test
    @DisplayName("未注入工具回调：回退兜底话术，不抛错")
    void fallsBackWhenToolMissing() {
        ChatResponse response = chatModel.call(prompt("我的订单到哪了？", false));

        assertThat(response.getResult().getOutput().getText()).isEqualTo(FALLBACK_REPLY);
    }

    @Test
    @DisplayName("流式输出：按码点逐字切片，拼接后与整段回答一致")
    void streamsReplyCharacterByCharacter() {
        List<String> chunks = chatModel.stream(prompt("我要转人工", true))
                .map(response -> response.getResult().getOutput().getText())
                .collectList()
                .block();

        assertThat(chunks).isNotNull().hasSize(4);
        assertThat(String.join("", chunks)).isEqualTo("已转人工");
    }

    /** 测试用工具集合：方法与真实 @Tool 一致地使用纯文本结果转换器。 */
    static class StubTools {

        @Tool(name = "query_order", description = "查询订单物流", resultConverter = AiTextResultConverter.class)
        public String queryOrder(@ToolParam(description = "订单号", required = false) String orderNo) {
            return orderNo == null ? "暂无在途订单" : "订单物流:" + orderNo;
        }

        @Tool(name = "handoff_to_human", description = "转人工客服", resultConverter = AiTextResultConverter.class)
        public String handoff(@ToolParam(description = "转人工原因") String reason) {
            return "已转人工";
        }

        @Tool(name = "search_faq", description = "检索帮助中心", resultConverter = AiTextResultConverter.class)
        public String searchFaq(@ToolParam(description = "问题关键词") String keyword) {
            return "FAQ:" + keyword;
        }

        @Tool(name = "search_product", description = "检索商品", resultConverter = AiTextResultConverter.class)
        public String searchProduct(@ToolParam(description = "商品关键词") String keyword) {
            return "商品:" + keyword;
        }

        @Tool(name = "query_refund", description = "查询退款进度", resultConverter = AiTextResultConverter.class)
        public String queryRefund(@ToolParam(description = "订单号", required = false) String orderNo) {
            return orderNo == null ? "暂无退款单" : "退款:" + orderNo;
        }
    }
}
