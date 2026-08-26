# 新会话开工 Prompt（后端架构落地）

> 复制下方「--- PROMPT START ---」到「--- PROMPT END ---」整段，粘贴到新 Cursor 会话即可。  
> 工作区：`E:\aseantec\agent\Smart Fitness`（当前仅有 `docs/`，无 Java 代码）

---

## --- PROMPT START ---

你是 Smart Fitness 项目的后端实现工程师。请在当前工作区从零搭建 **Spring Boot 模块化单体** 后端，严格按已冻结设计文档施工，不要重新发明架构。

### 工作区与文档（必读顺序）

1. `docs/06-p0-kickoff.md` — 范围、验收、D1–D7 排期  
2. `docs/12-engineering-bootstrap.md` — Maven 模块、版本、Flyway、Compose  
3. `docs/07-auth-users.md` — `app_user`、JWT  
4. `docs/08-contracts.md` — JSON Schema、错误码、幂等、wellness  
5. `docs/09-readiness-guardrails.md` — 准备度 v1、护栏 G1–G5  
6. `docs/10-session-lifecycle.md` — 课次状态机  
7. `docs/11-catalog-seed.md` — 30 个动作种子  
8. `docs/02-backend-architecture.md` — 模块图、ER、API 面  
9. `docs/04-llm-usage-monitoring.md` — `llm_call_usage`、配额  
10. `docs/05-training-rag.md` — `KnowledgePort`、retrieve 契约  

**冲突时以 06–12 为准**，不得擅自改 JSON Schema / 错误码 / 准备度 v1 公式。

### 技术栈（冻结）

- Java 21、Spring Boot 3.3.5、Maven 多模块  
- PostgreSQL 16 + Flyway、Redis 7（Redisson）  
- MyBatis-Plus 3.5.9、LangChain4j（**禁止 Spring AI**）  
- SpringDoc、Lombok 允许  
- GroupId `com.aseantec`，包 `com.aseantec.smartfitness`  

### 模块（12 文档）

```
smart-fitness-api（唯一启动）
smart-fitness-common / -infra
smart-fitness-auth / -athlete / -readiness / -training / -knowledge / -coach / -admin
```

- Controller 主要在 `api`（扫描 `com.aseantec.smartfitness`）  
- 业务模块之间 **禁止** 互注 Mapper；`coach` 只调各模块 Service/Port  
- Flyway 仅在 `smart-fitness-api/src/main/resources/db/migration/`：  
  - `V1__init.sql`（含 02/04/07/08 全部表）  
  - `V2__catalog_seed.sql`（11 的 30 条）  
  - `V3__seed_policy_and_quota.sql`（默认 policy + 日配额 200k token）

### 四条红线

1. LLM 不直连 Mapper；全部经 `LlmGateway`  
2. 写库必须用户确认 Advice；`commit_advice` 不对模型暴露  
3. SSE 绑定 **runId**，禁止按 userId 广播  
4. 每次真实 LLM 调用写 `llm_call_usage`；无 usage 则 ESTIMATED，禁止 null  

### P0 必须实现的闭环

1. 注册/登录 → 建档（equipment/preferences/constraint）→ `PUT /v1/wellness/today`  
2. `POST /v1/coach/runs`（SSE）：Observe → `retrieve_exercise_options` → 护栏 → `advice`（含 `alternatives[]`）  
3. `POST /v1/advice/{id}/decide` accept → `session_log IN_PROGRESS` + `set_log`  
4. `PATCH` 记组 → `POST complete` → 重算 `athletic_state` calc_version=v1  
5. FakeLlm 可先跑通，但 `LlmGateway` + usage + 日配额检查必须存在  

### P0 明确不做（不要 scope creep）

- BYOK API（表可建，接口 501 或隐藏）  
- Admin Vue、向量 RAG、手表、身体成分流水、课 block、饮食、付费套餐  
- 脱离教练的纯记训入口  

### 必须先实现的接口

```java
interface LlmGateway { /* 流式 + 内部写 usage */ }
interface KnowledgePort { /* P0: CatalogKnowledgeAdapter SQL */ }
```

第一周：`FakeLlmGateway` + 固定 advice JSON；D7 再接 OpenAI 兼容真模型（`stream_options.include_usage=true`）。

### 实施顺序（按天，不要跳步）

- **D1**：父 POM、子模块空壳、`docker-compose`、V1–V3 Flyway、`GET /v1/health`、`.env.example`、`.gitignore`  
- **D2**：auth + athlete + wellness（07、08）  
- **D3**：catalog 种子 + KnowledgePort（11、05）  
- **D4**：session 状态机（10），可无 LLM  
- **D5–D6**：Coach Run SSE + FakeLlm + 护栏 G1–G5 + usage/配额（09、04）  
- **D7**：真模型适配器  

每完成一阶段：`mvn -pl smart-fitness-api -am package` 能通过，并简要汇报与 06 验收清单的对应项。

### 测试最低线

- 准备度金样 A–D（09）  
- decide 幂等  
- 肩痛约束下 advice 无 VERTICAL_PUSH 的 pick  
- 优先 Testcontainers PostgreSQL  

### 可读性（强制）

所有 Java 必须遵守 [15-backend-readability.md](15-backend-readability.md)：

- public 类：职责 + 对应表/API + 不变量
- public 方法：副作用 + 抛出的 `ErrorCode`（Lombok getter / Mapper CRUD 除外）
- Entity 业务字段：合法枚举值或单位，禁止裸字段
- 禁止复述代码的注释

### 交付物

1. 可 `docker compose up` + `mvn spring-boot:run` 的完整骨架  
2. SpringDoc 与 08 契约一致  
3. 代码达到 15 的可读性基准  
4. 不要 git commit，除非我明确要求  

请先阅读 `docs/06` 和 `docs/12`，输出你理解的模块依赖与 D1 文件清单，确认后开始创建 D1 工程骨架。

## --- PROMPT END ---
