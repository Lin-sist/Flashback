# Tasks：Agent Resilience（C8）

> 闸门 1、闸门 2 已于 2026-08-03 批准；闸门 3、验收归档与 Agent 提交已于 2026-08-08 获用户授权。

## 阶段 0：闸门与基线

- [x] **T-01** 读取 AGENTS、ACTIVE_TASK、蓝图 C8 意图卡片、C7 archive 与 accepted specs
- [x] **T-02** 只读核对 Git/IDLE/C7 archive/622+4 基线；记录开工锚点 `fb68082`
- [x] **T-03** 核对真实超时语义：20s 为单次 provider request，不是整轮 deadline
- [x] **T-04** 创建 proposal/design/tasks 与四份 delta；明确 v2-product-scope 无 delta
- [x] **T-05** 初始化 ACTIVE_TASK Current Progress 并追加规划期 AGENT_LOG
- [x] **T-06 闸门 1** 用户 2026-08-03 批准规划；N1–N6 按推荐定稿
- [x] **T-07 闸门 2** 用户 2026-08-03 明确授权开始 C8 实现
- [x] **T-08** 实现前全量 baseline：74 suites / 622 tests / 0 failures / 0 errors / 4 skipped；默认 Maven 仓库解析失败与代码结果分开记账

## 阶段 1：failure taxonomy（TDD）

- [x] **T-09 RED** 为 timeout/429/401/403/5xx/4xx/connect/invalid/interrupted/unknown 编写分类失败测试
- [x] **T-10 GREEN** 新增封闭 `AgentProviderFailureCategory` 与类型化 `AgentProviderException`
- [x] **T-11** 在 `AgentModelClient` HTTP/parse 边界按异常类型与 status 分类，不解析异常 message
- [x] **T-12** 锁定 transient metadata mapping；它只供 trace/排查，不授权自动 retry 或控制 UI
- [x] **T-13** 证明异常 message、response body、request body、endpoint 与 credential 不进入类型化错误/日志/trace

## 阶段 2：整轮 deadline（P14）

- [x] **T-14 RED** 为单调用、双调用、剩余预算、耗尽不发请求与 monotonic clock 编写测试
- [x] **T-15 GREEN** 新增 request-scope `AgentCallBudget`，默认 provider-work 总预算 24000ms
- [x] **T-16** 每次 provider timeout=`min(app.ai.timeout-millis, remaining)`；小于最小阈值不发请求
- [x] **T-17** opening/turn/reflection/material/finish 调用接收同一请求预算；不创建嵌套新预算绕过总上限
- [x] **T-18** 证明 C8 不增加自动 retry；非 CLOSING≤2、CLOSING reply+material≤2、finish material≤1
- [x] **T-19** 证明 backend 在 frontend 30000ms 前保留响应余量；纯 DB 请求仍不放宽

## 阶段 3：失败终态与阶段化模板

- [x] **T-20 RED** 覆盖 opening/turn/closing-material × failure category 的固定模板映射
- [x] **T-21 GREEN** 新增窄 `AgentResiliencePolicy`；只接收 enum/operation/stage，不接收用户文本或异常 message
- [x] **T-22** provider failure 保持 FAILED/UNAVAILABLE；Assistant 不落库，本地模板不冒充正常回复
- [x] **T-23** 用户消息保留、同轮主动 retry 不重复 insert/turn/stage；新消息在 pending turn 完成前仍被阻止
- [x] **T-24** material timeout/failure 返回 null，只留痕，不反转已成功 reply/session outcome
- [x] **T-25** interrupted 恢复线程中断标记且不重试；unknown fail-safe 映射稳定

## 阶段 4：API 与 Mini Program 零契约回归

- [x] **T-26** contract 测试证明 `AgentSessionVO` / frontend `AgentSession` 字段集合不因 C8 扩张
- [x] **T-27** 既有 FAILED/UNAVAILABLE + message 映射显示阶段化模板，不暴露 category/status code
- [x] **T-28** error card、“再试一次”、pending-turn 输入禁用与同轮 retry 行为保持
- [x] **T-29** 记录页与回看页复用同一规则；Preview 仍不访问真实 Agent
- [x] **T-30** 不新增页面、Tab、技术术语、进度灯、后台恢复承诺或 major visual reconstruction
- [x] **T-31** 若实现需要新增 API/DTO/frontend 字段，停止并回到闸门 1，不在实现中主观扩约

