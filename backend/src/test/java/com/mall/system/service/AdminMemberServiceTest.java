package com.mall.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.system.entity.Member;
import com.mall.system.mapper.MemberMapper;
import com.mall.system.vo.AdminMemberVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 平台端会员管理测试：列表展示完整手机号、启用/禁用只改状态字段。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminMemberServiceTest {

    @Mock
    private MemberMapper memberMapper;

    private AdminMemberService adminMemberService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Member.class);
    }

    @BeforeEach
    void setUp() {
        adminMemberService = new AdminMemberService(memberMapper);
    }

    private Member member() {
        Member m = new Member();
        m.setId(1L);
        m.setUsername("m13800000000");
        m.setNickname("测试会员");
        m.setPhone("13800000000");
        m.setStatus(1);
        m.setLevel(1);
        m.setPoints(20);
        m.setBalance(new BigDecimal("10000.00"));
        return m;
    }

    @Test
    @DisplayName("会员列表：运营可见完整手机号，且金额/状态一并返回")
    void pageReturnsFullPhone() {
        when(memberMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Member> page = invocation.getArgument(0);
            page.setRecords(List.of(member()));
            page.setTotal(1);
            return page;
        });

        PageResult<AdminMemberVO> result = adminMemberService.page("138", new PageQuery());

        assertThat(result.getTotal()).isEqualTo(1);
        AdminMemberVO vo = result.getList().get(0);
        assertThat(vo.getPhone()).isEqualTo("13800000000");
        assertThat(vo.getPhone()).doesNotContain("*");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(vo.getPoints()).isEqualTo(20);
    }

    @Test
    @DisplayName("禁用会员：只更新 status 字段，不覆盖其它列")
    void updateStatusOnlyChangesStatus() {
        when(memberMapper.selectById(1L)).thenReturn(member());

        adminMemberService.updateStatus(1L, AdminMemberService.STATUS_DISABLED);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(captor.capture());
        Member update = captor.getValue();
        assertThat(update.getId()).isEqualTo(1L);
        assertThat(update.getStatus()).isZero();
        assertThat(update.getPhone()).isNull();
        assertThat(update.getBalance()).isNull();
        assertThat(update.getPassword()).isNull();
    }

    @Test
    @DisplayName("非法状态值抛 10000")
    void updateStatusRejectsIllegalValue() {
        assertThatThrownBy(() -> adminMemberService.updateStatus(1L, 9))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.INVALID_PARAM.getCode());
        verify(memberMapper, never()).updateById(any(Member.class));
    }

    @Test
    @DisplayName("会员不存在抛 30012")
    void updateStatusThrowsWhenMemberMissing() {
        when(memberMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> adminMemberService.updateStatus(1L, AdminMemberService.STATUS_ENABLED))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.MEMBER_NOT_FOUND.getCode());
    }
}
