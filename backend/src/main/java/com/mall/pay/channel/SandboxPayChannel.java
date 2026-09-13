package com.mall.pay.channel;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 沙箱支付渠道：模拟第三方支付，生成假交易号和支付页 URL。
 * 生产环境替换为真实 PayChannel 实现，不影响业务层代码。
 */
@Slf4j
@Component
public class SandboxPayChannel implements PayChannel {

    private static final String SANDBOX_PAY_URL_PREFIX = "http://localhost:8080/sandbox/pay?tx=";

    @Override
    public PayResult pay(String transactionNo, BigDecimal amount, int payMethod) {
        log.info("沙箱支付: txNo={}, amount={}, method={}", transactionNo, amount, payMethod);
        String payUrl = SANDBOX_PAY_URL_PREFIX + transactionNo;
        return new PayResult(transactionNo, payUrl, true, "沙箱支付已创建，请在沙箱页面完成支付");
    }

    @Override
    public boolean verifyCallback(String callbackContent) {
        // 沙箱模式不做签名校验，直接放行
        return true;
    }
}
