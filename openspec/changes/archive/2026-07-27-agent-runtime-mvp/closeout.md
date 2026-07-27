# C1 `agent-runtime-mvp` Closeout

- **Change**: `agent-runtime-mvp`（C1，蓝图 v1.1 §4）
- **Type**: C
- **开工锚点**: `b6140b3`
- **规划批准**: 2026-07-27（Q1–Q4 全按推荐方案定稿）
- **实现授权**: 2026-07-27
- **用户验收**: 2026-07-27（真机手验后确认「可以视为验收通过」）
- **提交责任**: 用户手动提交（Agent 未执行 `git add` / `commit` / `push`）

---

## 1. 交付内容

### 后端（新增 `agent` 模块，未重写既有代码）

- `agent/AgentStageMachine` + `AgentStageDecision`：7 阶段显式状态机（纯逻辑，无 IO）
- `agent/AgentPromptBuilder`：system prompt 组装、上下文滑动窗口、素材整理 prompt
- `agent/AgentGuardrailPolicy`：5 条最小护栏文案 + 回复长度硬裁剪
- `agent/AgentModelClient`：复用 `app.ai` provider/secret 与 OpenAI-compatible 调用形状
- `agent/AgentMockResponder`：mock provider 下的本地引导器（source 恒为 mock）
- `service/AgentChatService(+Impl)`：会话编排与失败语义
- `controller/api/AgentController`：4 个端点，纳入 `/api/**` 鉴权
- `domain` 5 项、`mapper` 2 项 + XML、`dto` 2 项、`vo` 2 项、`config/AppAgentProperties`
- DDL：`sql/mysql/c1-agent-runtime.sql` + `schema.mysql.sql` + 测试 H2 `schema.sql`

### 前端（编辑页内被动入口，未改主路径视觉）

- `services/agentService.ts`、`stores/agentChat.ts`
- `pages/record-editor/components/AgentChatSheet.vue`（半屏浮层）
- `pages/record-editor/index.vue` 接入入口与素材回填

---

## 2. 决策落实情况

| 决策（design.md） | 落实 |
|---|---|
| 1 持久化选 MySQL | `agent_session` / `agent_message`，真库已验证 4 个 CASCADE 外键与全部索引 |
| 2 对话落原文 | 只进业务表；日志仅结构化元数据 |
| 3 新模块而非改造 `AiServiceImpl` | 既有三个 AI 方法行为与契约零改动 |
| 4 显式状态机 | 阶段推进 100% 后端决定，模型不参与节奏控制 |
| 5 不引入 FALLBACK | 只用 `SUCCESS`/`UNAVAILABLE`/`FAILED` |
| 6 不做 Tool Calling | 单测 `verify(never())` 断言无记录写操作 |
| 7 不做后置过滤 | 仅 prompt + 长度裁剪，C4 补 |
| 8 半屏浮层 | 已实现，被动触发 |
| 9 新建 `agent-runtime` capability | 已接受进 baseline |

---

## 3. 验证结果

| 项 | 结果 |
|---|---|
| backend `mvn -B test` | **PASS** — 254 tests / 0 failures / 0 errors（新增 43） |
| frontend `type-check` | **PASS** |
| frontend `build:mp-weixin` | **PASS** |
| mock provider 端到端 | **PASS** — `AgentRuntimeIntegrationTest` 覆盖开场→多轮→恢复→结束→落库计数 |
| 真实 MySQL DDL | **PASS** — 表结构、外键、索引与设计一致 |
| 真实 provider 多轮 | **PASS** — DeepSeek 4 轮引导全部解析成功，无 `Agent provider issue` |
| 微信手验 | **PASS（修复后）** — 登录→开启→4 轮推进→阶段正确 |
| 最小护栏手验 | **PASS** — 回复均 1–2 句短问句、无诊断词、未改写原文 |
| 日记/对话原文入日志 | **未发现泄露** |

### 手验发现并修复的缺陷

`AgentChatSheet` 布局缺陷：`max-height` 配合 `min-height: 360rpx` 使 `scroll-view` 无确定高度、不启用内部滚动；消息累积后 composer 被顶出可视区导致**无法发送**，且小程序原生 `textarea` 层级高于普通元素造成**消息重叠**。修复为固定 `height: 78vh` + `min-height: 0` + 头尾 `flex-shrink: 0`，已复验。该场景已作为契约条款写入 `miniapp-core` baseline。

---

## 4. 流程偏差（如实记录）

**闸门 3（外调授权）未事前取得。** 用户手验时本地脚本 `AI_PROVIDER=deepseek`，因此手验即真实 provider 调用（约 4 轮用户消息 / 7 条 Agent 回复，远低于 proposal 申明的 30 次预算上限）。用量未超预算、未使用用户真实敏感日记做批量测试，但**流程上应先取得授权再联调**。已事后向用户披露并记入 `AGENT_LOG`。

后续 change（C2 起）在启动手验前须先确认本地 `AI_PROVIDER` 取值，或显式取得闸门 3 授权。

---

## 5. 遗留与后续

| 项 | 处置 |
|---|---|
| 素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错 | 已知缺口，手验未触发；Type B 修或纳入 C2 |
| 最小护栏仅 prompt 单层 + 长度裁剪 | **已接受风险**，C4 `agent-guardrails-hardening` 系统化补齐 |
| 真实 provider 长期输出稳定性 | 本次 4 轮全部成功；M4 曾观测到该模型结构化输出非 100% 稳定，C2 需继续观察 |
| `agent_message` 已存真实日记语义数据 | 本地重置数据库时注意 |
| MySQL `EXPLAIN` timeline | M4 carry-over，与 C1 无依赖 |
| 本地启动脚本明文存放 secret | 已向用户提示轮换与改为环境变量读取；脚本本身被 gitignore，未进版本库 |

---

## 6. 归档后状态

- baseline 接受：新建 `openspec/specs/agent-runtime/`；`backend-core` / `miniapp-core` / `v2-product-scope` 各追加 `Accepted From C1 Agent Runtime MVP` 段落
- 归档位置：`openspec/changes/archive/2026-07-27-agent-runtime-mvp/`
- `ACTIVE_TASK` → `IDLE`
- 主线下一刀：C2 `agent-tool-calling`（蓝图 §3.2 默认顺序，须新开 Type C 并走三道闸门）
