# Active Task

## Status

`ACTIVE`

## Task

- Change：`agent-tool-calling`（C2）
- 位置：`openspec/changes/agent-tool-calling/`
- 阶段：**规划闸（闸门 1）—— Q1–Q5 已定稿，待用户批准放行**，**零业务代码**
- 协议定稿：原生 OpenAI-compatible Function Calling + DeepSeek strict mode，**无降级路径**
- 目标：Agent 可在对话中调用受白名单约束的后端工具，对话过程自然产生行动（提议 → 用户确认 → 受控执行 → 结果回注）
- 开工锚点：`63d1767`（工作区干净）
- 上游方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md` v1.1 §4 C2（已冻结）

## Gate Status

| 闸门 | 状态 |
|---|---|
| 1 规划批准 | **已放行**（2026-07-27），Q1–Q5 已定稿 |
| 2 实现授权 | **已取得**（2026-07-27）；后端 + 前端实现完成，本地验证全绿 |
| 3 外调授权 | **已取得**（2026-07-27）。真实 DeepSeek FC + strict mode 验证 **PASS**，用量 5 / 预算 45。事前已确认本地 `AI_PROVIDER`，未重演 C1 偏差。微信端到端手验待用户复验 |

> 提交责任：**已变更为 Agent 代为提交**（用户授权），验收仍由用户进行。`push` 未授权。

## 规划闸决策（已定稿）

| # | 决策项 | 定稿结论 |
|---|---|---|
| Q1 | 提议协议 | **原生 OpenAI-compatible Function Calling + DeepSeek strict mode，唯一路径、无降级**。FC 不可用即显式 `UNAVAILABLE` |
| Q2 | Tool 白名单 | 写工具 `append_record_content` / `add_record_tags` / `propose_unlock_at` 下发为 FC tools；读工具 `list_available_tags` / `read_draft_snapshot` 改为 **prompt 预注入**（不注册为 FC tool）；seal/delete/unlock/location/cover/attachment/later-reflection/标签创建全部代码级排除 |
| Q3 | 持久化落点 | 新表 `agent_tool_call`（不动 `agent_message` 唯一键） |
| Q4 | 执行入口 | 新增 `POST /api/agent/sessions/{sid}/tool-calls/{tid}/confirm` |
| Q5 | spec delta 落点 | agent-runtime + backend-core + miniapp-core + v2-product-scope |

### Q1 定稿的关键约束

- **不做单轮内 FC 循环**：`tool_calls` 只转成待确认提议，本轮结束；执行在用户确认的独立请求里（design 决策 9）
- **单轮至多一个提议**：多个 `tool_calls` 只取第一个合法项，其余记审计（design 决策 10）
- **strict mode 不支持** `maxLength` / `maxItems` / `minItems` → 长度与数量边界必须留在 `AgentToolValidator` 代码层（design §3.2）
- **既有链路不碰**：`complete()` 与三个单轮 AI 端点继续走 `response_format=json_object`
- **不引入 MCP / Spring AI / LangChain4j**（design 决策 11，方向记入 proposal §12）

> Q2 附带确认项：标签「追加」需在 `RecordService` 新增 `appendTags`，因既有 `PUT /api/records/{id}` 是全量重绑且要求 content 非空（design 决策 5）。

## Previous Completed

- Change：`agent-runtime-mvp`（C1）
- 位置：`openspec/changes/archive/2026-07-27-agent-runtime-mvp/`
- 结果：Backend Agent Runtime 基底 + 「写下此刻」多轮引导 + 最小护栏内嵌，2026-07-27 用户真机验收通过并归档；delta 已接受进 baseline（新建 `agent-runtime` capability，另在 `backend-core` / `miniapp-core` / `v2-product-scope` 追加接受段落）
- 验证：backend 254 tests PASS；frontend type-check + `build:mp-weixin` PASS；真实 MySQL DDL 已执行校验；真实 DeepSeek 4 轮联调成功；微信手验通过
- 提交：`602b31b`（用户授权 Agent 代为提交；未 push）
- 流程偏差：闸门 3 未事前取得——手验时本地 `AI_PROVIDER=deepseek` 导致手验即真实外调。已披露并记入 AGENT_LOG；**C2 须避免重演**
- 更早：`m4-real-capability-completion`（`archive/2026-07-27-m4-real-capability-completion/`）

## Direction Layer

- **迭代蓝图已冻结**：`Docs/agent-iteration/roadmap/iteration-blueprint.md` **v1.1**
- 主线进度：C1 已归档 → **C2 `agent-tool-calling` 规划中** → C3 → C4 → C5（规则见蓝图 §3.2）
- 蓝图 §3.2 允许在气质越界时将 C4 前移；C1 手验未观察到越界，按默认顺序执行 C2

## Source Of Truth

- `AGENTS.md`
- `openspec/changes/agent-tool-calling/`（**active change**：proposal / design / tasks / specs delta）
- `openspec/specs/agent-runtime/spec.md`（C1 已接受的 Agent 核心契约，C2 的直接基础）
- `openspec/specs/backend-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/miniapp-core/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/v2-product-scope/spec.md`（含已接受 M4 + C1 条款）
- `openspec/specs/agent-collaboration/spec.md`
- `openspec/project.md`
- 方向：`Docs/agent-iteration/roadmap/iteration-blueprint.md`（**已冻结 v1.1**）
- 工作流：`Docs/agent-iteration/workflow/`
- 开工清单：`Docs/agent-iteration/workflow/prompt-snippets/type-c-checklist.md`

## Current Progress

- **Last session**: 2026-07-27 — C2 规划闸 + 闸门 2 实现全部完成。规划阶段 Q1 经官方文档核查后由「自研 JSON 协议」翻转为「原生 FC + strict mode」；实现阶段完成 Tool 层、持久化、执行层、会话集成、确认端点与前端确认交互
- **Completed**: T-00 ~ T-36 全部勾选（规划 T-00~T-02b；实现 T-03~T-36）
- **Blocked on**: 用户真机复验（JSON 显示缺陷已修，需确认）；以及是否将 C4 前移的决定
- **Next step**: 用户复验对话回复为自然文本 + 提议→确认→执行链路 → 决定 C4 是否前移 → 验收归档
- **Verification**: backend `mvn -B test` **339 PASS**；frontend `type-check` PASS、`build:mp-weixin` PASS；真实 MySQL DDL PASS；真实 DeepSeek 原生 FC + **strict mode 均 PASS**（F23 的核心不确定性已消除，strict schema 被服务端接受）
- **闸门 3 关键结论**：原生 FC 与 strict mode 在本仓库首次实测可用，design §4 的降级处置顺序无需启用；`content` 为空仅有 `tool_calls` 的情形真实发生，证实 `askText` 兜底必要
- **Commit**: 由 Agent 代为提交（见 AGENT_LOG 补录 hash）；**未 push**
- **实现期新增决策**：design 决策 12（待确认提议的执行参数存放方式 → 瞬态 `pending_args` 列，终结即清），已同步 spec delta
- **已知无需修复**：`design.md` / `tasks.md` 在 Kiro 诊断中报「缺少 Kiro Spec 章节」。本项目按规则使用 OpenSpec 而非 `.kiro/specs/`，已归档并验收的 C1 同款文档报同样的诊断，**属误报，不修**

## Residual / Carry-over

### C2 手验发现（2026-07-27，用户真机）

- **[已修复｜已二次确认] 对话气泡显示 JSON 原文**：回复呈现为 `{"reply":"..."}`。根因＝C2 把解析路径从 `extractText(raw,"reply")` 改为直接读 `message.content`，但 `buildSystemPrompt` 仍要求模型「只输出 JSON」，模型照做而后端不再剥壳。修复＝改写输出要求为直出自然语句 + 新增 `normalizeReplyShape` 形状兜底 + 回归守门测试。**属 C2 引入的缺陷，非模型问题**
  - **7-28 复验时用户仍看到 JSON → 已查明为运行实例未重启，非修复无效**。证据：含 JSON 的消息落库时间 `2026-07-28 09:21`（`agent_message` id=21、turn_no=0），而 `AgentPromptBuilder.class` 编译于 `08:56`，即进程加载的是修复前代码
  - 并已用真实 provider 单独验证修复后的 prompt：返回 `starts_with_brace=False`、无 `reply` 字段（外调 1 次）
  - **待办：用户重启后端后即可消失，无需再改代码**
  - 库中 6 条历史 JSON 消息（id 13/15/17/19/20/21）为修复前产生的既有数据，不会自动清理；如影响观感可手动清库，属本地数据非代码问题
- **[新发现｜闸门 3 真实联调暴露] 工具提议话术越过「不改写原文」边界**：真实 DeepSeek 返回的 `askText` 为「我帮你把这两句**整理了一下**，想给你加到记录里」，且 `text` 参数确实把用户两轮口语**改写重组**成了通顺长句（原话为「我学的是软件工程，一直想做后端」+「刚才说的这些我觉得挺重要的，想留下来」，返回值增写了「但最近心里有点空，不知道该不该继续沿着这条路走下去，方向是不是对的，自己也说不清楚」——**后半句用户从未说过**）。
  - 严重性：这直接触碰 `AGENTS.md` Non-Negotiable「不改写用户原文」与 `agent-runtime` spec 的「引用用户表达时 SHALL 原样引用」。**当前白名单与二段式确认拦不住它**——它们只管「能不能执行」，不管「参数内容是否忠实」
  - 与用户提出的「素材生硬」问题是**同一根源的两面**：prompt 要求「只用用户说过的内容」，模型要么逐句拼接（生硬），要么自行润色（越界）。中间地带需要明确定义
  - 归属：本质是**内容忠实度**校验，属 C4 系统化护栏范畴（后置检查 + 违规降级），C2 的白名单是权限校验、二者不重叠（design 决策 7 已划清此边界）
  - 建议：C4 优先级应上调；蓝图 §3.2 允许在观察到气质越界时把 C4 前移到 C3 之前，**本次已构成该条件的实证依据**

- **[待优化，不在 C2 范围｜用户已明确要求延后] 引导问题突兀 + 素材拼接生硬**：`buildMaterialMessages` 现要求「只使用用户说过的内容 / 尽量保留用户自己的措辞」，模型将其执行为**逐句拼接原话**。用户口语是断断续续的，直接拼接会导致句子逻辑不连贯。
  - **7-28 手验实例（用户反馈「比上一版还突兀」）**：开场问「现在的你，更像哪种天气？」→ 用户答「像晴天也像雨天」→ 追问「是忽晴忽雨的那种，还是同时有光也有雨的感觉？」→ 用户反问「这两种不是差不多吗？我也说不清楚，你为什么要这样问？」。最终素材拼成：「心情像晴天也像雨天，但觉得这两种差不多，说不清楚为什么被这样问。或许难过多一些。人无法预知此刻的价值，直到此刻成为了回忆。」
  - **两层问题**：① **引导策略**——用比喻式提问（天气）开场，在用户答不上来时继续追问比喻细节，而非退回具体情境，导致用户被问烦并质疑提问动机；② **素材合成**——把用户的**反问句与困惑本身**也当作素材拼进正文，产生「来记录心事，却被问了几个突兀问题，还把答案拼在一起」的观感
  - 用户判断：接近大模型微调范畴，**C2 阶段不做优化**，待 C1–C5 全部完工后统一收集问题逐个处理
  - 注意 ① 与 `AgentStageMachine` 的 `stageGoal` 文案直接相关（`OPENING/EMOTION` 阶段目标写的是「不要问抽象的大问题」，但模型把「天气比喻」当成了具体问题），属 prompt 与阶段目标的落差
  - 张力所在：产品硬约束是「不改写用户原文」（`AGENTS.md` Non-Negotiable + `agent-runtime` spec），而「让素材读起来连贯」天然需要一定程度的重组。**这两者的边界属产品语义决策，须用户拍板，不能由实现自行放宽**
  - 候选方向（仅备选，未批准）：① 仅允许衔接性调整（连接词、语序），不改实词与语义；② 素材分段呈现而非合成一段，由用户自行取舍；③ 提供「原话版 / 顺过一遍版」两个候选让用户选
  - 建议承载：独立 Type B（仅 prompt 调优且不触碰契约）或纳入 C3 —— **须先向用户确认走哪条，以及「顺一遍」是否已越过「不改写原文」**

### 更早遗留

- **C1 遗留**：素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错 → 已纳入 C2 设计（后端 `appendContent` 路径，`design.md` 决策 5）
- **C1 已接受风险**：最小护栏仅 system prompt + 长度裁剪单层，C4 系统化补齐；C2 沿用同一立场
- **C1 已接受风险**：真实 provider 仅验 4 轮，长会话稳定性待 C2 观察
- **M4 carry-over**：MySQL `EXPLAIN` timeline（Type B，不阻塞主线）
- **本地环境**：MySQL80 StartType=Manual，重启后需手动启动
- **[已完成] secret 外移**：`start-dev-wechat.local.ps1` 的明文凭证已迁出到 `backend/secrets.local.env`（gitignore），脚本改为 `Get-LocalSecret` 读取，缺键时快速失败而非静默用空值继续。入库的是 `secrets.local.env.example` 模板。迁移 5 项：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`
  - **仍待用户执行：轮换这些凭证**。原因＝本轮我的 grep 范围过宽，曾将 `AI_API_KEY` 与 S3 AK/SK 打印到终端输出，取值已存在于会话记录中
  - 备份 `start-dev-wechat.local.ps1.bak` 含旧明文，已 gitignore；**轮换完成后建议删除该备份**
- **本地数据**：`agent_message` 已存真实对话数据，重置数据库时注意
- **未 push**：`main` 领先 `origin/main`（C1 提交），待用户自行决定推送

## Out Of Scope While This Change Is Active

- 未获闸门 1 批准前不写任何业务代码
- 不做 Memory / 历史记录检索 / 跨记录关联（C3）
- 不做后置内容过滤 / 违规降级 / 边界用例集（C4）
- 不做决策链路查询端点或可观测界面（C5）
- 不让 Agent 触达 seal / delete / unlock / location / cover / attachment / later-reflection / 标签创建
- 不改 C1 四个既有 Agent 端点的字段语义，不改 `/api/ai/**` 与 `/api/stage-summaries/**` 契约
- 不引入 FC → 自研 JSON 提议协议的降级路径
- 不在生成回复的同一处理过程内执行工具或回灌 tool 结果
- 不引入 MCP / Spring AI / LangChain4j
- 不改三 Tab、不改用户可见命名、不做视觉大改、不改 package / lockfile
