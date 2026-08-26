# LLM Token 用量监控

> **P0 必做。** 现状：无完整监控。旧 Training Log 仅 Debug 控制台偶发读取 usage，Copilot 主链路把 `promptTokens/completionTokens` 写成 `null`。新设计里原先只有 `coach_run.token_in/out` 两个汇总值，不够支撑系统额度与用户自带 Key（BYOK）。  
> 日期：2026-08-23

---

## 1. 结论与原则

| 谁付钱 | Key 来源 `key_source` | 平台是否记账 | 是否扣系统额度 |
|--------|----------------------|--------------|----------------|
| 平台 | `SYSTEM` | 必须 | 必须（日/月配额） |
| 用户自己的供应商账单 | `BYOK` | **仍然必须记账** | 不扣平台额度；仍限流、仍可告警 |

BYOK **不等于不监控**。用户 Key 也要记每一次调用：谁、哪个模型、多少 token、耗时、成败。否则无法排查滥用、无法对比策略版本成本、无法在 Key 失效时定位。

红线：

1. **一次真实 LLM HTTP 调用 = 一行 `llm_call_usage`**。禁止只写 Run 汇总。  
2. 流式结束必须拿 usage；拿不到则用分词器估算，并标 `usage_source = ESTIMATED`，**禁止静默丢弃**。  
3. 原始 API Key **永不落库明文、不进日志、不进 Trace payload**。只存加密密文 + `key_fingerprint`（SHA-256 前 12 位）。  
4. 调用前检查配额（SYSTEM）与速率（SYSTEM + BYOK）。超限返回业务错误，不打到供应商。

---

## 2. 计量粒度

一次 Coach Run 里通常有 **多次** LLM 调用（主对话、工具循环中的每一跳、可选的标题生成）。Run 上的 `token_in/out` 只是汇总，账单与排障看 Call 表。

```text
Coach Run
  ├─ llm_call #1  propose / chat     SYSTEM or BYOK
  ├─ llm_call #2  工具循环第 2 跳
  └─ llm_call #3  （可选）生成对话标题
         │
         ▼
  异步累加 → llm_usage_daily（按 athlete × 日 × key_source）
         │
         ▼
  超阈值 → usage_alert（80% / 100% 系统额度）
```

LangChain4j：从 `TokenStream.onComplete` / `ChatResponse.tokenUsage()` 取值。OpenAI 兼容流必须开 `stream_options.include_usage = true`，否则最终 chunk 没有 usage。

---

## 3. 表结构

### 3.1 `llm_credential`（用户 BYOK + 系统默认）

```sql
CREATE TABLE llm_credential (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT,                    -- NULL = 系统级
    provider        VARCHAR(32) NOT NULL,      -- openai / deepseek / dashscope / ...
    base_url        VARCHAR(256) NOT NULL,
    model           VARCHAR(64) NOT NULL,
    api_key_cipher  BYTEA NOT NULL,            -- AES-GCM / KMS，非明文
    key_fingerprint VARCHAR(16) NOT NULL,      -- 用于排障，不可逆推 Key
    status          VARCHAR(16) NOT NULL,      -- active / disabled / invalid
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cred_athlete ON llm_credential (athlete_id) WHERE athlete_id IS NOT NULL;
```

解析顺序：用户 `active` BYOK → 否则系统默认 credential。

### 3.2 `llm_call_usage`（事实表，P0）

