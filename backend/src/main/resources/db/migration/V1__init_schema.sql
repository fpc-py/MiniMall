-- ============================================================================
-- V1__init_schema.sql - 商城 v1.0 完整数据库初始化
-- Flyway V1 迁移
-- ============================================================================

-- ==================== 会员域 ====================

-- 1. 会员表
CREATE TABLE IF NOT EXISTS `user_member` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '会员ID',
    `username`    VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`    VARCHAR(256)  NOT NULL COMMENT 'BCrypt加密密码',
    `phone`       VARCHAR(20)   NULL COMMENT '手机号',
    `email`       VARCHAR(128)  NULL COMMENT '邮箱',
    `avatar`      VARCHAR(512)  NULL COMMENT '头像URL',
    `nickname`    VARCHAR(64)   NULL COMMENT '昵称',
    `real_name`   VARCHAR(64)   NULL COMMENT '真实姓名',
    `gender`      TINYINT       NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `birthday`    DATE          NULL COMMENT '生日',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员表';

-- 2. 用户地址表
CREATE TABLE IF NOT EXISTS `user_address` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `member_id`     BIGINT        NOT NULL COMMENT '会员ID',
    `receiver_name` VARCHAR(64)   NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20)  NOT NULL COMMENT '收货人电话',
    `province`      VARCHAR(64)   NULL COMMENT '省',
    `city`          VARCHAR(64)   NULL COMMENT '市',
    `district`      VARCHAR(64)   NULL COMMENT '区/县',
    `detail_address` VARCHAR(256) NOT NULL COMMENT '详细地址',
    `post_code`     VARCHAR(20)   NULL COMMENT '邮政编码',
    `is_default`    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否默认地址 0否 1是',
    `create_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';

-- ==================== 系统管理域 ====================

-- 3. 系统用户表（管理员）
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`    VARCHAR(256)  NOT NULL COMMENT 'BCrypt加密密码',
    `real_name`   VARCHAR(64)   NULL COMMENT '真实姓名',
    `phone`       VARCHAR(20)   NULL COMMENT '手机号',
    `email`       VARCHAR(128)  NULL COMMENT '邮箱',
    `avatar`      VARCHAR(512)  NULL COMMENT '头像URL',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表（管理员）';

-- 4. 系统角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name`        VARCHAR(64)   NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64)   NOT NULL COMMENT '角色编码',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 5. 系统权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `name`        VARCHAR(64)   NOT NULL COMMENT '权限名称',
    `permission`  VARCHAR(128)  NOT NULL COMMENT '权限标识（如 system:user:list）',
    `type`        TINYINT       NOT NULL DEFAULT 1 COMMENT '类型 1菜单 2按钮 3接口',
    `parent_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '父权限ID',
    `path`        VARCHAR(256)  NULL COMMENT '前端路由路径',
    `icon`        VARCHAR(128)  NULL COMMENT '图标',
    `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission` (`permission`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- 6. 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `role_id`     BIGINT   NOT NULL COMMENT '角色ID',
    `create_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512) NULL COMMENT '备注',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 7. 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id`       BIGINT   NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT   NOT NULL COMMENT '权限ID',
    `create_by`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`        VARCHAR(512) NULL COMMENT '备注',
    `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ==================== 店铺域 ====================

-- 8. 店铺表
CREATE TABLE IF NOT EXISTS `shop_shop` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    `name`           VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    `description`    VARCHAR(512)  NULL COMMENT '店铺描述',
    `logo`           VARCHAR(512)  NULL COMMENT '店铺Logo',
    `contact_name`   VARCHAR(64)   NULL COMMENT '联系人',
    `contact_phone`  VARCHAR(20)   NULL COMMENT '联系电话',
    `contact_email`  VARCHAR(128)  NULL COMMENT '联系邮箱',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1审核通过 2审核驳回',
    `create_by`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`         VARCHAR(512)  NULL COMMENT '备注',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- ==================== 商品域 ====================

