# Design：Agent Resilience（C8）

> 阶段：已定稿；闸门 1 已批准、闸门 2 已授权（2026-08-03）。

## 1. 推荐摘要

1. 在 `AgentModelClient` 边界把异常与 HTTP status 归一为封闭的 `AgentProviderFailureCategory`。
2. 每个 Agent HTTP 编排创建 `AgentCallBudget`，默认 provider-work 总预算 24000ms。
3. 每次调用 timeout=`min(20000ms, remaining)`；预算耗尽不发下一调用。
4. C8 第一阶段零自动 retry；现有用户主动同轮 retry 保持不变。
5. provider failure 仍是显式 FAILED/UNAVAILABLE，不持久化本地 Assistant；阶段化模板只改变失败提示，不伪装生成成功。
6. 复用现有 status/message 与用户主动同轮 retry；不新增 API/DTO/frontend 字段，不向前端暴露技术分类。
7. 轨迹记录 category/phase/transient/budget 状态，不记录异常 message、response body 或用户内容。
8. 多 provider 路由、熔断、缓存、监控与自动退避 deferred。

## 2. 架构

```text
AgentController request
  └─ AgentChatServiceImpl
       ├─ create AgentCallBudget(total=24s)
       ├─ AgentReplyPipeline
       │    ├─ initial call(timeout=min(20s, remaining))
       │    ├─ guards
       │    └─ optional C7 reflection call(timeout=min(20s, remaining))
       ├─ optional material call(timeout=min(20s, remaining))
       └─ AgentResiliencePolicy
            ├─ failure category -> transient metadata
            ├─ operation/stage -> warm failure message
            └─ trace/log safe metadata

AgentModelClient
  └─ HTTP/parse exception -> AgentProviderException(category, httpStatus?, cause)
```

边界原则：client 负责“发生了什么”，resilience policy 负责“该如何对用户和编排表达”。业务 service 不解析异常字符串，也不持有 provider response body。

## 3. 错误分类

### 3.1 封闭 taxonomy

| category | 来源 | transient（只供证据） | 用户可见技术细节 |
|---|---|---:|---|
| `TIMEOUT` | `HttpTimeoutException` 或 deadline exhausted | true | 不暴露 |
| `THROTTLED` | HTTP 429 | true | 不暴露 |
| `AUTH_CONFIGURATION` | HTTP 401/403、调用前配置不可用 | false | 不暴露 key/provider |
| `UPSTREAM_UNAVAILABLE` | connect failure、HTTP 5xx | true | 不暴露 endpoint/body |
| `INVALID_RESPONSE` | 2xx 但 JSON/shape/content 无效 | false | 不暴露 response |
| `REQUEST_REJECTED` | 除 401/403/429 外的 4xx | false | 不暴露 request/body |
| `INTERRUPTED` | `InterruptedException` | false | 不暴露线程信息 |
| `UNKNOWN` | 未归类异常 | false | 不暴露异常 message |

`transient` 只用于 trace 与排查，不授权自动 retry，也不控制前端按钮。`AUTH_CONFIGURATION` 同时覆盖调用前 `unavailableReason()` 与运行期 401/403；调用前未发请求为 `UNAVAILABLE`，已发请求失败为 `FAILED`。

### 3.2 HTTP 映射顺序

1. 先按具体异常类型识别 timeout/interrupted/connect。
2. 有 HTTP status 时按 401/403 → 429 → 5xx → 4xx → unexpected status 分类。
3. 2xx 后的 JSON/shape/content 问题统一 `INVALID_RESPONSE`。
4. 其余落 `UNKNOWN`。

核心分类不得依赖异常 message 或 provider response body 的关键词匹配。

## 4. 整轮 deadline（P14）

### 4.1 预算模型

建议新增 `app.agent.resilience.provider-work-timeout-millis=24000`，默认值小于前端 30000ms。它不是第三套 provider timeout，而是一次 Agent HTTP 编排中全部 provider 子调用的共享总预算。

`AgentCallBudget` 使用 monotonic clock：

