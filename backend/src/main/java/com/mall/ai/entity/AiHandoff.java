package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mall.common.entity.BaseEntity;

/**
 * 转人工工单（ai_handoff）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_handoff")
@Schema(description = "转人工工单")
public class AiHandoff extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "工单ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "转人工原因")
    private String reason;

    @Schema(description = "联系方式")
    private String contact;

    /** 0待处理 1已处理 */
    @Schema(description = "状态：0待处理 1已处理")
    private Integer status;
}
