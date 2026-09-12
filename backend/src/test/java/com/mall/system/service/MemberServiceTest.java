package com.mall.system.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.system.entity.Member;
import com.mall.system.mapper.MemberMapper;
import com.mall.system.vo.MemberVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员「登录即注册」测试。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final String PHONE = "13800138000";

    @Mock
    private MemberMapper memberMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberMapper, passwordEncoder);
    }

    private Member existingMember(Long id) {
        Member member = new Member();
        member.setId(id);
        member.setUsername("m" + PHONE);
        member.setPhone(PHONE);
        member.setStatus(1);
        return member;
    }

    @Test
    @DisplayName("手机号已存在：直接复用，不再插入")
    void existingPhoneIsReused() {
        Member existing = existingMember(9527L);
        when(memberMapper.selectOne(any())).thenReturn(existing);

        MemberService.MemberResult result = memberService.getOrCreateByPhone(PHONE);

        assertThat(result.created()).isFalse();
        assertThat(result.member()).isSameAs(existing);
        verify(memberMapper, never()).insert(any(Member.class));
    }

    @Test
    @DisplayName("手机号不存在：自动注册，默认启用、余额 0、密码为随机 BCrypt 密文")
    void newPhoneIsRegistered() {
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.insert(any(Member.class))).thenAnswer(invocation -> {
            Member inserted = invocation.getArgument(0);
            inserted.setId(9527L);
            return 1;
        });

        MemberService.MemberResult result = memberService.getOrCreateByPhone(PHONE);

        assertThat(result.created()).isTrue();
        Member member = result.member();
        assertThat(member.getId()).isEqualTo(9527L);
        assertThat(member.getPhone()).isEqualTo(PHONE);
        assertThat(member.getUsername()).isEqualTo("m" + PHONE);
        assertThat(member.getNickname()).isEqualTo("用户8000");
        assertThat(member.getStatus()).isEqualTo(1);
        assertThat(member.getLevel()).isZero();
        assertThat(member.getPoints()).isZero();
        assertThat(member.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(member.getLastLoginTime()).isNotNull();
        // 密码必须是 BCrypt 密文且不可用于登录（随机值）
        assertThat(member.getPassword()).startsWith("$2").hasSize(60);
        assertThat(member.getPassword()).doesNotContain(PHONE);
    }

    @Test
    @DisplayName("并发注册：唯一键冲突后回查已存在会员，不重复建号")
    void concurrentRegisterReusesExistingMember() {
        Member concurrent = existingMember(8888L);
        when(memberMapper.selectOne(any())).thenReturn(null, concurrent);
        when(memberMapper.insert(any(Member.class))).thenThrow(new DuplicateKeyException("uk_phone"));

        MemberService.MemberResult result = memberService.getOrCreateByPhone(PHONE);

        assertThat(result.created()).isFalse();
        assertThat(result.member().getId()).isEqualTo(8888L);
        verify(memberMapper, times(1)).insert(any(Member.class));
    }

    @Test
    @DisplayName("用户名撞车：换随机后缀重试注册成功")
    void usernameConflictRetriesWithSuffix() {
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.insert(any(Member.class)))
                .thenThrow(new DuplicateKeyException("uk_username"))
                .thenAnswer(invocation -> {
                    Member inserted = invocation.getArgument(0);
                    inserted.setId(7777L);
                    return 1;
                });

        MemberService.MemberResult result = memberService.getOrCreateByPhone(PHONE);

        assertThat(result.created()).isTrue();
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getUsername()).isEqualTo("m" + PHONE);
        assertThat(captor.getAllValues().get(1).getUsername())
                .startsWith("m" + PHONE)
                .hasSizeGreaterThan(("m" + PHONE).length());
    }

    @Test
    @DisplayName("重试仍失败：抛 50001，不会产生半截数据")
    void registerFailsAfterRetries() {
        when(memberMapper.selectOne(any())).thenReturn(null);
        when(memberMapper.insert(any(Member.class))).thenThrow(new DuplicateKeyException("conflict"));

        assertThatThrownBy(() -> memberService.getOrCreateByPhone(PHONE))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Server.DB_ERROR.getCode());

        verify(memberMapper, times(3)).insert(any(Member.class));
    }

    @Test
    @DisplayName("会员信息脱敏：手机号中间四位打码")
    void toVoMasksPhone() {
        Member member = existingMember(9527L);
        member.setNickname("用户8000");
        member.setLevel(1);
        member.setPoints(20);

        MemberVO vo = memberService.toVO(member);

        assertThat(vo.getPhone()).isEqualTo("138****8000");
        assertThat(vo.getPhone()).doesNotContain(PHONE);
        assertThat(vo.getId()).isEqualTo(9527L);
        assertThat(vo.getNickname()).isEqualTo("用户8000");
        assertThat(vo.getLevel()).isEqualTo(1);
        assertThat(vo.getPoints()).isEqualTo(20);
    }

    @Test
    @DisplayName("按ID查会员不存在：抛 30012")
    void getRequiredByIdThrowsWhenMissing() {
        when(memberMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> memberService.getRequiredById(1L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.MEMBER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("更新登录时间只写 last_login_time，不影响其它字段")
    void touchLastLoginOnlyUpdatesLoginTime() {
        memberService.touchLastLogin(9527L);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(captor.capture());
        Member update = captor.getValue();
        assertThat(update.getId()).isEqualTo(9527L);
        assertThat(update.getLastLoginTime()).isNotNull();
        assertThat(update.getUsername()).isNull();
        assertThat(update.getPhone()).isNull();
        assertThat(update.getPassword()).isNull();
        assertThat(update.getBalance()).isNull();
    }

    @Test
    @DisplayName("禁用会员判定")
    void isEnabledChecksStatus() {
        Member disabled = existingMember(1L);
        disabled.setStatus(0);

        assertThat(memberService.isEnabled(existingMember(2L))).isTrue();
        assertThat(memberService.isEnabled(disabled)).isFalse();
        assertThat(memberService.isEnabled(null)).isFalse();
    }
}
