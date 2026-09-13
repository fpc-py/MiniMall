-- ============================================
-- V3__seed_data.sql - 初始化种子数据
-- ============================================

-- 默认管理员账号：admin / admin123
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `type`, `status`, `deleted`, `create_time`, `update_time`)
VALUES (1, 'admin', '$2b$10$H6pvr0bZCn4CC3bvQCYap.4VZsVldQ3O8r1H74usd0mlDPF5fOf1S', '管理员', 0, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 默认测试商家账号：shop / shop123
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `type`, `status`, `deleted`, `create_time`, `update_time`)
VALUES (2, 'shop', '$2b$10$H6pvr0bZCn4CC3bvQCYap.4VZsVldQ3O8r1H74usd0mlDPF5fOf1S', '测试商家', 2, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `id` = `id`;