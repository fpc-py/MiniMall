package com.mall.common;

import lombok.Getter;

@Getter
public class MallException extends RuntimeException {
    private final int code;
    private final String msg;

    public MallException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public MallException(ErrorCode errorCode, String extraMsg) {
        super(errorCode.getMsg() + ": " + extraMsg);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg() + ": " + extraMsg;
    }

    public MallException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public static MallException paramError(String msg) {
        return new MallException(ErrorCode.Param.INVALID_PARAM, msg);
    }

    public static MallException authError(String msg) {
        return new MallException(ErrorCode.Auth.UNAUTHORIZED, msg);
    }

    public static MallException bizError(ErrorCode.Biz biz, String msg) {
        return new MallException(biz, msg);
    }
}
