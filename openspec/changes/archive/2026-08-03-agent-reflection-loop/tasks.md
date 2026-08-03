# Tasks：Agent Reflection Loop（C7）

> 阶段：**已验收并归档**（2026-08-03）。闸门 1 已批准、闸门 2 已授权（2026-08-02）。
> 外调授权：未获得；提交责任：用户已授权 Agent 提交 C7，未授权 push / 部署 / 发布。
> 回归基线：606 tests PASS / 4 skipped（C6 closeout）。

## 闸门检查点

- [x] **闸门 1 · 规划批准**：用户 2026-08-02 已批准
- [x] **闸门 2 · 实现授权**：用户 2026-08-02 已明确允许条件满足时进入实现
- [—] **闸门 3 · 外调授权**：未授权、未执行；用户在已知该项 SKIPPED 后接受归档
- [x] **提交责任确认**：用户已授权 Agent 提交本次 C7；push / 部署 / 发布未授权

## 阶段 0：实现前复核

- [x] **T-01** 记录开工锚点 `b459b8f`；工作区只有 C7 规划产物；M1/M3 历史目录未动
- [x] **T-02** 复核 `AgentChatServiceImpl`：1274 行，generation / guardrail / trace 挂点与规划一致；
  但发现 CLOSING 一轮本来就含 reply + material 两次 provider 调用，material reflection 会把单轮推到 3 次
- [x] **T-03** focused baseline：C4 guardrail、C5 trace、C6 eval 指定测试 **PASS**（2026-08-02）
- [x] **T-04** 锁定无 API/DTO/DDL/config/timeout/frontend/package 变更；若实现确需任一项则回闸门 1

## 阶段 1：类型化 reflection policy

- [x] **T-05** 新增 `AgentReflectionPolicy`（或等价窄组件），只接受阶段与 violation enum
- [x] **T-06** 映射非 `CLOSING` 回复的 `MISSING_TIME_ATTRIBUTION` 固定中文要求
- [x] **T-07** 其余违规与未知值返回不可重写；不得有 default retry
- [x] **T-08** 隐私单测：组件签名/输出不承载 candidate/history/corpus/memory；固定指令不拼输入文本
- [x] **T-09** 上限常量 `MAX_REFLECTION_REWRITES=1`，不可配置、不可由模型改变

## 阶段 2：reply pipeline

- [x] **T-10** 抽出最小 `AgentReplyPipeline`，迁移生成→normalize→检查→终态处置所需逻辑
- [x] **T-11** initial 一次通过保持现状：同 messages/tools/strict、同 reply/toolCalls、同长度裁剪
- [x] **T-12** `MISSING_TIME_ATTRIBUTION` 触发一次 reflection；第二次完整重跑 reply guards
- [x] **T-13** reflection 调用 `tools=[]` 且 strict=false，证明不会产生第二组工具提议
- [x] **T-14** reflection 成功保留 initial tool calls；最终兜底/不可恢复违规丢弃 initial tool calls
- [x] **T-15** `DIAGNOSTIC` / `FAKE_ACTION` / `CHECK_ERROR` 及 provider failure/invalid content 不 reflection
- [x] **T-16** reflection provider failure/invalid 走现有本地安全兜底，不新增 C8 错误重试语义
- [x] **T-17** mock provider 行为不伪装真实 reflection；内置 mock 分支保持零外调且既有测试通过

## 阶段 3：material 边界守护（reply-only 裁决）

- [—] **T-18** `AgentMaterialPipeline` 移出 C7：用户 2026-08-02 裁决 reply-only
- [—] **T-19** material `UNFAITHFUL` reflection 移出 C7；继续沿用 C4 直接丢弃
- [x] **T-20** 回归证明 material 违规/异常仍返回 null，调用数不因 C7 增加
- [x] **T-21** 证明 tool proposal 的 `UNFAITHFUL` 不触发 reflection、不增加 provider 调用
- [x] **T-22** `CLOSING` 回复不 reflection；会话 ENDED、消息持久化与 `materialDraft` 可选语义不变

## 阶段 4：轨迹与预算

