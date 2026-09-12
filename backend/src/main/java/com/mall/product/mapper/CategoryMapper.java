package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Category;

/**
 * 商品分类 Mapper：全部走 MyBatis-Plus 条件构造器，禁止字符串拼接 SQL。
 */
public interface CategoryMapper extends BaseMapper<Category> {
}
