package com.mall.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.system.dto.ShopApplySaveRequest;
import com.mall.system.entity.Shop;
import com.mall.system.entity.ShopApply;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.ShopApplyMapper;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.vo.ShopApplyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家入驻申请服务：会员自助提交 -> 平台审核 -> 通过后建店 + 开商家账号。
 * <p>
 * 铁律：审核建店与开账号必须在同一本地事务内完成，任一步失败整体回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopApplyService {

    /** 申请状态：0待审核 1通过 2拒绝 */
    public static final int APPLY_PENDING = 0;
    public static final int APPLY_APPROVED = 1;
    public static final int APPLY_REJECTED = 2;

    /** 新建店铺：0自营 1第三方 */
    private static final int SHOP_TYPE_THIRD_PARTY = 1;
    /** 商家账号类型：0平台超管 1平台运营 2商家管理员 3商家员工 */
    private static final int SYS_USER_TYPE_SHOP_ADMIN = 2;
    private static final int ENABLED = 1;

    private final ShopApplyMapper shopApplyMapper;
    private final ShopMapper shopMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /** 会员提交入驻申请。同一会员同时只能存在一条「待审核/已通过」的申请。 */
    @Transactional(rollbackFor = Exception.class)
    public ShopApplyVO submit(Long memberId, ShopApplySaveRequest req) {
        requireMember(memberId);

        Long existing = shopApplyMapper.selectCount(Wrappers.<ShopApply>lambdaQuery()
                .eq(ShopApply::getMemberId, memberId)
                .in(ShopApply::getStatus, APPLY_PENDING, APPLY_APPROVED));
        if (existing != null && existing > 0) {
            throw new MallException(ErrorCode.Biz.SHOP_APPLY_EXISTS);
        }

        ShopApply apply = new ShopApply();
        apply.setMemberId(memberId);
        apply.setShopName(req.getShopName());
        apply.setShopLogo(req.getShopLogo());
        apply.setContactName(req.getContactName());
        apply.setContactPhone(req.getContactPhone());
        apply.setProvince(req.getProvince());
        apply.setCity(req.getCity());
        apply.setDistrict(req.getDistrict());
        apply.setAddress(req.getAddress());
        apply.setBusinessLicense(req.getBusinessLicense());
        apply.setIdCardFront(req.getIdCardFront());
        apply.setIdCardBack(req.getIdCardBack());
        apply.setStatus(APPLY_PENDING);

        shopApplyMapper.insert(apply);
        log.info("提交商家入驻申请: memberId={}, applyId={}, shopName={}", memberId, apply.getId(), apply.getShopName());
        return toVO(apply);
    }

    /** 会员查看自己的入驻申请（最新在前）。 */
    public List<ShopApplyVO> listByMember(Long memberId) {
        requireMember(memberId);
        return shopApplyMapper.selectList(Wrappers.<ShopApply>lambdaQuery()
                        .eq(ShopApply::getMemberId, memberId)
                        .orderByDesc(ShopApply::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    /** 平台分页查询入驻申请，可按状态过滤。 */
    public PageResult<ShopApplyVO> page(Integer status, PageQuery pageQuery) {
        var wrapper = Wrappers.<ShopApply>lambdaQuery().orderByDesc(ShopApply::getCreateTime);
        if (status != null) {
            wrapper.eq(ShopApply::getStatus, status);
        }
        Page<ShopApply> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        return PageResult.of(shopApplyMapper.selectPage(page, wrapper), this::toVO);
    }

    /** 平台按状态查询全部入驻申请（不分页），用于待办列表。 */
    public List<ShopApplyVO> listByStatus(Integer status) {
        return shopApplyMapper.selectList(Wrappers.<ShopApply>lambdaQuery()
                        .eq(status != null, ShopApply::getStatus, status)
                        .orderByDesc(ShopApply::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    /**
     * 平台审核入驻申请。
     * 通过：创建 shop_shop(status=1,type=1) + 创建商家 sys_user(type=2, 密码 BCrypt) + 回写申请。
     * 拒绝：仅回写申请状态与备注。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long applyId, Integer status, String auditRemark,
                      String username, String password, Long auditorId) {
        ShopApply apply = shopApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new MallException(ErrorCode.Biz.AUDIT_NOT_FOUND);
        }
        if (apply.getStatus() == null || apply.getStatus() != APPLY_PENDING) {
            throw new MallException(ErrorCode.Biz.SHOP_APPLY_HANDLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (status != null && status == APPLY_APPROVED) {
            createShopAndMerchant(apply, username, password, auditRemark, auditorId, now);
        } else if (status != null && status == APPLY_REJECTED) {
            apply.setStatus(APPLY_REJECTED);
            apply.setAuditRemark(auditRemark);
            apply.setAuditTime(now);
            apply.setAuditorId(auditorId);
            shopApplyMapper.updateById(apply);
            log.info("驳回商家入驻申请: applyId={}, auditorId={}", applyId, auditorId);
        } else {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "审核结果只能为 1通过 或 2拒绝");
        }
    }

    // ==================== 私有方法 ====================

    private void createShopAndMerchant(ShopApply apply, String username, String password,
                                       String auditRemark, Long auditorId, LocalDateTime now) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "通过审核时必须提供商家登录账号与初始密码");
        }
        String uname = username.trim();
        Long dup = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, uname));
        if (dup != null && dup > 0) {
            throw new MallException(ErrorCode.Biz.SHOP_USERNAME_EXISTS);
        }

        Shop shop = new Shop();
        shop.setName(apply.getShopName());
        shop.setLogo(apply.getShopLogo());
        shop.setContactName(apply.getContactName());
        shop.setContactPhone(apply.getContactPhone());
        shop.setProvince(apply.getProvince());
        shop.setCity(apply.getCity());
        shop.setDistrict(apply.getDistrict());
        shop.setAddress(apply.getAddress());
        shop.setStatus(ShopService.STATUS_OPEN);
        shop.setType(SHOP_TYPE_THIRD_PARTY);
        shop.setAuditRemark(auditRemark);
        shopMapper.insert(shop);

        SysUser merchant = new SysUser();
        merchant.setUsername(uname);
        merchant.setPassword(passwordEncoder.encode(password));
        merchant.setRealName(apply.getContactName());
        merchant.setPhone(apply.getContactPhone());
        merchant.setShopId(shop.getId());
        merchant.setStatus(ENABLED);
        merchant.setType(SYS_USER_TYPE_SHOP_ADMIN);
        sysUserMapper.insert(merchant);

        apply.setStatus(APPLY_APPROVED);
        apply.setShopId(shop.getId());
        apply.setAuditRemark(auditRemark);
        apply.setAuditTime(now);
        apply.setAuditorId(auditorId);
        shopApplyMapper.updateById(apply);

        log.info("通过商家入驻申请: applyId={}, shopId={}, merchantId={}, auditorId={}",
                apply.getId(), shop.getId(), merchant.getId(), auditorId);
    }

    private void requireMember(Long memberId) {
        if (memberId == null) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
    }

    private ShopApplyVO toVO(ShopApply apply) {
        ShopApplyVO vo = new ShopApplyVO();
        vo.setId(apply.getId());
        vo.setMemberId(apply.getMemberId());
        vo.setShopName(apply.getShopName());
        vo.setShopLogo(apply.getShopLogo());
        vo.setContactName(apply.getContactName());
        vo.setContactPhone(apply.getContactPhone());
        vo.setProvince(apply.getProvince());
        vo.setCity(apply.getCity());
        vo.setDistrict(apply.getDistrict());
        vo.setAddress(apply.getAddress());
        vo.setBusinessLicense(apply.getBusinessLicense());
        vo.setIdCardFront(apply.getIdCardFront());
        vo.setIdCardBack(apply.getIdCardBack());
        vo.setStatus(apply.getStatus());
        vo.setAuditRemark(apply.getAuditRemark());
        vo.setAuditTime(apply.getAuditTime());
        vo.setShopId(apply.getShopId());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }
}
