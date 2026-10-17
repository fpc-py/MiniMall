package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.marketing.dto.CouponSaveRequest;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponMember;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.mapper.CouponMemberMapper;
import com.mall.marketing.vo.CouponMemberVO;
import com.mall.marketing.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券服务：平台发券 -> 会员领取 -> 下单抵扣 -> 取消/退款退回。
 * <p>
 * 并发安全：领取用 {@code received_count < total_count} 条件更新防超发；
 * 使用用 {@code status=未使用} 条件更新防同一张券重复抵扣。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    /** 优惠券类型 */
    public static final int TYPE_FULL_REDUCE = 1;
    public static final int TYPE_DISCOUNT = 2;

    /** 优惠券模板状态 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    /** 会员券状态 */
    public static final int MEMBER_UNUSED = 0;
    public static final int MEMBER_USED = 1;
    public static final int MEMBER_EXPIRED = 2;

    private final CouponMapper couponMapper;
    private final CouponMemberMapper couponMemberMapper;

    // ==================== 平台管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public CouponVO create(CouponSaveRequest req) {
        if (req.getStartTime() == null || req.getEndTime() == null
                || !req.getEndTime().isAfter(req.getStartTime())) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "失效时间必须晚于生效时间");
        }
        if (req.getType() != null && req.getType() == TYPE_FULL_REDUCE) {
            if (req.getDiscountAmount() == null || req.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new MallException(ErrorCode.Param.INVALID_PARAM, "满减券必须指定大于0的满减金额");
            }
        } else if (req.getType() != null && req.getType() == TYPE_DISCOUNT) {
            if (req.getDiscountRate() == null) {
                throw new MallException(ErrorCode.Param.INVALID_PARAM, "折扣券必须指定折扣率");
            }
        } else {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "优惠券类型只能为 1满减 或 2折扣");
        }

        Coupon coupon = new Coupon();
        coupon.setName(req.getName());
        coupon.setType(req.getType());
        coupon.setShopId(req.getShopId());
        coupon.setThresholdAmount(req.getThresholdAmount() == null ? BigDecimal.ZERO : req.getThresholdAmount());
        coupon.setDiscountAmount(req.getDiscountAmount() == null ? BigDecimal.ZERO : req.getDiscountAmount());
        coupon.setDiscountRate(req.getDiscountRate());
        coupon.setTotalCount(req.getTotalCount());
        coupon.setReceivedCount(0);
        coupon.setPerUserLimit(req.getPerUserLimit() == null ? 1 : req.getPerUserLimit());
        coupon.setStartTime(req.getStartTime());
        coupon.setEndTime(req.getEndTime());
        coupon.setStatus(STATUS_ENABLED);
        coupon.setDescription(req.getDescription());
        couponMapper.insert(coupon);
        log.info("平台创建优惠券: id={}, name={}, total={}", coupon.getId(), coupon.getName(), coupon.getTotalCount());
        return toCouponVO(coupon);
    }

    public PageResult<CouponVO> page(String keyword, Integer status, PageQuery pageQuery) {
        var wrapper = Wrappers.<Coupon>lambdaQuery()
                .orderByDesc(Coupon::getCreateTime).orderByDesc(Coupon::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Coupon::getName, keyword);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        Page<Coupon> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        return PageResult.of(couponMapper.selectPage(page, wrapper), this::toCouponVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long couponId, Integer status) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_FOUND);
        }
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "状态只能为 0停用 或 1启用");
        }
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    // ==================== 会员端 ====================

    /** 可领取列表：启用 + 在有效期内 + 未领完 + 未达个人限领。 */
    public List<CouponVO> listAvailable(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, STATUS_ENABLED)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .orderByAsc(Coupon::getEndTime));
        return coupons.stream()
                .filter(c -> c.getReceivedCount() == null || c.getTotalCount() == null
                        || c.getReceivedCount() < c.getTotalCount())
                .filter(c -> countMemberReceived(memberId, c.getId()) < perLimit(c))
                .map(c -> toCouponVO(c, true))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponMemberVO receive(Long memberId, Long couponId) {
        if (memberId == null) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() == null || coupon.getStatus() != STATUS_ENABLED) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_USABLE, "优惠券已停用");
        }
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_USABLE, "优惠券不在有效期内");
        }
        if (countMemberReceived(memberId, couponId) >= perLimit(coupon)) {
            throw new MallException(ErrorCode.Biz.COUPON_RECEIVE_LIMIT, "已超出个人限领数量");
        }
        // 条件更新防超发：received_count < total_count 才 +1
        int rows = couponMapper.update(null, Wrappers.<Coupon>update()
                .eq("id", couponId)
                .apply("received_count < total_count")
                .setSql("received_count = received_count + 1"));
        if (rows == 0) {
            throw new MallException(ErrorCode.Biz.COUPON_RECEIVE_LIMIT, "优惠券已领完");
        }

        CouponMember cm = new CouponMember();
        cm.setCouponId(couponId);
        cm.setMemberId(memberId);
        cm.setStatus(MEMBER_UNUSED);
        cm.setReceiveTime(now);
        cm.setExpireTime(coupon.getEndTime());
        couponMemberMapper.insert(cm);
        log.info("会员领取优惠券: memberId={}, couponId={}, couponMemberId={}", memberId, couponId, cm.getId());
        return toMemberVO(cm, coupon);
    }

    public PageResult<CouponMemberVO> myCoupons(Long memberId, Integer status, PageQuery pageQuery) {
        var wrapper = Wrappers.<CouponMember>lambdaQuery()
                .eq(CouponMember::getMemberId, memberId)
                .orderByDesc(CouponMember::getReceiveTime)
                .orderByDesc(CouponMember::getId);
        if (status != null) {
            wrapper.eq(CouponMember::getStatus, status);
        }
        Page<CouponMember> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        Page<CouponMember> result = couponMemberMapper.selectPage(page, wrapper);
        return PageResult.of(result, cm -> toMemberVO(cm, couponMapper.selectById(cm.getCouponId())));
    }

    /**
     * 计算一张会员券对指定金额与店铺的优惠额（下单试算与提交共用，保证口径一致）。
     */
    public BigDecimal calculateDiscount(Long memberId, Long couponMemberId, BigDecimal amount, Long shopId) {
        CouponMember cm = couponMemberId == null ? null : couponMemberMapper.selectById(couponMemberId);
        if (cm == null) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_FOUND);
        }
        if (!cm.getMemberId().equals(memberId)) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权使用该优惠券");
        }
        if (cm.getStatus() == null || cm.getStatus() != MEMBER_UNUSED) {
            throw new MallException(ErrorCode.Biz.COUPON_STATUS_INVALID, "优惠券已使用或已失效");
        }
        if (cm.getExpireTime() != null && cm.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_USABLE, "优惠券已过期");
        }
        Coupon coupon = couponMapper.selectById(cm.getCouponId());
        if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != STATUS_ENABLED) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_USABLE, "优惠券不可用");
        }
        if (coupon.getShopId() != null && (shopId == null || !coupon.getShopId().equals(shopId))) {
            throw new MallException(ErrorCode.Biz.COUPON_NOT_USABLE, "该优惠券仅限指定店铺使用");
        }
        BigDecimal threshold = coupon.getThresholdAmount() == null ? BigDecimal.ZERO : coupon.getThresholdAmount();
        if (amount.compareTo(threshold) < 0) {
            throw new MallException(ErrorCode.Biz.COUPON_THRESHOLD_NOT_MET, "未满足使用门槛");
        }
        return computeDiscount(coupon, amount);
    }

    /** 下单成功后标记券已使用（条件更新防并发重复使用）。 */
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long memberId, Long couponMemberId, Long orderId) {
        if (couponMemberId == null) {
            return;
        }
        int rows = couponMemberMapper.update(null, Wrappers.<CouponMember>update()
                .eq("id", couponMemberId)
                .eq("member_id", memberId)
                .eq("status", MEMBER_UNUSED)
                .set("status", MEMBER_USED)
                .set("order_id", orderId)
                .set("use_time", LocalDateTime.now()));
        if (rows == 0) {
            throw new MallException(ErrorCode.Biz.COUPON_STATUS_INVALID, "优惠券已被使用");
        }
    }

    /** 取消订单 / 退款时退回优惠券（恢复为未使用）。 */
    @Transactional(rollbackFor = Exception.class)
    public void returnCoupon(Long orderId) {
        if (orderId == null) {
            return;
        }
        couponMemberMapper.update(null, Wrappers.<CouponMember>update()
                .eq("order_id", orderId)
                .eq("status", MEMBER_USED)
                .set("status", MEMBER_UNUSED)
                .setSql("order_id = NULL, use_time = NULL"));
    }

    // ==================== 私有方法 ====================

    private long countMemberReceived(Long memberId, Long couponId) {
        Long c = couponMemberMapper.selectCount(Wrappers.<CouponMember>lambdaQuery()
                .eq(CouponMember::getMemberId, memberId)
                .eq(CouponMember::getCouponId, couponId));
        return c == null ? 0L : c;
    }

    private int perLimit(Coupon coupon) {
        return coupon.getPerUserLimit() == null || coupon.getPerUserLimit() < 1 ? 1 : coupon.getPerUserLimit();
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal discount;
        if (coupon.getType() != null && coupon.getType() == TYPE_DISCOUNT && coupon.getDiscountRate() != null) {
            discount = amount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        } else {
            discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        }
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount;
    }

    private CouponVO toCouponVO(Coupon c) {
        return toCouponVO(c, false);
    }

    private CouponVO toCouponVO(Coupon c, boolean received) {
        CouponVO vo = new CouponVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setType(c.getType());
        vo.setShopId(c.getShopId());
        vo.setThresholdAmount(c.getThresholdAmount());
        vo.setDiscountAmount(c.getDiscountAmount());
        vo.setDiscountRate(c.getDiscountRate());
        vo.setTotalCount(c.getTotalCount());
        vo.setReceivedCount(c.getReceivedCount());
        vo.setRemainCount(c.getTotalCount() == null || c.getReceivedCount() == null
                ? null : Math.max(c.getTotalCount() - c.getReceivedCount(), 0));
        vo.setPerUserLimit(c.getPerUserLimit());
        vo.setStartTime(c.getStartTime());
        vo.setEndTime(c.getEndTime());
        vo.setStatus(c.getStatus());
        vo.setDescription(c.getDescription());
        vo.setReceived(received);
        return vo;
    }

    private CouponMemberVO toMemberVO(CouponMember cm, Coupon coupon) {
        CouponMemberVO vo = new CouponMemberVO();
        vo.setId(cm.getId());
        vo.setCouponId(cm.getCouponId());
        vo.setStatus(cm.getStatus());
        vo.setOrderId(cm.getOrderId());
        vo.setReceiveTime(cm.getReceiveTime());
        vo.setUseTime(cm.getUseTime());
        vo.setExpireTime(cm.getExpireTime());
        if (coupon != null) {
            vo.setName(coupon.getName());
            vo.setType(coupon.getType());
            vo.setShopId(coupon.getShopId());
            vo.setThresholdAmount(coupon.getThresholdAmount());
            vo.setDiscountAmount(coupon.getDiscountAmount());
            vo.setDiscountRate(coupon.getDiscountRate());
        }
        return vo;
    }
}
