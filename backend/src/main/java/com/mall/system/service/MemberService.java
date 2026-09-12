package com.mall.system.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.entity.Member;
import com.mall.system.mapper.MemberMapper;
import com.mall.system.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会员领域服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String USERNAME_PREFIX = "m";
    private static final int MAX_REGISTER_RETRY = 3;

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param member  会员
     * @param created 是否本次新注册（登录即注册）
     */
    public record MemberResult(Member member, boolean created) {
    }

    public Member getByPhone(String phone) {
        return memberMapper.selectOne(Wrappers.<Member>lambdaQuery().eq(Member::getPhone, phone));
    }

    public Member getRequiredById(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new MallException(ErrorCode.Biz.MEMBER_NOT_FOUND);
        }
        return member;
    }

    public boolean isEnabled(Member member) {
        return member != null && member.getStatus() != null && member.getStatus() == 1;
    }

    /**
     * 登录即注册：手机号已存在直接返回，不存在则创建会员。
     * 并发注册由 user_member 的 uk_phone / uk_username 唯一索引兜底，捕获冲突后回查。
     */
    @Transactional(rollbackFor = Exception.class)
    public MemberResult getOrCreateByPhone(String phone) {
        Member existing = getByPhone(phone);
        if (existing != null) {
            return new MemberResult(existing, false);
        }
        for (int attempt = 1; attempt <= MAX_REGISTER_RETRY; attempt++) {
            String username = attempt == 1
                    ? USERNAME_PREFIX + phone
                    : USERNAME_PREFIX + phone + RandomUtil.randomNumbers(4);
            try {
                Member member = buildMember(phone, username);
                memberMapper.insert(member);
                log.info("短信登录自动注册会员: id={}, phone={}", member.getId(), DesensitizedUtil.mobilePhone(phone));
                return new MemberResult(member, true);
            } catch (DuplicateKeyException e) {
                // 手机号被并发请求抢先注册 -> 直接复用；用户名撞车 -> 换后缀重试
                Member concurrent = getByPhone(phone);
                if (concurrent != null) {
                    return new MemberResult(concurrent, false);
                }
                log.warn("会员注册唯一键冲突，准备第{}次重试: {}", attempt, e.getMessage());
            }
        }
        throw new MallException(ErrorCode.Server.DB_ERROR, "注册失败，请重试");
    }

    public void touchLastLogin(Long memberId) {
        Member update = new Member();
        update.setId(memberId);
        update.setLastLoginTime(LocalDateTime.now());
        memberMapper.updateById(update);
    }

    public MemberVO toVO(Member member) {
        MemberVO vo = new MemberVO();
        vo.setId(member.getId());
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setPhone(DesensitizedUtil.mobilePhone(member.getPhone()));
        vo.setAvatar(member.getAvatar());
        vo.setGender(member.getGender());
        vo.setLevel(member.getLevel());
        vo.setPoints(member.getPoints());
        return vo;
    }

    private Member buildMember(String phone, String username) {
        Member member = new Member();
        member.setUsername(username);
        // 短信注册用户没有密码登录入口：写入随机 BCrypt 密文，既满足非空约束又不可被猜解
        member.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        member.setPhone(phone);
        member.setNickname("用户" + phoneSuffix(phone));
        member.setGender(0);
        member.setStatus(1);
        member.setLevel(0);
        member.setPoints(0);
        member.setBalance(BigDecimal.ZERO);
        member.setLastLoginTime(LocalDateTime.now());
        return member;
    }

    private String phoneSuffix(String phone) {
        return (phone == null || phone.length() < 4) ? "" : phone.substring(phone.length() - 4);
    }
}
