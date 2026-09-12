-- ============================================
-- V1__init.sql - 完整商城数据库表结构
-- ============================================

-- ==================== 用户体系 ====================

-- 1. 会员用户表
CREATE TABLE IF NOT EXISTS `user_member` (
    `id`            BIGINT        NOT NULL COMMENT '会员ID',
    `username`      VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`      VARCHAR(256)  NOT NULL COMMENT 'BCrypt加密密码',
    `nickname`      VARCHAR(64)   NULL COMMENT '昵称',
    `phone`         VARCHAR(20)   NULL COMMENT '手机号',
    `email`         VARCHAR(128)  NULL COMMENT '邮箱',
    `avatar`        VARCHAR(512)  NULL COMMENT '头像URL',
    `gender`        TINYINT       NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
    `birthday`      DATE          NULL COMMENT '生日',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `level`         TINYINT       NOT NULL DEFAULT 0 COMMENT '会员等级 0普通 1银 2金 3钻石',
    `points`        INT           NOT NULL DEFAULT 0 COMMENT '积分',
    `balance`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
    `last_login_time` DATETIME    NULL COMMENT '最后登录时间',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员用户表';

-- 2. 用户地址表
CREATE TABLE IF NOT EXISTS `user_address` (
    `id`            BIGINT        NOT NULL COMMENT '地址ID',
    `member_id`     BIGINT        NOT NULL COMMENT '会员ID',
    `receiver_name` VARCHAR(64)   NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(20)  NOT NULL COMMENT '收货人电话',
    `province`      VARCHAR(32)   NOT NULL COMMENT '省',
    `city`          VARCHAR(32)   NOT NULL COMMENT '市',
    `district`      VARCHAR(32)   NULL COMMENT '区/县',
    `detail`        VARCHAR(256)  NOT NULL COMMENT '详细地址',
    `zip_code`      VARCHAR(10)   NULL COMMENT '邮编',
    `is_default`    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否默认 1是 0否',
    `label`         VARCHAR(32)   NULL COMMENT '地址标签 家/公司/学校',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地址表';

-- ==================== 系统管理 ====================

-- 3. 系统用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT        NOT NULL COMMENT '用户ID',
    `username`      VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`      VARCHAR(256)  NOT NULL COMMENT '密码(BCrypt)',
    `real_name`     VARCHAR(64)   NULL COMMENT '真实姓名',
    `phone`         VARCHAR(20)   NULL COMMENT '手机号',
    `email`         VARCHAR(128)  NULL COMMENT '邮箱',
    `avatar`        VARCHAR(512)  NULL COMMENT '头像',
    `shop_id`       BIGINT        NULL COMMENT '关联店铺ID(商家)',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `type`          TINYINT       NOT NULL DEFAULT 0 COMMENT '类型 0平台超管 1平台运营 2商家管理员 3商家员工',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 4. 系统角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`            BIGINT        NOT NULL COMMENT '角色ID',
    `name`          VARCHAR(64)   NOT NULL COMMENT '角色名称',
    `code`          VARCHAR(64)   NOT NULL COMMENT '角色编码',
    `shop_id`       BIGINT        NULL COMMENT '所属店铺ID(平台角色为空)',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 5. 系统权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`            BIGINT        NOT NULL COMMENT '权限ID',
    `name`          VARCHAR(64)   NOT NULL COMMENT '权限名称',
    `code`          VARCHAR(128)  NOT NULL COMMENT '权限编码',
    `type`          TINYINT       NOT NULL DEFAULT 0 COMMENT '类型 0菜单 1按钮 2API',
    `parent_id`     BIGINT        NULL COMMENT '父权限ID',
    `path`          VARCHAR(256)  NULL COMMENT '路由路径',
    `icon`          VARCHAR(64)   NULL COMMENT '图标',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

-- 6. 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `user_id`       BIGINT        NOT NULL COMMENT '系统用户ID',
    `role_id`       BIGINT        NOT NULL COMMENT '角色ID',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 7. 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `role_id`       BIGINT        NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT        NOT NULL COMMENT '权限ID',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ==================== 店铺 ====================

-- 8. 店铺表
CREATE TABLE IF NOT EXISTS `shop_shop` (
    `id`            BIGINT        NOT NULL COMMENT '店铺ID',
    `name`          VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    `logo`          VARCHAR(512)  NULL COMMENT '店铺Logo',
    `banner`        VARCHAR(512)  NULL COMMENT '店铺Banner',
    `description`   TEXT          NULL COMMENT '店铺描述',
    `contact_name`  VARCHAR(64)   NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20)   NULL COMMENT '联系电话',
    `province`      VARCHAR(32)   NULL COMMENT '省',
    `city`          VARCHAR(32)   NULL COMMENT '市',
    `district`      VARCHAR(32)   NULL COMMENT '区/县',
    `address`       VARCHAR(256)  NULL COMMENT '详细地址',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1已上架 2已下架 3审核拒绝',
    `audit_remark`  VARCHAR(512)  NULL COMMENT '审核备注',
    `type`          TINYINT       NOT NULL DEFAULT 0 COMMENT '店铺类型 0自营 1第三方',
    `score`         DECIMAL(3,2)  NOT NULL DEFAULT 5.00 COMMENT '评分',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺表';

-- ==================== 商品体系 ====================

-- 9. 商品分类表
CREATE TABLE IF NOT EXISTS `pms_category` (
    `id`            BIGINT        NOT NULL COMMENT '分类ID',
    `name`          VARCHAR(64)   NOT NULL COMMENT '分类名称',
    `parent_id`     BIGINT        NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `level`         TINYINT       NOT NULL DEFAULT 1 COMMENT '层级 1一级 2二级 3三级',
    `icon`          VARCHAR(512)  NULL COMMENT '图标',
    `image`         VARCHAR(512)  NULL COMMENT '分类图片',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1显示 0隐藏',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 10. 商品SPU表
CREATE TABLE IF NOT EXISTS `pms_spu` (
    `id`            BIGINT        NOT NULL COMMENT 'SPU ID',
    `name`          VARCHAR(256)  NOT NULL COMMENT '商品名称',
    `subtitle`      VARCHAR(256)  NULL COMMENT '副标题/卖点',
    `category_id`   BIGINT        NOT NULL COMMENT '分类ID',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `brand`         VARCHAR(128)  NULL COMMENT '品牌',
    `description`   TEXT          NULL COMMENT '商品详情(HTML)',
    `main_image`    VARCHAR(512)  NULL COMMENT '主图URL',
    `unit`          VARCHAR(16)   NULL COMMENT '单位 件/个/斤',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0下架 1上架',
    `audit_status`  TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态 0待审核 1通过 2拒绝',
    `min_price`     DECIMAL(12,2) NULL COMMENT '最低SKU价格',
    `max_price`     DECIMAL(12,2) NULL COMMENT '最高SKU价格',
    `total_sales`   INT           NOT NULL DEFAULT 0 COMMENT '总销量',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status` (`status`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU表';

-- 11. 商品SKU表
CREATE TABLE IF NOT EXISTS `pms_sku` (
    `id`            BIGINT        NOT NULL COMMENT 'SKU ID',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `sku_code`      VARCHAR(64)   NOT NULL COMMENT 'SKU编码',
    `name`          VARCHAR(256)  NULL COMMENT 'SKU名称(规格组合)',
    `image`         VARCHAR(512)  NULL COMMENT 'SKU图片',
    `price`         DECIMAL(12,2) NOT NULL COMMENT '售价',
    `original_price` DECIMAL(12,2) NULL COMMENT '原价',
    `cost_price`    DECIMAL(12,2) NULL COMMENT '成本价',
    `weight`        DECIMAL(10,2) NULL COMMENT '重量(kg)',
    `volume`        DECIMAL(10,2) NULL COMMENT '体积(m³)',
    `attrs`         JSON          NULL COMMENT '规格属性键值对 {"颜色":"红色","尺寸":"M"}',
    `stock`         INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    `frozen_stock`  INT           NOT NULL DEFAULT 0 COMMENT '冻结库存(已下单未支付)',
    `sales`         INT           NOT NULL DEFAULT 0 COMMENT '销量',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_price` (`price`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SKU表';

-- 12. 商品SPU图片表
CREATE TABLE IF NOT EXISTS `pms_spu_image` (
    `id`            BIGINT        NOT NULL COMMENT '图片ID',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `url`           VARCHAR(512)  NOT NULL COMMENT '图片URL',
    `sort`          INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `type`          TINYINT       NOT NULL DEFAULT 0 COMMENT '类型 0轮播图 1详情图',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品SPU图片表';

-- 13. 秒杀活动表
CREATE TABLE IF NOT EXISTS `pms_seckill_activity` (
    `id`            BIGINT        NOT NULL COMMENT '活动ID',
    `name`          VARCHAR(128)  NOT NULL COMMENT '活动名称',
    `sku_id`        BIGINT        NOT NULL COMMENT '秒杀SKU ID',
    `spu_id`        BIGINT        NOT NULL COMMENT '秒杀SPU ID',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `seckill_price` DECIMAL(12,2) NOT NULL COMMENT '秒杀价',
    `seckill_stock` INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    `limit_per_user` INT          NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `start_time`    DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`      DATETIME      NOT NULL COMMENT '结束时间',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1待开始 2进行中 3已结束 4已取消',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

-- ==================== 购物车 ====================

-- 14. 购物车表
CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `member_id`     BIGINT        NOT NULL COMMENT '会员ID',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `quantity`      INT           NOT NULL DEFAULT 1 COMMENT '数量',
    `price`         DECIMAL(12,2) NOT NULL COMMENT '加入时单价',
    `selected`      TINYINT       NOT NULL DEFAULT 1 COMMENT '是否选中 1选中 0未选',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_sku` (`member_id`, `sku_id`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_spu_id` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ==================== 订单体系 ====================

-- 15. 订单表
CREATE TABLE IF NOT EXISTS `oms_order` (
    `id`                BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`          VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `member_id`         BIGINT        NOT NULL COMMENT '会员ID',
    `shop_id`           BIGINT        NOT NULL COMMENT '店铺ID',
    `total_amount`      DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    `pay_amount`        DECIMAL(12,2) NOT NULL COMMENT '实付金额',
    `freight_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
    `discount_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_method`        TINYINT       NULL COMMENT '支付方式 1余额 2支付宝 3微信',
    `pay_status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2已退款 3部分退款',
    `order_status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态 0待付款 1待发货 2已发货 3已收货 4已完成 5已取消 6售后中',
    `delivery_company`  VARCHAR(64)   NULL COMMENT '快递公司',
    `delivery_no`       VARCHAR(64)   NULL COMMENT '快递单号',
    `payment_time`      DATETIME      NULL COMMENT '支付时间',
    `delivery_time`     DATETIME      NULL COMMENT '发货时间',
    `receive_time`      DATETIME      NULL COMMENT '收货时间',
    `finish_time`       DATETIME      NULL COMMENT '完成时间',
    `cancel_time`       DATETIME      NULL COMMENT '取消时间',
    `cancel_reason`     VARCHAR(256)  NULL COMMENT '取消原因',
    `receiver_name`     VARCHAR(64)   NOT NULL COMMENT '收货人',
    `receiver_phone`    VARCHAR(20)   NOT NULL COMMENT '收货电话',
    `receiver_province` VARCHAR(32)   NOT NULL COMMENT '收件省',
    `receiver_city`     VARCHAR(32)   NOT NULL COMMENT '收件市',
    `receiver_district` VARCHAR(32)   NULL COMMENT '收件区',
    `receiver_address`  VARCHAR(256)  NOT NULL COMMENT '收件详细地址',
    `member_remark`     VARCHAR(512)  NULL COMMENT '买家备注',
    `shop_remark`       VARCHAR(512)  NULL COMMENT '商家备注',
    `create_by`         VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`         VARCHAR(64)   NULL COMMENT '更新人',
    `remark`            VARCHAR(512)  NULL COMMENT '备注',
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_order_status` (`order_status`),
    KEY `idx_pay_status` (`pay_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 16. 订单项表
CREATE TABLE IF NOT EXISTS `oms_order_item` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `sku_code`      VARCHAR(64)   NULL COMMENT 'SKU编码',
    `product_name`  VARCHAR(256)  NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(512)  NULL COMMENT '商品图片',
    `attrs`         JSON          NULL COMMENT '规格属性快照',
    `quantity`      INT           NOT NULL COMMENT '购买数量',
    `price`         DECIMAL(12,2) NOT NULL COMMENT '单价',
    `subtotal`      DECIMAL(12,2) NOT NULL COMMENT '小计',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- 17. 订单状态日志表
CREATE TABLE IF NOT EXISTS `oms_order_status_log` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `from_status`   TINYINT       NULL COMMENT '原状态',
    `to_status`     TINYINT       NOT NULL COMMENT '新状态',
    `operator_type` TINYINT       NOT NULL DEFAULT 0 COMMENT '操作人类型 0系统 1用户 2商家 3管理员',
    `operator_id`   BIGINT        NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64)   NULL COMMENT '操作人名称',
    `content`       VARCHAR(512)  NULL COMMENT '操作内容',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态日志表';

-- ==================== 库存体系 ====================

-- 18. 库存表
CREATE TABLE IF NOT EXISTS `wms_stock` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `spu_id`        BIGINT        NOT NULL COMMENT 'SPU ID',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `warehouse_code` VARCHAR(64)  NOT NULL DEFAULT 'default' COMMENT '仓库编码',
    `total_stock`   INT           NOT NULL DEFAULT 0 COMMENT '总库存',
    `available_stock` INT         NOT NULL DEFAULT 0 COMMENT '可用库存',
    `frozen_stock`  INT           NOT NULL DEFAULT 0 COMMENT '冻结库存(已锁)',
    `safety_stock`  INT           NOT NULL DEFAULT 0 COMMENT '安全库存',
    `version`       INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_warehouse` (`sku_id`, `warehouse_code`),
    KEY `idx_spu_id` (`spu_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_available_stock` (`available_stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 19. 库存锁定表
CREATE TABLE IF NOT EXISTS `wms_stock_lock` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `sku_id`        BIGINT        NOT NULL COMMENT 'SKU ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `quantity`      INT           NOT NULL COMMENT '锁定数量',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0锁定中 1已出库 2已释放 3已取消',
    `expire_time`   DATETIME      NULL COMMENT '锁超时时间',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存锁定表';

-- 20. 出库任务表
CREATE TABLE IF NOT EXISTS `wms_outbound_task` (
    `id`            BIGINT        NOT NULL COMMENT 'ID',
    `order_id`      BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `shop_id`       BIGINT        NOT NULL COMMENT '店铺ID',
    `warehouse_code` VARCHAR(64)  NOT NULL DEFAULT 'default' COMMENT '仓库编码',
    `status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待出库 1出库中 2已出库 3已取消',
    `total_items`   INT           NOT NULL DEFAULT 0 COMMENT '总商品件数',
    `outbound_time` DATETIME      NULL COMMENT '出库时间',
    `operator_id`   BIGINT        NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64)   NULL COMMENT '操作人名称',
    `create_by`     VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)   NULL COMMENT '更新人',
    `remark`        VARCHAR(512)  NULL COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0,
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库任务表';

-- ==================== 支付 ====================

-- 21. 支付记录表
CREATE TABLE IF NOT EXISTS `pay_payment` (
    `id`                BIGINT        NOT NULL COMMENT 'ID',
    `order_id`          BIGINT        NOT NULL COMMENT '订单ID',
    `order_no`          VARCHAR(64)   NOT NULL COMMENT '订单编号',
    `member_id`         BIGINT        NOT NULL COMMENT '会员ID',
    `pay_amount`        DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    `pay_method`        TINYINT       NOT NULL COMMENT '支付方式 1余额 2支付宝 3微信',
    `transaction_no`    VARCHAR(128)  NULL COMMENT '第三方交易号(唯一)',
    `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待支付 1支付成功 2支付失败 3已退款',
    `pay_time`          DATETIME      NULL COMMENT '支付成功时间',
    `callback_content`  TEXT          NULL COMMENT '回调原始内容(JSON)',
    `refund_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已退款金额',
    `refund_time`       DATETIME      NULL COMMENT '退款时间',
    `create_by`         VARCHAR(64)   NULL COMMENT '创建人',
    `update_by`         VARCHAR(64)   NULL COMMENT '更新人',
    `remark`            VARCHAR(512)  NULL COMMENT '备注',
    `deleted`           TINYINT       NOT NULL DEFAULT 0,
    `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_member_id` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';
