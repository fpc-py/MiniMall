package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SPU 图片（pms_spu_image）：type 0轮播图 1详情图。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_spu_image")
public class SpuImage extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long spuId;

    private String url;

    private Integer sort;

    private Integer type;
}
