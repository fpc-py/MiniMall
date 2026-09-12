-- V1__init_schema.sql - 初始化商城数据库表结构
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL COMMENT '用户ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`      VARCHAR(256) NOT NULL COMMENT 'BCrypt加密密码',
    `phone`         VARCHAR(20)  NULL COMMENT '手机号',
    `email`         VARCHAR(128) NULL COMMENT '邮箱',
    `avatar`        VARCHAR(512) NULL COMMENT '头像URL',
    `nickname`      VARCHAR(64)  NULL COMMENT '昵称',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id`            BIGINT      NOT NULL COMMENT '角色ID',
    `name`          VARCHAR(64) NOT NULL COMMENT '角色名称',
    `code`          VARCHAR(64) NOT NULL COMMENT '角色编码',
    `remark`        VARCHAR(256) NULL COMMENT '备注',
    `deleted`       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
    `id`            BIGINT   NOT NULL COMMENT 'ID',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`       BIGINT   NOT NULL COMMENT '角色ID',
    `deleted`       TINYINT  NOT NULL DEFAULT 0,
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 商品分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id`            BIGINT       NOT NULL COMMENT '分类ID',
    `name`          VARCHAR(128) NOT NULL COMMENT '分类名称',
    `parent_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID 0表示顶级',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `icon`          VARCHAR(512) NULL COMMENT '图标URL',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id`            BIGINT        NOT NULL COMMENT '商品ID',
    `name`          VARCHAR(256)  NOT NULL COMMENT '商品名称',
    `description`   TEXT          NULL COMMENT '商品描述',
    `category_id`   BIGINT        NULL COMMENT '分类ID',
    `price`         DECIMAL(12,2) NOT NULL COMMENT '价格',
    `original_price` DECIMAL(12,2) NULL COMMENT '原价',
    `stock`         INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales`         INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `main_image`    VARCHAR(512)  NULL COMMENT '主图URL',
    `images`        TEXT          NULL COMMENT '轮播图JSON数组',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 购物车表
CREATE TABLE IF NOT EXISTS `cart` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `quantity`      INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `selected`      TINYINT       NOT NULL DEFAULT 1 COMMENT '是否选中 1选中 0未选',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id`              BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`        VARCHAR(64)   NOT NULL COMMENT '订单号',
    `user_id`         BIGINT        NOT NULL COMMENT '用户ID',
    `total_amount`    DECIMAL(12,2) NOT NULL COMMENT '总金额',
    `pay_amount`      DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    `freight`         DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待付款 1待发货 2已发货 3已完成 -1已取消',
    `payment_method`  TINYINT       NULL COMMENT '支付方式',
    `payment_time`    DATETIME      NULL COMMENT '支付时间',
    `delivery_time`   DATETIME      NULL COMMENT '发货时间',
    `receive_time`    DATETIME      NULL COMMENT '收货时间',
    `receiver_name`   VARCHAR(64)   NOT NULL COMMENT '收货人',
    `receiver_phone`  VARCHAR(20)   NOT NULL COMMENT '收货人电话',
    `receiver_address` VARCHAR(512) NOT NULL COMMENT '收货地址',
    `remark`          VARCHAR(512)  NULL COMMENT '备注',
    `deleted`         TINYINT       NOT NULL DEFAULT 0,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 订单项表
CREATE TABLE IF NOT EXISTS `order_item` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(256)  NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(512)  NULL COMMENT '商品图片',
    `price`         DECIMAL(12,2) NOT NULL COMMENT '购买单价',
    `quantity`      INT           NOT NULL COMMENT '购买数量',
    `subtotal`      DECIMAL(12,2) NOT NULL COMMENT '小计金额',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 支付记录表
CREATE TABLE IF NOT EXISTS `payment_record` (
    `id`                BIGINT        NOT NULL COMMENT 'ID',
    `order_id`          BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`          VARCHAR(64)   NOT NULL COMMENT '订单号',
    `user_id`           BIGINT        NOT NULL COMMENT '用户ID',
    `amount`            DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    `payment_method`    TINYINT       NOT NULL COMMENT '支付方式 1余额 2沙箱支付宝 3沙箱微信',
    `transaction_no`    VARCHAR(128)  NULL COMMENT '第三方交易号',
    `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0处理中 1成功 2失败',
    `callback_time`     DATETIME      NULL COMMENT '回调时间',
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS `stock_record` (
    `id`            BIGINT       NOT NULL COMMENT 'ID',
    `product_id`    BIGINT       NOT NULL COMMENT '商品ID',
    `order_id`      BIGINT       NULL COMMENT '订单ID',
    `change_type`   TINYINT      NOT NULL COMMENT '变化类型 1下单占用 2支付出库 3取消释放 4管理员调整',
    `change_amount` INT          NOT NULL COMMENT '变化数量（正数增加 负数减少）',
    `before_stock`  INT          NOT NULL COMMENT '变化前库存',
    `after_stock`   INT          NOT NULL COMMENT '变化后库存',
    `remark`        VARCHAR(256) NULL COMMENT '备注',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存流水表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`            BIGINT       NOT NULL COMMENT 'ID',
    `config_key`    VARCHAR(128) NOT NULL COMMENT '配置键',
    `config_value`  TEXT         NOT NULL COMMENT '配置值',
    `remark`        VARCHAR(256) NULL COMMENT '备注',
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