- [x] **T-23** provider step 增加 `phase=initial|reflection`，既有单调用轨迹仍可读
- [x] **T-24** 新增脱敏 `reflection-decision` / `reflection-result` step
- [x] **T-25** P13 验证：同一 turn 只持久化一条 trace、同一 `attemptNo`；reflection 不调用 `nextAttemptNo`
- [x] **T-26** 顶层 `providerDurationMs` 汇总全部子调用；单次 duration 留在 steps
- [x] **T-27** 成功重写 outcome=SUCCESS；只有最终本地兜底/丢弃才是 DOWNGRADED
- [x] **T-28** provider call count 不变量：非 CLOSING 普通≤1、eligible≤2；CLOSING 保持既有 reply+material≤2
- [x] **T-29** 轨迹隐私扫描：无候选文本、用户文本、记忆文本、prompt、provider response

## 阶段 5：C6 评测回归

- [x] **T-30** 用 scripted client 新增成功/仍违规/provider 失败/CLOSING/material 零反思用例
- [x] **T-31** 23 条既有 + 5 条新增不变量全绿；未削弱或删除既有断言
- [x] **T-32** 审查全部快照 diff；只更新确属 C7 编排行为变化或新增的条目
- [x] **T-33** 每条快照更新同步修改 `baselineNote` + checksum，明确 change-id 与原因
- [x] **T-34** baseline guard 元测试继续证明不存在自动刷新/批量接受入口

## 阶段 6：回归与本地真实边界

- [x] **T-35** focused tests：policy / reply pipeline / material 零回归 / trace / C6 eval PASS
- [x] **T-36** 后端全量 Maven test：622 PASS / 4 skipped，0 failures / 0 errors（较 606 新增 16）
- [—] **T-37** SKIPPED：本轮未建立真实 MySQL + scripted reflection 联调夹具；H2 不冒充 MySQL
- [x] **T-38** 验证没有 API/DTO/schema/frontend/timeout/package/lockfile 变化
- [x] **T-39** `git diff --check`、敏感标记扫描、真实日记内容不入 tracked files

## 阶段 7：闸门 3（仅获授权后）

- [—] **T-40** SKIPPED：闸门 3 未授权，未执行真实 provider canary
- [—] **T-41** SKIPPED：未进入真实外调，stop conditions 未做活体验证
- [—] **T-42** SKIPPED：未执行重复观察；真实 provider 调用数为 0
- [—] **T-43** SKIPPED：无真实调用耗时可记录
- [—] **T-44** SKIPPED：C6 narrative anchors 仍为空，不冒充已完成人评
- [—] **T-45** SKIPPED：未执行真机 C7 活体验收

## 阶段 8：验收与收口

- [x] **T-46** 更新四份 delta 与实现事实 exact match；确认 miniapp-core 无 delta
- [x] **T-47** 输出 Required Output；更新 ACTIVE_TASK Current Progress；追加 AGENT_LOG
- [x] **T-48** 用户 2026-08-03 在已知 SKIPPED 项后明确要求归档；delta 已接受，change 已归档，ACTIVE_TASK→IDLE
- [x] **T-49** 按 D33 更新叙事文档 §8 与 §9；不含日记原文、secret、本机绝对路径
- [x] **T-50** 按提交责任处理：执行已授权 Agent commit；未获 push/部署/发布授权，不执行

## 范围守护自检

- 不扩大 eligible violation 集合；仅非 CLOSING reply 的 `MISSING_TIME_ATTRIBUTION`；最大 reflection=1。
- material 与 CLOSING reply 不 reflection，确保单轮调用预算不超过既有上限。
- 不给普通 reply 新增全量忠实度闸；不改护栏阈值/词表/来源集合。
- 不做工具 reflection，不做 provider/network error retry。
- 不引入图框架、Judge、评分 dashboard、A/B、C8 resilience、C9 temporal。
- 不改 stage machine、memory、tool whitelist、API、DTO、DDL、frontend、timeout、package/lockfile。
- 不自动刷新 C6 baseline；不修改既有测试断言换绿。
- 不写真实日记/候选输出/prompt/response 到日志、trace、spec、AGENT_LOG 或 tracked samples。
- 不修改 archive 与冻结蓝图；偏离只登记在本 change。
- 未明确授权时不 `git add` / `commit` / `push` / 部署 / 发布 / 真实外调。
