-- ============================================
-- V3_1__demo_seed.sql - 本地联调演示数据
-- 仅 local profile 加载（spring.flyway.locations 追加 classpath:db/seed）
-- 用途：打通 注册登录 → 加购 → 下单锁库存 → 沙箱支付 → 发货 → 确认收货 全链路
-- ============================================

-- 演示店铺（商家账号 shop/shop123 归属）
INSERT INTO `shop_shop` (`id`, `name`, `contact_name`, `contact_phone`, `province`, `city`, `district`, `address`, `status`, `type`, `create_time`, `update_time`)
VALUES (1, '示例官方旗舰店', '张三', '13900000000', '广东省', '深圳市', '南山区', '科技园1号', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 商家账号(shop/shop123)关联店铺
UPDATE `sys_user` SET `shop_id` = 1 WHERE `id` = 2;

-- 商品分类
INSERT INTO `pms_category` (`id`, `name`, `parent_id`, `level`, `sort`, `status`, `create_time`, `update_time`)
VALUES (1, '数码配件', 0, 1, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- SPU（属于演示店铺，已上架、已审核）
INSERT INTO `pms_spu` (`id`, `name`, `subtitle`, `category_id`, `shop_id`, `brand`, `status`, `audit_status`, `min_price`, `max_price`, `total_sales`, `create_time`, `update_time`)
VALUES (1, '示例商品 无线蓝牙耳机', '高保真 · 主动降噪', 1, 1, 'DemoSound', 1, 1, 99.00, 99.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- SKU
INSERT INTO `pms_sku` (`id`, `spu_id`, `sku_code`, `name`, `price`, `stock`, `status`, `sort`, `create_time`, `update_time`)
VALUES (1, 1, 'SKU-DEMO-001', '标准版 / 曜石黑', 99.00, 100, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 库存（Redis 缓存从 wms_stock 初始化）
INSERT INTO `wms_stock` (`id`, `sku_id`, `spu_id`, `shop_id`, `warehouse_code`, `total_stock`, `available_stock`, `frozen_stock`, `create_time`, `update_time`)
VALUES (1, 1, 1, 1, 'default', 100, 100, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 演示会员：手机号 13800000000，可直接用验证码登录
INSERT INTO `user_member` (`id`, `username`, `password`, `nickname`, `phone`, `status`, `create_time`, `update_time`)
VALUES (1, 'demo_member', '$2b$10$H6pvr0bZCn4CC3bvQCYap.4VZsVldQ3O8r1H74usd0mlDPF5fOf1S', '演示用户', '13800000000', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 演示收货地址
INSERT INTO `user_address` (`id`, `member_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail`, `is_default`, `create_time`, `update_time`)
VALUES (1, 1, '李四', '13800000000', '广东省', '深圳市', '南山区', '科技园2号 A座101', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;
