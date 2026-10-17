-- ============================================
-- V6__coupon.sql - 优惠券（领券中心 + 下单抵扣）
-- ============================================

-- 1. 优惠券模板
CREATE TABLE IF NOT EXISTS `pms_coupon` (
    `id`               BIGINT        NOT NULL COMMENT '优惠券ID',
    `name`             VARCHAR(128)  NOT NULL COMMENT '优惠券名称',
    `type`             TINYINT       NOT NULL DEFAULT 1 COMMENT '类型 1满减 2折扣',
    `shop_id`          BIGINT        NULL COMMENT '适用店铺ID，NULL=平台通用',
    `threshold_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
    `discount_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '满减金额（type=1）',
    `discount_rate`    DECIMAL(4,2)  NULL COMMENT '折扣率 0.01~0.99（type=2）',
    `total_count`      INT           NOT NULL DEFAULT 0 COMMENT '发行总量',
    `received_count`   INT           NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit`   INT           NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `start_time`       DATETIME      NOT NULL COMMENT '生效时间',
    `end_time`         DATETIME      NOT NULL COMMENT '失效时间',
    `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `description`      VARCHAR(512)  NULL COMMENT '使用说明',
    `create_by`        VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`        VARCHAR(64)   NULL COMMENT '更新人',
    `remark`           VARCHAR(512)  NULL COMMENT '备注',
    `deleted`          TINYINT       NOT NULL DEFAULT 0,
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

-- 2. 会员优惠券（领取记录）
CREATE TABLE IF NOT EXISTS `pms_coupon_member` (
    `id`           BIGINT   NOT NULL COMMENT '领取记录ID',
    `coupon_id`    BIGINT   NOT NULL COMMENT '优惠券ID',
    `member_id`    BIGINT   NOT NULL COMMENT '会员ID',
    `status`       TINYINT  NOT NULL DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期',
    `order_id`     BIGINT   NULL COMMENT '使用的订单ID',
    `receive_time` DATETIME NOT NULL COMMENT '领取时间',
    `use_time`     DATETIME NULL COMMENT '使用时间',
    `expire_time`  DATETIME NOT NULL COMMENT '过期时间',
    `create_by`    VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`    VARCHAR(64)   NULL COMMENT '更新人',
    `remark`       VARCHAR(512)  NULL COMMENT '备注',
    `deleted`      TINYINT       NOT NULL DEFAULT 0,
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_coupon_id` (`coupon_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员优惠券表';

-- 3. 订单记录使用的优惠券（用于取消/退款时退回）
ALTER TABLE `oms_order`
    ADD COLUMN `coupon_member_id` BIGINT NULL COMMENT '使用的会员优惠券记录ID' AFTER `discount_amount`;
