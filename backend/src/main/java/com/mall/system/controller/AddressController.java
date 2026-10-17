package com.mall.system.controller;

import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.system.dto.AddressSaveRequest;
import com.mall.system.service.AddressService;
import com.mall.system.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收货地址")
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表", description = "默认地址置顶；memberId 取自 Token")
    @GetMapping
    public R<List<AddressVO>> list(@CurrentUser LoginUser user) {
        return R.ok(addressService.list(user.userId()));
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public R<AddressVO> detail(@CurrentUser LoginUser user, @PathVariable("id") Long id) {
        return R.ok(addressService.get(user.userId(), id));
    }

    @Operation(summary = "新增地址", description = "首个地址自动设为默认")
    @PostMapping
    public R<AddressVO> create(@CurrentUser LoginUser user, @Valid @RequestBody AddressSaveRequest req) {
        return R.ok(addressService.create(user.userId(), req));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public R<AddressVO> update(@CurrentUser LoginUser user,
                               @PathVariable("id") Long id,
                               @Valid @RequestBody AddressSaveRequest req) {
        return R.ok(addressService.update(user.userId(), id, req));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@CurrentUser LoginUser user, @PathVariable("id") Long id) {
        addressService.delete(user.userId(), id);
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@CurrentUser LoginUser user, @PathVariable("id") Long id) {
        addressService.setDefault(user.userId(), id);
        return R.ok();
    }
}
