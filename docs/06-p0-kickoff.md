# P0 开工包（后端）

> 本包补齐「可排期施工」缺口。补完前的 01–05 是方向文档；**06–12 是冻结契约**。  
> 日期：2026-08-23  
> 状态：文档冻结后才宣布后端开工。

---

## 1. 总负责人冻结范围

P0 **要做出可验收闭环**（垂直切片 + 可接真模型）：

1. 注册登录 → 建档（器材/偏好/伤痛）→ 写入当日 wellness  
2. `POST /v1/coach/runs` SSE → Observe + retrieve → 护栏 → `advice`（含 alternatives）  
3. `decide=accept` → `session_log IN_PROGRESS` + 计划组 `set_log`  
4. 记组 → `complete` → 重算 `athletic_state`  
5. 每次 LLM 写 `llm_call_usage`（可用 FakeLlm 先跑通，接真模型当天必须落账）  
6. 系统日配额拦截（数字见 [08](08-contracts.md) / [04](04-llm-usage-monitoring.md)）

P0 **本包明确不做**（写进范围，避免施工中膨胀）：

- BYOK 绑定与密钥加密（表可建，API 返回 501 或隐藏；**第二刀**再做）  
- Admin Vue 全套（可先 Swagger + SQL；Admin API 骨架可并行但非切片阻断）  
- 向量 RAG、手表、身体成分流水、课阶段 block、饮食  
- 用户付费套餐

第一刀允许用 `FakeLlmGateway`（固定 JSON advice），但 `LlmGateway` 接口、usage 写入、配额检查必须在，换真模型只换适配器。

---

## 2. 文档地图

| 文档 | 冻结内容 |
|------|----------|
| [07-auth-users.md](07-auth-users.md) | `app_user`、JWT、注册登录 |
| [08-contracts.md](08-contracts.md) | JSON Schema、错误码、幂等、并发 |
| [09-readiness-guardrails.md](09-readiness-guardrails.md) | 准备度公式 `calc_version=v1`、护栏 G1–G5 |
| [10-session-lifecycle.md](10-session-lifecycle.md) | 课次状态机、decide → 记组 → 结束 |
| [11-catalog-seed.md](11-catalog-seed.md) | 枚举 + 30 个动作种子 |
| [12-engineering-bootstrap.md](12-engineering-bootstrap.md) | 模块目录、版本、Flyway、Compose |

施工时冲突：以 **06–12 为准**，01–05 冲突则改 01–05 而不是改代码形状。

---

## 3. 验收（切片 Done）

- [ ] `docker compose` 起 Postgres 16 + Redis 7，Flyway 升到含 user/athlete/catalog 种子  
- [ ] 注册用户可建档；无 wellness 当日数据时准备度按缺省规则算（见 09）  
- [ ] 一场 SSE：至少发出 `run.created` / `advice` / `done`；FakeLlm 也写 usage 行（`usage_source=ESTIMATED` 可）  
- [ ] 确认后有且仅有一堂 `IN_PROGRESS`；记 1 组后 complete；`athletic_state` 多一行 `calc_version=v1`  
- [ ] 肩痛约束下 `advice` 不含 `vertical_push` 动作 code  
- [ ] 同一 `adviceId` 重复 decide → 幂等成功，不建第二堂课  
- [ ] OpenAPI 与 08 的 Schema 一致（SpringDoc）

---

## 4. 建议排期（后端）

| 顺序 | 内容 | 依赖 |
|------|------|------|
| D1 | 工程骨架 + Flyway + Fake 健康检查 | 12 |
| D2 | auth + athlete + wellness | 07、08 |
| D3 | catalog 种子 + KnowledgePort SQL | 11、05 |
| D4 | session 状态机（可先无 LLM） | 10 |
| D5–D6 | Coach Run + FakeLlm + 护栏 + usage | 08、09、04 |
| D7 | 接真模型（OpenAI 兼容）+ include_usage | 04 |
| 其后 | Admin API / 配额看板 / BYOK | 03、04 |

未完成 D1–D6 验收项，不算后端开工完成。
