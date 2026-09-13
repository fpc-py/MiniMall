<div align="center">
  <h1>🛒 cc-switch Mall</h1>
  <p><strong>单体可商用商城系统 · 全栈交易闭环</strong></p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-blue?logo=openjdk" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot" />
    <img src="https://img.shields.io/badge/Vue-3.4-%234fc08d?logo=vuedotninja" />
    <img src="https://img.shields.io/badge/MySQL-8.0-%234479a1?logo=mysql" />
    <img src="https://img.shields.io/badge/Redis-7-%23dc382d?logo=redis" />
    <img src="https://img.shields.io/badge/Last%20Commit-Sep%202026-blue" />
  </p>
</div>

---

## 📋 项目简介

一套完整的 **单体架构** 电商系统，覆盖 C 端移动端 H5 + B 端管理后台 + 平台后台。旨在用最简单的架构实现可商用的交易闭环，不引入微服务组件，一键 Docker Compose 即可启动全栈。

### 核心流程

```
注册/登录 → 浏览商品 → 加入购物车 → 下单锁库存 → 沙箱支付 → 回调扣库存出库 → 确认收货
```

---

## 🏗️ 技术栈

| 层级 | 技术 |
|---|---|
| **后端** | Java 21 · Spring Boot 3.2.x · MyBatis-Plus 3.5.x · Spring Security + JWT (jjwt 0.12) |
| **数据库** | MySQL 8.0 · Flyway 版本化迁移 · Redis 7 (Lua 原子库存) |
| **对象存储** | MinIO |
| **C 端 H5** | Vue 3 + Vite + Vant 4 + Pinia |
| **管理后台** | Vue 3 + Vite + Element Plus + Pinia |
| **部署** | Docker + Docker Compose (6 服务) |
| **工具库** | Hutool · Lombok · MapStruct · Knife4j (API 文档) |

---

## 📁 项目结构

```
mall-v1/
├── AGENTS.md                    # AI 开发规范
├── docker-compose.yml           # 全栈编排（6 服务）
├── nginx-admin.conf             # 管理后台 Nginx 配置
├── nginx-mobile.conf            # 移动端 H5 Nginx 配置
│
├── backend/                     # Spring Boot 单体后端
│   ├── Dockerfile               # 多阶段构建 (JDK21 → JRE21)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd          # Maven Wrapper
│   └── src/main/java/com/mall/
│       ├── common/              # 公共模块（R、异常、安全、配置）
│       ├── system/              # 用户体系（会员/商家/管理员/地址）
│       ├── product/             # 商品管理（SPU/SKU/分类）
│       ├── cart/                # 购物车（Redis 存储）
│       ├── order/               # 订单（状态机：10待付→20待发→30待收→40完成）
│       ├── stock/               # 库存（Redis Lua + DB 双保险）
│       └── pay/                 # 支付（沙箱模拟，幂等回调）
│
├── admin-web/                   # 管理后台（Element Plus）
│   └── src/views/
│       ├── Login.vue            # 管理员登录
│       ├── Dashboard.vue        # 仪表盘
│       ├── Category.vue         # 分类管理
│       ├── ProductSpu.vue       # 商品管理
│       ├── MerchantAudit.vue    # 商家审核
│       └── OrderManage.vue      # 订单管理
│
└── mobile-h5/                   # 移动端 H5（Vant 4）
    └── src/views/
        ├── Home.vue             # 首页
        ├── ProductDetail.vue    # 商品详情
        ├── Cart.vue             # 购物车
        ├── OrderConfirm.vue     # 下单确认
        ├── OrderList.vue        # 订单列表
        └── Payment.vue          # 支付页面
```

---

## 🚀 快速启动

### 前置要求

- Docker & Docker Compose
- Java 21（本地开发）
- Node.js 18+（前端开发）

### 一键启动全栈

