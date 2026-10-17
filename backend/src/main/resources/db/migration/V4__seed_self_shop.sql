-- ============================================
-- V4__seed_self_shop.sql - 初始化自营店铺
-- 平台端创建的 SPU 需归属自营店铺（pms_spu.shop_id NOT NULL）
-- ============================================

INSERT INTO `shop_shop` (`id`, `name`, `logo`, `description`, `contact_name`, `contact_phone`, `status`, `type`, `score`, `deleted`, `create_time`, `update_time`)
VALUES (1, '官方自营旗舰店', NULL, '平台官方自营店铺', '平台客服', '4008008888', 1, 0, 5.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;
