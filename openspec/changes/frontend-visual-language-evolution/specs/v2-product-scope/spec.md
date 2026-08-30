# V2 Product Scope Delta：Frontend Visual Language Evolution

## ADDED Requirements

### Requirement: Visual Evolution Must Serve The Present-Moment Product Promise / 视觉演进 MUST 服务“给此刻一个位置”

任何候选或选定视觉方向 MUST 让“留下此刻”成为首页与记录路径的视觉中心，并保持 AI、封存、时间回看和时间篇章为用户主动选择的次级能力。视觉辨识度 MUST NOT 以增加记录压力、未来焦虑或宏大人生叙事为代价。

#### Scenario: A direction is selected / 选择视觉方向

- **WHEN** 产品负责人审查候选方向
- **THEN** 方向 SHALL 帮助用户快速留下文字、图片或原始声音
- **AND** SHALL NOT 把首页变成未来信件倒计时、AI 对话、统计看板或作品展示流
- **AND** SHALL 保持用户内容为视觉主体

#### Scenario: A dramatic temporal style is proposed / 提出强烈时间叙事风格

- **WHEN** 深色、电影化、时间隧道或强仪式效果可能主导普通记录
- **THEN** 该效果 SHALL 限制在适合的时间回看/抵达语境或被拒绝
- **AND** 普通此刻的保存 SHALL 继续安静、完整且无需未来解释

### Requirement: External Design Work Must Use Synthetic Content Only / 外部设计工作 MUST 只使用合成内容

Open Design 或任何外部视觉生成/设计服务只可接收公开产品约束、公开参考链接和固定合成内容。真实用户日记、prompt、Agent 回复、账号、媒体、位置、token、password、secret 或可识别个人信息 MUST NOT 被发送。

#### Scenario: An external prototype is generated / 生成外部原型

- **WHEN** 使用 Open Design Cloud、Local Codex 或 secure BYOK 生成设计产物
- **THEN** brief SHALL 只包含合成文字、合成状态和公开参考
- **AND** SHALL NOT 上传或复制真实用户内容、真实媒体或 secret
- **AND** 生成稿 SHALL 标记为原型证据而不是已实现能力

### Requirement: Visual Rollout Must Be Incremental / 视觉落地 MUST 渐进推进

选定方向的生产落地 MUST 先稳定首页、记录编辑器和时光轴样板，再抽取共享 token/组件并分批迁移其它页面。change SHALL NOT 一次性重写全部前端页面。

#### Scenario: Gate 2 implementation begins / Gate 2 开始实现

- **WHEN** 用户批准选定方向并授权实现
- **THEN** 第一切片 SHALL 只覆盖选定的样板页、必要 token 和共享展示结构
- **AND** 其它页面 SHALL 分批迁移并分别验证业务状态、Preview、安全区和减少动态
- **AND** package/lockfile、backend、Agent、deployment、monitoring 和 admin SHALL 保持不变，除非另有独立授权

