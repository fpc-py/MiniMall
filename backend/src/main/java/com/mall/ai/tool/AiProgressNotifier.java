package com.mall.ai.tool;

import java.util.Map;

/**
 * SSE 进度通知器：工具方法体内主动向前端推送事件。
 * Spring AI 的流式 API 不暴露「正在调用哪个工具」的中间事件，故由工具自己回推。
 */
@FunctionalInterface
public interface AiProgressNotifier {

    /**
     * @param type    事件类型：tool（工具进度）/ products（商品卡片）
     * @param payload 事件载荷
     */
    void emit(String type, Object payload);

    /** 空实现，用于无 SSE 连接的场景（如单测） */
    static AiProgressNotifier noop() {
        return (type, payload) -> {
        };
    }

    /** 工具进度事件快捷方法 */
    default void tool(String name, String status) {
        emit("tool", Map.of("name", name, "status", status));
    }
}
