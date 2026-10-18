package com.mall.ai.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.util.SqlUtils;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品检索工具：只查在售（status=1）且审核通过（audit_status=1）的 SPU，返回卡片字段，
 * 并通过 notifier 推送 products 事件供前端渲染商品卡片。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAgentTools {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 5;

    private final SpuMapper spuMapper;

    @Tool(name = "search_product",
            description = "按关键词检索商城中在售的商品，返回商品名称、最低价与主图。用户在找商品、问价格、要推荐时调用。",
            resultConverter = AiTextResultConverter.class)
    public String searchProduct(
            @ToolParam(description = "搜索关键词，例如：手机、蓝牙耳机、跑鞋") String keyword,
            @ToolParam(required = false, description = "返回条数，默认 3，最大 5") Integer limit,
            ToolContext toolContext) {
        AiProgressNotifier notifier = AiToolSupport.notifier(toolContext);
        notifier.tool("search_product", "running");
        try {
            int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
            String normalized = SqlUtils.normalizeKeyword(keyword);
            var wrapper = Wrappers.<Spu>lambdaQuery()
                    .eq(Spu::getStatus, 1)
                    .eq(Spu::getAuditStatus, 1);
            if (normalized != null) {
                // 关键字已做 LIKE 通配符转义并以占位符绑定，不存在 SQL 注入
                wrapper.and(w -> w.like(Spu::getName, normalized)
                        .or().like(Spu::getSubtitle, normalized)
                        .or().like(Spu::getBrand, normalized));
            }
            List<Spu> spus = spuMapper.selectList(wrapper
                    .orderByDesc(Spu::getTotalSales)
                    .orderByDesc(Spu::getId)
                    .last("LIMIT " + size));
            if (spus.isEmpty()) {
                return "没有找到与「" + (keyword == null ? "" : keyword) + "」相关的商品，可以换个关键词试试，例如「手机」「耳机」「跑鞋」。";
            }
            List<Map<String, Object>> cards = new ArrayList<>();
            StringBuilder builder = new StringBuilder("为您找到以下商品：\n");
            for (Spu spu : spus) {
                builder.append("· ").append(spu.getName())
                        .append("（").append(AiToolSupport.money(spu.getMinPrice())).append(" 元起）\n");
                Map<String, Object> card = new LinkedHashMap<>();
                // SSE 走 Hutool 序列化（绕过全局 Jackson），雪花 ID 必须转字符串，否则前端 JS 丢精度
                card.put("id", String.valueOf(spu.getId()));
                card.put("name", spu.getName());
                card.put("mainImage", spu.getMainImage());
                card.put("minPrice", AiToolSupport.money(spu.getMinPrice()));
                cards.add(card);
            }
            builder.append("点击下方卡片可查看详情。");
            notifier.emit("products", cards);
            return builder.toString();
        } catch (Exception e) {
            log.warn("search_product 执行异常: {}", e.getMessage());
            return "抱歉，商品检索出了点小问题，请稍后再试。";
        } finally {
            notifier.tool("search_product", "done");
        }
    }
}
