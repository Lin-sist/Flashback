# Design：Agent Reflection Loop（C7）

> 阶段：**已定稿**；闸门 1 已批准、闸门 2 已授权（2026-08-02）。

## 1. 定稿摘要

1. 反思环只落在 reply pipeline。
2. 仅非 `CLOSING` 回复的 `MISSING_TIME_ATTRIBUTION` 开环；material 不开环。
3. 不给普通回复新增全量忠实度检查，不给工具提议开环。
4. 重写上限固定为 1；provider 错误、invalid content 与其余违规均不重试。
5. reflection 调用不下发 tools，避免重复提议与副作用歧义。
6. P13 定案：reflection 是同一业务 attempt 内的 provider 子调用，不增加 `attemptNo`；轨迹 step 用 phase 区分。
7. 不新增表/列/API/DTO/config/依赖；不修改 20s/30s 超时。
8. 真实外调预算上限 6，需闸门 3 单独授权。

## 2. 架构

```text
AgentChatServiceImpl
  ├─ 仍负责：会话/阶段/记忆/工具协调/消息持久化/事务
  ├─ AgentReplyPipeline
  │    ├─ initial provider call（可带 tools）
  │    ├─ normalize + reply guardrails
  │    ├─ eligible? MISSING_TIME_ATTRIBUTION only
  │    ├─ reflection provider call（tools=[]，固定类型化指令）
  │    └─ re-check → reply or existing local fallback
  └─ Material path（保持 C4 现状）
       └─ initial provider call → guards → material or null discard；不 reflection
```

`AgentGuardrailVerdict` 保持不变。pipeline 内部使用一个不承载文本的判定结果（例如 `GuardedTextResult<T>`）传递 `passed / violation / terminalValue`；候选文本只作为调用栈中的局部变量，不进入该结构的日志表示、trace 或持久化。

## 3. 数据流

### 3.1 Reply

1. 组装现有 conversation messages，initial 调用允许现有 tools。
2. 解析 content/tool calls，normalize reply shape。
3. 依现有顺序运行 content checker → time attribution checker。
4. 首次通过：返回 initial reply + initial tool calls。
5. 非 `CLOSING` 阶段命中 `MISSING_TIME_ATTRIBUTION`：
   - 记录 `reflection-decision(phase=initial, eligible=true, reason=missing-time-attribution)`；
   - 在同一份上下文末尾追加固定 system/user 指令；
   - 第二次调用 `completeWithTools(messages, [], false)`，**不下发 tools**；
   - normalize 后重新运行完整 reply guardrails；
   - 通过则返回 rewritten reply，工具提议取 initial call 的结果；
   - 不通过则返回现有 `safeFallbackReply()`，并丢弃 initial tool calls，避免“回复已降级但仍展示该次模型提议”。
6. `CLOSING` 或首次命中其他违规：不 reflection，直接走现有兜底并丢弃 tools。
7. initial provider 失败/invalid：保持现有 `AgentReply.fail(...)`，不 reflection。
8. reflection provider 失败/invalid：按“重写未成功”处理，走现有本地兜底，不升级成 C8 的错误重试。

### 3.2 Material

Material 路径保持 C4 现状：只生成一次，随后运行 faithfulness → content guards；
`UNFAITHFUL` 或其他违规继续返回 null。实现前复核确认 CLOSING 已包含 reply + material 两次调用，
material reflection 会令单轮达到 3 次并冲撞 20 秒超时，因此经用户裁决移出 C7。

### 3.3 类型化指令

映射器只接受 `AgentGuardrailViolation`：

| violation | 固定要求 | 适用路径 |
|---|---|---|
| `MISSING_TIME_ATTRIBUTION` | 明确这是用户过去某个时候写下或表达过的内容，不要把过去说成此刻 | reply |

映射器不得接收候选文本、corpus、history、memory fragment 或 prompt。未知/不允许的枚举返回 empty，不提供默认“都重试”分支。

## 4. 轨迹设计（P13）

### 4.1 `attemptNo` 不变

