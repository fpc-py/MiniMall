package com.mall.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.ai.config.AiProperties;
import com.mall.ai.entity.AiFaq;
import com.mall.ai.mapper.AiFaqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FAQ 知识库服务：关键词检索（无 Embedding 时的降级路径）+ 向量检索（配了 Embedding 时）。
 * 两条路径对上层工具与前端完全透明。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFaqService {

    /** 关键词检索时最多参与打分的 FAQ 条数，防止知识库异常膨胀拖慢检索 */
    private static final int SCAN_LIMIT = 200;

    private final AiFaqMapper faqMapper;
    private final AiProperties aiProperties;
    /** 只有配置了 Embedding（spring.ai.model.embedding=openai）时才有 VectorStore Bean */
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /**
     * FAQ 检索：优先向量检索，未配置 Embedding 时降级为关键词打分。
     *
     * @param question 用户原始提问
     * @param topK     期望返回条数，<=0 时取配置值
     */
    public List<AiFaq> search(String question, int topK) {
        int limit = topK > 0 ? topK : aiProperties.getFaqTopK();
        List<AiFaq> byVector = searchByVector(question, limit);
        if (!byVector.isEmpty()) {
            return byVector;
        }
        return searchByKeyword(question, limit);
    }

    /** 关键词检索：命中关键词按长度加权，标题连续片段命中额外加权 */
    public List<AiFaq> searchByKeyword(String question, int topK) {
        int limit = topK > 0 ? topK : aiProperties.getFaqTopK();
        List<AiFaq> all = loadEnabledFaqs();
        if (all.isEmpty()) {
            return List.of();
        }
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return all.subList(0, Math.min(limit, all.size()));
        }
        List<AiFaq> matched = new ArrayList<>();
        int[] scores = new int[all.size()];
        for (int i = 0; i < all.size(); i++) {
            scores[i] = score(all.get(i), q);
        }
        Integer[] order = new Integer[all.size()];
        for (int i = 0; i < all.size(); i++) {
            order[i] = i;
        }
        // 稳定排序：分数降序，同分保持 sort 顺序
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(scores[b], scores[a]));
        for (Integer index : order) {
            if (scores[index] <= 0 || matched.size() >= limit) {
                break;
            }
            matched.add(all.get(index));
        }
        return matched;
    }

    /** 向量检索：需 Embedding 已配置且索引已建立；任一条件不满足返回空列表，由调用方降级 */
    public List<AiFaq> searchByVector(String question, int topK) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null || question == null || question.isBlank()) {
            return List.of();
        }
        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(topK).build());
            List<AiFaq> result = new ArrayList<>();
            for (Document document : documents) {
                Object faqId = document.getMetadata().get("faqId");
                if (faqId == null) {
                    continue;
                }
                AiFaq faq = faqMapper.selectById(Long.valueOf(faqId.toString()));
                if (faq != null) {
                    result.add(faq);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("FAQ 向量检索失败，降级为关键词检索: {}", e.getMessage());
            return List.of();
        }
    }

    /** 热门问题（快捷提问 chips），按 sort 取前 limit 条 */
    public List<String> hotQuestions(int limit) {
        return loadEnabledFaqs().stream()
                .limit(Math.max(limit, 0))
                .map(AiFaq::getQuestion)
                .toList();
    }

    /** 应用就绪后把全部启用 FAQ 装配为 Document 写入向量库；未配置 Embedding 时跳过 */
    @EventListener(ApplicationReadyEvent.class)
    public void indexFaqVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.info("未配置 Embedding，FAQ 检索走关键词降级路径");
            return;
        }
        try {
            List<AiFaq> faqs = loadEnabledFaqs();
            List<Document> documents = faqs.stream()
                    .map(faq -> new Document(String.valueOf(faq.getId()),
                            faq.getQuestion() + "\n" + faq.getAnswer(),
                            Map.of("faqId", faq.getId(), "question", faq.getQuestion())))
                    .toList();
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }
            log.info("FAQ 向量索引完成: {} 条", documents.size());
        } catch (Exception e) {
            log.warn("FAQ 向量索引建立失败，检索将走关键词降级: {}", e.getMessage());
        }
    }

    private List<AiFaq> loadEnabledFaqs() {
        return faqMapper.selectList(Wrappers.<AiFaq>lambdaQuery()
                .eq(AiFaq::getStatus, 1)
                .orderByAsc(AiFaq::getSort)
                .last("LIMIT " + SCAN_LIMIT));
    }

    private int score(AiFaq faq, String question) {
        int score = 0;
        if (faq.getKeywords() != null) {
            for (String keyword : faq.getKeywords().split("[,，]")) {
                String key = keyword.trim();
                if (!key.isEmpty() && question.contains(key)) {
                    score += key.length() * 2;
                }
            }
        }
        String title = faq.getQuestion() == null ? "" : faq.getQuestion().replaceAll("[？?！!，,。.、\\s]", "");
        int maxLen = Math.min(6, title.length());
        for (int len = maxLen; len >= 3; len--) {
            boolean hit = false;
            for (int i = 0; i + len <= title.length(); i++) {
                if (question.contains(title.substring(i, i + len))) {
                    score += len;
                    hit = true;
                    break;
                }
            }
            if (hit) {
                break;
            }
        }
        return score;
    }

    /** 供上层判断向量路径是否启用（仅用于日志/测试） */
    public boolean vectorEnabled() {
        return Objects.nonNull(vectorStoreProvider.getIfAvailable());
    }
}
