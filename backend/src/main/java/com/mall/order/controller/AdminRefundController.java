package com.mall.order.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.order.service.RefundService;
import com.mall.order.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台端 - 退款/售后")
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @Operation(summary = "退款单列表（全平台）")
    @GetMapping
    @RequirePermission("admin:refund:list")
    public R<PageResult<RefundVO>> list(@Valid PageQuery pageQuery,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(refundService.pageAll(status, pageQuery));
    }
}