```sql
CREATE TABLE llm_call_usage (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    run_id              BIGINT,                -- 可空：非 Run 的后台调用也要记
    call_seq            INTEGER NOT NULL DEFAULT 1,
    purpose             VARCHAR(32) NOT NULL,  -- chat / tool_loop / title / embedding
    key_source          VARCHAR(16) NOT NULL,  -- SYSTEM / BYOK
    billed_to           VARCHAR(16) NOT NULL,  -- PLATFORM / USER
    credential_id       BIGINT,
    key_fingerprint     VARCHAR(16),
    provider            VARCHAR(32) NOT NULL,
    model               VARCHAR(64) NOT NULL,
    prompt_tokens       INTEGER NOT NULL DEFAULT 0,
    completion_tokens   INTEGER NOT NULL DEFAULT 0,
    cached_tokens       INTEGER NOT NULL DEFAULT 0,
    reasoning_tokens    INTEGER NOT NULL DEFAULT 0,  -- o-series / 思考模型
    total_tokens        INTEGER NOT NULL DEFAULT 0,
    usage_source        VARCHAR(16) NOT NULL,  -- PROVIDER_REPORT / ESTIMATED
    estimated_cost_minor INTEGER NOT NULL DEFAULT 0, -- 分；BYOK 仍按价目估算「若走平台」便于对比
    currency            CHAR(3) NOT NULL DEFAULT 'CNY',
    duration_ms         INTEGER,
    http_status         INTEGER,
    status              VARCHAR(16) NOT NULL,  -- SUCCESS / FAILED / TIMEOUT / QUOTA_BLOCKED
    error_code          VARCHAR(64),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_usage_athlete_time ON llm_call_usage (athlete_id, created_at DESC);
CREATE INDEX idx_usage_run ON llm_call_usage (run_id);
CREATE INDEX idx_usage_day_source ON llm_call_usage (created_at, key_source);
```

`estimated_cost_minor`：用 `llm_price_list` 按模型计价。BYOK 的 `billed_to = USER`，成本列仍填估算值，报表要分「平台实付」与「用户侧估算」。

### 3.3 `llm_usage_daily`（汇总，给仪表盘）

```sql
CREATE TABLE llm_usage_daily (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    usage_date          DATE NOT NULL,
    key_source          VARCHAR(16) NOT NULL,
    call_count          INTEGER NOT NULL DEFAULT 0,
    fail_count          INTEGER NOT NULL DEFAULT 0,
    prompt_tokens       BIGINT NOT NULL DEFAULT 0,
    completion_tokens   BIGINT NOT NULL DEFAULT 0,
    total_tokens        BIGINT NOT NULL DEFAULT 0,
    platform_cost_minor INTEGER NOT NULL DEFAULT 0, -- 仅 SYSTEM 计入平台实付
    byok_est_cost_minor INTEGER NOT NULL DEFAULT 0,
    UNIQUE (athlete_id, usage_date, key_source)
);
```

写入：调用成功/失败后 **同步写事实表**，再异步 upsert 日表（Redis 计数做热路径配额，日表做对账）。

### 3.4 `llm_quota` 与价目

```sql
CREATE TABLE llm_quota (
    id              BIGINT PRIMARY KEY,
    athlete_id      BIGINT,                -- NULL = 全局默认
    period          VARCHAR(16) NOT NULL,  -- DAY / MONTH
    token_limit     BIGINT,                -- 仅约束 SYSTEM
    cost_limit_minor INTEGER,              -- 可选，按金额封顶
    rpm_limit       INTEGER NOT NULL DEFAULT 20,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE llm_price_list (
    id              BIGINT PRIMARY KEY,
    provider        VARCHAR(32) NOT NULL,
    model           VARCHAR(64) NOT NULL,
    input_per_1k_minor  INTEGER NOT NULL,
    output_per_1k_minor INTEGER NOT NULL,
    cached_per_1k_minor INTEGER NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL DEFAULT 'CNY',
    effective_from  DATE NOT NULL,
    UNIQUE (provider, model, effective_from)
);
```

配额解析：运动员专属 → 全局默认。P0 建议全局：`DAY token_limit`（例如 200_000）+ `rpm_limit`。

### 3.5 Redis 热计数（配额快路径）

```text
usage:sys:{athleteId}:{yyyyMMdd}   → INCRBY total_tokens   TTL 48h
usage:rpm:{athleteId}              → INCR 滑动窗口
```

