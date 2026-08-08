# Closeout：Agent Temporal Intelligence（C9）

## 1. 结论

- Change：`agent-temporal-intelligence`（Phase 2 第四刀 C9）
- 开工锚点：`544e9ea`
- 闸门：规划、实现、真实 provider / MySQL 验收与归档均获用户授权
- 归档日期：2026-08-08
- 结果：确定性时间距离、旁支记忆衰减、证据门控的克制重复主题提示与反分析越界检查已落地并接受进 baseline

## 2. 已交付能力

- backend `agent/temporal` 使用注入 `Clock` 计算 `RECENT / DISTANT / LONG_AGO / UNKNOWN`，边界为 30/180 天
- 旁支记忆按 100% / 75% / 50% 字符预算衰减且最低 40 字；用户主动打开的回看目标记录不衰减
- recurrence 仅允许在 `REVIEW_CHAT`、用户明确比较、至少两条不同旁支记录且跨度不少于 90 天时形成一次克制提示
- `TEMPORAL_OVERREACH` 对量化、绝对规律、因果、趋势、诊断与预测式话术 fail closed，且不扩张 C7 reflection
- 时间策略零额外 provider 调用，不修改 API/DTO/DDL/mapper SQL/frontend surface，不持久化 temporal hint

## 3. 验收证据

### 离线与本地回归

- C9 focused policy / prompt / checker / pipeline / trace / eval：PASS
- 后端最终全量：**87 suites / 664 tests / 0 failures / 0 errors / 8 skipped**；两个新增闸门探针默认关闭，故 skipped 由 6 增至 8
- 前端 type-check 与 `build:mp-weixin`：PASS；本刀未改 frontend source
- `git diff --check`、范围路径与敏感标记扫描：PASS
- OpenSpec CLI：**SKIPPED**；CLI 不在 PATH，改做五份 delta 与 baseline 的 Requirement/Scenario 文件级核对

### 闸门 3：真实 provider

- 数据：仅固定合成短文本，无用户日记、真实对话、文件、prompt、response 或 secret 入证据
- 场景：recent、distant、long-ago、review focal、recurrence eligible、recurrence insufficient
- 总调用：6/6 成功（达到批准上限 6）；未新增分类、衰减或 eligibility 调用
- 六条输出均通过 temporal overreach checker，未触发敏感内容、timeout、identity/config 漂移等停止条件
- recurrence eligible 场景实际 `hintUsed=false`；证明了 eligible prompt 链路可调用且输出安全，但未形成真实模型采纳提示的正证据

### 真实 MySQL

- 使用固定合成用户、焦点记录与不同年龄旁支记录验证真实 MySQL 查询
- owner 隔离、`SEALED` 排除、focal 排除、24 个月窗口、recent/distant/long-ago 衰减与 recurrence eligibility：PASS
- 合成数据在 `finally` 中清理：PASS

### 微信真机

- **SKIPPED**：本机未发现微信开发者工具或可控真机环境
- 不以 H2、scripted provider、构建或桌面浏览器冒充真机 UI 证据

## 4. Delta 接受

- `agent-runtime`：明确时间归属修订；确定性距离、旁支衰减、recurrence 门槛、overreach fail-closed 与上下文边界
- `backend-core`：L3 temporal module、后端配置、MemoryPort 复用、外部契约稳定与无内容观测
- `miniapp-core`：复用既有对话表面、保持安静且由用户主动发起
- `v2-product-scope`：朋友式距离感修订；解释权归还用户、反分析产品、旧记忆不删除与被动私密
- `agent-collaboration`：fixed Clock、C6 基线纪律、独立合成探针闸与范围守护

## 5. 保留风险与后续边界

- 真实 provider 仅 6 条固定合成小样本，不是生产 SLA，也不能证明所有自然语言越界形态均被词表覆盖
- recurrence eligible 的真实输出未采纳 hint；语言质量与采纳稳定性仍缺正向样本
- 微信真机上的话术长度、浮层体验与“无分析 UI”仍无活体证据
- 30/180/90 天与 100/75/50% 是已批准的保守产品阈值，尚未用真实用户样本校准
- 向量检索、相关性评分、统计周期、dashboard、提醒、C10 语气标定与 C11 上下文架构均未纳入

## 6. 提交与外部副作用

- Commit：pending（本归档提交后以 Git 事实为准）
- 未 push、未部署、未发布
