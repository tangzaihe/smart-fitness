# 教练 Agent 产品需求文档（PRD）

> Smart Fitness · 教练 Agent 重设计  
> 版本：v1.0 · 日期：2026-08-26  
> 状态：**待评审**（评审通过后作为 01 的方向性修订，施工契约在 Phase 落地时同步更新 08/02）

**关联文档：**

| 文档 | 关系 |
|------|------|
| [01-app-prototype.md](01-app-prototype.md) | 本文 supersede 其「教练 Tab / 自动 Observe / 主路径」部分 |
| [02-backend-architecture.md](02-backend-architecture.md) | Phase 2 起增补 Conversation / CoachPlan 表与 API |
| [08-contracts.md](08-contracts.md) | Advice / Session 契约保留；新增 Plan / Conversation 契约待冻 |
| [09-readiness-guardrails.md](09-readiness-guardrails.md) | 护栏 G1–G5 继续适用于今日 Advice |
| [10-session-lifecycle.md](10-session-lifecycle.md) | Session 状态机不变；新增 `coach_plan_id` / `daily_slot_id` 外键 |

---

## 1. 背景与问题陈述

### 1.1 产品初衷

教练 Agent 应拟人化为用户「常去健身房遇见的资深教练」：

- 长期认识用户（身体、健康、训练史、伤病、饮食）；
- 通过分类对话精准回应；
- 在运动规划对话中产出**周期训练计划**，采纳后持续带队执行；
- 每天结合状态给出**今日训练**，并监督执行。

### 1.2 当前实现差距

| 维度 | 当前（P0 切片） | 目标 |
|------|-----------------|------|
| 记忆 | 档案 + readiness + 近负荷 | 用户全域记忆 + 对话摘要 |
| 交互 | 无对话；打开页自动 Run | 意图路由 + 可选对话 |
| 计划 | 单次 Advice（一堂课） | 周期 CoachPlan + 每日 Slot |
| 每日门 | 无；被动等 LLM | 确认**今日执行**，非重确认整份 Plan |
| 首屏 | 教练 Tab 左一；自动 LLM | **今日**居中枢纽；L0 洞察即时展示 |
| 后端 | 固定流水线单次 JSON | Conversation → Plan → 今日 Advice |

### 1.3 核心问题（用户侧）

1. 新手打开 App 即触发 LLM，黑盒等待，不知为何。  
2. 「再观察一次 / 手动触发」为开发者语言。  
3. 教练 / 训练 Tab 割裂，主路径需多次跳转。  
4. 无周期计划与对话上下文，无法形成「教练在带我」的关系感。

---

## 2. 产品愿景与定位

### 2.1 一句话

> **Smart Fitness 的教练 Agent = 24 小时在线、记得你所有训练数据的资深私教；他为你制定周期计划，每天根据你的状态确认今日怎么练，并带你练完。**

### 2.2 不是什么

- 不是通用 ChatGPT / 闲聊机器人（主路径是训练执行）。  
- 不是计划模板市场或动作库浏览器。  
- 不是「每次打开都重新生成处方」的无状态 API。

### 2.3 设计原则

| # | 原则 | 说明 |
|---|------|------|
| P1 | **计划默认持续，每日确认执行** | CoachPlan 采纳后 ACTIVE；每天只问「今天练什么」 |
| P2 | **洞察即时，建议按需** | L0 规则数据首屏即显；调 LLM 须用户主动或明确 CTA |
| P3 | **对话服务计划，计划服务执行** | Conversation → CoachPlan → DailySlot → Advice → Session |
| P4 | **确认门不可绕过** | LLM 不写 `session_log`；今日开练仍走 Advice.decide |
| P5 | **居中 Tab = 今日行动** | 首屏是一键开练，对话是深入路径 |

---

## 3. 目标与非目标

### 3.1 本 PRD 目标（Phase 1–2）

- [ ] 定义 Conversation、CoachPlan、DailySlot 概念与状态机。  
- [ ] 重构 App 信息架构：**今日 Tab 居中**。  
- [ ] 实现每日三道门：按 plan 开练 / 调整今天 / 今天休息。  
- [ ] 运动规划对话产出 CoachPlan，Conversation 与 Plan 1:1。  
- [ ] 采纳 Plan 后，运动类上下文默认注入 Active Plan。  
- [ ] 保留并复用现有 Advice、护栏、Session 生命周期。

