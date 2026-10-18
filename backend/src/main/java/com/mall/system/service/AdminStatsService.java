package com.mall.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.order.entity.Order;
import com.mall.order.entity.Refund;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.order.service.OrderService;
import com.mall.order.service.RefundService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 统计聚合服务：平台仪表盘与商家端仪表盘共用一套聚合口径（纯 SQL 聚合，不引第三方）。
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final String SUM_AMOUNT_ALIAS = "amount";

    private final MemberMapper memberMapper;
    private final ShopMapper shopMapper;
    private final ShopApplyMapper shopApplyMapper;
    private final OrderMapper orderMapper;
    private final RefundMapper refundMapper;
    private final SpuMapper spuMapper;

    /** 平台统计概览。 */
    public AdminStatsVO overview() {
        AdminStatsVO vo = new AdminStatsVO();
        vo.setMemberCount(count(memberMapper.selectCount(Wrappers.<Member>lambdaQuery())));
        vo.setShopCount(count(shopMapper.selectCount(Wrappers.<Shop>lambdaQuery())));
        vo.setOrderCount(count(orderMapper.selectCount(Wrappers.<Order>lambdaQuery())));
        vo.setTodayOrderCount(count(orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .ge(Order::getCreateTime, LocalDate.now().atStartOfDay())
                .lt(Order::getCreateTime, LocalDate.now().plusDays(1).atStartOfDay()))));
        vo.setTotalSalesAmount(sumPaidAmount(null));
        vo.setPendingShopApplyCount(count(shopApplyMapper.selectCount(Wrappers.<ShopApply>lambdaQuery()
                .eq(ShopApply::getStatus, ShopApplyService.APPLY_PENDING))));
        vo.setPendingShipOrderCount(count(orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderStatus, OrderService.STATUS_PENDING_SHIP))));
        vo.setPendingRefundCount(count(refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getStatus, RefundService.STATUS_PENDING))));
        return vo;
    }

    /** 商家端统计概览：所有聚合都限定在当前店铺维度。 */
    public ShopStatsVO shopOverview(Long shopId) {
        ShopStatsVO vo = new ShopStatsVO();
        vo.setProductCount(count(spuMapper.selectCount(Wrappers.<Spu>lambdaQuery()
                .eq(Spu::getShopId, shopId))));
        vo.setOrderCount(count(orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getShopId, shopId))));
        vo.setPendingShipOrderCount(count(orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getShopId, shopId)
                .eq(Order::getOrderStatus, OrderService.STATUS_PENDING_SHIP))));
        vo.setPendingRefundCount(count(refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getShopId, shopId)
                .eq(Refund::getStatus, RefundService.STATUS_PENDING))));
        vo.setSalesAmount(sumPaidAmount(shopId));
        return vo;
    }

    /** 已支付订单金额之和；shopId 为 null 时统计全平台。 */
    private BigDecimal sumPaidAmount(Long shopId) {
        QueryWrapper<Order> wrapper = Wrappers.<Order>query()
                .select("IFNULL(SUM(pay_amount), 0) AS " + SUM_AMOUNT_ALIAS)
                .eq("pay_status", OrderService.PAY_PAID);
        if (shopId != null) {
            wrapper.eq("shop_id", shopId);
        }
        return firstValueAsDecimal(orderMapper.selectMaps(wrapper));
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal firstValueAsDecimal(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object value = maps.get(0).values().stream().filter(Objects::nonNull).findFirst().orElse(null);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
