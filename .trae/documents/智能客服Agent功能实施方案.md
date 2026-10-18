# 智能客服（Spring AI Agent）功能实施方案

> **修订说明（第 2 版）**：第 1 版用手写 `LlmClient` + Hutool `HttpUtil` 直连 OpenAI 兼容接口。用户否决并指定 **改用 Spring AI 框架实现**，同时选定：
> - 升级 Spring Boot 至 3.4.x，接入 **Spring AI 1.0.x**（用户已同意突破 AGENTS.md「Spring Boot 3.2.x」硬性约束）；
> - FAQ 知识库检索改用 **Spring AI 向量检索（RAG）**；
> - 其余不变：仅 C 端 H5、OpenAI 兼容协议 + 可配置、SSE 逐字流式、能力含 FAQ/订单售后/商品/转人工。

## 1. 摘要

为 `mall-v1` 新增**智能客服 Agent**：C 端 H5 用户自然语言提问，后端用 **Spring AI `ChatClient`** 驱动大模型，模型自主调用 5 个工具（FAQ 检索 / 订单查询 / 退款查询 / 商品检索 / 转人工），最终答案以 **SSE 逐字流式**回给前端。

关键点：**工具执行循环由 Spring AI 框架托管**（`ChatClient.prompt().tools(...).stream()` 会自动「调用工具 → 结果回流 → 继续流式输出」），本方案**不自己写 Agent 循环**。

**范围**：仅 C 端 `mobile-h5` + 后端。**不做**商家端/平台端客服工作台（转人工仅落库为待处理工单）。

## 2. 现状分析（已探明，均为实际文件）

