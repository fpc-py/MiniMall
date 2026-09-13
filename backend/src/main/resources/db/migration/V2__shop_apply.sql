-- ============================================
-- V2__shop_apply.sql - 商家入驻申请表
-- ============================================

-- 22. 商家入驻申请表
CREATE TABLE IF NOT EXISTS `shop_apply` (
    `id`                BIGINT        NOT NULL COMMENT '申请ID',
    `member_id`         BIGINT        NOT NULL COMMENT '申请人会员ID',
    `shop_name`         VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    `shop_logo`         VARCHAR(512)  NULL COMMENT '店铺Logo',
    `contact_name`      VARCHAR(64)   NOT NULL COMMENT '联系人姓名',
    `contact_phone`     VARCHAR(20)   NOT NULL COMMENT '联系电话',
    `province`          VARCHAR(32)   NULL COMMENT '省',
    `city`              VARCHAR(32)   NULL COMMENT '市',
    `district`          VARCHAR(32)   NULL COMMENT '区/县',
    `address`           VARCHAR(256)  NULL COMMENT '详细地址',
    `business_license`  VARCHAR(512)  NULL COMMENT '营业执照图片URL',
    `id_card_front`     VARCHAR(512)  NULL COMMENT '身份证正面',
    `id_card_back`      VARCHAR(512)  NULL COMMENT '身份证反面',
    `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1通过 2拒绝',
    `audit_remark`      VARCHAR(512)  NULL COMMENT '审核备注',
    `audit_time`        DATETIME      NULL COMMENT '审核时间',
    `auditor_id`        BIGINT        NULL COMMENT '审核人ID',
    `shop_id`           BIGINT        NULL COMMENT '审核通过后创建的店铺ID',
    `create_by`         VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`         VARCHAR(64)   NULL COMMENT '更新人',
    `remark`            VARCHAR(512)  NULL COMMENT '备注',
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻申请表';