-- 9. 商品分类表
CREATE TABLE IF NOT EXISTS `pms_category` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(128)  NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '父分类ID 0表示顶级',
    `level`       TINYINT       NOT NULL DEFAULT 1 COMMENT '层级 1一级 2二级 3三级',
    `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `icon`        VARCHAR(512)  NULL COMMENT '图标URL',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 10. 商品SPU表
CREATE TABLE IF NOT EXISTS `pms_spu` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SPU ID',
    `name`        VARCHAR(256)  NOT NULL COMMENT '商品名称',
    `description` TEXT          NULL COMMENT '商品描述',
    `category_id` BIGINT        NULL COMMENT '分类ID',
    `brand`       VARCHAR(128)  NULL COMMENT '品牌',
    `main_image`  VARCHAR(512)  NULL COMMENT '主图URL',
    `shop_id`     BIGINT        NULL COMMENT '店铺ID',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    `sales`       INT           NOT NULL DEFAULT 0 COMMENT '总销量',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- 11. 商品SKU表
CREATE TABLE IF NOT EXISTS `pms_sku` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    `spu_id`         BIGINT        NOT NULL COMMENT 'SPU ID',
    `name`           VARCHAR(256)  NOT NULL COMMENT 'SKU名称（如 红色/XL）',
    `price`          DECIMAL(12,2) NOT NULL COMMENT '销售价',
    `original_price` DECIMAL(12,2) NULL COMMENT '原价',
    `stock`          INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales`          INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `image`          VARCHAR(512)  NULL COMMENT 'SKU图片',
    `specs_json`     TEXT          NULL COMMENT '规格JSON',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_by`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`         VARCHAR(512)  NULL COMMENT '备注',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_price` (`price`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- 12. SPU图片表
CREATE TABLE IF NOT EXISTS `pms_spu_image` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `spu_id`      BIGINT        NOT NULL COMMENT 'SPU ID',
    `image_url`   VARCHAR(512)  NOT NULL COMMENT '图片URL',
    `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `is_main`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否主图 0否 1是',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SPU图片表';

-- 13. 秒杀活动表
CREATE TABLE IF NOT EXISTS `pms_seckill_activity` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `seckill_price` DECIMAL(12,2) NOT NULL COMMENT '秒杀价',
    `seckill_stock` INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    `limit_count`   INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time`    DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`      DATETIME      NOT NULL COMMENT '结束时间',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束',
    `create_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

-- ==================== 购物车域 ====================

-- 14. 购物车项表
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `member_id`   BIGINT   NOT NULL COMMENT '会员ID',
    `spu_id`      BIGINT   NOT NULL COMMENT 'SPU ID',
    `sku_id`      BIGINT   NOT NULL COMMENT 'SKU ID',
    `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `selected`    TINYINT  NOT NULL DEFAULT 1 COMMENT '是否选中 1选中 0未选',
    `create_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512) NULL COMMENT '备注',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_sku` (`member_id`, `sku_id`),
    KEY `idx_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车项表';

-- ==================== 订单域 ====================

-- 15. 订单表
CREATE TABLE IF NOT EXISTS `oms_order` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`        VARCHAR(64)   NOT NULL COMMENT '订单号',
    `member_id`       BIGINT        NOT NULL COMMENT '会员ID',
    `shop_id`         BIGINT        NULL COMMENT '店铺ID',
    `total_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    `pay_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
    `freight_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_type`        TINYINT       NULL COMMENT '支付方式 1余额 2支付宝 3微信',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待付款 1待发货 2已发货 3已完成 4已关闭 -1已取消',
    `order_type`      TINYINT       NOT NULL DEFAULT 0 COMMENT '订单类型 0普通 1秒杀',
    `source`          TINYINT       NOT NULL DEFAULT 0 COMMENT '来源 0PC 1H5 2小程序',
    `receiver_name`   VARCHAR(64)   NOT NULL COMMENT '收货人姓名',
    `receiver_phone`  VARCHAR(20)   NOT NULL COMMENT '收货人电话',
    `receiver_address` VARCHAR(512) NOT NULL COMMENT '收货地址',
    `delivery_time`   DATETIME      NULL COMMENT '发货时间',
    `receive_time`    DATETIME      NULL COMMENT '收货时间',
    `pay_time`        DATETIME      NULL COMMENT '支付时间',
    `close_time`      DATETIME      NULL COMMENT '关闭时间',
    `member_remark`   VARCHAR(512)  NULL COMMENT '会员备注',
    `create_by`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`          VARCHAR(512)  NULL COMMENT '后台备注',
    `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 16. 订单项表
