package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.mall.common.entity.BaseEntity;

/**
 * 客服知识库 FAQ（ai_faq）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_faq")
@Schema(description = "客服知识库FAQ")
public class AiFaq extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "FAQ ID")
    private Long id;

    @Schema(description = "标准问题")
    private String question;

    @Schema(description = "标准答案")
    private String answer;

    @Schema(description = "关键词，逗号分隔，用于关键词降级检索")
    private String keywords;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "排序，越小越靠前")
    private Integer sort;

    /** 0停用 1启用 */
    @Schema(description = "状态：0停用 1启用")
    private Integer status;
}
