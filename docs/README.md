# Smart Fitness 设计文档

绿场重启，产品核为 Agent 教练。移动端只做 Expo 单仓。

**后端开工以 06–12 为冻结契约**；01–05 为产品/架构方向。冲突时改方向文档，不改已冻 Schema。

| 文档 | 内容 |
|------|------|
| [01-app-prototype.md](01-app-prototype.md) | App 信息架构、页面流、线框 |
| [02-backend-architecture.md](02-backend-architecture.md) | 模块、ER、DDL 草案、API 面 |
| [03-admin-console.md](03-admin-console.md) | 管理端功能与页面 |
| [04-llm-usage-monitoring.md](04-llm-usage-monitoring.md) | Token 账本、配额、BYOK |
| [05-training-rag.md](05-training-rag.md) | RAG 扩展点 |
| [06-p0-kickoff.md](06-p0-kickoff.md) | **开工包总览、范围、验收、排期** |
| [07-auth-users.md](07-auth-users.md) | 用户表、JWT |
| [08-contracts.md](08-contracts.md) | JSON Schema、错误码、幂等 |
| [09-readiness-guardrails.md](09-readiness-guardrails.md) | 准备度 v1、护栏 G1–G5 |
| [10-session-lifecycle.md](10-session-lifecycle.md) | 课次状态机 |
| [11-catalog-seed.md](11-catalog-seed.md) | 30 个动作种子 |
| [12-engineering-bootstrap.md](12-engineering-bootstrap.md) | Maven 模块、Flyway、Compose |
| [13-backend-implementation-prompt.md](13-backend-implementation-prompt.md) | 后端新会话开工 Prompt |
| [14-mobile-implementation-prompt.md](14-mobile-implementation-prompt.md) | **Expo 移动端新会话开工 Prompt** |
| [15-backend-readability.md](15-backend-readability.md) | **Java 可读性基准**（类/方法/实体注释，后续生成必须遵守） |
| [16-coach-agent-prd.md](16-coach-agent-prd.md) | **教练 Agent 重设计 PRD**（CoachPlan、今日 Tab、对话与分阶段交付） |

原型图在 [assets/](assets/)。

**方向性修订：** [16-coach-agent-prd.md](16-coach-agent-prd.md) 评审通过后，取代 [01-app-prototype.md](01-app-prototype.md) 中教练 Tab / 自动 Observe 相关描述；冻结 Schema 仍按 06–12 流程增补。