DB 是账本；Redis 是拦截。对账任务每日把 Redis 与 `llm_usage_daily` 比对，偏差告警。

---

## 4. 调用拦截（必须经过）

所有 LangChain4j 调用走唯一 `LlmGateway`：

```text
1. 解析 credential（BYOK active 优先，否则 SYSTEM）
2. RPM 检查（两种 Key 都查）
3. 若 SYSTEM：读 Redis 日用量，≥ token_limit → 不发请求，写 usage status=QUOTA_BLOCKED
4. 发起流式/非流式请求（stream 带 include_usage）
5. 结束：解析 TokenUsage；缺则 ESTIMATED
6. INSERT llm_call_usage；更新 coach_run 汇总；INCR Redis；upsert daily
7. SYSTEM 达 80% / 100% → 写告警，100% 下次直接拦截
```

禁止业务 Service 自己 `new OpenAiChatModel()`。

---

## 5. App 与 Admin 呈现

### App「我的 → 用量」（P0）

| 区块 | 内容 |
|------|------|
| 本月系统额度 | 已用 token / 上限；进度条；超限文案「今日教练次数已用完，可绑定自己的 API Key」 |
| BYOK | 已绑定 provider + 脱敏 fingerprint；本月调用次数与 token（**不展示用户供应商账单金额为「已扣费」**，只标「走你的 Key，费用以供应商为准」） |
| 近 7 日 | 按日条形：SYSTEM vs BYOK |

接口：`GET /v1/me/usage?period=month`

绑定 Key：`PUT /v1/me/llm-credential`（只收 Key 一次，回包永不带出）。校验：发一条极短 ping，失败标 `invalid`。

### Admin「Token 监控」（P0 独立菜单，不是只在总览放「均 Token」）

| 能力 | 说明 |
|------|------|
| 筛选 | 日期、key_source、provider、model、athlete |
| KPI | 平台实付、SYSTEM token、BYOK token、拦截次数、ESTIMATED 占比 |
| 排行 | 运动员 SYSTEM 消耗 Top N |
| 告警 | 80%/100% 额度、ESTIMATED > 5%、供应商 4xx/5xx 突增 |
| 下钻 | 点运动员 → 调用列表 → 关联 runId / Trace |
| 价目 | 维护 `llm_price_list`（owner） |

`operator` 只读；`owner` 改全局配额与价目。

---

## 6. API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/me/usage` | App 用量 |
| PUT/DELETE | `/v1/me/llm-credential` | BYOK 绑定/解绑 |
| GET | `/v1/admin/usage/summary` | 管理汇总 |
| GET | `/v1/admin/usage/calls` | 调用明细（分页） |
| GET | `/v1/admin/usage/athletes/{id}` | 单人下钻 |
| PUT | `/v1/admin/quotas` | 全局/个人配额 |
| CRUD | `/v1/admin/prices` | 价目 |

SSE `done.usage` 只回本次 **Run 汇总**（token_in/out、key_source），明细以管理端为准，避免把成本结构暴露过细。

---

## 7. 与旧系统差异（不要再犯）

| 旧 Training Log | 本系统 |
|-----------------|--------|
| 字段有、主链路传 `null` | `LlmGateway` 强制写入，无 usage 则 ESTIMATED |
| 无 BYOK / 系统拆分 | `key_source` + `billed_to` |
| 无日汇总、无配额拦截 | Redis 热计数 + `llm_quota` |
| 仅 Debug 能看到 usage | App 用量页 + Admin Token 监控 |

---

## 8. P0 / P1

| 项 | 阶段 |
|----|------|
| 事实表 + Gateway + 流式 include_usage | P0 |
| SYSTEM 日配额拦截 + App 用量页 | P0 |
| Admin Token 监控 + 80% 告警 | P0 |
| 日表对账任务 | P0 |
| 按金额封顶、多币种、缓存 token 优惠价 | P1 |
| 用户级自定义月配额包（付费套餐） | P1 |
