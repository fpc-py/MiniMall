# AGENTS.md — 单体可商用商城（v1.0）

## 项目目标
单体 Spring Boot 商城，交付完整交易闭环：注册登录 → 浏览商品 → 加购 → 下单锁库存 → 沙箱支付 → 回调扣库存出库 → 确认收货；含商家端与平台后台。v1.0 不引入微服务组件（Nacos/Gateway/RocketMQ/Seata/K8s 一律不用）。

## 技术栈（硬性，不要擅自替换）
- 后端：Java 21、Spring Boot 3.2.x、MyBatis-Plus 3.5.x、Spring Security + JWT(jjwt 0.12)、Flyway、Knife4j 4.x、Hutool、Lombok、MapStruct
- 存储：MySQL 8.0、Redis 7、MinIO
- 移动端：Vue 3 + Vite + Vant 4
- 管理后台：Vue 3 + Vite + Element Plus + Pinia
- 部署：Docker + Docker Compose

## 目录结构
```
mall-v1/
├── AGENTS.md
├── .codex/config.toml
├── docker-compose.yml
├── backend/                 # Spring Boot 单体，包结构按领域分包
│   └── src/main/java/com/mall/{common,system,product,cart,order,stock,pay}
│   └── src/main/resources/db/migration/   # Flyway 版本化 SQL
├── admin-web/               # 管理后台
└── mobile-h5/               # 移动端 H5
```



## 编码规范
- 金额一律 BigDecimal，DB 用 decimal(12,2)；禁止 float/double 表示钱。
- 时间用 java.time（LocalDateTime）；主键 Long；逻辑删除 deleted tinyint。
- 统一返回体 R<T>{code,msg,data}；全局异常处理；错误码分段 1xxxx 参数/2xxxx 认证/3xxxx 业务/5xxxx 服务端。
- 所有列表接口分页（pageNum/pageSize）；写操作必须 @Transactional，且事务方法不吞异常。
- 敏感密码用 BCrypt；JWT 不放敏感明文。
- 每个对外写接口都要校验「资源归属」（买家不能操作他人订单）。

## 铁律（违反必须重写）
1. 下单：锁库存 + 建订单必须在同一本地事务；Redis 预占用 Lua 原子脚本，DB 再用 `available>=?` 条件更新做双保险，杜绝超卖。
2. 支付回调必须幂等：第三方交易号加唯一索引，重复回调直接返回成功，不重复扣库存/不重复改状态。
3. 禁止字符串拼接 SQL；禁止把密钥/真实商户号提交进仓库（放 application-local.yml，已 gitignore）。
4. 数据库结构变更一律走 Flyway 新迁移脚本（V2__xxx.sql），不准手改已有 SQL。
5. 不引入本文件技术栈之外的新框架/中间件，除非先向我说明理由并获同意。

## 交付要求
- 每完成一个模块，自行运行：后端 `mvn -q compile`，前端 `npm run build`，编译/构建失败必须修到通过再停。
- 改完业务逻辑，补对应单元测试（重点：下单锁库存、支付幂等、金额计算）。
- 提交前自查：是否有越权、是否有金额精度问题、是否有硬编码密钥。

## 沟通方式
- 用简体中文回复。先简述你要做什么，再动手；不确定的设计取舍先问我，不要自行拍板架构。