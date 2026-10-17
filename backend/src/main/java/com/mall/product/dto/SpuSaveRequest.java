package com.mall.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "SPU 创建请求（商家/平台通用）")
public class SpuSaveRequest {

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Schema(description = "商品详情(HTML)")
    private String description;

    @Schema(description = "商品图片 URL 列表")
    private List<String> images;

    @Schema(description = "SKU 列表，至少一个", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "至少需要一个商品规格")
    @Valid
    private List<SkuItem> skus;

    @Data
    @Schema(description = "SKU 项")
    public static class SkuItem {

        @Schema(description = "规格描述，如 曜石黑 / 8GB+128GB")
        private String spec;

        @Schema(description = "售价", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "SKU 售价不能为空")
        @DecimalMin(value = "0.01", message = "SKU 售价必须大于 0")
        private BigDecimal price;

        @Schema(description = "初始库存", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "SKU 库存不能为空")
        @Min(value = 0, message = "SKU 库存不能为负数")
        private Integer stock;

        @Schema(description = "SKU 编码")
        private String skuCode;

        @Schema(description = "排序")
        private Integer sort;
    }
}
