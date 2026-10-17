package com.mall.common;

public interface ErrorCode {
    int getCode();
    String getMsg();

    // ====== 1xxxx 参数错误 ======
    enum Param implements ErrorCode {
        INVALID_PARAM(10000, "参数错误"),
        MISSING_PARAM(10001, "缺少必要参数"),
        INVALID_FORMAT(10002, "参数格式错误");

        private final int code;
        private final String msg;

        Param(int code, String msg) { this.code = code; this.msg = msg; }
        public int getCode() { return code; }
        public String getMsg() { return msg; }
    }

    // ====== 2xxxx 认证/授权 ======
    enum Auth implements ErrorCode {
        UNAUTHORIZED(20000, "未登录或Token已过期"),
        FORBIDDEN(20001, "无权限访问"),
        INVALID_TOKEN(20002, "无效Token"),
        ACCOUNT_DISABLED(20003, "账号已被禁用"),
        SMS_CODE_INVALID(20004, "验证码错误"),
        SMS_CODE_EXPIRED(20005, "验证码已过期或未发送"),
        SMS_SEND_TOO_FREQUENT(20006, "验证码发送过于频繁，请稍后再试"),
        SMS_CODE_FAIL_LIMIT(20007, "验证码错误次数过多，请重新获取"),
        NO_PERMISSION(20008, "没有操作权限"),
        NOT_SHOP_USER(20009, "当前用户不是商家用户");

        private final int code;
        private final String msg;

        Auth(int code, String msg) { this.code = code; this.msg = msg; }
        public int getCode() { return code; }
        public String getMsg() { return msg; }
    }

    // ====== 3xxxx 业务错误 ======
    enum Biz implements ErrorCode {
        PRODUCT_NOT_FOUND(30000, "商品不存在"),
        PRODUCT_OFFLINE(30001, "商品已下架"),
        STOCK_NOT_ENOUGH(30002, "库存不足"),
        ORDER_NOT_FOUND(30003, "订单不存在"),
        ORDER_STATUS_INVALID(30004, "订单状态不允许操作"),
        CART_IS_EMPTY(30005, "购物车为空"),
        CART_ITEM_NOT_FOUND(30006, "购物车项不存在"),
        CART_LIMIT_EXCEEDED(30007, "购物车数量超限"),
        PRICE_CHANGED(30008, "商品价格已变更"),
        PAY_AMOUNT_MISMATCH(30009, "支付金额不匹配"),
        DUPLICATE_PAYMENT(30010, "重复支付"),
        REFUND_NOT_ALLOWED(30011, "不允许退款"),
        MEMBER_NOT_FOUND(30012, "用户不存在"),
        CATEGORY_NOT_FOUND(30013, "商品分类不存在"),
        SKU_NOT_FOUND(30014, "商品规格不存在"),
        SHOP_NOT_FOUND(30015, "店铺不存在"),
        SHOP_NOT_OPEN(30016, "店铺未启用"),
        AUDIT_NOT_FOUND(30017, "审核申请不存在"),
        ADDRESS_NOT_FOUND(30018, "收货地址不存在"),
        SHOP_APPLY_EXISTS(30019, "已存在待审核或已通过的入驻申请"),
        SHOP_APPLY_HANDLED(30020, "该入驻申请已处理，不能重复审核"),
        SHOP_USERNAME_EXISTS(30021, "商家账号已存在"),
        FILE_UPLOAD_FAILED(30022, "文件上传失败"),
        FILE_TYPE_NOT_ALLOWED(30023, "仅支持 jpg/jpeg/png/gif/webp/bmp 图片"),
        FILE_TOO_LARGE(30024, "图片大小超出限制"),
        REFUND_NOT_FOUND(30025, "退款单不存在"),
        REFUND_ALREADY_APPLIED(30026, "该订单已有处理中的退款申请"),
        REFUND_STATUS_INVALID(30027, "退款单状态不允许该操作"),
        ORDER_NOT_REFUNDABLE(30028, "该订单当前不可申请退款"),
        COUPON_NOT_FOUND(30029, "优惠券不存在"),
        COUPON_RECEIVE_LIMIT(30030, "已达领取上限或超出个人限领"),
        COUPON_NOT_USABLE(30031, "优惠券不可用"),
        COUPON_THRESHOLD_NOT_MET(30032, "未达到优惠券使用门槛"),
        COUPON_STATUS_INVALID(30033, "优惠券状态不允许该操作");

        private final int code;
        private final String msg;

        Biz(int code, String msg) { this.code = code; this.msg = msg; }
        public int getCode() { return code; }
        public String getMsg() { return msg; }
    }

    // ====== 5xxxx 服务端错误 ======
    enum Server implements ErrorCode {
        INTERNAL_ERROR(50000, "服务器内部错误"),
        DB_ERROR(50001, "数据库操作失败"),
        CACHE_ERROR(50002, "缓存操作失败"),
        UPLOAD_FAILED(50003, "文件上传失败");

        private final int code;
        private final String msg;

        Server(int code, String msg) { this.code = code; this.msg = msg; }
        public int getCode() { return code; }
        public String getMsg() { return msg; }
    }
}
