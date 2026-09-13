package com.mall.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "SPU 上下架请求")
public class SpuStatusRequest {

    @Schema(description = "SPU ID")
    @NotNull(message = "SPU ID 不能为空")
    private Long spuId;

    @Schema(description = "状态：0下架 1上架", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;
}