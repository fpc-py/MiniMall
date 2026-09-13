package com.mall.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.entity.Shop;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 店铺服务：商家审核、店铺查询。
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopMapper shopMapper;

    /** 店铺状态 */
    public static final int STATUS_PENDING_AUDIT = 0;
    public static final int STATUS_OPEN = 1;
    public static final int STATUS_CLOSED = 2;
    public static final int STATUS_REJECTED = 3;

    public Shop findById(Long id) {
        return id == null ? null : shopMapper.selectById(id);
    }

    public Shop getRequiredById(Long id) {
        Shop shop = findById(id);
        if (shop == null) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "店铺不存在");
        }
        return shop;
    }

    /**
     * 平台审核商家入驻。
     * 通过：创建 shop_shop 记录，关联 sys_user 的 shop_id。
     */
    public void audit(Long applyId, Integer status, String auditRemark, Long auditorId) {
        // 审核逻辑实际由 AdminController 实现
        // 此处提供基础查询方法
    }

    public ShopVO toVO(Shop shop) {
        if (shop == null) return null;
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setLogo(shop.getLogo());
        vo.setContactName(shop.getContactName());
        vo.setContactPhone(shop.getContactPhone());
        vo.setProvince(shop.getProvince());
        vo.setCity(shop.getCity());
        vo.setDistrict(shop.getDistrict());
        vo.setAddress(shop.getAddress());
        vo.setStatus(shop.getStatus());
        vo.setType(shop.getType());
        vo.setScore(shop.getScore());
        vo.setAuditRemark(shop.getAuditRemark());
        vo.setCreateTime(shop.getCreateTime());
        return vo;
    }
}
