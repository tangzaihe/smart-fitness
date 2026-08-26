# 新会话开工 Prompt（Expo 移动端）

> 复制下方「--- PROMPT START ---」到「--- PROMPT END ---」整段，粘贴到新 Cursor 会话即可。  
> 工作区：`E:\aseantec\agent\Smart Fitness`（后端已就绪；移动端尚未创建）  
> 后端 API 文档：`http://localhost:8080/v3/api-docs`（Swagger UI：`http://localhost:8080/swagger-ui.html`）

---

## --- PROMPT START ---

你是 Smart Fitness 项目的 **Expo 移动端**实现工程师。请在当前 monorepo 中新建 `smart-fitness-mobile/` 目录，从零搭建 App，对接**已运行的本地后端**，严格按冻结设计文档施工。

### 现状

- 后端 Spring Boot 已启动：`http://localhost:8080`
- OpenAPI：**以 `http://localhost:8080/v3/api-docs` 为运行时契约**（优先于文档文字）
- CORS 已允许：`http://localhost:8081`、`http://localhost:19006`（见 `SecurityConfig`）
- 仓库内 **尚无** `package.json` / Expo 工程

### 必读文档（顺序）

1. `docs/01-app-prototype.md` — 信息架构、页面流、线框、SSE 状态机  
2. `docs/08-contracts.md` — 统一响应、错误码、枚举、SSE 事件、advice.payload  
3. `docs/10-session-lifecycle.md` — decide → 记组 → complete  
4. `docs/09-readiness-guardrails.md` — 准备度展示（calc_version、肌群疲劳）  
5. `docs/04-llm-usage-monitoring.md` — 「我的」页用量展示  
6. `docs/06-p0-kickoff.md` — P0 范围与验收  
7. 线框参考图：`docs/assets/app-coach-home-prototype.png`、`docs/assets/app-session-live-prototype.png`

**冲突时**：OpenAPI > 08 契约 > 01 原型文案。

### 技术栈（冻结）

- **Expo 单仓**（禁止 Flutter / 第二移动端仓库）
- TypeScript、**Expo Router**（file-based routing）
- `expo-secure-store` 存 access/refresh token
- 网络：`expo/fetch`（SSE 必须 ReadableStream，禁止整包 `response.text()`）
- 状态：TanStack Query + 轻量 Zustand（或等价方案，勿过度抽象）
- UI：NativeWind 或 StyleSheet 二选一；深色运动风，对齐 `docs/assets` 原型

### 目录建议

```text
smart-fitness-mobile/
  app/                    # expo-router
    (auth)/login.tsx
    (auth)/register.tsx
    onboarding.tsx
    (tabs)/
      coach/index.tsx
      training/index.tsx
      body/index.tsx
      profile/index.tsx
  src/
    api/                  # client、types（可从 OpenAPI 生成或手写）
    auth/                 # token、refresh、logout
    coach/                # SSE parser、advice 状态机
    session/
    types/
  .env.example            # EXPO_PUBLIC_API_BASE_URL
```

`EXPO_PUBLIC_API_BASE_URL` 默认 `http://localhost:8080`；真机调试改成本机局域网 IP。

### 后端 API 面（P0 必须对接）

| 域 | 方法 | 路径 | 说明 |
|----|------|------|------|
| auth | POST | `/v1/auth/register` | email + password |
| auth | POST | `/v1/auth/login` | 返回 `TokenVO` |
| auth | POST | `/v1/auth/refresh` | body: `{ refreshToken }` |
| auth | POST | `/v1/auth/logout` | 可选 refresh |
| athlete | GET/PUT | `/v1/athlete/me` | 建档 / 更新约束 |
| wellness | PUT | `/v1/wellness/today` | `{ sleepHours, subjectiveFatigue }` |
| readiness | GET | `/v1/readiness/current` | 身体 Tab |
| coach | POST | `/v1/coach/runs?trigger=` | **SSE**；鉴权 Header Bearer |
| advice | POST | `/v1/advice/{id}/decide` | `{ action: "ACCEPT"\|"REST"\|"REJECT" }` |
| session | GET | `/v1/sessions/current` | 进行中课，无则 null |
| session | GET | `/v1/sessions/{id}` | 详情 + sets |
| session | GET | `/v1/sessions` | 历史分页 |
| session | PATCH | `/v1/sessions/{id}/sets/{setId}` | 记组 |
| session | POST | `/v1/sessions/{id}/complete` | 结束 |
| session | POST | `/v1/sessions/{id}/abandon` | 放弃 |
| usage | GET | `/v1/me/usage` | 近 7 日 token |

统一响应：`{ code: 0, message: "ok", data: T }`。`code !== 0` 按 `08` 错误码表展示；`2001/2002` 触发 refresh 或回登录。

