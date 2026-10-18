package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.marketing.dto.CouponSaveRequest;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponMember;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.mapper.CouponMemberMapper;
import com.mall.marketing.vo.CouponMemberVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠券测试：创建校验、领取限领/超发、抵扣计算（满减/折扣/门槛/店铺）、使用与退回幂等。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponServiceTest {

    private static final long MEMBER_ID = 1L;

    @Mock
    private CouponMapper couponMapper;
    @Mock
    private CouponMemberMapper couponMemberMapper;

    private CouponService couponService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Coupon.class);
        TableInfoHelper.initTableInfo(assistant, CouponMember.class);
    }

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponMapper, couponMemberMapper);
    }

    private Coupon coupon(int type, String threshold, String amount, String rate, Long shopId) {
        Coupon c = new Coupon();
        c.setId(10L);
        c.setName("满100减20");
        c.setType(type);
        c.setShopId(shopId);
        c.setThresholdAmount(threshold == null ? null : new BigDecimal(threshold));
        c.setDiscountAmount(amount == null ? null : new BigDecimal(amount));
        c.setDiscountRate(rate == null ? null : new BigDecimal(rate));
        c.setTotalCount(100);
        c.setReceivedCount(0);
        c.setPerUserLimit(1);
        c.setStartTime(LocalDateTime.now().minusDays(1));
        c.setEndTime(LocalDateTime.now().plusDays(7));
        c.setStatus(CouponService.STATUS_ENABLED);
        return c;
    }

    private CouponMember memberCoupon(int status) {
        CouponMember cm = new CouponMember();
        cm.setId(500L);
        cm.setCouponId(10L);
        cm.setMemberId(MEMBER_ID);
        cm.setStatus(status);
        cm.setReceiveTime(LocalDateTime.now().minusHours(1));
        cm.setExpireTime(LocalDateTime.now().plusDays(7));
        return cm;
    }

    private CouponSaveRequest saveRequest() {
        CouponSaveRequest req = new CouponSaveRequest();
        req.setName("满100减20");
        req.setType(CouponService.TYPE_FULL_REDUCE);
        req.setThresholdAmount(new BigDecimal("100.00"));
        req.setDiscountAmount(new BigDecimal("20.00"));
        req.setTotalCount(100);
        req.setPerUserLimit(1);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusDays(7));
        return req;
    }

    // ==================== 创建 ====================

    @Test
    @DisplayName("创建：满减券缺金额抛 10000")
    void createFullReduceWithoutAmount() {
        CouponSaveRequest req = saveRequest();
        req.setDiscountAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> couponService.create(req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
    }

    @Test
    @DisplayName("创建：折扣券缺折扣率抛 10000")
    void createDiscountWithoutRate() {
        CouponSaveRequest req = saveRequest();
        req.setType(CouponService.TYPE_DISCOUNT);
        req.setDiscountRate(null);

        assertThatThrownBy(() -> couponService.create(req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
    }

    @Test
    @DisplayName("创建：非法类型抛 10000")
    void createIllegalType() {
        CouponSaveRequest req = saveRequest();
        req.setType(9);

        assertThatThrownBy(() -> couponService.create(req))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
    }

    @Test
    @DisplayName("创建：正常写入，状态启用、已领数为0")
    void createSuccess() {
        when(couponMapper.insert(any(Coupon.class))).thenAnswer(inv -> {
            inv.getArgument(0, Coupon.class).setId(10L);
            return 1;
        });

        couponService.create(saveRequest());

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponMapper).insert(captor.capture());
        Coupon saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(CouponService.STATUS_ENABLED);
        assertThat(saved.getReceivedCount()).isZero();
    }

    // ==================== 领取 ====================

    @Test
    @DisplayName("领取：券不存在抛 30029")
    void receiveNotFound() {
        when(couponMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> couponService.receive(MEMBER_ID, 10L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("领取：券已停用抛 30031")
    void receiveDisabled() {
        Coupon c = coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, null);
        c.setStatus(CouponService.STATUS_DISABLED);
        when(couponMapper.selectById(10L)).thenReturn(c);

        assertThatThrownBy(() -> couponService.receive(MEMBER_ID, 10L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_NOT_USABLE.getCode());
    }

    @Test
    @DisplayName("领取：超出个人限领抛 30030")
    void receiveExceedsPerLimit() {
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, null));
        when(couponMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> couponService.receive(MEMBER_ID, 10L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_RECEIVE_LIMIT.getCode());

        verify(couponMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("领取：已领完（条件更新影响0行）抛 30030")
    void receiveSoldOut() {
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, null));
        when(couponMemberMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> couponService.receive(MEMBER_ID, 10L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_RECEIVE_LIMIT.getCode());

        verify(couponMemberMapper, never()).insert(any(CouponMember.class));
    }

    @Test
    @DisplayName("领取：正常领取，写未使用记录，过期时间取券的失效时间")
    void receiveSuccess() {
        Coupon c = coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, null);
        when(couponMapper.selectById(10L)).thenReturn(c);
        when(couponMemberMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.update(any(), any())).thenReturn(1);
        when(couponMemberMapper.insert(any(CouponMember.class))).thenAnswer(inv -> {
            inv.getArgument(0, CouponMember.class).setId(500L);
            return 1;
        });

        CouponMemberVO vo = couponService.receive(MEMBER_ID, 10L);

        assertThat(vo.getId()).isEqualTo(500L);
        assertThat(vo.getStatus()).isEqualTo(CouponService.MEMBER_UNUSED);
        assertThat(vo.getName()).isEqualTo("满100减20");
        ArgumentCaptor<CouponMember> captor = ArgumentCaptor.forClass(CouponMember.class);
        verify(couponMemberMapper).insert(captor.capture());
        assertThat(captor.getValue().getExpireTime()).isEqualTo(c.getEndTime());
    }

    // ==================== 抵扣计算 ====================

    @Test
    @DisplayName("抵扣：他人优惠券抛 20001（越权）")
    void discountRejectsOtherMember() {
        CouponMember cm = memberCoupon(CouponService.MEMBER_UNUSED);
        cm.setMemberId(999L);
        when(couponMemberMapper.selectById(500L)).thenReturn(cm);

        assertThatThrownBy(() -> couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("抵扣：已使用的券抛 30033")
    void discountRejectsUsedCoupon() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_USED));

        assertThatThrownBy(() -> couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_STATUS_INVALID.getCode());
    }

    @Test
    @DisplayName("抵扣：已过期的券抛 30031")
    void discountRejectsExpiredCoupon() {
        CouponMember cm = memberCoupon(CouponService.MEMBER_UNUSED);
        cm.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(couponMemberMapper.selectById(500L)).thenReturn(cm);

        assertThatThrownBy(() -> couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_NOT_USABLE.getCode());
    }

    @Test
    @DisplayName("抵扣：未达门槛抛 30032")
    void discountRejectsBelowThreshold() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_UNUSED));
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "300", "20", null, null));

        assertThatThrownBy(() -> couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_THRESHOLD_NOT_MET.getCode());
    }

    @Test
    @DisplayName("抵扣：店铺券用于其他店铺抛 30031")
    void discountRejectsWrongShop() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_UNUSED));
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, 9L));

        assertThatThrownBy(() -> couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), 100L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_NOT_USABLE.getCode());
    }

    @Test
    @DisplayName("抵扣：满减券按固定金额抵扣")
    void discountFullReduce() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_UNUSED));
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, null));

        BigDecimal d = couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null);

        assertThat(d).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("抵扣：折扣券按比例抵扣并保留2位小数")
    void discountByRate() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_UNUSED));
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_DISCOUNT, null, null, "0.85", null));

        BigDecimal d = couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null);

        assertThat(d).isEqualByComparingTo("29.85");
    }

    @Test
    @DisplayName("抵扣：优惠额不超过订单金额")
    void discountCappedByAmount() {
        when(couponMemberMapper.selectById(500L)).thenReturn(memberCoupon(CouponService.MEMBER_UNUSED));
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "500", null, null));

        BigDecimal d = couponService.calculateDiscount(MEMBER_ID, 500L, new BigDecimal("199.00"), null);

        assertThat(d).isEqualByComparingTo("199.00");
    }

    // ==================== 使用 / 退回 ====================

    @Test
    @DisplayName("标记使用：条件更新影响0行抛 30033（防重复使用）")
    void markUsedConflict() {
        when(couponMemberMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> couponService.markUsed(MEMBER_ID, 500L, 100L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_STATUS_INVALID.getCode());
    }

    @Test
    @DisplayName("标记使用：成功更新1行")
    void markUsedSuccess() {
        when(couponMemberMapper.update(any(), any())).thenReturn(1);

        couponService.markUsed(MEMBER_ID, 500L, 100L);

        verify(couponMemberMapper).update(any(), any());
    }

    @Test
    @DisplayName("退回：按订单号把已使用券恢复为未使用并清空订单")
    void returnCoupon() {
        when(couponMemberMapper.update(any(), any())).thenReturn(1);

        couponService.returnCoupon(100L);

        verify(couponMemberMapper).update(any(), any());
    }

    // ==================== 商家端（店铺券） ====================

    @Test
    @DisplayName("商家建券：shopId 强制取自当前商家，忽略前端传值")
    void createForShopForcesShopId() {
        CouponSaveRequest req = saveRequest();
        req.setShopId(999L);
        when(couponMapper.insert(any(Coupon.class))).thenAnswer(inv -> {
            inv.getArgument(0, Coupon.class).setId(10L);
            return 1;
        });

        couponService.createForShop(1L, req);

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponMapper).insert(captor.capture());
        assertThat(captor.getValue().getShopId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("商家建券：无店铺权限抛 20001")
    void createForShopWithoutShopIdIsRejected() {
        assertThatThrownBy(() -> couponService.createForShop(null, saveRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
        verify(couponMapper, never()).insert(any(Coupon.class));
    }

    @Test
    @DisplayName("商家券列表：无店铺权限抛 20001")
    void pageForShopWithoutShopIdIsRejected() {
        assertThatThrownBy(() -> couponService.pageForShop(null, null, null, new PageQuery()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("商家改状态：券不存在抛 30029")
    void updateStatusForShopNotFound() {
        when(couponMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> couponService.updateStatusForShop(1L, 10L, CouponService.STATUS_DISABLED))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.COUPON_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("商家改状态：非本店券抛 20001（越权）")
    void updateStatusForShopRejectsForeignCoupon() {
        when(couponMapper.selectById(10L)).thenReturn(coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, 9L));

        assertThatThrownBy(() -> couponService.updateStatusForShop(1L, 10L, CouponService.STATUS_DISABLED))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());
        verify(couponMapper, never()).updateById(any(Coupon.class));
    }

    @Test
    @DisplayName("商家改状态：本店券可正常停用")
    void updateStatusForShopSucceeds() {
        Coupon c = coupon(CouponService.TYPE_FULL_REDUCE, "100", "20", null, 1L);
        when(couponMapper.selectById(10L)).thenReturn(c);

        couponService.updateStatusForShop(1L, 10L, CouponService.STATUS_DISABLED);

        assertThat(c.getStatus()).isEqualTo(CouponService.STATUS_DISABLED);
        verify(couponMapper).updateById(c);
    }
}
