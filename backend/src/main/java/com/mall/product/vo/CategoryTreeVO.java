package com.mall.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "分类树节点")
public class CategoryTreeVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "父分类ID，0 表示一级分类")
    private Long parentId;

    @Schema(description = "层级 1/2/3")
    private Integer level;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "分类图片")
    private String image;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "子分类")
    private List<CategoryTreeVO> children = new ArrayList<>();
}
