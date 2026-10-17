package com.mall.order.controller;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.common.security.RequirePermission;
import com.mall.order.dto.RefundAuditRequest;
import com.mall.order.service.RefundService;
import com.mall.order.vo.RefundVO;
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

@Tag(name = "商家端 - 退款/售后")
@RestController
@RequestMapping("/api/shop/refunds")
@RequiredArgsConstructor
public class ShopRefundController {

    private final RefundService refundService;

    @Operation(summary = "店铺退款单列表", description = "商家查看本店铺退款申请，可按状态筛选")
    @GetMapping
    @RequirePermission("shop:refund:list")
    public R<PageResult<RefundVO>> list(@CurrentUser LoginUser user,
                                        @Valid PageQuery pageQuery,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(refundService.pageByShop(getShopId(user), status, pageQuery));
    }

    @Operation(summary = "审核退款", description = "商家同意退款（原路退回并回补库存）或拒绝")
    @PostMapping("/audit")
    @RequirePermission("shop:refund:audit")
    public R<Void> audit(@CurrentUser LoginUser user,
                         @Valid @RequestBody RefundAuditRequest req) {
        refundService.audit(getShopId(user), req.getRefundId(), req.getStatus(),
                req.getAuditRemark(), user.userId());
        return R.ok();
    }

    private Long getShopId(LoginUser user) {
        if (user == null || user.shopId() == null) {
            throw new MallException(ErrorCode.Auth.FORBIDDEN, "当前用户无店铺权限");
        }
        return user.shopId();
    }
}
