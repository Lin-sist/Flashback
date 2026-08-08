# Design：Agent Temporal Intelligence（C9）

> 本设计依赖 `proposal.md` 与五份 spec delta。所有阈值与语义在闸门 1 批准前均为 `planned`；本文不授权业务实现。

## 1. 推荐摘要

C9 第一阶段采用**确定性时间策略 + 现有 provider 一次生成 + 确定性反分析检查**：

- 新增窄 `agent/temporal` L3 模块，使用已注入 `Clock` 计算时间距离；
- 保留 C3 月份级来源锚点，补充 `RECENT / DISTANT / LONG_AGO / UNKNOWN`；
- 只对 memory port 返回的旁支片段做 100% / 75% / 50% 字符预算衰减；
- 用户主动打开的回看目标记录不衰减；
- recurrence hint 只在回看模式 + 显式比较意图 + 至少两条旁支记录 + 跨度 ≥90 天时成立；
- provider 回复经过 temporal overreach checker，越界直接走既有安全兜底，不 reflection；
- 不新增 provider 调用、API、DTO、DDL、页面、依赖或外部引擎。

## 2. 架构

### 2.1 模块边界

建议新增：

```text
backend/.../agent/temporal/
├── AgentTemporalPolicy.java
├── TemporalContext.java
├── TemporalDistance.java
├── TemporalDistanceBand.java
├── TemporalPatternEvidence.java
├── TemporalPolicyResult.java
└── AgentTemporalLanguageChecker.java
```

职责：

- `TemporalDistanceBand`：封闭枚举 `RECENT / DISTANT / LONG_AGO / UNKNOWN`；
- `TemporalDistance`：保留 `occurredAt`、既有 `timeLabel`、距离天数（内部）与 band；
- `TemporalPatternEvidence`：只保存 eligible、recordCount、spanBucket、reason enum，不保存内容；
- `TemporalPolicyResult`：最终旁支片段、无内容 temporal supplement、pattern evidence 与版本源；
- `AgentTemporalPolicy`：唯一阈值/衰减/eligibility 决策源；
- `AgentTemporalLanguageChecker`：确定性检查公开声明的量化、绝对规律、因果诊断与预测形态。

`AgentChatServiceImpl` 只负责把 `mode + current user input + reviewFragments + retrievedFragments` 交给 policy，再把 policy result 传给 prompt、来源集合、trace 与 reply pipeline。阈值不得散落在 service、prompt builder 或 mapper。

### 2.2 与现有模块的依赖方向

```text
MemoryPort / reviewFragments
          │
          ▼
 AgentTemporalPolicy  ◄── Clock + backend config
          │
          ├── final ancillary fragments ──► AgentLayeredCorpus
          ├── temporal supplement ─────────► AgentPromptBuilder
          ├── pattern evidence ────────────► trace/eval (metadata only)
          └── policy fingerprint ──────────► AgentTraceVersions

provider reply
    │
    ▼
existing content/time/length checks
    │
    ▼
AgentTemporalLanguageChecker
    ├── PASS ──► persist Assistant
    └── TEMPORAL_OVERREACH ──► existing safe fallback (no reflection)
```

Temporal policy 可以依赖 `MemoryFragment` 与 `AgentChatMode`；memory port、mapper、domain record 不反向依赖 temporal。这样 C9 落在蓝图 L3 策略层，不把策略塞进持久化或 UI。

## 3. 时间距离模型

### 3.1 计算方式

使用 `Clock` 当前本地日期与 `occurredAt.toLocalDate()` 计算完整天数：

- `0..30` → `RECENT`
- `31..180` → `DISTANT`
- `181+` → `LONG_AGO`
- null、未来时间、计算异常 → `UNKNOWN`

边界按日期而不是毫秒计算，避免同一天因小时差落入两个 band。未来时间不取绝对值；把未来记录说成“过去了 N 天”属于事实错误，必须进入 `UNKNOWN`。

### 3.2 可读表达

