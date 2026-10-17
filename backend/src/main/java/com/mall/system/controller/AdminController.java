package com.mall.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.system.dto.ShopAuditRequest;
import com.mall.system.entity.Shop;
import com.mall.system.entity.SysUser;
import com.mall.system.mapper.ShopMapper;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.service.ShopApplyService;
import com.mall.system.service.ShopService;
import com.mall.system.vo.ShopApplyVO;
import com.mall.system.vo.ShopVO;
import com.mall.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台端 - 系统管理")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopMapper shopMapper;
    private final ShopService shopService;
    private final ShopApplyService shopApplyService;
    private final SysUserMapper sysUserMapper;

    // ==================== 商家审核 ====================

    @Operation(summary = "待审核入驻申请列表", description = "返回所有待审核的商家入驻申请，最新在前")
    @GetMapping("/shops/pending")
    @RequirePermission("admin:shop:audit")
    public R<List<ShopApplyVO>> pendingShops() {
        return R.ok(shopApplyService.listByStatus(ShopApplyService.APPLY_PENDING));
    }

    @Operation(summary = "入驻申请列表（分页）", description = "平台分页查看全部入驻申请，可按状态过滤")
    @GetMapping("/shops/applies")
    @RequirePermission("admin:shop:audit")
    public R<PageResult<ShopApplyVO>> applyList(@Valid PageQuery pageQuery,
                                                @RequestParam(required = false) Integer status) {
        return R.ok(shopApplyService.page(status, pageQuery));
    }

    @Operation(summary = "审核商家入驻", description = "通过时创建店铺并开通商家账号；拒绝时仅回写状态与备注")
    @PostMapping("/shops/audit")
    @RequirePermission("admin:shop:audit")
    public R<Void> auditShop(@CurrentUser LoginUser user,
                             @Valid @RequestBody ShopAuditRequest req) {
        shopApplyService.audit(req.getApplyId(), req.getStatus(), req.getAuditRemark(),
                req.getUsername(), req.getPassword(), user.userId());
        return R.ok();
    }

    @Operation(summary = "店铺列表（全部）")
    @GetMapping("/shops")
    @RequirePermission("admin:shop:list")
    public R<PageResult<ShopVO>> shopList(@Valid PageQuery pageQuery,
                                          @RequestParam(required = false) Integer status) {
        var wrapper = Wrappers.<Shop>lambdaQuery().orderByDesc(Shop::getCreateTime);
        if (status != null) {
            wrapper.eq(Shop::getStatus, status);
        }
        Page<Shop> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        var result = shopMapper.selectPage(page, wrapper);
        return R.ok(PageResult.of(result, shopService::toVO));
    }

    // ==================== 商家管理 ====================

    @Operation(summary = "商家用户列表", description = "平台查看所有商家用户")
    @GetMapping("/merchants")
    @RequirePermission("admin:merchant:list")
    public R<PageResult<SysUserVO>> merchantList(@Valid PageQuery pageQuery) {
        Page<SysUser> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        var wrapper = Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getType, 2, 3)
                .orderByDesc(SysUser::getCreateTime);
        var result = sysUserMapper.selectPage(page, wrapper);
        return R.ok(PageResult.of(result, this::toSysUserVO));
    }

    private SysUserVO toSysUserVO(SysUser u) {
        SysUserVO vo = new SysUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setType(u.getType());
        vo.setShopId(u.getShopId());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }
}
