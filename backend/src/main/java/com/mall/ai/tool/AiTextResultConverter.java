package com.mall.ai.tool;

import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

/**
 * 工具结果转换器：把返回值原样转成纯文本，避免框架默认的 JSON 包装（String 会被加引号）。
 * 所有 @Tool 方法均通过 resultConverter = AiTextResultConverter.class 指定。
 */
public final class AiTextResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        return result == null ? "" : result.toString();
    }
}
