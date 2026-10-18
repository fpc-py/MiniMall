package com.mall.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.common.PageQuery;
import com.mall.common.PageResult;
import com.mall.system.entity.Member;
import com.mall.system.mapper.MemberMapper;
import com.mall.system.vo.AdminMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台端会员管理：分页查询与启用/禁用。
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    /** 会员状态：1启用 0禁用 */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    private final MemberMapper memberMapper;

    public PageResult<AdminMemberVO> page(String keyword, PageQuery pageQuery) {
        var wrapper = Wrappers.<Member>lambdaQuery()
                .orderByDesc(Member::getCreateTime)
                .orderByDesc(Member::getId);
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(Member::getPhone, k).or().like(Member::getNickname, k));
        }
        Page<Member> page = new Page<>(pageQuery.safePageNum(), pageQuery.safePageSize());
        return PageResult.of(memberMapper.selectPage(page, wrapper), this::toVO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long memberId, Integer status) {
        if (status == null || (status != STATUS_ENABLED && status != STATUS_DISABLED)) {
            throw new MallException(ErrorCode.Param.INVALID_PARAM, "状态只能为 0禁用 或 1启用");
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new MallException(ErrorCode.Biz.MEMBER_NOT_FOUND);
        }
        // 只更新状态字段，避免覆盖其它列
        Member update = new Member();
        update.setId(memberId);
        update.setStatus(status);
        memberMapper.updateById(update);
    }

    public AdminMemberVO toVO(Member m) {
        AdminMemberVO vo = new AdminMemberVO();
        vo.setId(m.getId());
        vo.setUsername(m.getUsername());
        vo.setNickname(m.getNickname());
        vo.setPhone(m.getPhone());
        vo.setAvatar(m.getAvatar());
        vo.setGender(m.getGender());
        vo.setStatus(m.getStatus());
        vo.setLevel(m.getLevel());
        vo.setPoints(m.getPoints());
        vo.setBalance(m.getBalance());
        vo.setLastLoginTime(m.getLastLoginTime());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