`attemptNo` 的既有语义是：同一 `(sessionId, turnNo)` 在 HTTP/provider 失败后由用户触发的同轮请求重试。reflection 是一次请求内部的子调用，不是新业务 attempt。

因此：

- 一轮仍只创建一个 `AgentTraceCollector`、持久化一条 `agent_turn_trace`；
- reflection 不调用 `nextAttemptNo`，不新增 trace row；
- `attemptNo` 不改 schema、不改语义。

### 4.2 新增结构化 steps

在 collector 内新增窄方法或复用 `step` 封装：

- `provider` step 增加 `phase=initial|reflection`（现有消费者须兼容）；
- `reflection-decision`：`path=reply`、`eligible`、`reason`、`maxRetries=1`；
- `reflection-result`：`path=reply`、`attempted`、`passed`、`terminal=rewritten|fallback|provider-failed`。

不新增任何文本字段。顶层 `providerDurationMs` 采用**两次 provider duration 的总和**，使它能反映该业务轮真实占用；单次耗时留在各 provider step。顶层 `violation` / `downgradePath` 只在最终降级时设置，首次违规被成功修正不应把整轮标成 DOWNGRADED。

## 5. 调用预算与超时

- `MAX_REFLECTION_REWRITES = 1`，代码常量，非配置项。
- 非 CLOSING 一轮 provider calls：正常 1；eligible 且首次违规时最多 2。
- CLOSING 仍为 reply + material 既有 2 次调用，reply 不开 reflection。
- reflection 不因 provider 异常、解析异常、`CHECK_ERROR` 再重试。
- 不修改 backend 20s / frontend 30s。
- 若 canary 任一合法两调用路径超过 backend timeout 或多次接近 18s，停止后续闸门 3；回到规划决定“取消该路径/缩小范围/为 C8 预留预算”，不得直接放宽超时。

## 6. 与工具调用的关系

reply initial call 仍可产生 tools；reflection call 固定不下发 tools。终态策略：

- reflection 成功：保留 initial tool calls，因工具参数仍须经过现有 validator 与二段确认；
- reflection 失败并使用本地兜底：丢弃 initial tool calls，避免一条本地兜底回复旁边出现由不合格 generation 同时产生的提议；
- 首次命中不可恢复违规：沿用上条，丢弃 tool calls；
- 不重复调用 tool coordinator，不产生任何自动副作用。

## 7. C6 回归与人评

### 7.1 离线评测

- 用 `ScriptedAgentModelClient` 编排 initial / reflection 两段输出；
- 为 reflection 增加独立用例，不削弱 23 条既有不变量；
- 对预期快照变化逐条人工确认；`baselineNote` 写明 `C7 agent-reflection-loop`、变化原因与路径；
- checksum 按 C6 机制手工更新，不增加自动刷新入口。

### 7.2 人评锚点

闸门 3 同批真实产出用于受控评级，不存文本。推荐每条记录：

- `changeId=C7`、prompt/policy version；
- path、violation、initial/final terminal；
- restraint / warmth / faithfulness 三档受控评级；
- latency bucket、reviewer/date；
- 明确 `sampleTextStored=false`。

锚点用于说明“小样本人评观察”，不形成绝对质量分数，不宣称统计显著。

## 8. 验证策略

| 层 | 验证 | 外调 |
|---|---|---|
| 映射器 | 仅非 CLOSING 的时间归属违规有固定指令；其余返回 empty；指令不含输入文本 | 0 |
| Reply pipeline | 首过、重写过、重写失败、不可恢复、provider 失败、tool 不重复 | 0 |
| Material path | 既有单次生成与直接丢弃语义零回归 | 0 |
| Trace | 同一 trace/attempt；phase 顺序；总耗时；成功修正不记 DOWNGRADED | 0 |
| C6 eval | 不变量 + 新 reflection cases；预期快照逐条留痕 | 0 |
| 全量 | Maven 默认测试，基线 606/4；既有断言零修改 | 0 |
| MySQL | 含 reflection 的真实事务与 trace row/steps 检查 | 本地服务，0 AI |
| 真机/provider | reply canary + 重复观察 + 受控人评 | ≤6，闸门 3 |

