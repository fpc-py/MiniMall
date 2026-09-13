package com.mall.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "购物车项修改请求")
public class CartUpdateRequest {

    @Schema(description = "购买数量，传 null 表示不修改")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer quantity;

    @Schema(description = "是否选中，传 null 表示不修改")
    private Boolean selected;
}
