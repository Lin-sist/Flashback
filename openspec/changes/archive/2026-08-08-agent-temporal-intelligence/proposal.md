# Agent Temporal Intelligence（C9）

> Type C 规划草案。change-id：`agent-temporal-intelligence`。
> 开工锚点：`544e9ea`。提交责任：**Agent 提交**（2026-08-08 已授权；不含 push）。
> 本文只申请规划闸；未获闸门 2 前不修改业务代码，未获闸门 3 前不执行真实 provider / 真机外调。

## 1. Why Now

C3a 已能检索用户自己的历史记录并为片段附带月份级时间标签，C3b 已提供被动召唤的时间回看对话；C4/C7 能阻止“把过去说成现在”，C6 已提供可复现的克制与护栏量尺，C8 已把最多两次 provider 子调用收进共享 24000ms budget。

当前能力仍只回答“这段内容发生在什么时候”，没有回答“它距离现在有多远、旧记忆应以多大分量进入上下文、什么时候才可以克制地提到重复主题”。这使“时间感”主要停留在固定月份标签和 prompt 文案，尚未形成可审查、可回归的 L3 策略。

## 2. Readiness Verdict

**GO**：

- `.ai/ACTIVE_TASK.md` 在开刀前为 `IDLE`；
- C8 已归档至 `openspec/changes/archive/2026-08-08-agent-resilience/`；
- `main` 工作区 clean，HEAD 为 `544e9ea`；
- 冻结蓝图 v1.2 明确下一刀为 C9 `agent-temporal-intelligence`；
- C3a/C3b 硬依赖已归档，C6 克制/护栏评测已存在；
- 当前不存在同名 active/archive change 冲突；
- `openspec` CLI 不在 PATH，故本轮沿用仓库既有 change 结构并做文件级验证，不能声称 CLI validate PASS。

## 3. 现状事实（能力五态）

### confirmed

1. `MemoryFragment` 已携带 `recordId`、`occurredAt`、月份级 `timeLabel` 与片段文本；无需新增外部 DTO。
2. C3a 默认检索窗口为 24 个月、最多 3 个片段、单片段最多 120 字；SQL 按 `created_at DESC, id DESC` 返回候选。
3. 历史检索只匹配标签与说明性字段，不匹配正文；无检索线索时不查库，且不退化为“最近记录”。
4. 回看目标记录使用 `content + ai_summary + belief_then`，发生时间取 `created_at`；解锁后的 `reality_later/reply` 不混入“那时”的来源层。
5. 回看目标记录与关联历史片段均进入 MEMORY 层，复述过去内容必须带时间归属；缺少归属时确定性降级。
6. 回看对话已是被动召唤、无阶段机、无工具、无素材；现有入口、API 与 UI 可继续复用。
7. `Clock` 已注入 Agent 编排与离线评测，C6 harness 使用固定时钟，可复现地测试时间距离。
8. C6 已覆盖回复长度/长度比、诊断降级、时间归属、reflection 上限与隐私边界；基线不允许自动刷新。
9. C8 已将 initial/reflection/material 纳入共享 24000ms provider-work budget；C9 无需新增 provider 子调用。

### partial / unknown

1. 现有月份标签能说明绝对时间，但不会计算“最近、几个月前、更早”等距离层级，能力为 `partial`。
2. 现有检索天然偏向最近记录，但没有显式、版本化的旧记忆注入衰减策略，能力为 `partial`。
3. 当前没有“重复主题可否提及”的确定性证据门槛，也没有对应的反分析越界检查，能力为 `unknown`。
4. 真实用户数据中能否稳定形成两条以上相关、已解锁且跨时间的记录证据，当前 `unknown`。
5. 真实 provider 对时间距离指令、克制重复主题提示的遵从率与体感收益，当前 `unknown`。
6. 微信真机上的话术体感与长文本布局尚未验证，当前 `unknown`。

### planned