### 3.2 非目标（本 PRD 不含）

- 饮食日志与饮食 RAG 全量（Phase 3；意图枚举预留）。  
- 向量 RAG 替换 Catalog 检索（见 [05-training-rag.md](05-training-rag.md)）。  
- 端上 Prompt 编辑、Admin 以外策略热改。  
- 多 Plan 并行（同一 athlete 仅一个 ACTIVE Plan）。  
- Flutter / 第二移动端。

---

## 4. 用户画像与核心场景

### 4.1 画像

| 画像 | 特征 | 核心诉求 |
|------|------|----------|
| **新手 Hanna** | 第一次系统训练，不懂分化 | 「告诉我今天练什么，别让我想」 |
| **复出者** | 有伤病史，怕练错 | 「记得我肩膀不行，别给我推举」 |
| **规律训练者** | 有目标，要周期计划 | 「这周按计划走，每天微调就行」 |

### 4.2 场景矩阵

| ID | 场景 | 触发 | 期望结果 |
|----|------|------|----------|
| S1 | 首次建档后 | 完成 onboarding | 引导发起「运动规划」对话，产出首份 Plan |
| S2 | 每日打开 App | 有 ACTIVE Plan | 今日页即时展示洞察 + 今日 Slot + 主 CTA |
| S3 | 按 plan 开练 | 点主 CTA | 生成/展示今日 Advice → 采纳 → Session |
| S4 | 肩酸想调整 | 点「调整今天」 | 对话（带 Plan 上下文）→ 修订今日 Slot → 新 Advice |
| S5 | 太累要休息 | 点「今天休息」 | REST Advice，Slot 标记 SKIPPED，Plan 可顺延 |
| S6 | 训练中问教练 | 训练页 / 全局条 | 带 `sessionId` + `activePlanId` 的轻量对话 |
| S7 | 连续缺席 | 系统检测 ≥3 天未执行 | 提示修订或暂停 Plan，非每日追问「还要不要计划」 |
| S8 | 换新计划 | 新运动规划对话 | 确认替换 ACTIVE Plan → 旧 Plan PAUSED/ABANDONED |

---

## 5. 概念模型与术语

```text
Athlete（用户）
  │
  ├── L0 UserMemory（结构化事实 + 时序事件 + 对话摘要）
  │
  ├── Conversation（对话线程）
  │     ├── messages[]
  │     ├── intent（线程主意图，可混合消息级 intent）
  │     └── CoachPlan?（运动规划类，采纳后 1:1 绑定）
  │
  ├── CoachPlan（周期训练计划）
  │     ├── status: DRAFT → ACTIVE → PAUSED / COMPLETED / ABANDONED
  │     ├── slots[]: DailySlot（按日或按模板推导）
  │     └── active_plan_id（运动员维度唯一 ACTIVE）
  │
  ├── DailySlot（计划中的「某一天练什么」）
  │     └── status: PENDING | DONE | SKIPPED | ADJUSTED
  │
  ├── Advice（今日可确认处方，现有实体扩展）
  │     └── 关联 daily_slot_id、coach_plan_id
  │
  └── SessionLog（执行中的课，现有实体扩展）
        └── 关联 advice_id、coach_plan_id、daily_slot_id
```

| 术语 | 定义 |
|------|------|
| **L0 洞察** | 规则计算的 readiness、睡眠、负荷等，**不调 LLM** |
| **CoachPlan** | 多周周期计划，Conversation 采纳后生效 |
| **DailySlot** | Plan 内单日训练意图（部位/动作结构） |
| **今日 Advice** | 由今日 Slot + readiness 微调生成的可确认处方 |
| **ACTIVE Plan** | 当前正在执行的 Plan，全站运动上下文默认来源 |

---

## 6. 分层架构

