package com.mall.product.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageResult;
import com.mall.product.converter.ProductConverterImpl;
import com.mall.product.dto.SpuQuery;
import com.mall.product.entity.Category;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.entity.SpuImage;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuImageMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.vo.SpuDetailVO;
import com.mall.product.vo.SpuListItemVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SPU 查询测试：分页映射、关键字转义与参数绑定、排序白名单、详情可见性与脱敏。
 */
@ExtendWith(MockitoExtension.class)
class SpuServiceTest {

    @Mock
    private SpuMapper spuMapper;

    @Mock
    private SkuMapper skuMapper;

    @Mock
    private SpuImageMapper spuImageMapper;

    @Mock
    private CategoryService categoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SpuService spuService;

    /** 让 LambdaQueryWrapper 能在纯单测环境解析出列名（无 Spring 容器） */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Spu.class);
    }

    @BeforeEach
    void setUp() {
        spuService = new SpuService(spuMapper, skuMapper, spuImageMapper, categoryService,
                new ProductConverterImpl(), objectMapper);
    }

    private Spu spu(long id, String name) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setName(name);
        spu.setSubtitle("旗舰新品");
        spu.setBrand("Apple");
        spu.setCategoryId(10L);
        spu.setShopId(1L);
        spu.setMainImage("http://img/" + id + ".jpg");
        spu.setMinPrice(new BigDecimal("5999.00"));
        spu.setMaxPrice(new BigDecimal("7999.00"));
        spu.setTotalSales(100);
        spu.setStatus(1);
        spu.setAuditStatus(1);
        return spu;
    }

    private Sku sku(long id, String attrsJson, Integer stock, Integer frozen, String costPrice) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setSpuId(1L);
        sku.setSkuCode("SKU-" + id);
        sku.setName("黑色 256G");
        sku.setPrice(new BigDecimal("5999.00"));
        sku.setOriginalPrice(new BigDecimal("6999.00"));
        sku.setCostPrice(costPrice == null ? null : new BigDecimal(costPrice));
        sku.setAttrs(attrsJson);
        sku.setStock(stock);
        sku.setFrozenStock(frozen);
        sku.setSales(10);
        sku.setStatus(1);
        sku.setSort(1);
        return sku;
    }

    private SpuImage image(long id, String url, int type, int sort) {
        SpuImage image = new SpuImage();
        image.setId(id);
        image.setSpuId(1L);
        image.setUrl(url);
        image.setType(type);
        image.setSort(sort);
        return image;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Spu> capturedPageWrapper() {
        ArgumentCaptor<Wrapper<Spu>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(spuMapper).selectPage(any(), captor.capture());
        return (LambdaQueryWrapper<Spu>) captor.getValue();
    }

    private void mockPage(List<Spu> records, long total) {
        when(spuMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Spu> page = invocation.getArgument(0);
            page.setRecords(records);
            page.setTotal(total);
            return page;
        });
    }

    @Test
    @DisplayName("分页：实体正确映射为 VO，分页元信息完整")
    void pageMapsEntitiesAndPagination() {
        mockPage(List.of(spu(1L, "iPhone 15"), spu(2L, "iPhone 14")), 25L);
        SpuQuery query = new SpuQuery();
        query.setPageNum(2);
        query.setPageSize(10);

        PageResult<SpuListItemVO> result = spuService.page(query);

        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(25L);
        assertThat(result.getPages()).isEqualTo(3);
        assertThat(result.getList()).hasSize(2);
        assertThat(result.getList().get(0).getName()).isEqualTo("iPhone 15");
        assertThat(result.getList().get(0).getMinPrice()).isEqualByComparingTo("5999.00");
    }

    @Test
    @DisplayName("分页：pageSize 超上限时被收敛，避免一次拉全表")
    void pageLimitsPageSize() {
        mockPage(List.of(), 0L);
        SpuQuery query = new SpuQuery();
        query.setPageNum(1);
        query.setPageSize(10000);

        PageResult<SpuListItemVO> result = spuService.page(query);

        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("关键字：% 和 _ 被转义并作为参数绑定，不会退化成全表匹配也不会注入")
    void pageEscapesKeywordWildcards() {
        mockPage(List.of(), 0L);
        SpuQuery query = new SpuQuery();
        query.setKeyword("100%_off");

        spuService.page(query);

        LambdaQueryWrapper<Spu> wrapper = capturedPageWrapper();
        // 先渲染 SQL：MyBatis-Plus 3.5.x 的参数是懒填充的，渲染后才会进入 paramNameValuePairs
        String sql = wrapper.getSqlSegment();
        assertThat(wrapper.getParamNameValuePairs().values())
                .anyMatch(value -> "%100\\%\\_off%".equals(String.valueOf(value)));
        assertThat(sql).contains("LIKE");
        assertThat(sql).doesNotContain("100%_off");
        assertThat(sql).doesNotContain("DROP");
    }

    @Test
    @DisplayName("排序白名单：非法排序值回退默认排序，不会拼进 SQL")
    void pageIgnoresIllegalSort() {
        mockPage(List.of(), 0L);
        SpuQuery query = new SpuQuery();
        query.setSort("id; DROP TABLE pms_spu--");

        spuService.page(query);

        LambdaQueryWrapper<Spu> wrapper = capturedPageWrapper();
        String sql = wrapper.getSqlSegment().toUpperCase();
        assertThat(sql).contains("ORDER BY TOTAL_SALES DESC");
        assertThat(sql).doesNotContain("DROP");
        assertThat(sql).doesNotContain(";");
    }

    @Test
    @DisplayName("排序：priceAsc 按最低价升序，并追加 id 保证分页稳定")
    void pageAppliesPriceAscSort() {
        mockPage(List.of(), 0L);
        SpuQuery query = new SpuQuery();
        query.setSort("priceAsc");

        spuService.page(query);

        String sql = capturedPageWrapper().getSqlSegment().toUpperCase();
        assertThat(sql).contains("MIN_PRICE ASC");
        assertThat(sql).contains("ID DESC");
    }

    @Test
    @DisplayName("分类筛选：包含子分类ID")
    void pageFiltersByCategoryAndDescendants() {
        mockPage(List.of(), 0L);
        when(categoryService.listSelfAndDescendantIds(1L)).thenReturn(List.of(1L, 10L, 11L));
        SpuQuery query = new SpuQuery();
        query.setCategoryId(1L);

        spuService.page(query);

        LambdaQueryWrapper<Spu> wrapper = capturedPageWrapper();
        assertThat(wrapper.getSqlSegment().toUpperCase()).contains("IN (");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1L, 10L, 11L);
    }

    @Test
    @DisplayName("分类无子节点数据时不产生非法的 IN () 语句")
    void pageSkipsInClauseWhenNoCategoryMatched() {
        mockPage(List.of(), 0L);
        when(categoryService.listSelfAndDescendantIds(404L)).thenReturn(List.of());
        SpuQuery query = new SpuQuery();
        query.setCategoryId(404L);

        spuService.page(query);

        assertThat(capturedPageWrapper().getSqlSegment().toUpperCase()).doesNotContain("IN (");
    }

    @Test
    @DisplayName("列表只查上架且审核通过的商品")
    void pageOnlyQueriesVisibleProducts() {
        mockPage(List.of(), 0L);

        spuService.page(new SpuQuery());

        LambdaQueryWrapper<Spu> wrapper = capturedPageWrapper();
        String sql = wrapper.getSqlSegment();
        assertThat(sql).contains("status").contains("audit_status");
        assertThat(wrapper.getParamNameValuePairs().values()).contains(1);
    }

    @Test
    @DisplayName("详情：商品不存在抛 30000")
    void detailThrowsWhenNotFound() {
        when(spuMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> spuService.detail(404L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("详情：已下架/未过审商品抛 30001，不泄漏数据")
    void detailThrowsWhenOffline() {
        Spu offline = spu(1L, "下架商品");
        offline.setStatus(0);
        when(spuMapper.selectById(1L)).thenReturn(offline);

        assertThatThrownBy(() -> spuService.detail(1L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.PRODUCT_OFFLINE.getCode());

        Spu notAudited = spu(2L, "未过审商品");
        notAudited.setAuditStatus(0);
        when(spuMapper.selectById(2L)).thenReturn(notAudited);

        assertThatThrownBy(() -> spuService.detail(2L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.PRODUCT_OFFLINE.getCode());
    }

    @Test
    @DisplayName("详情：ID 为空抛 10001")
    void detailThrowsWhenIdMissing() {
        assertThatThrownBy(() -> spuService.detail(null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.MISSING_PARAM.getCode());
    }

    @Test
    @DisplayName("详情：返回分类名、图片与 SKU；库存为可售库存，规格 JSON 解析为键值对")
    void detailReturnsFullInformation() throws Exception {
        when(spuMapper.selectById(1L)).thenReturn(spu(1L, "iPhone 15"));
        Category category = new Category();
        category.setId(10L);
        category.setName("手机");
        when(categoryService.findById(10L)).thenReturn(category);
        when(spuImageMapper.selectList(any())).thenReturn(List.of(
                image(1L, "http://img/1.jpg", 0, 1),
                image(2L, "http://img/2.jpg", 1, 2)));
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(11L, "{\"颜色\":\"黑色\",\"内存\":\"256G\"}", 10, 3, "4000.00"),
                sku(12L, "not-a-json", null, null, "4000.00")));

        SpuDetailVO detail = spuService.detail(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getName()).isEqualTo("iPhone 15");
        assertThat(detail.getCategoryName()).isEqualTo("手机");
        assertThat(detail.getImages()).hasSize(2);
        assertThat(detail.getImages().get(0).getUrl()).isEqualTo("http://img/1.jpg");
        assertThat(detail.getSkus()).hasSize(2);
        // 可售库存 = 库存 - 冻结库存
        assertThat(detail.getSkus().get(0).getStock()).isEqualTo(7);
        assertThat(detail.getSkus().get(0).getAttrs())
                .containsEntry("颜色", "黑色")
                .containsEntry("内存", "256G");
        // 脏数据不抛异常，规格降级为空
        assertThat(detail.getSkus().get(1).getAttrs()).isEmpty();
        assertThat(detail.getSkus().get(1).getStock()).isZero();
        // 成本价绝不外泄
        String json = objectMapper.writeValueAsString(detail);
        assertThat(json).doesNotContain("costPrice").doesNotContain("4000.00");
    }

    @Test
    @DisplayName("可售库存不会出现负数")
    void availableStockIsNeverNegative() {
        assertThat(SpuService.availableStock(sku(1L, null, 2, 5, null))).isZero();
        assertThat(SpuService.availableStock(sku(1L, null, 10, 0, null))).isEqualTo(10);
        assertThat(SpuService.availableStock(sku(1L, null, null, null, null))).isZero();
    }

    @Test
    @DisplayName("可见性判定：上架 + 审核通过才可见")
    void isVisibleRequiresOnSaleAndAudited() {
        assertThat(spuService.isVisible(spu(1L, "正常商品"))).isTrue();
        Spu offline = spu(2L, "下架");
        offline.setStatus(0);
        assertThat(spuService.isVisible(offline)).isFalse();
        Spu rejected = spu(3L, "审核拒绝");
        rejected.setAuditStatus(2);
        assertThat(spuService.isVisible(rejected)).isFalse();
        assertThat(spuService.isVisible(null)).isFalse();
    }
}