现有 `MemoryFragment.timeLabel`（如“2026年3月”）仍是 canonical source anchor。distance band 只生成受控语义提示：

| band | provider 可用语义 | 禁止推断 |
|---|---|---|
| `RECENT` | 最近、前阵子 | “持续如此” |
| `DISTANT` | 几个月前、隔了一段时间 | “已经改变/没有改变” |
| `LONG_AGO` | 更早的一段时间、很久以前 | “成长阶段/长期人格” |
| `UNKNOWN` | 那时候、以前 | 任何精确距离 |

Prompt 仍提供月份标签；不把内部天数、band 名称或阈值发给用户。

## 4. 记忆衰减注入

### 4.1 定义

C9 的“衰减”是**prompt context budget decay**，不是业务数据生命周期：

- SQL 候选、24 个月 lookback、owner/status、检索字段不变；
- 不删除、不更新、不隐藏记录；
- 不改变时光轴/记录详情；
- 不把 recency 变成相关性分数。

### 4.2 预算计算

对 memory port 返回的每个旁支片段：

```text
base = app.agent.memory.max-fragment-chars
RECENT    = base × 100%
DISTANT   = base × 75%
LONG_AGO  = base × 50%
UNKNOWN   = base × 50%
effective = clamp(floor(calculated), min=40, max=base)
```

截断沿用 Java 字符长度语义，与现有实现保持一致；不在本刀引入 tokenizer。若 `base < 40`，effective 以 `base` 为上限，不能因最小值反而扩大配置。

### 4.3 两类记忆必须分开

- **focal review fragments**：用户主动打开的 UNLOCKED 记录，继续使用 `review.record-excerpt-chars`；
- **ancillary retrieved fragments**：memory port 基于当前线索命中的旁支历史，应用 decay。

Policy 必须在 `mergedMemoryOf` 之前接收两份列表，避免合并后失去“主体/旁支”身份。最终进入 prompt 与 `AgentLayeredCorpus` 的必须是**同一份衰减后列表**，继续守住 C3a“注入与来源集合一致”不变量。

## 5. 克制的重复主题提示

### 5.1 eligibility

全部条件同时满足才 eligible：

1. `mode == REVIEW_CHAT`；
2. 当前这一条用户消息含确定性比较/反复意图词，例如“以前、又、再次、总觉得、相比、变化、还是、反复”；
3. 不把 focal record 计入证据；旁支记录至少 2 个不同 `recordId`；
4. 证据记录 `occurredAt` 均已知；最早与最晚跨度 ≥90 天；
5. 本轮最多形成一个 hint；不跨轮持久化“已经识别到某模式”。

词表是“是否允许提示”的窄触发器，不是 NLP 意图分类器。未命中时宁可少说，不扩大为模型自判。

### 5.2 hint 内容

Policy 不拼接日记内容，只提供无内容指令：

```text
本轮可以克制地提到：类似主题似乎不止一次出现过。
最多提一次，并以问题邀请用户自己判断是否有关。
不得声称周期、趋势、原因、诊断、评分或预测。
```

实际记录月份仍通过现有 memory supplement 提供。Agent 可以引用月份，但不能看到“命中数=2、跨度=xxx 天”等内部指标。

### 5.3 为什么不叫“周期检测”

两条或三条 LIKE/tag 命中不能证明统计周期。代码与 trace 使用 `recurrence` / `pattern-eligible`，用户话术只允许“似乎不止一次”；不得出现“系统发现了周期”。这保留蓝图的时间感方向，同时拒绝制造分析能力。

## 6. Temporal overreach 检查

### 6.1 检查位置

建议在既有 reply content/time/length 检查之后、持久化之前执行；只检查最终候选回复，不检查用户文本。检查器输出 `PASS` 或内部 `TEMPORAL_OVERREACH`。

### 6.2 第一阶段封闭规则

至少覆盖：

