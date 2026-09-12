package com.mall.product.dto;

import com.mall.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SPU 列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "SPU 列表查询条件")
public class SpuQuery extends PageQuery {

    @Schema(description = "搜索关键字，匹配商品名称/副标题/品牌", example = "手机")
    private String keyword;

    @Schema(description = "分类ID，自动包含其子分类", example = "1")
    private Long categoryId;

    @Schema(description = "店铺ID", example = "1")
    private Long shopId;

    @Schema(description = "排序：sales 销量、priceAsc 价格升、priceDesc 价格降、newest 最新；其它值按综合(销量)排序",
            example = "sales", allowableValues = {"sales", "priceAsc", "priceDesc", "newest"})
    private String sort;
}
