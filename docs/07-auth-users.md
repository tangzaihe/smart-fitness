# 鉴权与用户

> 冻结：`app_user` 与 `athlete` 1:1。App JWT 与 Admin JWT 隔离。  
> 日期：2026-08-23

---

## 1. 表

```sql
CREATE TABLE app_user (
    id              BIGINT PRIMARY KEY,
    email           VARCHAR(128) NOT NULL,
    phone           VARCHAR(20),
    password_hash   VARCHAR(255) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);
CREATE UNIQUE INDEX uk_app_user_phone ON app_user (phone) WHERE phone IS NOT NULL;

-- athlete.user_id 对应 app_user.id，UNIQUE，注册成功后立即插 athlete 空档（goal 可暂 STRENGTH，建档 PUT 补全）
```

`athlete` 在注册事务内创建：`display_name` 默认邮箱前缀；`goal=STRENGTH`；`equipment='[]'`；`preferences='{}'`。未完成建档标记：`athlete.onboarded_at TIMESTAMPTZ NULL`（**本包增列**）。

```sql
ALTER TABLE athlete ADD COLUMN onboarded_at TIMESTAMPTZ;
-- onboarded_at IS NULL → App 强制建档页；教练 API 返回 3001
```

Admin：已有 `admin_user`，禁止用 `app_user` 登录管理端。

---

## 2. 密码与 JWT

- 密码：BCrypt，强度 10；注册体校验 8–64 位，至少 1 字母 1 数字  
- Access：HS256，**2h**，claim：`sub`（userId 字符串）、`typ=access`、`aud=app`  
- Refresh：HS256，**14d**，`typ=refresh`，`aud=app`，`jti` 唯一  
- 密钥：`APP_JWT_SECRET` ≥ 32 字节，无默认生产值  
- 登出：`jti` 进 Redis 黑名单，TTL = 剩余有效期  
- 刷新：旧 refresh 旋转（旧 `jti` 拉黑），下发新一对  

Admin：`ADMIN_JWT_SECRET` 不同；`aud=admin`；access 8h。

请求头：`Authorization: Bearer <access>`。SSE **禁止** query 传 token。

---

## 3. API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/v1/auth/register` | 否 | email + password（+ 可选 phone） |
| POST | `/v1/auth/login` | 否 | email 或 phone + password |
| POST | `/v1/auth/refresh` | 否 | body: `{ "refreshToken" }` |
| POST | `/v1/auth/logout` | Access | 拉黑当前 access + 可选 refresh jti |
| GET/PUT | `/v1/athlete/me` | Access | 建档；PUT 成功则写 `onboarded_at` |

注册/登录响应 `data`：

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 7200,
  "onboarded": false,
  "athleteId": "123"
}
```

`UserContext`：ThreadLocal 放 `userId` + `athleteId`（Filter 里一次查 athlete）。所有业务表按 `athleteId` 过滤。

---

## 4. 校验码

P0 **不做**短信/邮箱验证码。防刷：登录失败 Redis 计数，10 次 / 15min 锁定返回 `2003`。
