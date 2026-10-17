-- ============================================
-- V5__refund.sql - 退款/售后单表
-- ============================================

CREATE TABLE IF NOT EXISTS `oms_refund` (
    `id`            BIGINT        NOT NULL COMMENT '退款单ID',
    `refund_no`     VARCHAR(64)   NOT NULL COMMENT '退款单号',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `member_id`     BIGINT        NOT NULL COMMENT '申请会员ID',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '退款金额',
    `reason`        VARCHAR(256)  NOT NULL COMMENT '退款原因',
    `description`   VARCHAR(512)  NULL COMMENT '补充说明',
    `images`        VARCHAR(1024) NULL COMMENT '凭证图片URL，逗号分隔',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1退款中 2已退款 3已拒绝 4已撤销',
    `audit_remark`  VARCHAR(512)  NULL COMMENT '审核备注',
    `audit_time`    DATETIME      NULL COMMENT '审核时间',
    `auditor_id`    BIGINT        NULL COMMENT '审核人ID',
    `refund_time`   DATETIME      NULL COMMENT '退款到账时间',
    `transaction_no` VARCHAR(128) NULL COMMENT '退款渠道流水号',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_no` (`refund_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款/售后单表';
