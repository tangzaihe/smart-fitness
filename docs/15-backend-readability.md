# 后端代码可读性基准

> 后续所有 Java（含 Agent 生成）必须遵守。Cursor 规则：`.cursor/rules/java-readability.mdc`。  
> 日期：2026-08-23  
> 原则：**注释解释职责、不变量和副作用；命名解释做什么。禁止用注释复述代码。**

与「少写废话注释」不冲突：缺少类职责、字段合法值、写库副作用的代码，在本仓库视为不合格。

---

## 1. 分层读者预期

打开一个文件，应在 **10 秒内** 知道：

| 类型 | 必须立刻看到 |
|------|----------------|
| Entity | 对应哪张表、在主闭环哪一环、关键状态机 |
| Service | 对外能力、写哪些表、失败用哪些 `ErrorCode` |
| Controller | 路径、鉴权、调用哪个 Service |
| Port / Gateway | 实现方不得越过的边界（如 LLM 不碰 Mapper） |
| Enum | 每个常量的业务含义，不只是英文翻译 |

---

## 2. 类 Javadoc（public 必写）

模板：

```java
/**
 * <一句话职责>。对应表 {@code table_name} / 接口 {@code METHOD /path}。
 * <p><不变量或生命周期：谁创建、谁更新、禁止什么。>
 */
```

要求：

- 写 **对应表名** 或 **API 路径**，方便和 Flyway / 08 契约对照。
- 写 **一条不变量**（例：`athletic_state` 只插入不更新；`advice` 非 ACCEPTED 不得建课）。
- 模块对外 Port 写清「只给谁用」（例：`KnowledgePort` 仅 coach 调用）。

DTO / VO：一行即可（「建档请求体，对应 `PUT /v1/athlete/me`」）。不要每个 getter 再写一遍。

---

## 3. 方法 Javadoc（public 必写）

Lombok 生成的 getter/setter、纯 Mapper CRUD **不写**。

其余 public 方法必须包含：

1. **做什么**（业务结果，不是步骤清单）
2. **副作用**（插入/更新哪些表；发 SSE；记 usage）
3. **@param / @return** 只补充命名看不出来的约定（空串 vs null、幂等字段）
4. **@throws BizException** 列出 `ErrorCode` 常量名

```java
/**
 * 完课：课次改为 COMPLETED，并插入一条新的准备度快照。
 *
 * @throws BizException {@code SESSION_STATE} 非 IN_PROGRESS；{@code NOT_FOUND} 非本人课次
 */
```

private 方法：仅在 **非显然**（护栏编号、幂等、时区、JSON 形状）时用一行 `//` 说明 **为什么**。

---

## 4. Entity 字段

| 写 | 不写 |
|----|------|
| 合法枚举：`IN_PROGRESS \| COMPLETED \| ABANDONED` | `// 状态` |
| 单位：`loadKg` 千克、金额 `*Minor` 为分 | `// 重量` |
| JSON 列：简述 shape（slots[].pick） | 把整份 Schema 贴进注释 |
| 可空语义：`adviceId` 空表示非处方课（P0 不应出现） | 列名的中文直译 |
| 逻辑外键：`exerciseCode` → `exercise_catalog.code` | |

`id` / `createdAt` / `updatedAt` 只在 `BaseEntity` / `AuditedEntity` 写一次。

状态、trigger、period 等字符串，优先抽 enum 或常量类；P0 已是 VARCHAR 的，必须在字段注释写死合法集合。

---

## 5. 结构可读（比堆注释更重要）

- public 方法 **建议 ≤ 80 行**；超出则按步骤拆 private（`assertCanDecide` / `insertDecision` / `openSession`）。
- 禁止深层嵌套 > 3 层； capturable 条件提前 return。
- 同一魔法字符串出现 2 次以上 → 常量。
- 包内职责固定：`entity` / `mapper` / `service` / `dto|vo`；Controller 只在 `smart-fitness-api`。
- 业务模块 **禁止互注 Mapper**；coach 只调其他模块 Service/Port。

---

## 6. Controller 与 OpenAPI

- 类：`@Tag` 用业务名；类 Javadoc 写鉴权（需登录 / 需已建档）。
- `@Operation(summary)` 用中文或完整英文短语，禁止 `register`、`login` 这种零信息词。
- `description` 写清与 08 不一致的行为（如 ACCEPT 幂等返回已有 `sessionId`）。

---

## 7. 反例 → 正例

```java
// ❌ 不合格
@TableName("advice")
public class Advice extends BaseEntity {
    private String status;
}

public Map<String, Object> decide(...) {
    // 改状态
    advice.setStatus("ACCEPTED");
    // 写库
    adviceMapper.updateById(advice);
    return Map.of("status", advice.getStatus());
}
```

```java
// ✅ 合格
/**
 * 待用户确认的教练处方。对应表 {@code advice}。
 * <p>护栏通过后才插入；仅 ACCEPTED 且类型为课表/减载时才创建 {@code session_log}。
 */
@TableName("advice")
public class Advice extends BaseEntity {
    /** {@code PENDING} 待确认；{@code ACCEPTED} 已采纳；{@code REJECTED} 已拒绝。 */
    private String status;
}

/**
 * 用户确认处方。重复 ACCEPT 已确认单返回原 sessionId，不新建课。
 *
 * @throws BizException {@code NOT_FOUND}；{@code ADVICE_NOT_DECIDABLE} 当前不可决
 */
@Transactional
public Map<String, Object> decide(...) { ... }
```

---

## 8. 生成代码检查单

Agent 在声明「后端改完」前必须自检：

- [ ] 每个新建/改动的 public 类有职责 Javadoc（含表名或路径）
- [ ] 每个新建/改动的 public 方法有副作用 + ErrorCode
- [ ] Entity 新字段有合法值或单位
- [ ] 无「设置/返回/循环」类废话注释
- [ ] 未把 LLM 逻辑写进 Mapper；未在未确认 advice 时插 `session_log`

存量文件以 `entity/` 与 `api/controller`、`*/service` 为样板；新代码不得低于样板。
