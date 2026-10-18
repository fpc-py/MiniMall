package com.mall.ai.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 工具方法公共支撑：从 ToolContext 安全取值 + 金额格式化。
 */
public final class AiToolSupport {

    private AiToolSupport() {
    }

    /** 当前登录会员ID；上下文缺失时返回 null，调用方必须按「未登录」处理 */
    public static Long userId(ToolContext toolContext) {
        return longValue(toolContext, AiToolContextKeys.USER_ID);
    }

    /** 当前会话ID；可能为 null（新会话首轮由服务端创建后回填） */
    public static Long conversationId(ToolContext toolContext) {
        return longValue(toolContext, AiToolContextKeys.CONVERSATION_ID);
    }

    /** 进度通知器；上下文缺失时返回空实现，保证工具可独立调用（如单测） */
    public static AiProgressNotifier notifier(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return AiProgressNotifier.noop();
        }
        Object value = toolContext.getContext().get(AiToolContextKeys.NOTIFIER);
        return value instanceof AiProgressNotifier notifier ? notifier : AiProgressNotifier.noop();
    }

    /** 金额格式化：统一两位小数、纯数字字符串（不加千分位与货币符号，由话术补充） */
    public static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static Long longValue(ToolContext toolContext, String key) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(key);
        return value instanceof Number number ? number.longValue() : null;
    }
}