- 百分比/评分：`\d+%`、`分` 与情绪/状态/成长语境组合；
- 绝对频率：每年/每月/每隔…就/总是/从不/必然；
- 因果诊断：说明/证明/导致/根源/因为…所以 与用户状态结论组合；
- 趋势与预测：越来越/持续恶化/明显改善/以后还会/下一次一定；
- 诊断词继续由 C4 既有 checker 负责，temporal checker 不复制整套诊断词表。

规则必须在单一声明源中版本化并测试正反例。正常引用“2026年3月”或用户自己消息中的百分比不能被无条件误伤；checker 只面向 Assistant 候选且使用上下文组合规则。

### 6.3 终态

- overreach → 既有 `SAFE_FALLBACK_REPLY`；
- trace layer 可新增内部 `temporal-language`，violation 为 `temporal-overreach`；
- 不进入 C7 reflection，因为 C7 eligibility 是封闭的 reply-only `MISSING_TIME_ATTRIBUTION`；
- 不新增 provider call，C8 budget/call-count invariants 不变。

## 7. Prompt 与版本

### 7.1 Prompt 组装

`AgentPromptBuilder` 增加窄 `buildTemporalSupplement(TemporalPolicyResult)`：

- 空结果返回空串，不注入“没有发现规律”；
- 只包含允许表达的距离语义与无内容 hint；
- 不列内部 band、阈值、比例、命中数量或跨度；
- memory fragment 原文仍仅由既有 `buildMemorySupplement` 负责；
- temporal supplement 不得成为工具/素材 prompt 的新来源。

### 7.2 版本锚点

`AgentTraceVersions` 的 policy fingerprint source 增加：

- band 边界；
- decay 比例与最小字符数；
- recurrence cue 词表版本与跨度门槛；
- overreach 规则版本；
- prompt 约束文本。

任何变化都应改变 fingerprint；但 fingerprint 不包含用户数据或运行时日期。

## 8. Config 与兼容

建议在 `AppAgentProperties` 新增：

```yaml
app:
  agent:
    temporal:
      enabled: true
      recent-days: 30
      distant-days: 180
      distant-budget-percent: 75
      long-ago-budget-percent: 50
      min-fragment-chars: 40
      recurrence-min-span-days: 90
```

约束：`recentDays < distantDays`、percentage 为 1..100、min chars >0、span >0。跨字段关系若 Bean Validation 注解难以清楚表达，policy 构造时 fail-fast；不得静默交换阈值。

`enabled=false`：不做距离 supplement、不衰减、不生成 recurrence hint、不跑 temporal checker，行为等价 C8；只留无内容 enabled=false trace step。

## 9. API、数据与前端边界

- API/DTO：零变化；继续使用现有 Agent endpoints 与 `AgentSessionVO`。
- 数据库：零 DDL；不保存 band、pattern、score 或用户画像。
- mapper：不改 `selectMemoryCandidates` 的 SQL、排序或字段。
- frontend：零业务代码预期；只运行 type-check/build 与必要的既有契约回归。
- UI：不新增页面/卡片/徽标/图表；复用写作引导和回看消息气泡。
- 记录：不改变封存后不变性，也不把 temporal 结果写入正文/reality/reply。

## 10. Trace 与隐私

允许记录：

- `policyVersion` / fingerprint；
- `enabled`；
- 各 distance band 的计数；
- decay 前后总字符数（或桶）；
- `patternEligible` / `patternUsed`；
- `temporal-overreach` 与 downgrade path；
- provider call count（用于证明无新增调用）。

禁止记录：

- 用户消息、日记、记忆片段、prompt、provider response；
- keyword/tag 值、精确记录时间列表、记录标题；
- checker 命中的完整原句或 exception message；
- 可还原用户历史的跨轮 pattern profile。

TemporalContext、hint 与衰减后的片段只存在于本轮内存；不新增缓存或持久化表。

## 11. 验证策略

### 11.1 TDD focused