```text
┌─────────────────────────────────────────────────────────────┐
│ L0 用户长期记忆                                              │
│ 身体档案 · 健康/睡眠 · 训练史 · 伤病约束 · 饮食(Phase3) · 对话摘要 │
│ 检索：结构化直读 + 时序聚合 +（Phase3）语义向量                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│ L1 对话层                                                    │
│ Conversation + Intent 路由 → Prompt / Tool 集 / 输出形态      │
│ 意图：CHAT | WORKOUT_PLANNING | NUTRITION | GENERAL_WELLNESS  │
└──────────────────────────┬──────────────────────────────────┘
                           │ WORKOUT_PLANNING 采纳后
┌──────────────────────────▼──────────────────────────────────┐
│ L2 计划层                                                    │
│ CoachPlan（周期）· DailySlot · ACTIVE 唯一 · 修订历史           │
└──────────────────────────┬──────────────────────────────────┘
                           │ 每日拆解
┌──────────────────────────▼──────────────────────────────────┐
│ L3 执行层                                                    │
│ 今日 Advice → decide → SessionLog → SetLog → 回写 readiness   │
│ 全局「继续训练」条 · 组间追问（Phase 3）                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. CoachPlan 数据形态（详细）

### 7.1 计划结构

CoachPlan 支持两种互补形态（**至少实现 A，B 可 Phase 2 末加入**）：

**A. 按日排列（`day_index` 1…N）**

```json
{
  "schema_version": 1,
  "title": "4 周增肌 · 推拉腿",
  "goal": "HYPERTROPHY",
  "duration_days": 28,
  "days_per_week": 4,
  "slots": [
    {
      "day_index": 1,
      "label": "推日",
      "focus_muscles": ["CHEST", "SHOULDER", "TRICEP"],
      "session_template": {
        "patterns": ["HORIZONTAL_PUSH", "VERTICAL_PUSH", "ISOLATION"],
        "slot_count": 4,
        "notes": "主项卧推"
      }
    }
  ]
}
```

**B. 周模板循环（`week_template` + `current_day`）**

适合「推拉腿 × 4 周」类重复结构；`day_index` 由 `(week-1)*7 + weekday` 推导。

### 7.2 DailySlot 物化策略

| 策略 | 说明 | 推荐 |
|------|------|------|
| 运行时推导 | 每次读 Plan JSON 算今日 | Phase 1 够用 |
| 物化表 `coach_plan_day` | 跳过的天顺延写入 | Phase 2 |

物化字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | PK |
| `coach_plan_id` | bigint | FK |
| `day_index` | int | Plan 内第几天 |
| `scheduled_date` | date | 计划日历日（可顺延） |
| `label` | string | 「腿日」「休息」 |
| `session_template` | jsonb | 结构意图，非最终动作 code |
| `status` | enum | PENDING / DONE / SKIPPED / ADJUSTED |
| `resolved_advice_id` | bigint? | 当日最终 Advice |

### 7.3 今日处方生成逻辑

```text
输入：active_plan + today_scheduled_slot + athletic_state + constraints
处理：
  1. 若 slot.status = SKIPPED → 今日页显示「计划休息日」
  2. 若 readiness < 40 或 MEDICAL 约束 → 倾向 REST（护栏 G4）
  3. 否则 retrieve + LLM 将 session_template 具化为 Advice.payload.slots
  4. 插入 advice（PENDING），关联 plan_id + daily_slot_id
