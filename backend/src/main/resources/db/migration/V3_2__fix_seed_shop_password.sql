-- ============================================
-- V3_2__fix_seed_shop_password.sql
-- 修正种子数据缺陷：V3__seed_data.sql 中 admin 与 shop 误用了同一个 BCrypt 哈希，
-- 导致 shop 账号实际密码为 admin123，与文档/注释声明的 shop123 不符。
-- 本迁移将 shop 账号密码修正为 shop123 对应的 BCrypt 哈希。
-- ============================================

UPDATE `sys_user`
SET `password` = '$2a$10$lESxgQdbbVn7pakXJWlxvOqD.ATi3XJFNclTjwkY1Yaz6oYThjZPy'
WHERE `username` = 'shop';
