package com.mall.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.dto.AddressSaveRequest;
import com.mall.system.entity.Address;
import com.mall.system.mapper.AddressMapper;
import com.mall.system.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务。
 * <p>
 * 所有对外写操作都校验「资源归属」：地址必须属于当前登录会员，否则抛 FORBIDDEN。
 * 默认地址在同一会员下唯一：设为默认时清除其它地址的默认标记。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int DEFAULT_YES = 1;
    private static final int DEFAULT_NO = 0;

    private final AddressMapper addressMapper;

    public List<AddressVO> list(Long memberId) {
        requireMember(memberId);
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<Address>()
                .eq(Address::getMemberId, memberId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId);
        return addressMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public AddressVO get(Long memberId, Long id) {
        return toVO(getOwned(memberId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long memberId, AddressSaveRequest req) {
        requireMember(memberId);
        Address entity = new Address();
        entity.setMemberId(memberId);
        applyRequest(entity, req);

        boolean asDefault = Boolean.TRUE.equals(req.getIsDefault());
        // 首个地址自动设为默认
        if (!asDefault && countByMember(memberId) == 0) {
            asDefault = true;
        }
        if (asDefault) {
            clearDefault(memberId);
        }
        entity.setIsDefault(asDefault ? DEFAULT_YES : DEFAULT_NO);

        addressMapper.insert(entity);
        log.info("新增收货地址: memberId={}, addressId={}", memberId, entity.getId());
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long memberId, Long id, AddressSaveRequest req) {
        Address entity = getOwned(memberId, id);
        applyRequest(entity, req);

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearDefault(memberId);
            entity.setIsDefault(DEFAULT_YES);
        }

        addressMapper.updateById(entity);
        log.info("修改收货地址: memberId={}, addressId={}", memberId, id);
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long id) {
        Address entity = getOwned(memberId, id);
        boolean wasDefault = entity.getIsDefault() != null && entity.getIsDefault() == DEFAULT_YES;
        addressMapper.deleteById(id);
        log.info("删除收货地址: memberId={}, addressId={}", memberId, id);

        // 删除的是默认地址时，顺延一个默认地址，保证始终有默认可用
        if (wasDefault) {
            List<Address> remain = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                    .eq(Address::getMemberId, memberId)
                    .orderByDesc(Address::getId));
            if (!remain.isEmpty()) {
                Address next = remain.get(0);
                Address patch = new Address();
                patch.setId(next.getId());
                patch.setIsDefault(DEFAULT_YES);
                addressMapper.updateById(patch);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long id) {
        Address entity = getOwned(memberId, id);
        clearDefault(memberId);
        Address patch = new Address();
        patch.setId(entity.getId());
        patch.setIsDefault(DEFAULT_YES);
        addressMapper.updateById(patch);
        log.info("设置默认地址: memberId={}, addressId={}", memberId, id);
    }

    /** 下单时校验地址归属并返回实体，供订单模块使用。 */
    public Address getOwned(Long memberId, Long id) {
        requireMember(memberId);
        if (id == null) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "地址ID不能为空");
        }
        Address entity = addressMapper.selectById(id);
        if (entity == null) {
            throw new MallException(ErrorCode.Biz.ADDRESS_NOT_FOUND);
        }
        if (!memberId.equals(entity.getMemberId())) {
            // 越权访问他人地址
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "无权操作该收货地址");
        }
        return entity;
    }

    // ==================== 私有方法 ====================

    private void requireMember(Long memberId) {
        if (memberId == null) {
            throw new MallException(ErrorCode.Auth.UNAUTHORIZED);
        }
    }

    private long countByMember(Long memberId) {
        return addressMapper.selectCount(new LambdaQueryWrapper<Address>().eq(Address::getMemberId, memberId));
    }

    private void clearDefault(Long memberId) {
        addressMapper.update(null, Wrappers.<Address>update()
                .eq("member_id", memberId)
                .set("is_default", DEFAULT_NO));
    }

    private void applyRequest(Address entity, AddressSaveRequest req) {
        entity.setReceiverName(req.getReceiverName());
        entity.setReceiverPhone(req.getReceiverPhone());
        entity.setProvince(req.getProvince());
        entity.setCity(req.getCity());
        entity.setDistrict(req.getDistrict());
        entity.setDetail(req.getDetail());
        entity.setZipCode(req.getZipCode());
        entity.setLabel(req.getLabel());
    }

    private AddressVO toVO(Address entity) {
        AddressVO vo = new AddressVO();
        vo.setId(entity.getId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetail(entity.getDetail());
        vo.setZipCode(entity.getZipCode());
        vo.setLabel(entity.getLabel());
        vo.setIsDefault(entity.getIsDefault());
        vo.setFullAddress(buildFullAddress(entity));
        return vo;
    }

    private String buildFullAddress(Address entity) {
        StringBuilder sb = new StringBuilder();
        if (entity.getProvince() != null) {
            sb.append(entity.getProvince());
        }
        if (entity.getCity() != null) {
            sb.append(entity.getCity());
        }
        if (entity.getDistrict() != null) {
            sb.append(entity.getDistrict());
        }
        if (entity.getDetail() != null) {
            sb.append(entity.getDetail());
        }
        return sb.toString();
    }
}