1. `AgentTemporalPolicyTest`：30/31、180/181、null/future、同日、固定时钟。
2. decay：120→120/90/60、base<40、不扩大、中文截断、focal exempt。
3. recurrence：mode、cue、distinct id、90 天边界、unknown time、每轮一个。
4. `AgentTemporalLanguageCheckerTest`：量化/绝对/因果/预测正例与月份/普通共情反例。
5. prompt：空结果、distance + exact label、无内部指标、无内容泄露。
6. service/pipeline：最终来源集合使用衰减后同一列表；overreach 直接 fallback；provider calls 不增加。
7. trace/version：字段白名单、fingerprint 变化、零内容。

### 11.2 C6 eval

- fixed clock 的最近/边界/更早用例；
- 写作引导只表达距离、不主动归纳模式；
- 回看显式比较 + 足够证据 → hint；
- 不足证据/无 cue/仅 focal → no hint；
- 数字评分、绝对规律、因果、预测 → downgrade；
- 既有 MISSING_TIME_ATTRIBUTION 与 C7 reflection 上限不变；
- snapshot 只逐条更新受 C9 合法影响的条目，说明与 checksum 同步。

### 11.3 全量与消费端

- 后端 focused → 全量 Maven；
- 前端 `vue-tsc --noEmit` 与 mp-weixin build；
- `git diff --check`、变更路径 allowlist、依赖/DDL/API/DTO 零变化检查；
- 敏感 pattern scan；
- OpenSpec CLI 缺失时做目录、Requirement/Scenario、delta 落点与链接检查。

### 11.4 闸门 3

只有单独授权后：固定合成 provider 6 调用上限；真实 MySQL 合成数据；可控微信真机。任何未执行项写 `SKIPPED + 原因`，不能从 scripted/H2/build 推断真实质量。

## 12. 决策记录

### 决策 1：时间智能放在确定性 L3 policy，还是只改 prompt

1. **面临的选择**：只在 prompt 加“有时间感”；引入第二次模型判断；新增确定性 L3 policy + 后置 checker。
2. **选了哪个 + 为什么**：选择确定性 L3 policy + checker。距离、预算与 eligibility 可由现有时间/模式/片段事实计算，能固定时钟回归；模型只负责措辞。
3. **放弃的代价**：只改 prompt 无法证明衰减/门槛真的生效；第二次模型判断不可复现、增加外调与 C8 budget 压力。

### 决策 2：距离分几档

1. **面临的选择**：只分新/旧；按天数连续生成；分 `RECENT/DISTANT/LONG_AGO/UNKNOWN` 四档。
2. **选了哪个 + 为什么**：选择四档，推荐 30/180 天边界。足以区分最近、数月与更早，又不会把朋友式回忆变成精密分析。
3. **放弃的代价**：二档表达力不足；连续天数会暴露系统感、放大时区/边界噪声并诱导量化。

### 决策 3：“记忆衰减”衰减什么

1. **面临的选择**：删除/缩短业务记录窗口；概率性抽样；缩小旁支片段的 prompt 字符预算。
2. **选了哪个 + 为什么**：选择字符预算衰减。它可复现、无数据破坏、保持 24 个月检索契约，并直接限制旧记忆在上下文中的话语量。
3. **放弃的代价**：删除/缩窗破坏时间回看语义；概率抽样会让同一输入结果漂移、难以写 C6 baseline。

### 决策 4：回看目标记录是否也衰减

1. **面临的选择**：所有旧内容统一衰减；按年龄衰减目标记录；目标记录豁免、只衰减旁支记忆。
2. **选了哪个 + 为什么**：选择目标记录豁免。用户主动打开它，说明它是本次对话主体；把主体变短违背用户意图。
3. **放弃的代价**：统一衰减实现简单但会让年代久远的回看内容反而更薄；按年龄衰减也把“系统遗忘”凌驾于用户选择。

### 决策 5：什么时候允许提到重复主题

