# Agent Resilience（C8）

> Change ID：`agent-resilience`
> 类型：Type C（改变 provider 错误语义、整轮超时预算与用户可见失败态）
> 阶段：闸门 1 已批准、闸门 2 已授权，进入实现（2026-08-03）
> 开工锚点：`fb68082`
> 提交责任：用户手动提交；未授权 `git add` / `commit` / `push`

## 1. Why Now

C1 已保证 provider 失败时用户消息不丢、Assistant 回复不落库并可同轮重试；C5 已有结构化轨迹；C7 又允许部分非 `CLOSING` reply 在同一请求内调用 provider 两次。当前缺口是：所有 provider 异常仍被压成近似的 `IOException` / `FAILED`，用户只看到同一条技术感较强的失败文案，轨迹也主要保存异常类名，无法稳定区分超时、限流、鉴权/配置、上游故障与无效响应。

更关键的是，现有 `app.ai.timeout-millis=20000` 实际作用于**每一次** `HttpRequest`，并不是整轮 Agent 请求的总上限。C7 的 initial + reflection 两次调用理论上可各等 20 秒，而前端 Agent 请求只等待 30 秒；“backend 20s < frontend 30s”这一旧论证在多调用路径上已不成立。

C8 第一阶段因此聚焦三件事：建立封闭的错误分类、建立整轮 deadline、把失败文案按会话阶段变得温和且可操作。自动重试与多 provider 路由不进入第一阶段，避免在 C7 已占用两次调用的路径上继续扩张预算。

## 2. Readiness Verdict

规划期开刀结论：**GO，可以进入 C8 规划。** 实现现已另行获得闸门 2 授权，见 §13。

依据：

1. `.ai/ACTIVE_TASK.md` 开刀前为 `IDLE`，没有并行 Type C change。
2. C7 已归档至 `openspec/changes/archive/2026-08-03-agent-reflection-loop/`，相关 delta 已进入 baseline。
3. C7 归档后端基线为 622 tests PASS / 4 skipped；真实 MySQL、provider/真机与人评锚点仍为 SKIPPED，不冒充已验证。
4. Git 工作区开刀前为 clean；开工锚点为 `fb68082`。
5. 蓝图 v1.2 已冻结，默认序列为 C6 → C7 → C8 → C9；C8 的硬依赖 C1 已满足，C7 的预算事实可作为输入。
6. OpenSpec CLI 当前不在 PATH；本轮沿用仓库既有目录结构并做文件级验证，不声称运行 CLI validate。

## 3. 现状事实（能力五态）

### confirmed

- R1：真实 provider 调用使用 JDK `HttpClient.send`；`HttpRequest.timeout` 直接读取 `app.ai.timeout-millis`，当前默认 20000ms。
- R2：该 20000ms 是单次 HTTP 调用超时；代码没有 request/turn 级 deadline，也没有把“剩余预算”传给 reflection 或 material。
- R3：前端 Agent 请求固定等待 30000ms；纯数据库请求仍使用默认 10000ms。
- R4：非 `CLOSING` reply 最多 initial + reflection 两次 provider 调用；`CLOSING` reply 不 reflection，但同一请求还会生成 material，仍最多两次调用。
- R5：provider HTTP 非 2xx、内容缺失与 JSON 解析问题大多以 `IOException` 暴露；上层捕获宽泛 `Exception`，用户无法区分失败类别。
- R6：配置缺失在调用前通过 `unavailableReason()` 返回 `UNAVAILABLE`；运行期失败返回 `FAILED`。
- R7：provider 失败时用户消息已落库、Assistant 消息不落库；用户主动“再试一次”会复用同一 turn，不重复用户消息、不再次推进阶段机。
- R8：前端已有错误卡片与“再试一次”；该入口是 pending 用户轮次恢复机制的一部分，而不只是“暂态错误提示”。
- R9：护栏本地兜底与 provider 失败是两种语义：前者对用户是成功回复、轨迹为 `DOWNGRADED`；后者必须保持显式失败，不得冒充模型正常成功。
- R10：轨迹 `causeType` 当前主要保存异常类名；未形成稳定、可查询的错误分类枚举。
- R11：开场 `turnNo=0` 不落 turn trace；失败仅有结构化日志。普通 turn 的失败可进入一轮一条 trace。
- R12：material 是可选产物；其 provider 失败不改变对话成功 outcome，也不应阻塞会话收束。
- R13：C5 真实 provider 历史耗时 min 4571 / avg 6476 / max 8467ms，但样本只来自 C5 小规模探针，不代表 C7 双调用稳定性。
- R14：现有依赖足够实现 deadline 与类型化错误；第一阶段无需改 Maven dependency、数据库 schema 或 secret 配置。

### partial / unknown

- R15：真实 provider 的 429、401/403、5xx、连接失败与 timeout 分布为 **unknown**；项目未上线且无真实流量。
- R16：C7 双调用在 24 秒整轮预算内的通过率为 **unknown**；C7 闸门 3 未执行。
- R17：阶段化失败文案是否改善体感为 **unknown**；离线测试只能验证映射与边界，不能证明用户体验收益。
- R18：阶段化 backend message 在当前微信错误卡片中的实际体感为 **unknown**，须实现后真机验证；协议本身不扩张。

