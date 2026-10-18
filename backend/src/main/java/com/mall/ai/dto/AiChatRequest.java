package com.mall.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能客服提问入参。
 */
@Data
@Schema(description = "智能客服提问")
public class AiChatRequest {

    @Schema(description = "会话ID；不传表示开启新会话")
    private Long conversationId;

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 500, message = "提问内容过长，请控制在 500 字以内")
    @Schema(description = "提问内容")
    private String content;
}
