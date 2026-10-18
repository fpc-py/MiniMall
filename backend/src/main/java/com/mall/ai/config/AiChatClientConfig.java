package com.mall.ai.config;

import com.mall.ai.llm.MockChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 装配：mock 模式与真实模型模式互斥，避免 Bean 歧义。
 * <p>mock 模式（mall.ai.mock-enabled=true）：使用自研 MockChatModel，无外部调用即可跑通全链路；
 * 真实模式（false）：使用 Spring AI 自动装配的 ChatModel（OpenAI 兼容，需 application-local.yml 打开
 * {@code spring.ai.model.chat=openai} 并提供 api-key）。
 */
@Configuration
public class AiChatClientConfig {

    /** 客服系统提示词：限定身份、工具使用时机、语气与安全边界 */
    public static final String SYSTEM_PROMPT = """
            你是「商城」App 的智能客服助手，服务于已登录的买家。
            你的职责：用简洁、友好、口语化的中文回答用户关于商品、订单、物流、退款、优惠券、账号的问题。

            你可以调用以下工具来获取真实数据，务必遵守使用时机：
            - search_faq：用户询问政策类问题（如退换货规则、发货时效、运费、发票、退款到账时间、优惠券用法）时优先调用。
            - query_order：用户询问自己的订单状态、物流、发货、签收情况时调用。可带订单号；不带订单号时按状态列出最近订单。
            - query_refund：用户询问退款进度、审核结果时调用。
            - search_product：用户想找商品、问价格、要推荐时调用，可返回商品卡片。
            - handoff_to_human：用户明确要求转人工，或问题涉及投诉、纠纷、账号安全等你无法处理的事项时调用。

            回答规则：
            1. 只依据工具返回的结果回答，绝不编造订单号、金额、物流单号或政策条款。
            2. 工具查询不到数据时，如实告知「未查询到」，并引导用户核对信息或转人工，不要沉默也不要编造。
            3. 任何情况下都不得透露其他用户的信息；查询结果为空一律按「未查询到」处理。
            4. 涉及金钱、时间承诺时使用「一般」「通常」「预计」等措辞，不做绝对承诺。
            5. 回答控制在 200 字以内，能分点就分点，不要输出 Markdown 表格；金额需带「元」。
            6. 若工具返回商品卡片信息，用一两句话概括即可，不要逐条罗列全部字段。
            """;

    @Bean
    @ConditionalOnProperty(name = "mall.ai.mock-enabled", havingValue = "true", matchIfMissing = true)
    public ChatClient mockChatClient(MockChatModel mockChatModel) {
        return ChatClient.builder(mockChatModel).defaultSystem(SYSTEM_PROMPT).build();
    }

    @Bean
    @ConditionalOnProperty(name = "mall.ai.mock-enabled", havingValue = "false")
    public ChatClient openAiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * FAQ 向量库（内存实现，无需新增中间件）。
     * 仅在配置了 Embedding（application-local.yml 打开 spring.ai.model.embedding=openai）时创建；
     * 未配置时该 Bean 不存在，FaqAgentTools 自动降级为关键词检索。
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.embedding", havingValue = "openai")
    public VectorStore aiFaqVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