- `remainingMillis()`：总预算减去已耗时；
- `nextCallTimeoutMillis(perCallMax)`：返回 `min(remaining, perCallMax)`；
- 小于最小安全发起阈值（建议 100ms）时直接抛 `TIMEOUT/deadline-exhausted`，不发 HTTP；
- 只保存起点与总预算，不保存用户数据。

### 4.2 各路径上限

| 路径 | provider calls | 预算行为 |
|---|---:|---|
| opening | ≤1 | 单次最多 20s，总预算 24s |
| 普通 turn 首次通过/失败 | ≤1 | 单次最多 20s |
| 普通 turn C7 reflection | ≤2 | 第二次仅拿剩余预算 |
| CLOSING reply + material | ≤2 | material 仅拿 reply 后剩余预算 |
| finish material-only | ≤1 | 单次最多 20s |

第一阶段不自动 retry，因此 C8 不增加任何一行的 call count。

### 4.3 material 的特殊语义

若 reply 已成功而 material 因 deadline/failure 缺失：

- Assistant reply 仍落库，会话仍可正常结束；
- `materialDraft=null`；
- turn outcome 不反转为 FAILED；
- trace 记录 material failure category 与 budget state；
- 不在同一请求再试 material。

## 5. 用户可见失败策略

### 5.1 不伪装成功

阶段化模板只写入 `AgentSessionVO.message`，不作为 `AgentMessageRole.ASSISTANT` 持久化。provider failure 仍返回 `FAILED`，调用前不可用仍返回 `UNAVAILABLE`。

### 5.2 推荐模板意图

固定模板由 `operation + stage + category` 选择，不拼接用户输入或异常文本：

- opening 可重试：说明“现在没能接上”，邀请稍后再试；不假装已经理解用户。
- turn 可重试：说明“刚才写下的这句还在”，提供再试一次。
- turn 不可重试：说明“现在暂时无法继续”，保留已写内容，不鼓励立即反复点击。
- closing/material：reply 已成功时不显示整轮错误；只是不提供可选素材。

模板须短、克制、不诊断、不承诺后台自动恢复，不出现 provider、timeout、429、鉴权、配置等技术术语。

### 5.3 既有主动 retry 契约

C8 不新增 `retryable` API 字段。原因是 provider failure 后存在 pending 用户轮次；backend 会阻止提交新消息，直到原消息完成。若按某些技术类别隐藏 retry，用户可能无法完成该轮。

- error card 与“再试一次”保持现状；
- retry 仍由用户显式触发，不是 backend 自动 retry；
- retry 复用同一 turn，不重复用户消息、不推进阶段机；
- `transient` 只进入 backend trace，不参与 UI 决策。

## 6. Trace 与日志

### 6.1 turn trace

`provider-failed` / `provider-invalid-content` 收敛为类型化方法，建议 step 字段：

- `phase=initial|reflection|material`
- `category=<wire-id>`
- `transient=true|false`
- `budgetExhausted=true|false`
- `remainingBucket=none|lt-1s|1-5s|gt-5s`（不要求精确 deadline）

顶层 `causeType` 改存稳定 category wire id，或新增兼容 getter 后由 persistence 继续写同一 `cause_type` 列；不改 schema。

### 6.2 opening 日志

开场不伪造 turn 0 trace。日志仅包含 operation、stage、provider 配置标识、category、durationMs、transient；不写 endpoint、status body、exception message、prompt 或用户内容。

### 6.3 material

`material-failed` 增加 category 与 budget 状态；不得改变顶层成功 outcome。

## 7. 与 C7 / C6 的关系

- C7 reflection policy 的 eligible 集合与最大次数 1 不变。
- reflection provider failure 不再只记异常类名，而是走 C8 分类；终态仍回到 C7 既有本地安全兜底，不再重试。
- CLOSING reply 仍不 reflection；reply 与 material 共享 deadline。
- C6 scripted client 扩展为可抛类型化 failure，并增加 deadline/budget/error taxonomy 用例。
- 快照只在 C8 合法改变结构化指标时逐条审查；不得批量刷新。

## 8. 验证策略

