package com.mall.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.dto.ShopInfoUpdateRequest;
import com.mall.system.entity.Shop;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 商家查看本店信息。 */
    public ShopVO getInfo(Long shopId) {
        return toVO(getRequiredById(shopId));
    }

    /** 商家维护本店信息：仅允许修改联系人与地址等展示字段，状态/审核信息不可自改。 */
    @Transactional(rollbackFor = Exception.class)
    public ShopVO updateInfo(Long shopId, ShopInfoUpdateRequest req) {
        Shop shop = getRequiredById(shopId);
        shop.setContactName(req.getContactName());
        shop.setContactPhone(req.getContactPhone());
        shop.setProvince(req.getProvince());
        shop.setCity(req.getCity());
        shop.setDistrict(req.getDistrict());
        shop.setAddress(req.getAddress());
        shop.setDescription(req.getDescription());
        shopMapper.updateById(shop);
        return toVO(shop);
    }

    public ShopVO toVO(Shop shop) {
        if (shop == null) return null;
        ShopVO vo = new ShopVO();
        vo.setId(shop.getId());
        vo.setName(shop.getName());
        vo.setLogo(shop.getLogo());
        vo.setDescription(shop.getDescription());
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
