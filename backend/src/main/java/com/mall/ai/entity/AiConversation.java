package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mall.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 客服会话（ai_conversation）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
@Schema(description = "客服会话")
public class AiConversation extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会话标题，取首条提问摘要")
    private String title;

    /** 0进行中 1已转人工 2已结束 */
    @Schema(description = "状态：0进行中 1已转人工 2已结束")
    private Integer status;

    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageTime;

    @Schema(description = "消息条数")
    private Integer messageCount;
}
