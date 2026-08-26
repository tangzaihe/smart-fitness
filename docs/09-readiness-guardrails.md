# 准备度 v1 与护栏 G1–G5

> `athletic_state.calc_version` P0 固定 **`v1`**。改权重必须升 `v2` 并写新文档，禁止改 v1 语义。  
> 日期：2026-08-23

---

## 1. 输入

| 输入 | 来源 | 缺省（无数据） |
|------|------|----------------|
| `sleep_hours` | 当日 `wellness_log`，否则昨日 | **7.0** |
| `subjective_fatigue` | 同上，1–10 | **5** |
| `sets_7d[muscle]` | 近 7 日（含今天）`set_log.completed=true` 按 `muscle_group` 计数 | 0 |
| 约束 | `athlete_constraint` 且 `ends_on IS NULL OR ends_on >= today`，type∈{PAIN,INJURY,MEDICAL} | 无 |

只统计已完成组；进行中课未 complete 的组不计入。

---

## 2. 肌群周容量上限（组数）

| 肌群 | cap（组/7日） |
|------|----------------|
| CHEST, BACK, QUAD, GLUTE, HAMSTRING | 16 |
| SHOULDER, BICEP, TRICEP, CORE | 12 |
| CALF, FOREARM | 10 |

```
fatigue[m] = min(10, 10 * sets_7d[m] / cap[m])   -- 保留 1 位小数
avg_fatigue = 有训练的肌群均值；全 0 则为 0
```

---

## 3. 分项分（0–100）

**sleep_score**

| sleep_hours | sleep_score |
|-------------|-------------|
| ≥ 8.0 | 100 |
| 7.0 ≤ x < 8.0 | 80 + 20 * (x-7) |
| 5.0 ≤ x < 7.0 | 40 + 20 * (x-5) |
| x < 5.0 | 20 |

**load_score** = `100 - avg_fatigue * 10`  
**feel_score** = `100 - subjective_fatigue * 10`

**constraint_penalty**

- 存在 MEDICAL → **+ 不进入 session 处方**（见 G4），readiness 再减 **20**  
- 存在 PAIN 或 INJURY → 减 **15**（多项不叠加，最多 -15）  
- 无 → 0

```
raw = 0.40 * sleep_score + 0.35 * load_score + 0.25 * feel_score - constraint_penalty
readiness = clamp(0, 100, round(raw))
recovery = clamp(0, 100, round(0.5 * sleep_score + 0.5 * load_score))
```

`fatigue_by_muscle` JSON：`{ "QUAD": 3.1, "SHOULDER": 7.4 }` 仅含 >0 的肌群。

`source`：P0 恒 `rule`。  
触发计算：wellness 写入后；课 `COMPLETED` 后；教练 Run 开始时若当日尚无 snapshot 则先算再 Observe。

---

## 4. body_part → 禁止 pattern

| body_part | exclude_patterns |
|-----------|------------------|
| SHOULDER | VERTICAL_PUSH |
| ELBOW | HORIZONTAL_PUSH, VERTICAL_PUSH, ISOLATION（臂） |
| WRIST | HORIZONTAL_PUSH, VERTICAL_PUSH, HORIZONTAL_PULL |
| NECK | VERTICAL_PUSH |
| LOWER_BACK | HINGE, SQUAT（若 severity≥4 则两条都禁；&lt;4 只禁 HINGE） |
| KNEE | SQUAT, LUNGE |
| HIP | SQUAT, HINGE |
| ANKLE | LUNGE, SQUAT |
| OTHER | 无自动排除，只减 readiness |

`severity` 缺省当 3。`ISOLATION` 臂：catalog.muscle_group ∈ {BICEP,TRICEP,FOREARM}。

---

## 5. 护栏（代码里按序执行，全部硬失败或改写）

**G1 伤痛排除**  
Retrieve 与最终 slots 的 `pick/alternatives` 不得含 exclude_patterns 下的 code。违者从列表删除；删空则该 slot 失败。

**G2 过低准备度**  
`readiness < 40` **或** `avg_fatigue ≥ 8` → 禁止 `kind=SESSION`。只允许 `REST` 或 `DELOAD`（DELOAD：slots≤3 且均为 ISOLATION/CORE，且组数≤2）。否则 `3008` 或由编排器改写为 REST advice（P0 推荐改写，不抛给用户 3008）。

**G3 同肌群过密**  
某肌群上次 **已完成课** 中出现过，且距今 &lt; 48h，则该肌群不得作为 **slot 的主刺激**（`goal_muscle` 第一项）。可作 alternatives 降权。P0 简化：48h 内练过的 muscle 不得出现在 `pick`（可以在 alternatives）。

**G4 医疗**  
存在 MEDICAL 约束 → 只出 `REST`，`rationale` 固定含：不诊断、建议就医。禁止 session。

**G5 code 封闭集**  
`pick` 必须 ∈ 当次 `retrieve_exercise_options` 返回的 code 集合。`alternatives` 必须 ∈ catalog 且同 `swap_group`（可与 pick 相同组）。模型输出未知 code → **丢弃该 slot**，不得写入 advice。

护栏在 LLM 产出 JSON **之后**、落 `advice` 行 **之前**。失败则：能降级为 REST 就降级；否则 SSE `error` `3008`。

---

## 6. 单测金样（必须写进 `readiness` 模块测试）

**样例 A** 睡眠 8h，主观疲劳 4，无训练无约束 → sleep=100, load=100, feel=60 → **90**（允许 ±1）。

**样例 B** 睡眠 4h，疲劳 9，无训练 → sleep=20, feel=10, load=100 → raw=0.4*20+0.35*100+0.25*10=45.5 → 46。

**样例 C** SHOULDER+PAIN，retrieve 含垂直推 code → G1 后该 code 不得出现在 pick。

**样例 D** readiness 算得 30 → 最终 advice.kind 不得为 SESSION。
