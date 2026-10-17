package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.dto.ShopApplySaveRequest;
import com.mall.system.entity.Shop;
import com.mall.system.entity.ShopApply;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.ShopApplyMapper;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.vo.ShopApplyVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商家入驻服务测试：重复申请拦截、审核建店+开账号、拒绝仅回写、账号密码加密。
 */
@ExtendWith(MockitoExtension.class)
class ShopApplyServiceTest {

    @Mock
    private ShopApplyMapper shopApplyMapper;
    @Mock
    private ShopMapper shopMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ShopApplyService service;

    @BeforeEach
    void setUp() {
        service = new ShopApplyService(shopApplyMapper, shopMapper, sysUserMapper, passwordEncoder);
    }

    private ShopApplySaveRequest request() {
        ShopApplySaveRequest req = new ShopApplySaveRequest();
        req.setShopName("小米旗舰店");
        req.setContactName("王经理");
        req.setContactPhone("13800138000");
        req.setProvince("广东省");
        req.setCity("深圳市");
        req.setDistrict("南山区");
        req.setAddress("科技园路1号");
        return req;
    }

    private ShopApply apply(Long id, int status) {
        ShopApply a = new ShopApply();
        a.setId(id);
        a.setMemberId(1L);
        a.setShopName("小米旗舰店");
        a.setContactName("王经理");
        a.setContactPhone("13800138000");
        a.setStatus(status);
        return a;
    }

    @Test
    @DisplayName("提交：已有待审核/已通过申请时抛 30019")
    void submitRejectsDuplicate() {
        when(shopApplyMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.submit(1L, request()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.SHOP_APPLY_EXISTS.getCode());

        verify(shopApplyMapper, never()).insert(any(ShopApply.class));
    }

    @Test
    @DisplayName("提交：正常写入待审核申请，memberId 来自入参")
    void submitCreatesPendingApply() {
        when(shopApplyMapper.selectCount(any())).thenReturn(0L);
        when(shopApplyMapper.insert(any(ShopApply.class))).thenAnswer(inv -> {
            inv.getArgument(0, ShopApply.class).setId(100L);
            return 1;
        });

        ShopApplyVO vo = service.submit(1L, request());

        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getStatus()).isEqualTo(ShopApplyService.APPLY_PENDING);
        ArgumentCaptor<ShopApply> captor = ArgumentCaptor.forClass(ShopApply.class);
        verify(shopApplyMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getShopName()).isEqualTo("小米旗舰店");
    }

    @Test
    @DisplayName("审核：申请不存在抛 30017")
    void auditMissingApply() {
        when(shopApplyMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.audit(99L, 1, null, "shop1", "123456", 9L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.AUDIT_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("审核：已处理的申请不能重复审核，抛 30020")
    void auditAlreadyHandled() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_APPROVED));

        assertThatThrownBy(() -> service.audit(10L, 1, null, "shop1", "123456", 9L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.SHOP_APPLY_HANDLED.getCode());

        verify(shopMapper, never()).insert(any(Shop.class));
    }

    @Test
    @DisplayName("审核通过：创建店铺(启用/第三方) + 商家账号(BCrypt) + 回写申请")
    void auditApproveCreatesShopAndMerchant() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_PENDING));
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("pwd123")).thenReturn("$2a$hashed");
        when(shopMapper.insert(any(Shop.class))).thenAnswer(inv -> {
            inv.getArgument(0, Shop.class).setId(500L);
            return 1;
        });
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysUser.class).setId(900L);
            return 1;
        });

        service.audit(10L, ShopApplyService.APPLY_APPROVED, "资料齐全", "shop_leader", "pwd123", 9L);

        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopMapper).insert(shopCaptor.capture());
        assertThat(shopCaptor.getValue().getStatus()).isEqualTo(ShopService.STATUS_OPEN);
        assertThat(shopCaptor.getValue().getType()).isEqualTo(1);
        assertThat(shopCaptor.getValue().getName()).isEqualTo("小米旗舰店");

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser merchant = userCaptor.getValue();
        assertThat(merchant.getUsername()).isEqualTo("shop_leader");
        assertThat(merchant.getPassword()).isEqualTo("$2a$hashed");
        assertThat(merchant.getType()).isEqualTo(2);
        assertThat(merchant.getStatus()).isEqualTo(1);
        assertThat(merchant.getShopId()).isEqualTo(500L);

        ArgumentCaptor<ShopApply> applyCaptor = ArgumentCaptor.forClass(ShopApply.class);
        verify(shopApplyMapper).updateById(applyCaptor.capture());
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(ShopApplyService.APPLY_APPROVED);
        assertThat(applyCaptor.getValue().getShopId()).isEqualTo(500L);
        assertThat(applyCaptor.getValue().getAuditTime()).isNotNull();
        assertThat(applyCaptor.getValue().getAuditorId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("审核通过：缺少账号或密码抛 10001，不建店")
    void auditApproveRequiresCredential() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_PENDING));

        assertThatThrownBy(() -> service.audit(10L, ShopApplyService.APPLY_APPROVED, null, "shop_leader", " ", 9L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.MISSING_PARAM.getCode());

        verify(shopMapper, never()).insert(any(Shop.class));
    }

    @Test
    @DisplayName("审核通过：商家账号已存在抛 30021，不建店")
    void auditApproveRejectsDuplicateUsername() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_PENDING));
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.audit(10L, ShopApplyService.APPLY_APPROVED, null, "shop_leader", "pwd123", 9L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.SHOP_USERNAME_EXISTS.getCode());

        verify(shopMapper, never()).insert(any(Shop.class));
    }

    @Test
    @DisplayName("审核拒绝：仅回写申请状态与备注，不建店不建号")
    void auditRejectOnlyUpdatesApply() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_PENDING));

        service.audit(10L, ShopApplyService.APPLY_REJECTED, "资质不全", null, null, 9L);

        verify(shopMapper, never()).insert(any(Shop.class));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
        ArgumentCaptor<ShopApply> captor = ArgumentCaptor.forClass(ShopApply.class);
        verify(shopApplyMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ShopApplyService.APPLY_REJECTED);
        assertThat(captor.getValue().getAuditRemark()).isEqualTo("资质不全");
        assertThat(captor.getValue().getAuditTime()).isNotNull();
    }

    @Test
    @DisplayName("审核：非法状态抛 10000，不做任何写操作")
    void auditInvalidStatus() {
        when(shopApplyMapper.selectById(10L)).thenReturn(apply(10L, ShopApplyService.APPLY_PENDING));

        assertThatThrownBy(() -> service.audit(10L, 9, null, null, null, 9L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());

        verify(shopMapper, never()).insert(any(Shop.class));
        verify(shopApplyMapper, never()).updateById(any(ShopApply.class));
    }
}
