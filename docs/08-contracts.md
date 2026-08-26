# P0 契约：Schema、错误码、幂等

> 施工时 JSONB 与 SSE `data` 必须符合本文。日期：2026-08-23

---

## 1. 统一响应（非 SSE）

```json
{ "code": 0, "message": "ok", "data": {} }
```

`code=0` 成功。失败 `data` 可空或带 `details`。

---

## 2. 错误码

| code | HTTP | 含义 |
|------|------|------|
| 0 | 200 | 成功 |
| 1001 | 400 | 参数校验失败 |
| 1002 | 400 | JSON Schema 不匹配 |
| 2001 | 401 | 未登录 / Token 无效 |
| 2002 | 401 | Token 过期 |
| 2003 | 429 | 登录锁定 |
| 2004 | 403 | 无权限（Admin 角色） |
| 3001 | 409 | 未完成建档 |
| 3002 | 409 | 已有进行中课次 |
| 3003 | 409 | Advice 不可确认（非 proposed / 已过期） |
| 3004 | 409 | 课次状态不允许该操作 |
| 3005 | 429 | 系统 Token 日配额用尽 |
| 3006 | 429 | RPM 超限 |
| 3007 | 409 | 已有进行中 Coach Run |
| 3008 | 422 | 护栏拒绝生成 session 类处方（应出 rest） |
| 4001 | 404 | 资源不存在或不属于当前用户 |
| 5001 | 500 | 系统错误 |
| 5002 | 502 | LLM 供应商失败 |

业务异常类型：`BusinessException(code, message)`，全局处理。

---

## 3. 枚举（禁止自由字符串）

**goal：** `HYPERTROPHY` | `FATLOSS` | `STRENGTH` | `REHAB`

**equipment（数组元素）：**  
`BARBELL` | `DUMBBELL` | `KETTLEBELL` | `SMITH` | `LEG_PRESS` | `CABLE` | `MACHINE` | `BODYWEIGHT` | `BAND` | `PULLUP_BAR`

**pattern：**  
`SQUAT` | `HINGE` | `LUNGE` | `HORIZONTAL_PUSH` | `VERTICAL_PUSH` | `HORIZONTAL_PULL` | `VERTICAL_PULL` | `CARRY` | `CORE` | `ISOLATION`

**muscle_group：**  
`CHEST` | `BACK` | `SHOULDER` | `BICEP` | `TRICEP` | `QUAD` | `HAMSTRING` | `GLUTE` | `CALF` | `CORE` | `FOREARM`

**constraint.type：** `PAIN` | `INJURY` | `MEDICAL` | `TIME` | `EQUIPMENT`

**constraint.body_part**（与 pattern 映射见 09）：  
`SHOULDER` | `ELBOW` | `WRIST` | `NECK` | `LOWER_BACK` | `KNEE` | `HIP` | `ANKLE` | `OTHER`

---

## 4. JSON Schema

### 4.1 `athlete.equipment`

```json
{ "type": "array", "items": { "enum": ["BARBELL", "DUMBBELL", "KETTLEBELL", "SMITH", "LEG_PRESS", "CABLE", "MACHINE", "BODYWEIGHT", "BAND", "PULLUP_BAR"] }, "uniqueItems": true }
```

空数组视为仅 `BODYWEIGHT`。

### 4.2 `athlete.preferences`

```json
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "liked": { "type": "array", "items": { "type": "string" } },
    "disliked": { "type": "array", "items": { "type": "string" } },
    "never": { "type": "array", "items": { "type": "string" } }
  }
}
```

`liked`/`disliked`/`never` 的元素为 `exercise_code` 或标签 `free_weight` | `machine`。`never` 检索时物理排除。

### 4.3 `retrieve_exercise_options` 入参 / 出参

入参：

```json
{
  "type": "object",
  "required": ["movement_pattern", "equipment", "k"],
  "properties": {
    "goal_muscle": { "type": "array", "items": { "type": "string" } },
    "movement_pattern": { "type": "string" },
    "equipment": { "type": "array", "items": { "type": "string" } },
    "exclude_patterns": { "type": "array", "items": { "type": "string" } },
    "preferences": { "$ref": "#/preferences" },
    "k": { "type": "integer", "minimum": 1, "maximum": 8 }
  }
}
```

