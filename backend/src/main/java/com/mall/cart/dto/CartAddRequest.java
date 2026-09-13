package com.mall.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "加购请求")
public class CartAddRequest {

    @Schema(description = "SKU ID")
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @Schema(description = "加购数量", example = "1")
    @Min(value = 1, message = "加购数量至少为 1")
    private Integer quantity = 1;
}