### planned

- R19：封闭的 provider failure taxonomy、整轮 deadline/剩余预算、阶段化温暖失败模板与结构化轨迹分类。

### out_of_scope

- R20：自动 provider 重试、指数退避、语义缓存、复杂熔断中间件、多 provider 路由、部署监控与告警。

## 4. 规划期事实修正

蓝图与 C7 文档把“backend 20s / frontend 30s”当成整轮可用预算，但 checked-in code 证明 20s 只限制单次 provider HTTP 请求。C8 必须修正的是**多调用请求缺少总 deadline**，而不是简单继续调大超时。

本 change 不修改已冻结蓝图；在 design 与 delta 中把 P14 定案为：

- Agent provider-work 总预算建议为 24000ms；
- 每次调用的 timeout 为 `min(app.ai.timeout-millis, remainingBudget)`；
- 预算耗尽时不再发起下一次 provider 调用；
- frontend 30000ms 保持不变，预留至少 6000ms 给编排、数据库、护栏与响应传输；
- 第一阶段不做任何自动 provider retry。

## 5. Goals

1. 用封闭枚举区分 `TIMEOUT`、`THROTTLED`、`AUTH_CONFIGURATION`、`UPSTREAM_UNAVAILABLE`、`INVALID_RESPONSE`、`REQUEST_REJECTED`、`INTERRUPTED`、`UNKNOWN`。
2. 在 provider client 边界保留 HTTP 状态/异常来源，但日志、trace 与 API 不写 provider response、请求体、用户内容或异常 message。
3. 为单个 Agent HTTP 编排创建总 deadline；所有 reply/reflection/material 子调用共享同一预算。
4. 预算不足时 fail-closed，不发起下一次 provider 调用；C7 reflection 上限仍为 1，CLOSING 总调用上限仍为 2。
5. 第一阶段不自动重试 provider；只保留现有用户主动同轮重试。
6. provider 失败继续返回显式 `FAILED` / `UNAVAILABLE`，不持久化本地 Assistant 冒充成功。
7. 按 opening / conversational turn / closing-material 语境返回克制、温暖且不诊断的失败提示。
8. 复用现有 `status/message` 与“再试一次”契约，不新增 API/DTO/frontend 字段；避免隐藏 retry 后让 pending turn 无法完成。
9. trace 使用稳定 failure category 与 transient 标识；同一 turn 仍一条 trace，错误分类不含自由文本。
10. C6/C7 离线不变量与快照纪律继续生效；叙事文档只在归档收尾更新。

## 6. Non-Goals

- 不做自动重试、指数退避、随机抖动或后台补偿任务。
- 不做多 provider/fallback model 路由；蓝图“二期 deferred”保持 deferred。
- 不做语义缓存、熔断器中间件、bulkhead、队列、流式响应或 SSE。
- 不修改 provider/model/secret 选择，不新增 credential。
- 不把本地模板持久化为 Assistant 正常消息，不新增 `FALLBACK` 成功状态。
- 不修改护栏阈值、reflection eligible 集合、工具白名单、阶段机、记忆检索或记录生命周期。
- 不让 material 失败反转已成功的对话，也不把可选素材缺失显示成整轮失败。
- 不扩大到旧 `AiServiceImpl` 三个非 Agent 端点；C8 只处理 Agent runtime。
- 不改 frontend 30000ms 与纯数据库请求 10000ms；不靠增大超时掩盖预算问题。
- 不改数据库 schema、package/lockfile、Maven dependency、部署、监控或 admin portal。

## 7. 用户故事

- 用户发出一句话后遇到超时，系统明确告诉他“刚才写下的这句还在”，并提供一次主动重试；不会让他重新输入，也不会出现假 Assistant 回复。
- provider 因配置/鉴权不可用时，用户看到克制的暂不可用说明，不再被鼓励反复点击一个立即无效的重试。
- 收束阶段来不及生成可选素材时，对话与用户已写内容仍完成；系统不把素材缺失夸大成整轮失败。
- 开发者只看结构化 trace 就能区分 timeout、throttled、auth/config、upstream 与 invalid response，并知道失败发生于 initial/reflection/material 哪个 phase。

## 8. 建议待裁决项

本草案给出推荐默认值；闸门 1 批准即视为一并确认：

| 编号 | 事项 | 推荐 |
|---|---|---|
| N1 | 第一阶段是否自动重试 | **不重试**；C7 已可能占两次调用，保留用户主动同轮重试 |
| N2 | 整轮 provider-work budget | **24000ms**，每次调用取剩余预算与 20000ms 的较小值 |
| N3 | 用户可见终态 | 保持 `FAILED/UNAVAILABLE`，不落本地 Assistant 假回复 |
| N4 | 前端是否需要契约字段 | **不新增**；复用现有 FAILED/UNAVAILABLE + message + 用户主动同轮 retry |
| N5 | 关闭/素材失败 | material 仍是可选产物；失败只留结构化证据，不反转成功对话 |
| N6 | 多 provider 路由 | deferred，不进入 C8 第一阶段 |