输出：今日 Advice（需用户 confirm，规则同 10-session-lifecycle）
```

**不调 LLM 的降级路径（Phase 1 可选）：** 若昨日同结构 Slot 有 ACCEPTED Advice，可模板复用 + 微调文案。

### 7.4 Plan 进度与顺延规则

| 事件 | Plan 进度 | DailySlot |
|------|-----------|-----------|
| 今日 ACCEPT 并完成 Session | `current_day++`（或标记 slot DONE） | DONE |
| 今日 REST | 不递增 day_index | SKIPPED，**scheduled_date 不顺延**（默认） |
| 今日 SKIPPED（用户点休息） | 可选策略见下 | SKIPPED |
| 连续 3 天无 DONE | 触发 S7 修订提示 | — |

**顺延策略（默认：固定日历）**

- **固定日历**：rest/skip 不推迟后续 day；用户「落后」时教练提示加练或修订 Plan。  
- **顺延日历**（Phase 3 配置项）：skip 将后续 slot 整体 +1 天。

默认采用 **固定日历**，避免用户「越休越债」心理压力。

### 7.5 Plan 级确认 vs 每日确认（关键产品规则）

| 问题 | 何时问 | 文案示例 |
|------|--------|----------|
| 是否继续**整份 Plan** | 采纳时、S7/S8、周期结束 | 「将替换当前增肌计划，继续吗？」 |
| 是否执行**今日训练** | 每日首次进入今日页、无 IN_PROGRESS session | 「今天按计划练腿，开练吗？」 |

**禁止**每日询问「是否按照之前的计划进行」（指整份 Plan）。

---

## 8. Conversation 与意图路由

### 8.1 Conversation 模型

| 字段 | 说明 |
|------|------|
| `id` | PK |
| `athlete_id` | FK |
| `primary_intent` | 线程主意图 |
| `coach_plan_id` | 采纳后绑定；运动规划类 1:1 |
| `status` | OPEN / ARCHIVED |
| `title` | 自动生成，如「4 周增肌计划」 |
| `summary` | 滚动摘要，写入 L0 语义记忆（Phase 3） |

### 8.2 意图枚举

| Intent | 说明 | 默认 Tool | 可产出 |
|--------|------|-----------|--------|
| `CHAT` | 寒暄、动机 | 少量 L0 | 纯文本 |
| `WORKOUT_PLANNING` | 周期/调整计划 | L0 + retrieve + 护栏 | CoachPlan 草案 |
| `NUTRITION` | 饮食 | L0 + 通用知识（P1 无饮食 RAG） | 文本 + 免责 |
| `GENERAL_WELLNESS` | 睡眠/恢复/装备 | L0 | 文本 |

消息级 `intent` 可覆盖线程默认值（同线程内从闲聊切到规划）。

### 8.3 路由流程

```text
用户消息
  → intent_classifier（规则关键词 + 小模型，Phase 1 可仅规则）
  → 选择 policy_template + allowed_tools
  → LLM 流式回复
  → 若产出 structured_output（plan_draft / advice_draft）→ 落库待确认
```

### 8.4 Active Plan 上下文注入

凡 `intent ∈ {WORKOUT_PLANNING, CHAT}` 且用户消息命中运动关键词，或显式 `sessionId`：

```json
{
  "active_plan_summary": { "planId", "title", "currentDay", "todayLabel" },
  "today_slot": { ... },
  "readiness_snapshot": { ... }
}
```

新 Conversation 也注入，**不要求用户回到原对话 A**。

---

## 9. 信息架构与 UI 需求

### 9.1 Tab 结构（修订 01）

```text
┌─────────┬─────────┬─────────┬─────────┐
│  身体   │  训练   │  今日*  │  我的   │
└─────────┴─────────┴─────────┴─────────┘
              ↑ 默认落地 Tab（居中，index 2）
```

| Tab | 职责 | 变更 |
|-----|------|------|
| **今日** | Plan 上下文 + L0 洞察 + 每日三道门 + 进入对话 | **新**，合并原教练首屏核心 |
| **训练** | 进行中记组 + 历史课 | 无 IN_PROGRESS 时引导回今日 |
| **身体** | readiness 详情、手填 wellness | 不变 |
| **我的** | 档案、约束、额度、当前 Plan 管理 | 增加 Plan 只读/暂停入口 |

Tab 文案对用户显示 **「今日」**；品牌语境下副标题可为「你的教练」。

### 9.2 今日页线框

```text
┌─────────────────────────┐
│ 早安，{name}               │
│ {Plan标题} · 第 {d}/{N} 天 │  ← 无 Plan 时：「和教练制定你的计划」
├─────────────────────────┤
│ 准备度 {score}  规则 v1  │  ← L0 即时，进入即显
│ 睡眠 · 肌群疲劳 · 近7日课次 │
├─────────────────────────┤
│ 今日：{腿日 / 休息 / 未排} │
│ {教练一句话解释，规则或 LLM}│
├─────────────────────────┤
│ [ 按今日计划开练 ]         │  ← Primary，有 slot 且非 REST
│ [ 调整今天 ]              │  ← 打开对话，带 plan 上下文
│ [ 今天休息 ]              │
├─────────────────────────┤
│ 跟教练聊聊 →              │  ← 进入 Conversation 列表/默认线程
└─────────────────────────┘
│ 身体  训练  今日  我的      │
└─────────────────────────┘
```

### 9.3 无 ACTIVE Plan 空态

```text
今日页：
  「还没有训练计划」
  「花 3 分钟和教练聊聊你的目标」
  [ 制定我的计划 ]  → WORKOUT_PLANNING 新 Conversation
