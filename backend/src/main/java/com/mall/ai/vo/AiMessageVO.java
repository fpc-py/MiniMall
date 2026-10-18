package com.mall.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服消息出参。
 */
@Data
@Schema(description = "客服消息")
public class AiMessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "角色：1用户 2客服")
    private Integer role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
