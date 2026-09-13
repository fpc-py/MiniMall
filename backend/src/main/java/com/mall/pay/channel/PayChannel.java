package com.mall.pay.channel;

import java.math.BigDecimal;

/**
 * 支付渠道抽象接口。
 * 生产环境替换为真实支付宝/微信实现，沙箱实现仅用于本地联调。
 */
public interface PayChannel {

    /**
     * 发起支付，返回支付凭证（URL / 二维码等）。
     *
     * @param transactionNo 内部交易号
     * @param amount        支付金额
     * @param payMethod     1 余额 2 支付宝 3 微信
     * @return 支付凭证
     */
    PayResult pay(String transactionNo, BigDecimal amount, int payMethod);

    /**
     * 校验回调签名/合法性。
     *
     * @param callbackContent 回调原始内容
     * @return true=合法
     */
    boolean verifyCallback(String callbackContent);

    /**
     * 支付结果。
     */
    record PayResult(String transactionNo, String payUrl, boolean success, String message) {}
}