```

**禁止**空态自动 `POST /coach/runs`。

### 9.4 进行中训练全局条

任意 Tab 顶部，当存在 `session_log.status=IN_PROGRESS`：

```text
┌──────────────────────────────────┐
│ ● 训练中 · 杠铃深蹲 第 2 组  [继续] │
└──────────────────────────────────┘
```

点击「继续」→ 训练 Tab。避免今日居中后训练中找课困难。

### 9.5 今日 Advice 确认

用户点「按今日计划开练」后：

1. 若尚无今日 PENDING Advice → 触发生成（可展示轻量进度，非全页阻塞）。  
2. 展示 `AdviceCard`（复用现有组件）：采纳 / 只要休息 / 忽略。  
3. 采纳 → `sessionId` → 训练 Tab 或全局条。

### 9.6 对话 UI

- 从今日页「调整今天」「跟教练聊聊」进入。  
- 支持流式文本；结构化 Plan 草案用 **PlanPreviewCard** 确认采纳。  
- P0 不要求全量聊天历史 UI；至少保留当前线程最近 20 条。

---

## 10. 状态机

### 10.1 CoachPlan

```text
DRAFT ──用户采纳──► ACTIVE
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
    PAUSED         COMPLETED        ABANDONED
   （用户/教练）    （跑完 N 天）    （替换/放弃）
```

- 同一 `athlete_id` 仅允许一个 `ACTIVE`。  
- 新 Plan 采纳 → 旧 ACTIVE 自动 `ABANDONED`（需二次确认）。

### 10.2 DailySlot

```text
PENDING ──今日 Advice ACCEPT+完成课──► DONE
   │
   ├──用户 REST / 点今天休息──► SKIPPED
   │
   └──调整今天──► ADJUSTED（链到新 Advice，可仍 DONE）
```

### 10.3 Conversation

```text
OPEN ──归档/替换──► ARCHIVED
```

### 10.4 今日页 UI 状态（客户端）

| 状态 | 条件 | 主 CTA |
|------|------|--------|
| `no_plan` | 无 ACTIVE Plan | 制定计划 |
| `ready` | 有 Plan，今日 PENDING slot | 按今日计划开练 |
| `rest_day` | 今日 slot 为休息或建议 REST | 确认休息（次要：仍想练） |
| `generating` | 正在生成 Advice | 进度，禁用 CTA |
| `awaiting_confirm` | 有 PENDING Advice | AdviceCard |
| `in_session` | IN_PROGRESS session | 全局条「继续训练」 |

---

## 11. 核心用户旅程（端到端）

### 11.1 新用户

```text
注册 → 建档 → 今日页（空态）→ 制定计划对话
  → Plan 草案预览 → 采纳（ACTIVE）
  → 今日页显示 Day 1 → 按今日计划开练 → Advice → Session
```

### 11.2 老用户每日

```text
打开 App → 今日页（L0 即时）
  → 阅读「今日：腿日」+ 教练一句话
  → [按今日计划开练] → Advice 确认 → 训练
  → complete → readiness 更新 → 今日页显示「练完了，明天…」
```

### 11.3 调整今天

```text
今日页 [调整今天] → 对话（注入 active plan）
  → 用户：「肩还酸，别推了」
  → 教练修订今日 template → 新 Advice
  → 用户采纳 → Session（upper 降级或改部位）
