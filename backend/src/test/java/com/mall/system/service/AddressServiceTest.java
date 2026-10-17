package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.dto.AddressSaveRequest;
import com.mall.system.entity.Address;
import com.mall.system.mapper.AddressMapper;
import com.mall.system.vo.AddressVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收货地址服务测试：资源归属校验、默认地址唯一性、首个地址自动默认、删除后顺延默认。
 */
@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressMapper addressMapper;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(addressMapper);
    }

    private Address address(Long id, Long memberId) {
        Address a = new Address();
        a.setId(id);
        a.setMemberId(memberId);
        a.setReceiverName("张三");
        a.setReceiverPhone("13800138000");
        a.setProvince("广东省");
        a.setCity("深圳市");
        a.setDistrict("南山区");
        a.setDetail("科技园路1号");
        a.setZipCode("518000");
        a.setLabel("家");
        a.setIsDefault(0);
        return a;
    }

    private AddressSaveRequest request(boolean isDefault) {
        AddressSaveRequest r = new AddressSaveRequest();
        r.setReceiverName("张三");
        r.setReceiverPhone("13800138000");
        r.setProvince("广东省");
        r.setCity("深圳市");
        r.setDistrict("南山区");
        r.setDetail("科技园路1号");
        r.setZipCode("518000");
        r.setLabel("家");
        r.setIsDefault(isDefault);
        return r;
    }

    @Test
    @DisplayName("列表：返回完整地址拼接，memberId 过滤")
    void listReturnsFullAddress() {
        when(addressMapper.selectList(any())).thenReturn(List.of(address(10L, 1L)));

        List<AddressVO> list = addressService.list(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFullAddress()).isEqualTo("广东省深圳市南山区科技园路1号");
        assertThat(list.get(0).getIsDefault()).isZero();
    }

    @Test
    @DisplayName("详情：地址不存在抛 30018")
    void getThrowsWhenNotFound() {
        when(addressMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> addressService.get(1L, 99L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.ADDRESS_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("越权：操作他人地址抛 20001")
    void getOwnedRejectsOtherMember() {
        when(addressMapper.selectById(5L)).thenReturn(address(5L, 2L));

        assertThatThrownBy(() -> addressService.getOwned(1L, 5L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Auth.FORBIDDEN.getCode());

        verify(addressMapper, never()).updateById(any(Address.class));
    }

    @Test
    @DisplayName("新增：首个地址自动设为默认并清除其它默认")
    void createFirstAddressBecomesDefault() {
        when(addressMapper.selectCount(any())).thenReturn(0L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.getArgument(0, Address.class).setId(100L);
            return 1;
        });

        AddressVO vo = addressService.create(1L, request(false));

        assertThat(vo.getIsDefault()).isEqualTo(1);
        verify(addressMapper).update(isNull(), any());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }

    @Test
    @DisplayName("新增：非首个且未指定默认，不触发清默认")
    void createNonDefaultAddress() {
        when(addressMapper.selectCount(any())).thenReturn(3L);
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.getArgument(0, Address.class).setId(101L);
            return 1;
        });

        AddressVO vo = addressService.create(1L, request(false));

        assertThat(vo.getIsDefault()).isZero();
        verify(addressMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("新增：显式指定默认时清除其它默认")
    void createExplicitDefaultClearsOthers() {
        when(addressMapper.insert(any(Address.class))).thenAnswer(inv -> {
            inv.getArgument(0, Address.class).setId(102L);
            return 1;
        });

        AddressVO vo = addressService.create(1L, request(true));

        assertThat(vo.getIsDefault()).isEqualTo(1);
        verify(addressMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("修改：非默认修改不触发清默认，仅 updateById")
    void updateNonDefault() {
        Address existing = address(10L, 1L);
        existing.setProvince("旧省");
        when(addressMapper.selectById(10L)).thenReturn(existing);

        AddressVO vo = addressService.update(1L, 10L, request(false));

        assertThat(vo.getProvince()).isEqualTo("广东省");
        assertThat(vo.getReceiverName()).isEqualTo("张三");
        verify(addressMapper).updateById(existing);
        verify(addressMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("修改：设为默认时清除其它默认并更新自身")
    void updateAsDefaultClearsOthers() {
        Address existing = address(10L, 1L);
        when(addressMapper.selectById(10L)).thenReturn(existing);

        addressService.update(1L, 10L, request(true));

        verify(addressMapper).update(isNull(), any());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateById(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }

    @Test
    @DisplayName("删除默认地址：顺延剩余地址为默认")
    void deleteDefaultPromotesNext() {
        Address def = address(10L, 1L);
        def.setIsDefault(1);
        when(addressMapper.selectById(10L)).thenReturn(def);
        when(addressMapper.selectList(any())).thenReturn(List.of(address(11L, 1L)));

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteById(10L);
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(11L);
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }

    @Test
    @DisplayName("删除非默认地址：不触发顺延")
    void deleteNonDefaultDoesNotPromote() {
        Address normal = address(10L, 1L);
        when(addressMapper.selectById(10L)).thenReturn(normal);

        addressService.delete(1L, 10L);

        verify(addressMapper).deleteById(10L);
        verify(addressMapper, never()).updateById(any(Address.class));
    }

    @Test
    @DisplayName("设为默认：清除其它默认并把目标置为默认")
    void setDefaultClearsOthers() {
        when(addressMapper.selectById(10L)).thenReturn(address(10L, 1L));

        addressService.setDefault(1L, 10L);

        verify(addressMapper).update(isNull(), any());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
    }
}
