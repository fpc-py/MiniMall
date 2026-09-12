package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Sku;

/**
 * 商品 SKU Mapper：全部走 MyBatis-Plus 条件构造器，禁止字符串拼接 SQL。
 */
public interface SkuMapper extends BaseMapper<Sku> {
}