## 9. 决策记录

### 决策 1：为什么 material reflection 移出 C7

- 选择：C7 reply-only；material `UNFAITHFUL` 继续直接丢弃。
- 理由：CLOSING 现状已有 reply + material 两次调用，再重写 material 会达到 3 次；按历史平均耗时约 19.4s，几乎顶满 20s。
- 代价：C7 不能挽救不忠实素材；但保住了可证明的调用上限与既有超时契约。

### 决策 2：为什么工具提议不开环

- 选择：不重写工具提议。
- 理由：工具提议被拒绝时普通 reply 仍可返回，失败成本低；重跑整轮会增加工具重复提议与调用预算。
- 代价：不忠实提议仍直接消失，不会尝试挽救；符合 C4 的 fail-closed 取向。

### 决策 3：P13——reflection 是否新 attempt

- 选择：否；同一 trace、同一 attempt，step 标 phase。
- 理由：attempt 是请求级重试身份；把子调用升格会扭曲“一轮一条”并要求 schema/唯一性变更。
- 代价：顶层字段必须定义聚合语义，且查询单次耗时要读 stepsJson。

### 决策 4：重写指令如何注入

- 选择：固定模板 + enum 映射，追加到 messages；不传候选片段。
- 理由：兑现 D29，输入面零扩大，且可单测完整枚举覆盖。
- 代价：指令不够精细，可能降低成功率；这是为隐私与可审计性接受的代价。

### 决策 5：reflection 是否允许 tools

- 选择：不允许，`tools=[]`、strict=false。
- 理由：reflection 只修文本；允许工具会产生第二组提议并把一次重写变成新行为分支。
- 代价：reflection 回复无法同步修正 initial tool args；工具参数仍由既有 validator 独立决定。

### 决策 6：provider 失败是否借 reflection 重试

- 选择：不重试。
- 理由：C7 只处理确定性护栏可恢复违规；网络/服务错误分类属于 C8，混入会吃掉调用预算且模糊归因。
- 代价：偶发失败仍按当前失败语义返回。

### 决策 7：顶层 providerDurationMs 如何处理

- 选择：同一轮所有 provider 子调用耗时求和，单次值保留在 steps。
- 理由：顶层应表达用户等待的总 provider 时间，供 C8 预算设计使用。
- 代价：与 C5 “单次调用耗时”历史口径发生可解释变化；spec 与查询文档必须标明 C7 后是总和。

### 决策 8：成功 reflection 是否算 DOWNGRADED

- 选择：不算；记 SUCCESS + reflection steps。
- 理由：最终用户得到 provider 合格输出，本地兜底未发生；把它算降级会混淆“自修复成功”与“终态兜底”。
- 代价：统计 reflection 频率必须读 steps，不能只看 outcome。

### 决策 9：是否修改超时

- 选择：不改。
- 理由：20s/30s 刚完成真实修复；历史均值推算可容纳，但必须用 canary 证实。
- 代价：慢请求可能暴露 C7 方案不可行；这应触发重新设计，而不是延长用户等待。

### 决策 10：为什么现在抽 pipeline

- 选择：只抽最小 reply pipeline 协作者，不重构整个服务与 material 路径。
- 理由：1274 行服务类的后置检查没有 provider 能力；加环必然需要掉转依赖。独立协作者是能力落地的边界，不是洁癖式重构。
- 代价：构造依赖和测试装配会变化；须以 focused tests + 全量回归守住。

## 10. 对蓝图的偏离与勘误

1. 类大小采用实测 1274 行，不采用冻结蓝图 1183 行。
2. `UNFAITHFUL` material reflection 经实现前复核与用户裁决移出 C7；理由见决策 1。
3. 蓝图“最坏约 13s”只是基于历史平均值的估算，不写成 confirmed；闸门 3 前为 unknown。
4. 蓝图测试基线 534 已过时；本 change 采用 C6 closeout 的 606/4。