1. 新增 L3 `agent/temporal` 策略模块，使用 `Clock` 与已有 `occurredAt` 形成封闭距离层级。
2. 对“关联历史片段”施加确定性的上下文字符预算衰减；不删除记录、不改变 SQL 时间窗口。
3. 对用户主动打开的回看目标记录保持既有注入预算，不因年代久远而削弱其主体地位。
4. 只有在回看模式、用户明确询问比较/反复、且存在足够跨时间证据时，才生成一次克制的重复主题提示。
5. 新增确定性的 temporal overreach 检查，拦截百分比、评分、必然规律、因果诊断与预测式话术。
6. 将策略版本、距离桶分布、衰减结果与 pattern eligibility 写入无内容 trace/eval，不记录日记或片段原文。

### out_of_scope

- 情绪趋势、成长报告、心理诊断、人格画像、分数或“焦虑 +40%”；
- dashboard、图表、统计卡片、新页面、新 Tab、主动推送与解锁提醒；
- 自动标签、向量检索、全文索引、embedding、外部记忆引擎；
- 删除、过期或降权用户的业务记录；修改封存后 location、attachments、cover；
- 新 API、DTO 字段、数据库表/列、Maven/npm 依赖、package/lockfile；
- 新 provider 调用、LLM-as-Judge、自动 provider retry、多 provider 路由；
- C10 语气标定、C11 上下文架构或 C6 既有阈值校准。

## 4. 规划期事实修正

1. “记忆衰减”不能解释为删除旧记录或缩短 24 个月业务可见窗口；C9 第一阶段只衰减**关联历史片段的 prompt 注入字符预算**。
2. 回看目标记录不是偶然检索命中，而是用户主动打开的主体；它不参与衰减，只有旁支关联记忆参与。
3. 两条相似记录不足以证明“周期”；C9 只允许说“似乎不止一次出现过”，禁止“每隔一段时间”“每年都会”等周期结论。
4. 当前 SQL 只提供命中候选与时间顺序，不提供可靠相关性分数；C9 不把 recency 伪装成 relevance，也不在本刀重做检索排序。
5. C9 不需要额外 provider 调用；时间策略应是 provider 调用前的确定性上下文策略与调用后的确定性越界检查。

## 5. Goals

1. 让写作引导与回看对话能够区分最近、数月前与更早的记忆，并保留准确月份锚点。
2. 让越旧的旁支记忆以越小的上下文预算进入 prompt，降低旧事压过此刻表达的风险。
3. 让回看对话在证据充分且用户主动询问时，最多一次地提到“重复出现的主题”，把解释权交还给用户。
4. 用确定性规则阻止时间话术滑向趋势分析、诊断、评分、因果归纳或预测。
5. 复用 C3 memory port、C4 guardrails、C5 trace、C6 eval、C7 bounded reflection 与 C8 budget，不另建平行链路。
6. 保持 API/DTO/DDL/前端结构不变，避免把 Agent 策略升级扩成产品分析功能。

## 6. Non-Goals

1. 不承诺识别真正的心理/行为周期，也不输出统计显著性或因果关系。
2. 不对用户建立长期画像、情绪曲线、风险标签或“成长结论”。
3. 不改变历史检索字段、24 个月窗口、owner/status 隔离或正文不参与匹配的边界。
4. 不修改 C4/C7 既有阈值、reflection eligibility 或最大调用数。
5. 不让 temporal hint 进入用户记录正文、素材、工具参数或新的持久化位置。
6. 不新增用户可见开关、时间智能设置页或检索内部状态展示。

## 7. 用户故事

### 故事 A：时间距离

- 改前坏事：Agent 只会说“2025 年 3 月”，无法体现这件事离现在已经很久，回复仍像一次数据库引用。
- 改后不同：Agent 得到“更早的一段记录 + 2025 年 3 月”的受控上下文，能以朋友口吻承认时间距离，同时不替用户总结变化。

### 故事 B：旧记忆不压过此刻

- 改前坏事：两年前的旁支记忆与最近记忆拥有同样注入长度，旧内容可能在 prompt 中占据过多分量。
- 改后不同：旁支记忆越久远，注入预算越小；当前会话与用户主动打开的回看记录仍保持主体地位。

### 故事 C：克制地提到重复主题

