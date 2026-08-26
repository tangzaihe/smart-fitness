# 训练知识 RAG 扩展

> 结论：**现有 Coach Runtime 能接专业级训练 RAG，不需要推翻。** 前提是检索必须走 Tool、必须带器械/偏好/伤痛过滤，且 **RAG 不能越过护栏**。  
> P0 用同一 Tool 打 `exercise_catalog`（结构化检索）；P2 把实现换成向量库，Agent 与 App 契约不变。  
> 日期：2026-08-23

---

## 1. 要解决的产品问题

教练不能只会「深蹲 4×8」。同一训练刺激，要按：

- **场馆器械**（史密斯 / 哈克 / 腿举 / 只有哑铃）
- **用户偏好**（讨厌腿屈伸、喜欢自由重量、怕过头推）
- **约束**（右肩酸 → 排除垂直推，给出替代）

给出 **2–3 个可互换方案**（多元化），而不是一篇散文里塞一个动作。专业 RAG 提供「为什么能替、怎么做、禁忌」，不是替代 `session_log`。

---

## 2. 为什么现在这套架构接得上

| 已有能力 | 对 RAG 的意义 |
|----------|----------------|
| LangChain4j + 唯一 `LlmGateway` | Embedding / 对话都走网关，token 可记 `purpose=embedding` |
| Tool 才是 Agent 的手 | 加 `retrieve_exercise_options`，不必改 SSE、不必改 App 状态机 |
| `athlete.equipment` + 约束 | 检索的 **强制 metadata filter**，禁止「检索到史密斯但用户在酒店哑铃房」 |
| `exercise_catalog` | 结构化主数据；RAG 是注释/替代/器械用法，**不替代 code** |
| `advice.payload` + `evidence` JSONB | 多方案 + 引用 chunk id，前端做「方案 A/B/C」卡片 |
| 护栏在 `propose_session` 之后 | RAG 召回再宽，疼痛/容量规则仍可把动作剔掉 |

**不要**把向量库塞进 Prompt 预填（旧 Copilot 那套）。一次 Run 按需 retrieve，和 `get_readiness` 一样是 Tool。

```text
Observe: get_athlete / get_readiness / get_recent_load
Retrieve: retrieve_exercise_options(goal, muscles, equipment[], prefs[], k=5)
          └─ P0: SQL 过滤 catalog
          └─ P2: 同一入参 → pgvector + 器械 metadata filter
Guard:    伤痛/容量剔除
Advise:   2–3 个 alternatives，每条带 catalog.code + rag citations
```

---

## 3. 契约（P0 就要冻住，P2 只换实现）

### 3.1 Tool：`retrieve_exercise_options`

```json
{
  "goal_muscle": ["quad", "glute"],
  "movement_pattern": "squat",
  "equipment": ["barbell", "leg_press", "dumbbell"],
  "exclude_patterns": ["overhead_press"],
  "preferences": { "liked": ["free_weight"], "disliked": ["leg_extension"] },
  "k": 5
}
```

返回（P0/P2 形状相同）：

```json
{
  "options": [
    {
      "exercise_code": "bb_back_squat",
      "score": 0.91,
      "why": "自由重量、匹配偏好",
      "swap_group": "squat_pattern",
      "citations": []
    },
    {
      "exercise_code": "leg_press_45",
      "score": 0.84,
      "why": "场馆有 45° 腿举，腰椎压力更低",
      "swap_group": "squat_pattern",
      "citations": ["kb:machine:leg_press_45#chunk-2"]
    }
  ]
}
```

P0：`citations` 恒为 `[]`，`score` 用规则分（器械命中 + 偏好加减）。  
P2：`citations` 填知识块 id，`score` 为向量相似度 × 规则加权。

### 3.2 Advice 必须「多选择」

`propose_session` 的 payload 增加：

```json
{
  "slots": [
    {
      "slot": "primary_squat",
      "pick": "bb_back_squat",
      "alternatives": ["leg_press_45", "goblet_squat"]
    }
  ]
}
```

App：主方案 + 「换一个（同刺激）」不重新跑完整 Run，只在已召回的 `alternatives` 里切（可再调一次轻量 retrieve）。这才是「多选择」，而不是再问一句模型碰运气。

### 3.3 过滤红线（RAG 也必须遵守）

检索层 **先滤后搜**：

1. `equipment` 与场馆/用户器材求交，空集则只返回自重/当前有的器械。  
2. `athlete_constraint` 映射到 `exclude_patterns` / 排除肌群。  
3. `disliked` 降权，不直接物理删除（除非用户标记「永远不要」）。  
4. 护栏再滤一遍。向量分再高也不能推荐过头推给肩伤用户。

---

## 4. P2 知识模块（现在不实现，目录先占位）

建议模块：`smart-fitness-knowledge`，Coach **只依赖接口** `KnowledgePort`。

```text
KnowledgePort.retrieve(RetrieveQuery) → List<ExerciseOption>
P0 实现：CatalogKnowledgeAdapter（SQL）
P2 实现：RagKnowledgeAdapter（EmbeddingStore + metadata）
```

### 4.1 建议存储（P2）

与现网同一 PostgreSQL 即可，降低运维：

- 扩展 `vector`（pgvector）  
- 表：`kb_document`（语料版本、来源、器械标签）  
- 表：`kb_chunk`（embedding、metadata jsonb：equipment[], pattern, contra_indications[]）  
- 语料类型：器械操作 / 动作替代矩阵 / 禁忌与回归 / 周期化原则（短块，禁止整本教材塞进一次检索）

Embedding 调用同样进 `llm_call_usage.purpose = embedding`。

### 4.2 摄入

Admin P2：「知识库」菜单 — 上传 Markdown/PDF、打器械标签、发布版本。Agent 只读 **active** 版本，可回滚。不在 P0 导航出现。

---

## 5. catalog 与 RAG 的分工（不要混成一张表）

| | `exercise_catalog` | RAG chunks |
|--|--------------------|------------|
| 是什么 | 动作主数据（code 稳定） | 说明、替代理由、器械用法 |
| 写入 | 产品/运营维护 | 专业知识入库 |
| Agent | 处方必须落到 code | 用来选哪个 code、怎么解释 |
| 无 RAG 时 | 仍能出方案（选择变少） | — |

P0 就要把 catalog 的 `equipment`、`pattern`、`swap_group` 列写全，否则 P2 向量也滤不干净。

`athlete` 增加 `preferences jsonb`（liked / disliked / never），建档页收集，Retrieve 必带。

---

## 6. 明确不能靠 RAG 做的事

- 不算准备度、不写 `set_log`  
- 不绕过容量/疼痛护栏  
- 不让模型自由发明未入库 `exercise_code`（无 code 则只能进「待运营审核」槽，P0 直接丢弃）  
- 不把整库 Top-K 无过滤塞进上下文（费 token 且会推荐用户没有的器械）

---

## 7. 阶段

| 阶段 | 做什么 |
|------|--------|
| **P0** | 冻结合约：Tool + Advice.alternatives；Catalog 实现；preferences 字段；护栏后过滤 |
| **P2** | pgvector、摄入、citations、Admin 知识版本；Adapter 切换，**不改** App SSE |
| P3 | 评测集（替代是否合法、是否泄漏禁用动作），再谈微调 |

P0 不接向量库，但 **P0 若把 `propose_session` 做成只返回一个写死动作列表，P2 就要改 App**。多方案字段必须现在就有。
