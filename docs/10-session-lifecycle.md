# 课次生命周期

> 冻结 decide → 训练中 → 结束。日期：2026-08-23

---

## 1. 状态

`session_log.status`：

```text
IN_PROGRESS ──complete──► COMPLETED
      │
      └──abandon──► ABANDONED
```

P0 不使用 CREATED。确认成功即 `IN_PROGRESS`。

同一 athlete 至多一条 `IN_PROGRESS`（08-3002）。

---

## 2. 确认 Advice

`POST /v1/advice/{id}/decide`

```json
{ "action": "accept" | "rest" | "reject" }
```

| action | 条件 | 效果 |
|--------|------|------|
| accept | status=proposed，未过期，payload.kind∈{SESSION,DELOAD} | advice→accepted；**创建课** |
| rest | proposed | advice→accepted；若 kind 已是 REST 不建课；若是 SESSION 则视为改选休息，不建课 |
| reject | proposed | advice→rejected；`decision_event`；不建课 |

`accept` + `kind=REST`：不建课，仅 decision。

创建课（同一事务）：

1. `INSERT session_log`：status=IN_PROGRESS，advice_id，started_at=now()  
2. 按 `payload.slots` 顺序展开 `set_log`：每个 slot 产生 `sets` 行，`set_index` 从 1，`exercise_code=pick`，`completed=false`，`reps/load_kg` 用槽位目标（实际成绩先空）  
3. `decision_event.action=accept`  
4. 返回 `{ "sessionId", "setCount" }`

幂等：已 accepted 且已有 session → 返回原 sessionId。

---

## 3. 训练中

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/sessions/current` | 当前 IN_PROGRESS，无则 data=null |
| GET | `/v1/sessions/{id}` | 含 sets |
| PATCH | `/v1/sessions/{id}/sets/{setId}` | `{ "reps", "loadKg", "rpe", "completed" }` |
| POST | `/v1/sessions/{id}/complete` | 全部或允许未完成组以 completed=false 结束 |
| POST | `/v1/sessions/{id}/abandon` | 中途放弃 |

PATCH：仅 IN_PROGRESS 且属于自己。`completed=true` 必须带 `reps`（`loadKg` 自重动作可 0）。

换动作（同 slot alternatives）：P0 允许 PATCH 增字段 `exerciseCode`，必须 ∈ 该 session 对应 advice 该 slot 的 `pick∪alternatives`，否则 1001。

训练中追问教练：`POST /v1/coach/runs` 且 body `{ "message", "sessionId" }`。P0 若已有 running Run 则 3007；**不**取消进行中课。

---

## 4. 结束与复盘

**complete**

- status→COMPLETED，ended_at=now()  
- 可选 body `{ "perceivedExertion": 1-10, "notes" }`  
- 同步调用准备度 v1，insert `athletic_state`  
- P0 **不自动**再开一轮 post_workout LLM（避免配额）；App 可提示「问问教练今天怎么样」由用户发起

**abandon**

- status→ABANDONED，ended_at=now()  
- **不算** G3 的「上次完成课」  
- 仍可重算准备度（未完成组不计入负荷）

---

## 5. 时序

```text
Coach SSE advice
    → POST decide accept
    → session IN_PROGRESS + N 个 set_log
    → PATCH sets…
    → POST complete
    → athletic_state v1
```

无教练、纯记训：P0 **不做**（避免第二套入口）。必须先有 accepted SESSION/DELOAD advice。

---

## 6. GET 历史

`GET /v1/sessions?page=&size=` 仅 COMPLETED/ABANDONED，按 started_at desc。  
进行中只用 `current`。