- 改前坏事：没有证据门槛时，Agent 要么完全看不见跨时间重复，要么容易把两段相似文字说成“你的规律”。
- 改后不同：只有用户在回看中主动问“是不是又这样/和以前相比呢”，且至少两条不同历史记录跨越足够时间时，Agent 才能最多提示一次“似乎不止一次出现过”，并邀请用户自己判断。

### 故事 D：拒绝分析化

- 改前坏事：模型可能把温和时间提示扩写成“你的焦虑频率提高了 40%”或“每到春天必然复发”。
- 改后不同：确定性 temporal checker 将量化评分、绝对规律、因果诊断与预测式话术降级；前端不会出现分析面板或评分。

## 8. 建议待裁决项

| 编号 | 推荐方案 | 备选与代价 |
|---|---|---|
| N1 距离层级 | `RECENT` 0–30 天、`DISTANT` 31–180 天、`LONG_AGO` 181 天以上、`UNKNOWN`；精确月份标签始终保留 | 更细层级更像分析系统；只分新旧又无法表达数月距离 |
| N2 衰减对象 | 只衰减从 memory port 检索到的旁支片段；回看目标记录不衰减 | 全部衰减会削弱用户主动打开的记录；按数据库删除/过期破坏产品语义 |
| N3 字符预算 | 在既有 `max-fragment-chars` 上按 100% / 75% / 50% 截断，最低 40 字；`UNKNOWN` 按 50% fail-safe | 引入概率抽样不可复现；重做相关性评分扩大 C3/R9 范围 |
| N4 重复主题门槛 | 仅 `REVIEW_CHAT` + 当前用户消息含比较/反复意图 + 至少 2 个不同旁支记录 + 已知时间跨度 ≥90 天；每轮最多一个 hint | 写作引导主动提模式更打扰；无显式意图就提容易像画像 |
| N5 表达上限 | 只允许“似乎不止一次/要不要一起看看有没有关系”，禁止声称周期、趋势、原因、预测或改善/恶化 | 直接说“规律/周期”证据不足；完全不提又无法兑现蓝图目标 |
| N6 越界处置 | 新增内部 `TEMPORAL_OVERREACH`，直接走既有安全兜底，不进入 C7 reflection | 让 reflection 重写会扩大 eligibility 与调用预算；只靠 prompt 无法形成硬边界 |
| N7 外部契约 | API/DTO/DDL/页面结构零变化；只新增 backend config、策略、trace/eval | 新字段会把内部判断暴露为产品分析状态，并扩大前后端契约 |
| N8 策略开关 | 新增 backend-side `temporal.enabled`；关闭时等价于 C8 行为并留无内容结构化痕迹 | 无开关难以回退；前端开关会制造用户可见设置面 |

## 9. 外调预算与闸门 3

- **规划期外调预算：0**。本轮不调用真实 provider、不连接真实 MySQL、不做真机操作。
- 实现授权不自动包含闸门 3。
- 若后续单独批准闸门 3，推荐真实 provider 总调用上限 **6**：2 次时间距离、2 次回看锚点、2 次重复主题/反例；全部使用固定合成文本，不发送真实日记。
- 真实 MySQL 只使用可清理的合成用户/记录，验证时间字段、owner/status 与不同年龄片段；不能以 H2 冒充。
- 微信真机只验证现有回看浮层的话术长度、失败态与无新增分析 UI；无可控环境则记 `SKIPPED`。
- 任一敏感内容进入日志/证据、调用数超限、前端 timeout、身份/config 漂移时立即停止。

## 10. 验收标准

