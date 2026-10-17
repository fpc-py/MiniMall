package com.mall.order.controller;

import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.order.dto.RefundApplyRequest;
import com.mall.order.service.RefundService;
import com.mall.order.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "买家端 - 退款/售后")
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "申请退款", description = "会员对自己已支付的订单发起退款申请")
    @PostMapping
    public R<RefundVO> apply(@CurrentUser LoginUser user,
                             @Valid @RequestBody RefundApplyRequest req) {
        return R.ok(refundService.apply(user.userId(), req));
    }

    @Operation(summary = "撤销退款申请", description = "仅待审核的退款申请可撤销")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@CurrentUser LoginUser user,
                          @PathVariable("id") Long id) {
        refundService.cancel(user.userId(), id);
        return R.ok();
    }

    @Operation(summary = "我的退款单", description = "memberId 取自 Token")
    @GetMapping("/my")
    public R<PageResult<RefundVO>> myRefunds(@CurrentUser LoginUser user,
                                             @Valid PageQuery pageQuery,
                                             @RequestParam(required = false) Integer status) {
        return R.ok(refundService.pageByMember(user.userId(), status, pageQuery));
    }
}
