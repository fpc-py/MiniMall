-- ============================================
-- V8__ai_customer_service.sql - 智能客服（Spring AI Agent）
-- 四张表：ai_faq / ai_conversation / ai_message / ai_handoff
-- 种子：10 条常见问题，固定主键 + ON DUPLICATE KEY UPDATE 保证幂等
-- ============================================

-- 1. 客服知识库（FAQ）
CREATE TABLE IF NOT EXISTS `ai_faq` (
    `id`          BIGINT        NOT NULL COMMENT 'FAQ ID',
    `question`    VARCHAR(255)  NOT NULL COMMENT '标准问题',
    `answer`      VARCHAR(2048) NOT NULL COMMENT '标准答案',
    `keywords`    VARCHAR(512)  NULL COMMENT '关键词，逗号分隔，用于关键词降级检索',
    `category`    VARCHAR(64)   NULL COMMENT '分类：物流/售后/支付/优惠/其他',
    `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `create_by`   VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`   VARCHAR(64)   NULL COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0,
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能客服知识库表';

-- 2. 客服会话
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id`                BIGINT       NOT NULL COMMENT '会话ID',
    `member_id`         BIGINT       NOT NULL COMMENT '会员ID',
    `title`             VARCHAR(128) NULL COMMENT '会话标题，取首条提问摘要',
    `status`            TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0进行中 1已转人工 2已结束',
    `last_message_time` DATETIME     NULL COMMENT '最后一条消息时间',
    `message_count`     INT          NOT NULL DEFAULT 0 COMMENT '消息条数',
    `create_by`         VARCHAR(64)  NULL COMMENT '创建人',
    `update_by`         VARCHAR(64)  NULL COMMENT '更新人',
    `remark`            VARCHAR(512) NULL COMMENT '备注',
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能客服会话表';

-- 3. 客服消息
CREATE TABLE IF NOT EXISTS `ai_message` (
    `id`              BIGINT       NOT NULL COMMENT '消息ID',
    `conversation_id` BIGINT       NOT NULL COMMENT '会话ID',
    `member_id`       BIGINT       NOT NULL COMMENT '会员ID',
    `role`            TINYINT      NOT NULL COMMENT '角色 1用户 2客服',
    `content`         VARCHAR(4096) NOT NULL COMMENT '消息内容',
    `create_by`       VARCHAR(64)  NULL COMMENT '创建人',
    `update_by`       VARCHAR(64)  NULL COMMENT '更新人',
    `remark`          VARCHAR(512) NULL COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能客服消息表';

-- 4. 转人工工单
CREATE TABLE IF NOT EXISTS `ai_handoff` (
    `id`              BIGINT       NOT NULL COMMENT '工单ID',
    `conversation_id` BIGINT       NOT NULL COMMENT '会话ID',
    `member_id`       BIGINT       NOT NULL COMMENT '会员ID',
    `reason`          VARCHAR(512) NULL COMMENT '转人工原因',
    `contact`         VARCHAR(128) NULL COMMENT '联系方式',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待处理 1已处理',
    `create_by`       VARCHAR(64)  NULL COMMENT '创建人',
    `update_by`       VARCHAR(64)  NULL COMMENT '更新人',
    `remark`          VARCHAR(512) NULL COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转人工工单表';

-- 5. FAQ 种子（10 条，固定主键幂等）
INSERT INTO `ai_faq` (`id`, `question`, `answer`, `keywords`, `category`, `sort`, `status`) VALUES
(90001, '你们支持七天无理由退货吗？', '支持。签收后 7 天内，商品不影响二次销售（保持吊牌、包装、配件完整）即可申请无理由退货，定制类、鲜活易腐类及已拆封的个护类商品除外。', '七天无理由,退货,7天,退换货', '售后', 1, 1),
(90002, '下单后多久可以发货？', '现货商品一般在付款后 24 小时内发出，预售商品以商品详情页标注的发货时间为准。发货后会发送物流通知，可在「我的订单」中查看物流信息。', '发货,什么时候发货,几天到,物流时效', '物流', 2, 1),
(90003, '运费怎么计算？包邮吗？', '单笔订单实付满 99 元包邮（新疆、西藏、港澳台等偏远地区除外）。未满 99 元收取 10 元运费。偏远地区运费以下单页面展示为准。', '运费,包邮,邮费,快递费', '物流', 3, 1),
(90004, '可以开发票吗？', '支持开具电子发票，可在下单时选择「需要发票」并填写抬头信息，也可在订单完成后 30 天内于「我的订单」申请补开。电子发票会在开票后发送至您的邮箱。', '发票,开票,报销,电子发票', '支付', 4, 1),
(90005, '退款多久能到账？', '退款申请审核通过后，系统会原路退回。微信/支付宝渠道一般 1-3 个工作日到账，银行卡渠道 3-7 个工作日到账。若超时未到账，请联系客服核实。', '退款,退款到账,退款多久,退钱', '售后', 5, 1),
(90006, '优惠券怎么使用？在哪里领取？', '在「领券中心」领取优惠券后，下单结算时会自动匹配可用优惠券，也可手动选择。注意优惠券有使用门槛和有效期，过期作废；退款时已使用的优惠券将按规则退回。', '优惠券,领券,怎么用券,满减', '优惠', 6, 1),
(90007, '订单可以修改收货地址吗？', '未发货的订单可在「我的订单」→ 订单详情中自行修改收货地址；已发货的订单无法修改，可联系客服协助与快递沟通。', '改地址,修改收货地址,收货地址', '物流', 7, 1),
(90008, '商品有质量问题怎么办？', '请在签收后 7 天内提供商品问题照片或视频，在「我的订单」发起售后申请，选择「质量问题」并上传凭证。审核通过后支持退货退款或换货，由此产生的运费由平台承担。', '质量问题,坏了,破损,售后,换货', '售后', 8, 1),
(90009, '怎么修改绑定的手机号？', '为保障账号安全，手机号修改需通过身份验证。请在「个人中心」→「账号安全」中操作，或联系在线客服转人工协助处理。', '改手机号,换手机号,账号安全,绑定手机', '其他', 9, 1),
(90010, '怎么联系人工客服？', '您可以直接对我说「转人工」，或在「个人中心」→「在线客服」中留言，我们的人工客服会在工作时间内尽快与您联系。人工客服工作时间：每日 9:00-21:00。', '人工,人工客服,联系客服,客服电话,转人工', '其他', 10, 1)
ON DUPLICATE KEY UPDATE `question` = VALUES(`question`), `answer` = VALUES(`answer`), `keywords` = VALUES(`keywords`), `category` = VALUES(`category`), `sort` = VALUES(`sort`), `status` = VALUES(`status`);
