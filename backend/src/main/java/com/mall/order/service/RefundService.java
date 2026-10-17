package com.mall.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.order.dto.RefundApplyRequest;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.entity.Refund;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundMapper;
import com.mall.order.vo.RefundVO;
import com.mall.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 退款/售后服务：买家申请 -> 商家审核 -> 同意后原路退回并回补库存。
 * <p>
 * 铁律：退款执行（改支付状态 + 回补库存 + 落库退款单）必须在同一本地事务；
 * 重复审核由状态机拦截，不会重复退款。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    /** 退款单状态 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_REFUNDING = 1;
    public static final int STATUS_REFUNDED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_CANCELLED = 4;

    /** 订单支付状态：已退款 */
    private static final int PAY_REFUNDED = 2;
    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;

    /** 买家申请退款：校验归属与订单可退状态，同一订单同时只允许一条处理中的退款单。 */
    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(Long memberId, RefundApplyRequest req) {
        if (memberId == null) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
        Order order = orderMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_FOUND);
        }
        // 资源归属：只能给自己的订单申请退款
        if (!order.getMemberId().equals(memberId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权操作该订单");
        }
        if (order.getPayStatus() == null || order.getPayStatus() != OrderService.PAY_PAID) {
            throw new MallException(ErrorCode.Biz.ORDER_NOT_REFUNDABLE, "订单未支付，不可申请退款");
        }
        Long open = refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, order.getId())
                .in(Refund::getStatus, STATUS_PENDING, STATUS_REFUNDING));
        if (open != null && open > 0) {
            throw new MallException(ErrorCode.Biz.REFUND_ALREADY_APPLIED);
        }

        Refund refund = new Refund();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setMemberId(memberId);
        refund.setShopId(order.getShopId());
        refund.setRefundAmount(order.getPayAmount());
        refund.setReason(req.getReason());
        refund.setDescription(req.getDescription());
        refund.setImages(joinImages(req.getImages()));
        refund.setStatus(STATUS_PENDING);
        refundMapper.insert(refund);

        log.info("买家申请退款: refundNo={}, orderNo={}, memberId={}", refund.getRefundNo(), order.getOrderNo(), memberId);
        return toVO(refund);
    }

    /** 买家撤销退款申请（仅待审核可撤销）。 */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long memberId, Long refundId) {
        Refund refund = getRequired(refundId);
        if (!refund.getMemberId().equals(memberId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权操作该退款单");
        }
        if (refund.getStatus() == null || refund.getStatus() != STATUS_PENDING) {
            throw new MallException(ErrorCode.Biz.REFUND_STATUS_INVALID, "仅待审核的退款申请可撤销");
        }
        refund.setStatus(STATUS_CANCELLED);
        refundMapper.updateById(refund);
        log.info("买家撤销退款申请: refundId={}, memberId={}", refundId, memberId);
    }

    /**
     * 商家审核退款。
     * status=1 同意：原路退回（沙箱）+ 回补库存 + 订单支付状态置为已退款 + 退款单置为已退款。
     * status=3 拒绝：仅回写状态与备注。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long shopId, Long refundId, Integer status, String auditRemark, Long auditorId) {
        Refund refund = getRequired(refundId);
        if (shopId == null || !shopId.equals(refund.getShopId())) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权审核该退款单");
        }
        if (refund.getStatus() == null || refund.getStatus() != STATUS_PENDING) {
            throw new MallException(ErrorCode.Biz.REFUND_STATUS_INVALID, "退款单已处理，不能重复审核");
        }

        LocalDateTime now = LocalDateTime.now();
        if (status != null && status == STATUS_REFUNDING) {
            executeRefund(refund, auditRemark, auditorId, now);
        } else if (status != null && status == STATUS_REJECTED) {
            refund.setStatus(STATUS_REJECTED);
            refund.setAuditRemark(auditRemark);
            refund.setAuditTime(now);
            refund.setAuditorId(auditorId);
            refundMapper.updateById(refund);
            log.info("商家拒绝退款: refundNo={}, auditorId={}", refund.getRefundNo(), auditorId);
        } else {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "审核结果只能为 1同意 或 3拒绝");
        }
    }

    // ==================== 查询 ====================

    public PageResult<RefundVO> pageByMember(Long memberId, Integer status, PageQuery pageQuery) {
        return page(memberId, null, status, pageQuery);
    }

    public PageResult<RefundVO> pageByShop(Long shopId, Integer status, PageQuery pageQuery) {
        return page(null, shopId, status, pageQuery);
    }

    public PageResult<RefundVO> pageAll(Integer status, PageQuery pageQuery) {
        return page(null, null, status, pageQuery);
    }

    // ==================== 私有方法 ====================

    private void executeRefund(Refund refund, String auditRemark, Long auditorId, LocalDateTime now) {
        // 1) 原路退回（沙箱：仅记录退款流水号，真实渠道对接在此处替换）
        refund.setTransactionNo("SANDBOX-REFUND-" + refund.getRefundNo());

        // 2) 回补库存（商品退回）
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, refund.getOrderId()));
        for (OrderItem item : items) {
            stockService.returnStock(item.getSkuId(), item.getQuantity() == null ? 0 : item.getQuantity());
        }

        // 3) 订单支付状态置为已退款
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order != null) {
            order.setPayStatus(PAY_REFUNDED);
            orderMapper.updateById(order);
        }

        // 4) 退款单落库
        refund.setStatus(STATUS_REFUNDED);
        refund.setRefundTime(now);
        refund.setAuditRemark(auditRemark);
        refund.setAuditTime(now);
        refund.setAuditorId(auditorId);
        refundMapper.updateById(refund);

        log.info("退款成功: refundNo={}, orderNo={}, amount={}",
                refund.getRefundNo(), refund.getOrderNo(), refund.getRefundAmount());
    }

    private PageResult<RefundVO> page(Long memberId, Long shopId, Integer status, PageQuery pageQuery) {
        var wrapper = Wrappers.<Refund>lambdaQuery().orderByDesc(Refund::getCreateTime).orderByDesc(Refund::getId);
        if (memberId != null) {
            wrapper.eq(Refund::getMemberId, memberId);
        }
        if (shopId != null) {
            wrapper.eq(Refund::getShopId, shopId);
        }
        if (status != null) {
            wrapper.eq(Refund::getStatus, status);
        }
        Page<Refund> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        return PageResult.of(refundMapper.selectPage(page, wrapper), this::toVO);
    }

    private Refund getRequired(Long refundId) {
        Refund refund = refundId == null ? null : refundMapper.selectById(refundId);
        if (refund == null) {
            throw new MallException(ErrorCode.Biz.REFUND_NOT_FOUND);
        }
        return refund;
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(NO_FORMAT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        String joined = String.join(",", images);
        return joined.length() > 1024 ? joined.substring(0, 1024) : joined;
    }

    private RefundVO toVO(Refund refund) {
        RefundVO vo = new RefundVO();
        vo.setId(refund.getId());
        vo.setRefundNo(refund.getRefundNo());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(refund.getOrderNo());
        vo.setMemberId(refund.getMemberId());
        vo.setShopId(refund.getShopId());
        vo.setRefundAmount(refund.getRefundAmount());
        vo.setReason(refund.getReason());
        vo.setDescription(refund.getDescription());
        vo.setImages(refund.getImages() == null || refund.getImages().isBlank()
                ? List.of() : Arrays.asList(refund.getImages().split(",")));
        vo.setStatus(refund.getStatus());
        vo.setAuditRemark(refund.getAuditRemark());
        vo.setAuditTime(refund.getAuditTime());
        vo.setRefundTime(refund.getRefundTime());
        vo.setCreateTime(refund.getCreateTime());
        return vo;
    }
}
