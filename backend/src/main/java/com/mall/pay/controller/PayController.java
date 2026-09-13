package com.mall.pay.controller;

import com.mall.common.R;
import com.mall.common.security.CurrentUser;
import com.mall.common.security.LoginUser;
import com.mall.pay.dto.PayCallbackRequest;
import com.mall.pay.dto.PayCreateRequest;
import com.mall.pay.service.PayService;
import com.mall.pay.vo.PayCreateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @Operation(summary = "发起支付", description = "创建支付记录，返回沙箱支付页 URL")
    @PostMapping("/create")
    public R<PayCreateVO> create(@CurrentUser LoginUser user,
                                 @Valid @RequestBody PayCreateRequest req) {
        return R.ok(payService.create(user.userId(), req));
    }

    @Operation(summary = "支付回调", description = "第三方回调，幂等处理。重复回调直接返回成功")
    @PostMapping("/callback")
    public R<Boolean> callback(@Valid @RequestBody PayCallbackRequest req) {
        return R.ok(payService.callback(req));
    }
}
