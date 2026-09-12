package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类（pms_category），最多三级，parent_id = 0 表示一级分类。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_category")
public class Category extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Long parentId;

    /** 层级 1一级 2二级 3三级 */
    private Integer level;

    private String icon;

    private String image;

    private Integer sort;

    /** 1显示 0隐藏 */
    private Integer status;
}