1. **面临的选择**：只要多条命中就主动提；由模型自行判断；回看模式 + 显式用户意图 + 两条旁支记录 + 90 天跨度的确定性门槛。
2. **选了哪个 + 为什么**：选择窄门槛。它坚持被动召唤与解释权归用户，同时让行为可单测。
3. **放弃的代价**：自动主动提像画像；模型自判不可审计且可能把偶然相似说成规律。

### 决策 6：把它叫周期，还是只说有限重复

1. **面临的选择**：直接输出周期/规律；完全禁止跨时间关联；只允许“似乎不止一次”并邀请用户判断。
2. **选了哪个 + 为什么**：选择有限重复表达。现有 LIKE/tag 命中与最多三片段不足以证明统计周期，但可以支持一个克制问题。
3. **放弃的代价**：声称周期是无证据分析；完全禁止则无法兑现 C9 的时间关联价值。

### 决策 7：temporal overreach 是否进入 C7 reflection

1. **面临的选择**：新增 reflection eligibility；自动再调用一次重写；直接使用既有安全兜底。
2. **选了哪个 + 为什么**：选择直接兜底。C7 eligibility 是已接受的封闭契约，C8 又要求调用数上限；C9 不应借机扩环。
3. **放弃的代价**：reflection 可能改善话术但会扩大预算/失败面；自动重试与 C8 零自动 retry 叙事冲突。

### 决策 8：是否重做 memory SQL/相关性排序

1. **面临的选择**：在 SQL 加打分；引入向量/全文检索；复用现有 MemoryPort，只在注入层做 temporal policy。
2. **选了哪个 + 为什么**：选择复用现有 Port。C9 解决时间策略，不解决 R9 检索相关性；现有接口已有 purpose/occurredAt/timeLabel。
3. **放弃的代价**：SQL 打分会把 recency 冒充 relevance 并扩大 mapper 范围；向量/全文引入依赖、索引、隐私与独立评测需求。

### 决策 9：是否向前端暴露 band/pattern

1. **面临的选择**：新增 distance/pattern DTO；新增分析卡片；外部契约零变化、只展示普通 Agent 回复。
2. **选了哪个 + 为什么**：选择零变化。内部策略不是用户可验证的“系统分析结果”，现有消息表面已足够承载朋友式话术。
3. **放弃的代价**：DTO/卡片会迅速演变为 dashboard，扩大前后端与产品契约，并暴露检索内部状态。

### 决策 10：temporal hint 和证据是否持久化

1. **面临的选择**：存长期 pattern profile；把 hint 写进 agent_message/trace；只在本轮内存存在，trace 仅记枚举/计数。
2. **选了哪个 + 为什么**：选择本轮内存 + 无内容元数据。日记与跨记录关系高度敏感，C9 无需新存储即可工作。
3. **放弃的代价**：长期 profile 会形成画像与删除/纠错难题；写完整 hint/证据会复制敏感内容到新持久化位置。

### 决策 11：是否新增前端 temporal 开关或入口

1. **面临的选择**：用户设置页开关；单独“时间智能”入口；仅 backend feature flag，复用既有入口。
2. **选了哪个 + 为什么**：选择 backend flag + 既有入口。C9 是 Agent 策略升级，不是新的产品主流程。
3. **放弃的代价**：设置页撞明确 out-of-scope；新入口会抢占“留下回应”和回看对话的既有层级。

## 13. 实现边界提示

- 规划批准前：不得修改 `backend/src/**`、`frontend/**` 或 baseline specs。
- 实现授权后：先写 policy/checker RED tests，再接 service/prompt/trace/eval；不得先大改 `AgentChatServiceImpl`。
- 若实现发现需要 mapper 排序、API/DTO/DDL、前端页面或第三次 provider call：立即停止，回到闸门 1 重新裁决。
- 不修改 archive、冻结蓝图、C4 阈值、C7 reflection eligibility、C8 budget/call limits。
- 所有真实 provider/MySQL/真机操作仍需闸门 3；Git 默认用户手动提交。
