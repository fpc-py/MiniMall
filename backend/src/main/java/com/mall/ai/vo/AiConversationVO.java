package com.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服会话出参。
 */
@Data
@Schema(description = "客服会话")
public class AiConversationVO {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "状态：0进行中 1已转人工 2已结束")
    private Integer status;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "消息条数")
    private Integer messageCount;
}
