package com.mall.ai.tool;

import com.mall.ai.entity.AiFaq;
import com.mall.ai.service.AiFaqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FAQ 检索工具：配了 Embedding 走向量检索，否则自动降级为关键词检索（对模型透明）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqAgentTools {

    private final AiFaqService aiFaqService;

    @Tool(name = "search_faq",
            description = "检索商城帮助中心知识库，用于回答退换货政策、发货时效、运费、发票、退款到账时间、优惠券使用等政策类问题。",
            resultConverter = AiTextResultConverter.class)
    public String searchFaq(
            @ToolParam(description = "用户的问题原文或关键词，例如：七天无理由退货") String keyword) {
        List<AiFaq> faqs = aiFaqService.search(keyword, 0);
        if (faqs.isEmpty()) {
            return "未查询到相关的帮助文档。您可以换个说法再问，或回复「转人工」由人工客服为您服务。";
        }
        StringBuilder builder = new StringBuilder();
        for (AiFaq faq : faqs) {
            builder.append("【").append(faq.getQuestion()).append("】")
                    .append(faq.getAnswer()).append('\n');
        }
        return builder.toString().trim();
    }
}
