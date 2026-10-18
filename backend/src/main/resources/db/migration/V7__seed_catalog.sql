-- ============================================
-- V7__seed_catalog.sql - 演示种子数据（分类/商品/库存/优惠券/会员）
-- 目的：让首页、商品详情、领券中心、下单闭环开箱即可看到完整效果。
-- 全部使用固定主键 + ON DUPLICATE KEY UPDATE，保证重复执行幂等。
-- ============================================

-- 0. 修复种子商家账号：V3 创建 shop 用户时未绑定店铺，导致商家端接口因 shopId 为空而 403
UPDATE `sys_user` SET `shop_id` = 1 WHERE `username` = 'shop';

-- ==================== 1. 商品分类（一级 6 个 / 二级 18 个） ====================
INSERT INTO `pms_category` (`id`, `name`, `parent_id`, `level`, `icon`, `sort`, `status`) VALUES
(101, '手机数码', 0, 1, 'phone-o', 1, 1),
(102, '电脑办公', 0, 1, 'computer-o', 2, 1),
(103, '家用电器', 0, 1, 'tv-o', 3, 1),
(104, '服饰鞋包', 0, 1, 'bag-o', 4, 1),
(105, '食品生鲜', 0, 1, 'shopping-cart-o', 5, 1),
(106, '美妆个护', 0, 1, 'flower-o', 6, 1),
(10101, '手机', 101, 2, NULL, 1, 1),
(10102, '平板电脑', 101, 2, NULL, 2, 1),
(10103, '智能穿戴', 101, 2, NULL, 3, 1),
(10201, '笔记本', 102, 2, NULL, 1, 1),
(10202, '台式机', 102, 2, NULL, 2, 1),
(10203, '显示器', 102, 2, NULL, 3, 1),
(10301, '电视', 103, 2, NULL, 1, 1),
(10302, '空调', 103, 2, NULL, 2, 1),
(10303, '冰箱洗衣机', 103, 2, NULL, 3, 1),
(10401, '男装', 104, 2, NULL, 1, 1),
(10402, '女装', 104, 2, NULL, 2, 1),
(10403, '运动鞋', 104, 2, NULL, 3, 1),
(10501, '休闲零食', 105, 2, NULL, 1, 1),
(10502, '生鲜水果', 105, 2, NULL, 2, 1),
(10503, '粮油调味', 105, 2, NULL, 3, 1),
(10601, '面部护肤', 106, 2, NULL, 1, 1),
(10602, '彩妆', 106, 2, NULL, 2, 1),
(10603, '洗护清洁', 106, 2, NULL, 3, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `parent_id` = VALUES(`parent_id`), `level` = VALUES(`level`), `sort` = VALUES(`sort`), `status` = VALUES(`status`);

-- ==================== 2. 商品 SPU（12 条，均属官方自营旗舰店 shop_id=1，已上架且审核通过） ====================
INSERT INTO `pms_spu`
(`id`, `name`, `subtitle`, `category_id`, `shop_id`, `brand`, `description`, `main_image`, `unit`, `status`, `audit_status`, `min_price`, `max_price`, `total_sales`) VALUES
(1001, '旗舰智能手机 5G全网通 12GB+256GB 高清拍照 长续航', '骁龙旗舰芯片 · 一亿像素主摄', 10101, 1, '星辰', '<p>6.7英寸OLED直屏，5000mAh大电池，支持66W快充。</p>', 'https://picsum.photos/seed/spu1001/800/800', '台', 1, 1, 3999.00, 4299.00, 1286),
(1002, '轻薄笔记本电脑 14英寸 商务办公 学生本', '16G内存 · 512G固态 · 1.2kg轻薄', 10201, 1, '极光', '<p>2.8K高色域屏，长续航，全金属机身。</p>', 'https://picsum.photos/seed/spu1002/800/800', '台', 1, 1, 4599.00, 5299.00, 863),
(1003, '智能4K电视 65英寸 全面屏 家庭影院', '4K HDR · 远场语音 · 3+64G', 10301, 1, '视界', '<p>65英寸全面屏，MEMC运动补偿，Dolby音效。</p>', 'https://picsum.photos/seed/spu1003/800/800', '台', 1, 1, 3299.00, 4999.00, 632),
(1004, '蓝牙降噪耳机 无线 长续航 HiFi音质', '主动降噪 · 40小时续航', 10103, 1, '声阔', '<p>双馈主动降噪，LDAC高清音频，佩戴舒适。</p>', 'https://picsum.photos/seed/spu1004/800/800', '副', 1, 1, 599.00, 999.00, 2138),
(1005, '运动跑鞋 透气网面 轻便减震 男女同款', '回弹科技 · 轻量缓震', 10403, 1, '疾风', '<p>飞织透气鞋面，缓震中底，适合日常跑步与通勤。</p>', 'https://picsum.photos/seed/spu1005/800/800', '双', 1, 1, 299.00, 599.00, 1867),
(1006, '精华液护肤品 保湿补水 提亮肤色', '小分子玻尿酸 · 温和不刺激', 10601, 1, '芙洛', '<p>多重保湿精华，改善干燥粗糙，适配敏感肌。</p>', 'https://picsum.photos/seed/spu1006/800/800', '瓶', 1, 1, 239.00, 399.00, 3421),
(1007, '男士休闲夹克 春秋薄款 修身防风', '简约百搭 · 多色可选', 10401, 1, '本色', '<p>轻薄防风面料，立体剪裁，通勤休闲两相宜。</p>', 'https://picsum.photos/seed/spu1007/800/800', '件', 1, 1, 189.00, 359.00, 742),
(1008, '新疆红枣 特级和田大枣 500g', '原产地直发 · 独立小包装', 10502, 1, '疆果', '<p>新疆和田原产，果肉饱满，自然晾晒无添加。</p>', 'https://picsum.photos/seed/spu1008/800/800', '袋', 1, 1, 39.90, 69.90, 5216),
(1009, '智能手表 运动心率 血氧监测 长续航', '14天续航 · 100+运动模式', 10103, 1, '星辰', '<p>1.43英寸AMOLED屏，全天候健康监测，5ATM防水。</p>', 'https://picsum.photos/seed/spu1009/800/800', '只', 1, 1, 1299.00, 1699.00, 954),
(1010, '电竞显示器 27英寸 2K 165Hz 快速液晶', 'IPS硬屏 · 1ms响应 · HDR400', 10203, 1, '视界', '<p>2K分辨率165Hz刷新率，支持旋转升降，护眼低蓝光。</p>', 'https://picsum.photos/seed/spu1010/800/800', '台', 1, 1, 1099.00, 1599.00, 586),
(1011, '变频空调 1.5匹 一级能效 壁挂式', '快速冷暖 · 静音节能', 10302, 1, '清风', '<p>一级能效变频压缩机，自清洁，低噪运行。</p>', 'https://picsum.photos/seed/spu1011/800/800', '台', 1, 1, 2599.00, 3299.00, 431),
(1012, '氨基酸洗面奶 温和洁净 保湿不紧绷', '弱酸性 · 敏感肌可用', 10603, 1, '芙洛', '<p>氨基酸表活温和清洁，绵密泡沫，洗后不紧绷。</p>', 'https://picsum.photos/seed/spu1012/800/800', '支', 1, 1, 59.00, 99.00, 4188)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `subtitle` = VALUES(`subtitle`), `category_id` = VALUES(`category_id`), `shop_id` = VALUES(`shop_id`), `brand` = VALUES(`brand`), `main_image` = VALUES(`main_image`), `unit` = VALUES(`unit`), `status` = VALUES(`status`), `audit_status` = VALUES(`audit_status`), `min_price` = VALUES(`min_price`), `max_price` = VALUES(`max_price`), `total_sales` = VALUES(`total_sales`);

-- ==================== 3. 商品 SKU（每 SPU 2 个规格） ====================
INSERT INTO `pms_sku`
(`id`, `spu_id`, `sku_code`, `name`, `image`, `price`, `original_price`, `attrs`, `stock`, `frozen_stock`, `sales`, `status`, `sort`) VALUES
(2001, 1001, 'SKU1001-BLK-256', '曜石黑 12GB+256GB', 'https://picsum.photos/seed/sku2001/600/600', 3999.00, 4999.00, '{"颜色":"曜石黑","存储":"12GB+256GB"}', 500, 0, 0, 1, 1),
(2002, 1001, 'SKU1001-BLU-512', '冰川蓝 16GB+512GB', 'https://picsum.photos/seed/sku2002/600/600', 4299.00, 5299.00, '{"颜色":"冰川蓝","存储":"16GB+512GB"}', 300, 0, 0, 1, 2),
(2003, 1002, 'SKU1002-SIL-16-512', '银色 16G+512G', 'https://picsum.photos/seed/sku2003/600/600', 4599.00, 5599.00, '{"颜色":"银色","配置":"16G+512G"}', 200, 0, 0, 1, 1),
(2004, 1002, 'SKU1002-GRY-32-1T', '深空灰 32G+1T', 'https://picsum.photos/seed/sku2004/600/600', 5299.00, 6299.00, '{"颜色":"深空灰","配置":"32G+1T"}', 120, 0, 0, 1, 2),
(2005, 1003, 'SKU1003-65-STD', '65英寸 标准版', 'https://picsum.photos/seed/sku2005/600/600', 3299.00, 4299.00, '{"尺寸":"65英寸","版本":"标准版"}', 150, 0, 0, 1, 1),
(2006, 1003, 'SKU1003-65-PRO', '65英寸 高配版 3+64G', 'https://picsum.photos/seed/sku2006/600/600', 4999.00, 5999.00, '{"尺寸":"65英寸","版本":"高配版"}', 80, 0, 0, 1, 2),
(2007, 1004, 'SKU1004-BLK', '静夜黑', 'https://picsum.photos/seed/sku2007/600/600', 599.00, 899.00, '{"颜色":"静夜黑"}', 800, 0, 0, 1, 1),
(2008, 1004, 'SKU1004-WHT', '月光白', 'https://picsum.photos/seed/sku2008/600/600', 999.00, 1299.00, '{"颜色":"月光白"}', 600, 0, 0, 1, 2),
(2009, 1005, 'SKU1005-40-BLK', '40码 曜石黑', 'https://picsum.photos/seed/sku2009/600/600', 299.00, 499.00, '{"尺码":"40","颜色":"曜石黑"}', 400, 0, 0, 1, 1),
(2010, 1005, 'SKU1005-42-GRY', '42码 岩石灰', 'https://picsum.photos/seed/sku2010/600/600', 599.00, 799.00, '{"尺码":"42","颜色":"岩石灰"}', 350, 0, 0, 1, 2),
(2011, 1006, 'SKU1006-30ML', '30ml 精华液', 'https://picsum.photos/seed/sku2011/600/600', 239.00, 329.00, '{"规格":"30ml"}', 1000, 0, 0, 1, 1),
(2012, 1006, 'SKU1006-50ML', '50ml 精华液', 'https://picsum.photos/seed/sku2012/600/600', 399.00, 499.00, '{"规格":"50ml"}', 700, 0, 0, 1, 2),
(2013, 1007, 'SKU1007-M-BLK', 'M码 黑色', 'https://picsum.photos/seed/sku2013/600/600', 189.00, 299.00, '{"尺码":"M","颜色":"黑色"}', 300, 0, 0, 1, 1),
(2014, 1007, 'SKU1007-L-NVY', 'L码 藏青', 'https://picsum.photos/seed/sku2014/600/600', 359.00, 499.00, '{"尺码":"L","颜色":"藏青"}', 260, 0, 0, 1, 2),
(2015, 1008, 'SKU1008-500G', '500g 独立装', 'https://picsum.photos/seed/sku2015/600/600', 39.90, 59.90, '{"规格":"500g"}', 2000, 0, 0, 1, 1),
(2016, 1008, 'SKU1008-1KG', '1kg 家庭装', 'https://picsum.photos/seed/sku2016/600/600', 69.90, 99.90, '{"规格":"1kg"}', 1500, 0, 0, 1, 2),
(2017, 1009, 'SKU1009-BLK', '曜石黑 标准版', 'https://picsum.photos/seed/sku2017/600/600', 1299.00, 1599.00, '{"颜色":"曜石黑"}', 220, 0, 0, 1, 1),
(2018, 1009, 'SKU1009-GLD', '星河金 皮革版', 'https://picsum.photos/seed/sku2018/600/600', 1699.00, 1999.00, '{"颜色":"星河金","版本":"皮革版"}', 140, 0, 0, 1, 2),
(2019, 1010, 'SKU1010-27-2K', '27英寸 2K 165Hz', 'https://picsum.photos/seed/sku2019/600/600', 1099.00, 1499.00, '{"尺寸":"27英寸","刷新率":"165Hz"}', 180, 0, 0, 1, 1),
(2020, 1010, 'SKU1010-32-2K', '32英寸 2K 165Hz', 'https://picsum.photos/seed/sku2020/600/600', 1599.00, 1999.00, '{"尺寸":"32英寸","刷新率":"165Hz"}', 90, 0, 0, 1, 2),
(2021, 1011, 'SKU1011-15-PRO', '1.5匹 一级能效', 'https://picsum.photos/seed/sku2021/600/600', 2599.00, 3199.00, '{"匹数":"1.5匹","能效":"一级"}', 130, 0, 0, 1, 1),
(2022, 1011, 'SKU1011-20-PRO', '2匹 一级能效', 'https://picsum.photos/seed/sku2022/600/600', 3299.00, 3999.00, '{"匹数":"2匹","能效":"一级"}', 70, 0, 0, 1, 2),
(2023, 1012, 'SKU1012-100G', '100g 温和型', 'https://picsum.photos/seed/sku2023/600/600', 59.00, 89.00, '{"规格":"100g"}', 1200, 0, 0, 1, 1),
(2024, 1012, 'SKU1012-150G', '150g 保湿型', 'https://picsum.photos/seed/sku2024/600/600', 99.00, 139.00, '{"规格":"150g"}', 900, 0, 0, 1, 2)
ON DUPLICATE KEY UPDATE `spu_id` = VALUES(`spu_id`), `sku_code` = VALUES(`sku_code`), `name` = VALUES(`name`), `image` = VALUES(`image`), `price` = VALUES(`price`), `original_price` = VALUES(`original_price`), `attrs` = VALUES(`attrs`), `stock` = VALUES(`stock`), `status` = VALUES(`status`), `sort` = VALUES(`sort`);

-- ==================== 4. SPU 轮播图（每 SPU 2 张） ====================
INSERT INTO `pms_spu_image` (`id`, `spu_id`, `url`, `sort`, `type`) VALUES
(3001, 1001, 'https://picsum.photos/seed/spu1001/800/800', 1, 0),
(3002, 1001, 'https://picsum.photos/seed/spu1001b/800/800', 2, 0),
(3003, 1002, 'https://picsum.photos/seed/spu1002/800/800', 1, 0),
(3004, 1002, 'https://picsum.photos/seed/spu1002b/800/800', 2, 0),
(3005, 1003, 'https://picsum.photos/seed/spu1003/800/800', 1, 0),
(3006, 1003, 'https://picsum.photos/seed/spu1003b/800/800', 2, 0),
(3007, 1004, 'https://picsum.photos/seed/spu1004/800/800', 1, 0),
(3008, 1004, 'https://picsum.photos/seed/spu1004b/800/800', 2, 0),
(3009, 1005, 'https://picsum.photos/seed/spu1005/800/800', 1, 0),
(3010, 1005, 'https://picsum.photos/seed/spu1005b/800/800', 2, 0),
(3011, 1006, 'https://picsum.photos/seed/spu1006/800/800', 1, 0),
(3012, 1006, 'https://picsum.photos/seed/spu1006b/800/800', 2, 0),
(3013, 1007, 'https://picsum.photos/seed/spu1007/800/800', 1, 0),
(3014, 1007, 'https://picsum.photos/seed/spu1007b/800/800', 2, 0),
(3015, 1008, 'https://picsum.photos/seed/spu1008/800/800', 1, 0),
(3016, 1008, 'https://picsum.photos/seed/spu1008b/800/800', 2, 0),
(3017, 1009, 'https://picsum.photos/seed/spu1009/800/800', 1, 0),
(3018, 1009, 'https://picsum.photos/seed/spu1009b/800/800', 2, 0),
(3019, 1010, 'https://picsum.photos/seed/spu1010/800/800', 1, 0),
(3020, 1010, 'https://picsum.photos/seed/spu1010b/800/800', 2, 0),
(3021, 1011, 'https://picsum.photos/seed/spu1011/800/800', 1, 0),
(3022, 1011, 'https://picsum.photos/seed/spu1011b/800/800', 2, 0),
(3023, 1012, 'https://picsum.photos/seed/spu1012/800/800', 1, 0),
(3024, 1012, 'https://picsum.photos/seed/spu1012b/800/800', 2, 0)
ON DUPLICATE KEY UPDATE `spu_id` = VALUES(`spu_id`), `url` = VALUES(`url`), `sort` = VALUES(`sort`), `type` = VALUES(`type`);

-- ==================== 5. 库存（与 pms_sku.stock 一致，供下单 available_stock>=? 条件更新使用） ====================
INSERT INTO `wms_stock`
(`id`, `sku_id`, `spu_id`, `shop_id`, `warehouse_code`, `total_stock`, `available_stock`, `frozen_stock`, `safety_stock`, `version`) VALUES
(4001, 2001, 1001, 1, 'default', 500, 500, 0, 10, 0),
(4002, 2002, 1001, 1, 'default', 300, 300, 0, 10, 0),
(4003, 2003, 1002, 1, 'default', 200, 200, 0, 10, 0),
(4004, 2004, 1002, 1, 'default', 120, 120, 0, 10, 0),
(4005, 2005, 1003, 1, 'default', 150, 150, 0, 10, 0),
(4006, 2006, 1003, 1, 'default', 80, 80, 0, 10, 0),
(4007, 2007, 1004, 1, 'default', 800, 800, 0, 10, 0),
(4008, 2008, 1004, 1, 'default', 600, 600, 0, 10, 0),
(4009, 2009, 1005, 1, 'default', 400, 400, 0, 10, 0),
(4010, 2010, 1005, 1, 'default', 350, 350, 0, 10, 0),
(4011, 2011, 1006, 1, 'default', 1000, 1000, 0, 10, 0),
(4012, 2012, 1006, 1, 'default', 700, 700, 0, 10, 0),
(4013, 2013, 1007, 1, 'default', 300, 300, 0, 10, 0),
(4014, 2014, 1007, 1, 'default', 260, 260, 0, 10, 0),
(4015, 2015, 1008, 1, 'default', 2000, 2000, 0, 10, 0),
(4016, 2016, 1008, 1, 'default', 1500, 1500, 0, 10, 0),
(4017, 2017, 1009, 1, 'default', 220, 220, 0, 10, 0),
(4018, 2018, 1009, 1, 'default', 140, 140, 0, 10, 0),
(4019, 2019, 1010, 1, 'default', 180, 180, 0, 10, 0),
(4020, 2020, 1010, 1, 'default', 90, 90, 0, 10, 0),
(4021, 2021, 1011, 1, 'default', 130, 130, 0, 10, 0),
(4022, 2022, 1011, 1, 'default', 70, 70, 0, 10, 0),
(4023, 2023, 1012, 1, 'default', 1200, 1200, 0, 10, 0),
(4024, 2024, 1012, 1, 'default', 900, 900, 0, 10, 0)
ON DUPLICATE KEY UPDATE `spu_id` = VALUES(`spu_id`), `shop_id` = VALUES(`shop_id`), `total_stock` = VALUES(`total_stock`), `available_stock` = VALUES(`available_stock`), `frozen_stock` = VALUES(`frozen_stock`);

-- ==================== 6. 优惠券（2 张平台通用 + 2 张店铺专享） ====================
INSERT INTO `pms_coupon`
(`id`, `name`, `type`, `shop_id`, `threshold_amount`, `discount_amount`, `discount_rate`, `total_count`, `received_count`, `per_user_limit`, `start_time`, `end_time`, `status`, `description`) VALUES
(5001, '新人专享 满199减20', 1, NULL, 199.00, 20.00, NULL, 10000, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '全场通用，满199元可用，仅限领取1张'),
(5002, '平台通用 满599减80', 1, NULL, 599.00, 80.00, NULL, 5000, 0, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '全场通用，满599元可用'),
(5003, '官方自营店 满299减30', 1, 1, 299.00, 30.00, NULL, 3000, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '仅限官方自营旗舰店商品使用'),
(5004, '官方自营店 满100打9折', 2, 1, 100.00, 0.00, 0.90, 2000, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, '仅限官方自营旗舰店商品，满100元享9折')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `type` = VALUES(`type`), `shop_id` = VALUES(`shop_id`), `threshold_amount` = VALUES(`threshold_amount`), `discount_amount` = VALUES(`discount_amount`), `discount_rate` = VALUES(`discount_rate`), `total_count` = VALUES(`total_count`), `per_user_limit` = VALUES(`per_user_limit`), `start_time` = VALUES(`start_time`), `end_time` = VALUES(`end_time`), `status` = VALUES(`status`), `description` = VALUES(`description`);

-- ==================== 7. 测试会员与默认收货地址（手机号 13800000000，短信验证码登录演示） ====================
INSERT INTO `user_member`
(`id`, `username`, `password`, `nickname`, `phone`, `gender`, `status`, `level`, `points`, `balance`) VALUES
(1, 'member13800000000', '$2b$10$H6pvr0bZCn4CC3bvQCYap.4VZsVldQ3O8r1H74usd0mlDPF5fOf1S', '测试会员', '13800000000', 1, 1, 1, 100, 10000.00)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `phone` = VALUES(`phone`), `status` = VALUES(`status`), `balance` = VALUES(`balance`);

INSERT INTO `user_address`
(`id`, `member_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `district`, `detail`, `zip_code`, `is_default`, `label`) VALUES
(1, 1, '测试会员', '13800000000', '广东省', '深圳市', '南山区', '科技园南区 Deep Street 1 号 A 座 1801', '518000', 1, '家')
ON DUPLICATE KEY UPDATE `receiver_name` = VALUES(`receiver_name`), `receiver_phone` = VALUES(`receiver_phone`), `province` = VALUES(`province`), `city` = VALUES(`city`), `district` = VALUES(`district`), `detail` = VALUES(`detail`), `is_default` = VALUES(`is_default`);