| 层 | 验证 | 外调 |
|---|---|---:|
| classifier | 每类异常/HTTP status 映射、transient metadata、未知兜底 | 0 |
| budget | monotonic remaining、min per-call、耗尽不发请求、两调用共享 | 0 |
| model client | request timeout 接收剩余预算；不泄露 response/message | 0 |
| reply/reflection | 无自动 retry；reflection 只拿剩余预算；终态保持 C7 | 0 |
| material | 共享预算；失败不反转 reply/session | 0 |
| service/API | 用户消息保留、Assistant 不落库、外部契约零变化 | 0 |
| frontend | 既有 retry、pending user 输入不丢与错误卡片零回归 | 0 |
| C6 eval | 全部不变量 + 新 resilience cases + baseline guard/privacy | 0 |
| 全量 | backend Maven test；frontend type-check/build | 0 |
| MySQL/真机/provider | 真实事务、双调用 deadline 与 UI 体感 | 另行授权 |

## 9. 决策记录

### 决策 1：P14 采用整轮 24s deadline，而非继续放宽单次超时

- 选择：共享 24000ms provider-work budget；单次仍最多 20000ms。
- 理由：前端只等待 30000ms；多调用路径需要总上限，继续提高单次 timeout 会让前端先断。
- 代价：慢 initial 会压缩 reflection/material 时间，后者可能更早失败；这是守住用户等待边界的必要取舍。

### 决策 2：第一阶段不自动 retry

- 选择：所有分类都不在同一请求自动重试；用户主动同轮 retry 保持既有统一入口。
- 理由：C7 合法路径已可能两次调用；上线前无错误分布证据，自动 retry 会预支延迟与预算并可能放大 429。
- 代价：短暂网络抖动不能在后台自愈；但用户输入不丢且已有同轮 retry。

### 决策 3：温暖降级仍是显式失败，不是本地 Assistant 回复

- 选择：模板放 `message`，status 保持 FAILED/UNAVAILABLE，Assistant 不落库。
- 理由：延续 C1 工程诚实边界，避免本地模板冒充模型已理解用户。
- 代价：体验上仍出现错误卡片，而不是无缝对话；但文案和重试入口更准确。

### 决策 4：不新增 `retryable`，failure category 只在 backend

- 选择：外部 API/DTO/frontend contract 不变；现有 retry 继续服务 pending-turn 恢复。
- 理由：失败后 backend 要求先完成原消息；按技术类别隐藏 retry 会产生会话卡死风险。技术分类只用于排查，不应改变用户是否能恢复该轮。
- 代价：配置/鉴权等非暂态错误仍允许用户稍后点击 retry；但不会自动重试，也不暴露内部技术细节。

### 决策 5：分类发生在 provider client 边界

- 选择：按异常类型与 HTTP status 构造类型化异常，上层只消费 enum。
- 理由：只有 client 能可靠看到 status 与 parse 阶段；service 解析 exception message 会脆弱且可能泄露响应。
- 代价：需要调整现有 client 测试替身，但不新增依赖。

### 决策 6：material 失败保持可选能力语义

- 选择：共享 deadline，但 failure 只留痕、返回 null，不反转对话成功。
- 理由：material 从 C1 起就是辅助产物；让它阻塞收束会违背核心记录生命周期不依赖 AI。
- 代价：用户可能拿不到回填素材；真实稳定性另行观察。

### 决策 7：多 provider 路由继续 deferred

- 选择：本刀只把分类/预算/终态边界做稳，不新增 provider registry 或 fallback credential。
- 理由：项目未上线、无故障分布；路由会引入成本、身份、模型行为与隐私出站的新决策。
- 代价：单 provider 故障时仍需用户稍后重试。

## 10. 实现边界提示

推荐最小新增对象：

- `AgentProviderFailureCategory`
- `AgentProviderException`
- `AgentCallBudget`
- `AgentResiliencePolicy` / `AgentFailurePresentation`

具体类名可在实现期微调，但不得改变 taxonomy、deadline、零自动 retry、显式失败与隐私契约。任何需要修改外部 API、持久化 schema、timeout 数值或主动 retry 语义的偏离，必须先回到闸门 1 重新确认。