## 阶段 5：trace / eval / privacy

- [x] **T-32** trace provider failure step 增加 phase/category/transient/budget 状态；同一 turn 仍一条 row
- [x] **T-33** `cause_type` 复用现有列保存稳定 category，不改 DDL/schema；查询兼容性测试锁定
- [x] **T-34** opening 只写脱敏结构化日志，不伪造 turnNo=0 trace
- [x] **T-35** material failure 分类可见但不改变顶层 SUCCESS/DOWNGRADED 语义
- [x] **T-36** C6 scripted client 支持类型化 failure 与 fake clock；新增 resilience cases
- [x] **T-37** C6/C7 全部硬不变量继续执行；既有断言零削弱
- [x] **T-38** 所有 snapshot diff 逐条人工审查；合法变化同步 `baselineNote` + checksum
- [x] **T-39** privacy/meta tests 证明 trace/log/spec/report 无用户文本、prompt、response、exception message 或 secret

## 阶段 6：回归与本地边界

- [x] **T-40** focused tests：classifier/budget/policy/client/pipeline/service/trace/eval PASS
- [x] **T-41** 后端全量 Maven test 不低于实现前基线，既有 skip 不新增或逐项解释
- [x] **T-42** 前端 type-check/build PASS；Agent 错误卡片与重试分支测试 PASS（若仓库有对应 runner）
- [x] **T-43** `git diff --check`、增量 secret/敏感内容扫描、改动路径审计 PASS
- [x] **T-44** 真实 MySQL 联调 PASS：失败后同轮主动重试仅保留一条 USER message；同一 turn 的 attempt 1 为 `UNAVAILABLE/auth-configuration`、attempt 2 为 `SUCCESS`；合成数据已清理
- [x] **T-45** 确认无 DDL、provider secret、pom/package/lockfile、部署、监控、C9 temporal 变化

## 阶段 7：闸门 3（仅另行授权后）

- [x] **T-46** 闸门 3 已授权；执行前披露 provider/model/endpoint、最多 6 次调用、仅固定合成文本与停止条件
- [x] **T-47** 真实 DeepSeek canary 2/2 PASS：1378ms、1656ms
- [x] **T-48** 真实双调用 2/2 组 PASS：2898ms、3531ms；总计 6/6 次成功，均在共享 24000ms budget 与前端 30000ms 窗口内
- [—] **T-49 SKIPPED** 本机未发现微信开发者工具或可控真机环境；未用 scripted provider、构建或桌面浏览器冒充真机证据
- [x] **T-50** 闸门 3 证据已记录；真实调用总数 6（≤8），未触发前端先超时、调用超限、identity/config 漂移或敏感内容入证据停止条件

## 阶段 8：验收与收口

- [x] **T-51** delta 与实现 exact match；若 API/预算/分类偏离，回到规划闸而非静默改 spec
- [x] **T-52** 输出 Required Output；更新 ACTIVE_TASK Current Progress；追加 AGENT_LOG
- [x] **T-53** 用户验收后接受 delta、写 closeout、归档 change、ACTIVE_TASK→IDLE
- [x] **T-54** 按 D33 更新叙事 §9；只写可证明事实与诚实 SKIPPED
- [x] **T-55** 用户授权 Agent 提交；仅提交 C8 范围，不 push/deploy/release

## 范围守护自检

- 不自动 retry；不引入指数退避、熔断、缓存、队列或多 provider 路由。
- 不把 provider failure 改成成功，不持久化本地 Assistant 假回复。
- 不扩大 C7 reflection eligible 集合或调用上限；不改 C4 护栏权威。
- 不让 material 失败阻塞记录生命周期或反转已成功对话。
- 不暴露 failure category、HTTP status、endpoint、response body、异常 message 给用户。
- 不记录用户日记/对话、prompt、memory、candidate、provider response 或 secret。
- 不改 stage machine、memory、tool whitelist、record lifecycle、DDL、provider credential。
- 不改 frontend 30s 与纯 DB 10s；如需改数值须回到闸门 1。
- 不做 C9 temporal、部署、监控、admin portal 或 major UI reconstruction。
- 不修改 archive 与冻结蓝图；不自动刷新 C6 baseline。
