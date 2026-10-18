package com.mall.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mall.order.entity.Order;
import com.mall.order.entity.Refund;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import com.mall.system.entity.Member;
import com.mall.system.entity.Shop;
import com.mall.system.entity.ShopApply;
import com.mall.system.mapper.MemberMapper;
import com.mall.system.mapper.ShopApplyMapper;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.vo.AdminStatsVO;
import com.mall.system.vo.ShopStatsVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 统计聚合测试：平台维度与店铺维度的聚合口径、空数据兜底。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminStatsServiceTest {

    @Mock
    private MemberMapper memberMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private ShopApplyMapper shopApplyMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private RefundMapper refundMapper;
    @Mock
    private SpuMapper spuMapper;

    private AdminStatsService adminStatsService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Member.class);
        TableInfoHelper.initTableInfo(assistant, Shop.class);
        TableInfoHelper.initTableInfo(assistant, ShopApply.class);
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, Refund.class);
        TableInfoHelper.initTableInfo(assistant, Spu.class);
    }

    @BeforeEach
    void setUp() {
        adminStatsService = new AdminStatsService(
                memberMapper, shopMapper, shopApplyMapper, orderMapper, refundMapper, spuMapper);
    }

    @Test
    @DisplayName("平台概览：聚合各维度计数与已支付销售额")
    void overviewAggregatesAllDimensions() {
        when(memberMapper.selectCount(any())).thenReturn(12L);
        when(shopMapper.selectCount(any())).thenReturn(2L);
        // 订单总数 / 今日订单数 / 待发货数
        when(orderMapper.selectCount(any())).thenReturn(100L, 5L, 3L);
        when(refundMapper.selectCount(any())).thenReturn(4L);
        when(shopApplyMapper.selectCount(any())).thenReturn(1L);
        when(orderMapper.selectMaps(any())).thenReturn(List.of(Map.of("amount", new BigDecimal("8888.88"))));

        AdminStatsVO vo = adminStatsService.overview();

        assertThat(vo.getMemberCount()).isEqualTo(12L);
        assertThat(vo.getShopCount()).isEqualTo(2L);
        assertThat(vo.getOrderCount()).isEqualTo(100L);
        assertThat(vo.getTodayOrderCount()).isEqualTo(5L);
        assertThat(vo.getPendingShipOrderCount()).isEqualTo(3L);
        assertThat(vo.getPendingRefundCount()).isEqualTo(4L);
        assertThat(vo.getPendingShopApplyCount()).isEqualTo(1L);
        assertThat(vo.getTotalSalesAmount()).isEqualByComparingTo("8888.88");
    }

    @Test
    @DisplayName("平台概览：空库时计数为0、销售额为0")
    void overviewHandlesEmptyDatabase() {
        when(orderMapper.selectMaps(any())).thenReturn(List.of());

        AdminStatsVO vo = adminStatsService.overview();

        assertThat(vo.getMemberCount()).isZero();
        assertThat(vo.getShopCount()).isZero();
        assertThat(vo.getOrderCount()).isZero();
        assertThat(vo.getTodayOrderCount()).isZero();
        assertThat(vo.getPendingShipOrderCount()).isZero();
        assertThat(vo.getPendingRefundCount()).isZero();
        assertThat(vo.getPendingShopApplyCount()).isZero();
        assertThat(vo.getTotalSalesAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("平台概览：SUM 返回字符串时也能正确解析为金额")
    void overviewParsesStringSumValue() {
        when(orderMapper.selectMaps(any())).thenReturn(List.of(Map.of("amount", "123.45")));

        assertThat(adminStatsService.overview().getTotalSalesAmount()).isEqualByComparingTo("123.45");
    }

    @Test
    @DisplayName("店铺概览：所有聚合限定 shopId 维度")
    void shopOverviewAggregatesWithinShopScope() {
        when(spuMapper.selectCount(any())).thenReturn(8L);
        // 本店订单总数 / 待发货
        when(orderMapper.selectCount(any())).thenReturn(20L, 4L);
        when(refundMapper.selectCount(any())).thenReturn(1L);
        when(orderMapper.selectMaps(any())).thenReturn(List.of(Map.of("amount", new BigDecimal("600.00"))));

        ShopStatsVO vo = adminStatsService.shopOverview(1L);

        assertThat(vo.getProductCount()).isEqualTo(8L);
        assertThat(vo.getOrderCount()).isEqualTo(20L);
        assertThat(vo.getPendingShipOrderCount()).isEqualTo(4L);
        assertThat(vo.getPendingRefundCount()).isEqualTo(1L);
        assertThat(vo.getSalesAmount()).isEqualByComparingTo("600.00");
    }
}