## 9. 外调预算与闸门 3

规划与实现的离线外调预算均为 **0**。错误分类、deadline 与 UI 分支应由 scripted/fake client 验证。

若实现完成后需要真实 provider 验收，须另行申请闸门 3。建议上限 **8 次真实 provider 调用**：先 2 次正常 canary，再最多 6 次覆盖单/双调用 deadline 观察；不主动制造 401/403、429 或 provider 故障，不用真实日记内容。任何调用超限、前端先超时、敏感内容入证据或 identity/config 漂移均立即停止。

真实 provider 授权不包含 push、部署或发布。

## 10. 验收标准

1. failure taxonomy 为封闭枚举；未知错误落 `UNKNOWN`，不存在按异常 message 模糊匹配的核心分类。
2. HTTP timeout、429、401/403、5xx、其他 4xx、连接失败、invalid body、interrupted 与 unknown 均有确定分类测试。
3. 单个 Agent 编排共享 24000ms provider-work deadline；所有调用 timeout 不超过剩余预算。
4. 预算耗尽前不以 0/负数发请求；预算耗尽后不发起 reflection/material 等下一子调用。
5. 第一阶段任一 provider failure 都不触发自动 retry；非 `CLOSING` reply 总调用≤2，`CLOSING` reply+material 总调用≤2。
6. provider 失败继续保留用户消息、不落 Assistant 消息、不重复推进阶段；用户主动重试仍复用同一 turn。
7. taxonomy 可标记 `transient=true|false` 供 trace/排查使用，但 C8 第一阶段不据此自动调用 provider；用户主动同轮 retry 继续适用于所有 pending turn failure。
8. 失败模板按 opening / turn / closing-material 映射，保持克制、不诊断、不承诺后台恢复，并明确用户内容未丢（适用时）。
9. 前端既有错误卡片、pending-turn 输入禁用与“再试一次”行为零回归；不新增技术状态或契约字段。
10. trace 记录 category、phase、transient、budget exhausted/remaining bucket；不记录异常 message、provider response、prompt 或用户内容。
11. material failure 不改变已成功 reply 的 outcome/session end；finish 的既有记录生命周期不受影响。
12. C6/C7 全部硬不变量继续执行；快照变化只能逐条人工确认并更新 `baselineNote` + checksum。
13. 后端全量测试不得低于 622 PASS / 4 skipped 基线；前端 type-check/build 与 Agent 组件测试（若有）通过。
14. 真实 MySQL、provider/真机未执行时必须分别标记 SKIPPED，不从 H2/scripted 结果推断真实稳定性。
15. 无 DDL、secret、provider credential、package/lockfile、部署、监控或 C9 时间智能改动。

## 11. Spec Delta 落点

- `agent-runtime`：错误分类、整轮 deadline、零自动 retry、失败/护栏降级语义分离。
- `backend-core`：调用预算、异常边界、外部契约不变与轨迹聚合。
- `miniapp-core`：阶段化失败提示、既有重试入口与用户输入不丢。
- `agent-collaboration`：故障证据脱敏、离线故障注入、真实探针单独授权。
- `v2-product-scope`：无 delta；不新增产品表面、页面或 V2 范围。

## 12. 关键风险

| 风险 | 缓解 |
|---|---|
| 24s 预算仍不足以完成两次慢调用 | 第二次只拿剩余预算；真实 canary 观察；不自动重试、不调大前端超时 |
| 分类依赖异常 message，provider 改文案即漂移 | 在 client 边界按异常类型与 HTTP status 分类；不解析 response body 文案 |
| 温暖模板被当成正常 Agent 回复 | 仍返回 FAILED/UNAVAILABLE，不落 Assistant 消息；trace outcome 不伪装 SUCCESS |
| 按错误类别隐藏 retry 导致 pending turn 卡死 | 不新增 retryable 契约；所有失败仍保留现有用户主动同轮 retry |
| deadline 只覆盖 reply，漏掉 material | request-scope budget 由 service 创建并传入 reply/reflection/material，共享同一对象 |
| opening 无 turn trace 难观测 | 保持 turn trace 语义不变；开场只写脱敏结构化日志，不伪造 turn 0 trace |
| scope 演变成生产级容灾平台 | 第一阶段明确排除路由、熔断、缓存、监控、部署与自动 retry |

## 13. Gate State

- 闸门 1：**已批准**（2026-08-03，N1–N6 按推荐方案定稿，其中 N4 为“不改 API/DTO/frontend”）。
- 闸门 2：**已授权**（2026-08-03，用户要求“开始 C8 阶段实现”）。
- 闸门 3：未授权；真实 provider / 真机调用为 0。
- Git：用户手动提交；未授权 stage / commit / push。