出参 `options[]`：`exercise_code`（必须存在于 catalog）、`score`（0–1）、`why`（string）、`swap_group`、`citations`（P0 恒 `[]`）。

### 4.4 `advice.payload`（P0）

```json
{
  "type": "object",
  "required": ["schema_version", "kind", "slots"],
  "properties": {
    "schema_version": { "const": 1 },
    "kind": { "enum": ["REST", "DELOAD", "SESSION"] },
    "title": { "type": "string" },
    "rationale": { "type": "string" },
    "slots": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["slot", "pick", "alternatives", "sets", "reps"],
        "properties": {
          "slot": { "type": "string" },
          "pick": { "type": "string" },
          "alternatives": { "type": "array", "items": { "type": "string" }, "maxItems": 3 },
          "sets": { "type": "integer", "minimum": 1, "maximum": 8 },
          "reps": { "type": "integer", "minimum": 1, "maximum": 30 },
          "load_kg": { "type": ["number", "null"] },
          "rpe_cap": { "type": ["number", "null"] }
        }
      }
    }
  }
}
```

`kind=REST` 时 `slots` 必须 `[]`。  
所有 `pick` 与 `alternatives` 必须是 catalog.code，且 `pick` 须出现在当次 retrieve 结果中（护栏 G5）。

`advice.evidence`：

```json
{
  "readiness": 72,
  "calc_version": "v1",
  "sleep_hours": 7.2,
  "constraint_ids": [],
  "retrieve_run": "optional-debug"
}
```

### 4.5 SSE `data`

| event | data JSON |
|-------|-----------|
| `run.created` | `{ "runId", "policyVersion" }` |
| `token` | `{ "delta": "..." }` |
| `tool.start` | `{ "name", "args": {} }` |
| `tool.result` | `{ "name", "ok": true, "summary": "..." }` |
| `advice` | `{ "adviceId", "type", "payload", "evidence", "riskLevel", "confirmRequired": true }` |
| `error` | `{ "code": 3005, "message": "..." }` |
| `done` | `{ "runId", "usage": { "tokenIn", "tokenOut", "keySource" } }` |

`advice.type` 与 payload.kind 对齐：`rest`/`deload`/`session`（小写，兼容 02 文档）。

---

## 5. 幂等与并发

| 操作 | 规则 |
|------|------|
| `POST /v1/advice/{id}/decide` | 同一 advice 已是 `accepted` 且 action=accept → 200 返回已有 `sessionId`，不新建课 |
| `POST /v1/coach/runs` | 同一 athlete 若有 `coach_run.status=running` → `3007`；新 Run 前可产品侧取消旧 Run（P0：直接拒绝） |
| 进行中课 | 同一 athlete 最多 1 条 `session_log.status=IN_PROGRESS` → 否则 `3002` |
| 记组 | `PATCH` 带 `setId`；完成态组再 PATCH → `3004` |
| 可选 | 请求头 `Idempotency-Key`：24h 内相同 Key + 相同 path 返回首次响应（P0 只强制 decide） |

Advice 过期：`proposed` 且 `created_at + 24h` → 自动 `expired`（可用查询时惰性更新）。

---

## 6. 配额默认值（P0）

全局 `llm_quota`（athlete_id NULL）：

- `period=DAY`，`token_limit=200000`  
- `rpm_limit=20`  
- FakeLlm **仍写 usage**，但 Fake 的 token 计入配额（避免无限制刷切片）

超限：`3005` / `3006`，不调用供应商。

---

## 7. Wellness（准备度输入）

P0 增表，否则公式无法落地：

```sql
CREATE TABLE wellness_log (
    id                  BIGINT PRIMARY KEY,
    athlete_id          BIGINT NOT NULL,
    log_date            DATE NOT NULL,
    sleep_hours         NUMERIC(4,1),
    subjective_fatigue  SMALLINT,          -- 1-10，10 最累
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (athlete_id, log_date)
);
```

API：`PUT /v1/wellness/today` `{ "sleepHours", "subjectiveFatigue" }`。教练首页打开时若当日无记录，可用昨日；再无则走 09 缺省。
