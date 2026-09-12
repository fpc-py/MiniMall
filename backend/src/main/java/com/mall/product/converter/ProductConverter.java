package com.mall.product.converter;

import com.mall.product.entity.Category;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.vo.CategoryTreeVO;
import com.mall.product.vo.SkuVO;
import com.mall.product.vo.SpuDetailVO;
import com.mall.product.vo.SpuListItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 商品领域实体 -> VO 转换（MapStruct 编译期生成实现）。
 * 敏感/需要计算的字段（SKU 成本价、可售库存、规格 JSON）一律不自动映射，由 Service 显式处理。
 */
@Mapper(componentModel = "spring")
public interface ProductConverter {

    SpuListItemVO toListItem(Spu spu);

    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "skus", ignore = true)
    SpuDetailVO toDetail(Spu spu);

    /** attrs(String JSON) 与 stock(可售库存) 由 SpuService 计算后填充 */
    @Mapping(target = "attrs", ignore = true)
    @Mapping(target = "stock", ignore = true)
    SkuVO toSkuVO(Sku sku);

    @Mapping(target = "children", ignore = true)
    CategoryTreeVO toCategoryNode(Category category);
}
