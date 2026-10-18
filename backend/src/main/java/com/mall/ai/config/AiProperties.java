package com.mall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能客服配置（mall.ai.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.ai")
public class AiProperties {

    /** 无真实模型 key 时为 true：使用自研 MockChatModel 降级，保证开箱可用；生产配真实 key 后置为 false */
    private boolean mockEnabled = true;

    /** 单次对话超时（秒），用于 SseEmitter 与模型调用 */
    private int timeoutSeconds = 60;

    /** 装配给模型的历史消息条数上限 */
    private int historyLimit = 20;

    /** FAQ 检索返回条数 */
    private int faqTopK = 3;
}
