package com.mall.ai.tool;

/**
 * ToolContext 约定的 key 常量。
 * 工具方法由框架调用时只能拿到 ToolContext，故 userId/conversationId/notifier 均经此传入。
 */
public final class AiToolContextKeys {

    /** 当前登录会员ID（Long），一律取自 JWT，禁止信任前端 */
    public static final String USER_ID = "userId";

    /** 当前会话ID（Long） */
    public static final String CONVERSATION_ID = "conversationId";

    /** 进度通知器（AiProgressNotifier），用于向前端回推 tool/products 事件 */
    public static final String NOTIFIER = "notifier";

    private AiToolContextKeys() {
    }
}