```bash
# 克隆仓库
git clone https://github.com/your-username/mall-v1.git
cd mall-v1

# 构建前端
cd admin-web && npm install && npm run build && cd ..
cd mobile-h5 && npm install && npm run build && cd ..

# 构建后端
cd backend && ./mvnw package -DskipTests -B && cd ..

# 启动全部服务
docker compose up -d
```

启动后访问：

| 服务 | 地址 |
|---|---|
| 移动端 H5 | http://localhost:3002 |
| 管理后台 | http://localhost:3001 |
| 后端 API | http://localhost:8080/doc.html (Knife4j) |
| MinIO 控制台 | http://localhost:9001 |

### 本地开发

```bash
# 启动基础设施
docker compose up -d mysql redis minio

# 启动后端（backend/）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 启动管理后台（admin-web/）
npm run dev     # → http://localhost:3001

# 启动移动端（mobile-h5/）
npm run dev     # → http://localhost:3002
```

---

## 🧪 测试

```bash
cd backend
./mvnw test              # 运行全部测试（144+ 用例）
./mvnw test -Dtest=XxxTest  # 运行单个测试类
```

测试覆盖重点：
- **下单锁库存** — Redis Lua 原子预占 + DB `available>=?` 双保险，杜绝超卖
- **支付幂等** — 第三方交易号唯一索引，重复回调直接返回成功
- **金额计算** — `BigDecimal` 全程无浮点误差
- **资源归属** — 买家不能操作他人订单/商家不能越权发货

---

## 🧱 核心业务模块

### 用户体系
- 会员注册/登录（BCrypt 密码、JWT 无状态认证）
- 商家入驻申请 + 平台审核
- 管理员 RBAC 权限控制
- 收货地址管理

### 商品系统
- SPU / SKU 多规格商品模型
- 商品分类树
- 商品上下架管理
- 多图展示

### 购物车
- Redis 存储购物车数据
- 勾选 / 数量修改 / 删除
- 库存实时校验

### 订单系统
- 状态机：`05已取消 → 10待支付 → 20待发货 → 30待收货 → 40已完成`
- `confirm` 试算（生成防重复令牌） → `submit` 下单（Redis Lua + DB 双保险锁库存）
- 每次状态流转记录 `oms_order_status_log`

### 库存系统
- **Redis Lua 原子脚本** 预占库存
- **DB 乐观条件更新** `available_stock >= ?` 双保险
- 支付成功扣减冻结库存，超时关单自动释放
- 出库任务管理（商家发货触发）

### 支付系统
- 沙箱支付渠道（可替换为支付宝/微信生产实现）
- **幂等回调** — 交易号唯一索引
- 回调成功：订单 10→20 + 扣冻结库存 + 生成出库任务

---

## 📐 设计原则

- **金额一律 `BigDecimal`**，DB 用 `decimal(12,2)`，禁 `float/double`
- **统一返回体** `R<T>{code, msg, data}`，错误码 1xxxx 参数/2xxxx 认证/3xxxx 业务/5xxxx 服务端
- **数据库变更走 Flyway**，不手改已有 SQL
- **写操作必须 `@Transactional`**，事务方法不吞异常
- **资源归属校验** — 每个对外写接口都校验当前用户是否有权操作

---

## 📊 DB 表一览（Flyway V1__init.sql）

| 表名 | 说明 |
|---|---|
| `user_member` | 会员用户 |
| `sys_user` | 系统管理员 |
| `user_address` | 收货地址 |
| `user_shop` | 店铺 / 商家 |
| `pms_category` | 商品分类 |
| `pms_spu` | 商品 SPU |
| `pms_sku` | 商品 SKU（规格） |
| `pms_spu_image` | 商品图片 |
| `oms_order` | 订单主表 |
| `oms_order_item` | 订单项 |
| `oms_order_status_log` | 订单状态日志 |
| `wms_stock` | 库存 |
| `wms_stock_lock` | 库存锁定记录 |
| `wms_outbound_task` | 出库任务 |
| `pay_payment` | 支付记录 |

---

## 📄 License

MIT License — 可自由使用、修改和商用。