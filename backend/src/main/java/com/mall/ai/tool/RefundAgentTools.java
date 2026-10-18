package com.mall.ai.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.order.entity.Refund;
import com.mall.order.mapper.RefundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 退款/售后查询工具。强制以 JWT 中的 memberId 做归属过滤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundAgentTools {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<Integer, String> REFUND_STATUS_TEXT = Map.of(
            0, "待审核", 1, "退款中", 2, "已退款", 3, "已拒绝", 4, "已撤销");

    private static final int RECENT_LIMIT = 5;

    private final RefundMapper refundMapper;

    @Tool(name = "query_refund",
            description = "查询当前登录用户的退款/售后单进度与审核结果。用户给出订单号时查该订单的退款单，未给出时列出最近的退款记录。",
            resultConverter = AiTextResultConverter.class)
    public String queryRefund(
            @ToolParam(required = false, description = "订单号，22 位纯数字；用户明确给出时传入") String orderNo,
            ToolContext toolContext) {
        Long memberId = AiToolSupport.userId(toolContext);
        if (memberId == null) {
            return "请先登录后再查询退款进度。";
        }
        AiProgressNotifier notifier = AiToolSupport.notifier(toolContext);
        notifier.tool("query_refund", "running");
        try {
            if (orderNo != null && !orderNo.isBlank()) {
                Refund refund = refundMapper.selectOne(Wrappers.<Refund>lambdaQuery()
                        .eq(Refund::getOrderNo, orderNo.trim())
                        .eq(Refund::getMemberId, memberId)
                        .orderByDesc(Refund::getCreateTime)
                        .last("LIMIT 1"));
                if (refund == null) {
                    return "未查询到该订单的退款申请。若您还没有提交退款，可在「我的订单」中发起售后申请。";
                }
                return describe(refund);
            }
            List<Refund> refunds = refundMapper.selectList(Wrappers.<Refund>lambdaQuery()
                    .eq(Refund::getMemberId, memberId)
                    .orderByDesc(Refund::getCreateTime)
                    .last("LIMIT " + RECENT_LIMIT));
            if (refunds.isEmpty()) {
                return "您目前没有退款/售后记录。若需要退货退款，可在「我的订单」中发起售后申请。";
            }
            StringBuilder builder = new StringBuilder("为您查询到最近的退款记录：\n");
            int index = 1;
            for (Refund refund : refunds) {
                builder.append(index++).append(". 退款单号 ").append(refund.getRefundNo())
                        .append(" ｜ 订单号 ").append(refund.getOrderNo())
                        .append(" ｜ ").append(statusText(refund.getStatus()))
                        .append(" ｜ 退款金额 ").append(AiToolSupport.money(refund.getRefundAmount())).append(" 元");
                if (refund.getCreateTime() != null) {
                    builder.append(" ｜ 申请时间 ").append(refund.getCreateTime().format(TIME_FORMATTER));
                }
                builder.append('\n');
            }
            return builder.toString().trim();
        } catch (Exception e) {
            log.warn("query_refund 执行异常: {}", e.getMessage());
            return "抱歉，退款查询出了点小问题，请稍后再试。";
        } finally {
            notifier.tool("query_refund", "done");
        }
    }

    private String describe(Refund refund) {
        StringBuilder builder = new StringBuilder();
        builder.append("退款单号：").append(refund.getRefundNo()).append('\n');
        builder.append("订单号：").append(refund.getOrderNo()).append('\n');
        builder.append("退款状态：").append(statusText(refund.getStatus())).append('\n');
        builder.append("退款金额：").append(AiToolSupport.money(refund.getRefundAmount())).append(" 元\n");
        if (refund.getReason() != null) {
            builder.append("退款原因：").append(refund.getReason()).append('\n');
        }
        if (refund.getAuditRemark() != null && !refund.getAuditRemark().isBlank()) {
            builder.append("审核备注：").append(refund.getAuditRemark()).append('\n');
        }
        if (refund.getRefundTime() != null) {
            builder.append("到账时间：").append(refund.getRefundTime().format(TIME_FORMATTER)).append('\n');
        }
        return builder.toString().trim();
    }

    private String statusText(Integer status) {
        return status == null ? "未知" : REFUND_STATUS_TEXT.getOrDefault(status, "未知");
    }
}
