# 工程脚手架（后端开工用）

> 本文是仓库落地说明，**不代替**业务代码。开工第一天按此建模块与 Compose。  
> 日期：2026-08-23

---

## 1. 坐标

| 项 | 值 |
|----|-----|
| GroupId | `com.aseantec` |
| 根 Artifact | `smart-fitness` |
| 包名 | `com.aseantec.smartfitness` |
| Java | 21 |
| Spring Boot | 3.3.5 |
| LangChain4j | BOM `1.0.x`（开工当日锁具体版；**禁止**引入 Spring AI） |
| ORM | MyBatis-Plus 3.5.9 |
| DB | PostgreSQL 16 |
| 迁移 | Flyway |
| Redis | 7，Redisson |
| 校验 | Hibernate Validator |
| API 文档 | SpringDoc 2.6.x |
| 构建 | Maven 多模块 |

Lombok 允许（与 Training Log 一致）：`@Data` / `@Builder` / `@RequiredArgsConstructor`。

---

## 2. 模块

```text
smart-fitness/
  pom.xml
  docker/docker-compose.yml
  smart-fitness-common
  smart-fitness-infra
  smart-fitness-auth
  smart-fitness-athlete
  smart-fitness-readiness
  smart-fitness-training
  smart-fitness-knowledge
  smart-fitness-coach
  smart-fitness-admin
  smart-fitness-api          ← 唯一可启动
```

依赖方向：`api` → 各业务；业务 → `infra` + `common`；**业务模块之间不互依 Mapper**。  
`coach` 依赖各模块 **Service 接口**（接口可放 common 或各模块 api 包）。  
`knowledge` 只被 `coach` 使用。

Flyway 脚本仅放 `smart-fitness-api/src/main/resources/db/migration/`：

- `V1__init.sql` — 02 + 04 + 07 + 08 的表（含 `app_user`、`onboarded_at`、`wellness_log`）  
- `V2__catalog_seed.sql` — 11 的 30 行  
- `V3__seed_policy_and_quota.sql` — 默认 `coach.default` v1 人设草稿 + 全局配额

---

## 3. docker-compose（开发）

服务名建议：`postgres` 5432 库 `smartfitness`；`redis` 6379。网络 `sfnet`。  
环境变量：`DB_HOST` `DB_PASSWORD` `REDIS_HOST` `APP_JWT_SECRET` `ADMIN_JWT_SECRET` `LLM_BASE_URL` `LLM_API_KEY` `LLM_MODEL`。

应用端口 8080。健康检查：`GET /v1/health` 无需登录。

本地 FakeLlm：`LLM_PROVIDER=fake` 时不访问外网，usage 仍插入，prompt/completion 用估算。

---

## 4. 包内分层（每个业务模块）

```text
.../{module}/
  entity/
  mapper/
  service/ + impl/
  port/          ← 对外接口（给 coach 用）
```

Controller 只放 `api` 与 `admin`（admin 控制器可在 admin 模块，由 api 扫描 `com.aseantec.smartfitness`）。

统一：`Result<T>`、`BusinessException`、`ErrorCode` 枚举（数字与 08 一致）。

---

## 5. 必须先写的接口（避免未开工就乱 new Model）

```java
public interface LlmGateway {
    TokenStream chatStream(LlmRequest req); // 内部写 usage
}

public interface KnowledgePort {
    RetrieveResult retrieve(RetrieveQuery query);
}
```

第一周 `FakeLlmGateway` + `CatalogKnowledgeAdapter`。

---

## 6. 配置

`application-dev.yml`：profile `dev` 连 compose。  
禁止把密钥提交进 git。`.env` 进 `.gitignore`。

CORS：`CORS_ALLOWED_ORIGINS`（Expo 开发机 IP / localhost）。

日志：`com.aseantec.smartfitness=DEBUG`；禁止打印 Authorization、api_key、密码。

---

## 7. 测试最低线

- `readiness`：09 节金样 A–D  
- `decide` 幂等  
- G1 肩痛不含 VERTICAL_PUSH pick  
- Flyway `test` profile 用 Testcontainers 或 H2 不强制；优先 Testcontainers PostgreSQL

---

## 8. 开工第一天检查单

- [ ] 父 POM 能 `mvn -pl smart-fitness-api -am package -DskipTests`  
- [ ] compose up 后 Flyway 成功  
- [ ] `/v1/health` 200  
- [ ] 注册+登录拿到 token  

完成后进入 06 的 D2。

---

## 9. 可读性基准（强制）

后续所有 Java（含 Agent 生成）必须遵守 [15-backend-readability.md](15-backend-readability.md)。Cursor 规则：`.cursor/rules/java-readability.mdc`。

实体样板：`**/entity/*.java`（类注释含表名 + 不变量，业务字段含合法值）。
公开 API 样板：`smart-fitness-api/.../controller/*`、`AdviceService`、`CoachRunService`。