`TokenVO` 字段：`accessToken`, `refreshToken`, `expiresIn`, `athleteId`, `onboarded`。

### 路由与导航（对齐 01）

```text
未登录 → 登录/注册
已登录 && !onboarded → 建档（一次性）
已登录 && onboarded → Tab：教练* | 训练 | 身体 | 我的
```

- **教练**为默认 Tab；冷启动可自动 `POST /v1/coach/runs?trigger=OBSERVE`（轻量 Observe）
- 有 `IN_PROGRESS` session 时，训练 Tab 显示「继续训练」

### SSE 客户端（核心）

`POST /v1/coach/runs`，`Accept: text/event-stream`，`Authorization: Bearer <accessToken>`。

实现 `parseSseStream(reader)`，按事件名分发：

| event | 处理 |
|-------|------|
| `run.created` | 保存 `runId`；UI → running |
| `token` | 追加 `data.delta` 到流式气泡 |
| `tool.start` / `tool.result` | Chip：「正在看你的恢复」等（字段可能是 `tool` 或 `name`，兼容两者） |
| `advice` | 解析 `adviceId`、`kind`、`payload`（schema_version=1, slots, alternatives）→ awaiting_confirm |
| `error` | 展示 message/code；保留已生成 advice |
| `done` | 收尾 → idle |

**注意（与 08 文档差异，以实际后端为准）**：

- `tool.start`/`tool.result` 当前 payload 用 `tool: "observe"|"retrieve"`  
- `advice` 事件字段为 `kind`（非 `type`）；advice 库内 status=`PENDING`  
- P0 后端 **尚无** 用户自由文本入参；教练回合通过 `trigger` 查询参数触发，输入框可先 UI 占位或触发新 run

未知 SSE 事件：**忽略，不断流**。

### Advice 卡 UI

渲染 `payload.slots[]`：`pick`、`alternatives[]`、`sets`×`reps`、`loadKg`、`rpeCap`。

按钮：

- **采纳并开始** → `decide` action=`ACCEPT` → 拿 `sessionId` 跳训练页  
- **只要休息** → `REST`  
- **忽略** → `REJECT`

`kind=REST` 时 slots 为空，仅展示 rationale。

### 训练页

- `GET /v1/sessions/current` 拉 sets  
- 当前组 `PATCH`：`{ reps, loadKg, rpe, completed: true }`  
- 完成 → `POST .../complete`；可 abandon  
- **不通过 LLM 生成重量**；顶部只读展示 advice rationale

### 身体 / 我的

- 身体：`GET /v1/readiness/current` — 展示 readiness、calcVersion、fatigueByMuscle（JSON）、sleepHours  
- 我的：档案只读/编辑跳转、`GET /v1/me/usage` 用量条；`3005` 配额用尽时教练输入禁用并提示

### 四条红线（与后端一致）

1. 确认 Advice 后才进入记组（decide ACCEPT）  
2. SSE 按 **runId** 理解，不做 user 级广播假设  
3. Token 只放 `Authorization` Header，不放 query  
4. 错误码与后端 `08` 一致处理，不吞 409

### P0 不做

- Admin Vue、BYOK 绑定 UI、向量 RAG、手表同步、成就排行、端上 Prompt 编辑  
- Flutter 第二仓  
- 脱离教练的「纯记训」独立入口

### 实施顺序

| 阶段 | 内容 |
|------|------|
| **F1** | `npx create-expo-app`、Router、API client、Result 解包、401 refresh、`.env.example` |
| **F2** | 登录/注册 + SecureStore + onboard 表单（goal/equipment/constraints 枚举见 08） |
| **F3** | 教练 Tab：准备度卡 + SSE 流式 + Advice 卡 + decide |
| **F4** | 训练 Tab：current session、记组、complete/abandon |
| **F5** | 身体 Tab + 我的 Tab（wellness 表单、usage） |
| **F6** | 错误态、空态、3005/3007 提示、真机 API_BASE 说明 |

每阶段确保 `npx expo start` 可跑；优先 Web（8081）联调，再验 Android/iOS 模拟器。

### 类型与 OpenAPI

启动时拉取 `http://localhost:8080/v3/api-docs`，为上述 DTO 生成或手写 TypeScript 类型；枚举与 `08` 第三节对齐。

### 交付物

1. 完整 `smart-fitness-mobile/` 可运行工程  
2. README：安装、`.env`、联调后端、模拟器 localhost 注意点  
3. 不要 git commit，除非我明确要求  

请先：读取 OpenAPI + `docs/01` + `docs/08`，输出你理解的导航结构、API 模块划分与 F1 文件清单，确认后开始 F1 脚手架。

## --- PROMPT END ---
