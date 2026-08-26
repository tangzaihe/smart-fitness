# Smart Fitness Mobile

Expo + TypeScript 客户端，对接本地 `smart-fitness-api`。P0 只保证这条垂直切片：

`注册/登录 → 建档 → 今日 wellness → SSE 建议 → 确认 → 记组 → 完成`

四个 Tab 都有页面，但身体/我的只覆盖联调必需能力，不追求 01 文档里的全部交互。

## 环境

- Node 20+
- 后端已在 `http://localhost:8080` 运行
- CORS 已允许 `http://localhost:8081`、`http://localhost:19006`

## 安装

```bash
cd smart-fitness-mobile
cp .env.example .env
npm install
```

`.env`：

```
EXPO_PUBLIC_API_BASE_URL=http://localhost:8080
```

真机调试请改成电脑局域网 IP，例如 `http://192.168.1.12:8080`。Android 模拟器常用 `http://10.0.2.2:8080`。

## 启动

优先 Web 联调：

```bash
npx expo start --web --port 8081
```

Expo 默认也可能走 `19006`。后端 CORS 两个端口都放行了。

iOS / Android：

```bash
npx expo start
```

然后按 `i` / `a`，或扫码进 Expo Go。

## 联调顺序

1. 注册：邮箱 + 密码（至少 8 位，含字母和数字）
2. 建档：目标、器材、可选伤痛
3. 填写今日睡眠和主观疲劳
4. 教练页自动 `OBSERVE`，走 Fake LLM SSE
5. 确认建议后进入训练页
6. 记一组并结束本课

当前后端 P0 没有用户自由文本入参。教练页输入框只是占位，真正开回合用「再观察一次 / 手动触发」。

后端可用 Fake LLM：`LLM_PROVIDER=fake`。联调顺序：Auth → 建档 → wellness → SSE advice → decide → 记组 complete。

## 目录

```
app/                Expo Router
src/api/            Result 解包、401 refresh、SSE
src/auth/           SecureStore / web localStorage
src/coach/          SSE 解析与回合状态
src/components/     准备度卡、建议卡
```

Token 只放 `Authorization` Header。未知 SSE 事件会忽略，不断流。
