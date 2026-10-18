package com.mall.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.dto.ShopInfoUpdateRequest;
import com.mall.system.entity.Shop;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.vo.ShopVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商家店铺信息读写测试：状态/审核字段不可被商家自改。
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopMapper shopMapper;

    private ShopService shopService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Shop.class);
    }

    @BeforeEach
    void setUp() {
        shopService = new ShopService(shopMapper);
    }

    private Shop shop() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("官方自营旗舰店");
        shop.setDescription("正品保障");
        shop.setContactName("张老板");
        shop.setContactPhone("13900000000");
        shop.setProvince("广东省");
        shop.setCity("深圳市");
        shop.setDistrict("南山区");
        shop.setAddress("科技园1号");
        shop.setStatus(ShopService.STATUS_OPEN);
        shop.setType(0);
        shop.setScore(new BigDecimal("5.00"));
        return shop;
    }

    private ShopInfoUpdateRequest updateRequest() {
        ShopInfoUpdateRequest req = new ShopInfoUpdateRequest();
        req.setContactName("李老板");
        req.setContactPhone("13700000000");
        req.setProvince("浙江省");
        req.setCity("杭州市");
        req.setDistrict("西湖区");
        req.setAddress("文一西路100号");
        req.setDescription("新简介");
        return req;
    }

    @Test
    @DisplayName("查看本店信息：返回含简介的店铺视图")
    void getInfoReturnsShopVo() {
        when(shopMapper.selectById(1L)).thenReturn(shop());

        ShopVO vo = shopService.getInfo(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("官方自营旗舰店");
        assertThat(vo.getDescription()).isEqualTo("正品保障");
        assertThat(vo.getStatus()).isEqualTo(ShopService.STATUS_OPEN);
    }

    @Test
    @DisplayName("查看本店信息：店铺不存在抛 10000")
    void getInfoThrowsWhenMissing() {
        when(shopMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> shopService.getInfo(1L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
    }

    @Test
    @DisplayName("修改本店信息：仅写展示字段，状态/评分/类型保持不变")
    void updateInfoOnlyTouchesDisplayFields() {
        when(shopMapper.selectById(1L)).thenReturn(shop());

        ShopVO vo = shopService.updateInfo(1L, updateRequest());

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopMapper).updateById(captor.capture());
        Shop saved = captor.getValue();
        assertThat(saved.getContactName()).isEqualTo("李老板");
        assertThat(saved.getContactPhone()).isEqualTo("13700000000");
        assertThat(saved.getProvince()).isEqualTo("浙江省");
        assertThat(saved.getCity()).isEqualTo("杭州市");
        assertThat(saved.getDistrict()).isEqualTo("西湖区");
        assertThat(saved.getAddress()).isEqualTo("文一西路100号");
        assertThat(saved.getDescription()).isEqualTo("新简介");
        assertThat(saved.getStatus()).isEqualTo(ShopService.STATUS_OPEN);
        assertThat(saved.getType()).isZero();
        assertThat(vo.getDescription()).isEqualTo("新简介");
    }

    @Test
    @DisplayName("修改本店信息：店铺不存在时不写库")
    void updateInfoThrowsWhenMissing() {
        when(shopMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> shopService.updateInfo(1L, updateRequest()))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
        verify(shopMapper, never()).updateById(any(Shop.class));
    }
}