```

---

## 12. API 需求概要（待冻入 08）

### 12.1 新增（Phase 1–2）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/today` | 聚合：readiness + active_plan + today_slot + pending_advice + current_session |
| GET/POST | `/v1/conversations` | 列表 / 创建 |
| GET | `/v1/conversations/{id}/messages` | 分页消息 |
| POST | `/v1/conversations/{id}/messages` | 发消息（SSE 流式回复） |
| POST | `/v1/coach-plans` | 从对话草案创建 DRAFT |
| POST | `/v1/coach-plans/{id}/adopt` | DRAFT→ACTIVE，替换旧 ACTIVE |
| POST | `/v1/coach-plans/{id}/pause` | ACTIVE→PAUSED |
| GET | `/v1/coach-plans/active` | 当前 ACTIVE Plan + 进度 |
| POST | `/v1/today/generate-advice` | 为今日 slot 生成 Advice（幂等） |
| POST | `/v1/today/rest` | 今日休息，标记 slot SKIPPED |

### 12.2 修订（兼容）

| 路径 | 变更 |
|------|------|
| `POST /v1/coach/runs` | **废弃自动 Observe**；改为带 `conversationId` + `message` 的会话 Run，或 Phase 1 标记 deprecated |
| `POST /v1/advice/{id}/decide` | 响应不变；副作用写入 `daily_slot.status` |
| `session_log` | 新增可选 `coach_plan_id`, `daily_slot_id` |

### 12.3 错误码（草案）

| code | 含义 |
|------|------|
| 3010 | 无 ACTIVE Plan |
| 3011 | Plan 状态不允许该操作 |
| 3012 | 今日 Slot 已 DONE，不可重复开练 |
| 3013 | 替换 Plan 需 confirm 标志 |

---

## 13. L0 记忆与 Tool（分阶段）

### Phase 1（结构化，无向量）

| Tool / 数据源 | 内容 |
|---------------|------|
| `get_athlete_profile` | 目标、器材、偏好、约束 |
| `get_readiness` | athletic_state 最新 |
| `get_recent_sessions` | 近 7/14 日课次摘要 |
| `get_active_plan` | Plan + today slot |

### Phase 3

| 扩展 | 内容 |
|------|------|
| `get_nutrition_log` | 饮食记录 |
| `search_user_memory` | 对话摘要向量检索 |
| `retrieve_exercise_options` | 同 05，不变 |

---

## 14. 分阶段交付计划

### Phase 1 — 今日枢纽（4–6 周）

**目标：** 新手 30 秒内理解「今天干嘛」；消灭自动 Run。

| 交付项 | 说明 |
|--------|------|
| 今日 Tab 居中 + 页面 | L0 洞察 + 三道门（无 Plan 时空态） |
| GET `/v1/today` | 聚合接口 |
| 移除教练页自动 `OBSERVE` | 移动端 |
| 全局「继续训练」条 | 任意 Tab |
| Mock Plan（可选） | 服务端写死 7 天模板，验证每日流 |

**验收：** 新用户不再触发静默 LLM；有 mock plan 时可走完开练路径。

### Phase 2 — Plan 与对话（6–8 周）

| 交付项 | 说明 |
|--------|------|
| `conversation` / `coach_plan` / `coach_plan_day` 表 | Flyway |
| 运动规划对话 + PlanPreviewCard | 采纳 / 替换确认 |
| `POST /today/generate-advice` | Slot → Advice |
| Active Plan 上下文注入 | 所有运动类 Run |
| 我的 · 当前计划 | 只读 + 暂停 |

**验收：** S1–S6 场景可走通；Conversation A 采纳 Plan 后，新对话仍知 active plan。

### Phase 3 — 记忆与深度教练（后续）

- 对话摘要 → 用户记忆  
- 饮食意图 + 饮食日志  
- 训练中组间追问  
- 周复盘自动修订 Plan  
- 顺延日历策略可选  

---

## 15. 验收标准（Phase 2 完整）

### 15.1 功能