1. 新 temporal 策略位于 L3 backend 模块，使用注入的 `Clock`，无系统时间散落调用。
2. 距离层级边界在固定时钟下可复现，未来时间/null 时间进入 `UNKNOWN`，不产生虚假相对时间。
3. 精确月份 `timeLabel` 继续保留；相对距离只是补充，不替代来源锚点。
4. 旁支记忆按 100% / 75% / 50% 字符预算衰减，且不低于 40 字、不超过原配置。
5. 用户主动打开的回看目标记录保持既有 `review.record-excerpt-chars` 预算。
6. 衰减不删除/修改业务记录，不改变 24 个月窗口、SQL owner/status 谓词或检索字段。
7. 无线索仍不查库；检索失败仍 fail-open 于增强能力且不放宽护栏。
8. pattern eligibility 只在回看模式、显式比较/反复意图、两个不同旁支记录、跨度 ≥90 天时成立。
9. pattern hint 每轮最多一个；不足证据、时间未知或仅有目标记录时不生成。
10. temporal hint 只进入 prompt 内存上下文，不落 agent_message、trace、日志、工具审计或记录正文。
11. `TEMPORAL_OVERREACH` 至少覆盖百分比/评分、绝对频率、必然因果、诊断与预测式表达。
12. temporal overreach 直接降级，不触发 reflection，不增加 provider 调用。
13. 既有 MISSING_TIME_ATTRIBUTION、诊断、代决、伪引用、长度与忠实度护栏全部保持。
14. 非 CLOSING reply、CLOSING reply+material 与 finish 的 C8 调用数上限不变。
15. API 路径、请求/响应 DTO、status/message、数据库 schema 与前端 store/component 结构不变。
16. 三个一级 Tab 与“我的记录、时光轴、时间回看”命名不变；无 dashboard/图表/评分卡。
17. trace 只记录策略版本、距离桶计数、衰减后字符数、pattern eligible/used 与违规枚举，不记录内容。
18. policy fingerprint 纳入版本锚点；策略变化可通过 C5/C6 证据追踪。
19. C6 新增固定时钟边界、衰减、pattern 正反例、overreach、no-extra-call 与隐私用例。
20. 既有 snapshot 只有 C9 合法改变的用例可变化；必须逐条人工确认并同步 `baselineNote` + checksum。
21. 后端 focused 与全量测试 PASS；新增 skip 为 0。
22. 前端至少执行 type-check 与 mp-weixin build，证明零契约变化未破坏消费端。
23. `git diff --check`、范围路径审计与增量敏感模式扫描 PASS。
24. 真实 provider/MySQL/真机只有闸门 3 授权后执行；未授权必须写 `SKIPPED + 原因`。
25. OpenSpec CLI 缺失时只报告文件级结构/delta 对齐，不声称 CLI validation PASS。

## 11. Spec Delta 落点

- `agent-runtime`：距离策略、注入衰减、pattern gate、temporal overreach 与既有调用上限。
- `backend-core`：backend-side config、Clock、API/DTO/DDL 零变化与复用现有 memory port。
- `v2-product-scope`：朋友式时间感、反分析/反诊断、被动召唤与无 dashboard。
- `miniapp-core`：复用既有回看浮层与状态，不新增分析 UI/字段。
- `agent-collaboration`：固定时钟离线证据、内容零泄露、闸门 3 合成探针预算。

## 12. 关键风险

1. 30/180/90 天和 100/75/50% 是规划推荐值，尚未由真实样本校准；批准后仍应标记为 `planned`，直到验证完成。
2. 当前检索相关性较弱；pattern gate 即使满足，也只能说明“同一线索命中过不止一次”，不能证明周期或因果。
3. 确定性词表只能拦截声明过的越界形态，不能证明所有自然语言都绝对无分析意味；真实 provider 质量仍需人评。
4. 缩短旧片段可能截断关键上下文；focused tests 必须覆盖中文截断、最小预算与来源层一致性。
5. 新增内部违规类型若误接入 C7 reflection，会突破调用预算；需要调用数不变量守护。
6. C9 若触碰 mapper 排序、外部 DTO 或前端页面，即为 scope drift，应停止并回到闸门重新裁决。

## 13. Gate State

- readiness：**GO**。
- 闸门 1（规划批准）：**已批准**（2026-08-08，N1–N8 按推荐方案）。
- 闸门 2（实现授权）：**已授权**（2026-08-08）。
- 闸门 3（真实 provider / MySQL / 真机）：**未授权**。
- Git：**Agent commit 已授权**；`push` 仍未授权。