CREATE TABLE IF NOT EXISTS `oms_order_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`    VARCHAR(64)   NOT NULL COMMENT '订单号',
    `spu_id`      BIGINT        NULL COMMENT 'SPU ID',
    `sku_id`      BIGINT        NULL COMMENT 'SKU ID',
    `sku_name`    VARCHAR(256)  NULL COMMENT 'SKU名称',
    `sku_image`   VARCHAR(512)  NULL COMMENT 'SKU图片',
    `price`       DECIMAL(12,2) NOT NULL COMMENT '购买单价',
    `quantity`    INT           NOT NULL COMMENT '购买数量',
    `subtotal`    DECIMAL(12,2) NOT NULL COMMENT '小计金额',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 17. 订单状态日志表
CREATE TABLE IF NOT EXISTS `oms_order_status_log` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id`    BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`    VARCHAR(64)   NOT NULL COMMENT '订单号',
    `from_status` TINYINT       NULL COMMENT '原状态',
    `to_status`   TINYINT       NOT NULL COMMENT '新状态',
    `operator`    VARCHAR(64)   NULL COMMENT '操作人',
    `content`     VARCHAR(512)  NULL COMMENT '操作内容',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态日志表';

-- ==================== 库存域 ====================

-- 18. 库存表
CREATE TABLE IF NOT EXISTS `wms_stock` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `sku_id`      BIGINT   NOT NULL COMMENT 'SKU ID',
    `available`   INT      NOT NULL DEFAULT 0 COMMENT '可用库存',
    `locked`      INT      NOT NULL DEFAULT 0 COMMENT '锁定库存',
    `total_stock` INT      NOT NULL DEFAULT 0 COMMENT '总库存',
    `create_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512) NULL COMMENT '备注',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 19. 库存锁定记录表
CREATE TABLE IF NOT EXISTS `wms_stock_lock` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '锁定记录ID',
    `sku_id`      BIGINT        NOT NULL COMMENT 'SKU ID',
    `order_id`    BIGINT        NULL COMMENT '订单ID',
    `order_no`    VARCHAR(64)   NULL COMMENT '订单号',
    `lock_count`  INT           NOT NULL COMMENT '锁定数量',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0锁定中 1已释放 2已出库',
    `create_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`      VARCHAR(512)  NULL COMMENT '备注',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存锁定记录表';

-- 20. 出库任务表
CREATE TABLE IF NOT EXISTS `wms_outbound_task` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '出库任务ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单号',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `quantity`      INT           NOT NULL COMMENT '出库数量',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待出库 1已出库',
    `outbound_time` DATETIME      NULL COMMENT '出库时间',
    `create_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库任务表';

-- ==================== 支付域 ====================

-- 21. 支付记录表
CREATE TABLE IF NOT EXISTS `pay_payment` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `order_id`         BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`         VARCHAR(64)   NOT NULL COMMENT '订单号',
    `member_id`        BIGINT        NOT NULL COMMENT '会员ID',
    `pay_amount`       DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    `pay_type`         TINYINT       NOT NULL COMMENT '支付方式 1余额 2支付宝 3微信',
    `transaction_no`   VARCHAR(128)  NULL COMMENT '第三方交易号',
    `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0处理中 1成功 2失败',
    `callback_content` TEXT          NULL COMMENT '回调原始内容',
    `callback_time`    DATETIME      NULL COMMENT '回调时间',
    `create_by`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`        VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新人',
    `remark`           VARCHAR(512)  NULL COMMENT '备注',
    `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';
