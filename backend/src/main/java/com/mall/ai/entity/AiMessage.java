package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mall.common.entity.BaseEntity;

/**
 * 客服消息（ai_message）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
@Schema(description = "客服消息")
public class AiMessage extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "会员ID")
    private Long memberId;

    /** 1用户 2客服 */
    @Schema(description = "角色：1用户 2客服")
    private Integer role;

    @Schema(description = "消息内容")
    private String content;
}