| 关注点 | 现状 |
|---|---|
| 版本基线 | [pom.xml](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/pom.xml#L7-L29)：`spring-boot-starter-parent 3.2.5`、Java 21、MyBatis-Plus 3.5.7、Knife4j 4.5.0（内含 springdoc 2.2.0）、Hutool 5.8.32、jjwt 0.12.6、MapStruct 1.6.0、minio 8.5.10 |
| 后端分包 | `com.mall.{common,system,product,cart,order,stock,pay,marketing}`，分层 `controller/service/entity/mapper/dto/vo` |
| 统一返回体 | [R.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/R.java)：`R.ok(data)` / `R.fail(code,msg)` |
| 错误码 | [ErrorCode.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/ErrorCode.java)：1xxxx/2xxxx/3xxxx/5xxxx 四段；**3xxxx 段当前最大为 `COUPON_STATUS_INVALID(30033)`** |
| 异常 | [MallException.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/MallException.java) + [GlobalExceptionHandler.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/GlobalExceptionHandler.java) |
| 实体基类 | [BaseEntity.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/entity/BaseEntity.java)（createBy/updateBy/deleted/createTime/updateTime）+ [AuditMetaObjectHandler.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/AuditMetaObjectHandler.java)；MP `id-type: ASSIGN_ID`、逻辑删除 `deleted` |
| 登录人 | [SecurityUtils.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/security/SecurityUtils.java) + `@CurrentUser LoginUser`（[CurrentUserArgumentResolver.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/security/CurrentUserArgumentResolver.java)，未登录且 `required` 抛 20000） |
| 鉴权 | [SecurityConfig.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/security/SecurityConfig.java)：白名单放行 `/api/auth/**` 等，**`anyRequest().authenticated()`** → 新增 `/api/ai/**` 自动受登录保护，**无需改白名单** |
| Flyway | 最新 [V7__seed_catalog.sql](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/resources/db/migration/V7__seed_catalog.sql)；[V6__coupon.sql](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/resources/db/migration/V6__coupon.sql) 展示建表风格（`BIGINT id`、`TINYINT` 状态、`DECIMAL(12,2)`、审计列、`utf8mb4_unicode_ci`、中文 COMMENT、KEY 索引、`ON DUPLICATE KEY UPDATE` 幂等种子） |
| 配置 | [application.yml](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/resources/application.yml)：`mall.*` 段；`mall.sms.mock-enabled: true` 为「无真实通道也能联调」的既有先例 |
| 定时/异步 | 已有 `@Scheduled`（[OrderTimeoutJob.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/pay/job/OrderTimeoutJob.java)）；**无** `@Async`/线程池 Bean |
| 后端测试 | `backend/src/test/java/com/mall/**`，JUnit5 + Mockito + AssertJ；MP 实体需 `TableInfoHelper.initTableInfo` 预热（见 [CouponServiceTest.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/test/java/com/mall/marketing/service/CouponServiceTest.java#L52-L57)） |
| mobile-h5 | hash 路由 [router/index.js](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/router/index.js)（14 视图、`meta.requiresAuth`）；[request.js](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/utils/request.js) 注入 Bearer + `mall_token`；设计变量在 `src/styles/theme.css` |
| nginx | [nginx-mobile.conf](file:///d:/aafpc/Java/demo/aiMall/mall-v1/nginx-mobile.conf#L15-L21)：`location /api/` 反代 `http://backend:8080`，**未关缓冲**，SSE 必须单独加 location |
| 现有客服代码 | 无任何「客服/chat/conversation/LLM」相关代码或表，全新功能 |

## 3. 假设与关键决策

1. **框架放行（已获用户同意）**：AGENTS 铁律 5 禁止引入技术栈外框架。用户明确要求「SpringAI实现」，故本方案引入 **Spring AI**，并把 Spring Boot 由 3.2.5 升至 3.4.5。此为**用户显式同意的约束突破**，在方案与提交信息中均记录。
2. **版本组合（首选，坐标均已在 Maven Central 核实存在）**：
   - Spring Boot：`spring-boot-starter-parent` **3.4.5**（Spring AI 1.0.0 GA 官方对齐的 Boot 3.4.x 线）
   - Spring AI：`org.springframework.ai:spring-ai-bom` **1.0.0**（import scope），starter 用 `spring-ai-starter-model-openai`
   - MyBatis-Plus：`mybatis-plus-spring-boot3-starter` 3.5.7 → **3.5.9**
   - Knife4j：保留 4.5.0，但**显式覆盖 springdoc 到 2.7.0**（Knife4j 4.5.0 自带 springdoc 2.2.0 不支持 Boot 3.4；2.7.0 是该 Boot 线的官方适配版本，且早于 springdoc 2.8 的 `ControllerAdviceBean` 构造器变更，可避免 Knife4j 已知的 `NoSuchMethodError`）
   - Flyway：`flyway-core` + `flyway-mysql` 保持（Boot 3.4 管理的 Flyway 10.x 仍需 `flyway-mysql` 做 MySQL 8 支持，坐标已在 pom 中）
   - **回退规则（写入验证门禁）**：若启动期仍出现 Knife4j/springdoc 相关 `NoSuchMethodError`，则**移除 Knife4j 依赖**，改用官方 `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0`（Swagger UI 仍可用，仅失去 Knife4j 皮肤）；其余不变。
3. **不自己写 Agent 循环**：Spring AI 1.0.x 每个 `ChatModel` 实现内置 `ToolCallingManager` 工具执行循环，`stream()` 与 `call()` 对称走同一判定（`isToolExecutionRequired`）。因此用 `ChatClient.prompt(...).tools(...).stream().content()` 即可拿到「工具调用后的最终答案」的 `Flux<String>`。
4. **保持 Servlet 技术栈**：不引入 `spring-boot-starter-webflux`。Spring AI 会传递带入 `reactor-core`，在 Servlet 应用中订阅 `Flux` 并写入 `SseEmitter` 即可；若将来同时存在 web 与 webflux，Boot 判定规则是 **Spring MVC 优先（仍为 SERVLET）**，不会破坏现有 Security 配置。
5. **RAG 双路径（保证开箱可用）**：
   - 配了 Embedding（`spring.ai.openai.embedding.*` 有 key）：用 Spring AI `SimpleVectorStore`（内存，需显式引入 `org.springframework.ai:spring-ai-vector-store`）做 FAQ 语义检索；
   - 没配 Embedding：`spring.ai.model.embedding=none`，`FaqSearchTool` **自动降级为关键词 LIKE + 权重排序**（复用第 1 版 `AiFaqService` 的检索逻辑）。
   - 两条路径对上层工具与前端**完全透明**。
6. **Mock 降级（无 key 也能跑通全链路）**：
   - 默认 `application.yml` 里 **`spring.ai.model.chat=none`**（官方唯一关闭 chat 自动装配的开关；`spring.ai.openai.chat.enabled` 已被移除），避免无 api-key 时启动直接失败；
   - 同时注册自研 **`MockChatModel implements ChatModel`**（纯 Java，无外部调用）：实现 `ChatResponse call(Prompt)` 与 `Flux<ChatResponse> stream(Prompt)`，按关键词意图路由（订单/退款/商品/人工/FAQ）真实触发对应 `@Tool`，并把话术按字符切片产出以模拟逐字流；
   - `mock-enabled=false`（配了真实 key）时改由 Spring AI 自动装配的 OpenAI 兼容模型接管；两个 `ChatClient` Bean 用 `@ConditionalOnProperty("mall.ai.mock-enabled")` 互斥，避免 Bean 歧义。
7. **工具进度事件靠 `ToolContext` 传递**：Spring AI 的流式 API 不暴露「模型正在调用哪个工具」的中间事件。因为工具方法**由我们自己实现**，可在工具方法体内主动向前端推 `tool` 事件：Controller 把 `SseEmitter`（或一个 `AiProgressNotifier`）放进 `ToolContext`，工具方法通过 `toolContext.get(...)` 取出后发事件。这样既保留「正在查询订单…」的交互，也不需要伪造框架能力。
8. **SSE 事件协议简化**：保留 `meta` / `tool` / `delta` / `done` / `error`；**去掉 `reset`**（框架托管的工具循环不会先把正文流给前端再撤回，`reset` 已无必要）。
9. **密钥安全**：仓库内 `application.yml` 只留 `${MALL_AI_API_KEY:}` 占位；真实 key 写 `application-local.yml`（已 gitignore）或环境变量，符合铁律 3。
10. **归属与安全**：所有工具在服务端执行，`memberId` **一律取自 JWT 并经 `ToolContext` 传入**（禁止信任前端）；订单/退款查询必须校验归属，他人数据返回「未查询到」而非报错，避免信息泄露。
11. **表名前缀 `ai_`**：`ai_conversation` / `ai_message` / `ai_faq` / `ai_handoff`，沿用领域前缀约定。
12. **数据库变更走 Flyway**：新增 `V8__ai_customer_service.sql`，不改既有脚本（铁律 4）。
13. **仅新增、不改既有**：路由表、`Home.vue`、`Profile.vue`、`nginx-mobile.conf` 只做**追加**，不改动既有 path/参数/业务逻辑与交互流程。
14. **历史上下文**：会话历史仍由我们自己从 `ai_message` 读取并组装为 Spring AI `UserMessage`/`AssistantMessage` 列表（不用框架 `ChatMemory` Advisor），保持落库口径与第 1 版一致。
15. **不做**：商家/平台客服工作台、后台 FAQ 管理界面、多模态客服、跨会话共享上下文、消息推送/未读计数、token 计费与成本统计。

## 4. 总体设计

### 4.1 请求链路

```
mobile-h5 CustomerService.vue
  └─ fetch POST /api/ai/chat/stream   (Bearer mall_token, body: { conversationId?, content })
        └─ nginx (精确匹配 location，proxy_buffering off)
              └─ AiChatController → SseEmitter（独立线程池执行）
                    └─ AiChatService
                          ├─ 装配历史 → ChatClient.prompt().messages(...).tools(5个工具).stream().content()
                          ├─ Spring AI 内部：模型请求 ⇒ 命中工具 ⇒ ToolCallingManager 执行 @Tool ⇒ 结果回流 ⇒ 继续流式
                          ├─ 工具方法内经 ToolContext 回推 tool 事件
                          └─ 每个 delta 写 SseEmitter；结束后落库 ai_message / ai_conversation，发 done
```

### 4.2 Spring AI 集成方式

- `ChatClient.Builder` 由我们自己的 `AiChatClientConfig` 构造（**不依赖自动装配的 Builder**，规避 mock/real 两种模式的 Bean 歧义）：
  - `mock-enabled=true`：`ChatClient.builder(mockChatModel).defaultSystem(系统提示词).build()`
  - `mock-enabled=false`：`ChatClient.builder(openAiChatModel).defaultSystem(系统提示词).build()`（`openAiChatModel` 由 Spring AI 自动装配注入，`spring.ai.model.chat=openai` 由 `application-local.yml` 打开）
- 系统提示词（中文）说明客服身份、可用工具的使用时机、语气与安全边界（不承诺无法确认的信息、不透露他人数据、涉及纠纷引导转人工）。
- 工具通过 `@Tool` 注解声明，类由 `@Component` 注册，调用时 `.tools(orderTools, refundTools, productTools, faqTools, handoffTools)` 传入实例。

### 4.3 Agent 工具（`@Tool`，服务端执行）

| 工具名（`@Tool(name=...)`） | 参数 | 行为 | 归属校验 | 进度事件 |
|---|---|---|---|---|
| `search_faq` | `keyword` | 有 Embedding：`VectorStore.similaritySearch(keyword, topK=3)`；否则 `ai_faq` 按 question/keywords `LIKE` + 命中权重排序取 TopN | 不涉及 | 否 |
| `query_order` | `orderNo?`、`status?` | 有 orderNo 查单条详情（状态/金额/时间）；无则按状态列最近 5 单 | **必须** `member_id = ToolContext.userId` | 是 |
| `query_refund` | `orderNo?` | 查退款单进度与审核结果 | **必须** 归属当前用户订单 | 是 |
| `search_product` | `keyword`、`limit?` | 复用 SPU/SKU 查询，返回卡片字段（id/name/mainImage/minPrice） | 不涉及 | 是 |
| `handoff_to_human` | `reason`、`contact?` | 写 `ai_handoff` 工单 + 会话置 `status=1`，同事务；返回安抚话术 | 不涉及 | 是 |

`ToolContext` 约定的 key：`userId`（Long）、`conversationId`（Long）、`notifier`（`AiProgressNotifier` 函数式接口，用于推 `tool` 事件）。

### 4.4 SSE 事件协议（`text/event-stream`，每帧 `data: {json}\n\n`）

| type | 载荷 | 说明 |
|---|---|---|
| `meta` | `conversationId`,`messageId` | 首帧下发，前端回填并用于历史落库 |
| `tool` | `name`,`status`(running/done) | 工具方法内部触发，前端显示「正在查询订单…」 |
| `delta` | `content` | 增量文本，逐字追加 |
| `done` | `messageId`,`handoff` | 结束标记（`handoff=true` 表示已转人工） |
| `error` | `msg` | 失败提示，发送后关闭连接 |

### 4.5 配置项（`application.yml`）

```yaml
spring:
  ai:
    model:
      chat: none          # 默认关闭 chat 自动装配（无 key 时避免启动失败），由 mall.ai.mock-enabled 决定是否在 local 覆盖为 openai
      embedding: none     # 同上；配了 embedding key 后在 application-local.yml 改为 openai
    openai:
      api-key: ${MALL_AI_API_KEY:}
      chat:
        base-url: ${MALL_AI_BASE_URL:https://api.deepseek.com/v1}
        options:
          model: ${MALL_AI_MODEL:deepseek-chat}
      embedding:                     # 仅在做 RAG 时配置（DeepSeek 无 embedding 接口，可指向 DashScope/OpenAI 兼容端点）
        base-url: ${MALL_AI_EMBEDDING_BASE_URL:}
        api-key: ${MALL_AI_EMBEDDING_API_KEY:}
        options:
          model: ${MALL_AI_EMBEDDING_MODEL:}

mall:
  ai:
    mock-enabled: true        # 无 key 时降级自研 MockChatModel，保证开箱可用；配真实 key 后置 false
    timeout-seconds: 60
    history-limit: 20
    faq-top-k: 3
```

> `spring.ai.model.chat` / `spring.ai.model.embedding` 的实际取值由 `application-local.yml` 覆盖（`mock-enabled=false` 时改为 `openai`），避免"默认值随开关变化"需要 profile 之外的手段。

## 5. 具体改动清单

### 5.0 前置：先提交上一轮未提交的修复

当前 `git status` 有 4 个未提交文件，均为上一轮已完成并实测通过的修复，**先单独提交**（中文 message），再开始本功能：

- [docker-compose.yml](file:///d:/aafpc/Java/demo/aiMall/mall-v1/docker-compose.yml)：MySQL 宿主端口 3308、backend 8088、**MySQL 时区 `TZ=Asia/Shanghai` + `--default-time-zone=+08:00`**（修复超时关单误杀新订单）
- [Cart.vue](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/views/Cart.vue)：结算栏被 TabBar 遮挡导致无法点击
- [Payment.vue](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/views/Payment.vue) / [RefundApply.vue](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/views/RefundApply.vue)：雪花 ID 被 `Number()` 截断导致「订单不存在」

### 5.1 `backend/pom.xml` 依赖升级

| 变更 | 目标值 | 理由 |
|---|---|---|
| `spring-boot-starter-parent` | 3.2.5 → **3.4.5** | Spring AI 1.0.0 官方对齐线 |
| 新增 `spring-ai-bom`（`dependencyManagement`，import） | **1.0.0** | 统一 Spring AI 各模块版本 |
| 新增 `spring-ai-starter-model-openai` | 随 BOM | OpenAI 兼容 chat + embedding + `ChatClient` 自动配置 |
| 新增 `spring-ai-vector-store` | 随 BOM | 提供 `SimpleVectorStore`（内存向量库，无需新中间件） |
| `mybatis-plus.version` | 3.5.7 → **3.5.9** | 适配 Boot 3.4 |
| 新增 `springdoc.version` = **2.7.0** + `dependencyManagement` 覆盖 `springdoc-openapi-starter-webmvc-ui` | — | Knife4j 4.5.0 自带 2.2.0 不兼容 Boot 3.4 |
| Knife4j | 保留 4.5.0 | 若启动报 Knife4j 相关 `NoSuchMethodError` → 按 §3.2 回退规则移除，改用 springdoc 2.7.0 |

### 5.2 后端新增（包 `com.mall.ai`）

| 文件 | 职责与要点 |
|---|---|
| `config/AiProperties.java` | `@ConfigurationProperties("mall.ai")`：`mockEnabled`/`timeoutSeconds`/`historyLimit`/`faqTopK`；仿 [SmsProperties.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/system/sms/SmsProperties.java) 写法（`@Data @Component`） |
| `config/AiChatClientConfig.java` | 两个互斥的 `ChatClient` Bean（`@ConditionalOnProperty("mall.ai.mock-enabled")` true/false），含中文 System Prompt |
| `config/AiExecutorConfig.java` | `@Bean("aiExecutor") ExecutorService`（`Executors.newFixedThreadPool(8)`，JDK 原生）供 SSE 后台执行 |
| `llm/MockChatModel.java` | `implements ChatModel`（`call(Prompt)` + `stream(Prompt)`）：关键词意图路由 → 触发对应 `@Tool` 或返回 FAQ 兜底话术；答案按字符切片产出 `Flux<ChatResponse>` 模拟逐字流 |
| `llm/MockToolCalling.java` | （包内辅助）Mock 模式下把「意图」映射为「工具调用参数」，与真实 `@Tool` 共用同一批 Tool 实例，保证两条路径行为一致 |
| `tool/AiToolContextKeys.java` | `ToolContext` 的 key 常量：`userId`/`conversationId`/`notifier` |
| `tool/AiProgressNotifier.java` | 函数式接口 `void emit(String toolName, String status)`，由 Controller 实现为「往 SseEmitter 发 `tool` 事件」 |
| `tool/OrderAgentTools.java` | `@Tool(name="query_order")`；复用 `OrderMapper`/`OrderItemMapper`；**强制 `member_id` 归属校验** |
| `tool/RefundAgentTools.java` | `@Tool(name="query_refund")`；复用 `RefundMapper`；强制归属校验 |
| `tool/ProductAgentTools.java` | `@Tool(name="search_product")`；复用 `SpuMapper`/`SkuMapper`，返回卡片字段 |
| `tool/FaqAgentTools.java` | `@Tool(name="search_faq")`；注入 `Optional<VectorStore>`：有则 `similaritySearch`，无则调 `AiFaqService.searchByKeyword` |
| `tool/HandoffAgentTools.java` | `@Tool(name="handoff_to_human")`；写 `ai_handoff` + 更新会话 `status=1`，`@Transactional` |
| `service/AiFaqService.java` | 关键词检索（question/keywords 双字段 LIKE + 命中长度加权 TopN）+ `VectorStore` 冷启动索引：应用就绪后把 `ai_faq` 全量装配为 `Document`（`id`/`question`/`answer` 入 metadata）写入向量库；Embedding 未配置时跳过 |
| `service/AiChatService.java` | 会话与历史装配、调用 `ChatClient` 流式、SSE 编排、消息/会话落库；落库与转人工用 `@Transactional`；**不吞异常** |
| `entity/AiConversation.java` `AiMessage.java` `AiFaq.java` `AiHandoff.java` | 继承审计字段约定，`@TableName("ai_xxx")` |
| `mapper/AiConversationMapper.java` `AiMessageMapper.java` `AiFaqMapper.java` `AiHandoffMapper.java` | `extends BaseMapper<T>` |
| `dto/AiChatRequest.java` | `{ conversationId?, content }`，`content` 非空 + 长度上限（`@Valid`） |
| `vo/AiMessageVO.java` `AiConversationVO.java` | 历史消息/会话出参（`Long` 由全局 Jackson 序列化为字符串） |
| `controller/AiChatController.java` | `@RequestMapping("/api/ai")`，全部 `@CurrentUser LoginUser` 注入 |

**接口清单**（均需登录，`memberId` 取自 Token）：

- `POST /api/ai/chat/stream` → `SseEmitter`（`produces = text/event-stream`，timeout 120s，提交到 `aiExecutor` 执行）
- `GET /api/ai/conversations` → `R<List<AiConversationVO>>`（我的最近会话）
- `GET /api/ai/conversations/{id}/messages` → `R<List<AiMessageVO>>`（**归属校验**，非本人抛 `MallException`）
- `GET /api/ai/faq/hot` → `R<List<String>>`（快捷提问 chips）

### 5.3 数据库迁移

`backend/src/main/resources/db/migration/V8__ai_customer_service.sql`（不改既有脚本）：

- `ai_faq`：`id`,`question`,`answer`,`keywords`,`category`,`sort`,`status`,审计列，`KEY idx_status`
- `ai_conversation`：`id`,`member_id`,`title`,`status`(0进行中/1已转人工/2已结束),`last_message_time`,`message_count`,审计列，`KEY idx_member_id`
- `ai_message`：`id`,`conversation_id`,`member_id`,`role`(1用户/2客服),`content`,审计列，`KEY idx_conversation_id`
- `ai_handoff`：`id`,`conversation_id`,`member_id`,`reason`,`contact`,`status`(0待处理/1已处理),审计列，`KEY idx_status`
- 种子：8~10 条 `ai_faq`（退换货政策、发货时效、运费、发票、优惠券、退款到账、联系人工等），`ON DUPLICATE KEY UPDATE` 幂等（对齐 V7 风格）

### 5.4 通用层小改（仅新增常量）

[ErrorCode.java](file:///d:/aafpc/Java/demo/aiMall/mall-v1/backend/src/main/java/com/mall/common/ErrorCode.java) 的 `Biz` 枚举末尾（当前最大 `COUPON_STATUS_INVALID(30033)`）追加 4 个常量：

- `AI_SERVICE_UNAVAILABLE(30034, "智能客服暂时不可用，请稍后再试")`
- `AI_CONVERSATION_NOT_FOUND(30035, "会话不存在")`
- `AI_CONVERSATION_FORBIDDEN(30036, "无权访问该会话")`
- `AI_REQUEST_INVALID(30037, "提问内容不合法")`

### 5.5 前端 mobile-h5（只新增，不改既有路由/接口/交互）

| 文件 | 变更 |
|---|---|
| `src/api/ai.js`（新增） | `chatStream(payload, handlers, signal)`：`fetch('/api/ai/chat/stream')` + `Authorization: Bearer ${getToken()}` + `TextDecoder` 解析 `\n\n` 分帧 SSE；`getConversations()`/`getMessages(id)`/`getHotFaq()` 走既有 [request.js](file:///d:/aafpc/Java/demo/aiMall/mall-v1/mobile-h5/src/utils/request.js) |
| `src/views/CustomerService.vue`（新增） | 会话页：`van-nav-bar`「智能客服」、消息气泡（用户右/客服左）、`tool` 事件显示「正在查询订单…」、`delta` 逐字追加、热门问题 chips、商品卡片、输入框+发送、转人工入口、`onUnmounted` 时 `AbortController.abort()` |
| `src/router/index.js`（追加 1 条） | `{ path:'/customer-service', name:'CustomerService', component: () => import('@/views/CustomerService.vue'), meta:{ requiresAuth:true } }` |
| `src/views/Home.vue`（追加 1 个悬浮按钮） | 右下角「客服」悬浮入口，`$router.push('/customer-service')` |
| `src/views/Profile.vue`（追加 1 行） | 菜单新增「在线客服」项 |

样式一律复用 `theme.css` 既有变量与 class（暗夜霓虹 + 玻璃拟态），不新增设计 token、不新增 npm 依赖。

### 5.6 nginx（`nginx-mobile.conf`，追加一个 location）

现有 `location /api/`（[nginx-mobile.conf](file:///d:/aafpc/Java/demo/aiMall/mall-v1/nginx-mobile.conf#L15-L21)）未关缓冲，会破坏逐字效果。**新增精确匹配 location**（前缀更长者优先，无需改动既有块）：

```nginx
# SSE 逐字输出必须关闭反代缓冲；精确匹配，避免影响其他 /api 请求
location = /api/ai/chat/stream {
    proxy_pass http://backend:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 300s;
}
```

## 6. 实施顺序

1. 提交 §5.0 的上一轮修复（1 个 commit）
2. **依赖升级门禁**：改 `pom.xml`（§5.1）→ `mvn -q compile` + 本地启动后端，确认 Boot 3.4.5 + MyBatis-Plus 3.5.9 + Knife4j/springdoc 2.7.0 均正常；**若 Knife4j 报错则按 §3.2 回退规则处理后再继续**（此步必须通过才进入下一步）
3. `V8` 迁移 + 四个实体/Mapper
4. `AiProperties` + `application.yml` 配置段 + `ErrorCode` 常量
5. `ChatClient` 装配（`AiChatClientConfig`）+ `MockChatModel`
6. 五个 `@Tool` 工具 + `AiFaqService`（含 `VectorStore` 冷启动索引与关键词降级）
7. `AiChatService` + `AiChatController`（SSE + `ToolContext` 进度回推）
8. 后端单测
9. `mobile-h5` api + 视图 + 路由 + 入口 + nginx
10. 构建与实测，最后 1 个 commit

## 7. 验证步骤

1. **编译/测试**：`mvn -q compile`、`mvn -q test` 全绿（含新增用例）；`mobile-h5` 执行 `npx vite build` 成功。
2. **依赖门禁复核**：后端启动日志无 `NoSuchMethodError`/`ClassNotFoundException`；`/doc.html`（Knife4j）或 `/swagger-ui.html` 可正常打开。
3. **重建容器**：`docker compose up -d --build backend`；因 nginx 变更需 `docker compose up -d --force-recreate mobile-h5`；确认 Flyway 落到 `v8`：
   `SELECT version,success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;`
   `SELECT COUNT(*) FROM ai_faq;`
4. **mock 模式实测**（无 key，`mall.ai.mock-enabled=true`）：浏览器 `http://localhost:3002/#/customer-service` 登录后依次问
   - 「你们支持七天无理由退货吗」→ 命中 FAQ 工具
   - 「我的订单到哪了」→ 触发 `query_order`，返回本人订单状态
   - 「有便宜的手机吗」→ 触发 `search_product`，渲染商品卡片
   - 「转人工」→ 生成 `ai_handoff` 且会话 `status=1`
   核对：逐字流式、`tool` 事件提示、`ai_message` 落库（user/assistant 各 1 条）、`ai_handoff` 1 条。
5. **归属校验取证**：构造他人 `orderNo` 提问，确认工具返回「未查询到」而非他人数据；直接 `GET /api/ai/conversations/{他人id}/messages` 返回 3xxxx 越权错误。
6. **真实模型实测**：`application-local.yml` 填 `mall.ai.mock-enabled=false` + `spring.ai.model.chat=openai` + `api-key`，重启后端，重复第 4 步，确认真实 LLM 的工具调用与逐字输出；console 无 `net::` 资源错误、SSE 连接成功。
7. **（可选）RAG 实测**：配 `spring.ai.openai.embedding.*` + `spring.ai.model.embedding=openai` 后重启，确认启动日志出现 FAQ 向量索引条数，且「退货政策」类问题由向量检索命中。
8. 收尾：`git commit`（中文 message，只提交本功能相关文件）。

## 8. 风险与不做的事

- **依赖升级风险（最高）**：Boot 3.2 → 3.4 可能连带 MyBatis-Plus / Knife4j / springdoc / Flyway 兼容问题。已在 §6 第 2 步设**强制门禁**，并给出 Knife4j 回退规则；门禁不过不推进。
- **Spring AI 1.0.x 的流式工具循环**：官方文档无专门「streaming 工具循环」章节，结论来自官方博客（1.x 每个 ChatModel 内含私有工具执行循环）+ `OpenAiChatModel` 源码中 `internalStream` 与阻塞路径对称的 `isToolExecutionRequired` 判定。**若实测发现流式下工具未被自动执行**，回退方案：改用 `ChatClient.prompt().tools(...).call()` 逐轮非流式拿工具结果、最后一轮再 `.stream()` 输出（保留逐字观感）。
- **外部依赖可用性**：LLM 不可用时 `AiChatService` 需捕获并回 `error` 事件 + 友好话术，不阻塞连接、不抛 500。此处为**非事务**的流式分支，允许捕获后转 SSE error（铁律「事务方法不吞异常」不适用于此）。
- **Embedding 与 Chat 供应商可能不同**：DeepSeek 无 Embedding 接口，RAG 需另配兼容端点（如 DashScope）；未配则自动走关键词降级，功能不受影响。
- **越权**：所有涉及订单/退款/会话的工具与接口必须做归属校验。
- **不做**：向量库持久化与生产级选型（`SimpleVectorStore` 为内存实现，官方标注非生产级，仅满足本期演示/可用）、后台 FAQ 与客服工作台、多模态客服、消息推送与未读计数、跨会话共享上下文。

---

## 9. 实施结果（已落地，与实际代码对齐）

本方案已全部实施完成，最终落地情况与本文档的差异如下（其余按原设计执行）：

1. **依赖回退规则已触发**：Knife4j 4.5.0 在 Boot 3.4 下仍报兼容问题，按 §3.2 回退规则**移除 Knife4j**，改用官方 `springdoc-openapi-starter-webmvc-ui`；API 文档入口由 `/doc.html` 变为 **`/swagger-ui.html`**。最终版本：Spring Boot **3.4.5** + Spring AI **1.0.0** + MyBatis-Plus **3.5.9**。
2. **交付文件**：`backend` 新增 `com.mall.ai` 包（`AiChatController`、`AiChatService`、`AiChatClientConfig`、`MockChatModel`、`AiFaqService`、5 个 `@Tool` 工具、4 个实体 + Mapper、`AiProperties`）；`V8__ai_customer_service.sql` 建 4 张表 + FAQ 种子；`mobile-h5` 新增 `src/api/ai.js`、`src/views/CustomerService.vue`、路由 `/customer-service`，并在 `Home.vue` / `Profile.vue` 追加入口；`nginx-mobile.conf` 追加 SSE 精确 location。
3. **接口实际路径**：`POST /api/ai/chat/stream`（SSE）、`GET /api/ai/conversations`、`GET /api/ai/conversations/{conversationId}/messages`、`GET /api/ai/faq/hot`。
4. **额外修复（方案外发现）**：`JwtAuthenticationFilter` 继承 `OncePerRequestFilter` 时默认不处理 ASYNC 派发，导致 `SseEmitter.complete()` 触发的异步派发阶段 `SecurityContext` 为空、被 Spring Security 6 的 `AuthorizationFilter` 判为 403 而中断连接（前端表现为 `ERR_ABORTED`）。已在 `SecurityConfig` 放行 ASYNC 派发修复。
5. **验证结果**：`mvn test` 全部 **267** 个用例通过；浏览器实测移动端客服页通过——快捷提问 chips 正常、SSE 逐字回复正常、转人工提示正常、输入框可连续发送。`mall.ai.mock-enabled=true` 下**无需真实 API Key 即可开箱使用**。
