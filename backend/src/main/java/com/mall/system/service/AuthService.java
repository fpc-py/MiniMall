package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.security.JwtUtils;
import com.mall.common.security.UserType;
import com.mall.system.dto.SmsLoginRequest;
import com.mall.system.entity.Member;
import com.mall.system.vo.LoginVO;
import com.mall.system.vo.SmsSendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证服务：短信验证码登录（登录即注册）+ JWT 签发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SmsCodeService smsCodeService;
    private final MemberService memberService;
    private final JwtUtils jwtUtils;

    public SmsSendVO sendSmsCode(String phone) {
        return smsCodeService.send(normalizePhone(phone));
    }

    /**
     * 验证码登录：校验通过 -> 查/建会员 -> 更新登录时间 -> 签发 JWT。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginBySms(SmsLoginRequest request) {
        if (request == null) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "登录参数不能为空");
        }
        String phone = normalizePhone(request.getPhone());
        smsCodeService.verify(phone, request.getCode());

        MemberService.MemberResult result = memberService.getOrCreateByPhone(phone);
        Member member = result.member();
        if (!memberService.isEnabled(member)) {
            throw new MallException(ErrorCode.Auth.ACCOUNT_DISABLED);
        }
        memberService.touchLastLogin(member.getId());

        String token = jwtUtils.generateToken(member.getId(), member.getUsername(), UserType.MEMBER);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtUtils.getExpiration() / 1000);
        vo.setNewMember(result.created());
        vo.setUserInfo(memberService.toVO(member));
        log.info("短信验证码登录成功: memberId={}, 新注册={}", member.getId(), result.created());
        return vo;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "手机号不能为空");
        }
        String trimmed = phone.trim();
        if (!trimmed.matches("^1[3-9]\\d{9}$")) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "手机号格式不正确");
        }
        return trimmed;
    }
}
