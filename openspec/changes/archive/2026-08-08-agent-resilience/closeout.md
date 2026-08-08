# Closeout：Agent Resilience（C8）

## 1. 结论

- Change：`agent-resilience`（Phase 2 第三刀 C8）
- 开工锚点：`fb68082`
- 闸门：规划、实现、真实 provider / MySQL 验收与归档均获用户授权
- 归档日期：2026-08-08
- 结果：封闭失败分类、共享 provider-work deadline、零自动 retry、阶段化失败呈现与无内容轨迹已落地并接受进 baseline

## 2. 已交付能力

- 8 类封闭 provider failure taxonomy；核心分类基于异常类型和 HTTP status，不解析自由文本
- request-scope `AgentCallBudget`：默认 24000ms，单次调用取 `min(20000ms, remaining)`，不足阈值不发请求
- initial / reflection / material 共用预算；不新增自动 retry，保留用户主动同轮重试
- provider failure 保持 `FAILED` / `UNAVAILABLE`，固定温暖模板不冒充 Assistant 正常回复
- trace 仅记录 phase/category/transient/budget/终态；不记录用户内容、prompt、provider body 或 exception message
- API/DTO/frontend 字段、DDL、依赖、lockfile、部署与监控均未扩张

## 3. 验收证据

### 离线与本地回归

- C8 focused classifier/budget/policy/client/pipeline/service/trace/eval/contract：PASS
- 后端最终全量：**81 suites / 645 tests / 0 failures / 0 errors / 6 skipped**；其中新增两个闸门探针默认关闭，故默认 skipped 由 4 增至 6
- 前端 type-check 与 `build:mp-weixin`：PASS；本刀未改 frontend source
- `git diff --check`、范围路径与敏感标记扫描：PASS
- OpenSpec CLI：**SKIPPED**；CLI 不在 PATH，改做 delta/baseline/archive 文件级核对

### 闸门 3：真实 provider

- provider/model：DeepSeek / `deepseek-v4-pro`
- 数据：仅固定合成短文本，无用户日记、真实对话、文件或 secret 入证据
- canary：1378ms、1656ms，2/2 PASS
- 双调用：2898ms、3531ms，2/2 组 PASS
- 总调用：6/6 成功（预算上限 8）；均在 24000ms provider-work budget 与 frontend 30000ms 窗口内
- 未主动制造鉴权失败、限流或 outage；未触发停止条件

### 真实 MySQL

- 使用固定合成数据验证 provider 首次不可用后用户主动重试
- 同一 turn 仅一条 USER message；attempt 1=`UNAVAILABLE/auth-configuration`，attempt 2=`SUCCESS`
- 合成数据在 `finally` 中清理：PASS

### 微信真机

- **SKIPPED**：本机未发现微信开发者工具或可控真机环境
- 不以 scripted provider、H2、构建或桌面浏览器冒充真机 UI 证据

## 4. Delta 接受

- `agent-runtime`：失败 taxonomy、共享 deadline、零自动 retry、失败/护栏分流、阶段化文案、无内容观测
- `backend-core`：超时顺序、类型化 client 边界、request-scope budget、稳定 API、material 可选失败、零 schema/依赖扩张
- `miniapp-core`：显式失败态、基础设施信息隔离、pending turn 恢复与零 UI 扩张
- `agent-collaboration`：无内容证据、离线故障注入、真实探针独立预算闸与范围守护
- `v2-product-scope`：无 delta

## 5. 保留风险与后续边界

- 真实 provider 仅 2 次 canary + 2 组双调用，是小样本链路验收，不是生产 SLA
- 微信真机错误卡片与主动重试体验尚无活体证据
- 多 provider、自动 retry、熔断、缓存、队列、监控与部署均未实现；需要证据触发独立 change
- C9 Temporal 仍须单独规划闸，不因 C8 归档获得实现授权

## 6. 提交与外部副作用

- Commit：pending（本归档提交后以 Git 事实为准）
- 未 push、未部署、未发布
