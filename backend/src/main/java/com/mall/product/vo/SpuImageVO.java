package com.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "SPU 图片")
public class SpuImageVO {

    @Schema(description = "图片ID")
    private Long id;

    @Schema(description = "图片URL")
    private String url;

    @Schema(description = "类型 0轮播图 1详情图")
    private Integer type;

    @Schema(description = "排序值")
    private Integer sort;

    public static SpuImageVO of(Long id, String url, Integer type, Integer sort) {
        SpuImageVO vo = new SpuImageVO();
        vo.setId(id);
        vo.setUrl(url);
        vo.setType(type);
        vo.setSort(sort);
        return vo;
    }
}