- [ ] 新用户建档后**不会**自动调用 LLM。  
- [ ] 同一用户仅一个 ACTIVE Plan；采纳新 Plan 有替换确认。  
- [ ] 今日页进入 **<500ms** 展示 L0 洞察（不含 LLM）。  
- [ ] 每日主路径为三 CTA，无「是否继续之前计划」整 Plan 文案。  
- [ ] 「按今日计划开练」→ Advice → decide → Session 符合 10-session-lifecycle。  
- [ ] 护栏 G1–G5 对今日 Advice 仍生效。  
- [ ] IN_PROGRESS 时全局条在任意 Tab 可见。  

### 15.2 体验

- [ ] 新手无需理解 OBSERVE/MANUAL/trigger。  
- [ ] 教练解释可见（`rationale` 或流式一句人话）。  
- [ ] 调整今天后今日 Slot 状态为 ADJUSTED 且可追溯。  

### 15.3 非功能

- [ ] 今日页 LLM 调用仅发生在用户点「开练」或「调整」之后。  
- [ ] 每次 LLM 调用落 `llm_call_usage`（04 不变）。  
- [ ] SSE 仍绑定 runId，不广播。  

---

## 16. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| Plan 生成质量不稳定 | 用户不敢采纳 | 草案可编辑；护栏 + 人工模板 fallback |
| 对话成本过高 | 配额快速耗尽 | CHAT 小模型/限额；运动规划才用大模型 |
| 与 01/08 文档冲突 | 施工混乱 | 本文评审通过后修订 01；Phase 2 冻 Schema 入 08 |
| 现有用户无 Plan | 升级空窗 | 迁移脚本生成默认 7 天模板或引导建档对话 |
| 每日生成 Advice 慢 | 开练挫败 | 预生成（前晚推送）或模板缓存 |

---

## 17. 开放问题（评审待决）

| # | 问题 | 建议默认 | 决策人 |
|---|------|----------|--------|
| Q1 | Tab 文案「今日」还是「教练」 | **今日**（教练为副标题） | 产品 |
| Q2 | skip 是否顺延日历 | **不顺延**（固定日历） | 产品 |
| Q3 | Phase 1 是否上 mock Plan | **是**，加速验证每日流 | 研发 |
| Q4 | 对话是否支持多线程列表 | Phase 2 仅 1 个 OPEN 运动线程 + 归档 | 产品 |
| Q5 | NUTRITION 意图 Phase 2 是否露出入口 | **隐藏**，枚举预留 | 产品 |

---

## 18. 附录 A：今日页文案规范

| 场景 | 教练一句话（示例） |
|------|-------------------|
| 准备良好 | 「睡眠不错，今天按计划练腿，主项深蹲。」 |
| 疲劳偏高 | 「肩还有点紧，今天仍练腿，但头上推举我会帮你拿掉。」 |
| 建议休息 | 「准备度偏低，建议今天休息；想轻量活动可以跟我说。」 |
| 无 Plan | 「先聊聊你的目标，我来帮你排一份计划。」 |
| 课后 | 「今天完成得不错，明天是休息日，好好恢复。」 |

---

## 19. 附录 B：与现有 `coach_run` 的关系

| 现实体 | 演进 |
|--------|------|
| `coach_run` | 保留；一次 LLM 调用单元（消息回复 or 生成 Advice/Plan 草案） |
| `advice` | 保留；增加 `coach_plan_id`, `daily_slot_id` |
| `coach_run.trigger` | 废弃 `OBSERVE`/`APP_OPEN` 自动触发；改为 `CONVERSATION`, `TODAY_START`, `TODAY_ADJUST`, `IN_SESSION` |

---

## 20. 附录 C：评审检查清单

- [ ] 产品：每日确认对象是否为「今日执行」而非「整份 Plan」  
- [ ] 产品：Tab 居中内容为「今日枢纽」而非纯聊天  
- [ ] 后端：Advice/Session 确认门是否保留  
- [ ] 后端：Flyway 迁移与现有数据兼容方案  
- [ ] 移动端：移除自动 Run 的回归范围  
- [ ] 全员：Phase 1 范围是否可独立上线  

---

**文档维护：** 评审结论与 Q1–Q5 决策请直接修订本节 + 变更记录表。

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-08-26 | PM（Agent 协作起草） | 初稿：CoachPlan、今日 Tab、每日三道门 |
