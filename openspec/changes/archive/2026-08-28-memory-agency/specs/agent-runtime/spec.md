# Delta Spec：agent-runtime（P4.2 Memory Agency）

## ADDED Requirements

### Requirement: Cross Record Memory Must Require Explicit Session Authorization

Runtime SHALL 默认只使用当前会话与当前记录；跨记录检索 SHALL 同时要求 backend 配置允许且当前 session 由用户显式授权。

#### Scenario: 授权关闭

- GIVEN session 的跨记录授权为 false
- WHEN Runtime 处理 opening、普通 turn、retry 或 closing
- THEN cue extraction 与 `MemoryPort.retrieve` 调用数 SHALL 为 0
- AND prompt/memory layer SHALL 不含其他记录片段

#### Scenario: 配置开启但用户未授权

- GIVEN backend memory config 为 true 且 session authorization 为 false
- WHEN 当前表达存在关键词或标签 cue
- THEN Runtime SHALL 仍不检索其他记录
- AND 配置开关 SHALL NOT 被视为用户同意

#### Scenario: 双重允许

- GIVEN backend config 与 ACTIVE session authorization 均为 true
- WHEN 当前表达存在 cue
- THEN Runtime MAY 复用既有 `MemoryPort` 检索符合边界的其他记录
- AND 既有片段数量、长度、时间窗、来源层与时间归属限制 SHALL 继续成立

### Requirement: Review Target And Cross Record Memory Must Remain Distinct

REVIEW_CHAT 的目标 UNLOCKED 记录 SHALL 是用户主动选择的默认上下文；其他记录 SHALL 继续受 session authorization 控制。

#### Scenario: 回看未开启跨记录授权

- GIVEN 用户为自己的一条 UNLOCKED 记录开启 REVIEW_CHAT 且 authorization=false
- WHEN Runtime 组装上下文
- THEN 目标记录自身 MAY 进入 REVIEW_TARGET memory layer
- AND 其他历史记录 SHALL NOT 被检索或注入

#### Scenario: 回看开启跨记录授权

- GIVEN REVIEW_CHAT authorization=true 且存在有效 cue
- WHEN Runtime 检索其他历史
- THEN SHALL 复用同一 `MemoryPort`
- AND target 与其他历史 SHALL 以不同 source kind 记录

### Requirement: User Visible Sources Must Match The Final Injected Memory

用户可见来源 SHALL 只从本轮最终进入 prompt 的 memory fragments 派生。

#### Scenario: 候选未注入

- GIVEN 一条记录被检索为候选但被限制或 temporal policy 丢弃
- WHEN 本轮来源被保存和返回
- THEN 该记录 SHALL NOT 出现在用户可见来源

#### Scenario: 实际注入

- GIVEN 一条 REVIEW_TARGET 或 CROSS_RECORD fragment 实际进入 prompt
- WHEN assistant message 成功持久化
- THEN 该 message SHALL 关联该来源记录与 source kind
- AND 来源 SHALL NOT 包含 fragment、摘要、正文、关键词、分数、命中原因或 prompt

#### Scenario: 失败轮次

- GIVEN provider、guardrail 或 message persistence 失败
- WHEN 本轮没有成功 assistant message
- THEN SHALL NOT 产生声称本轮已使用来源的成功关联

### Requirement: Revocation Exclusion And Deletion Must Govern Future Turns

session 撤销、record 排除和删除 SHALL 在提交完成后立即阻止未来跨记录使用。

#### Scenario: 撤销 session 授权

- GIVEN 当前 session 曾开启并使用跨记录来源
- WHEN 用户关闭授权后发起下一轮
- THEN 下一轮跨记录检索与注入 SHALL 为 0
- AND 历史 message 的实际来源证据 MAY 保留

#### Scenario: 排除一条记录

- GIVEN owner 将记录标为不再供 Agent 参考
- WHEN 任何后续 session 检索历史
- THEN 该记录 SHALL 不进入候选或 prompt

#### Scenario: 删除一条记录

- GIVEN 来源记录进入删除中或已删除
- WHEN 后续 turn 检索或历史 message 解析来源
- THEN 该记录 SHALL 不再参与未来检索
- AND 历史来源 SHALL 只显示不可用，不得恢复已删除内容

#### Scenario: 已发生的模型调用

- GIVEN 一条过去记录已在某次 provider 请求中使用
- WHEN 用户随后撤销、排除或删除
- THEN 产品 SHALL 只承诺阻止未来使用
- AND SHALL NOT 声称技术上撤回或抹除已发生的模型调用

### Requirement: Memory Corrections Must Remain User Authored Context

记录的时间语境说明 SHALL 只来自用户显式提交，SHALL NOT 成为 AI 自动事实或画像。

#### Scenario: 用户说明只代表当时

- GIVEN owner 为来源记录保存一段时间语境说明
- WHEN 该记录在未来被实际使用
- THEN Runtime MAY 将说明标识为用户后来补充的语境
- AND SHALL NOT 把说明改写为模型推断的人格、趋势或诊断

#### Scenario: AI 推测

- GIVEN provider 从多条记录推测出阶段、性格或模式
- WHEN 本轮结束
- THEN 推测 SHALL NOT 自动写入 record policy、用户画像或长期事实
- AND 过去记录内容 SHALL 继续不能成为当前正文或工具正文的合法来源
