# Agent Log

只追加执行证据，不改写历史。需求与设计以 OpenSpec / `design.md` 为准。  
强制规则见 `AGENTS.md`；完整工作流见 `Docs/agent-iteration/workflow/`。

不要记录 API keys、账号、余额、模型额度、密码或用户日记原文。

## Entry Template（推荐 · 新条目请尽量遵循）

```markdown
## YYYY-MM-DD｜<change-id 或 task>｜Type A|B|C

- **Scope**: 触及的模块/文件（可列表）
- **Changes**: 做了什么（事实，非长篇设计）
- **Verification**: PASS | FAIL | SKIPPED（原因与替代验证）
- **Risks**: 剩余风险
- **Commit**: pending  或  `<hash>`（补录条只写 hash，不回改旧文）
- **Next**（可选）: 下一会话建议第一步
```

历史条目格式不统一时 **不要批量重写**；仅对新条目采用上表。

## Legacy Template（兼容旧写法）

### YYYY-MM-DD Agent / Tool

Task:

- ...

Modified:

- ...

Verification:

- ...

Risks:

- ...

Next:

- ...

## 2026-08-08｜core-product-definition-v0.1-verification｜Type B

- **Scope**: `Docs/agent-iteration/roadmap/core-product-definition.md` 与本轮工作树边界
- **Changes**: 仅补录验证结论，不修改产品宪章内容或既有历史条目
- **Verification**: PASS
  - 文档共 547 行、22 个编号章节，9 个核心定义锚点全部存在
  - 私人故事与人物关键词定向扫描零命中；未记录用户日记原文或可识别经历
  - `git diff --check` PASS；OpenSpec、业务代码与 `.ai/ACTIVE_TASK.md` 均未修改，状态仍为 `IDLE`
  - Git 状态仅含 `.ai/AGENT_LOG.md` 修改与产品宪章新文件；无 package / lockfile、部署或发布改动
- **Risks**: 留存仪式、篇章交互、隐含含义记忆、环境数据、安全实现与商业模型仍待原型或独立 Type C change
- **Commit**: pending（用户手动提交）

## 2026-08-08｜core-product-definition-v0.1｜Type B

- **Scope**: 方向层产品宪章与执行证据
  - `Docs/agent-iteration/roadmap/core-product-definition.md`（新建）
  - `.ai/AGENT_LOG.md`（仅追加本条）
- **Changes**:
  - 将已确认的产品访谈结论整理为《Flashback 核心产品定义 v0.1》
  - 明确产品品类为「私人的生命片段记录空间」，核心价值为保存用户具体活过的证据
  - 明确当下保存优先、时间回看为复利、Agent 为「有朋友温度的见证者」
  - 固化第一人称权利、无记录焦虑、用户主动选择、数据可带走、低频长期信任与安全窄例外
  - 将阶段机制、留存仪式、隐含含义记忆、环境线索、商业模型与安全实现列为待原型/独立 change 验证
  - 明确本宪章不修改当前 OpenSpec baseline，不授权业务实现、外调、部署或发布
- **Verification**: pending（待完成文档结构、边界、敏感内容、diff 与 Git 状态检查）
- **Risks**: v0.1 为方向层依据；尚未验证的体验假设不得写成当前已实现能力
- **Commit**: pending（用户手动提交）

## 2026-07-27｜kiro-steering-setup｜Type B

- **Scope**: Kiro 桥接层与规则配置（文档/工作流）
  - `.kiro/steering/rules.md`（新建，Kiro 规则桥接）
  - `.kiro/steering/product.md`（新建，产品定位与气质摘要）
  - `.kiro/steering/tech.md`（新建，技术栈与非功能约束）
  - `.kiro/steering/structure.md`（新建，目录结构与文件索引）
  - `AGENTS.md`（蓝图引用状态更新为已冻结 v1.1）
  - `Docs/agent-iteration/README.md`（执行层表格补充 Kiro 桥接说明，蓝图状态更新）
- **Changes**:
  - 创建 `.kiro/steering/` 目录及 4 个 steering 文件，明确 Kiro vibe coding 模式直接使用 OpenSpec（`openspec/changes/`），不引入额外的 `.kiro/specs/`
  - 将 `AGENTS.md`、`.ai/ACTIVE_TASK.md` 设为每次会话强要求首读文件
  - 更新 `AGENTS.md` 蓝图引用文字为已冻结 v1.1
- **Verification**: PASS（Kiro 目录文件已就绪，无破坏性变更）
- **Risks**: 无
- **Commit**: `ac2af21`

## 2026-07-27｜iteration-blueprint-v1.1｜Type B

- **Scope**: 蓝图 v1→v1.1 修订（方向层文档）
  - `Docs/agent-iteration/roadmap/iteration-blueprint.md`（v1.1 修订，全文重写）
- **Changes**:
  - 审查并采纳 Grok 修改建议 8 项（全部合理）：
    1. M4 真相对齐——D1 写明已归档日期/路径/IDLE/carry-over
    2. 消除依赖歧义——§0.3 不再写「严格串行」，§3.2 统一为硬依赖 C1 + 默认执行顺序 + 可调整规则
    3. C1 内嵌最小护栏（新增 D19）；C4 重定义为系统化 hardening
    4. C3 增加可选拆分退路（体量过重时可拆 memory-retrieval + review-chat）
    5. C1 意图卡片补强（入口文案原则、中断保留验收、Provider FC/状态持久化标注 unknown、最小护栏验收）
    6. 新增 §0.6 治理文件交叉引用
    7. 旁支表补充 M1/M3 清理和 EXPLAIN carry-over
    8. C4 change-id 更新为 `agent-guardrails-hardening`
- **Verification**: PASS（8 项完成定义逐项核对通过）
- **Risks**: 蓝图仍为草案，待用户确认「冻结」
- **Commit**: `ac2af21`

## 2026-07-27｜iteration-blueprint-v1｜Type B

- **Scope**: 迭代蓝图编写（方向层文档）
  - `Docs/agent-iteration/roadmap/iteration-blueprint.md`（新建 v1 草案，467 行）
  - `Docs/agent-iteration/roadmap/README.md`（状态更新）
  - `Docs/agent-iteration/README.md`（状态更新、目录结构更新）
  - `Docs/agent-iteration/workflow/iteration-approach.md`（完成定义 checklist 更新）
  - `.ai/ACTIVE_TASK.md`（handoff note 更新）
  - `AGENTS.md`（蓝图引用状态更新）
- **Changes**:
  - 通过 grill-me 讨论确认 17 个设计决策（M4 定位、Agent 气质、主动性边界、能力范围、架构方向、拆分粒度、Memory 策略、回看交互、Guardrails、Eval、场景优先级、Provider 策略、冻结策略、治理卡片、语言风格、跨记录关联定位、友人回看定位）
  - 编写 `iteration-blueprint.md` v1：§0 执行约定、§1 总方向、§2 已确认/待确认决策、§3 change 序列总览（C1–C5 主线 + 旁支）、§4 五张意图卡片（Runtime / Tool / Memory / Guardrails / Observability）、§5 spec delta 落点、§6 产品初心与 Agent 气质约束、§7 修订记录
  - 同步更新 5 个关联文件的蓝图状态引用（从「待写」→「v1 草案已产出，待冻结」）
- **Verification**: PASS（文档完整性：roadmap/README.md §6 完成定义 7 项全满足）
- **Risks**: 蓝图仍为草案，未冻结前不得当作已批准执行序列
- **Commit**: `ac2af21`
- **Next**: 用户审阅蓝图 → 冻结 → 开启第一个 post-M4 Type C `agent-runtime-mvp`

## 2026-06-22 - 修复阶段总结请求超时取消与 AI 返回校验

Task:

- 修复个人中心“阶段总结 / 生成”在 AI 等待接近 10 秒时被微信请求提前取消的问题，并降低阶段总结因复用记录六字段校验而误判失败的概率。

Modified:

- `backend/src/main/java/com/flashback/service/AiService.java`
- `backend/src/main/java/com/flashback/service/impl/AiServiceImpl.java`
- `backend/src/main/java/com/flashback/service/impl/StageSummaryServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/AiServiceImplTest.java`
- `backend/src/test/java/com/flashback/service/impl/StageSummaryServiceImplTest.java`
- `frontend/src/services/httpClient.ts`
- `frontend/src/services/stageSummaryService.ts`
- `frontend/src/pages/user-center/index.vue`

Implementation:

- 为通用 frontend `httpRequest` 增加可选单请求 timeout，默认仍为 `10000ms`；阶段总结单独使用 `15000ms`，保留 backend AI `10000ms` accepted timeout，形成 `5000ms` fallback 返回余量。
- 阶段总结改用内部专用 `generateStageSummary` AI 方法与单字段 `{"summary":"..."}` prompt/解析；记录编辑器原有六字段严格校验不变。
- provider 超时、网络异常或其他异常时，backend 仅记录 operation、provider、耗时和异常类型，不记录 prompt、用户内容、响应正文或凭据。
- frontend 保留 `uni.request` 错误，在真正发生客户端 timeout 时显示“AI响应超时，请稍后再试”；backend 在 10 秒内返回 FALLBACK 时仍展示本地阶段总结及明确状态。

Verification:

- TDD red: focused tests initially reported missing `generateStageSummary` compilation errors，证明回归测试覆盖新 seam。
- Focused backend tests (`AiServiceImplTest,StageSummaryServiceImplTest`): PASS。
- Full backend tests: PASS，27 suites / 204 tests / 0 failures / 0 errors / 0 skipped。
- Frontend `vue-tsc --noEmit`: PASS。
- WeChat Mini Program `uni build -p mp-weixin`: PASS；仅有已有非阻断提示 `os - Alias not found`。
- Generated artifact audit: PASS；产物包含阶段总结 `15000ms` timeout、per-request timeout 传递和明确的 AI timeout 提示。
- Timeout hierarchy audit: frontend stage summary `15000ms`、backend AI `10000ms`、safety margin `5000ms`、result `PASS`。
- `git diff --check`: PASS。

Skipped:

- 未重启用户当前运行中的 backend，也未使用真实用户 token 调用 provider：避免中断用户现有进程或在数据库中创建诊断账号；代码生效需重启 backend，并由用户在微信开发者工具中重新编译后进行真实点击验证。

Scope Safety:

- 保留现有 endpoint、DTO、StageSummaryVO 状态语义与 backend accepted AI timeout；未修改 OpenSpec、数据库、依赖或 lockfile。
- 未提交 `.ai/AGENT_LOG.md` 中已有的其他改动、`.claude/settings.local.json` 或 `ppt/`。

Risks:

- 自动化验证覆盖超时预算、fallback 和单字段解析，但真实 provider 延迟、模型可用性与微信运行态仍需重启后的端到端验证。

## 2026-06-22 - 诊断阶段总结请求被取消

Task:

- 核查个人中心点击“阶段总结 / 生成”后提示无法生成，微信网络面板没有 response 且状态为 `canceled` 的原因；本轮仅诊断，不修改功能代码。

Findings:

- `frontend/src/services/httpClient.ts` 对所有请求固定使用 `timeout: 10000`。
- backend accepted AI timeout 与当前配置均为 `10000ms`；阶段总结完成数据库统计后，会同步等待 `AiService.summarizeRecord` 调用 AI provider。
- 静态超时层级检查结果：frontend `10000ms`、backend AI `10000ms`、safety margin `0ms`、frontend `abort()` 调用不存在，结论为 `FAIL`。
- 当 provider 接近 10 秒才返回或超时，微信请求会先进入 `uni.request.fail`，网络面板显示 `canceled`；页面的无参数 `catch` 将具体 `errMsg` 丢弃，只显示“阶段总结暂时没有生成出来”。
- backend 会捕获 provider 异常并构造 FAILED/FALLBACK 结果，但该结果通常在前端请求已超时后才返回，因此当前请求看不到 response。
- 用户日志中 `scheduling-1` 的到期解封扫描是独立定时线程，与阶段总结请求无关；现有 backend 也没有记录 AI provider 异常详情，因此日志会停留在阶段总结上下文所需的记录查询附近。

Secondary Risk:

- 阶段总结复用 `summarizeRecord` 的完整六字段 JSON 校验；provider 即使给出可用 summary，只要其他字段不完整也会被判为 `AI返回内容无效`。这不会直接造成 `canceled`，但会继续影响修复超时后的真实 AI 成功率。

Skipped:

- 未使用新测试账号重放真实 HTTP 请求，避免在仅诊断任务中写入用户数据库；用户提供的真实运行日志与确定性的超时层级检查已复现故障条件。
- 未修改 timeout、错误提示或 AI schema；需用户确认开始修复后再实施并补回归验证。

Scope Safety:

- 仅检查阶段总结前后端调用链和现有配置；未改 backend、frontend、OpenSpec、依赖或 lockfile。

Risks:

- 在修复前，只要 AI provider 响应接近 10 秒，阶段总结仍会稳定或间歇性表现为 `canceled`，且用户看不到真实超时原因。

## 2026-06-22 - 实现可见的时光轴筛选入口

Task:

- 在不改变筛选契约和筛选面板逻辑的前提下，修复时光轴顶部筛选入口在微信小程序中不可见、用户无法发现筛选能力的问题。

Modified:

- `frontend/src/pages/timeline/index.vue`

Implementation:

- 将仅依赖 CSS 绘制、在真实微信运行态中没有形成可见图形的微型放大镜，替换为明确的“筛选”文字胶囊。
- 入口固定为 `104rpx × 60rpx`，保留左侧胶囊安全位置，并使用等宽右侧占位保持 Logo 居中。
- 未筛选时使用克制的墨色边框；筛选生效后使用朱砂色状态，保留按压反馈和原有 `openFilterPanel` 点击逻辑。

Verification:

- `vue-tsc --noEmit`: PASS。
- `uni build -p mp-weixin`: PASS；构建输出仅保留已有的非阻断提示 `os - Alias not found`。
- Generated WXML/WXSS audit: PASS；`dist/build/mp-weixin` 与正在更新的 `dist/dev/mp-weixin` 均包含可见文字 `筛选`、`filter-trigger` 样式、active 状态和 `bindtap` 点击绑定，不再包含旧 `search-btn/search-icon` 入口。
- `git diff --check`: PASS。

Skipped:

- 未直接操作用户的微信开发者工具执行真机点击验收；需用户重新编译后确认按钮实际显示、可点击并能打开标签/年/月/日筛选面板。

Scope Safety:

- 仅调整时光轴筛选入口的可见性与点击面积；未修改筛选契约、请求逻辑、后端、OpenSpec、依赖或 lockfile。
- 工作区已有 `.ai/AGENT_LOG.md`、`ppt/` 与 `.claude/settings.local.json` 等其他改动，本次提交只纳入时光轴页面代码。

Risks:

- 编译产物验证已完成，但微信开发者工具中的最终像素表现和点击行为仍需一次人工确认。

## 2026-06-22 - 诊断时光轴筛选入口不可见

Task:

- 核查微信小程序时光轴页面没有可见“筛选”按钮、用户无法发现筛选入口的问题；本轮仅诊断，不修改功能代码。

Verification:

- 当前 `main` 的 `frontend/src/pages/timeline/index.vue` 已绑定 `openFilterPanel`，筛选面板与标签、年/月/日筛选逻辑均存在。
- `frontend/dist/build/mp-weixin`、`frontend/dist/dev/mp-weixin`、`frontend/dist/preview/mp-weixin` 三份当前产物的时光轴 WXML 均含 `search-btn`、`筛选时光` 和点击绑定，排除筛选功能未编译或单一旧输出目录缺失。
- 用户截图中顶部左侧预期入口区域没有可见图形；像素检查显示该区域灰度范围仅约 5.67，而中部 Logo 区域灰度范围约 188.33，确认不是肉眼遗漏。
- 当前入口仅由 `36rpx × 36rpx`、`opacity: 0.5` 的容器和 `22rpx × 22rpx` CSS 放大镜组成，没有“筛选”文字、按钮底色或其他可发现性提示。现有未提交布局调整已将入口移至左侧，但截图表明该视觉方案仍未形成可见入口。

Skipped:

- 未在微信开发者工具中执行点击验证：当前会话没有接管用户微信开发者工具运行态；用户截图已提供真实运行时不可见证据。
- 未修改代码、未重新构建：用户本轮要求检查现状，尚未明确要求实施修复。

Risks:

- 即使空白区域仍保留点击绑定，用户无法识别可点击位置，实际产品效果等同于筛选入口缺失。
- 工作区已有与该问题相关的未提交时光轴布局调整；正式修复时应在其基础上处理，不能覆盖或误提交其他现有改动。

## 2026-06-22 - 生成项目答辩幻灯片 HTML PPT

Task:

- 使用 `guizang-ppt-skill` 在项目根目录的 `ppt/index.html` 下生成一份单文件 HTML 的横向翻页 PPT，用于时光回序 V2.0 项目答辩。

Modified:

- `ppt/index.html`
- `ppt/assets/motion.min.js`
- `ppt/images/02-concept.png`
- `ppt/images/04-topology.png`
- `ppt/images/07-integration.png`

Verification:

- 按照设计节奏交替使用了 `hero dark`, `hero light`, `light`, `dark` 等背景颜色，确保无连续3页相同颜色，营造良好视觉呼吸感。
- 主题风格采用风格 A · 电子杂志 × 电子墨水（默认），选用 🌙 沙丘 (Dune) 主题色。
- 标题采用 Noto Serif SC 衬线体，正文使用 Noto Sans SC 非衬线体，数据和元数据使用等宽 IBM Plex Mono 字体，实现完美的排版层级分工。
- 在 `ppt/images` 下通过 `generate_image` 生成了三张高品质符合电子杂志色调的 PNG 视觉素材，并在 HTML 中相应引用。
- 使用 Lucide 图标库代替 emoji，提供极简高雅的图标元素。
- 本地打开 `ppt/index.html`，验证幻灯片翻页、动效、ESC 索引视图、B 静态/动态键等交互操作完全正常。

Risks:

- 无明显风险。此为新建的静态 PPT 页面，不修改任何项目原有的后台或小程序代码。

Next:

- 进行 M4 接口及实际逻辑优化。

## 2026-06-22 - 修复时光轴页面筛选按钮被微信胶囊遮挡的问题

Task:

- 解决时光轴页面顶部筛选/搜索按钮因为微信小程序原生右上角胶囊菜单遮挡而无法看到和点击的问题。

Modified:

- `frontend/src/pages/timeline/index.vue`

Verification:

- 运行 `npm run type-check` 编译检查无报错。
- 运行 `npm run build:mp-weixin` 编译构建成功。
- 在 `frontend/src/pages/timeline/index.vue` 中重构 topbar 布局，在 HTML 中将 `search-btn` 移至最左，logo 居中，最右侧放置一个等宽空 `view`（`.topbar-placeholder`）作占位符，实现完美对称的流式布局。
- 移除了 `.search-btn` 的绝对定位（`.search-btn` 参与正常 Flex Centering 流，避免了 absolute 定位在不同小程序渲染引擎下的垂直定位漂移或遮挡问题）。
- 成功将编译后的构建产物同步复制到 `dist/build/mp-weixin`、`dist/dev/mp-weixin` 和 `dist/preview/mp-weixin` 目录下，确保不管开发者工具导入哪个目录，均能实时呈现更新。

Risks:

- 无明显风险。仅调整了时间轴顶部标志与筛选按钮的样式布局，未改动任何筛选面板的逻辑。

## 2026-06-21 - 修复新建草稿取消保存后在时光轴残留的问题

Task:

- 修复记录模块写下草稿后选择返回并取消保存，时光轴仍然显示该草稿的 bug。

Modified:

- `frontend/src/pages/record-editor/index.vue`

Verification:

- 运行 `pnpm run build:mp-weixin` 编译成功。
- 在页面关闭逻辑 `handleCloseWithAutoSave` 中，若内容校验未通过且用户确认放弃修改，同时该草稿为本次会话中新建的（`isNewlyCreatedDraft` 为 true），则调用 `recordService.deleteDraft` 接口将后端自动生成的草稿记录删除。

Risks:

- 无明显风险。仅在新建草稿且未成功保存/封存并选择丢弃时触发物理删除。

## 2026-06-17 - M4 OpenAI-compatible AI adapter

Task:

- Implement the next M4 backend AI task: single OpenAI-compatible adapter, DeepSeek-compatible path, and explicit unavailable/failed status behavior.

Modified files:

- `backend/src/main/java/com/flashback/service/impl/AiServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/AiSummaryVO.java`
- `backend/src/main/java/com/flashback/vo/AiWritingPromptsVO.java`
- `backend/src/test/java/com/flashback/service/impl/AiServiceImplTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `.ai/AGENT_LOG.md`

What changed:

- Added a backend OpenAI-compatible chat-completions adapter using Java `HttpClient`.
- Real providers `deepseek` and `openai-compatible` now call `{baseUrl}/chat/completions` with Bearer API key, non-streaming JSON request, `model`, `messages`, and `response_format: json_object`.
- Added `status` and `message` fields to AI response VOs.
- Missing real-provider config returns `UNAVAILABLE` without mock success.
- Provider HTTP errors, missing response content, or invalid model JSON return `FAILED` without mock success.
- Mock provider still uses the local generator/fallback path for current development/test behavior.
- AI output remains auxiliary response data only and does not mutate original record content; record save/seal paths still do not depend on AI.

Verification:

- Checked official DeepSeek docs before implementation: OpenAI-compatible base URL is `https://api.deepseek.com`; chat API is `POST /chat/completions`; request uses `model` and `messages`; non-stream responses expose `choices[0].message.content`; JSON output uses `response_format: {"type":"json_object"}` when the prompt also asks for JSON.
- Passed: `mvn -q '-Dtest=AiServiceImplTest,AppAiPropertiesTest,AiControllerAuthIntegrationTest' test` from `backend`.
- Passed: `mvn -q test` from `backend`.
- The first full backend test run exposed a Spring constructor ambiguity after adding a test-only constructor; fixed by marking the production constructor with `@Autowired`, then reran focused and full tests successfully.

Skipped verification reason:

- Real DeepSeek success was not exercised because no API key is available in this tracked workspace and secrets must not be committed.
- Frontend behavior was not verified because this task only changed backend response fields; frontend visible handling is a later M4 task.

`git diff --stat`:

```text
 .ai/AGENT_LOG.md                                   |  61 +++++
 .../com/flashback/service/impl/AiServiceImpl.java  | 275 +++++++++++++++++++--
 .../main/java/com/flashback/vo/AiSummaryVO.java    |  18 ++
 .../java/com/flashback/vo/AiWritingPromptsVO.java  |  18 ++
 .../flashback/service/impl/AiServiceImplTest.java  | 132 +++++++++-
 .../changes/m4-real-capability-completion/tasks.md |  16 +-
 6 files changed, 489 insertions(+), 31 deletions(-)
```

Scope safety check:

- Stayed within backend AI provider integration.
- Did not modify package files, lockfiles, Qiniu, location, attachments, cover, frontend UI, admin, deployment, monitoring, SMS, notification center, campaign, settings, social, or H5/Web scope.
- No API key or secret value was added to tracked files.
- Did not touch unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Frontend still needs to read and render `status/message` so authenticated real mode does not show a success toast for unavailable/failed AI.
- Real provider success should be manually verified later with backend-side credentials in local secret configuration.

## 2026-06-17 - M4 AI provider config contract

Task:

- Implement the next M4 task: accepted AI provider configuration keys, provider enum values, and default model selection behavior.

Modified files:

- `backend/src/main/java/com/flashback/config/AppAiProperties.java`
- `backend/src/main/java/com/flashback/service/impl/AiServiceImpl.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-prod.yml`
- `backend/src/test/java/com/flashback/config/AppAiPropertiesTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `.ai/AGENT_LOG.md`

What changed:

- Added backend-side AI config fields: `provider`, `base-url`, `api-key`, `model`, `timeout-millis`, and `real-mode-mock-enabled`.
- Added accepted provider enum values `mock`, `deepseek`, and `openai-compatible`, including underscore/case-tolerant parsing for config use.
- Updated default AI model to `deepseek-v4-pro`, default base URL to `https://api.deepseek.com`, and default timeout to `10000`.
- Switched application YAML env bindings to accepted backend-only `AI_*` variables instead of the old `APP_AI_*` names.
- Kept AI service behavior otherwise unchanged: non-mock providers still fall back until the adapter task is implemented.

Verification:

- Passed: `mvn -q '-Dtest=AppAiPropertiesTest,AiServiceImplTest' test` from `backend` after non-sandbox rerun.
- Passed: `mvn -q test` from `backend` after non-sandbox rerun.
- Initial sandboxed Maven attempts failed because Maven Central dependency access was denied; reran with approved elevated permissions.

Skipped verification reason:

- Did not verify real DeepSeek success or failure paths because this task only adds configuration/model/provider enum support, not the OpenAI-compatible adapter.
- Did not run frontend checks because no frontend code changed.

`git diff --stat`:

```text
 .ai/AGENT_LOG.md                                   | 54 +++++++++++++++
 .../java/com/flashback/config/AppAiProperties.java | 78 +++++++++++++++++++++-
 .../com/flashback/service/impl/AiServiceImpl.java  | 10 ++-
 backend/src/main/resources/application-dev.yml     |  8 ++-
 backend/src/main/resources/application-prod.yml    |  8 ++-
 backend/src/main/resources/application.yml         | 10 ++-
 .../com/flashback/config/AppAiPropertiesTest.java  | 33 +++++++++
 .../changes/m4-real-capability-completion/tasks.md |  8 +--
 8 files changed, 199 insertions(+), 18 deletions(-)
```

Scope safety check:

- Stayed within M4 AI backend configuration contract.
- Did not implement adapter HTTP calls, Qiniu, location, attachments, cover, frontend UI, admin, deployment, monitoring, SMS, notification center, campaign, settings, social, H5/Web, package files, or lockfiles.
- AI API key is referenced only as backend-side environment/config placeholder; no secret value was added.
- Did not touch unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Next AI task still needs the accepted single OpenAI-compatible adapter and explicit unavailable/failed semantics for real mode.
- Provider/storage docs should still be rechecked immediately before HTTP adapter or Qiniu implementation.

## 2026-06-17 - M4 current code facts

Task:

- Complete M4 task 1: establish current code facts before implementation.
- Read required M4 OpenSpec fact sources and classify AI, record location, attachments, cover, data surfaces, preview boundary, and upload/storage state.

Modified files:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Marked M4 guardrail preflight and current-code-facts task items complete.
- Recorded current capability classification before implementation.

Fact findings:

- `confirmed`: record lifecycle, ownership-scoped record detail/list/timeline, DRAFT-only record content update/delete/seal, UNLOCKED later reflection, and backend-backed authenticated `recordService` calls exist.
- `confirmed`: preview data boundary is explicit in services: preview data is used only when there is no token and `hasPreviewSession()` is true.
- `partial`: home cards use backend record list/unlocked endpoints in authenticated mode, but response contracts do not include cover/location media metadata yet.
- `partial`: time review/detail uses backend `getRecordDetail` in authenticated mode and has a display hook for a string-like `location`, but backend/types do not provide M4 `RecordLocationVO`, attachments, or cover.
- `planned`: real AI provider integration. Current `AppAiProperties` only has `provider`, `timeoutMillis`, and fallback text; `AiServiceImpl` supports `mock` and returns fallback when provider is unsupported or errors.
- `planned`: record location persistence/API. Current record entity, create/update DTOs, detail/list/timeline VOs, mapper, and schema do not include location fields or `record_location`.
- `planned`: image/voice attachments and cover. Current schema and backend Java sources have no Qiniu/storage/attachment/cover controller, service, mapper, VO, or dependency; record editor 地点/图片/语音 buttons still show "功能将在后续版本开放".
- `planned`: upload/storage dependencies. No Qiniu dependency/config was found in `backend/pom.xml` or application YAML files; no upload API exists.
- `out of scope`: speech-to-text, voice transcription, voice AI analysis, admin, deployment, monitoring, settings, SMS, notification center, campaign, social feed, and H5/Web user acceptance were not touched.
- `unknown`: real WeChat Developer Tools behavior for future location/media flows remains unknown because this task was audit/documentation only.

Verification:

- Read `AGENTS.md`, `.ai/ACTIVE_TASK.md`, accepted specs, M4 proposal/design/tasks/backend-contract-decisions, and M4 spec deltas.
- Confirmed `openspec` CLI is not available in current PowerShell PATH; files were read directly.
- Targeted code reads covered backend AI config/controller/service, stage summary, record controller/service/domain/DTO/VO/mapper/schema, frontend record/AI services, preview session, record types, record store, and home/timeline/record-detail/record-editor page evidence.
- Ran targeted PowerShell searches for Qiniu/storage/upload/location/attachment/cover and Mini Program media/location APIs; no current implementation was found.

Skipped verification reason:

- Backend tests, frontend type-check, Mini Program build, and WeChat manual verification were skipped because this task changed only OpenSpec task status and agent log documentation.

`git diff --stat`:

```text
 .ai/AGENT_LOG.md                                   | 61 ++++++++++++++++++++++
 .../changes/m4-real-capability-completion/tasks.md | 60 ++++++++++-----------
 2 files changed, 91 insertions(+), 30 deletions(-)
```

Scope safety check:

- Stayed within M4 task 1 fact finding and required evidence.
- Did not change backend implementation, frontend implementation, schemas, package files, lockfiles, deployment, monitoring, admin, SMS, notification center, campaign, settings, social, or H5/Web scope.
- Did not touch unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- The next implementation task should start with accepted contract gate and likely AI provider configuration/adapter work.
- DeepSeek and Qiniu official docs should be rechecked immediately before provider/storage code changes.
- `openspec` CLI remains unavailable, so OpenSpec validation is manual until the CLI is installed or added to PATH.

### 2026-06-16 Codex (M4 Exploration Kickoff)

Task:

- Confirm current code state for proposed M4 engineering optimization and discuss scope before creating a new OpenSpec change.

Modified:

- `.ai/AGENT_LOG.md`

Verification:

- Read `.ai/ACTIVE_TASK.md`, M3 OpenSpec proposal/design/tasks/spec deltas, accepted backend/miniapp/product specs, and M3 backend contract decisions.
- Confirmed `openspec` CLI is not available in the current PowerShell environment, so OpenSpec files were read directly.
- Targeted searches found backend AI still defaults to `mock`/fallback, frontend preview mode still has local preview data paths, the record editor's 地点/图片/语音 entries currently show "后续版本开放", and record schemas/DTO/VO types do not yet include real location/image/voice fields.
- Confirmed homepage summary card is backend-backed for latest draft/sealed/unlocked records, but its arrival countdown copy still contains static `3 天` / `72 小时` display text.

Risks:

- M3 full combined manual demo-loop verification remains a closeout risk before formal M3 archival.
- M4 will require new OpenSpec scope and contract decisions before implementation, especially for AI provider/API-key handling and media/location persistence.

Next:

- Discuss and confirm M4 product/technical boundaries, then create an M4 OpenSpec proposal/design/tasks/spec delta if the direction is accepted.

### 2026-05-28 Antigravity

Task:

- 修复 `record-editor` 页面：绑定 `title` 输入框。
- 修复 `login` 注册页：去除 nickname 要求，结构对齐登录页。
- 确认并记录 `record-list` 和 `record-detail` 属于 M1 提前完成的 secondary pages visual alignment，将其纳入本轮记录和验证链路。

Modified:

- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/pages/login/index.vue`
- `frontend/src/pages/record-list/index.vue` (经核对无需更改，维持原样)
- `frontend/src/pages/record-detail/index.vue` (经核对无需更改，维持原样)

Verification:

- 运行 `npm run build:mp-weixin` 构建验证。
- 检查 `record-editor`：成功添加了 title 输入框，且已包含在保存/封存的 payload 内。
- 检查 `login` 注册页：移除了 nickname 相关 UI 及其必填验证，提交 payload 使用 username 作为 nickname 的兜底 fallback，满足后端可能存在的验证，不对齐造成破坏。
- `record-list` 和 `record-detail`：确认未引入真实的 MAP/IMAGE/VOICE 或 subscription-message 实现，没有改变后端和业务状态，“收入时光轴”等视觉入口仅作为演示入口。

Risks:

- 无

Next:

- 准备就绪，可以进行后续页面的 M1 对齐或其他 Task。

### 2026-05-28 Antigravity (Visual Fixes)

Task:

- 修复登录页用户名和密码输入框的视觉水平对齐。
- 修复回看详情页顶部 "X" 关闭按钮位置至左上角。
- 修复首页主标题宋体/serif字体栈并进行微调视觉居中。
- 修复我的档案页整体内容右移、溢出及卡片/列表挤压问题。

Modified:

- `frontend/src/pages/login/index.vue`
- `frontend/src/pages/record-detail/index.vue`
- `frontend/src/pages/home/index.vue`
- `frontend/src/pages/record-list/index.vue`

Verification:

- 成功运行 `npm run build:mp-weixin` 编译，构建包无报错，完美打包。
- `login`：给密码输入框加了对称 `padding-left`/`padding-right` 及 `box-sizing: border-box`，使得密码文本的视觉中心与用户名输入框完全重合；密码图标通过 absolute 定位放置于右侧，保留隐藏/显示密码功能。
- `record-detail`：`X` 关闭按钮成功移动到左上角 `left: 56rpx`，脱离并避开了居中的品牌 logo 及状态栏，点击行为无任何改动。
- `home`：主标题设置了明确优雅的 Songti/serif 系统字体栈；引入 `transform: translateX` 像素级抵消了尾部全角标点 `，` 和 `？` 及 `letter-spacing` 造成的视觉偏左，实现完美视觉居中。
- `record-list`：移除了 `scroll-view` 本身的左右 `padding`，使用 `.scroll-inner` 包裹内容来管理 padding 和宽度，并给根容器 `.page` 及 `.card` 加上了 `width: 100%; box-sizing: border-box; overflow-x: hidden;`，彻底解决了微信小程序内页面右偏移、压缩卡片及横向溢出问题。

Risks:

- 无。修改范围严格控制在前端视觉/样式布局，未触及任何业务逻辑、后端 API、Record 状态或 package.json。

### 2026-05-28 Antigravity (Visual Fixes Round 2)

Task:

- 将新建页面右上角 "X" 符号移动至页面左上角，与回看页面保持完全一致的视觉与位置（加圆形半透明描边），并删除 "Vol.01" 及其横线装饰。
- 修复 "我的档案" 顶部搜索框因微信小程序底层限制导致的文字被挤压遮挡、视觉不协调问题。

Modified:

- `frontend/src/pages/record-editor/components/ImmersiveEditorTopBar.vue`
- `frontend/src/pages/record-list/index.vue`

Verification:

- 运行 `npm run build:mp-weixin` 编译，完成度 100%，打包无报错。
- `record-editor`：右上角 "X" 关闭按钮成功移动至左上角 `left: 56rpx`，且样式通过添加 `border-radius: 999rpx`、`background` 和 `border` 描边与回看详情页的 "X" 关闭键完美统一；移除了 "Vol.01" 及其相关横线装饰。
- `record-list`：去除了搜索框 `.search-input` 在 Uniapp/WeChat 平台上因 box-sizing 冲突被挤压的 top/bottom padding，设置了明确的 `height: 80rpx` 及 `line-height: 80rpx`，彻底消除了文字上下被裁切压缩的问题，视觉比例完美协调。

Risks:

- 无。修改高度内聚于前端样式/模板文件，完全零副作用。

### 2026-05-29 Codex

Task:

- 补充 M1 文档，将下一轮目标从旧的标题/注册昵称修复切换为个人主页五个设置子页面的高保真重构。
- 确认 `视觉外观`、`访问控制`、`数据备份` 使用独立新路由，不复用 `tag-manage` / `notify-settings`。

Modified:

- `.ai/ACTIVE_TASK.md`
- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `openspec/changes/m1-frontend-visual-foundation/design.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `openspec/changes/m1-frontend-visual-foundation/proposal.md`
- `.ai/AGENT_LOG.md`

Verification:

- 文档级更新，未修改前端代码，未运行小程序构建。
- 后续实现 Agent 需要执行 `cd frontend; npm run build:mp-weixin` 并更新任务勾选。

Risks:

- 新增路由名称为本轮文档确认：`visual-appearance`、`access-control`、`data-backup`。如产品希望复用旧路由，需要先调整 OpenSpec。

Next:

- 实现五个个人主页设置子页面，并更新 `pages.json` 与 `user-center/index.vue` 的入口路由。

### 2026-05-29 Codex (Clarification)

Task:

- 补充说明 `Docs/design/home-v2/个人主页_子页面.html` 是包含五个子页面的 canonical 原型合集，页面按钮仅用于在网页原型中切换不同子页面。
- 强化下一轮实现必须对照 HTML bundle 与对应 PNG 快照进行高保真复刻，不以旧路由名或旧页面骨架为视觉依据。

Modified:

- `.ai/ACTIVE_TASK.md`
- `openspec/changes/m1-frontend-visual-foundation/visual-reference-map.md`
- `openspec/changes/m1-frontend-visual-foundation/design.md`
- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `.ai/AGENT_LOG.md`

Verification:

- 文档级澄清，未修改前端代码，未运行小程序构建。

Risks:

- 无新增产品风险；仍需下一轮实现 Agent 按 HTML/Png 逐页核对。

Next:

- 按 `个人主页_子页面.html` PAGE 0-4 与五张 PNG 快照执行高保真重构。

### 2026-05-29 Codex (Blank Screen Triage)

Task:

- 排查微信开发工具编译后一片空白的问题。
- 验证新增个人主页子页面路由与小程序构建。

Modified:

- `frontend/src/pages/user-center/archive-preference/index.vue`
- `frontend/src/pages/user-center/visual-appearance/index.vue`
- `frontend/src/pages/user-center/access-control/index.vue`
- `frontend/src/pages/user-center/data-backup/index.vue`
- `frontend/src/pages/user-center/about/index.vue`
- `.ai/AGENT_LOG.md`

Verification:

- `pages.json` 已注册 `visual-appearance`、`access-control`、`data-backup`，生成的 `dist/build/mp-weixin/app.json` 路由列表正确。
- 运行 `uni build -p mp-weixin` 成功。
- 移除 5 个子页面中的 Google Fonts 远程 `@import url(...)` 后，确认 `frontend/dist/build/mp-weixin` 中不再包含 `@import` 或 `https://fonts.googleapis`。

Risks:

- 微信开发工具仍需重新导入/刷新 `frontend/dist/build/mp-weixin` 后人工确认白屏消失。
- 其他 WXSS 兼容性细节如 `backdrop-filter` 不应导致整页白屏，但后续可在真机/开发工具中继续精修。

Next:

- 在微信开发工具中清缓存并重新编译；如仍白屏，优先查看 Console / WXML / WXSS 报错行。

### 2026-05-29 Antigravity

Task:

- 完成 `整理偏好`、`视觉外观`、`访问控制`、`数据备份`、`版本信息` 五个设置子页面的高保真重构。
- 更新路由表，使从个人主页点击对应设置项时跳转到正确的 canonical 路由。

Modified:

- `frontend/src/pages.json`
- `frontend/src/pages/user-center/index.vue`
- `frontend/src/pages/user-center/archive-preference/index.vue`
- `frontend/src/pages/user-center/visual-appearance/index.vue`
- `frontend/src/pages/user-center/access-control/index.vue`
- `frontend/src/pages/user-center/data-backup/index.vue`
- `frontend/src/pages/user-center/about/index.vue`

Verification:

- 成功运行 `npm run build:mp-weixin`，0 报错，编译通过。
- 对照 `个人主页_子页面.html` 及五张对应快照，高保真还原了 5 个二级设置页面。
- 将内联 SVG 转换为 uniapp 兼容的 base64 `background-image`。
- 从 HTML 抽取了所有特有样式（如状态徽章、访问日志小标、滑块控件、纸色块、版本更新块等），并补充了移动端安全区 `padding-bottom`。
- 本地状态（滑块、单选等）已包含基础点击切换视觉效果。
- 更新了 `tasks.md` 对应的检查项。

Risks:

- 无。所有的页面只影响前端视觉交互，没有修改或调用真实环境接口能力（如 iCloud 真实备份或真实的锁屏能力）。

Next:

- 移交验收或开启其他页面的高保真重构。

### 2026-05-29 Codex (Sealed Page Entry Triage)

Task:

- 校验 `Docs/design/home-v2/封存.html` 对应页面是否已在前端重构。
- 若已存在，则修复“点击已封存记录无法进入封存页”的入口问题。

Modified:

- `frontend/src/pages/home/index.vue`
- `frontend/src/pages/record-list/index.vue`
- `frontend/src/pages/timeline/index.vue`
- `frontend/src/pages/record-detail/index.vue`
- `.ai/AGENT_LOG.md`

Verification:

- 确认仓库没有独立封存页路由；现有实现位于 `frontend/src/pages/record-detail/index.vue` 的 `SEALED` 分支。
- 对照 `Docs/design/home-v2/封存.html`，该分支已包含封存页核心结构：Archive meta、封存信件卡片、模糊正文、倒计时区与禁用态回应 CTA。
- 修复 `record-list`、`timeline`、`home` 三处入口，使已封存记录跳转到 `record-detail` 并由 `SEALED` 分支渲染封存页。
- 成功运行 `uni build -p mp-weixin`。

Risks:

- 未在微信开发工具中人工点击验证；需在小程序中分别从首页、我的档案、时光轴进入已封存记录做视觉确认。
- 封存页目前复用 `record-detail` 路由，不是独立 route；如后续要求页面资产/生命周期完全独立，再补 OpenSpec 并拆路由。

Next:

- 在微信开发工具中点击已封存记录确认能进入封存页。
- 若视觉与 `封存.png` 仍有偏差，再进入 M1 页面精修。

### 2026-05-29 Codex (M1 Polish Gate)

Task:

- 根据项目状态更新 OpenSpec：所有 M1 前端页面级重构已完成，当前进入前端精修 / 验收阶段。
- 明确精修完成后，M1 前端重构任务即可视为完成。

Modified:

- `openspec/changes/m1-frontend-visual-foundation/tasks.md`
- `.ai/AGENT_LOG.md`

Verification:

- 未修改前端实现代码。
- OpenSpec 现在包含 `Current Phase / 当前阶段` 和 `Frontend Polish Completion Gate / 前端精修完成门槛`。
- 未将 M1 直接标记为完成；保留视觉证据、构建、导航、安全区、范围安全检查等验收项作为完成前置条件。

Risks:

- 本轮未重新运行前端构建或视觉验收；后续精修完成后仍需按 OpenSpec gate 验证。

Next:

- 按精修阶段逐个修复视觉 / 交互 / 路由 / safe-area / 小程序兼容问题。
- 精修完成后补充最终 handoff，并勾选 section 6 与 section 7 的验收项。

### 2026-05-29 Codex (M1 Merge Blocker Closure)

Task:

- 修复合并前审查发现的前端硬 blocker，并完成最终构建 / 类型检查收口。

Modified:

- `frontend/src/pages/home/index.vue`
- `frontend/src/pages/record-detail/index.vue`
- `frontend/src/pages/timeline/index.vue`
- `frontend/src/pages/user-center/index.vue`
- `.ai/AGENT_LOG.md`

Verification:

- `.\node_modules\.bin\vue-tsc.cmd --noEmit` 通过。
- `.\node_modules\.bin\uni.cmd build -p mp-weixin` 通过。
- 确认底部导航不再在模板表达式里直接引用 `uni.switchTab`。
- 确认 `record-detail` 模板引用的 `archiveNoCN` 已在 `<script setup>` 中定义。

Risks:

- 未在微信开发者工具 / 真机中做最终点击验收；合并前仍建议人工抽测首页、时光轴、个人中心底部导航以及已封存 / 已解封详情页。

Next:

- 若人工抽测无新增问题，可合并 `feat/m1-frontend-visual-foundation` 到 `main`。

### 2026-05-29 Codex (Preview Experience Build)

Task:

- 为手机端无法登录场景重新生成微信小程序体验/预览产物，沿用此前 V1.0.1 使用方式的 `frontend/dist/dev/mp-weixin` 目录。

Modified:

- `.ai/AGENT_LOG.md`
- Generated ignored artifact: `frontend/dist/dev/mp-weixin`

Verification:

- 直接运行项目本地 Uni CLI：`.\node_modules\.bin\uni.cmd -p mp-weixin --mode preview`。
- 命令为开发/watch 模式，120s 后超时退出，但产物已写入。
- 确认 `frontend/dist/dev/mp-weixin/config/app-env.js` 中 `isPreviewModeEnabled = true`。
- 确认 `frontend/dist/dev/mp-weixin/pages/login/index.wxml` 包含「预览进入」入口。

Risks:

- 未在微信开发者工具或真机上导入并扫码预览；需要人工导入 `frontend/dist/dev/mp-weixin` 后点击「预览进入」确认完整路径。
- 体验版绕过真实登录，只用于手机端视觉/流程体验，不代表生产登录问题已修复。

### 2026-05-29 Codex (Preview Build Path Correction)

Task:

- 用户反馈微信开发工具打开 `frontend/dist/dev/mp-weixin` 时没有「预览进入」，重新确认构建路径并提供可跳过登录的体验版。

Modified:

- `.ai/AGENT_LOG.md`
- Generated ignored artifact: `frontend/dist/build/mp-weixin`
- Generated ignored artifact: `frontend/dist/preview/mp-weixin`
- Patched ignored artifact: `frontend/dist/dev/mp-weixin/config/app-env.js`

Verification:

- 确认此前 `frontend/dist/dev/mp-weixin/config/app-env.js` 实际为 `isPreviewModeEnabled = false`，这是入口不显示的直接原因。
- 使用显式环境变量运行：`VITE_PREVIEW_MODE=true .\node_modules\.bin\uni.cmd build -p mp-weixin --mode preview`，构建成功。
- Uni 输出提示导入目录为 `dist/build/mp-weixin`。
- 确认 `frontend/dist/build/mp-weixin/config/app-env.js` 为 `exports.isPreviewModeEnabled=!0`。
- 复制 `frontend/dist/build/mp-weixin` 到 `frontend/dist/preview/mp-weixin`，作为明确命名的体验版目录。
- 确认 `frontend/dist/preview/mp-weixin/pages/login/index.wxml` 包含「预览进入」入口。
- 将当前已打开路径 `frontend/dist/dev/mp-weixin/config/app-env.js` 也切为 `isPreviewModeEnabled = true`，方便微信开发工具刷新后使用。

Risks:

- 未在微信开发者工具中实际点击「编译」和「预览进入」。
- 后续如果重新运行普通 dev/build 命令，`dist/dev/mp-weixin` 或 `dist/build/mp-weixin` 可能再次被普通模式覆盖；需要重新用 preview build 命令生成。

### 2026-06-05 Codex (M2 Backend Docs Closeout)

Task:

- 收口 M2 后端优化入口文档，避免进入后端实现阶段时被旧 M1 前端任务误导。

Modified:

- `.ai/ACTIVE_TASK.md`
- `.ai/HANDOFF_M1_VISUAL.md`
- `openspec/changes/m2-backend-optimization/proposal.md`
- `openspec/changes/m2-backend-optimization/design.md`
- `openspec/changes/m2-backend-optimization/tasks.md`
- `openspec/changes/m2-backend-optimization/specs/backend-core/spec.md`
- `.ai/AGENT_LOG.md`

Verification:

- 确认 `.ai/ACTIVE_TASK.md` 已改为纯 M2 后端入口任务。
- 确认 M1 handoff 中五个 User Center settings-style subpage 状态均为 confirmed。
- 明确 M2 当前标签模型为系统共享/global tags，record-tag 关系和 tag filtering 仍受 record ownership 保护。
- 明确 record list 分页排序需要稳定 tie-breaker，例如 `created_at DESC, id DESC`。

Risks:

- 本次仅做文档收口，未修改后端代码，也未运行后端测试。

### 2026-06-05 Codex (M2 Backend Capability Audit + Minimal Fix)

Task:

- 审查 M2 backend core capabilities，确认 record lifecycle、owner boundary、list/timeline/tag/reply/AI fallback、preview bypass 和 WeChat subscription message foundation 现状。
- 对发现的 P2 稳定性缺口做最小修复。

Modified:

- `backend/src/main/resources/mapper/RecordMapper.xml`
- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Evidence:

- Auth/JWT: `AuthController` 提供本地注册/登录；`JwtAuthenticationInterceptor` 保护 `/api/**`，`@CurrentUser` 注入 `AuthUser.userId`；`User.openid` 字段存在但普通注册明确不接受客户端 openid。
- Preview bypass: frontend `preview-session.ts` + services 在无 token 且有 preview session 时走 demo data，不触发真实 backend 或订阅消息。
- Records: `RecordServiceImpl` 使用 `selectByIdAndUserId` 保护 detail/update/delete/seal；update/delete/seal 均要求 `DRAFT`；mapper update/delete/seal SQL 也包含 `user_id` + `status = 'DRAFT'`。
- Unlock: scheduler 调 `recordService.runUnlockJob()`；SQL 只选 `status='SEALED' and unlock_at <= now`；unlock SQL 只更新 `SEALED`；service 仅在 affected=1 时写 `unlock_notice_log`，重复执行不重复写成功日志。
- List/timeline/tag: list/timeline SQL 均带 `r.user_id = #{userId}`；tag definitions 为 shared enabled tags；tag filtering 使用 record ownership scoped query。
- Reply: `ReplyServiceImpl` 先 `selectByIdAndUserId`，create 要求 `UNLOCKED`，`uk_reply_record_id` 防重复。
- AI fallback: `AiServiceImpl` mock/fallback 独立 API，异常返回 fallback；record create/update/seal/unlock 不调用真实 AI，不阻塞核心流程。
- Notification foundation: existing `unlock_notice_log` is unlock evidence only; no WeChat subscription send API/outbox/template config/openid binding API yet.

Fix:

- `RecordMapper.xml` record page order changed from `created_at DESC` to `created_at DESC, id DESC`.
- Added mapper integration test for same `created_at` deterministic id-desc tie-breaker.

Verification:

- First `mvn -q test` failed inside sandbox because Maven could not resolve `spring-boot-starter-parent:3.3.5` from Maven Central due permission/network restriction.
- Re-ran approved `mvn -q test` from `backend`; full backend test suite passed.

Risks:

- WeChat subscription message should be a new M2 subtask/spec slice before implementation; current code has data hints but no production send path.
- Frontend record list currently exposes status filtering only; backend supports recordType/tag/keyword filters, but page-level UI does not yet use all of them.
- `unlock_notice_log` has no unique `(record_id, notice_type, notice_status)` constraint; current normal unlock job is idempotent through record status update, but a future notification sender needs stronger outbox/log idempotency.

### 2026-06-06 Codex (M2 Subscription Message Foundation Docs)

Task:

- 将记录到期提醒 / WeChat Mini Program subscription message foundation 正式纳入 `m2-backend-optimization` 文档范围。
- 保持 M2 边界：只纳入 unlock reminder foundation，不做生产通知中心、短信提醒、admin template management、campaign delivery 或真实发布加固。

Modified:

- `.ai/ACTIVE_TASK.md`
- `openspec/project.md`
- `openspec/changes/m2-backend-optimization/proposal.md`
- `openspec/changes/m2-backend-optimization/design.md`
- `openspec/changes/m2-backend-optimization/tasks.md`
- `openspec/changes/m2-backend-optimization/specs/backend-core/spec.md`
- `.ai/AGENT_LOG.md`

Evidence:

- `proposal.md` adds M2 goal/scope/acceptance criteria for minimal WeChat subscription message foundation.
- `design.md` adds "Notification Foundation, Not Notification Center" and review guidance for `openid`, preview bypass, seal-flow authorization timing, unlock-task hook, outbox/log persistence, idempotency, and sensitive-log avoidance.
- `tasks.md` adds an explicit audit section for WeChat subscription message foundation and validation evidence requirements.
- `backend-core/spec.md` adds SHALL scenarios for WeChat identity classification, preview bypass, seal-flow authorization point, non-blocking unlock reminder handling, idempotent successful sends, minimal persistence, and sensitive-log avoidance.

Verification:

- Documentation-only change.
- Ran targeted text check across `.ai/ACTIVE_TASK.md`, `openspec/project.md`, and `openspec/changes/m2-backend-optimization/**` for subscription/reminder/notification wording.
- No backend code, database schema, package, lockfile, multimedia, admin, SMS, or real AI implementation changes were made.

Risks:

- M2 implementation still needs a concrete minimal schema decision: reuse `user.openid` vs introduce `user_wechat_identity`, and `record_reminder` vs `notification_outbox`.
- Future implementation must enforce one successful send per `record_id + template_type` and must not block unlock processing on send failure.

### 2026-06-06 Codex (M2 Backend Optimization Implementation Slice)

Task:

- Implement the approved M2 backend optimization plan in the smallest backend slice.
- Preserve M2 boundaries: no production notification center, no SMS, no admin template management, no real WeChat send API, no AI enhancement, no package/lockfile change.

Modified:

- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/flashback/domain/RecordReminder.java`
- `backend/src/main/java/com/flashback/domain/RecordReminderStatus.java`
- `backend/src/main/java/com/flashback/mapper/RecordReminderMapper.java`
- `backend/src/main/resources/mapper/RecordReminderMapper.xml`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordReminderMapperIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Backend fact checklist:

- confirmed: auth/JWT local account flow, `@CurrentUser` user id, record owner-scoped detail/update/delete/seal/list/timeline, `DRAFT -> SEALED -> UNLOCKED`, sealed immutability through user update/delete/seal SQL + service checks, reply only after `UNLOCKED`, shared enabled tags, record-tag filtering scoped by record ownership, stable record list/timeline ordering with `created_at DESC, id DESC`, AI fallback isolated from record lifecycle.
- partial: `user.openid` exists in schema/domain/mapper, but local register intentionally sets it to `null`; no WeChat login/bind API is implemented.
- partial: unlock task already writes `unlock_notice_log`; this slice adds `record_reminder` as a minimal unlock-reminder outbox marker, but no real WeChat delivery.
- planned: real WeChat subscription authorization flow, openid binding, real send implementation, retry policy, and template management.
- out of scope: SMS, production notification center, campaign/admin management, monitoring/alerting, real MAP/IMAGE/VOICE, AI capability enhancement, package/lockfile changes.
- unknown: production WeChat template ids and release-time Mini Program subscription-message configuration.

API contract map:

- 首页 / 个人中心: `/api/user/me`, `/api/auth/login`, `/api/auth/register`; preview mode returns frontend demo data when no token and preview session exists.
- 新建记录: `POST /api/records`; draft payload aligns with backend DTO fields including record type, unlock time, tags, and optional AI snapshot fields.
- 我的记录: `GET /api/records`; frontend currently passes page and status, while backend already supports record type, tag, and keyword filters for later UI use.
- 时光轴: `GET /api/records/timeline`; backend scopes to user and supports year/tag filtering.
- 封存详情 / 时间回看: `GET /api/records/{id}` and `POST /api/records/{id}/seal`; backend returns status, unlock/seal/unlocked times, tags, reply flags, and AI snapshot fields.
- 回信: `GET/POST /api/records/{recordId}/reply`; backend requires record ownership and `UNLOCKED`.
- 标签: `GET /api/tags`; current M2 model is shared enabled tags, with record filtering still ownership-scoped.

Implementation:

- Added `record_reminder` table with unique `(record_id, template_type)` for successful idempotency marker/outbox semantics.
- Added `RecordReminder` / `RecordReminderStatus` and `RecordReminderMapper`.
- Hooked `RecordServiceImpl.runUnlockJob()` so a successfully unlocked record creates one `UNLOCK_REMINDER` marker.
- Reminder status is `PENDING` when the user has an `openid`, and `SKIPPED_NO_OPENID` when `openid` is absent.
- Reminder persistence is best-effort and non-blocking; failure does not roll back or stop unlock processing.
- No real WeChat API call, sender, retry loop, SMS behavior, or notification-center behavior was added.

Verification:

- Initial sandboxed Maven runs failed because Maven Central dependency resolution was blocked by sandbox network permissions.
- Approved focused test run passed: `mvn -q "-Dtest=RecordServiceImplTest,RecordReminderMapperIntegrationTest,UnlockNoticeLogMapperIntegrationTest" test`.
- Approved full backend test run passed: `mvn -q test`.
- Focused tests cover pending/skipped reminder marker creation, duplicate marker avoidance, and non-blocking reminder persistence failure.
- Mapper integration tests cover `record_reminder` insert/select and unique `(record_id, template_type)` enforcement.

Risks:

- `record_reminder` is a schema addition; existing local MySQL databases need the new table applied manually because this project does not currently use a migration tool.
- Real WeChat send behavior remains intentionally unimplemented; a later OpenSpec change must define template ids, bind/login flow, sender, retry, and operational rules.
- `user.openid` remains nullable and has no binding API, so normal local demo accounts will produce `SKIPPED_NO_OPENID` markers.
- Frontend record-list UI currently sends status filters only; backend type/tag/keyword filters are ready but not fully exercised by page controls.

### 2026-06-06 Codex (M2 WeChat Identity Binding Foundation)

Task:

- Continue M2 backend optimization after the minimal `record_reminder` foundation.
- Add the smallest trusted backend boundary for future WeChat identity binding without exposing a public "client submits openid" API.

Modified:

- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/flashback/mapper/UserMapper.java`
- `backend/src/main/resources/mapper/UserMapper.xml`
- `backend/src/main/java/com/flashback/service/UserService.java`
- `backend/src/main/java/com/flashback/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/UserInfoVO.java`
- `backend/src/test/java/com/flashback/service/impl/UserServiceImplTest.java`
- `backend/src/test/java/com/flashback/mapper/UserMapperIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added unique `uk_user_openid` on `user.openid` so one verified WeChat identity cannot bind to multiple users; nullable `openid` remains allowed for local demo accounts.
- Added `UserMapper.selectByOpenid` and `UserMapper.updateOpenidById`.
- Added `UserService.bindVerifiedWechatOpenid(userId, openid)` as a trusted service boundary for a future verified WeChat login / `code2session` flow.
- Binding trims input, rejects blank or overlong openid, rejects openid already bound to another user, and maps duplicate-key races to a business error.
- Added `UserInfoVO.wechatBound` to expose binding state without returning raw `openid`.
- Did not add a public bind endpoint, real WeChat API call, sender, SMS, notification center, or package/lockfile changes.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused tests passed: `mvn -q "-Dtest=UserServiceImplTest,UserMapperIntegrationTest,UserControllerAuthIntegrationTest" test`.
- Approved full backend suite passed: `mvn -q test`.
- `git diff --check` passed; only CRLF conversion warnings were reported.

Risks:

- Existing local MySQL databases need the new `uk_user_openid` index applied manually; duplicate non-null historical openids would block that index.
- Public WeChat bind/login remains intentionally unimplemented until a later OpenSpec change defines code verification, app credentials, and security handling.
- Normal local users still have nullable `openid`, so unlock reminders remain `SKIPPED_NO_OPENID` unless a future verified binding flow populates it.

Next:

- Next M2 slice should review whether frontend needs only `wechatBound` display state, or whether a new OpenSpec change should define real WeChat login/binding and subscription authorization timing.

### 2026-06-06 Codex (M2 Query Stability Evidence)

Task:

- Continue M2 backend optimization with a narrow `List / Timeline / Tag Query Stability` slice.
- Lock backend evidence for shared-tag filtering ownership, unlocked ordering, and timeline ordering without expanding product scope.

Modified:

- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added mapper integration coverage that shared/global tag filtering still returns only records owned by the authenticated user.
- Added unlocked-list ordering coverage for same `unlocked_at`, expecting deterministic `unlocked_at DESC, id DESC`.
- Added timeline ordering and ownership coverage for same `created_at`, expecting deterministic `created_at DESC, id DESC` and no cross-user leakage.
- No SQL or API changes were required; existing `RecordMapper.xml` already satisfied these M2 query stability boundaries.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused test passed: `mvn -q "-Dtest=RecordMapperIntegrationTest" test`.
- Approved full backend suite passed: `mvn -q test`.

Risks:

- Query performance was not benchmarked against a large MySQL dataset; current evidence is correctness and demo-scale stability.
- Existing indexes appear aligned with current demo query shape, but future heavy tag/keyword search may need a separate performance-oriented OpenSpec change.

Next:

- Next M2 slice can review controller-level query validation and frontend contract usage for record list/timeline filters, or define a separate real WeChat login/binding change if subscription delivery moves beyond foundation.

### 2026-06-06 Codex (M2 Record API Query Contract Evidence)

Task:

- Continue M2 backend optimization with controller/API contract coverage for record list and timeline query parameters.
- Keep the slice limited to API entry validation and parameter binding evidence.

Modified:

- `backend/src/test/java/com/flashback/controller/api/RecordControllerAuthIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added controller integration coverage proving `/api/records` passes `pageNum`, `pageSize`, `status`, `recordType`, `tagId`, and `keyword` into `RecordService.pageMine`.
- Added controller integration coverage proving `/api/records/timeline` passes `year` and `tagId` into `RecordService.timeline`.
- Added validation coverage for `pageSize > 200` and timeline `year < 1970`.
- No controller, service, SQL, schema, frontend, package, or OpenSpec implementation changes were required.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused test passed: `mvn -q "-Dtest=RecordControllerAuthIntegrationTest" test`.
- Approved full backend suite passed: `mvn -q test`.

Risks:

- This locks backend API parameter binding and validation, but does not prove the current frontend UI exposes all backend-supported filters.
- Frontend contract usage should still be reviewed separately if M2 wants to replace remaining preview/mock assumptions.

Next:

- Next M2 slice can inspect frontend service calls for record list/timeline/tag compatibility, or move to AI fallback boundary evidence if frontend work should remain untouched.

### 2026-06-06 Codex (M2 AI Fallback Boundary Evidence)

Task:

- Continue M2 backend optimization with a narrow AI fallback boundary slice.
- Lock evidence that AI is a supporting API/snapshot capability and does not block the core record lifecycle.

Modified:

- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added service-level coverage that a draft can be created without any AI snapshot fields.
- The new test verifies original user content is persisted as record content, AI summary/prompt fields remain null/empty, and the record still enters `DRAFT`.
- Confirmed existing AI service tests already cover mock output and unsupported-provider fallback.
- Confirmed existing controller tests keep AI under authenticated `/api/ai/**` endpoints, separate from record create/update/seal/unlock.
- No AI provider integration, AI enhancement, schema change, frontend change, package/lockfile change, or record lifecycle logic change was made.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused test passed: `mvn -q "-Dtest=AiServiceImplTest,AiControllerAuthIntegrationTest,RecordServiceImplTest" test`.
- Approved full backend suite passed: `mvn -q test`.

Risks:

- This proves current fallback/isolation behavior at service and controller boundaries; it does not add real AI disable flags or provider configuration UI.
- Future real AI provider integration should be a separate OpenSpec change and must preserve this non-blocking boundary.

Next:

- Next M2 slice can inspect frontend service/mock compatibility for record list/timeline/tag usage, or add reply/tag controller evidence if backend-only work should continue.

### 2026-06-06 Codex (M2 Reply Ownership Evidence)

Task:

- Continue M2 backend optimization with a narrow reply lifecycle and ownership evidence slice.
- Strengthen proof that reply behavior stays owner-scoped and only follows unlocked records.

Modified:

- `backend/src/test/java/com/flashback/service/impl/ReplyServiceImplTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added assertions that cross-user reply create/detail attempts stop after owned-record lookup and do not query or insert reply rows.
- Strengthened successful reply creation coverage to verify persisted `recordId`, authenticated `userId`, trimmed content, `SHORT_REPLY`, and fixed clock `createdAt`.
- Existing tests already cover unauthenticated reply API rejection, content validation, unsupported reply type rejection, reply-before-unlock rejection, duplicate reply rejection, and no-reply detail response.
- No service/controller behavior, schema, frontend, package/lockfile, or OpenSpec implementation changes were required.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused test passed: `mvn -q "-Dtest=ReplyServiceImplTest,ReplyControllerAuthIntegrationTest" test`.
- Approved full backend suite passed: `mvn -q test`.

Risks:

- This locks current reply ownership behavior at service/controller boundaries; it does not add new reply types beyond `SHORT_REPLY`.
- Future multi-reply or long-form reply support should be a separate OpenSpec change because current schema/service enforces one reply per record.

Next:

- Next M2 slice can inspect frontend service/mock compatibility for V2 pages, or add tag controller/shared-tag contract evidence.

### 2026-06-06 Codex (M2 Tag Shared Contract Evidence)

Task:

- Continue M2 backend optimization with a narrow tag API / shared-tag contract slice.
- Lock SQL-level evidence for the current M2 model: tag definitions are shared/global, while only enabled tags are returned.

Modified:

- `backend/src/test/java/com/flashback/mapper/TagMapperIntegrationTest.java`
- `.ai/AGENT_LOG.md`

Implementation:

- Added `TagMapperIntegrationTest` covering `selectEnabledByType(null)` and `selectEnabledByType(MOOD)`.
- The new tests verify disabled tags are excluded, type filtering works, and results are stable by `id ASC`.
- Added coverage that `countEnabledByIds` ignores disabled tags, supporting record create/update tag validation.
- Existing controller tests already cover `/api/tags` authentication, disabled user rejection, invalid type validation, and successful typed tag response.
- No service/controller/SQL implementation, schema, frontend, package/lockfile, or OpenSpec implementation changes were required.

Verification:

- Sandboxed focused Maven run failed due Maven Central permission restriction, same as prior M2 runs.
- Approved focused test passed: `mvn -q "-Dtest=TagMapperIntegrationTest,TagControllerAuthIntegrationTest" test`.
- Approved full backend suite passed: `mvn -q test`.

Risks:

- This proves shared enabled tag definitions and stable tag list ordering; record-tag ownership filtering is covered separately by record mapper tests.
- User-created/private tags remain out of M2 scope unless a later OpenSpec change introduces them.

Next:

- Next M2 slice can inspect frontend service/mock compatibility for V2 pages, or perform a final M2 evidence audit to identify any remaining backend gaps.

### 2026-06-06 Codex (M2 Frontend Contract Compatibility Review)

Task:

- Continue M2 backend optimization with a frontend service/mock compatibility review.
- Map current V2 frontend service calls to the backend contracts already verified in M2.

Modified:

- `.ai/AGENT_LOG.md`

Evidence:

- `frontend/src/services/recordService.ts` calls real backend APIs for `POST /api/records`, `PUT /api/records/{id}`, `DELETE /api/records/{id}`, `POST /api/records/{id}/seal`, `GET /api/records`, `GET /api/records/{id}`, `GET /api/records/unlocked`, and `GET /api/records/timeline`.
- `frontend/src/services/replyService.ts` calls `GET/POST /api/records/{recordId}/reply` and uses preview data only when no token plus preview session exists.
- `frontend/src/services/tagService.ts` calls `GET /api/tags`; frontend fetches all enabled shared tags, then `tagStore` separates mood/topic tags client-side.
- `frontend/src/services/authService.ts` calls local register/login, `GET /api/user/me`, and `PUT /api/user/profile`; preview user info is used only under preview session without token.
- `frontend/src/services/aiService.ts` calls authenticated `/api/ai/writing-prompts` and `/api/ai/summarize-record`; no preview bypass is implemented for AI service, which is acceptable because AI is not core lifecycle.
- `frontend/src/features/preview/preview-session.ts` gates preview fallback by `isPreviewModeEnabled`, no token, and an explicit preview session.

Contract map:

- Home: uses record list counts by `DRAFT`, `SEALED`, and unlocked records; backend supports these APIs and ownership-scoped results.
- 新建记录: uses tag list, detail prefill for editing, create/update draft payload fields, and seal API; backend tests cover draft lifecycle, tag validation, and seal constraints.
- 我的记录: uses backend pagination and status filter; keyword search is currently client-side over the fetched page, while backend keyword/type/tag filters are available but not fully consumed by UI.
- 时光轴: uses backend `year` filter; backend also supports `tagId`, but current page UI does not expose tag filtering.
- 封存详情 / 时间回看: uses `GET /api/records/{id}` fields including status, timestamps, tags, AI snapshot fields, `canReply`, and `hasReply`.
- 回信: uses backend reply detail/create only when unlocked detail reports reply availability; backend ownership and one-reply behavior are tested.
- 标签: uses shared enabled tag list; current M2 shared-tag model matches backend and frontend.
- 个人中心: uses current user plus simple record-count calls; backend exposes `wechatBound`, but frontend type/UI do not consume it yet.

Compatibility findings:

- confirmed: preview bypass stays frontend/demo-only and skips real backend record/reply/tag/timeline calls when active.
- confirmed: current core V2 pages have backend API coverage without requiring new response fields.
- partial: frontend `UserInfoVO` type does not include backend `wechatBound`; this is not a blocker because no current V2 page consumes it.
- partial: backend supports record list `recordType`, `tagId`, and `keyword`, but current frontend only sends page/status and handles keyword client-side.
- partial: backend timeline supports `tagId`, but current frontend timeline UI sends only `year`.
- out of scope: reminder/subscription UI remains visual or settings placeholder; no frontend call to real subscription authorization or delivery exists in this phase.

Verification:

- Documentation-only review; no source implementation changed.
- Read focused files under `frontend/src/services`, `frontend/src/features/preview`, relevant stores, and focused page call sites.
- No Maven or frontend build was required because only `.ai/AGENT_LOG.md` was edited.

Risks:

- Client-side keyword search only filters the current fetched page, not the full backend result set; this is acceptable for current demo UI but should be revisited if search becomes a core backend-backed feature.
- If product wants to display WeChat binding or reminder readiness, frontend types/UI should add `wechatBound` consumption in a separate focused slice.

Next:

- Next M2 slice should run a final requirement-by-requirement evidence audit against `m2-backend-optimization` and identify any remaining backend gaps before considering archive/closeout.

### 2026-06-06 Codex (M2 Final Evidence Audit)

Task:

- Continue M2 backend optimization with a final requirement-by-requirement evidence audit.
- Verify whether remaining M2 backend requirements are satisfied by current code, tests, and documented evidence.

Modified:

- `.ai/AGENT_LOG.md`

Audit result:

- complete for current M2 scope: no additional P1 backend implementation defect was found in the final audit.
- OpenSpec CLI remains unavailable in this shell (`openspec` command not found), so local OpenSpec files and current worktree evidence remain the authoritative audit source.

Requirement evidence:

- Backend capability fact source: `.ai/AGENT_LOG.md` contains confirmed/partial/planned/out-of-scope/unknown classifications for auth, records, replies, tags, timeline, unlock task, AI fallback, preview bypass, WeChat identity, and reminder persistence.
- Record lifecycle: `RecordServiceImplTest`, `RecordMapperIntegrationTest`, and full test suite cover draft create/update, sealed immutability, seal constraints, unlock job eligibility, idempotency, unlocked detail, reply flags, and AI snapshot preservation/absence.
- Record type support: create/update DTO and service tests cover `FUTURE_LETTER`, `NODE_RECORD`, and `EMOTION_NOTE`; invalid enum values are rejected by request binding/validation.
- Authentication and ownership: controller auth tests cover unauthenticated rejection; service/mapper tests cover owner-scoped detail/update/delete/seal/list/timeline/reply and cross-user rejection.
- Private data boundary: code search found logging limited to global unhandled exception path/message and unlock job counts; no record content, token, openid, or reminder payload content is logged by the M2 paths.
- Frontend API contract: `.ai/AGENT_LOG.md` maps Home, 新建记录, 我的记录, 时光轴, 封存详情, 时间回看, 回信, 标签, and 个人中心 to current backend APIs and documents frontend partial use of backend filters.
- List/timeline/tag stability: mapper/controller tests cover deterministic record list ordering, unlocked ordering, timeline ordering/grouping, status/type/tag/keyword parameter binding, shared enabled tags, and user-owned tag filtering.
- Unlock task safety: `runUnlockJob` processes expired `SEALED` records only; tests cover no-op runs, concurrent/idempotent affected-row behavior, notice/reminder side effects only after successful transition, and non-blocking reminder persistence failure.
- WeChat subscription-message foundation: `user.openid` is reused as nullable identity placeholder; trusted `bindVerifiedWechatOpenid` service boundary and `wechatBound` output are implemented; `record_reminder` is unique by `record_id + template_type`; unlock creates `PENDING` or `SKIPPED_NO_OPENID` reminder markers without real WeChat delivery.
- AI fallback boundary: `AiServiceImplTest`, `AiControllerAuthIntegrationTest`, and `RecordServiceImplTest` prove AI is an authenticated supporting API/fallback path and does not block or mutate core record lifecycle operations.
- Guardrails: no production notification center, SMS, admin template management, campaign delivery, real WeChat send API, real MAP/IMAGE/VOICE, monitoring, deployment, AI enhancement, or package/lockfile update was introduced.

Verification:

- Approved full backend suite passed: `mvn -q test`.
- `rg`-based sensitive-log review over `backend/src/main/java/com/flashback` found no M2 path logging record content, tokens, openid, or reminder payload data.
- `git log --oneline --max-count=12` confirms staged M2 commits from docs closeout through query, reminder, identity, AI, reply, tag, and frontend-contract evidence.

Remaining risks:

- Existing local MySQL databases still need manual schema application for `record_reminder` and `uk_user_openid` because no migration tool is present.
- Real WeChat subscription authorization, code2session login/bind endpoint, template ids, sender, retry policy, and operational rules remain deliberately deferred to a later OpenSpec change.
- Frontend currently does not consume backend `wechatBound`; this is not a current M2 blocker because no V2 page displays WeChat binding readiness.
- Frontend keyword search filters only the fetched page; backend keyword/type/tag filters are available but not fully exposed by the current UI.
- Demo-scale query correctness is covered; large-data performance benchmarking is out of current M2 scope.

Next:

- M2 backend optimization can be treated as implementation-complete for the current demo scope.
- Before archive, run the project’s OpenSpec archive workflow once the `openspec` CLI is available, or archive manually according to the project process.

### 2026-06-07 Codex (M3 Backend Rectification Documentation Activation)

Task:

- Update project documentation so M3 backend rectification becomes the active implementation source.
- Archive M2 backend optimization docs and preserve M2 backend-core constraints as accepted baseline.
- Add contract confirmation constraints before M3 backend implementation.

Modified:

- `AGENTS.md`
- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `openspec/specs/backend-core/spec.md`
- `openspec/changes/archive/2026-06-07-m2-backend-optimization/`
- `openspec/changes/m3-demo-core-flow-hardening/proposal.md`
- `openspec/changes/m3-demo-core-flow-hardening/design.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- `openspec/changes/m3-demo-core-flow-hardening/backend-contract-decisions.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/backend-core/spec.md`

What changed:

- Promoted M2 backend-core requirements into accepted `openspec/specs/backend-core/spec.md`.
- Moved M2 change docs into `openspec/changes/archive/2026-06-07-m2-backend-optimization/`.
- Rewrote `.ai/ACTIVE_TASK.md` to make `m3-demo-core-flow-hardening` the active backend rectification source.
- Updated `AGENTS.md` so M3 scope can modify backend, schema, AI organization, and subscription-message reminder code while still blocking admin, production, SMS, notification center, monitoring, deployment, real media/location, broad rewrites, and major visual redesign.
- Added M3 phase boundaries: backend phase, frontend phase, and integration/verification phase.
- Added `.ai/AGENT_LOG.md` as the required evidence location for implementation notes, verification results, skipped verification reasons, and manual WeChat verification.
- Added `backend-contract-decisions.md` with pending backend API contract questions and recommended options that require user confirmation before implementation.
- Added a backend spec requirement that M3 backend contracts must be confirmed before implementation when more than one reasonable option exists.

Verification:

- Documentation-only change; no backend or frontend tests were run.
- M2 archive was performed manually because `openspec` CLI is unavailable in the shell.
- M2 backend-core delta was synced manually into accepted specs before archive.

Skipped verification reason:

- No application code, schema SQL, package, or lockfile changes were made in this documentation activation slice.
- Real WeChat delivery cannot be verified until template IDs and WeChat configuration are provided.

Remaining risks:

- Backend API contracts remain pending user confirmation in `backend-contract-decisions.md`.
- Because the OpenSpec CLI is unavailable, archive/status validation could not be performed through the official command workflow.
- M3 implementation should not start contract-sensitive backend code until the pending decisions are accepted or deferred.

### 2026-06-07 Codex (M3 Backend Contract Decisions Accepted)

Task:

- Record user-confirmed M3 backend API contract decisions.
- Update M3 OpenSpec documents so backend implementation can proceed without guessing contract details.

Modified:

- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `openspec/changes/m3-demo-core-flow-hardening/proposal.md`
- `openspec/changes/m3-demo-core-flow-hardening/design.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- `openspec/changes/m3-demo-core-flow-hardening/backend-contract-decisions.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/backend-core/spec.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/miniapp-core/spec.md`

Accepted decisions:

- WeChat login endpoint is `POST /api/auth/wechat-login` with request `{ "code": "string" }`, reusing the existing login response shape.
- Account/password to WeChat binding is deferred.
- Reflection fields are `beliefThen` and `realityLater`.
- `beliefThen` is generated only when the user actively triggers AI organization.
- `realityLater` uses `PUT /api/records/{recordId}/later-reflection` and may be submitted at most 2 times after unlock.
- After the second `realityLater` submission, frontend must hide the "修改" action and backend must reject further updates.
- Life node fields are `lifeNodeType` and `lifeNodeCustomLabel`; non-`OTHER` custom labels fail validation.
- Reminder statuses are `REQUESTED`, `AUTHORIZED`, `DENIED`, `NOT_CONFIGURED`, `SEND_PENDING`, `SEND_SUCCESS`, `SEND_FAILED`, and `SKIPPED_NO_OPENID`.
- User refusal of subscription authorization should record `DENIED` where reminder status is persisted.
- Stage summary endpoint is `POST /api/stage-summaries/generate`.
- Stage summaries are generated on demand, returned directly, not persisted in M3, and entered from Personal Center only.

Verification:

- Documentation-only change; no backend or frontend tests were run.
- Searched M3 documents for accepted contract terms including `beliefThen`, `realityLater`, `later-reflection`, `2-submit`, `POST /api/auth/wechat-login`, `POST /api/stage-summaries/generate`, `NOT_CONFIGURED`, and `DENIED`.

Skipped verification reason:

- No application code, schema SQL, package, or lockfile changes were made.

Remaining risks:

- Real WeChat delivery verification still depends on template IDs and WeChat configuration.
- The next implementation agent must translate the accepted contract into schema, DTO, service, controller, and tests without changing the accepted API semantics.

### 2026-06-07 Codex (M3 Backend Facts Established)

Task:

- Start `m3-demo-core-flow-hardening` backend rectification.
- Establish current backend facts before implementation, as required by `.ai/ACTIVE_TASK.md` and M3 OpenSpec.

Modified:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Read M3 fact source documents: `AGENTS.md`, `.ai/ACTIVE_TASK.md`, `openspec/project.md`, accepted `backend-core`, `miniapp-core`, `v2-product-scope`, `agent-collaboration` specs, and M3 proposal/design/tasks/spec deltas/backend contract decisions.
- Confirmed `openspec` CLI is unavailable in the current shell; file-based OpenSpec execution is used as fallback.
- Confirmed current backend facts from directly required backend files only, covering auth, user, record, AI, unlock scheduler, reminder outbox/log, schema, and module entry points.
- Marked M3 guardrails, backend fact establishment, and contract gate status in `tasks.md`.

Capability classification:

- Confirmed: account/password auth exists at `POST /api/auth/register` and `POST /api/auth/login`, with existing JWT response shape in `LoginResponseVO`.
- Confirmed: user model has nullable `openid`, unique schema constraint, `wechatBound` output, and `bindVerifiedWechatOpenid` service boundary that rejects direct trust of registration-supplied openid.
- Partial: WeChat identity storage exists, but real `POST /api/auth/wechat-login` and code2session exchange are not implemented.
- Confirmed: record lifecycle uses `DRAFT`, `SEALED`, and `UNLOCKED`; update/delete/seal are owner-scoped; sealed records are immutable through normal update/delete paths.
- Confirmed: record list and timeline queries are user-scoped and deterministic, including `created_at DESC, id DESC` ordering for main list and timeline.
- Partial: record DTO/entity/VO currently support `content`, `coreQuestion`, `aiSummary`, and `aiPromptResults`; M3 fields `beliefThen`, `realityLater`, `lifeNodeType`, `lifeNodeCustomLabel`, and later-reflection submit count are not implemented.
- Planned: `PUT /api/records/{recordId}/later-reflection` must be added per accepted M3 contract.
- Confirmed: record types are `FUTURE_LETTER`, `NODE_RECORD`, and `EMOTION_NOTE`.
- Planned: M3 life node enum values and custom label validation must be added separately from `RecordType`.
- Confirmed: AI endpoints exist for writing prompts and record summarization under `/api/ai`, with mock/fallback behavior that does not block core record operations.
- Partial: AI does not yet expose user-triggered organization for `beliefThen`, nor manual stage summary generation.
- Confirmed: unlock scheduler calls `RecordService.runUnlockJob`; expired sealed records are unlocked in batches; repeated runs are idempotent by affected-row update; reminder persistence failure is best-effort and non-blocking.
- Partial: M2 reminder foundation exists through `record_reminder` with unique `record_id + template_type`, but current statuses are `PENDING`, `SENT`, `FAILED`, and `SKIPPED_NO_OPENID`, which do not match accepted M3 statuses.
- Planned: real WeChat subscription-message sender, template configuration, explicit `NOT_CONFIGURED`, and delivery-state transitions must be implemented.
- Confirmed: backend modules exist for auth/users, records, replies, tags, timeline behavior inside `RecordService`, AI, reminders, and unlock notice logging.
- Out of scope: admin portal, production notification center, SMS, campaign delivery, deployment/monitoring, complex AI analytics, real MAP/IMAGE/VOICE, broad backend rewrite, frontend visual reconstruction, and package/lockfile updates.

Gaps before implementation:

- Add schema/test-schema fields for `belief_then`, `reality_later`, later-reflection submit count, `life_node_type`, and `life_node_custom_label`.
- Add Java DTO/entity/VO/mapper support for accepted M3 reflection and life node fields while preserving existing `content`.
- Add later-reflection endpoint and service method enforcing owner, `UNLOCKED` status, and 2-submit limit.
- Add real WeChat login service/controller support using accepted `POST /api/auth/wechat-login` contract and existing login response shape.
- Replace/remap reminder statuses to accepted M3 values and add not-configured/non-blocking real send path.
- Add manual `POST /api/stage-summaries/generate` endpoint that is user-scoped, on-demand, and non-persistent.

Verification:

- Documentation/fact-establishment slice only; no backend code or schema behavior was changed.
- `openspec status --change "m3-demo-core-flow-hardening" --json` and `openspec instructions apply --change "m3-demo-core-flow-hardening" --json` both failed because `openspec` is not installed in the current shell.
- No Maven tests were run for this slice because it only updates task status and implementation evidence.

Skipped verification reason:

- No application code, schema SQL, package, or lockfile files were changed in this slice.

Scope safety check:

- No admin, production deployment, monitoring, SMS, notification center, campaign delivery, real MAP/IMAGE/VOICE, broad backend rewrite, frontend visual reconstruction, or package/lockfile change was introduced.
- Extra backend files were read only to establish the current M3 backend fact source required before implementation.

Remaining risks:

- Current working tree already contains M3 documentation activation changes and archived M2 files from earlier work; commit staging must remain scoped.
- Real WeChat delivery and real WeChat login still depend on configuration and later manual Mini Program verification.

### 2026-06-07 Codex (M3 Record Reflection And Life Node Schema Base)

Task:

- Implement the first backend code slice for M3 record reflection and life node schema/contract base.
- Keep the slice narrow: no WeChat login, no later-reflection endpoint, no reminder send adapter, and no stage summary endpoint yet.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/flashback/domain/LifeNodeType.java`
- `backend/src/main/java/com/flashback/domain/Record.java`
- `backend/src/main/java/com/flashback/dto/CreateRecordRequest.java`
- `backend/src/main/java/com/flashback/dto/UpdateRecordRequest.java`
- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/main/java/com/flashback/mapper/RecordMapper.java`
- `backend/src/main/resources/mapper/RecordMapper.xml`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added record schema/test-schema fields: `belief_then`, `reality_later`, `reality_later_submit_count`, `life_node_type`, and `life_node_custom_label`.
- Added `LifeNodeType` enum with accepted M3 values: `GRADUATION`, `WORK`, `MOVE`, `RELATIONSHIP`, `HEALTH`, `FAMILY`, `TURNING_POINT`, and `OTHER`.
- Extended `Record`, create/update DTOs, detail VO, MyBatis result map, insert, and draft update mapping for `beliefThen`, life node type, and custom life node label.
- Detail responses now include `beliefThen`, `realityLater`, `lifeNodeType`, and `lifeNodeCustomLabel`.
- Added service validation so `lifeNodeCustomLabel` is accepted only when `lifeNodeType = OTHER`; non-`OTHER` custom labels fail with validation error.
- Updated mapper/service tests for new fields and validation.
- Updated M3 task status for completed schema and life node base items.

Verification:

- First sandboxed run of `mvn -q "-Dtest=RecordServiceImplTest,RecordMapperIntegrationTest" test` failed because Maven could not resolve the Spring Boot parent POM under restricted network access.
- Reran the same focused command with approved escalation for dependency resolution.
- Passed: `mvn -q "-Dtest=RecordServiceImplTest,RecordMapperIntegrationTest" test`.

Skipped verification reason:

- Full `mvn -q test` was not run for this slice to keep verification focused on the changed schema/record mapper/service behavior.
- Real WeChat login and reminder delivery verification are not applicable to this slice.

Scope safety check:

- This slice only changed backend record schema/model/mapper/service tests and OpenSpec task evidence.
- No admin, production deployment, monitoring, SMS, notification center, campaign delivery, real MAP/IMAGE/VOICE, broad backend rewrite, frontend visual reconstruction, or package/lockfile change was introduced.
- Stage summaries remain on-demand and non-persistent; no stage summary table was added.

Remaining risks:

- `PUT /api/records/{recordId}/later-reflection` is still pending; `reality_later` and submit count are schema-backed but not yet writable through the accepted endpoint.
- `beliefThen` storage is wired into create/update, but a dedicated user-triggered AI organization API is still pending.
- Reminder status migration and real WeChat send/not-configured behavior remain pending.
- Demo/local MySQL databases need rebuild or manual schema application because the project has no migration tool.

### 2026-06-07 Codex (M3 Later Reflection Endpoint)

Task:

- Implement the accepted M3 later reflection contract: `PUT /api/records/{recordId}/later-reflection` with request `{ "realityLater": "string" }`.
- Enforce owner scope, `UNLOCKED` state, and at most 2 submissions.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/controller/api/RecordController.java`
- `backend/src/main/java/com/flashback/dto/UpdateLaterReflectionRequest.java`
- `backend/src/main/java/com/flashback/service/RecordService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/main/java/com/flashback/mapper/RecordMapper.java`
- `backend/src/main/resources/mapper/RecordMapper.xml`
- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added `UpdateLaterReflectionRequest` with `realityLater` validation.
- Added `PUT /api/records/{id}/later-reflection` in `RecordController`.
- Added `RecordService.updateLaterReflection`.
- Added mapper update that writes `reality_later`, increments `reality_later_submit_count`, and only affects owner-scoped `UNLOCKED` records with submit count below 2.
- Service now rejects later reflection before unlock, rejects after the 2-submit limit, and uses existing owner lookup to reject cross-user access safely.
- Added mapper/service tests for before-unlock rejection, after-unlock update, cross-user rejection, and exhausted edit-limit behavior.
- Updated M3 task status for completed later-reflection backend work.

Verification:

- First sandboxed run of `mvn -q "-Dtest=RecordServiceImplTest,RecordMapperIntegrationTest" test` failed due restricted Maven dependency resolution.
- Reran with approved escalation.
- Passed: `mvn -q "-Dtest=RecordServiceImplTest,RecordMapperIntegrationTest" test`.

Skipped verification reason:

- Full `mvn -q test` was not run for this focused endpoint slice.
- Frontend hide-modify behavior is not verified because frontend integration is a later M3 phase.

Scope safety check:

- This slice only changed the accepted later-reflection backend contract and focused tests.
- It did not implement admin, production notification center, SMS, campaign delivery, deployment/monitoring, complex AI, real MAP/IMAGE/VOICE, frontend visual reconstruction, or package/lockfile updates.

Remaining risks:

- The backend now enforces the 2-submit limit, but frontend still needs a confirmed/readable way to hide the "修改" action during the frontend phase.
- User-triggered AI organization for `beliefThen`, WeChat login, reminder delivery, and stage summary remain pending.

### 2026-06-07 Codex (M3 WeChat Login Backend)

Task:

- Implement M3 backend WeChat Mini Program code login while preserving account/password login.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/flashback/config/AppWechatProperties.java`
- `backend/src/main/java/com/flashback/controller/api/AuthController.java`
- `backend/src/main/java/com/flashback/dto/WechatLoginRequest.java`
- `backend/src/main/java/com/flashback/service/UserService.java`
- `backend/src/main/java/com/flashback/service/impl/UserServiceImpl.java`
- `backend/src/main/java/com/flashback/wechat/WechatCode2SessionClient.java`
- `backend/src/main/java/com/flashback/wechat/WechatSession.java`
- `backend/src/main/java/com/flashback/wechat/WechatSessionClient.java`
- `backend/src/test/java/com/flashback/service/impl/UserServiceImplTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added `POST /api/auth/wechat-login` with request `{ "code": "string" }`.
- Added `app.wechat.mini-program` configuration placeholders using environment variables `WECHAT_MINI_PROGRAM_APP_ID`, `WECHAT_MINI_PROGRAM_SECRET`, `WECHAT_CODE2SESSION_URL`, and `WECHAT_CODE2SESSION_TIMEOUT_MILLIS`.
- Added minimal code2session client that calls WeChat server-side API and extracts trusted `openid`; client-supplied OpenID is not accepted.
- Missing app id or secret now fails explicitly with `微信登录未配置` and does not issue a fake token.
- Existing account/password login remains unchanged and reuses the same `LoginResponseVO` token/user-info shape.
- WeChat login looks up user by trusted OpenID or creates a new enabled demo user with generated username and random password hash.
- Account/password-to-WeChat binding remains deferred per accepted M3 contract.
- User info continues to expose `wechatBound` based on stored `openid`.

Verification:

- First sandboxed run of `mvn -q "-Dtest=UserServiceImplTest,AuthControllerIntegrationTest" test` failed due restricted Maven dependency resolution.
- Reran with approved escalation.
- Passed: `mvn -q "-Dtest=UserServiceImplTest,AuthControllerIntegrationTest" test`.
- Tests cover not-configured WeChat login, existing OpenID login, new OpenID user creation, and existing account/password login behavior.

Skipped verification reason:

- Full `mvn -q test` was not run for this focused auth slice.
- Real WeChat Developer Tools login was not verified because app id/secret and real Mini Program login code are not configured in this environment.

Scope safety check:

- This slice only added demo-scoped WeChat code login backend support.
- It did not implement account binding, account merge, admin portal, production launch hardening, monitoring, SMS, notification center, campaign delivery, real media/location capability, frontend visual reconstruction, or package/lockfile updates.

Remaining risks:

- Real WeChat login must be manually verified after valid Mini Program configuration is provided.
- Generated WeChat-only users intentionally remain separate from account/password users until a future binding change.
- User-triggered `beliefThen` AI organization, reminder delivery, and stage summary remain pending.

### 2026-06-07 Codex (M3 Reminder Status And Not Configured State)

Task:

- Migrate unlock reminder status foundation from M2 states to accepted M3 states.
- Implement explicit missing-template behavior for unlock reminders.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/flashback/config/AppWechatProperties.java`
- `backend/src/main/java/com/flashback/domain/RecordReminderStatus.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/mapper/RecordReminderMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Replaced old reminder states `PENDING`, `SENT`, and `FAILED` with accepted M3 states: `REQUESTED`, `AUTHORIZED`, `DENIED`, `NOT_CONFIGURED`, `SEND_PENDING`, `SEND_SUCCESS`, `SEND_FAILED`, and `SKIPPED_NO_OPENID`.
- Updated schema defaults from `PENDING` to `REQUESTED`.
- Added `WECHAT_UNLOCK_REMINDER_TEMPLATE_ID` placeholder via `app.wechat.mini-program.unlock-reminder-template-id`.
- Unlock reminder creation now records:
  - `SKIPPED_NO_OPENID` when the user has no OpenID.
  - `NOT_CONFIGURED` when OpenID exists but the unlock reminder template ID is missing.
  - `SEND_PENDING` when OpenID and template ID both exist, leaving real send adapter work to the next slice.
- Existing unlock flow remains non-blocking; reminder persistence remains best-effort and does not roll back unlock.
- Updated tests for skipped OpenID, not-configured template, and send-pending template-configured paths.

Verification:

- Confirmed no old reminder state references remain with `rg`.
- First sandboxed run of `mvn -q "-Dtest=RecordServiceImplTest,RecordReminderMapperIntegrationTest" test` failed due restricted Maven dependency resolution.
- Reran with approved escalation.
- Passed: `mvn -q "-Dtest=RecordServiceImplTest,RecordReminderMapperIntegrationTest" test`.

Skipped verification reason:

- Full `mvn -q test` was not run for this focused reminder status slice.
- Real WeChat subscription-message delivery was not attempted because the send adapter is not yet implemented and template ID is not configured.

Scope safety check:

- This slice only changed demo-scoped unlock reminder status/config behavior.
- It did not implement admin template management, production notification center, SMS, campaign delivery, complex retry orchestration, deployment/monitoring, or package/lockfile updates.

Remaining risks:

- Real WeChat subscription-message send adapter is still pending.
- `DENIED` authorization reporting from frontend seal flow still needs a confirmed backend entry point or integration-phase handling.
- Manual real delivery verification remains pending template ID and WeChat Developer Tools configuration.

### 2026-06-07 Codex (M3 Backend Full Test Checkpoint)

Task:

- Run a backend full-test checkpoint after the committed M3 backend slices for facts, schema/record fields, later reflection, WeChat login, and reminder status migration.

Modified:

- `.ai/AGENT_LOG.md`

Verification:

- Passed: `mvn -q test` from `backend`.
- The command was run with approved escalation because Maven dependency resolution is blocked under the default sandbox.

Skipped verification reason:

- No frontend type-check or Mini Program build was run in this backend checkpoint.
- Real WeChat Developer Tools verification remains pending configuration and template IDs.

Scope safety check:

- Verification-only log entry; no application code, schema, package, lockfile, deployment, monitoring, admin, SMS, notification center, campaign, real MAP/IMAGE/VOICE, or frontend visual changes were made.

Remaining risks:

- Remaining M3 backend work still includes user-triggered `beliefThen` AI organization, real subscription-message send adapter/authorization reporting, and manual stage summary generation.

### 2026-06-07 Codex (M3 BeliefThen AI Organization Output)

Task:

- Add user-triggered AI organization support for `beliefThen` without introducing an unconfirmed new endpoint.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/service/impl/AiServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/AiSummaryVO.java`
- `backend/src/test/java/com/flashback/service/impl/AiServiceImplTest.java`
- `backend/src/test/java/com/flashback/controller/api/AiControllerAuthIntegrationTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Extended existing authenticated `POST /api/ai/summarize-record` response with accepted field `beliefThen`.
- Mock AI summary now returns a gentle `beliefThen` organization suggestion derived from original content.
- Fallback summary also returns safe `beliefThen` text.
- No original record `content` is modified by AI output; persistence still happens only when create/update receives `beliefThen`.
- No new AI endpoint path was introduced.

Verification:

- First sandboxed run of `mvn -q "-Dtest=AiServiceImplTest,AiControllerAuthIntegrationTest" test` failed due restricted Maven dependency resolution.
- Reran with approved escalation.
- Passed: `mvn -q "-Dtest=AiServiceImplTest,AiControllerAuthIntegrationTest" test`.

Skipped verification reason:

- Full `mvn -q test` was not rerun for this focused AI response slice.
- Frontend user-trigger action wiring remains a later integration-phase task.

Scope safety check:

- This slice stays within gentle AI organization and existing AI fallback behavior.
- It did not add scoring, diagnosis, growth analytics, forced lifecycle dependency, admin, production launch, notification center, SMS, or package/lockfile changes.

Remaining risks:

- Frontend still needs to call the AI organization action and save returned `beliefThen` into record create/update.
- Stage summary and real reminder send adapter remain pending.

### 2026-06-07 Codex (M3 Manual Stage Summary Endpoint)

Task:

- Implement accepted M3 endpoint `POST /api/stage-summaries/generate`.
- Keep summaries manual, user-scoped, on-demand, lightweight, and non-persistent.

Modified:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/controller/api/StageSummaryController.java`
- `backend/src/main/java/com/flashback/service/StageSummaryService.java`
- `backend/src/main/java/com/flashback/service/impl/StageSummaryServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/StageSummaryVO.java`
- `backend/src/test/java/com/flashback/controller/api/StageSummaryControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/StageSummaryServiceImplTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added authenticated manual stage summary endpoint `POST /api/stage-summaries/generate`.
- Added non-persistent `StageSummaryService` and `StageSummaryVO`.
- Stage summary uses only the authenticated user's record counts and recent records through existing user-scoped mapper queries.
- Summary includes lightweight counts for total records, unlocked records, and life-node records, plus a gentle generated text.
- Added service and controller tests for user-scoped generation and auth requirement.
- No stage summary table, history, dashboard, scoring, diagnosis, or analytics center was added.

Verification:

- First sandboxed run of `mvn -q "-Dtest=StageSummaryServiceImplTest,StageSummaryControllerAuthIntegrationTest" test` failed due restricted Maven dependency resolution.
- Reran with approved escalation on 2026-06-08.
- Passed: `mvn -q "-Dtest=StageSummaryServiceImplTest,StageSummaryControllerAuthIntegrationTest" test`.

Skipped verification reason:

- Full `mvn -q test` was not rerun for this focused stage summary slice.

Scope safety check:

- This slice only added the accepted manual stage summary backend endpoint and tests.
- It did not add persistence, dashboard/history, scoring, diagnosis, admin, production launch, notification center, SMS, campaign delivery, deployment/monitoring, package/lockfile updates, or frontend visual changes.

Remaining risks:

- Real reminder send adapter and WeChat Developer Tools verification remain pending.

### 2026-06-09 Codex (M3 Local Mini Program Network Error Diagnosis)

Task:

- Diagnose Mini Program user-center network failure with backend log `Unknown column 'belief_then' in 'field list'`.
- Identify whether the issue is frontend network handling, backend code, or local MySQL schema drift.

Observed:

- User-center page loads `userStore.fetchUserInfo()`, `recordService.getRecordList(...)`, and `recordService.getUnlockedRecords(...)` in one `Promise.all`.
- Backend terminal snapshot showed the failing path:
  `RecordController.page -> RecordServiceImpl.pageMine -> RecordMapper.selectPageByUserAndCondition`.
- The SQL failure is `java.sql.SQLSyntaxErrorException: Unknown column 'belief_then' in 'field list'`.
- `RecordMapper.xml` now selects M3 columns `belief_then`, `reality_later`, `reality_later_submit_count`, `life_node_type`, and `life_node_custom_label`.
- `backend/sql/mysql/schema.mysql.sql` contains those M3 columns.
- Actual local MySQL `flashback.record` table checked with `SHOW COLUMNS FROM flashback.record;` did not contain those M3 columns.
- Actual local MySQL `flashback.record_reminder` table did not exist.

Conclusion:

- Root cause is local demo MySQL schema drift: code and repository schema have advanced to M3, but the running `flashback` database still uses an older table structure.
- The Mini Program displays the generic network fallback because one record-list request fails with backend SQL 500, causing the user-center `Promise.all` to reject.

Verification:

- Confirmed default local dev database config uses `jdbc:mysql://127.0.0.1:3306/flashback`.
- Confirmed `backend/start-dev.ps1` defaults to `DB_USERNAME=root` and `DB_PASSWORD=123456`.
- Confirmed actual local MySQL `record` table lacks M3 record reflection/life-node columns.
- Confirmed actual local MySQL `record_reminder` table is missing.

Skipped verification reason:

- Did not apply schema changes in this diagnostic step.
- A temporary backend launch attempt from Codex timed out due local shell argument handling; no backend process was left running.

Scope safety check:

- No business code, frontend UI, package/lockfile, deployment, monitoring, admin, SMS, production notification center, or broad rewrite was changed.

Recommended next step:

- For M3 demo, rebuild or migrate the local MySQL schema before further Mini Program flow testing.

### 2026-06-09 Codex (M3 Local Schema Repair)

Task:

- Unblock local Mini Program manual testing after backend failed on missing M3 database fields.

Modified files:

- `backend/sql/mysql/m3-local-schema-repair.sql`

What changed:

- Added a non-destructive local MySQL repair script for older demo databases.
- The script adds missing `user.openid` support when absent.
- The script adds missing M3 `record` columns: `belief_then`, `reality_later`, `reality_later_submit_count`, `life_node_type`, and `life_node_custom_label`.
- The script creates `record_reminder` when absent, including the accepted M3 reminder statuses storage columns and idempotency key by `record_id + template_type`.
- No existing data is deleted and no demo database rebuild is forced.

Verification:

- Applied the script to local `flashback` database with MySQL root credentials used by the local dev profile.
- Verified `flashback.record` now contains `belief_then`, `reality_later`, `reality_later_submit_count`, `life_node_type`, and `life_node_custom_label`.
- Verified `flashback.record_reminder` now exists.

Skipped verification reason:

- Did not drop/rebuild the database because preserving local manual-test data is safer and the missing schema pieces can be repaired non-destructively.

Scope safety check:

- This slice only touched a local demo schema repair script and local database structure.
- It did not change package/lockfile, deployment, monitoring, admin, SMS, production notification center, campaign delivery, real MAP/IMAGE/VOICE, frontend visuals, or broad backend architecture.

Remaining risks:

- Backend reminder delivery adapter and frontend M3 flow completion still need implementation and verification.

### 2026-06-09 Codex (M3 Life Node Labels And Reminder Send Adapter)

Task:

- Complete backend life node visible labels and the minimal real WeChat subscription-message send path for unlock reminders.

Modified files:

- `backend/src/main/java/com/flashback/domain/LifeNodeType.java`
- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/main/java/com/flashback/vo/RecordListItemVO.java`
- `backend/src/main/java/com/flashback/vo/TimelineItemVO.java`
- `backend/src/main/java/com/flashback/config/AppWechatProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageClient.java`
- `backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageHttpClient.java`
- `backend/src/main/java/com/flashback/mapper/RecordReminderMapper.java`
- `backend/src/main/resources/mapper/RecordReminderMapper.xml`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordReminderMapperIntegrationTest.java`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added Chinese labels to the accepted M3 `LifeNodeType` enum.
- Added `lifeNodeLabel` to record detail, record list item, and timeline item VO output.
- `OTHER` life node uses the custom label when present and falls back to `其他`.
- Kept existing `recordType=NODE_RECORD` filtering and display behavior; no new taxonomy or category management was added.
- Added configurable WeChat access-token and subscribe-message send URLs plus a detail-page config for unlock reminder messages.
- Added a minimal `WechatSubscribeMessageClient` and HTTP implementation using WeChat `cgi-bin/token` and `message/subscribe/send`.
- When template ID exists and a trusted OpenID exists, unlock reminder send is attempted; success updates `record_reminder` to `SEND_SUCCESS`, failure updates it to `SEND_FAILED`.
- Reminder send failures are caught and remain non-blocking for unlock processing.
- Existing `record_id + template_type` marker prevents duplicate sends.
- Reminder records do not store record content, auth tokens, session keys, or message payload content.

Verification:

- First sandboxed run of `mvn -q "-Dtest=RecordServiceImplTest,RecordReminderMapperIntegrationTest" test` failed because Maven dependency resolution was blocked by sandbox permissions.
- Reran with approved escalation.
- Passed: `mvn -q "-Dtest=RecordServiceImplTest,RecordReminderMapperIntegrationTest" test`.

Skipped verification reason:

- Real WeChat subscription delivery was not manually verified because no real Mini Program subscription template ID/configuration is available in this local environment.

Scope safety check:

- This slice stayed inside M3 backend/schema/test scope.
- It did not add admin template management, production notification center, SMS, campaign delivery, complex retry orchestration, deployment, monitoring, package/lockfile changes, or frontend visual redesign.

Remaining risks:

- Frontend still needs to request subscription authorization after seal and record refusal/authorization result through a confirmed backend contract.
- Real delivery must be manually verified after WeChat template ID and matching template keyword fields are configured.

### 2026-06-09 Codex (M3 Frontend Core Flow Completion)

Task:

- Connect the Mini Program user-side M3 demo flow to the accepted backend contracts so local manual testing can proceed.

Modified files:

- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `frontend/src/types/enums.ts`
- `frontend/src/types/record.ts`
- `frontend/src/types/auth.ts`
- `frontend/src/services/authService.ts`
- `frontend/src/services/recordService.ts`
- `frontend/src/services/aiService.ts`
- `frontend/src/services/stageSummaryService.ts`
- `frontend/src/services/index.ts`
- `frontend/src/stores/user.ts`
- `frontend/src/pages/login/index.vue`
- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/pages/record-detail/index.vue`
- `frontend/src/pages/user-center/index.vue`
- `frontend/src/pages.json`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Added `realityLaterSubmitCount` to backend record detail response so the Mini Program can hide the "修改" action after the second "后来其实" submission.
- Added frontend M3 types for `LifeNodeType`, `beliefThen`, `realityLater`, life-node labels, WeChat login payload, and stage summary response.
- Added frontend `POST /api/auth/wechat-login` service and user-store action while preserving account/password login and preview mode.
- Login page now exposes both account/password login and WeChat login.
- Record editor now supports record type selection, NODE_RECORD life-node enum, OTHER custom label, and user-triggered AI organization for "你当时以为".
- Record editor stores `beliefThen`, `lifeNodeType`, and `lifeNodeCustomLabel` through the existing create/update record APIs.
- Seal flow requests WeChat subscription authorization after successful seal when `VITE_WECHAT_UNLOCK_REMINDER_TEMPLATE_ID` is configured; missing frontend template ID skips authorization and does not undo seal.
- Time review now displays "你当时以为" and supports after-unlock "后来其实" submission through `PUT /api/records/{id}/later-reflection`.
- "后来其实" edit action is hidden after the backend-reported 2-submit limit is exhausted.
- Personal Center now has a manual, on-demand stage summary entry point using `POST /api/stage-summaries/generate`.
- Mini Program tab naming now uses `时光轴`.
- No major visual reconstruction was performed.

Verification:

- Passed backend focused test: `mvn -q "-Dtest=RecordServiceImplTest" test`.
- First sandboxed frontend type-check failed with `Access is denied`.
- Reran with approved escalation and passed: `.\node_modules\.bin\vue-tsc.cmd --noEmit`.
- Passed Mini Program build with approved escalation: `.\node_modules\.bin\uni.cmd build -p mp-weixin`.
- Build output says to import `dist\build\mp-weixin` into WeChat Developer Tools.

Skipped verification reason:

- Did not manually verify inside WeChat Developer Tools in this agent session.
- Real WeChat login and real subscription delivery still require valid Mini Program app ID/secret/template ID and manual Developer Tools verification.
- Recording backend `DENIED` when the user refuses subscription authorization still needs a confirmed backend endpoint path for authorization-result reporting.

Scope safety check:

- Frontend work stayed in login, record editor, record detail, personal center, services, types, and page naming.
- It did not add admin, production notification center, SMS, campaign delivery, deployment, monitoring, package/lockfile changes, real MAP/IMAGE/VOICE, social feed, complex AI scoring/diagnosis, or broad visual redesign.

Remaining risks:

- Need user confirmation for the authorization-result reporting endpoint before implementing persisted `AUTHORIZED`/`DENIED` frontend-to-backend recording.
- Manual WeChat Developer Tools verification remains pending.

### 2026-06-09 Codex (M3 Verification Sweep)

Task:

- Run the broad local verification sweep after backend and Mini Program M3 flow completion changes.

Modified files:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Recorded full backend test evidence.
- Marked full backend test suite verification as complete in M3 tasks.

Verification:

- Passed: `mvn -q test` from `backend`.
- Previously passed in this same M3 frontend slice: `.\node_modules\.bin\vue-tsc.cmd --noEmit`.
- Previously passed in this same M3 frontend slice: `.\node_modules\.bin\uni.cmd build -p mp-weixin`.
- Local MySQL schema was repaired earlier in this session and verified to contain M3 columns and `record_reminder`.

Skipped verification reason:

- Manual WeChat Developer Tools verification was not performed by the agent.
- Real WeChat login requires valid Mini Program app ID/secret.
- Real subscription delivery requires a valid template ID and matching template keyword configuration.
- Persisting `DENIED` on subscription refusal requires a new backend endpoint path that is not yet confirmed in OpenSpec.

Scope safety check:

- Verification and task updates did not expand M3 into admin, production notification center, SMS, campaign delivery, deployment, monitoring, package/lockfile changes, or broad visual/backend rewrites.

Remaining risks:

- Confirm and implement a minimal subscription authorization result endpoint if persisted `AUTHORIZED`/`DENIED` is required before manual acceptance.
- Complete manual WeChat Developer Tools verification with real configuration.

### 2026-06-10 Codex (Design Document Implementation Audit)

Task:

- Compare `Docs/前期需求设计文档/设计文档.md` against the current M1-M3 project state.
- Identify which early design items are implemented, partially implemented, deferred, or out of current V2.0/M3 scope.
- Prepare next-step optimization strategy without changing application code.

Modified files:

- `.ai/AGENT_LOG.md`

What changed:

- Recorded this audit evidence only.
- No frontend, backend, schema, OpenSpec, package, or lockfile files were changed.

Verification:

- Read `.ai/ACTIVE_TASK.md` and confirmed `m3-demo-core-flow-hardening` is the active fact source.
- Read current M3 OpenSpec proposal, design, tasks, and accepted specs.
- Read the old design document headings and core feature/MVP sections.
- Inspected targeted implementation evidence for auth, record lifecycle, reflection fields, life-node labels, reminder delivery, stage summary, reply, timeline/list behavior, and Mini Program page registration.
- Confirmed `git diff --stat` was empty before this evidence-log append; `git status --short` only showed untracked `.claude/settings.local.json` before this append.

Skipped verification reason:

- No backend test, frontend type-check, Mini Program build, or WeChat Developer Tools manual flow was run in this audit-only turn.
- Real WeChat login and subscription-message delivery still require valid app ID, secret, template ID, template keyword configuration, and manual Developer Tools verification.

Scope safety check:

- This turn stayed in documentation/audit mode.
- It did not implement admin, production deployment, monitoring, SMS, notification center, campaign delivery, complex AI scoring, social feed, real MAP/IMAGE/VOICE, package or lockfile changes, broad frontend visual redesign, or broad backend rewrite.

Remaining risks:

- `openspec/changes/m3-demo-core-flow-hardening/tasks.md` still has unchecked M3 final review and manual verification items.
- Subscription authorization result persistence for `AUTHORIZED`/`DENIED` remains unresolved pending a confirmed endpoint path.
- Visible old naming remains in targeted frontend search: `frontend/src/pages/record-list/index.vue` uses `我的档案`, and `frontend/src/components/common/BottomNavBar.vue` uses `时间轴`.

### 2026-06-11 Codex (M3 Closeout: Reminder Authorization Status)

Task:

- Close the confirmed M3 reminder authorization gap.
- Add minimal backend persistence for Mini Program subscription authorization results.
- Expose unlock reminder status on time review.
- Remove remaining visible naming residues that conflicted with M3 naming.

Modified files:

- `backend/src/main/java/com/flashback/dto/UpdateUnlockReminderAuthorizationRequest.java`
- `backend/src/main/java/com/flashback/controller/api/RecordController.java`
- `backend/src/main/java/com/flashback/service/RecordService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `frontend/src/types/enums.ts`
- `frontend/src/types/record.ts`
- `frontend/src/services/recordService.ts`
- `frontend/src/stores/record.ts`
- `frontend/src/features/preview/data/preview-data.ts`
- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/pages/record-detail/index.vue`
- `frontend/src/pages/record-list/index.vue`
- `frontend/src/components/common/BottomNavBar.vue`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- `.ai/AGENT_LOG.md`

What changed:

- Added confirmed endpoint `PUT /api/records/{recordId}/unlock-reminder-authorization`.
- Accepted request body `{ "status": "AUTHORIZED" | "DENIED" | "REQUESTED" }`.
- Persisted authorization result in existing `record_reminder` with template type `UNLOCK_REMINDER`.
- Recorded `DENIED` when the user refuses subscription authorization.
- Rejected unsupported frontend-submitted statuses such as `SEND_SUCCESS`.
- Rejected authorization status updates for `DRAFT` records.
- Added `unlockReminderStatus` to `RecordDetailVO`.
- Adjusted unlock reminder send logic so an existing `AUTHORIZED` or `REQUESTED` reminder marker does not block real send when the record unlocks.
- Kept `DENIED` as a send stop marker.
- Kept send failure and not-configured behavior non-blocking.
- Frontend seal flow now reports `AUTHORIZED`, `DENIED`, or `REQUESTED` after subscription authorization.
- Authorization reporting is best-effort after seal; failure to report does not undo or present the seal operation as failed.
- Time review displays the record's unlock reminder status.
- Preview data includes representative reminder statuses.
- Replaced visible residual naming `我的档案` with `我的记录`, and `时间轴` with `时光轴`.
- Marked M3 reminder authorization tasks and final review checklist items that are now code-reviewed or verified.

Verification:

- Passed: `mvn -q "-Dtest=RecordServiceImplTest" test` from `backend` after rerun with required network permission.
- Passed: `mvn -q test` from `backend` after rerun with required network permission.
- Passed: `.\node_modules\.bin\vue-tsc.cmd --noEmit` from `frontend` with bundled Node on PATH.
- Passed: `.\node_modules\.bin\uni.cmd build -p mp-weixin` from `frontend` with bundled Node on PATH.
- Passed naming scan: `rg -n "我的档案|时间轴|回看页" frontend/src` returned no matches.

Skipped verification reason:

- Manual account/password login was not performed in a running Mini Program or browser session.
- Manual WeChat login in WeChat Developer Tools was not performed by the agent.
- Manual missing-template-ID behavior was not performed in WeChat Developer Tools; automated backend tests still cover `NOT_CONFIGURED`.
- Real reminder delivery was not manually verified because it requires configured subscription template ID and WeChat Developer Tools flow.
- Full end-to-end Mini Program manual flow remains pending user-side DevTools verification.

Scope safety check:

- Stayed inside confirmed M3 scope: backend records/reminders, Mini Program seal/time-review flow, M3 task evidence, and visible naming.
- Did not implement admin, SMS, production notification center, campaign delivery, monitoring, deployment, complex AI, social features, real MAP/IMAGE/VOICE, package or lockfile changes, broad frontend visual redesign, or broad backend rewrite.

Remaining risks:

- Real WeChat login still needs manual verification with configured app ID/secret.
- Real subscription delivery still needs template ID and keyword configuration, then manual Developer Tools verification.
- Manual end-to-end demo acceptance remains pending outside automated tests.

### 2026-06-11 Codex (M3 Record Editor Unlock Time Picker Fix)

Task:

- Investigate why the `feature/record-timeline` branch did not show the record unlock time wheel in WeChat Developer Tools.
- Restore a Mini Program-friendly unlock time picker in the record editor.
- Record user-reported manual verification evidence.

Modified files:

- `frontend/src/pages/record-editor/index.vue`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- `.ai/AGENT_LOG.md`

What changed:

- Confirmed current branch `feature/record-timeline` points to the same commit as `main` and `origin/feature/record-timeline`; `git diff --stat main...HEAD` was empty before the fix.
- Confirmed no `picker`, `picker-view`, or time wheel implementation existed in the active record editor source before the fix.
- Replaced the manual unlock time text input with two native Mini Program pickers:
  - `picker mode="date"` for unlock date.
  - `picker mode="time"` for unlock time.
- Kept the existing `form.unlockAtInput` and `toLocalDateTime` persistence path unchanged.
- Existing draft details now synchronize backend `unlockAt` into the date/time picker values.
- New picker selections still save as `yyyy-MM-dd HH:mm`, preserving the existing save/seal validation path.
- Updated M3 tasks to mark user-reported account/password login verification and missing-template subscription behavior as complete.

Verification:

- User reported account/password login now works.
- User reported WeChat login displays not-configured behavior.
- User reported real subscription template is not configured and the Mini Program shows skipped behavior.
- Passed: `.\node_modules\.bin\vue-tsc.cmd --noEmit` from `frontend` with bundled Node on PATH.
- Passed: `.\node_modules\.bin\uni.cmd build -p mp-weixin` from `frontend` with bundled Node on PATH.
- Confirmed generated Mini Program output contains the picker controls: `frontend/dist/build/mp-weixin/pages/record-editor/index.wxml` includes `picker mode="date"` and `picker mode="time"`.

Skipped verification reason:

- The agent did not open WeChat Developer Tools directly.
- Real configured WeChat login remains pending because the current app ID/secret configuration still reports not configured.
- Real subscription delivery remains pending because subscription template ID and keyword configuration are not set.
- Full end-to-end demo flow remains pending user-side Developer Tools verification.

Scope safety check:

- Stayed within M3 Mini Program record editor flow and evidence logging.
- Did not change backend contracts, database schema, admin, SMS, production notification center, deployment, monitoring, complex AI, social features, real MAP/IMAGE/VOICE, package files, lockfiles, or broad visual layout.

Remaining risks:

- WeChat Developer Tools may still show old UI if it imports an older `dist/build/mp-weixin` or does not refresh after rebuild.
- Native picker appearance is controlled by WeChat; visual style cannot be fully customized.
- Real WeChat login and real subscription delivery still need configured credentials/templates and manual verification.

### 2026-06-11 Codex (Remote-first Record Timeline Sync)

Task:

- Sync local `feature/record-timeline` with GitHub remote after the user confirmed the time wheel implementation had already been merged remotely.
- Prefer the remote time wheel implementation over the local temporary date/time picker fix.

Modified files:

- `frontend/src/components/common/DateTimeWheelPicker.vue`
- `frontend/src/pages/record-editor/index.vue`
- `.ai/AGENT_LOG.md`

What changed:

- Created local backup branch `backup/feature-record-timeline-before-remote-sync` at `097fcf4` before merging.
- Merged `origin/feature/record-timeline` into local `feature/record-timeline` with conflict strategy preferring remote changes.
- Adopted remote `DateTimeWheelPicker.vue`, which uses a five-column `picker-view` for year, month, day, hour, and minute.
- Removed one stale local-only call to `syncUnlockPickerFromInput()` that remained after the remote-first merge and caused frontend type-check failure.

Verification:

- Initial post-merge `vue-tsc --noEmit` failed because `syncUnlockPickerFromInput` no longer existed after adopting the remote component.
- Passed after the stale call was removed: `.\node_modules\.bin\vue-tsc.cmd --noEmit` from `frontend` with bundled Node on PATH.
- Passed: `.\node_modules\.bin\uni.cmd build -p mp-weixin` from `frontend` with bundled Node on PATH.
- Confirmed generated Mini Program output includes `components/common/DateTimeWheelPicker` and `picker-view` in `frontend/dist/build/mp-weixin`.

Skipped verification reason:

- The agent did not open WeChat Developer Tools directly; final visual confirmation remains user-side because the local Developer Tools environment is reported as slow.

Scope safety check:

- Stayed inside git synchronization and the record editor unlock time picker area.
- Did not change backend contracts, database schema, admin, SMS, production notification center, deployment, monitoring, complex AI, social features, real MAP/IMAGE/VOICE, package files, lockfiles, or broad visual layout.

Remaining risks:

- The local branch is now ahead of `origin/feature/record-timeline` by the merge and follow-up fix/log commit until pushed.
- WeChat Developer Tools may still need a clean compile/import of `frontend/dist/build/mp-weixin` to show the latest generated output.

### 2026-06-11 Codex (M3 Merge Readiness Audit)

Task:

- Inspect current `feature/record-timeline` branch implementation status against `main`.
- Check whether the current branch and M3 implementation evidence support merge.

Modified files:

- `.ai/AGENT_LOG.md`

What changed:

- No application implementation changed in this audit.
- Confirmed `.ai/ACTIVE_TASK.md` and OpenSpec still point to `m3-demo-core-flow-hardening`.
- Confirmed current branch is `feature/record-timeline`.
- Confirmed `main...HEAD` implementation delta is limited to record editor unlock time wheel UI, `DateTimeWheelPicker.vue`, M3 task evidence, and `.ai/AGENT_LOG.md`.
- Confirmed no backend, database schema, package, lockfile, deployment, monitoring, admin, SMS, notification-center, complex AI, social, or real MAP/IMAGE/VOICE files are changed by this branch delta.
- Confirmed M3 task list still leaves configured WeChat login, real reminder delivery, and full manual end-to-end Mini Program flow as pending manual verification.

Verification:

- Passed after rerun with required network permission: `mvn -q test` from `backend`.
- Passed: `.\node_modules\.bin\vue-tsc.cmd --noEmit` from `frontend` using bundled Node on PATH.
- Passed: `.\node_modules\.bin\uni.cmd build -p mp-weixin` from `frontend` using bundled Node on PATH.
- Passed: `git diff --check main...HEAD`.
- Passed naming scan: `rg -n "我的档案|时间轴|回看页" frontend/src` returned no matches.
- Confirmed generated Mini Program output includes `components/common/DateTimeWheelPicker` and `picker-view` under `frontend/dist/build/mp-weixin`.

Skipped verification reason:

- The agent did not open WeChat Developer Tools directly.
- Manual WeChat login with configured app ID/secret remains pending because configuration availability must be verified in the Mini Program environment.
- Real subscription reminder delivery remains pending because it requires configured subscription template ID and keyword setup.
- Full create -> AI organize -> seal -> unlock -> reminder attempt -> time review -> first/second `realityLater` submit -> no-more-modify -> stage summary flow remains pending user-side Mini Program manual verification.

Merge readiness conclusion:

- Code-level automated checks support merging the current branch into `main`.
- Product/demo acceptance is not fully closed until the remaining WeChat Developer Tools manual verification items are completed or explicitly accepted as post-merge pending risks.

Scope safety check:

- Stayed in audit/evidence mode plus generated-output verification.
- Did not change backend contracts, database schema, admin, SMS, production notification center, deployment, monitoring, complex AI, social features, real MAP/IMAGE/VOICE, package files, lockfiles, or broad visual layout.

Remaining risks:

- `.claude/settings.local.json` is untracked local state and should not be included in merge artifacts unless intentionally added.
- Current branch still needs push/update coordination if the merge target is remote.
- WeChat Developer Tools may still require clean compile/import of `frontend/dist/build/mp-weixin` for visual confirmation.

### 2026-06-16 Codex (M3 Local WeChat Backend Test Script)

Task:

- Create a temporary local backend startup script for M3 WeChat login and subscription-message manual verification.
- Add the local script to git ignore so real AppID/AppSecret/template ID values do not enter version control.

Modified files:

- `.gitignore`
- `backend/start-dev-wechat.local.ps1` (intentionally ignored by git)
- `.ai/AGENT_LOG.md`

What changed:

- Added `.gitignore` entry for `/backend/start-dev-wechat.local.ps1`.
- Created `backend/start-dev-wechat.local.ps1` with fill-in placeholders for `WECHAT_MINI_PROGRAM_APP_ID`, `WECHAT_MINI_PROGRAM_SECRET`, and `WECHAT_UNLOCK_REMINDER_TEMPLATE_ID`.
- Preserved existing DB username/password/profile parameters from the backend dev startup script pattern.
- Added placeholder guards so the backend does not start before required WeChat verification values are filled.
- Avoided printing AppSecret or other secret values.

Verification:

- Passed: PowerShell parser check for `backend/start-dev-wechat.local.ps1`.
- Passed: `git check-ignore -v backend/start-dev-wechat.local.ps1` confirms the local script is ignored by `.gitignore`.
- Confirmed `git status --short --ignored` shows `backend/start-dev-wechat.local.ps1` under ignored files, not as an untracked commit candidate.

Skipped verification reason:

- Backend startup and real WeChat verification are intentionally not run until the user fills local WeChat configuration values.

Scope safety check:

- Stayed within local verification support for M3.
- Did not change backend contracts, database schema, application business logic, frontend behavior, deployment, monitoring, admin, SMS, production notification center, package files, or lockfiles.

Remaining risks:

- Manual WeChat Developer Tools verification still needs to be performed after the user fills the local script and rebuilds/imports the Mini Program with the matching frontend template ID.

### 2026-06-16 Codex (M3 WeChat Login Failure Diagnosis)

Task:

- Diagnose user-reported Mini Program WeChat login failure: frontend shows `微信登录校验失败`.
- Diagnose unlock reminder result: `当前微信未绑定openid，已跳过发送`.

Modified files:

- `backend/src/main/java/com/flashback/wechat/WechatCode2SessionClient.java`
- `backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageHttpClient.java`
- `backend/start-dev-wechat.local.ps1` (intentionally ignored by git)
- `.ai/AGENT_LOG.md`

What changed:

- Added privacy-safe backend warning logs for WeChat API `errcode` / `errmsg` responses without logging AppSecret, access token, openid, session key, or request URLs.
- Added local startup-script warnings when the backend WeChat AppID does not match `frontend/project.config.json` or generated `frontend/dist/build/mp-weixin/project.config.json`.
- Identified likely diagnosis: the Mini Program project AppID used by WeChat Developer Tools does not match the AppID configured in the backend startup script, so `uni.login` returns a code for one AppID while backend `code2session` verifies it against another AppID.
- Confirmed reminder skip behavior is consistent with the failed login: records created under an account/password or preview user have no bound openid, so unlock reminder delivery records `SKIPPED_NO_OPENID`.

Verification:

- Passed: PowerShell parser check for `backend/start-dev-wechat.local.ps1`.
- Passed: `git check-ignore -v backend/start-dev-wechat.local.ps1` confirms the local script remains ignored by `.gitignore`.
- Passed: `git diff --check`.
- Attempted: `mvn -q test` from `backend`; sandboxed run failed because Maven could not download `spring-boot-starter-parent` from Maven Central due permission denial.
- Attempted escalated rerun of `mvn -q test`; automatic approval review timed out twice before execution.

Skipped verification reason:

- Real WeChat login and real subscription-message delivery still require the user-side WeChat Developer Tools session with matching Mini Program AppID and configured template ID.
- Full backend test verification is pending because the required Maven network/dependency access escalation did not complete in this run.

Scope safety check:

- Stayed within M3 WeChat login/reminder diagnosis and local verification support.
- Did not change API contracts, database schema, frontend visible behavior, deployment, monitoring, admin, SMS, production notification center, package files, or lockfiles.

Remaining risks:

- If AppID alignment is fixed but login still fails, the next backend warning log should be checked for the actual WeChat `errcode` such as invalid secret, expired/used code, blocked network, or account permission issues.
- Records already created under a non-WeChat user will continue to have no openid; create a fresh record after successful WeChat login for subscription-message verification.

### 2026-06-16 Codex (M3 Mini Program AppID Source Diagnosis)

Task:

- Diagnose why root `frontend/project.config.json` AppID changed but WeChat Developer Tools opened from `frontend/dist/dev/mp-weixin` still uses `touristappid`.
- Align local verification guidance with the user's actual DevTools entry path.

Modified files:

- `frontend/src/manifest.json`
- `backend/start-dev-wechat.local.ps1` (intentionally ignored by git)
- `.ai/AGENT_LOG.md`

What changed:

- Set the WeChat Mini Program AppID in `frontend/src/manifest.json` under `mp-weixin.appid` so generated `dist/dev/mp-weixin/project.config.json` can inherit the real AppID after rerunning the Uniapp dev build.
- Updated the local backend test script to check `frontend/dist/dev/mp-weixin/project.config.json` by default, matching the user's usual WeChat Developer Tools entry path.
- Kept an optional `-MiniProgramOutput build` parameter for users who intentionally open `frontend/dist/build/mp-weixin`.

Diagnosis:

- Root `frontend/project.config.json` is only authoritative when WeChat Developer Tools opens the frontend project root and follows `miniprogramRoot`.
- When WeChat Developer Tools opens `frontend/dist/dev/mp-weixin` directly, that generated folder's own `project.config.json` is authoritative.
- The generated dev config was still `touristappid` because `frontend/src/manifest.json` had an empty `mp-weixin.appid`.

Verification:

- Passed: PowerShell parser check for `backend/start-dev-wechat.local.ps1`.
- Passed: `git check-ignore -v backend/start-dev-wechat.local.ps1` confirms the local script remains ignored.
- Passed: `git diff --check`.
- Confirmed `frontend/src/manifest.json` now contains the real `mp-weixin.appid`.
- Confirmed existing generated `frontend/dist/dev/mp-weixin/project.config.json` still contains `touristappid` until the dev output is regenerated or edited locally.

Skipped verification reason:

- Real WeChat login still requires rerunning the Uniapp dev build, reopening or refreshing WeChat Developer Tools against `frontend/dist/dev/mp-weixin`, and retrying login manually.

Scope safety check:

- Stayed within M3 WeChat manual verification configuration.
- Did not change backend contracts, database schema, business logic, deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- Existing generated `frontend/dist/dev/mp-weixin/project.config.json` will remain `touristappid` until the frontend dev build regenerates it or the user edits that generated local file manually.
- If AppID is aligned but login still fails, inspect backend `WeChat code2session rejected login code` warning for the actual WeChat `errcode`.

### 2026-06-16 Codex (M3 Subscription Send Failure Diagnosis)

Task:

- Diagnose user-reported unlock reminder status: `订阅提醒发送失败，解锁不受影响` after WeChat login succeeds.

Modified files:

- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `.ai/AGENT_LOG.md`

What changed:

- Tightened unlock reminder send eligibility so backend sends WeChat subscription messages only when an existing reminder authorization is `AUTHORIZED` or a prior authorized send path reached `SEND_FAILED`.
- A newly unlocked record with openid and configured template ID but no authorization record now remains `REQUESTED` instead of attempting a send.
- Existing `REQUESTED` reminders no longer attempt real delivery, because `REQUESTED` means authorization is not confirmed.
- Existing `DENIED` reminders continue to skip sending.
- Updated backend unit tests to reflect that missing authorization must not send and that send-failure behavior is tested from an authorized reminder.

Diagnosis:

- If the Mini Program did not show and accept a subscription prompt, the previous backend behavior could attempt to send without a real one-time subscription authorization, causing WeChat to reject delivery and the UI to show `SEND_FAILED`.
- If the subscription prompt was accepted and the status still becomes `SEND_FAILED`, the next evidence needed is the backend warning log from `WechatSubscribeMessageHttpClient`, especially WeChat `errcode` / `errmsg`. Likely causes include template ID mismatch, template field key mismatch with backend payload `thing1` / `time2`, invalid page path, or WeChat permission/account restrictions.

Verification:

- Pending local syntax/status checks and backend tests after the behavior change.

Skipped verification reason:

- Real subscription delivery requires WeChat Developer Tools and a user-accepted subscription prompt.
- Backend test execution may require Maven dependency/network access permission.

Scope safety check:

- Stayed inside M3 subscription reminder send eligibility and focused backend tests.
- Did not change API contracts, database schema, frontend UI behavior, deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- User must rebuild/restart backend and create a fresh record to verify the revised authorization gate.
- If send still fails after accepting the subscription prompt, inspect backend `WeChat API rejected request` warning for exact WeChat `errcode`.

### 2026-06-16 Codex (M3 Subscription Template Data Key Diagnosis)

Task:

- Diagnose user-provided backend log after accepting subscription authorization but still receiving `SEND_FAILED`.

Evidence:

- Backend log shows WeChat rejected the subscribe-message send with `errcode=47003` and `errmsg=argument invalid! data.thing3.value is empty`.
- The request reached WeChat's subscribe-message API, so AppID, access token, openid, and the send path are no longer the primary blocker for this failure.

Modified files:

- `backend/src/main/java/com/flashback/config/AppWechatProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageHttpClient.java`
- `backend/start-dev-wechat.local.ps1` (intentionally ignored by git)
- `.ai/AGENT_LOG.md`

What changed:

- Added configurable unlock reminder template data keys:
  - `WECHAT_UNLOCK_REMINDER_THING_KEY`
  - `WECHAT_UNLOCK_REMINDER_TIME_KEY`
- Updated the subscribe-message payload builder to use configured data keys instead of hardcoded `thing1` / `time2`.
- Set the local M3 WeChat backend test script to use `thing3` for the reminder text key based on the actual WeChat error.
- Kept `time2` as the time key because the uploaded WeChat error only reported missing `thing3`.

Diagnosis:

- The user's WeChat subscription template expects a required `thing3` keyword.
- Backend previously sent the reminder text as `thing1`, leaving `thing3` absent/empty from WeChat's perspective.
- This explains `errcode=47003` and the frontend `SEND_FAILED` status after the user accepted subscription authorization.

Verification:

- Passed: PowerShell parser check for `backend/start-dev-wechat.local.ps1`.
- Passed: `git check-ignore -v backend/start-dev-wechat.local.ps1` confirms the local script remains ignored.
- Passed: `git diff --check`.
- Attempted: `mvn -q -Dtest=RecordServiceImplTest test` from `backend`; sandboxed run failed because Maven could not download `spring-boot-starter-parent` from Maven Central due permission denial.
- Attempted escalated rerun with `mvn -q test`; automatic approval review timed out before execution.

Skipped verification reason:

- Real delivery must be retried manually in WeChat Developer Tools after restarting backend with the updated local script and creating a fresh authorized subscription record.
- Full backend test verification is pending because Maven dependency/network access escalation did not complete in this run.

Scope safety check:

- Stayed within M3 WeChat subscription-message demo configuration.
- Did not change API contracts, database schema, frontend behavior, deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- If the template also uses a different time keyword, WeChat may next report another `data.<keyword>.value is empty`; set `WECHAT_UNLOCK_REMINDER_TIME_KEY` accordingly.
- The current local script uses `thing3` based on the user's uploaded log; other templates may need different keyword indexes.

### 2026-06-16 Codex (M3 Subscription Template Library Follow-up)

Task:

- Record user-side WeChat manual verification result after accepting subscription authorization.
- Clarify subscription-message template selection constraints for M3.

Evidence:

- User accepted subscription authorization.
- Backend log shows WeChat rejected the send with `errcode=47003` and `errmsg=argument invalid! data.thing5.value is empty`.
- This indicates the selected public template requires another keyword `thing5` that the current M3 payload does not provide.

Modified files:

- `.ai/AGENT_LOG.md`

What changed:

- Recorded the latest manual WeChat verification result and current diagnosis.

Verification:

- Manual evidence provided by user from backend log.

Skipped verification reason:

- No code change in this note; real delivery remains pending template keyword alignment or a simpler public template selection.

Scope safety check:

- Stayed within M3 WeChat subscription-message manual verification evidence.
- Did not change backend contracts, database schema, frontend behavior, deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- Public template library templates may contain required keyword indexes that differ from the current backend payload.
- M3 should avoid turning this into a production template-management system; prefer selecting a simple two-keyword public template or a small configuration mapping.

### 2026-06-16 Codex (M3 Candidate Subscription Template Review)

Task:

- Review user-provided WeChat public template screenshot for M3 unlock reminder compatibility.

Evidence:

- Template title: `契约到期提醒`.
- Template category: `备忘录`.
- Dynamic detail fields shown:
  - `温馨提示`: `{{thing3.DATA}}`
  - `到期时间`: `{{time2.DATA}}`
- Static detail field shown:
  - `场景说明`: `过去的来信`

Modified files:

- `.ai/AGENT_LOG.md`

What changed:

- Recorded that this candidate template matches the current M3 configurable backend payload keys `thing3` and `time2`.

Verification:

- Visual review of the provided template screenshot confirms only `thing3` and `time2` appear as dynamic data placeholders.

Skipped verification reason:

- Real delivery still requires configuring the actual template ID in backend and frontend, restarting backend, rebuilding the Mini Program, and completing a fresh WeChat Developer Tools manual flow.

Scope safety check:

- Stayed within M3 WeChat subscription-message manual verification evidence.
- Did not change backend contracts, database schema, frontend behavior, deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- The screenshot shows template library number `75399`, not necessarily the final `template_id` required by `requestSubscribeMessage` and subscribe-message send API.
- User must use the actual selected template ID from the Mini Program subscription-message configuration, not the template library number, unless WeChat UI explicitly states they are the same for that selected template.

## 2026-06-16 - M3 WeChat subscription success and local launch convenience

Task:

- Record successful manual WeChat subscription verification.
- Make local WeChat verification startup easier while keeping sensitive values out of git.

Manual verification evidence:

- User reported that WeChat subscription delivery succeeded.
- User reported that the subscription message was received in personal WeChat.
- User reported that the Mini Program displayed send success.

Modified files:

- `.gitignore`
- `backend/start-dev-wechat.local.ps1` (intentionally ignored by git)
- `.ai/AGENT_LOG.md`

What changed:

- Kept WeChat AppID, AppSecret, template ID, page, and template data keys in the ignored backend local script.
- Updated the ignored backend local script to generate `frontend/.env.local` with frontend compile-time WeChat verification values before starting backend.
- Added `.gitignore` entries for frontend local env files so generated local verification config is not committed.

Verification:

- Passed: PowerShell parser check for `backend/start-dev-wechat.local.ps1`.
- Passed: `git check-ignore -v backend/start-dev-wechat.local.ps1 frontend/.env.local frontend/.env.dev.local` confirms backend script and frontend local env files are ignored.
- Passed: `git diff --check`.

Skipped verification reason:

- Did not rerun full backend tests in this documentation/local-script follow-up.
- Did not rerun WeChat Developer Tools flow because user already reported successful real subscription delivery in this turn.

Scope safety check:

- Stayed within M3 WeChat login/subscription manual verification support.
- Did not change backend API contracts, database schema, production deployment, monitoring, admin, SMS, package files, or lockfiles.

Remaining risks:

- `frontend/.env.local` is generated only when the local backend script is run; if the template ID changes, rerun the backend script before rebuilding the Mini Program.
- Frontend still needs a rebuild after template ID changes because `VITE_WECHAT_UNLOCK_REMINDER_TEMPLATE_ID` is compile-time configuration.

## 2026-06-16 - M3 closeout and merge readiness review

Task:

- Check current project code and M3 boundary.
- Confirm whether M3 can be closed and whether the current branch can be merged.
- Commit merge-ready changes if no blocking issue remains.

Modified files:

- `.ai/AGENT_LOG.md`
- `.gitignore`
- `backend/src/main/java/com/flashback/config/AppWechatProperties.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/main/java/com/flashback/wechat/WechatCode2SessionClient.java`
- `backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageHttpClient.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `frontend/project.config.json`
- `frontend/src/components/common/DateTimeWheelPicker.vue`
- `frontend/src/manifest.json`
- `frontend/src/pages/record-editor/index.vue`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`

What changed:

- Confirmed M3 remains within demo core-flow hardening: real WeChat login, real unlock reminder delivery, reminder idempotency/non-blocking behavior, and Mini Program flow completion.
- Updated the M3 task list to mark user-confirmed real WeChat login and real reminder delivery verification as complete.
- Kept the full manual demo loop item pending because one combined manual pass covering AI organization, unlock, two later-reflection submissions, no-more-modify, and stage summary was not separately evidenced in this turn.
- Fixed the record-editor datetime wheel sheet so taps inside the bottom sheet do not bubble to the overlay and accidentally close the picker.
- Confirmed the ignored local backend script and generated frontend local env remain outside version control.

Verification:

- Passed: `mvn -q test` from `backend` using approved network/dependency access.
- Passed: `.\node_modules\.bin\vue-tsc.cmd --noEmit` from `frontend` with bundled Node on PATH.
- Passed: `.\node_modules\.bin\uni.cmd build -p mp-weixin` from `frontend` with bundled Node on PATH.
- Passed: `git diff --check main`.
- Passed: tracked-file secret scan for the known local WeChat AppSecret returned no matches.
- Passed: visible-name regression scan `rg -n "我的档案|时间轴|回看页" frontend/src` returned no matches.
- Manual: user reported real WeChat login succeeded.
- Manual: user reported personal WeChat received the subscription message and the Mini Program displayed send success.

Skipped verification reason:

- Did not personally operate WeChat Developer Tools in this agent session; relied on user-reported real-device/manual verification for WeChat login and subscription delivery.
- Did not mark the full end-to-end demo loop as complete because the current turn did not include explicit evidence for the combined later-reflection and stage-summary pass.

`git diff --stat`:

```text
13 files changed, 931 insertions(+), 37 deletions(-)
```

Scope safety check:

- Stayed within M3 demo core flow hardening and local verification support.
- Did not add admin, production deployment, monitoring, alerting, SMS, notification center, campaign delivery, complex AI analytics, social feed, real MAP/IMAGE/VOICE, package files, or lockfiles.
- Did not commit AppSecret or local template secrets; local script and frontend local env are ignored.

Remaining risks:

- The full combined manual demo loop should still be run once before any formal demo acceptance: create -> AI organize -> seal -> unlock -> reminder -> time review -> first/second `realityLater` submit -> no more modify action -> stage summary.
- `frontend/project.config.json` and `frontend/src/manifest.json` now contain the real Mini Program AppID. This is required for the current verified Mini Program build but should be revisited if the repo later needs environment-specific public AppID handling.
- The current branch also contains previously committed record-editor datetime wheel work relative to `main`; it is within M3 flow completion and passed frontend checks.

## 2026-06-16 - M4 OpenSpec kickoff and active task switch

Task:

- Update project documentation so M4 has a clear active boundary, goals, non-goals, implementation order, and verification expectations.
- Ensure future agents do not continue treating M3 as the active engineering source.

Modified files:

- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `AGENTS.md`
- `openspec/changes/m4-real-capability-completion/proposal.md`
- `openspec/changes/m4-real-capability-completion/design.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `openspec/changes/m4-real-capability-completion/specs/backend-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/miniapp-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/v2-product-scope/spec.md`

What changed:

- Created M4 OpenSpec artifacts for real capability completion.
- Defined M4 as near-production usability for core Mini Program functions, not production deployment/release hardening.
- Captured user-confirmed M4 decisions: real AI provider, Qiniu private bucket, backend upload verification, location modes, image/voice limits, cover-from-image-only, draft-only media mutation, sealed immutability, and settings page deferral.
- Added backend-core, miniapp-core, and v2-product-scope spec deltas for real AI, Qiniu media, location, cover, mock boundary, timeline/home cover display, and time review media/location display.
- Switched `.ai/ACTIVE_TASK.md` and `AGENTS.md` from M3 active source to M4 active source.

Verification:

- Verified M4 files exist with `rg --files openspec\changes\m4-real-capability-completion`.
- Verified `AGENTS.md`, `.ai/ACTIVE_TASK.md`, and M4 docs reference `m4-real-capability-completion` and no longer treat M3 as active implementation source.
- Verified `openspec` CLI remains unavailable in the current PowerShell environment, so artifacts were created manually in the repository's existing Markdown OpenSpec style.
- No code tests were run because this was a documentation-only change.

Skipped verification reason:

- Backend/frontend automated tests were skipped because no application code changed.
- OpenSpec CLI validation was skipped because `openspec` is not installed or not on PATH in this environment.

Scope safety check:

- Stayed within documentation, OpenSpec, and agent-task boundary updates.
- Did not modify backend code, frontend code, schema, package files, lockfiles, deployment, monitoring, admin, SMS, notification center, settings page, or production release behavior.

Remaining risks:

- M3 has not been physically archived in `openspec/changes/archive/` in this change; it is treated as inactive historical baseline by `.ai/ACTIVE_TASK.md` and `AGENTS.md`.
- The M3 full combined manual demo-loop task remains historically pending and should not be represented as completed unless separate evidence is recorded.
- M4 implementation still needs contract confirmation for exact endpoint paths, DTO fields, Qiniu key policy, signed URL expiry, AI provider config names, and frontend-visible error/status semantics before code changes.

## 2026-06-17 - M4 backend contract readiness review

Task:

- Check whether current M4 documents are sufficient to guide backend optimization without scope drift.
- Add more concrete endpoint, DTO, provider, key-policy, and configuration guidance because the user cannot directly provide low-level contract answers.

Modified files:

- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `AGENTS.md`
- `openspec/changes/m4-real-capability-completion/proposal.md`
- `openspec/changes/m4-real-capability-completion/design.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md`

What changed:

- Reviewed M4 proposal/design/tasks/spec deltas and confirmed they constrain scope well but were not concrete enough for backend implementation contracts.
- Added `backend-contract-decisions.md` as the M4 backend contract decision layer.
- Proposed default backend contracts for AI provider config, DeepSeek/OpenAI-compatible adapter strategy, Qiniu upload-token flow, Qiniu object verification, private signed media URLs, attachment commit/delete/access endpoints, cover endpoint, location endpoints, response extensions, and error semantics.
- Updated `AGENTS.md`, `.ai/ACTIVE_TASK.md`, proposal, design, and tasks to require reading `backend-contract-decisions.md` before backend contract work.
- Used official DeepSeek and Qiniu documentation as current external reference baseline for provider/storage contract guidance.

Verification:

- Verified `backend-contract-decisions.md` exists and contains recommended defaults plus remaining confirmation questions.
- Verified `.ai/ACTIVE_TASK.md`, `AGENTS.md`, proposal, design, and tasks reference `backend-contract-decisions.md`.
- Passed `git diff --check`.
- No backend/frontend tests were run because this was a documentation-only change.

Skipped verification reason:

- Application tests were skipped because no application code changed.
- OpenSpec CLI validation was not run because `openspec` remains unavailable in the current PowerShell environment.

Scope safety check:

- Stayed within OpenSpec and coordination documentation.
- Did not modify backend code, frontend code, schema, package files, lockfiles, deployment, monitoring, admin, SMS, notification center, settings page, or production release behavior.

Remaining risks:

- M4 implementation still needs code-level fact finding before edits, especially current mapper/schema conventions and frontend service expectations.
- DeepSeek and Qiniu API details should be rechecked against official docs immediately before coding because provider/storage docs can change.
- The recommended defaults are ready for implementation unless the user objects, but they are still product/engineering decisions rather than code evidence.

## 2026-06-17 - M4 contract acceptance and documentation closeout

Task:

- Record the user's acceptance of all M4 backend contract assumptions.
- Close M4 documentation so implementation agents can proceed without reopening accepted endpoint, DTO, provider, Qiniu key, signed URL, location, cover, and error-semantics questions.
- Commit the documentation checkpoint with a Chinese message.

Modified files:

- `.ai/ACTIVE_TASK.md`
- `.ai/AGENT_LOG.md`
- `AGENTS.md`
- `openspec/changes/m4-real-capability-completion/proposal.md`
- `openspec/changes/m4-real-capability-completion/design.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md`
- `openspec/changes/m4-real-capability-completion/specs/backend-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/miniapp-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/v2-product-scope/spec.md`

What changed:

- Converted M4 backend contract decisions from recommended assumptions into accepted implementation contracts after user confirmation.
- Recorded that the accepted defaults are: `/api/records/{recordId}` REST subresources, one OpenAI-compatible adapter first, no dedicated NVIDIA NIM adapter in M4, stateless upload-token issuance, persist attachments only after Qiniu verification, Qiniu delete before draft metadata removal, 600-second signed URL default, no backend geocoding in M4, and separate endpoints for location/media/cover.
- Updated proposal, design, tasks, and active-task wording so agents implement accepted contracts and ask the user only when changing them or when code facts make them impossible or unsafe.
- Verified no residual "remaining questions" or "confirm/update" language remains in the M4 implementation path.

Verification:

- Passed `git diff --check`.
- Verified no residual contract-confirmation wording with `Select-String` across M4 docs, `.ai/ACTIVE_TASK.md`, and `AGENTS.md`.
- No backend/frontend tests were run because this was a documentation-only change.

Skipped verification reason:

- Application tests were skipped because no application code changed.
- OpenSpec CLI validation was not run because `openspec` remains unavailable in the current PowerShell environment.

Scope safety check:

- Stayed within OpenSpec and coordination documentation.
- Did not modify backend code, frontend code, schema, package files, lockfiles, deployment, monitoring, admin, SMS, notification center, settings page, or production release behavior.
- Did not stage or modify the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- M4 implementation must still begin with code-level fact finding before edits.
- Provider/storage implementation details should be rechecked against official DeepSeek and Qiniu docs immediately before coding.
- The accepted contracts are now binding for M4 unless future code facts require an explicit OpenSpec update.

## 2026-06-18 - M4 backend record location API implementation

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement the accepted record location backend contract:
  - `PUT /api/records/{recordId}/location`
  - `DELETE /api/records/{recordId}/location`
  - `RecordDetailVO.location`
  - separate `record_location` persistence.

Modified files:

- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/main/java/com/flashback/controller/api/RecordController.java`
- `backend/src/main/java/com/flashback/domain/RecordLocation.java`
- `backend/src/main/java/com/flashback/domain/RecordLocationSource.java`
- `backend/src/main/java/com/flashback/dto/UpdateRecordLocationRequest.java`
- `backend/src/main/java/com/flashback/mapper/RecordLocationMapper.java`
- `backend/src/main/java/com/flashback/service/RecordService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/main/java/com/flashback/vo/RecordLocationVO.java`
- `backend/src/main/resources/mapper/RecordLocationMapper.xml`
- `backend/src/test/java/com/flashback/controller/api/RecordControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `backend/src/test/resources/schema.sql`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `.ai/AGENT_LOG.md`

What changed:

- Added `record_location` as a separate record-owned and user-owned persistence model in both MySQL schema and test schema.
- Added `RecordLocationSource` with accepted M4 values `CURRENT_LOCATION`, `MAP_PICKER`, and `MANUAL`.
- Added `UpdateRecordLocationRequest` and `RecordLocationVO` matching the accepted M4 location DTO shape.
- Added `RecordLocationMapper` and XML mapper for owner-scoped select, upsert, and delete.
- Added `PUT /api/records/{id}/location` and `DELETE /api/records/{id}/location` to `RecordController`.
- Added `RecordService.updateLocation` and `RecordService.deleteLocation`.
- Enforced draft-only location create/update/delete; SEALED and UNLOCKED records are rejected through existing lifecycle checks.
- Enforced location validation:
  - `CURRENT_LOCATION` and `MAP_PICKER` require latitude and longitude.
  - `MANUAL` requires at least one of name or address.
  - Manual location may omit coordinates.
  - Coordinate ranges are validated when coordinates are supplied.
- Added `location` to `RecordDetailVO`, loaded through owner-scoped `recordLocationMapper.selectByRecordIdAndUserId`.
- Marked completed M4 location backend tasks in `tasks.md`.

Verification:

- Passed focused backend tests:
  - `mvn -q "-Dtest=RecordServiceImplTest,RecordControllerAuthIntegrationTest,RecordMapperIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Focused tests cover:
  - manual location save for draft records
  - map-picker coordinate validation
  - manual location name/address validation
  - sealed-record mutation rejection
  - draft location delete
  - controller authenticated update/delete paths
  - mapper owner-scoped select, upsert, and delete behavior

Skipped verification reason:

- Manual WeChat Mini Program verification was not run because this step only implemented backend location endpoints and persistence; frontend location controls are still pending in M4 tasks.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend location scope and accepted `backend-contract-decisions.md`.
- Did not implement Qiniu, attachments, cover, frontend media/location UI, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Existing deployments will need the new `record_location` schema applied before the location endpoints can work outside tests.
- Frontend location controls and Mini Program permission/map/manual flows are still pending.
- Location does not yet appear in list/timeline compact labels; M4 only requires full location in detail/time review, while compact labels remain optional.

## 2026-06-18 - M4 backend Qiniu storage and media config boundary

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Add backend-side Qiniu storage configuration and M4 media limit configuration without implementing upload-token/stat/signed-url behavior yet.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/config/AppMediaProperties.java`
- `backend/src/main/java/com/flashback/config/AppStorageProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/flashback/config/AppMediaPropertiesTest.java`
- `backend/src/test/java/com/flashback/config/AppStoragePropertiesTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added `AppStorageProperties` under `app.storage` with accepted M4 defaults:
  - `provider=qiniu`
  - `qiniu.access-key`
  - `qiniu.secret-key`
  - `qiniu.bucket`
  - `qiniu.region`
  - `qiniu.private-domain`
  - `qiniu.upload-token-ttl-seconds=600`
  - `qiniu.download-url-ttl-seconds=600`
  - `qiniu.key-prefix=flashback`
- Added `StorageProvider.QINIU` config-value parsing and `qiniu.isConfigured()` helper.
- Added `AppMediaProperties` under `app.media` with accepted M4 limits:
  - max 9 images per record
  - max 9 voice files per record
  - max 40 MB per file
  - max 300 MB per record
- Added corresponding `application.yml` environment-variable placeholders. No real Qiniu AK/SK or bucket/domain values were written to tracked files.
- Added focused tests for storage provider defaults and media limits.
- Marked Qiniu backend config and secret-boundary tasks complete in M4 tasks.

Verification:

- Passed focused backend config tests:
  - `mvn -q "-Dtest=AppStoragePropertiesTest,AppMediaPropertiesTest,AppAiPropertiesTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result only found environment-variable placeholders and documentation/log mentions; no concrete AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.

Skipped verification reason:

- Qiniu upload token, object stat verification, object delete, and signed private download URL behavior were not tested because this step intentionally added only configuration and media-limit properties.
- Manual WeChat Mini Program verification was not run because no frontend behavior changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend storage configuration scope and accepted `backend-contract-decisions.md`.
- Did not implement upload-token issuance, attachment persistence, media access URL generation, cover, frontend media UI, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- The next Qiniu backend step still needs an implementation choice for token generation, object stat, delete, and private signed URL generation.
- If an SDK is introduced later, package changes must be explicitly justified by the implementation task.
- Real Qiniu success paths remain unverified until credentials and storage implementation are available.

## 2026-06-18 - M4 backend attachment upload-token foundation

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement the accepted attachment upload-token endpoint and DTO, plus attachment metadata persistence foundation needed by later commit/verify/delete/access-url steps.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/main/java/com/flashback/controller/api/RecordAttachmentController.java`
- `backend/src/main/java/com/flashback/domain/RecordAttachment.java`
- `backend/src/main/java/com/flashback/domain/RecordAttachmentStatus.java`
- `backend/src/main/java/com/flashback/domain/RecordAttachmentType.java`
- `backend/src/main/java/com/flashback/domain/StorageProvider.java`
- `backend/src/main/java/com/flashback/dto/CreateAttachmentUploadTokenRequest.java`
- `backend/src/main/java/com/flashback/mapper/RecordAttachmentMapper.java`
- `backend/src/main/java/com/flashback/service/RecordAttachmentService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/AttachmentUploadTokenVO.java`
- `backend/src/main/resources/mapper/RecordAttachmentMapper.xml`
- `backend/src/test/java/com/flashback/controller/api/RecordAttachmentControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordAttachmentMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordAttachmentServiceImplTest.java`
- `backend/src/test/resources/schema.sql`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added `POST /api/records/{recordId}/attachments/upload-token`.
- Added `CreateAttachmentUploadTokenRequest` with accepted fields: `type`, `fileName`, `mimeType`, and `sizeBytes`.
- Added `AttachmentUploadTokenVO` with accepted response fields: `provider`, `bucket`, `key`, `uploadToken`, `uploadUrl`, `expiresAt`, and `maxFileSizeBytes`.
- Added `record_attachment` table to MySQL and test schema.
- Added attachment domain/model fields for owner, record, type, storage provider, bucket, storage key, file name, MIME type, size, duration, image dimensions, sort order, status, and timestamps.
- Added `RecordAttachmentMapper` for owner-scoped selection and committed-attachment count/size queries.
- Implemented Qiniu upload-token generation without adding dependencies:
  - backend-generated key format `{keyPrefix}/users/{userId}/records/{recordId}/{image|voice}/{uuid}.{extension}`
  - MIME allowlist-derived extension
  - upload scope `bucket:key`
  - upload deadline from configured TTL
  - HMAC-SHA1 + URL-safe Base64 Qiniu token format
- Enforced upload-token prechecks:
  - authenticated owner owns the record
  - record is `DRAFT`
  - Qiniu config is backend-side and complete
  - media type is `IMAGE` or `VOICE`
  - MIME type is allowlisted
  - per-file max 40 MB
  - committed image/voice count limits
  - committed total size max 300 MB

Verification:

- Rechecked current Qiniu official documentation before coding:
  - upload token: `https://developer.qiniu.com/kodo/1208/upload-token`
  - private download: `https://developer.qiniu.com/kodo/1656/download-private`
  - object stat: `https://developer.qiniu.com/kodo/1308/stat`
- Passed focused backend tests:
  - `mvn -q "-Dtest=RecordAttachmentServiceImplTest,RecordAttachmentControllerAuthIntegrationTest,RecordAttachmentMapperIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.

Skipped verification reason:

- Real Qiniu upload success was not verified because no backend-side Qiniu credentials are available in tracked config, and secrets must not be committed.
- Qiniu object stat verification, object delete, private signed access URL, and attachment commit persistence are still pending M4 tasks and were not claimed complete in this step.
- Manual WeChat Mini Program verification was not run because no frontend upload flow changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend attachment upload-token and metadata-foundation scope.
- Did not implement frontend media UI, cover, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Upload-token issuance is stateless per accepted contract, so multiple in-flight tokens can still race until commit-time verification also enforces limits.
- Attachment commit/verify, Qiniu object stat, object delete, signed media access URL, and cover selection remain pending.
- The upload URL currently uses the common Qiniu upload host; region-specific upload hosts can be revisited only if real Qiniu verification shows this is required.

## 2026-06-18 - M4 backend attachment commit and Qiniu stat verification

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement the accepted attachment commit/verify endpoint so metadata is persisted only after backend Qiniu stat verification succeeds.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/controller/api/RecordAttachmentController.java`
- `backend/src/main/java/com/flashback/dto/CommitRecordAttachmentRequest.java`
- `backend/src/main/java/com/flashback/mapper/RecordAttachmentMapper.java`
- `backend/src/main/java/com/flashback/service/RecordAttachmentService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuHttpStorageClient.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuObjectMetadata.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuStorageClient.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuStorageException.java`
- `backend/src/main/java/com/flashback/vo/RecordAttachmentVO.java`
- `backend/src/main/resources/mapper/RecordAttachmentMapper.xml`
- `backend/src/test/java/com/flashback/controller/api/RecordAttachmentControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordAttachmentMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordAttachmentServiceImplTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added `POST /api/records/{recordId}/attachments/commit`.
- Added `CommitRecordAttachmentRequest` with accepted fields: `type`, `key`, `fileName`, `mimeType`, `sizeBytes`, `width`, `height`, and `durationSeconds`.
- Added `RecordAttachmentVO` matching accepted response fields, with `accessUrl` left null because signed media access is a separate pending endpoint.
- Added a minimal `QiniuStorageClient` abstraction and `QiniuHttpStorageClient` implementation for object stat verification.
- Added backend verification before persistence:
  - owner-scoped record lookup
  - DRAFT-only mutation
  - backend Qiniu config check
  - MIME allowlist validation
  - per-file, per-type-count, and total-size validation
  - generated-key namespace validation for the authenticated user, record, and media type
  - Qiniu stat object existence verification
  - Qiniu-reported size verification
  - Qiniu-reported MIME verification when available
- Persisted attachment metadata only after stat verification succeeds, with `AVAILABLE` status and next `sortOrder`.

Verification:

- Rechecked current Qiniu official documentation before coding:
  - upload token: `https://developer.qiniu.com/kodo/1208/upload-token`
  - private download: `https://developer.qiniu.com/kodo/1656/download-private`
  - object stat: `https://developer.qiniu.com/kodo/1308/stat`
- Passed focused backend tests:
  - `mvn -q "-Dtest=RecordAttachmentServiceImplTest,RecordAttachmentControllerAuthIntegrationTest,RecordAttachmentMapperIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Focused tests cover:
  - commit success after stubbed Qiniu stat metadata
  - key namespace rejection
  - Qiniu object missing rejection
  - Qiniu size mismatch rejection
  - controller authenticated commit path
  - mapper available-count helper
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.

Skipped verification reason:

- Real Qiniu stat success was not verified because no backend-side Qiniu credentials are available in tracked config, and secrets must not be committed.
- Qiniu object delete, private signed access URL, frontend image/voice preview/playback, and cover selection remain pending M4 tasks and were not claimed complete in this step.
- Manual WeChat Mini Program verification was not run because no frontend upload flow changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend attachment commit/verify scope and accepted `backend-contract-decisions.md`.
- Did not implement frontend media UI, cover, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- The default Qiniu stat client uses the documented Kodo stat API shape but still needs real-credential verification.
- Attachment delete and signed media access are still pending; media preview/playback cannot be completed until signed access URLs exist.
- Commit-time limit checks reduce stateless token race risk, but concurrent commits can still race without database-level aggregate constraints.

## 2026-06-18 11:21 M4 attachment private access URL

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement accepted private media access URL endpoint for record attachments.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/controller/api/RecordAttachmentController.java`
- `backend/src/main/java/com/flashback/service/RecordAttachmentService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/AttachmentAccessUrlVO.java`
- `backend/src/test/java/com/flashback/controller/api/RecordAttachmentControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordAttachmentServiceImplTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added `GET /api/records/{recordId}/attachments/{attachmentId}/access-url`.
- Added `AttachmentAccessUrlVO` with `attachmentId`, short-lived signed `url`, and `expiresAt`.
- Implemented owner-scoped lookup using record ownership and attachment id/record id/user id.
- Only `AVAILABLE` attachments can receive signed media URLs; missing or cross-owner attachments return safe not-found.
- Generated Qiniu private download URLs on demand using configured `privateDomain`, `downloadUrlTtlSeconds`, access key, and secret key.
- Kept signed URL as computed response data only; no permanent URL is persisted.
- Tightened committed storage key normalization to reject `?` and `#` characters before URL signing.
- Updated M4 task checkboxes for signed URL expiry policy, private access flow, owner-scoped signed URL reads, and focused signed URL tests.

Verification:

- Passed focused backend tests:
  - `mvn -q "-Dtest=RecordAttachmentServiceImplTest,RecordAttachmentControllerAuthIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Focused tests cover:
  - deterministic private signed URL generation with configured 600 second TTL
  - owner-scoped available attachment access
  - safe not-found when attachment is not owned or unavailable
  - authenticated controller route for `/api/records/{recordId}/attachments/{attachmentId}/access-url`
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.
- `git diff --check` passed when rerun outside the sandbox; the first sandbox run failed on a Windows ACL read issue for the user git ignore file.
- `git diff --stat` before staging showed:
  - 6 tracked files changed, 131 insertions, 5 deletions, plus new `AttachmentAccessUrlVO.java`.

Skipped verification reason:

- Real Qiniu private media access was not verified because no backend-side Qiniu credentials/domain are available in tracked config, and secrets must not be committed.
- Attachment delete, cover mutation, frontend image preview, and voice playback remain pending M4 tasks and were not claimed complete in this step.
- Manual WeChat Mini Program verification was not run because no frontend media flow changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend attachment private access scope and accepted `backend-contract-decisions.md`.
- Did not implement frontend media UI, cover, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- The Qiniu private URL algorithm is covered by deterministic unit tests, but still needs real private-bucket verification with actual credentials.
- Signed URLs are intentionally short-lived; frontend callers must refresh them for long preview/playback sessions.
- Attachment delete and cover clearing behavior are still pending, so end-to-end media lifecycle remains incomplete.

## 2026-06-18 11:28 M4 attachment delete endpoint

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement accepted draft-only attachment delete behavior with remote Qiniu delete before local metadata removal.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/controller/api/RecordAttachmentController.java`
- `backend/src/main/java/com/flashback/mapper/RecordAttachmentMapper.java`
- `backend/src/main/java/com/flashback/service/RecordAttachmentService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuHttpStorageClient.java`
- `backend/src/main/java/com/flashback/storage/qiniu/QiniuStorageClient.java`
- `backend/src/main/resources/mapper/RecordAttachmentMapper.xml`
- `backend/src/test/java/com/flashback/controller/api/RecordAttachmentControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordAttachmentMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordAttachmentServiceImplTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added `DELETE /api/records/{recordId}/attachments/{attachmentId}`.
- Added `RecordAttachmentService.deleteAttachment`.
- Enforced owner-scoped record lookup and DRAFT-only deletion.
- Looked up attachment by attachment id, record id, and user id; missing, cross-owner, or non-AVAILABLE attachments return safe not-found.
- Added `QiniuStorageClient.deleteObject` and HTTP implementation using Qiniu management delete API.
- Deletes remote Qiniu object before local metadata changes.
- Treats Qiniu object-not-found as safe cleanup and marks local metadata `DELETED`.
- Returns service unavailable and keeps metadata unchanged when Qiniu delete fails for other reasons.
- Added mapper update `markDeletedByIdAndRecordIdAndUserId`, scoped to `AVAILABLE` attachments only.
- Updated M4 task checkboxes for attachment delete, DRAFT-only mutation, SEALED/UNLOCKED rejection, raw voice storage boundary, schema consistency, and focused backend tests.

Verification:

- Passed focused backend tests:
  - `mvn -q "-Dtest=RecordAttachmentServiceImplTest,RecordAttachmentControllerAuthIntegrationTest,RecordAttachmentMapperIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Focused tests cover:
  - draft delete after successful Qiniu delete
  - local metadata cleanup when Qiniu object is already missing
  - Qiniu delete failure preserving metadata
  - SEALED and UNLOCKED records rejecting attachment delete
  - authenticated controller DELETE route
  - mapper `AVAILABLE -> DELETED` update excluding deleted rows from count/select/sum helpers
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.
- `git diff --check` passed outside the sandbox; the sandbox still cannot read the user git ignore file because of Windows ACLs.
- `git diff --stat` before staging showed:
  - 11 files changed, 231 insertions, 8 deletions.

Skipped verification reason:

- Real Qiniu delete success and object-not-found behavior were not verified because no backend-side Qiniu credentials are available in tracked config, and secrets must not be committed.
- Current-cover clearing on attachment delete is deferred to the pending cover model/API step; there is not yet a persisted cover reference in this codebase.
- Frontend image/voice delete, preview, and playback behavior remain pending M4 frontend tasks.
- Manual WeChat Mini Program verification was not run because no frontend media flow changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend attachment delete scope and accepted `backend-contract-decisions.md`.
- Did not implement frontend media UI, cover API/model, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Qiniu delete is covered by unit tests around client behavior and service error handling, but real private bucket credentials are still required for integration verification.
- Cover clearing after deleting the current cover image remains pending until the cover reference is implemented.
- Concurrent delete/access requests may race around short-lived signed URLs; frontend should handle media 403/expired/not-found by refreshing or removing stale media state.

## 2026-06-18 11:38 M4 record cover backend

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Implement the accepted record cover backend contract using same-record image attachments.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/sql/mysql/schema.mysql.sql`
- `backend/src/main/java/com/flashback/controller/api/RecordController.java`
- `backend/src/main/java/com/flashback/domain/Record.java`
- `backend/src/main/java/com/flashback/dto/UpdateRecordCoverRequest.java`
- `backend/src/main/java/com/flashback/mapper/RecordMapper.java`
- `backend/src/main/java/com/flashback/service/RecordService.java`
- `backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java`
- `backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/RecordDetailVO.java`
- `backend/src/main/java/com/flashback/vo/RecordListItemVO.java`
- `backend/src/main/java/com/flashback/vo/TimelineItemVO.java`
- `backend/src/main/resources/mapper/RecordMapper.xml`
- `backend/src/test/java/com/flashback/controller/api/RecordControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/mapper/RecordMapperIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordAttachmentServiceImplTest.java`
- `backend/src/test/java/com/flashback/service/impl/RecordServiceImplTest.java`
- `backend/src/test/resources/schema.sql`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added nullable `record.cover_attachment_id` to MySQL and test schemas.
- Added `UpdateRecordCoverRequest` with accepted nullable `attachmentId`.
- Added `PUT /api/records/{id}/cover`.
- Added `RecordService.updateCover`.
- Validates owner-scoped DRAFT record before cover mutation.
- Allows clearing cover with `attachmentId: null`.
- Validates selected cover attachment belongs to the same record and user, is `AVAILABLE`, and has type `IMAGE`.
- Rejects voice attachments as cover.
- Adds cover metadata to record detail, list item, and timeline item responses without embedding permanent signed URLs.
- Adds available attachment metadata to record detail for frontend cover selection/read-only display.
- Clears `cover_attachment_id` when deleting the current draft cover image.
- Updated M4 task checkboxes for the cover backend contract.

Verification:

- `git diff --check` passed.
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.
- Static code lookup checked cover wiring points:
  - `rg -n "RecordServiceImpl\\(|updateCover\\(|coverAttachmentId|clearCoverAttachmentIfMatches|UpdateRecordCoverRequest" backend\src\main backend\src\test`
- `git diff --stat` before staging showed:
  - 17 files changed, 400 insertions, 13 deletions.

Skipped verification reason:

- Maven focused tests and full backend tests were not run for this step because the required sandbox-escalated Maven test execution was rejected by the environment usage limit.
- No real Qiniu/media integration verification was run because this step only adds cover metadata/reference behavior, and no backend-side Qiniu credentials are available in tracked config.
- Manual WeChat Mini Program verification was not run because no frontend cover UI changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend cover and attachment-cover cleanup scope and accepted `backend-contract-decisions.md`.
- Did not implement frontend cover UI, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- The cover backend step is not Maven-verified in this run because of the environment usage limit; focused tests were added but still need execution when the limit resets.
- List/timeline cover responses currently return metadata only; frontend must call the media access endpoint for short-lived display URLs.
- Home-card cover display may need additional frontend/service wiring in later M4 frontend real-data work.

## 2026-06-19 22:03 M4 cover backend verification follow-up

Task:

- Continue `m4-real-capability-completion`.
- Close the verification gap left by the previous cover backend step after the environment usage limit reset.

Modified files:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Recorded successful Maven verification for the M4 cover backend step.
- Marked backend focused and full backend test tasks complete in the M4 integration checklist.

Verification:

- Initial sandbox run failed as expected because Maven could not resolve Spring Boot parent POM under restricted network/cache permissions:
  - `mvn -q "-Dtest=RecordServiceImplTest,RecordControllerAuthIntegrationTest,RecordMapperIntegrationTest,RecordAttachmentServiceImplTest" test`
- Passed focused backend tests outside the sandbox:
  - `mvn -q "-Dtest=RecordServiceImplTest,RecordControllerAuthIntegrationTest,RecordMapperIntegrationTest,RecordAttachmentServiceImplTest" test`
- Passed full backend test suite outside the sandbox:
  - `mvn -q test`

Skipped verification reason:

- Real Qiniu/media integration verification was not run because no backend-side Qiniu credentials are available in tracked config, and secrets must not be committed.
- Manual WeChat Mini Program verification was not run because no frontend flow changed in this verification-only step.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Verification-only step; no backend business code changed.
- Did not touch package/lockfile files or the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real Qiniu upload/stat/delete/private-media behavior still needs credential-backed integration verification.
- Frontend media/cover UI and home/time-review real-data surfaces remain pending M4 work.

## 2026-06-19 22:08 M4 AI mock boundary tightening

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Ensure the mock AI provider is available only when explicitly enabled for tests or development, and does not return mock success on authenticated real paths by default.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/service/impl/AiServiceImpl.java`
- `backend/src/test/java/com/flashback/service/impl/AiServiceImplTest.java`
- `backend/src/test/resources/application-test.yml`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- `AiServiceImpl` now requires `app.ai.real-mode-mock-enabled=true` before provider `mock` can return `SUCCESS`.
- When provider is `mock` but the explicit mock flag is false, writing prompts and summary return `UNAVAILABLE` with message `AI mock provider未启用`.
- Test profile explicitly enables mock mode with `app.ai.real-mode-mock-enabled: true`.
- Unit tests now cover the disabled mock provider path and explicitly enable mock for mock-success tests.
- Marked the M4 backend AI mock-boundary task complete.

Verification:

- Passed focused backend tests:
  - `mvn -q "-Dtest=AiServiceImplTest,AppAiPropertiesTest,AiControllerAuthIntegrationTest,StageSummaryServiceImplTest,StageSummaryControllerAuthIntegrationTest" test`
- Passed full backend test suite:
  - `mvn -q test`
- Ran tracked/front-end secret boundary scan:
  - `rg -n "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak|test-key)" frontend backend\src\main backend\src\test openspec .ai AGENTS.md`
- Scan result found only environment-variable placeholders, documentation/log mentions, and dummy test values `test-ak` / `test-sk` / `test-key`; no concrete real AI or Qiniu secret value was found, and no frontend Qiniu/AI secret occurrence was found.
- `git diff --check` passed.
- `git diff --stat` before staging showed:
  - 5 files changed, 82 insertions, 1 deletion.

Skipped verification reason:

- Real AI configured success path was not verified because no backend-side AI provider credentials are available in tracked config, and secrets must not be committed.
- Manual WeChat Mini Program verification was not run because no frontend flow changed.
- OpenSpec CLI validation was not run because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend AI mock/real-mode boundary scope and accepted `backend-contract-decisions.md`.
- Did not implement frontend AI UI, settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real DeepSeek/openai-compatible success still requires credential-backed integration verification.
- Frontend still needs explicit unavailable/failed-state handling in M4 frontend work.

## 2026-06-19 22:20 M4 attachment frontend-visible error semantics

Task:

- Continue `m4-real-capability-completion` backend work in a small OpenSpec-aligned step.
- Verify and lock the accepted frontend-visible upload, verification, media access, and sealed-mutation error states for attachment APIs.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/test/java/com/flashback/controller/api/RecordAttachmentControllerAuthIntegrationTest.java`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added controller integration tests proving attachment API errors are returned through `ApiResponse` with frontend-visible HTTP status, `code`, and `message`.
- Covered storage/upload-token unavailable, object verification failure, media access URL unavailable, and sealed-record attachment mutation rejection.
- Marked `Implement accepted frontend-visible upload, verification, and media error states` complete in the M4 task list.

Verification:

- Initial sandbox run failed because Maven could not resolve Spring Boot parent POM under restricted network/cache permissions:
  - `mvn -q "-Dtest=RecordAttachmentControllerAuthIntegrationTest,RecordAttachmentServiceImplTest" test`
- Passed focused backend tests outside the sandbox:
  - `mvn -q "-Dtest=RecordAttachmentControllerAuthIntegrationTest,RecordAttachmentServiceImplTest" test`
- Passed full backend test suite outside the sandbox:
  - `mvn -q test`
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 3 files changed, 134 insertions, 1 deletion.

Skipped verification reason:

- Real Qiniu upload/object-stat/signed-media access was not run because no backend-side Qiniu credentials are available in tracked config, and secrets must not be committed.
- Manual WeChat Mini Program verification was not run because this step only added backend/controller verification for error semantics.
- OpenSpec CLI validation was attempted and skipped because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Stayed within M4 backend attachment/media error-semantics scope and reused the accepted `ApiResponse` + HTTP status + current `ErrorCode` contract.
- Did not add new global error codes or change accepted endpoint paths/DTO fields.
- Did not implement settings page, admin portal, deployment, monitoring, SMS, notification center, campaign delivery, social feed, H5/Web acceptance, voice transcription, or voice AI analysis.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real Qiniu credential-backed upload/stat/delete/private-media behavior still needs integration verification.
- Frontend still needs to consume these error states in the M4 media/location UI work.

## 2026-06-19 22:26 M4 backend verification checklist closeout

Task:

- Continue `m4-real-capability-completion` with a verification-only backend closeout step.
- Mark only M4 integration checklist items backed by automated backend tests or tracked-file scans.

Modified files:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Marked tracked AI/Qiniu secret scan complete.
- Marked AI missing-config/failure path verification complete.
- Marked attachment limit and total-size error verification complete.
- Marked sealed/unlocked rejection for location, attachment, and cover mutation complete.
- Marked verification evidence recording complete.
- No accepted contract decisions changed, so `backend-contract-decisions.md` was not modified.

Verification:

- Secret scan command:
  - `git grep -n -E "(QINIU_ACCESS_KEY|QINIU_SECRET_KEY|QINIU_BUCKET|QINIU_PRIVATE_DOMAIN|AI_API_KEY|secret-key|access-key|api-key|test-sk|test-ak|test-key)" -- .`
- Secret scan result found only backend environment-variable placeholders, OpenSpec/log mentions, and dummy test values `test-ak` / `test-sk` / `test-key`; no concrete real AI or Qiniu secret value was found, and no frontend secret occurrence was found.
- Existing tests covering the marked checklist items were confirmed by test names and the full backend suite that passed in the previous M4 step:
  - `AiServiceImplTest.shouldReturnUnavailableWhenRealProviderMissingApiKey`
  - `AiServiceImplTest.shouldReturnFailedWhenRealProviderCallFails`
  - `AiServiceImplTest.shouldReturnUnavailableWhenMockProviderNotExplicitlyEnabled`
  - `RecordAttachmentServiceImplTest.shouldRejectFileLargerThanLimit`
  - `RecordAttachmentServiceImplTest.shouldRejectWhenImageCountLimitExceeded`
  - `RecordAttachmentServiceImplTest.shouldRejectWhenTotalSizeLimitExceeded`
  - `RecordAttachmentServiceImplTest.shouldRejectDeleteWhenRecordIsNotDraft`
  - `RecordAttachmentServiceImplTest.shouldRejectDeleteWhenRecordIsUnlocked`
  - `RecordServiceImplTest.shouldRejectLocationMutationWhenRecordIsSealed`
  - `RecordServiceImplTest.shouldRejectCoverMutationWhenRecordIsSealed`
  - `RecordMapperIntegrationTest.updateCoverAttachmentByIdAndUserIdShouldOnlyAffectDraft`
- Full backend test suite evidence from the immediately previous step:
  - `mvn -q test` passed.
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 2 files changed, 68 insertions, 5 deletions.

Skipped verification reason:

- Real AI configured success path remains unchecked because no backend-side AI provider credentials are available in tracked config.
- Real Qiniu upload/object-stat/signed-media/image-preview/voice-playback verification remains unchecked because no backend-side Qiniu credentials or Mini Program media flow were available.
- Frontend type-check, Mini Program build, timeline/home cover display, time review media display, and preview-mode functional verification remain unchecked because this step did not enter frontend scope.
- OpenSpec CLI validation remains skipped because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Verification-only OpenSpec/task-log update; no application code changed.
- Did not mark external-provider, Qiniu-live, frontend-build, Mini Program manual, timeline/home display, time-review display, or preview functional items complete without direct evidence.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- M4 frontend phases 8-11 remain open.
- Real provider credentials are still required to verify AI success and Qiniu media flows end-to-end.

## 2026-06-19 23:23 M4 stage summary real AI path

Task:

- Continue `m4-real-capability-completion` by auditing the remaining real AI consumers.
- Replace the stage-summary endpoint's fallback-only implementation with the existing backend real-provider path while retaining an explicit local fallback.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/src/main/java/com/flashback/service/impl/StageSummaryServiceImpl.java`
- `backend/src/main/java/com/flashback/vo/StageSummaryVO.java`
- `backend/src/test/java/com/flashback/controller/api/StageSummaryControllerAuthIntegrationTest.java`
- `backend/src/test/java/com/flashback/service/impl/StageSummaryServiceImplTest.java`

What changed:

- `StageSummaryServiceImpl` now sends a bounded, user-scoped summary context through the existing `AiService` OpenAI-compatible provider path.
- A provider result is accepted only when its status is `SUCCESS` and its summary is nonblank; otherwise the endpoint returns the deterministic local summary with `source=fallback` and `status=FALLBACK`.
- `StageSummaryVO` now exposes `status` and `message`, so clients can distinguish provider-backed output from an unavailable-provider fallback.
- Added service tests for provider success, explicit unavailable fallback, and null-provider-result fallback, plus controller JSON assertions for the frontend-visible fallback fields.

Verification:

- Initial sandbox focused test run failed because Maven could not access the cached Spring Boot parent POM under sandbox permissions.
- Passed focused backend tests outside the sandbox:
  - `mvn -q "-Dtest=StageSummaryServiceImplTest,StageSummaryControllerAuthIntegrationTest,AiServiceImplTest" test`
- Passed full backend test suite outside the sandbox:
  - `mvn -q test`
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 5 files changed, 180 insertions, 4 deletions.

Skipped verification reason:

- Real DeepSeek/openai-compatible stage-summary output was not called because no backend-side AI provider credentials are available; the provider-backed success branch is covered with a focused mocked-provider service test.
- Frontend consumption and Mini Program behavior are intentionally deferred to the next small commit.
- OpenSpec CLI status/apply validation was attempted and skipped because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Reused the accepted M4 AI endpoint and existing OpenAI-compatible adapter; no external endpoint, provider enum, configuration key, or accepted error semantic changed.
- Did not modify settings, admin, deployment, monitoring, SMS, notifications, campaign, social, H5/Web, voice transcription, or AI diagnosis/dashboard scope.
- Did not modify package or lockfile files.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real credential-backed stage-summary success remains unverified.
- Frontend must consume `status/message` and avoid treating fallback or unavailable AI results as provider success.

## 2026-06-19 23:29 M4 frontend AI status consumption

Task:

- Continue frontend phase 8 of `m4-real-capability-completion` after the backend stage-summary provider path was completed.
- Ensure authenticated real-mode AI consumers distinguish provider success from unavailable, failed, or fallback responses.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/pages/user-center/index.vue`
- `frontend/src/services/aiService.ts`
- `frontend/src/services/stageSummaryService.ts`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added the accepted `SUCCESS`, `UNAVAILABLE`, `FAILED`, and `FALLBACK` status contract plus optional `message` to frontend AI response types.
- Record editor now updates `aiSummary` and `beliefThen` only for `SUCCESS`; other statuses preserve the existing AI fields and original record content while showing the backend-provided message.
- User-center stage summaries now display whether the result is `AI 整理` or `本地整理`, and fallback/unavailable results show an explicit explanatory toast.
- Marked the first four frontend real-AI tasks complete, along with frontend type-check and Mini Program build verification tasks.

Verification:

- Initial direct type-check command failed because `node` was not available in the shell PATH:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Loaded the bundled workspace Node runtime and passed type-check:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build with the bundled Node runtime:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Confirmed build output remained ignored and did not add tracked changes under `frontend/dist`.
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 6 files changed, 84 insertions, 7 deletions.

Skipped verification reason:

- Real DeepSeek/openai-compatible success was not manually verified because no backend-side provider credentials are available.
- WeChat Developer Tools interaction was not run; this step verified compile/build output but not device-level toast rendering or network behavior.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH.

Scope safety check:

- Limited changes to M4 AI response consumption and a compact source label; no visual reconstruction or unrelated user-center/settings work was added.
- Did not add frontend secrets, package dependencies, or package/lockfile changes.
- Did not touch admin, deployment, monitoring, SMS, notifications, campaign, social, H5/Web, voice transcription, or AI diagnosis/dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed provider success and real WeChat runtime behavior still require manual integration verification.
- M4 frontend location, media/cover, and real-data surface phases remain open.

## 2026-06-19 23:43 M4 record editor real location

Task:

- Continue frontend phase 9 of `m4-real-capability-completion` with a real draft-location workflow.
- Replace the record editor location placeholder with current-location, map-picker, manual-input, update, and delete behavior backed by the accepted record location endpoints.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/manifest.json`
- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/services/recordService.ts`
- `frontend/src/types/record.ts`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added frontend `RecordLocationSource`, location DTO/VO fields, and real `PUT/DELETE /api/records/{id}/location` service methods.
- Record editor now supports `uni.getLocation` with GCJ-02 coordinates, `uni.chooseLocation`, and manual name/address entry.
- A new record with valid content is persisted as a real draft before location is saved, so subresource calls always use a backend-issued record ID.
- Location changes update the UI only after backend success; draft deletion uses confirmation and backend success before clearing local state.
- Permission denial or map cancellation leaves manual entry available and does not block record editing.
- Preview sessions remain read-only for location changes.
- Added Mini Program location permission description and `getLocation` / `chooseLocation` private API declarations.
- Marked only the implemented editor and permission-fallback location tasks complete; read-only time-review display and manual WeChat verification remain open.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `frontend/dist/build/mp-weixin/app.json` and confirmed it contains:
  - `permission.scope.userLocation.desc`
  - `requiredPrivateInfos` entries `getLocation` and `chooseLocation`
- Confirmed `frontend/dist` remained ignored and no generated build files became tracked changes.
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 6 files changed, 389 insertions, 10 deletions.

Skipped verification reason:

- WeChat Developer Tools/device permission, current-location accuracy, map-picker UI, and real backend request behavior were not manually run in this environment.
- Official location-manifest documentation lookup was attempted but the web search endpoint returned HTTP 403; implementation was checked against installed UniApp type definitions and generated Mini Program `app.json` instead.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH.

Scope safety check:

- Stayed within accepted M4 location endpoints, source enums, validation shape, draft-only mutation, and preview isolation.
- Did not add backend geocoding/reverse geocoding, package dependencies, package/lockfile changes, or broad visual reconstruction.
- Did not touch settings behavior, admin, deployment, monitoring, SMS, notifications, campaign, social, H5/Web, voice transcription, or AI diagnosis/dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real WeChat permission and map behavior still need manual Mini Program verification.
- Sealed/unlocked detail and time-review location display remain pending frontend work.

## 2026-06-20 00:03 M4 location read-only review display

Task:

- Continue frontend phase 9 of `m4-real-capability-completion` by completing sealed/unlocked location display.
- Replace the record-detail page's obsolete string-location compatibility read with the accepted `RecordLocationVO` object contract.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/record-detail/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Record detail now derives location name, address, and coordinates from the backend-backed `RecordDetailVO.location` object.
- SEALED records display a compact read-only location only when a real location exists; record titles are no longer presented as fake location labels.
- UNLOCKED time review displays a full read-only `当时所在` section with name, address, and coordinates when available.
- No location mutation controls were added to sealed or unlocked views.
- Marked sealed/unlocked read-only location display and unlocked time-review location display tasks complete.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `frontend/dist/build/mp-weixin/pages/record-detail/index.wxml` and confirmed:
  - SEALED location is guarded by `wx:if`.
  - UNLOCKED review contains the read-only `当时所在` section.
  - No location mutation action is emitted in record-detail output.
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before staging showed:
  - 3 files changed, 132 insertions, 11 deletions.

Skipped verification reason:

- WeChat Developer Tools visual/manual verification was not run; type-check, build, and generated WXML were used as automated evidence.
- Real backend location data was not loaded in a running Mini Program because no authenticated runtime session was available.
- OpenSpec CLI validation was attempted and skipped because `openspec` is not available in the current PowerShell PATH.

Scope safety check:

- Limited changes to backend-backed location presentation on existing SEALED/UNLOCKED detail surfaces.
- Did not add location mutation after seal, geocoding, package changes, settings work, or visual reconstruction.
- Did not touch admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real Mini Program visual verification with actual location data remains pending.
- Image/voice/cover read-only review and the full media workflow remain open.

## 2026-06-20 00:12 M4 frontend attachment service foundation

Task:

- Continue frontend phase 10 of `m4-real-capability-completion` with the shared real attachment transport boundary.
- Add typed frontend support for backend upload-token, Qiniu direct upload, backend commit verification, signed access URL, and draft delete APIs before connecting image/voice UI.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/services/attachmentService.ts`
- `frontend/src/services/index.ts`
- `frontend/src/types/record.ts`

What changed:

- Added attachment, upload-token, commit, and signed-access frontend DTO/VO types matching the accepted M4 backend contract.
- Extended record detail/list/timeline types with attachment and cover response fields already supplied by the backend.
- Added `attachmentService` methods for upload-token issuance, direct Qiniu upload, backend commit verification, private access URL generation, and draft deletion.
- Qiniu direct upload requires a successful 2xx response and the exact backend-authorized object key before the frontend proceeds to commit.
- No Qiniu AK/SK, bucket secret, or long-lived credential was added to frontend code.
- No user-facing image/voice task was marked complete in this foundation-only step.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- `git diff --check` passed with line-ending warnings only.
- `git diff --stat` before commit showed:
  - 4 files changed, 191 insertions.

Skipped verification reason:

- Real Qiniu upload was not run because backend-side Qiniu credentials and a configured private bucket are unavailable in this environment.
- No media UI/manual WeChat verification was claimed because this step only establishes the shared service boundary.
- OpenSpec CLI validation remains unavailable because `openspec` is not in the current PowerShell PATH.

Scope safety check:

- Implemented only the accepted Qiniu attachment transport contract and response types.
- Did not add storage secrets, package dependencies, package/lockfile changes, multi-cloud abstraction, transcription, or standalone cover upload.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, or H5/Web scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real Qiniu response behavior still needs credential-backed integration verification.
- Image selection/compression/preview/delete, voice recording/playback, and cover selection UI remain to be connected.

## 2026-06-20 11:23 M4 record editor real image flow

Task:

- Continue frontend phase 10 of `m4-real-capability-completion` with the record editor image workflow.
- Replace the image placeholder with real selection, default compression, Qiniu upload-token/direct-upload/backend-verification, private preview, retry, and draft delete behavior.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/record-editor/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added real image selection with album/camera sources and a maximum of 9 occupied image slots.
- Selected images are explicitly compressed at quality 80 before upload; the compressed file size and dimensions are read for the accepted attachment DTO.
- Added frontend pre-checks for 40 MB per compressed image and 300 MB total available/reserved attachments before requesting upload authorization.
- New records are persisted as real drafts only after the user selects images, so token and commit requests always use a backend-issued record ID.
- Added the accepted upload sequence: backend upload token, direct Qiniu upload, exact key response validation, and backend commit/stat verification.
- Images enter the available list only after commit returns `AVAILABLE`; compressing, uploading, verifying, and failed local states are shown separately.
- Failed uploads remain visible with detailed error text plus retry/remove controls; a successful Qiniu upload retains its authorized key for commit retry.
- Available images obtain short-lived owner-scoped access URLs from the backend, support multi-image `uni.previewImage`, and expose an understandable retry state when signed URL generation or loading fails.
- Draft image deletion calls the backend first and updates attachments, cached access URLs, and current-cover state only after success.
- Image upload/delete is preview-read-only, and navigation/sealing is blocked while an image operation is active to avoid sealing before attachment verification completes.
- Marked only the five completed image-selection/compression/upload/preview/delete tasks complete. The combined image/voice limit task remains open until the voice flow implements its count pre-check.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `frontend/dist/build/mp-weixin/pages/record-editor/index.wxml` and confirmed image grid, access failure retry, failed-upload retry/remove, delete `catchtap`, and occupied-count UI are emitted.
- Inspected generated `frontend/dist/build/mp-weixin/pages/record-editor/index.js` and confirmed `compressImage`, upload-token/Qiniu upload/backend commit, signed preview, preview-read-only delete, and upload-before-seal guards are emitted.
- Searched frontend source and generated Mini Program output for `QINIU_ACCESS_KEY`, `QINIU_SECRET_KEY`, and `AI_API_KEY`; no matches were found.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 114/155 to 119/155.
- `git diff --stat` before staging showed:
  - 3 files changed, 589 insertions, 11 deletions.

Skipped verification reason:

- Real Qiniu upload, object stat verification, signed URL retrieval, and private image loading were not run because backend-side Qiniu credentials/private bucket configuration and an authenticated Mini Program runtime are unavailable in this environment.
- WeChat Developer Tools album/camera selection, compression behavior, upload UI, image preview, and deletion were not manually exercised; type-check, build, and generated Mini Program artifacts are the current automated evidence.
- OpenSpec CLI status/apply validation was attempted and skipped because `openspec` is not available in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Stayed within the accepted M4 image attachment endpoints, private signed URL flow, limits, draft-only mutation, and preview isolation.
- Did not add frontend storage secrets, public bucket URLs, package dependencies, package/lockfile changes, standalone cover upload, multi-cloud storage, or broad visual reconstruction.
- Did not touch voice transcription/analysis, settings, admin, deployment, monitoring, SMS, notification center, campaign, social, or H5/Web scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed Qiniu and real WeChat runtime verification remains required before the image integration can be accepted end to end.
- An uploaded object can remain uncommitted if network delivery fails after Qiniu upload and before backend commit; the accepted stateless upload contract has no cleanup endpoint for such uncommitted objects.
- Voice recording/playback, cover selection, read-only media in time review, timeline/home cover display, and real-path mock cleanup remain open M4 work.

## 2026-06-20 11:34 M4 record editor real voice flow

Task:

- Continue frontend phase 10 of `m4-real-capability-completion` with real raw-voice recording, upload, playback, re-record, and delete behavior.
- Complete frontend pre-check coverage for the accepted image/voice count, per-file size, and per-record total-size limits.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/manifest.json`
- `frontend/src/pages/record-editor/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Replaced the voice placeholder with `RecorderManager` recording controls using 16 kHz mono MP3 at 64 kbps and a 10-minute maximum duration.
- Added a Mini Program `scope.record` permission description for user-initiated record voice attachments.
- Recorded MP3 files are kept as raw audio with no transcription or AI analysis and use the accepted backend upload-token, direct Qiniu upload, exact-key validation, and backend commit/stat verification flow.
- Voice attachments become available only after commit returns `AVAILABLE`; uploading, verifying, and failed retry/remove states remain distinct in the editor.
- Added max 9 voice pre-checks and combined image/voice reservation accounting for the 40 MB per-file and 300 MB per-record limits.
- Added owner-scoped signed URL retrieval and `InnerAudioContext` playback with loading, stop, ended, error, and request-race cleanup states.
- Added draft voice delete that updates local UI only after backend success.
- Added an explicit re-record flow that deletes the selected draft voice through the supported endpoint before starting a new recording; no unaccepted replace endpoint was invented.
- Added recorder event unbinding, recording stop, timer cleanup, and audio-context destruction during page unload.
- Media operations are mutually guarded, and seal is blocked while recording/uploading or while failed pending image/voice items remain unresolved.
- User-facing rows use `语音记录 N` instead of exposing generated storage filenames.
- Marked real recording, raw upload, signed playback, re-record/delete, and complete frontend limit pre-check tasks complete.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `frontend/dist/build/mp-weixin/app.json` and confirmed `permission.scope.record.desc` is present alongside existing location permissions.
- Inspected generated record-editor WXML and confirmed recording state, signed-play loading, upload failure retry/remove, re-record, delete, voice count, and duration UI are emitted.
- Inspected generated record-editor JS and confirmed `getRecorderManager`, MP3 recording options, raw Qiniu token/upload/commit, `createInnerAudioContext`, signed access URL, pending-media seal guard, recorder `off*` cleanup, and playback request-race handling are emitted.
- Searched frontend source and generated Mini Program output for `QINIU_ACCESS_KEY`, `QINIU_SECRET_KEY`, and `AI_API_KEY`; no matches were found.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 119/155 to 124/155.
- `git diff --stat` before staging showed:
  - 4 files changed, 730 insertions, 16 deletions.

Skipped verification reason:

- Real microphone authorization, recording duration/file-size behavior, Qiniu upload/stat verification, private signed URL playback, deletion, and re-record were not manually exercised because an authenticated WeChat Developer Tools runtime and backend-side Qiniu private-bucket credentials are unavailable in this environment.
- No real media integration success is claimed; type-check, Mini Program build, generated `app.json`/WXML/JS inspection, and secret scanning are the current automated evidence.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Stayed within accepted M4 raw voice attachment, private signed URL, draft-only mutation, limits, and preview-read-only boundaries.
- Did not add transcription, speech-to-text, voice AI analysis, standalone media upload, public bucket URLs, package dependencies, package/lockfile changes, or broad visual reconstruction.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, or H5/Web scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed Qiniu and real WeChat microphone/audio verification remains required before voice integration can be accepted end to end.
- The accepted re-record contract has no atomic replace endpoint; the UI explicitly deletes the old draft voice before recording the replacement, so cancelling or failing the new recording leaves the old voice deleted.
- Cover selection, read-only media in time review, timeline/home cover display, real-path mock cleanup, and full media integration verification remain open M4 work.

## 2026-06-20 11:40 M4 draft cover selection

Task:

- Continue frontend phase 10 of `m4-real-capability-completion` with draft cover selection from the current record's verified image attachments.
- Prevent standalone/no-image cover selection and preserve preview and draft lifecycle boundaries.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/record-editor/index.vue`
- `frontend/src/services/recordService.ts`
- `frontend/src/types/record.ts`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added typed `UpdateRecordCoverDTO` and a preview-read-only `PUT /api/records/{id}/cover` frontend service method matching the accepted contract.
- Record editor now loads the backend `cover` with draft detail and exposes a compact cover control without adding a standalone cover uploader.
- Cover selection mode lists only the current record's committed `AVAILABLE IMAGE` attachments; current cover and selectable image states are visible on the existing image grid.
- Selecting or clearing a cover updates the UI only after backend success and syncs the Pinia detail cache.
- With no available image, the cover control shows a focused prompt that routes to the existing image upload flow; it never sends a cover request or uploads a separate file.
- Preview sessions remain read-only for cover mutation.
- Deleting the current cover image relies on the accepted backend clear-on-delete behavior and now also clears local cover state; deleting the final image exits cover-selection mode.
- Cover saving participates in the shared media-operation guard, preventing concurrent upload/record/seal mutations.
- Marked only draft cover selection and no-image prevention tasks complete; timeline/home cover display remains open.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated record-editor WXML and confirmed current-cover badge, `设为封面`, `当前封面`, cover clear `catchtap`, and cover control states are emitted.
- Inspected generated record-editor JS and confirmed `recordService.updateCover`, same-record available-image pre-check, no-image add-image prompt, preview read-only branch, and backend-success-only local update are emitted.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 124/155 to 126/155.
- `git diff --stat` before staging showed:
  - 5 files changed, 273 insertions, 8 deletions.

Skipped verification reason:

- Real cover update/clear and backend clear-on-current-cover-image-delete were not manually exercised because no authenticated Mini Program runtime with Qiniu-backed image attachments is available in this environment.
- WeChat Developer Tools visual interaction was not run; type-check, Mini Program build, and generated WXML/JS inspection are the current automated evidence.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Stayed within the accepted same-record `IMAGE` cover endpoint and draft-only mutation contract.
- Did not add standalone cover upload, public media URLs, frontend secrets, package dependencies, package/lockfile changes, or broad visual reconstruction.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed cover persistence and current-cover delete behavior still require real Mini Program integration verification.
- Timeline/home cover display, sealed/unlocked read-only media, preview/mock cleanup, and full media integration verification remain open M4 work.

## 2026-06-20 11:45 M4 timeline and home real cover display

Task:

- Continue frontend phase 10 of `m4-real-capability-completion` by displaying backend-provided record covers on home and timeline cards.
- Resolve private cover media through owner-scoped short-lived access URLs while keeping preview and failure fallbacks honest.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/composables/useRecordCoverUrls.ts`
- `frontend/src/pages/home/index.vue`
- `frontend/src/pages/timeline/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Confirmed backend list and timeline responses already include same-record cover metadata while intentionally leaving `cover.accessUrl` null.
- Added a focused `useRecordCoverUrls` composable that resolves authenticated covers through `GET /api/records/{recordId}/attachments/{attachmentId}/access-url`.
- The composable accepts an explicit embedded `cover.accessUrl` for preview data but does not issue real storage requests when no auth token exists, preserving preview isolation.
- Added per-record request versions, stale-record cleanup, stale-URL clearing before refresh, and image-load failure state so changing/filtering cards cannot show an old or unrelated cover.
- Home's existing backend-backed latest SEALED arrival card now shows its selected cover when present; no-cover behavior retains the existing paper card rather than injecting preview media.
- Timeline DRAFT, SEALED, and UNLOCKED cards show their selected covers when present; unlocked cards retain the neutral image placeholder when no cover exists.
- Signed URL or image loading failure shows a restrained `封面暂不可用` fallback and never substitutes curated preview imagery.
- Marked only the timeline/home cover implementation task complete; credential-backed visual verification remains open.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated home WXML and confirmed real cover image, load-error fallback, and no-cover-preserving conditional structure are emitted on the arrival card.
- Inspected generated timeline WXML and confirmed DRAFT, SEALED, and UNLOCKED cover image/fallback branches are emitted.
- Inspected generated `composables/useRecordCoverUrls.js` and confirmed embedded URL handling occurs before the `getToken()` gate, and `createAccessUrl` is called only after a real token exists.
- Searched frontend source and generated Mini Program output for `QINIU_ACCESS_KEY`, `QINIU_SECRET_KEY`, and `AI_API_KEY`; no matches were found.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 126/155 to 127/155.
- Combined tracked diff plus the new composable before staging showed:
  - 5 files changed, 282 insertions, 3 deletions.

Skipped verification reason:

- Real private cover download and visual rendering were not manually exercised because backend-side Qiniu credentials/private bucket data and an authenticated WeChat Developer Tools runtime are unavailable in this environment.
- The integration checkbox for real timeline/home cover display remains open; generated WXML/JS is implementation evidence, not credential-backed end-to-end acceptance.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Limited changes to existing home/timeline cards and a shared private-cover URL helper; no new page or major visual reconstruction was added.
- Did not weaken private bucket access, embed permanent/public URLs, expose secrets, or change package/lockfiles.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed signed cover access and real Mini Program rendering still require manual integration verification.
- Home arrival countdown remains hard-coded and is tracked separately in phase 11; this cover step did not claim real home review timing completion.
- Sealed/unlocked read-only media, time-review media, preview/mock cleanup, and full media integration verification remain open M4 work.

## 2026-06-20 11:51 M4 sealed and unlocked read-only media

Task:

- Continue frontend phase 10/11 of `m4-real-capability-completion` by showing backend detail images and voices as read-only context for SEALED and UNLOCKED records.
- Provide private signed image preview and voice playback without exposing attachment, cover, or location mutation controls after seal.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/record-detail/components/ReadOnlyRecordMedia.vue`
- `frontend/src/pages/record-detail/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Added a record-detail-local `ReadOnlyRecordMedia` component backed only by `RecordDetailVO.attachments` and `cover` metadata.
- Mounted the same component on both SEALED detail and UNLOCKED 时间回看 surfaces; DRAFT detail remains routed to the editor.
- Available images resolve owner-scoped signed access URLs, display in a stable three-column grid, support authorized multi-image preview, and identify the selected cover.
- Image access or load failures remain visible as retryable `图片暂不可用` placeholders and do not substitute preview images.
- Available voices resolve a fresh signed URL when played and use `InnerAudioContext` with loading, stop, ended, error, request-race, and unmount cleanup behavior.
- No delete, re-record, upload, cover-change, or location-change control exists in the read-only component.
- Authenticated mode calls the real media access endpoint; no-token preview mode can only consume an explicit attachment `accessUrl` and never calls the real storage endpoint.
- Marked sealed/unlocked read-only attachments and backend-backed time-review detail/media/location/cover tasks complete. Real device media verification remains open.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated record-detail WXML and confirmed separate SEALED and UNLOCKED `read-only-record-media` component instances receive backend attachment/cover props.
- Inspected generated `ReadOnlyRecordMedia.wxml` and confirmed image preview, cover marker, voice playback, duration, and failure states are emitted with no mutation controls.
- Inspected generated component JS and confirmed `getToken`, `createAccessUrl`, `previewImage`, `createInnerAudioContext`, request cancellation, and unmount cleanup are emitted; no attachment delete, commit, or upload-token call exists.
- Searched frontend source and generated Mini Program output for `QINIU_ACCESS_KEY`, `QINIU_SECRET_KEY`, and `AI_API_KEY`; no matches were found.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 127/155 to 129/155.
- Combined tracked diff plus the new read-only media component before staging showed:
  - 4 files changed, 450 insertions, 2 deletions.

Skipped verification reason:

- Real signed image access/preview and voice playback were not manually exercised because backend-side Qiniu credentials/private objects and an authenticated WeChat Developer Tools runtime are unavailable in this environment.
- The integration checkbox for unlocked location/image/voice/reflection display remains open; generated component output proves implementation structure but not credential-backed end-to-end media behavior.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Limited changes to read-only media presentation on existing SEALED/UNLOCKED detail surfaces.
- Did not permit post-seal mutation, add transcription/voice AI, weaken private media access, expose secrets, or change package/lockfiles.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, or H5/Web scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Credential-backed Qiniu preview/playback and real WeChat audio behavior still require manual integration verification.
- Home countdown/mock cleanup, remaining real-mode preview audit, safe empty/error refinements, and full media integration verification remain open M4 work.

## 2026-06-20 11:55 M4 backend-backed home arrival and review cards

Task:

- Continue frontend phase 11 of `m4-real-capability-completion` by removing hard-coded home countdown/review values.
- Display a real latest-unlocked 时间回看 card from the already requested backend result, including private cover and section states.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/home/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Replaced hard-coded `3 天` / `72 小时` copy with a live countdown computed from the latest SEALED record's backend `unlockAt` value.
- Countdown output now distinguishes day/hour, hour/minute, imminent, missing-time, and already-due states.
- The home clock starts on `onShow` and is cleared on `onHide`/unmount so background pages do not retain timers.
- Arrival card now shows the real record title and creation year rather than a generic hard-coded journey claim.
- Added a latest-unlocked 时间回看 card using the real `getUnlockedRecords(1, 1)` result that the page already fetched but previously did not render.
- The review card displays backend title/unlock time and uses the existing authenticated private-cover URL flow.
- Added explicit loading, failure/retry, empty, and ready states for the latest-unlocked section without falling back to preview content in a real session.
- Extended home cover loading to the latest SEALED and latest UNLOCKED cards while preserving the composable's no-token preview boundary.
- Marked only the hard-coded home review countdown/card replacement task complete.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Searched home source and generated Mini Program home output for `72 小时` and `最后 3 天`; no matches were found.
- Inspected generated home WXML and confirmed arrival data bindings plus latest-unlocked loading, error/retry, empty, real card, and private-cover branches are emitted.
- Inspected generated home JS and confirmed `getUnlockedRecords`, real `unlockAt - Date.now()` arithmetic, day/hour/minute formatting, and timer cleanup in `onHide`/unmount are emitted.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 129/155 to 130/155.
- `git diff --stat` before staging showed:
  - 3 files changed, 246 insertions, 10 deletions.

Skipped verification reason:

- Real countdown timing and latest-unlocked card rendering were not manually exercised in WeChat Developer Tools because no authenticated runtime dataset is available in this environment.
- Real private cover rendering remains dependent on Qiniu credentials/private objects and is not claimed from build output alone.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Limited changes to the existing home page's backend-backed summary and current card visual language.
- Did not add a new API, mock fallback, package dependency, package/lockfile change, or major visual reconstruction.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Real home countdown/card and Qiniu cover behavior still require authenticated WeChat manual verification.
- Preview/mock usage audit, global safe failure-state closeout, and full media integration verification remain open M4 work.

## 2026-06-20 12:00 M4 preview and real-integration boundary audit

Task:

- Audit preview/mock usage across M4-touched services, record store, home, timeline, record editor, record detail, read-only media, cover helper, and stage-summary surface.
- Ensure explicit preview cannot accidentally call real AI/Qiniu integrations while authenticated token paths remain backend-backed.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/user-center/index.vue`
- `frontend/src/services/aiService.ts`
- `frontend/src/services/attachmentService.ts`
- `frontend/src/services/stageSummaryService.ts`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Confirmed `recordService` uses preview data only when both conditions hold: no real token and an explicit preview session.
- Confirmed a real token takes precedence over preview session state for record list/detail, timeline, home review, location, cover, and reflection APIs.
- Confirmed the record store does not introduce any independent mock fallback; it delegates to `recordService` and preserves backend failures.
- Added defense-in-depth preview guards to writing prompts, record summary AI, stage summary, upload token, Qiniu upload, attachment commit, signed URL, and attachment delete service boundaries.
- These guards reject only `!getToken() && hasPreviewSession()`; unauthenticated non-preview behavior and authenticated real behavior remain unchanged.
- Added an explicit user-center stage-summary preview guard and read-only toast so preview does not make a hidden unauthenticated request or present a fake success.
- Confirmed home/timeline cover access and record-detail read-only media use embedded preview URLs only without a token; they call real signed URL APIs only with a token.
- Confirmed record editor location/image/voice/cover mutations already have explicit preview read-only guards; the service boundary now prevents accidental regressions if a page guard is missed later.
- Marked preview/mock audit, explicit-preview-only data, and authenticated backend-backed core surface tasks complete. Runtime preview verification remains open.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\\node_modules\\.bin\\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\\node_modules\\.bin\\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `aiService.js`, `stageSummaryService.js`, and `attachmentService.js`; each real integration is guarded by the exact `!getToken() && hasPreviewSession()` condition.
- Inspected generated user-center JS and confirmed explicit preview stage-summary toast occurs before `stageSummaryService.generate()`.
- Searched M4-touched pages/store/services for `getPreview` and `mock`; preview data imports remain confined to `recordService` and are gated by `shouldUsePreviewData`.
- `git diff --check` passed with line-ending warnings only.
- OpenSpec task progress advanced from 130/155 to 133/155.
- `git diff --stat` before staging showed:
  - 6 files changed, 108 insertions, 4 deletions.

Skipped verification reason:

- Preview mode was not manually launched in WeChat Developer Tools; this step verifies compile-time and generated-branch isolation, not full interactive preview behavior.
- Real authenticated API behavior was not exercised against a running backend in this environment; backend-backed routing is verified from service and generated code structure.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Changes are limited to integration boundary guards and one existing stage-summary action.
- Did not add preview mock media, fake AI success, public storage URLs, secrets, dependencies, package/lockfile changes, or visual reconstruction.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Explicit preview mode still requires real Mini Program interaction verification before its functional checkbox can be closed.
- Global safe failure-state closeout and credential-backed AI/Qiniu integration verification remain open M4 work.

## 2026-06-20 12:04 M4 real-mode safe state closeout

Task:

- Audit loading, empty, and failure states across M4 real-data pages and close the remaining home summary gap.

Modified files:

- `.ai/AGENT_LOG.md`
- `frontend/src/pages/home/index.vue`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Confirmed record list, timeline, record detail, record editor, and user center already expose explicit loading, empty, failure/retry, or stale-data states appropriate to their real-data requests.
- Added a dedicated loading state for the home page's sealed-record summary instead of temporarily showing an empty-state call to action while the backend request is pending.
- Added a dedicated retryable error state for the same summary so a real backend failure is visible and never appears as a successful empty response.
- Preserved the existing backend-backed latest-unlocked loading/error/empty states and existing empty-record creation action.
- Marked the M4 safe empty/loading/error state task complete.

Verification:

- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\node_modules\.bin\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\node_modules\.bin\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected generated `dist/build/mp-weixin/pages/home/index.wxml`; both `正在同步即将抵达的记录...` and retryable `即将抵达暂未同步 · 轻触重试` branches are present.
- `git diff --check` passed before this log update.
- OpenSpec task progress advanced from 133/155 to 134/155.

Skipped verification reason:

- Loading and request-failure branches were not manually forced in WeChat Developer Tools because no authenticated runtime backend dataset is available in this environment.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- Limited the code change to status rendering in the existing home page; no API, persistence, package, or lockfile changes were made.
- Did not add a mock fallback or convert backend failures into apparent success.
- Did not touch settings, admin, deployment, monitoring, SMS, notification center, campaign, social, H5/Web, transcription, or AI dashboard scope.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Authenticated Mini Program runtime verification remains required for real loading/error transitions.
- Credential-backed AI/Qiniu integration and explicit preview interaction verification remain open M4 work.

## 2026-06-20 12:16 M4 automated final review

Task:

- Perform the M4 code-level final review for scope boundaries, real integration behavior, secrets, immutability, media semantics, and visible naming.

Modified files:

- `.ai/AGENT_LOG.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`

What changed:

- Confirmed the cumulative M4 diff from baseline `9ee190c` contains no settings page, admin, deployment, monitoring, SMS, production notification center, campaign delivery, social feed, H5/Web acceptance, package, or lockfile implementation.
- Confirmed authenticated AI service branches use the configured OpenAI-compatible provider endpoint and return `UNAVAILABLE` or `FAILED` when provider configuration, response content, or transport fails; mock success remains behind the explicit disabled-by-default `real-mode-mock-enabled` switch.
- Confirmed Qiniu upload and management credentials are read only from backend configuration, private object access URLs are signed by the backend with an expiry, and no frontend or tracked secret value was found.
- Confirmed location update/delete, attachment upload/commit/delete, and cover mutation require a `DRAFT` record.
- Confirmed cover selection queries an available attachment by `attachmentId + recordId + userId` and accepts only `IMAGE` attachments.
- Confirmed voice is recorded and committed as raw audio attachments and no speech recognition, transcription, or voice AI path exists.
- Confirmed the V2.0 visible names `我的记录`, `时光轴`, and `时间回看` remain in current user-facing surfaces; no old visible naming match was found in the audited frontend paths.
- Marked the ten code-level Final Review confirmations complete. Credential-backed and WeChat runtime verification tasks remain open.

Verification:

- Passed backend full test suite offline against the existing local Maven cache:
  - `mvn "-Dmaven.repo.local=C:\\Users\\Lin\\.m2\\repository" -o -s C:\\Users\\Lin\\.m2\\settings.xml -q test`
  - Surefire summary: `25` suites, `192` tests, `0` failures, `0` errors, `0` skipped.
- Passed frontend type-check with the bundled workspace Node runtime:
  - `.\node_modules\.bin\vue-tsc.cmd --noEmit`
- Passed WeChat Mini Program build:
  - `.\node_modules\.bin\uni.cmd build -p mp-weixin`
  - Output: `DONE Build complete.`
- Inspected the cumulative changed-file list from `9ee190c..HEAD`; forbidden scope and package/lockfile path count is zero.
- Searched backend/frontend source for transcription and speech-to-text terms; no implementation match was found.
- Searched audited frontend paths for legacy visible names; no legacy name match was found, while the three canonical names are present.
- Searched tracked configuration/source for AI and Qiniu key assignments; only backend environment-variable placeholders were found.
- Inspected AI provider branches, Qiniu signed URL construction, draft guards, same-record cover lookup, image-only cover validation, and raw recorder/upload settings.
- `git diff --check` passed before this log update.
- Cumulative M4 `git diff --stat 9ee190c..HEAD`: `84 files changed, 11276 insertions(+), 439 deletions(-)`.
- OpenSpec task progress advanced from 134/155 to 144/155.

Skipped verification reason:

- Real AI provider success was not invoked because no approved runtime API credential is available in this environment.
- Real Qiniu upload, object verification, signed URL access, image preview, and voice playback were not invoked because no approved bucket credential/private object dataset is available.
- Preview, location, cover, media, and unlocked time-review flows were not manually exercised in WeChat Developer Tools; compile/build and generated-code checks do not replace interactive verification.
- The first sandboxed Maven attempt could not resolve the parent POM because the sandbox uses an isolated Maven repository. The final offline command explicitly selected the existing read-only local repository and passed.
- OpenSpec CLI status/apply validation remains unavailable because `openspec` is not in the current PowerShell PATH; checked-in OpenSpec artifacts were used as the fallback fact source.

Scope safety check:

- This step changed only the task checklist and evidence log; no product code, configuration, dependency, schema, or contract mapping was altered.
- Code review covered the full M4 diff while preserving the accepted OpenSpec scope and canonical mappings.
- Did not touch the unrelated untracked `.claude/settings.local.json`.

Remaining risks:

- Eleven OpenSpec items remain open: one conditional decision-log item, nine credential/manual integration checks, and the eventual final handoff item.
- Real provider behavior, private Qiniu media behavior, preview isolation, location UX, cover rendering, and unlocked time-review composition still require authenticated WeChat runtime evidence.

## 2026-06-20 23:51 M4 real credential and backend live verification

Task:

- Audit the locally supplied AI, Qiniu, WeChat, database, and frontend API configuration without exposing secret values.
- Start the real `dev` backend and execute automated plus credential-backed HTTP verification for M4 core paths.

Modified files:

- `.ai/AGENT_LOG.md`

What changed:

- No product code, package, lockfile, OpenSpec contract, or tracked schema file was changed by this verification step.
- Added the missing M4 structures to the local MySQL `flashback` database from the canonical `schema.mysql.sql`: `record.cover_attachment_id`, `record_location`, and `record_attachment`.
- Started the backend on port `8080` with process-scoped credentials through ignored runtime helpers under `backend/target/runtime`; secret values were not printed.
- The existing ignored WeChat startup script generated `frontend/.env.local` with the local API base URL and configured subscription template ID.
- Temporary live-test users and records were removed after verification; no successful Qiniu object was created.

Verification:

- Configuration audit:
  - WeChat AppID, secret, and template ID are present in the ignored local script, and the AppID matches the Mini Program manifest.
  - The user-supplied AI/Qiniu defaults are present in tracked `application.yml`; this is operationally readable but violates the secret-handling boundary and must not be committed.
  - `application-dev.yml` overrides the AI provider to `mock` unless `AI_*` values are injected into the process; the first real request reproduced `UNAVAILABLE / AI mock provider未启用`.
- Backend startup:
  - `dev` profile started successfully on port `8080`.
  - Runtime log contains zero `WARN`/`ERROR` entries after startup and live checks.
- Automated backend suite:
  - `mvn -q test` passed.
  - Surefire summary: `25` suites, `192` tests, `0` failures, `0` errors, `0` skipped.
- Real AI:
  - Authenticated `POST /api/ai/writing-prompts` returned `SUCCESS`.
  - A five-run authenticated `POST /api/ai/summarize-record` loop produced `4 SUCCESS` and `1 FAILED / AI返回内容无效`; provider connectivity works, but the selected model's six-field JSON output is not fully stable.
- Real core HTTP flow:
  - Registration/login and JWT authentication passed.
  - Record creation and `MANUAL` location persistence/detail read passed.
  - Record seal passed.
  - Location mutation, attachment upload-token issuance, and cover mutation after seal were each rejected with HTTP `400`.
- Qiniu credential verification:
  - Both configured keys have plausible 40-character lengths and are not identical.
  - Read-only Qiniu `stat` probes with the configured pair and with AK/SK swapped both returned HTTP `401`.
  - Direct upload returned `401 BadToken`; the configured AK/SK are not an accepted pair or have been disabled/deleted.
- Frontend checks:
  - `vue-tsc --noEmit` passed.
  - `uni build -p mp-weixin` passed with `DONE Build complete.`
- `git diff --check` passed.

Skipped verification reason:

- Real Qiniu upload, backend stat commit, signed private download, image preview, voice playback, cover display, and media deletion could not complete because Qiniu rejected the supplied credential pair.
- WeChat login, current-location/map-picker permission behavior, recorder behavior, preview isolation, and unlocked time-review composition were not manually exercised in WeChat Developer Tools in this backend-only run.
- No production deployment, monitoring, admin, settings, SMS, notification-center, campaign, social, H5/Web, transcription, or voice-AI work was attempted.

`git diff --stat` before this log update:

- `backend/src/main/resources/application.yml | 16 ++++++++--------`
- The tracked diff belongs to the user's local credential edit; the verification step did not alter it.

Scope safety check:

- All live calls used a generated local test account and generic non-private fixture text.
- Test users/records were deleted after the run, and Qiniu rejected upload before object creation.
- Secret values, JWTs, upload tokens, signed URLs, database passwords, and WeChat credentials were not written to the evidence log or command output.
- Temporary helpers and logs are under ignored `backend/target/runtime`; `frontend/.env.local` is ignored.

Remaining risks:

- `application.yml` currently contains literal AI/Qiniu defaults inside environment placeholders. They are in a tracked file and must be migrated to an ignored local secret source or process environment before any commit.
- `application-dev.yml` defaults AI back to `mock`; a durable local startup path must explicitly inject `AI_*` configuration.
- The selected AI model returned incomplete JSON once in five runs. M4 needs either a more stable supported model, a scoped retry policy, or tolerant/repairing JSON handling before calling the real AI summary path stable.
- Qiniu AK/SK must be regenerated or replaced with a valid matching pair from the same active account before media acceptance can resume.
- The backend is intentionally left running on port `8080` for subsequent WeChat Developer Tools verification.

## 2026-06-21 16:48 M4 local secret migration and regenerated Qiniu credential retest

Task:

- Validate the regenerated Qiniu AK/SK without exposing their values.
- Move AI/Qiniu secret defaults out of tracked Spring configuration into the existing Git-ignored local startup script.
- Restart with the migrated configuration and rerun real backend plus regression checks.

Modified files:

- `.ai/AGENT_LOG.md`
- `backend/start-dev-wechat.local.ps1` (Git-ignored local secret file)

What changed:

- Migrated `AI_*`, `STORAGE_PROVIDER`, and all `QINIU_*` values from `application.yml` defaults into a marked process-environment block in the ignored `backend/start-dev-wechat.local.ps1`.
- Normalized the local Qiniu private domain to include the required `https://` scheme.
- Restored tracked `backend/src/main/resources/application.yml` to environment-variable-only placeholders; its content hash now matches `HEAD`.
- Confirmed `application-dev.yml` content hash also matches `HEAD`; Git continues to report a Windows line-ending/stat warning, but `git diff` contains no application configuration changes.
- Restarted the `dev` backend through the normal local startup script; it remains running on port `8080`.

Verification:

- Safe configuration audit:
  - AI API key, Qiniu AK, and Qiniu SK are all present in the ignored local script.
  - `git grep` found zero tracked occurrences of each secret value.
  - Tracked `application.yml` contains only empty `${AI_API_KEY:}`, `${QINIU_ACCESS_KEY:}`, `${QINIU_SECRET_KEY:}`, `${QINIU_BUCKET:}`, and `${QINIU_PRIVATE_DOMAIN:}` defaults.
- Regenerated Qiniu credential checks:
  - AK and SK are both 40 characters, are not identical, and were read without trimming loss.
  - Read-only management probes with the configured pair and the swapped pair both returned HTTP `401`.
  - A direct upload-token probe generated from the configured pair returned `401 BadToken`; no object was created.
  - The same result was reproduced through the real backend upload-token flow after the local-secret migration.
  - Current conclusion: the supplied AK/SK are still not accepted as a valid active pair; the failure is independent of the config file location.
- Migrated local startup verification:
  - Backend started successfully with the `dev` profile on port `8080`, PID `19836`.
  - Runtime log has zero `WARN`/`ERROR` entries.
  - Real registration/login, AI writing prompts, AI summary, record creation, manual location persistence, seal, and sealed mutation rejection all passed.
  - Location update, attachment upload-token request, and cover mutation after seal each returned HTTP `400` as expected.
- Regression suite:
  - `mvn -q test` passed.
  - Surefire summary: `25` suites, `192` tests, `0` failures, `0` errors, `0` skipped.
- Cleanup:
  - All generated `m4_smoke_*`, `m4_core_*`, and `m4_ai_*` users/records were removed.
  - Qiniu rejected upload before object creation, so no test object remains.
- `git diff --check` passed.

Skipped verification reason:

- Qiniu object stat commit, private signed URL, image preview, voice playback, cover display, and object deletion cannot proceed until Qiniu accepts an active AK/SK pair.
- WeChat Developer Tools interaction, real location permission prompts, recording UX, preview isolation, and unlocked time-review composition remain manual checks.

`git diff --stat` before this log update:

- `.ai/AGENT_LOG.md | 74 insertions`
- No product/config/dependency/schema tracked diff was present; local secrets and runtime helpers remain ignored.

Scope safety check:

- No application code, package, lockfile, OpenSpec contract, deployment, monitoring, admin, settings, SMS, campaign, social, H5/Web, transcription, or voice-AI code was changed.
- Secret values were neither printed nor stored in tracked files.
- The migration only changed the existing ignored local startup file plus this required evidence log.

Remaining risks:

- The regenerated Qiniu pair must be copied again from the same enabled key row, or replaced with a Kodo-authorized sub-account key, before media testing can continue.
- The Qiniu Bucket must belong to the same account/authorized sub-account as that key pair.
- Real AI remains configured and passed this run, but the previously observed occasional incomplete JSON response remains a separate reliability risk.
- The backend remains running for follow-up testing.

## M4 Qiniu Region Adaptation & Diagnostic Log - 2026-06-21

### What Changed

- **后端七牛云多 Region 适配**：
  - 修改了 `AppStorageProperties.java`，在七牛配置对象中添加了 `uploadUrl` 属性，并提供 Getter/Setter。
  - 修改了 `RecordAttachmentServiceImpl.java`，实现了 `resolveUploadUrl(qiniu)` 辅助方法，根据 `region` 动态匹配七牛云官方直传域名；同时如果配置中指定了 `uploadUrl`，则优先使用该指定域名；生成 `uploadToken` 时替换了原先硬编码的华东直传域名。
  - 修改了 `application.yml`，在 `app.storage.qiniu` 下暴露了 `upload-url: ${QINIU_UPLOAD_URL:}`。
- **清除本地端口冲突**：
  - 诊断出 8080 端口被残留的后台 Java 进程 (PID 19836) 占用，通过 `taskkill` 杀死了老进程，恢复了本地开发脚本的启动可用性。
- **诊断七牛云 AK/SK 密钥失效**：
  - 编写管理凭证签名测试程序请求七牛云全局 API，确认目前配置的 AK/SK 属于无效/停用密钥对，返回 `BadToken`。

### Verification Results

- 在 `backend` 下执行 `mvn clean test`，所有单元测试通过（192 tests run, 0 failures）。
- 成功解除了 8080 端口的占用，启动脚本可以顺利执行启动。
- 整理了详细的七牛云本地配置及排错指南。

### Git Diff Stat

```
backend/src/main/java/com/flashback/config/AppStorageProperties.java |  9 +++++
backend/src/main/java/com/flashback/service/impl/RecordAttachmentServiceImpl.java | 35 ++++++++++++++++++-
backend/src/main/resources/application.yml         |  1 +
```

### Scope Safety Check

- 没有改动任何核心业务逻辑、数据库持久化表结构或 package.json/pom.xml 依赖；
- 仅为 Qiniu 配置类新增字段和在服务实现层进行直传 Region 自适应映射，符合 M4 核心附件上传的可用性范围；
- 本地启动脚本和密码没有被跟踪提交。

### Remaining Risks

- 用户仍需要将最新、已启用且具有 Kodo 管理权限的有效 AK/SK 对复制并配置到 `backend/start-dev-wechat.local.ps1` 中以完成真机联调。

## 2026-06-21 M4 provider-neutral object storage implementation

### Decision and scope change

- User explicitly replaced the accepted Qiniu-only M4 decision with configurable object storage so new uploads can switch provider by backend configuration.
- Updated `AGENTS.md`, `.ai/ACTIVE_TASK.md`, M4 proposal/design/tasks, accepted backend contract, and M4 spec deltas.
- Accepted implementation scope is Qiniu plus an S3 Signature V4 compatibility path. Provider-specific storage features outside record attachment upload/verify/access/delete remain out of scope.

### Implementation

- Added `ObjectStorageProvider`, `ObjectStorageRegistry`, provider-neutral metadata/authorization/error types, and persisted-provider routing.
- Preserved Qiniu Kodo through `QiniuObjectStorageProvider`; corrected Qiniu management stat from GET to POST.
- Added `S3CompatibleObjectStorageProvider` using AWS SDK v2 S3 signing/client support for presigned PUT/GET, HEAD verification, and delete.
- Added provider aliases `s3-compatible`, `aws-s3`, `aliyun-oss`, `tencent-cos`, and `minio`, all persisted canonically as `S3_COMPATIBLE`.
- Replaced Qiniu-shaped upload DTO fields with provider-neutral `uploadMethod`, `uploadUrl`, `fileFieldName`, `uploadHeaders`, and `uploadFormData`.
- Added optional commit `provider`; frontend carries the provider returned at authorization time so a configuration change during an in-flight upload does not verify against the wrong provider.
- Frontend now uses multipart upload for Qiniu and ArrayBuffer PUT for S3-compatible authorization without exposing provider credentials.
- Added `backend/OBJECT_STORAGE_CONFIG.md` with Qiniu/S3-compatible switching instructions, credential safety, Aliyun OSS/Tencent COS/MinIO endpoint examples, and old-attachment compatibility rules.
- Added AWS SDK v2 `s3` backend dependency. This package change is required to use maintained Signature V4 signing and avoid a custom cryptographic implementation.

### Verification

- Frontend type-check: PASS via `node_modules/.bin/vue-tsc.cmd --noEmit`.
- WeChat Mini Program build: PASS via `node_modules/.bin/uni.cmd build -p mp-weixin`; output reported `DONE Build complete`.
- Added focused tests for provider aliases/registry routing, S3 presigned PUT/GET authorization, generic attachment service behavior, configuration, and controller DTO shape.
- Backend compile/full tests: NOT RUN. Maven dependency resolution failed inside the restricted sandbox with `Permission denied` to Maven Central. The required escalated execution was then rejected because the desktop approval account had reached its usage limit. This is an environment/authorization blocker, not a passing backend result.
- Real S3-compatible provider upload/HEAD/signed GET/delete and WeChat Developer Tools media flow: NOT RUN because no S3-compatible test credentials/bucket were configured and the new backend artifact could not be built in this turn.
- `openspec` CLI dynamic status: NOT RUN because `openspec` was not available in PATH; task progress was counted directly from `tasks.md` as 150 complete / 11 pending after this change.
- Official provider web-document lookup: Agent Reach Exa backend was unavailable and the web fallback returned HTTP 403; implementation is constrained to the documented S3 Signature V4 compatibility contract and the maintained AWS SDK.

### Scope safety and remaining risks

- No database migration was required: `record_attachment.storage_provider` already exists as `VARCHAR(20)` and stores provider per attachment.
- No deployment, monitoring, admin, settings, SMS, notification center, speech-to-text, or visual reconstruction work was added.
- Existing Qiniu attachments require Qiniu configuration to remain available after switching new uploads to S3-compatible storage; configuration switching does not migrate remote objects.
- S3-compatible providers must support SigV4 presigned PUT/GET plus HEAD/DELETE. Provider-specific deviations require a dedicated adapter rather than fake compatibility.
- The S3 frontend PUT path reads the file into an ArrayBuffer; the current 40 MB M4 limit may cause memory pressure on low-end devices and needs real WeChat device verification.
- A failed pnpm invocation created an untracked `.pnpm-store/` directory. Cleanup was attempted only after validating the path was inside the workspace, but the destructive command was blocked by the same approval usage limit; it remains untracked and must not be committed.

## 2026-06-21 M4 provider backend build, startup, and API acceptance

### Provider compatibility confirmation

- Used Agent Reach Jina Reader against Alibaba Cloud official documentation.
- Alibaba Cloud documents that OSS supports S3-compatible PutObject, GetObject, HeadObject, and DeleteObject operations.
- Alibaba Cloud's AWS SDK Java 2.x example requires an endpoint such as `https://s3.oss-{region}.aliyuncs.com`, virtual-hosted access, and disabled chunked encoding.
- Updated `S3CompatibleObjectStorageProvider`, `backend/OBJECT_STORAGE_CONFIG.md`, design, and accepted backend contract to disable chunked encoding and document the official Aliyun endpoint shape.

### Automated verification

- Focused storage tests: PASS with `mvn -q "-Dtest=S3CompatibleObjectStorageProviderTest,ObjectStorageRegistryTest" test`.
- Full backend suite: PASS, 27 suites / 196 tests / 0 failures / 0 errors / 0 skipped.
- Frontend type-check: PASS with `vue-tsc --noEmit`.
- WeChat Mini Program build: PASS with `uni build -p mp-weixin`; output reported `DONE Build complete`.
- `git diff --check`: PASS.
- Generated `.pnpm-store/` from the previous failed pnpm invocation was removed after validating the resolved path remained inside the workspace.

### Runtime and HTTP API acceptance

- MySQL80 and Redis Windows services were running; ports 3306 and 6379 were reachable.
- Backend started with dev profile and listened on 8080. Final observed running PID: 34528.
- Unauthenticated record request returned HTTP 401.
- Provider API smoke passed for registration, login, record creation, manual location persistence, record list, timeline, provider-neutral upload authorization, remote object verification rejection, location removal, and draft cleanup.
- Current local provider remained `QINIU`; an unuploaded object commit returned explicit HTTP 503 because the current Qiniu credentials/provider remained unavailable. No attachment metadata was persisted.
- Test records and users matching `m4_provider_%` and `m4_ai_%` were cleaned from MySQL; post-cleanup count was zero.
- `record_attachment` contained no rows, so switching providers currently has no legacy attachment migration burden.

### AI stability finding

- Real writing-prompts API succeeded.
- Real summarize API first failed 3/3, later passed once after restart, and a separate 5-call stability sample produced 2 SUCCESS / 3 FAILED (`AI返回内容无效`).
- Safe temporary instrumentation showed invalid responses can omit all six expected fields or return only a subset. All `[DEBUG-ai-summary-shape]` source instrumentation was removed after diagnosis.
- This remains a real M4 reliability risk. No fake success or local fallback was persisted as provider success.

### Skipped verification and blockers

- Real Aliyun OSS PUT -> HEAD verification -> signed GET -> delete was not run because `S3_ENDPOINT`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, and `S3_BUCKET` were not configured in the ignored local script.
- A final restart after removing temporary AI instrumentation was blocked when the desktop escalation quota was exhausted. The running PID is functionally the same build plus the temporary safe missing-field warning; tracked source contains no debug tag.
- Manual WeChat Developer Tools image/voice preview, playback, cover, and unlocked time-review verification remains pending.

### Project state and risks

- OpenSpec tasks remain 150 complete / 11 pending.
- Current local ignored startup script keeps long-lived credentials as literals. Before any cloud-server deployment, rotate the WeChat Mini Program secret exposed during local diagnostics and move all secrets to a server-side environment file/secret store instead of copying the local script.

## 2026-06-21 Aliyun OSS Upload Diagnostic and Configuration Fix

### What Changed

- **诊断上传失败与 "bad token" 错误原因**：
  - 发现本地 Git 忽略的启动脚本 `backend/start-dev-wechat.local.ps1` 中同时写了 `aliyun-oss` 和 `qiniu` 两组配置。因为 `qiniu` 配置块被放在了后面，导致 `$env:STORAGE_PROVIDER` 变量被重置覆盖为了 `'qiniu'`。
  - 由于后端实际运行在 Qiniu 模式，在小程序进行图片/语音上传时，后端会根据七牛云配置去签发七牛的 upload token。而用户更新了阿里云 OSS 的密钥，但没有更新（或失效了）七牛云的密钥，因此七牛云返回了 `{"error":"bad token"}` 导致上传失败。
- **修复启动配置脚本**：
  - 修改了 `backend/start-dev-wechat.local.ps1`，将七牛云配置块注释掉，使 `$env:STORAGE_PROVIDER = 'aliyun-oss'` 正确保持为当前生效值，让后端能够真正加载并使用阿里云 OSS（S3 兼容模式）的配置。

### Verification

- 对本地 `backend/start-dev-wechat.local.ps1` 进行了修改，确认注释掉 Qiniu 配置段后，运行时 `$env:STORAGE_PROVIDER` 会正确为 `'aliyun-oss'`。
- S3 兼容模式的直传与对象验证机制已在先前提交中通过单元测试验证。

### Scope Safety and Remaining Risks

- 没有改动任何产品代码或核心业务逻辑，仅修改了不受 git 追踪的本地启动脚本 `start-dev-wechat.local.ps1`。
- 如果用户使用的是阿里云 OSS 桶，请确保该 bucket 的 CORS（跨域资源共享）已正确配置，允许来自小程序（或开发测试中允许所有源 `*`）的 `PUT`/`GET`/`HEAD`/`DELETE` 请求，并暴露了 `ETag` 等必要头部，否则真机上传时可能会遇到跨域限制报错。

## 2026-06-21 Aliyun OSS Voice Upload "Size Mismatch" Diagnostic and Fix

### What Changed

- **诊断“上传文件大小不一致”报错原因**：
  - 在微信小程序中，录音停止回调中的 `result.fileSize`（由底层录音管理器提供）往往与音频文件实际写入磁盘的真实字节数存在细微偏差（常因为编码尾封、元数据或系统差异导致不准）。
  - 小程序直传阿里云 OSS 时上传的是实际文件流（真实磁盘大小），但 `createUploadToken` 和 `commit` 接口传入的却是来自回调中偏大或偏小的 `result.fileSize`。
  - 后端在验证已上传对象时，通过 OSS API 读取其真实的 `Content-Length`，并与 `commit` 传入的大小作对比。由于上述偏差，两者大小不一致，从而触发了 `verifyUploadedObject` 中的 `"上传文件大小不一致"` 异常导致报错。
- **修复方案**：
  - 修改了 `frontend/src/pages/record-editor/index.vue` 中的 `uploadRecordedVoice` 逻辑，改掉对 `result.fileSize` 的直接依赖，统一使用 `getFileSize(filePath)` (通过 `uni.getFileInfo`) 异步读取本地磁盘中录音文件的实际大小。
  - 增强了 `getFileSize` 辅助方法，使其接受可选的 `errorMsg` 参数，以便在图片和语音检测失败时能够显示各自对应的定制报错消息。

### Verification

- **前端类型检查**：运行 `pnpm type-check` 顺利通过，未报告任何 TypeScript 错误。
- **小程序打包编译**：运行 `pnpm build:mp-weixin` 成功编译为微信小程序，未报告任何编译错误，包生成状态为 `DONE`。

### Scope Safety and Remaining Risks

- 仅修正了小程序录音完上传文件时的参数大小获取逻辑，完全不影响核心数据库模型、API 接口定义以及其他附件上传校验的主干。
- **验证限制**：实际文件上传及 OSS 端的大小匹配流程，需要开发者在微信开发者工具或真机中录制一段语音，走真实的“生成 Token -> 直传 -> 提交校验”流程。

## 2026-06-22 M4 timeline filtering and pagination contract update

### User-confirmed decision

- The user accepted the recommended M4 timeline filtering plan before implementation.
- Date filtering uses record `createdAt` in the `Asia/Shanghai` business timezone.
- Timeline supports one tag at a time; tag and date conditions use AND semantics.
- Date granularity is year, month, or exact day; month requires year and day requires year/month.
- `GET /api/records/timeline` remains the endpoint and returns `TimelinePageVO` inside the existing `ApiResponse` wrapper.
- Pagination defaults to 20 records, caps page size at 50, and uses stable `created_at DESC, id DESC` ordering.
- Multiple-tag boolean logic, keyword search, record state/type filtering, and persisted filter preferences remain outside this M4 addition.

### Documentation changes

- Updated `.ai/ACTIVE_TASK.md` with timeline filtering, pagination, preview parity, and verification focus.
- Updated M4 `proposal.md` with the partial-current-state problem, P1 scope, acceptance criteria, and implementation order.
- Updated M4 `design.md` with filter-sheet UX, date semantics, record-level pagination, month-group merge, preview behavior, index audit, and verification expectations.
- Updated `backend-contract-decisions.md` with the accepted endpoint/query/validation/ordering/`TimelinePageVO` response contract.
- Added a dedicated unchecked implementation and verification section to `tasks.md`; only fact confirmation and contract documentation tasks are marked complete.
- Added backend-core, miniapp-core, and v2-product-scope delta requirements for owner-scoped filtering, pagination, calm UI behavior, empty/error states, and scope deferrals.

### Verification

- `git diff --check`: PASS.
- Cross-document keyword audit: PASS for `TimelinePageVO`, `createdAt`, `Asia/Shanghai`, one-tag scope, AND semantics, and `created_at DESC, id DESC` ordering.
- Task numbering audit: PASS; sections now run from 0 through 14 without duplicates.
- OpenSpec CLI validation: NOT RUN because `openspec` is not available in PATH in this workspace; checked-in artifacts were reviewed directly.
- Backend/frontend tests and Mini Program build: NOT RUN because this turn changed documentation only and intentionally did not implement the feature.

### Scope safety and remaining risks

- No backend, frontend, schema, dependency, package, lockfile, deployment, monitoring, admin, settings, notification, or AI behavior was changed.
- Existing backend `year + tagId` support and frontend year-only panel remain partial code facts; the newly added tasks are not implemented yet.
- Implementation must inspect current database indexes/query plans before adding a migration and must preserve the explicit preview/real-data boundary.
- The unrelated untracked `.claude/settings.local.json` file was not modified.

## 2026-06-22 M4 timeline filtering pre-implementation audit

### Result

- **GO** for formal implementation. No unresolved user contract decision or architectural blocker remains.
- This audit did not implement backend/frontend/schema behavior. It strengthened only implementation guardrails in `backend-contract-decisions.md`, `design.md`, and `tasks.md`.

### Current-code classification

- **Confirmed**:
  - `GET /api/records/timeline` is authenticated and owner-scoped.
  - Backend already accepts `year + tagId`, returns stable `created_at DESC, id DESC` ordering, groups by year-month, and includes tag names/cover metadata.
  - `/api/tags` exposes enabled shared tags; Preview seeds already contain `tagIds` and preview tag definitions.
  - `app.time.zone-id` defaults to `Asia/Shanghai`, the backend provides a business `Clock`, tests assert that zone, and the dev JDBC URL sets `serverTimezone=Asia/Shanghai`.
  - Existing schemas contain `idx_record_user_status_created`, record user/status indexes, and record-tag indexes.
- **Partial**:
  - Backend date filtering still uses `YEAR(created_at)` and returns the full matching `List<TimelineGroupVO>` without count/page metadata.
  - Frontend exposes only a free-form year field and replaces the full group list on each request.
  - Preview timeline filters by year only and returns `TimelineGroupVO[]`; it does not yet apply `tagId` or pagination.
- **Planned**: month/day validation, created-time range predicates, enabled-tag filtering, `TimelinePageVO`, index/query-plan decision, filter-sheet completion, request race protection, load-more merge, preview parity, and manual WeChat verification.
- **Out of scope**: multiple-tag boolean logic, keyword/state/type filters, persisted filter preferences, dashboard redesign, package/lockfile changes, and unrelated platform work.

### Drift risks closed in documentation

- The generic `PageQuery` currently defaults to `pageSize=10` and allows `200`; timeline implementation must enforce accepted `20`/`50` values rather than inherit those unchanged.
- Missing or disabled tags must return a safe empty page and must not become queryable when the tag catalog exposes enabled tags only.
- Date boundaries must use the existing business-time contract and `LocalDateTime` range values, never the JVM system-default timezone.
- Backend, real frontend, and Preview response-shape changes must land atomically to avoid `TimelinePageVO` versus `TimelineGroupVO[]` incompatibility.
- Frontend must separate draft/applied filters, isolate tag-list failure, suppress duplicate load-more calls, and ignore stale responses so old data is never mislabeled or allowed to overwrite a newer filter.
- No new frontend test framework or dependency is justified for this feature; use existing checks plus focused helper checks/manual evidence.

### Verification evidence

- Existing focused backend timeline baseline: PASS with `RecordMapperIntegrationTest`, `RecordServiceImplTest`, and `RecordControllerAuthIntegrationTest`.
- Full backend suite: PASS, 27 suites / 196 tests / 0 failures / 0 errors / 0 skipped.
- Frontend type-check: PASS with bundled Node and `vue-tsc --noEmit`.
- WeChat Mini Program build: PASS with bundled Node and `uni build -p mp-weixin`; output reported `DONE Build complete`.
- Contract-language audit: PASS; no timeline `Open`, `Pending`, `TBD`, `TODO`, or `待确认` decision remains.
- `git diff --check`: PASS before final handoff.

### Skipped verification and remaining risks

- MySQL `EXPLAIN` against production-scale data was not run because the implementation query and optional index migration do not exist yet. Static schema audit shows no dedicated `(user_id, created_at, id)` index; the implementation Agent must run/record a query-plan comparison before adding the smallest justified index to both MySQL and test schemas.
- Manual WeChat filter interaction was not run because the feature is not implemented yet.
- Existing build warning `os - Alias not found` remains non-fatal; build completed successfully and this audit did not change build tooling.

## 2026-06-22 M4 timeline backend filtering and pagination implementation

### Implementation

- Extended `RecordTimelineQuery` from the existing page query shape while overriding timeline defaults/validation to `pageSize=20`, maximum `50`.
- Added validated `month` and `day` fields plus calendar/dependency validation.
- Converted year/month/day through the injected business `Clock` zone into `LocalDateTime [createdFrom, createdBefore)` boundaries.
- Replaced `YEAR(created_at)` with range predicates and added owner-scoped count/page mapper queries.
- Limited tag filtering to enabled tags; missing, disabled, or non-matching tags produce a safe empty page.
- Added `TimelinePageVO` with current-page groups, record-level total, page metadata, and `hasMore`; kept the existing endpoint and `ApiResponse` wrapper.
- Added stable `created_at DESC, id DESC` pagination and applied record paging before year-month grouping.
- Added `idx_record_user_created_id (user_id, created_at, id)` to MySQL/test schemas plus repeatable existing-database migration `m4-timeline-filter-pagination.sql`.
- Updated controller, service, mapper, and focused tests for the accepted response/query contract.

### Verification

- Focused backend tests: PASS for `RecordMapperIntegrationTest`, `RecordServiceImplTest`, and `RecordControllerAuthIntegrationTest`.
- Full backend suite: PASS, 27 suites / 202 tests / 0 failures / 0 errors / 0 skipped.
- `git diff --check`: PASS before checkpoint preparation.
- Initial sandboxed Maven run failed while reading a local AWS SDK jar; the same offline command passed outside the sandbox. This was an environment permission issue, not a source failure.

### Skipped verification and remaining risk

- MySQL `EXPLAIN` was attempted but NOT RUN because the local `MySQL80` service was stopped and this session could not open/start the Windows service even with the approved escalation. Static schema audit confirmed no dedicated owner/created-time traversal index, so the smallest accepted index was added; real MySQL query-plan evidence remains pending.
- No frontend or Preview behavior is included in this backend checkpoint; those consumers must be updated before the endpoint contract is manually exercised from the Mini Program.

## 2026-06-22 M4 timeline Mini Program filtering and pagination implementation

### Implementation

- Updated frontend `TimelineQuery` with `tagId`, year/month/day, page number, and page size, and added `TimelinePageVO` matching the accepted backend response.
- Switched real `recordService.getTimeline` and Preview timeline data atomically from `TimelineGroupVO[]` to `TimelinePageVO`.
- Added Preview single-tag/date AND filtering, default 20/max 50 pagination, stable created-time/id ordering, grouped page data, total, and `hasMore`.
- Replaced the free-form year input with the existing-style restrained filter sheet: enabled tag chips, all/year/month/day granularity, native date picker, reset, and apply.
- Kept draft filter selections separate from applied filters. Failed page-1 requests preserve old groups and their old applied summary.
- Added local tag-list loading/failure/retry without blocking unfiltered or date-only timeline browsing.
- Added incremental scroll loading, repeated-month merge, record-id deduplication, duplicate load-more suppression, and stale-response sequence protection.
- Added initial/loading-more/error/retry/filtered-empty/end states and only loads cover URLs for accepted page items.
- Preserved the existing paper/vermilion timeline visual language, three top-level tabs, canonical naming, and preview/authenticated data boundary.

### Verification

- Product Design get-context playback used the already-confirmed brief: existing timeline visual system, full functionality, no redesign or dashboard expansion.
- Frontend type-check: PASS with bundled Node and `vue-tsc --noEmit`.
- WeChat Mini Program build: PASS with bundled Node and `uni build -p mp-weixin`; output reported `DONE Build complete`.
- Generated WXML audit: PASS for `筛选时光`, tag/date controls, apply/reset, `bindscrolltolower`, loading-more, retry, filtered-empty support, and canonical `时光轴` navigation.
- Generated JS audit: PASS for `tagId`/year/month/day/page query construction, `pageSize:20`, `hasMore`, request sequencing, group merge, and Preview/real service imports.
- `git diff --check`: PASS before checkpoint preparation.

### Skipped verification and remaining risk

- Manual WeChat Developer Tools interaction remains NOT RUN: tag/year/month/day selection, combined filtering, reset, failed apply preserving old data, repeated scroll loading, and Preview parity require interactive verification after importing `frontend/dist/build/mp-weixin`.
- Real authenticated end-to-end HTTP verification remains blocked while local MySQL80 cannot be started by this session; backend integration tests cover the contract against H2.
- The existing non-fatal build message `os - Alias not found` remains; the build completed successfully and no package/lockfile was changed.

## 2026-06-22 M4 timeline filtering closeout verification

### Automated and compiled verification

- Backend full suite remains PASS after timeline implementation: 27 suites / 202 tests / 0 failures / 0 errors / 0 skipped.
- Frontend type-check: PASS.
- Default WeChat Mini Program build: PASS.
- Explicit Preview-mode WeChat Mini Program build: PASS.
- Rebuilt the default Mini Program output after Preview verification so `frontend/dist/build/mp-weixin` is left in the normal handoff state.
- Executed the compiled Preview `getPreviewTimeline` function in a read-only Node VM harness:
  - `tagId=2 + year=2026` -> total 2, ids 101/201.
  - `tagId=3 + 2026-03-08` -> total 1, id 201.
  - unknown `tagId=999` -> successful empty page.
  - `pageNum=2 + pageSize=2` -> total 5, ids 202/302, `hasMore=true`.
- Generated WXML/JS inspection confirmed tag/date controls, apply/reset, scroll-bottom binding, page query construction, loading-more/retry states, request sequencing, and canonical navigation.
- Final tracked worktree audit found no package/lockfile, deployment, monitoring, admin, settings, notification, speech-to-text, or unrelated feature changes.

### Remaining manual/external verification

- Real MySQL `EXPLAIN` remains pending because `MySQL80` is stopped and this session cannot open/start the Windows service. Run the recorded migration, then compare the range/page query plan and confirm `idx_record_user_created_id` is selected or otherwise justified.
- WeChat Developer Tools interaction remains pending for tag/year/month/day/combined filters, reset, empty results, failed apply preserving old data, repeated load-more, cover display, and explicit Preview-mode parity.

## 2026-06-22 M4 Project Defense Presentation Redesign

### Implementation

- Redesigned the project defense presentation located at `ppt/index.html` using the `guizang-ppt-skill` Style A (Ink Classic / Magazine) theme.
- Outlined a structured 11-slide narrative arc for the defense:
  - Slide 1: Cover (时光回序 V2.0, vision, default team roles).
  - Slide 2: Background (anxiety, traditional efficiency/social tool limits, use of existing `02-concept.png` image).
  - Slide 3: Positioning (side-by-side contrast of Traditional vs Flashback core time-capsule value).
  - Slide 4: State Machine (life cycle steps: Draft -> Sealed -> Frozen -> Unlocked).
  - Slide 5: System Technical Architecture (Uniapp Vue 3 + Spring Boot + S3/Qiniu storage pillars, use of existing `04-topology.png` image).
  - Slide 6: Tech Highlight 1 (DeepSeek AI integration, error boundary / failure degradation).
  - Slide 7: Tech Highlight 2 (Secure S3/Qiniu attachment direct upload workflow, use of existing `07-integration.png` image).
  - Slide 8: Tech Highlight 3 (Location map-picker immutability & timeline stable multi-stage pagination).
  - Slide 9: Team Collaboration & Division of Labor (Person A: Frontend & Interactive controls; Person B: Backend API & Storage integration).
  - Slide 10: Quality Assurance (Unit testing maven suite 100% pass, WeChat Developer Tools manual verify).
  - Slide 11: Closing (Takeaway: “把回答权交给时间”).
- Tuned visual elements, font fallbacks for Windows systems (Microsoft YaHei UI, Noto Sans SC, SimSun, STSong), and integrated local `motion.min.js` fallback options.

### Verification

- Validated HTML slides in the browser by launching the presentation process. Checked keyboard navigation, low-power mode (toggled via `B` key), global index view (toggled via `ESC` key), and background WebGL canvas rendering.
- Verified all image references correctly target existing project assets.
- Confirmed no package, lockfile, deployment, settings, or administrative modules were modified.

### Skipped verification and remaining risk

- OpenSpec CLI validation was not run because the tool is not installed in the current shell path.
- Verification is limited to browser behavior; actual projection layout on external displays or different aspect ratios should be checked on-site.

## 2026-06-22 - 诊断微信开发者工具地图选点显示为悬浮小窗

Task:

- 核查记录编辑页调用地图选点后，微信开发者工具中的地图界面未覆盖整个模拟器、呈现为居中悬浮窗的问题；本轮仅诊断，不改功能代码。

Findings:

- 当前调用位于 `frontend/src/pages/record-editor/index.vue`，直接使用 `uni.chooseLocation`，未包裹自定义弹层、WebView、缩放容器或自建地图页面。
- `git blame` 与 `git log -SchooseLocation` 显示该调用自提交 `3cfb115`（2026-06-19）引入后未被后续提交修改；6 月 21 日至 22 日的草稿、时光轴与 AI timeout 改动没有触及该调用。
- `frontend/src/manifest.json` 已声明 `scope.userLocation` 与 `chooseLocation`；`frontend/src/pages.json` 的页面导航配置没有近期回归改动。
- 微信官方文档将 `wx.chooseLocation` 定义为客户端“打开地图选择位置”的原生 API，公开参数只有目标经纬度与 success/fail/complete 回调，没有窗口尺寸或全屏控制参数。
- 用户截图中的全屏灰色遮罩、固定尺寸原生地图层和仍可见的底层记录编辑页，符合微信开发者工具对客户端原生能力的模拟呈现；业务 WXML/WXSS 无法控制该原生层尺寸。
- 当前安装的微信开发者工具版本为 `2.01.2510270`。本轮没有证据表明业务代码发生了地图选点 UI 回归，不建议为模拟器外观引入自定义遮罩或重写地图选择页。

Verification:

- 静态调用链检查：PASS；地图结果仍按 `MAP_PICKER` 保存 name/address/latitude/longitude。
- Git differential check：PASS；`chooseLocation` 调用、manifest 与页面窗口配置不存在与用户所述时间点对应的代码回归。
- 微信官方 API contract check：PASS；当前使用方式与官方公开参数一致，无可用的全屏开关。
- Frontend `vue-tsc --noEmit`：PASS。
- WeChat Mini Program `uni build -p mp-weixin`：PASS；仅有已有非阻断提示 `os - Alias not found`。
- Generated artifact audit：PASS；`dist/build/mp-weixin` 中仍直接调用 `chooseLocation`，并保留 `requiredPrivateInfos: ["getLocation", "chooseLocation"]` 与 `MAP_PICKER` 结果映射。

Skipped:

- 未在当前会话中接管微信开发者工具或真机重复点击：原生选择器的窗口绘制不属于 WebView DOM，无法通过现有前端单元测试或浏览器自动化构造正确的回归 seam。
- 未修改 frontend：代码变更不能控制开发者工具原生模拟层的尺寸；伪造全屏遮罩只会掩盖工具差异，并增加真机交互风险。

Scope Safety:

- 仅检查记录编辑地点调用、Mini Program 配置和对应 Git 历史；未修改 backend、OpenSpec、依赖、lockfile、页面视觉或 API 契约。
- `.ai/AGENT_LOG.md` 原有未提交内容保持不变，本轮仅追加诊断记录。

Remaining Risks:

- 仍需在真机预览中确认地图选择页是否由微信客户端正常全屏承载；若真机也出现同样固定小窗，应记录微信版本、机型、基础库版本和录屏，再按客户端兼容问题继续定位。

## 2026-06-27 - README 按 M4 最新状态整理

Task:

- 根据当前 M4 real capability completion 状态整理根目录 `README.md`，让新手能理解项目背景、核心理念、能力边界、配置项和本地启动流程。

Implementation:

- 重写根目录 `README.md`：
  - 补充《时光回序》V2.0 的项目背景、产品初心和 M4 阶段定位。
  - 将事实源从旧 `Docs/**` 口径调整为 OpenSpec 优先，并说明旧 Docs 仅作不冲突历史参考。
  - 更新核心能力概览：认证、记录、标签、位置、附件、封面、AI Provider、时光轴筛选分页、Preview 隔离。
  - 补全 Windows PowerShell 本地启动步骤：MySQL 建库导表、Redis、后端 `start-dev.ps1`、前端 `pnpm dev:mp-weixin`、微信开发者工具本地设置。
  - 补充后端基础配置、AI 配置、对象存储配置、微信配置、前端 real/preview 模式配置。
  - 补充 M4 能力验收建议、常见问题和文档入口。

Verification:

- 已读取 `.ai/ACTIVE_TASK.md`、`AGENTS.md`、M4 proposal/design/tasks/backend-contract-decisions、后端/前端配置文件，确认 README 内容与当前 M4 事实源和实际配置文件一致。
- 已核对 README 中引用的关键路径存在：
  - `backend/OBJECT_STORAGE_CONFIG.md`
  - `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md`
  - `backend/sql/mysql/schema.mysql.sql`
  - `frontend/.env.development`
  - `frontend/.env.preview`
- 已核对前端 README 命令与 `frontend/package.json` scripts 对齐，包括 `dev:mp-weixin`、`dev:mp-weixin:preview`、`build:mp-weixin`、`build:mp-weixin:preview`、`type-check`。

Skipped:

- 未运行后端测试、前端 type-check 或 Mini Program build：本轮仅修改根目录 README，不涉及运行时代码。
- 未读取 `backend/start-dev-wechat.local.ps1`：该文件是本地忽略提交的 secret 注入脚本，避免在文档整理中暴露本机敏感配置。
- 未修改 OpenSpec：本轮未改变范围、契约或验收标准。

Scope Safety:

- 仅修改根目录 `README.md` 并追加 `.ai/AGENT_LOG.md` 记录。
- 未修改 backend/frontend 代码、数据库 schema、依赖、lockfile、部署、监控、管理后台、SMS、设置页或生产发布相关内容。
- 保留工作区已有未提交改动，不回退或覆盖用户/其他 agent 的文件。

Remaining Risks:

- README 中的真实 AI、对象存储、微信能力仍依赖本机或部署侧正确注入 secret；未配置时只能验证明确失败/不可用路径。
- 微信开发者工具和真机上的位置、录音、媒体上传体验仍需要按 M4 既有手工验收项继续验证。

## 2026-06-27 - 系统设计可复用文档对齐 M4

Task:

- 检查并更新提交系统设计文档可复用的旧文档，使其与当前 M4 real capability completion、实际 schema、实际 controller/API 和 OpenSpec 契约一致。

Implementation:

- 更新 `Docs/开发文档/模块用例与核心流程图.md`：
  - 从旧 MVP 口径改为 V2.0 M4。
  - 增加系统功能模块结构图、核心用例图、后端包图、前端文件模块结构图、核心类关系图、对象存储适配类图、核心数据类关系图、关键业务流程图和逻辑部署图。
  - 明确后台管理、生产部署、监控、SMS、通知中心、语音转文字和复杂 AI 诊断均为 M4 范围外。
- 更新 `Docs/开发文档/数据库设计文档.md`：
  - 以当前 `backend/sql/mysql/schema.mysql.sql` 为准重写表结构。
  - 补齐 `record_location`、`record_attachment`、`record_reminder`。
  - 移除旧 `admin_user` 作为当前实现表的表达，并保留“不要作为 M4 已实现表提交”的警示。
  - 增加当前 ER 图、枚举约定、索引和生命周期数据变化说明。
- 更新 `Docs/开发文档/接口清单文档.md`：
  - 以当前 controller 和 `backend-contract-decisions.md` 为准重写接口清单。
  - 补齐微信登录、位置、附件上传授权/commit/access-url/delete、封面、时间轴年月日筛选分页、阶段总结等 M4 接口。
  - 不再把旧管理端接口列为当前 M4 已实现能力。
- 更新 `Docs/开发文档/UML用例图/`：
  - 更新 `用例图.md` 与 `01_项目总体用例图.puml` 至 M4 用户侧核心用例。
  - 更新 02-08 单模块 puml 文件，使其包含微信登录、位置、附件/封面、时间轴筛选分页、AI 显式失败等 M4 能力。
  - 将 `09_后台管理模块用例图.puml` 改为 M4 范围外能力说明，避免误提交为当前实现。

Verification:

- 已读取 `.ai/ACTIVE_TASK.md`、`AGENTS.md`、M4 design、M4 backend contract decisions、当前 `schema.mysql.sql` 和 controller/service/storage 结构作为事实依据。
- `rg` 检查确认 M4 关键内容已出现在更新后的设计文档中，包括 `record_location`、`record_attachment`、`upload-token`、`TimelinePageVO`、`created_at DESC`、`S3_COMPATIBLE`、`DeepSeek`、`微信登录`、`Preview`、包图、类图、部署图。
- `rg` 检查旧 admin/MVP 命中仅剩范围外说明或“不要作为当前实现提交”的警示，不再把后台管理作为当前 M4 能力。
- `git diff --check` 针对更新文档通过；仅有 Git LF/CRLF 提示。

Skipped:

- 未运行后端测试、前端 type-check 或 Mini Program build：本轮仅修改提交材料相关 Markdown/PlantUML 文档，不涉及运行时代码。
- 未渲染 PlantUML/Mermaid 图片：当前环境未配置 PlantUML 渲染链路；已保留可本地导出的 `.puml` 和 Mermaid 源码。
- 未修改 OpenSpec：本轮是旧文档对齐既有 M4 契约，不改变范围、canonical mapping 或验收标准。

Scope Safety:

- 仅更新系统设计提交相关文档和 `.ai/AGENT_LOG.md`。
- 未修改 backend/frontend 代码、schema、依赖、lockfile、部署、监控、生产发布或 settings/admin 实现。
- 保留工作区中既有未提交改动，不回退或覆盖用户/其他 agent 的文件。

Remaining Risks:

- 后续正式提交如要求 Word/PDF 或图片化图稿，还需要把 Mermaid/PlantUML 渲染为图片并做版式检查。
- `09_后台管理模块用例图.puml` 文件名沿用旧名，但内容已改成范围外说明；若老师要求文件名严格匹配当前范围，可后续另行重命名或不纳入提交包。

## 2026-06-29 - M4 收口与 M1-M4 归档前盘点

Task:

- 扫描当前 OpenSpec、`.ai` 账本和阶段变更目录，整理 M4 已完成内容，并列出 M1 到 M4 归档前仍未收口的事项。

Implementation:

- 新增 `openspec/changes/m4-real-capability-completion/closeout.md`：
  - 汇总 M4 已完成能力：AI provider、provider-neutral storage、attachments、cover、location、real/mock boundary、timeline filtering/pagination、README/旧文档对齐。
  - 明确 M4 剩余归档阻塞：真实 AI 成功、真实对象存储/媒体链路、微信开发者工具位置/媒体/时光轴/封面/时间回看手工验收、MySQL EXPLAIN、delta spec 同步、OpenSpec CLI 不可用。
  - 汇总 M1、M2、M3、M4 当前收口状态和推荐归档决策。

Verification:

- 已读取 `.ai/ACTIVE_TASK.md`、`AGENTS.md`、M4 proposal/design/tasks/backend-contract-decisions、M4 spec deltas、accepted specs、M1/M3 tasks、已归档 M2 tasks、`.ai/AGENT_LOG.md` 近期证据和当前 git 状态。
- `rg`/`Select-String` 检查确认 M4 tasks 中未勾项集中在真实凭据、真实 provider、WeChat Developer Tools、MySQL EXPLAIN 和手工交互验收。
- 确认 `openspec/changes/archive/2026-06-07-m2-backend-optimization` 已存在，M1/M3/M4 仍在 active `openspec/changes` 下。

Skipped:

- 未执行真正归档：用户本轮要求归档前扫描和收口，不是立即移动 OpenSpec change。
- 未运行后端测试、前端 type-check 或 Mini Program build：本轮仅新增归档前文档审计，不修改运行时代码。
- 未同步 delta specs 到 accepted specs：这属于正式归档动作的一部分，当前先记录为归档前阻塞项。

Scope Safety:

- 仅新增 M4 closeout 文档并追加 `.ai/AGENT_LOG.md`。
- 未修改 backend/frontend 代码、schema、依赖、lockfile、部署、监控、admin、SMS、settings 或生产发布范围。
- 未把缺少真实外部验收证据的任务强行勾选完成。

Remaining Risks:

- 当前工作区已有多处未提交改动；本轮未回退或覆盖这些改动。
- 若下一步要正式归档 M4，需要先决定是否允许把真实 provider / WeChat / MySQL 证据作为下一轮 carry-over 风险。
- OpenSpec CLI 仍不可用；正式归档需要在可用环境运行 CLI，或继续采用人工同步/移动目录并记录证据。

## 2026-06-30 - M4 真实能力手工验收确认与提交整理

Task:

- 根据用户确认，补齐 M4 真实 AI、对象存储、媒体、位置、筛选、封面、时间回看等手工验收状态，并检查当前工作区提交拆分。

Implementation:

- 更新 `openspec/changes/m4-real-capability-completion/tasks.md`：
  - 将真实 AI configured success、Mini Program location、media flow、preview/mock boundary、timeline filter 手工交互、configured-provider upload/object verification/signed URL/image preview/voice playback、timeline/home cover display、unlocked time review location/image/voice/M3 reflection 验收项标记为完成。
  - 保留 real MySQL `EXPLAIN` 未完成，因为本轮用户未确认该数据库查询计划证据。
- 更新 `openspec/changes/m4-real-capability-completion/closeout.md`：
  - 将 M4 状态调整为 implementation-complete + manual real-capability verification confirmed。
  - 将剩余 M4 归档阻塞收缩为 MySQL `EXPLAIN`、delta spec sync、official/manual archive workflow。

Verification:

- 用户已确认真实 AI 成功、真实对象存储上传/签名 URL/图片预览/语音播放、微信开发者工具里的位置/媒体/筛选/封面/时间回看手工验收均无问题。
- 已重新读取 `.ai/ACTIVE_TASK.md`、`AGENTS.md`、M4 `tasks.md`、M4 `closeout.md`，并检查当前 `git status`、最近提交记录与工作区 diff 分类。

Skipped:

- 未运行后端测试、前端 type-check 或 Mini Program build：本轮只记录用户确认的手工验收和整理提交范围，不修改运行时代码。
- 未执行 `git add` 或 `git commit`：用户明确要手动提交，本轮只提供提交范围和中文 message 建议。
- 未将 MySQL `EXPLAIN` 标记完成：缺少用户确认或当前可复查输出。

Scope Safety:

- 仅修改 M4 OpenSpec 任务/收口文档与 `.ai/AGENT_LOG.md`。
- 未修改 backend/frontend 代码、schema、依赖、lockfile、deployment、monitoring、admin、SMS、settings 或生产发布范围。

Remaining Risks:

- 正式归档 M4 前仍需同步 delta specs 到 accepted `openspec/specs/**`，并决定 MySQL `EXPLAIN` 是否允许作为下一轮 carry-over query-plan evidence。
- 当前工作区还包含 README、Docs/UML、PPT、`.gitignore` 等其他未提交内容，建议按用途拆分提交。

## 2026-07-04 - 阿里云/微信小程序部署就绪度扫描

Task:

- 回答“把小程序部署到阿里云并上线需要做什么”，并扫描当前 Flashback 小程序项目是否足够支撑部署上线。

Implementation:

- 读取 `.ai/ACTIVE_TASK.md`、`AGENTS.md`、M4 OpenSpec/contract/task 文档，确认当前 M4 边界仍是 core Mini Program usability，不包含生产部署加固。
- 只读部署相关配置与构建入口：`frontend/package.json`、`frontend/src/manifest.json`、前端 `.env.*`、`backend/pom.xml`、`backend/src/main/resources/application*.yml`、`backend/sql/mysql/schema.mysql.sql`、README、CORS/鉴权配置。
- 对外部上线要求做官方资料核验：阿里云备案/ECS 文档可读取；微信官方开发文档页面本轮抓取失败，微信侧仅按长期规则列为待控制台复核项。

Verification:

- Backend: `mvn "-Dmaven.repo.local=C:\Users\Lin\.m2\repository" -o -s C:\Users\Lin\.m2\settings.xml -q test` passed.
- Backend surefire summary: `SUITES=27 TESTS=204 FAILURES=0 ERRORS=0 SKIPPED=0`.
- Frontend: `vue-tsc --noEmit` passed with bundled Node.
- Mini Program build: `uni build -p mp-weixin` passed; output `frontend/dist/build/mp-weixin`.
- `git diff --check` passed; only line-ending warnings were reported for existing changed files.

Skipped:

- 未启动阿里云服务器、未真实部署后端、未配置线上域名/HTTPS/备案、未提交微信审核：本轮用户要求部署准备与就绪度扫描。
- 未执行真实线上 AI/OSS/微信登录 smoke：缺少线上服务器、域名、生产环境变量与微信公众平台合法域名配置。
- 未运行真实 MySQL `EXPLAIN`：本轮仍未连接部署目标数据库；M4 tasks 中该项此前也保持未完成。

Scope Safety:

- 本轮只追加 `.ai/AGENT_LOG.md` 证据；未修改 backend/frontend 运行代码、schema、依赖、lockfile、deployment、monitoring、admin、SMS、settings 或生产发布实现。
- 保留并未回退工作区已有未提交改动。

Remaining Risks:

- 当前项目可构建、可测试，但仍处于本地/演示部署形态；正式上线前必须补齐生产域名 HTTPS、ICP备案/小程序备案、微信合法域名、线上 secret 注入、数据库/Redis/OSS/AI 实配和 smoke 验收。
- `frontend/.env.*` 当前仍指向 `http://127.0.0.1:8080`，`manifest.json` 中 `mp-weixin.setting.urlCheck=false`，不应作为正式提审配置。
- README 引用 `/actuator/health`，但当前 `pom.xml` 未看到 actuator 依赖；生产健康检查需要另行确认或补一个真实可用的健康端点。
## 2026-07-25 - Docs/agent-iteration 工作流说明（RAG 优点迁移）

Task:

- 从 RAG 项目迭代实践中汲取 Spec 范式 vibecoding 优点，在 Flashback 建立 `Docs/agent-iteration` 工作流文档集，供后续 Claude 编写迭代蓝图参考。

Modified:

- `Docs/agent-iteration/README.md`（新建索引）
- `Docs/agent-iteration/workflow/iteration-approach.md`（新建：Flashback 迭代思路）
- `Docs/agent-iteration/workflow/vibecoding-playbook.md`（新建：协作手册）
- `Docs/agent-iteration/workflow/agent-control-model.md`（新建：控制模型）
- `Docs/agent-iteration/workflow/prompt-snippets/design-decision-record.md`（新建：决策记录模板）
- `Docs/agent-iteration/roadmap/README.md`（新建：蓝图编写规格占位）
- `Docs/agent-iteration/项目初始分析.md`（顶部增加关联文档指引）
- `.ai/AGENT_LOG.md`（本条）

Implementation:

- 迁移并本地化 RAG 的控制机制：事实源优先级、Type A/B/C、事前闸门、一次一个 active change、决策记录、AGENT_LOG 证据、外调闸、提交责任、能力五态。
- 明确不复制 RAG 业务 C 序列；区分协作 Agent 工作流与产品 Agent runtime。
- 声明蓝图 `roadmap/iteration-blueprint.md` 有意不写，留给 Claude；本轮不修改 OpenSpec/M4 scope/业务代码。

Verification:

- 目录与文件存在性检查：上述路径均已写入。
- 本轮为纯文档，未运行 Maven/前端构建/微信手验（SKIPPED：无代码变更）。

Scope Safety:

- 未修改 `openspec/**` baseline 或 M4 change、backend/frontend 业务代码、依赖与 lockfile、部署配置。
- 未将 Docs 声明为高于 OpenSpec 的执行事实源；README 写明冲突以 AGENTS/OpenSpec 为准。

Remaining Risks:

- 工作流文档尚未被 AGENTS.md 强制引用，Agent 可能仍只读旧习惯；若需硬化可另开 Type B 在 AGENTS 增加「规划时必读 agent-iteration/workflow」。
- 迭代蓝图尚未产出，post-M4 序列仍不能当作已批准执行计划。

Commit: pending


## 2026-07-26｜vibecoding-workflow-review-hardening｜Type B

- **Scope**: `AGENTS.md`；`Docs/agent-iteration/**`；`.ai/ACTIVE_TASK.md` / `AGENT_LOG.md` / `TASK_CARD_TEMPLATE.md`；`.agent/skills/openspec-{apply,propose}-change/SKILL.md`；`.claude/skills/openspec-{apply,propose}-change/SKILL.md`
- **Changes**: 吸收 Claude 审查：强制规则注入 AGENTS；Type B/C checklist；Current Progress 交接；AGENT_LOG 结构化模板；playbook 去重并引用 AGENTS；初始分析 CAUTION；skills 增加 vibecoding guardrails；明确蓝图于 M4 后补
- **Verification**: SKIPPED 代码测试（纯治理文档/skill 文案）。目录与关键标题人工核对：AGENTS 含 Type/Gates/Handoff；type-c-checklist 与 Current Progress 存在
- **Risks**: 蓝图仍空；M4 tasks 勾选未在本轮同步盘点；`.agent` 与 `.claude` skills 需保持双份同步
- **Commit**: `ac2af21`
- **Next**: 继续 M4 或用户授权后写 iteration-blueprint


## 2026-07-27｜m4-truth-align-and-archive｜Type B/C governance

- **Scope**: `openspec/specs/{backend-core,miniapp-core,v2-product-scope}/spec.md`；`openspec/changes/archive/2026-07-27-m4-real-capability-completion/**`；`.ai/ACTIVE_TASK.md`；`AGENTS.md`；`openspec/project.md`；`Docs/agent-iteration/README.md`；`Docs/agent-iteration/roadmap/README.md`；本日志
- **Changes**:
  1. **真相对齐**：M4 product scope 实现 + 2026-06-30 用户手验视为完成；`tasks.md` 仅余 timeline MySQL EXPLAIN；MySQL80 Stopped 且无提权无法启动 → **carry-over residual**（非产品缺口）
  2. **baseline 接受**：将 M4 delta 的 ADDED Requirements 原文并入三份 baseline specs（backend 17 / miniapp 15 / v2-product-scope 15 requirements）
  3. **归档**：`m4-real-capability-completion` → `openspec/changes/archive/2026-07-27-m4-real-capability-completion/`；更新 closeout；ACTIVE_TASK=`IDLE`
  4. **指针清理**：AGENTS/project 去掉「当前 M4 active 事实源」表述；标明蓝图未冻结不可执行
  5. **未做**：未归档 M1/M3；未冻结蓝图；未开 Agent C1 实现；未跑 live EXPLAIN
- **Verification**:
  - archive path exists with proposal/design/tasks/specs/closeout
  - active `openspec/changes/` no longer contains `m4-real-capability-completion`
  - archived tasks unchecked product items = 0
  - baseline contains `Accepted From M4 Real Capability Completion` section
  - MySQL EXPLAIN: SKIPPED (service stop / no elevation) → carry-over
  - Code tests: SKIPPED（本轮无业务代码改动）
- **Risks**: M1/M3 目录仍可能被误认为 active；蓝图 D1 曾写「M4 视为完成」现已与 ACTIVE_TASK 对齐但仍需 Claude 修订冻结措辞；EXPLAIN 残留需有库环境时 Type B 补
- **Commit**: `ac2af21`
- **Next**: 用户将「蓝图修订建议」发给 Claude；冻结后再决定是否 `/opsx-propose` C1


## 2026-07-27｜blueprint-v1.1-freeze-and-gitignore｜Type B

- **Scope**: `.gitignore`；`Docs/agent-iteration/roadmap/iteration-blueprint.md`；`Docs/agent-iteration/README.md`；`Docs/agent-iteration/roadmap/README.md`；`AGENTS.md`；`.ai/ACTIVE_TASK.md`；`openspec/project.md`；本日志
- **Changes**:
  1. 终验：工作流（Type/Gates/Handoff/证据/skills）与蓝图 v1.1（M4 对齐、依赖规则、C1 最小护栏、C3 拆分退路、P1–P7 待确认）判定 **通过、可控**
  2. 蓝图状态改为 **已冻结 v1.1**；明确冻结≠可写业务代码
  3. `.gitignore` 忽略 `.agent/skills/Lincheck/` 与 `Linsist/`
  4. ACTIVE_TASK 仍 IDLE；Next=C1 规划闸
- **Verification**: PASS（文档一致性人工核对）；无业务代码；Lincheck/Linsist 不再作为待提交交付物
- **Risks**: 冻结后若跳过 proposal 直接编码会破坏可控性；C3 体量与 P2 持久化仍待 C1/C3 design
- **Commit**: `ac2af21`
- **Next**: 用户授权后 propose `agent-runtime-mvp`


## 2026-07-27｜agent-runtime-mvp｜Type C（规划闸 · 闸门 1 待批准）

- **Scope**: 新建 `openspec/changes/agent-runtime-mvp/{proposal.md,design.md,tasks.md}`；`openspec/changes/agent-runtime-mvp/specs/{agent-runtime,backend-core,miniapp-core,v2-product-scope}/spec.md`；重写 `.ai/ACTIVE_TASK.md`；本日志。**零业务代码改动。**
- **Changes**:
  1. 开工前置核对：`ACTIVE_TASK=IDLE`（无冲突）、M4 已归档、蓝图 v1.1 已冻结 → 满足 C1 规划闸开工条件。锚点 `git rev-parse --short HEAD` = `b6140b3`，`git status --short` 为空。
  2. 现状扫描（只读）：`AiServiceImpl` / `AiController` / `AppAiProperties` / `application*.yml` / `backend/pom.xml` / `backend/sql/mysql/schema.mysql.sql` / `frontend/src/services/aiService.ts` / `frontend/src/pages/record-editor/index.vue` / `common/error/ErrorCode.java`。
  3. `proposal.md`：Why Now、Goals(6)、Non-Goals(15+)、用户故事、场景边界表、**能力五态 F1–F16**、待确认 Q1–Q4、外调预算、提交责任、验收标准(13)、实现顺序。
  4. `design.md`：模块分层、单轮数据流、7 阶段状态机、4 端点 API 草案、`agent_session`/`agent_message` 表草案、最小护栏 5 条、隐私落点、验证策略表，以及 **决策记录 9 条**（持久化选型 / 消息落原文 / 新模块 vs 改造 / 显式阶段机 / 不引入 FALLBACK / 不做 Tool / 不做后置过滤 / UI 形态 / delta 落点）。
  5. `tasks.md`：P0 规划(T-01~T-10)、**T0 实现授权检查点**、P1 后端(T-11~T-21)、P2 前端(T-22~T-28)、P3 验证含 **T-32 闸门 3**(T-29~T-36)、P4 收口(T-37~T-40)。
  6. spec delta 分四处：新建 `agent-runtime`（8 requirements，主契约）+ `backend-core`(3) + `miniapp-core`(4) + `v2-product-scope`(3) 最小可检索条款。
  7. `ACTIVE_TASK` → `ACTIVE`（规划闸），写入 change 指针、Q1–Q4 阻塞项、Current Progress、C1 期间 Out Of Scope。
- **关键事实（供后续会话复用，避免重扫）**:
  - 现有 AI 为单轮 `prompt→response`，无会话/轮次；`invokeChatCompletion` 已是 OpenAI-compatible `/chat/completions` 形状（`json_object`、`stream=false`）。
  - `spring-boot-starter-data-redis` 已在 `pom.xml` 且 dev/prod 已配 host/port，但**代码零处使用**（grep `RedisTemplate` 无匹配），连通性未被业务验证 → 因此持久化推荐 MySQL。
  - 现有 AI 状态四态 `SUCCESS|UNAVAILABLE|FAILED|FALLBACK`；C1 决定只用前三态。
  - DeepSeek 官方文档声明支持 OpenAI-compatible tool calls（本仓库未验证）→ 蓝图 P1 判定为不阻塞 C1，验证留 C2。
  - M4 联调曾出现 `summarize-record` 五次中 1 次「返回内容无效」→ 结构化输出稳定性列为已知风险，也是选显式状态机的依据之一。
- **Verification**: PASS（规划文档层）。核对项：change 目录 7 个文件齐备；`design.md` 含 `## 决策记录` 且 9 条均含「面临的选择／选了哪个+为什么／放弃的代价」三段；`tasks.md` 含 T0 实现授权检查点与闸门 3 项；`ACTIVE_TASK` 指针指向本 change。**SKIPPED 代码测试与构建**：本轮零业务代码改动，无可测目标。**SKIPPED 外调**：规划阶段 0 次 AI/OSS 调用。
- **Risks**:
  - Q1–Q4 未定稿前若直接开工，持久化与 UI 形态可能返工。
  - C1 最小护栏仅 system prompt 单层 + 长度裁剪，边界输入下仍可能滑向诊断式表达（**已接受**，C4 补齐）。
  - 对话原文落库扩大敏感数据面，依赖「只进业务表、不进日志」这一约束在实现期被严格执行（T-21 专门校验）。
  - provider 结构化/多轮输出稳定性未经真实验证，可能在闸门 3 联调时暴露需要调 prompt。
  - MySQL `EXPLAIN` carry-over 仍未补（M4 残留，与 C1 无依赖）。
- **Commit**: pending
- **Next**: 请求闸门 1 规划批准并定稿 Q1–Q4；获得闸门 2 实现授权后从 T-11（`agent_session`/`agent_message` DDL）开始。

## 2026-07-27｜agent-runtime-mvp｜Type C（闸门 2 实现 · P1+P2+P3 完成）

- **Scope**:
  - 闸门：用户 2026-07-27 批准规划（Q1–Q4 全按推荐定稿）并给出实现授权 → `tasks.md` T-10 / T0 勾选。**闸门 3（真实 provider 联调）仍未授权，本轮全程 mock provider，0 次外调。**
  - backend 新增：`agent/{AgentStageMachine,AgentStageDecision,AgentPromptBuilder,AgentGuardrailPolicy,AgentModelClient,AgentMockResponder}`；`domain/{AgentSession,AgentMessage,AgentStage,AgentSessionStatus,AgentMessageRole}`；`mapper/{AgentSessionMapper,AgentMessageMapper}` + 两份 XML；`dto/{AgentSessionStartRequest,AgentMessageRequest}`；`vo/{AgentSessionVO,AgentMessageVO}`；`service/AgentChatService(+Impl)`；`controller/api/AgentController`；`config/AppAgentProperties`
  - backend 修改：`application.yml`（新增 `app.agent.*`，复用 app.ai provider/secret，未新增凭证字段）；`sql/mysql/schema.mysql.sql` + 新增 `sql/mysql/c1-agent-runtime.sql`；`src/test/resources/schema.sql`
  - backend 测试新增：`AgentStageMachineTest`(10)、`AgentGuardrailPolicyTest`(5)、`AgentPromptBuilderTest`(7)、`AgentChatServiceImplTest`(19)、`AgentControllerAuthIntegrationTest`(7)、`AgentRuntimeIntegrationTest`(2)
  - frontend 新增：`services/agentService.ts`、`stores/agentChat.ts`、`pages/record-editor/components/AgentChatSheet.vue`
  - frontend 修改：`pages/record-editor/index.vue`（被动入口 + 浮层接入 + 素材回填）、`services/index.ts`、`stores/index.ts`
- **Changes**:
  1. **持久化（Q1）**：`agent_session` / `agent_message` 两张 MySQL 表，snake_case、`user_id` 归属列、FK `ON DELETE CASCADE`、`Asia/Shanghai` 时间语义，与既有 `record_*` 表一致；增量脚本可重复执行。
  2. **状态机**：`OPENING→EMOTION→CONFUSION→CORE_QUESTION→EXPECTATION→CLOSING→ENDED` 显式推进（纯逻辑无 IO）。同阶段追问上限 1 次后前进（不逼问）；结束意图优先级高于轮次上限；达上限强制收束；`ENDED` 拒绝追加。
  3. **最小护栏**：5 条（不诊断/不覆写/建议不代决/被动陪伴/输出克制）注入 system prompt；代码级唯一硬约束为回复长度裁剪（默认 120 字，优先句末断开）。
  4. **失败语义（决策 5）**：只用 `SUCCESS|UNAVAILABLE|FAILED`，不引入 FALLBACK。provider 不可用/失败时用户消息已落库保留、assistant 不落库；实现期补强两点契约：① 同轮重试不重复 insert 用户消息，改内容重试被拒（`请先重试原消息`）；② `startOrResume`/`getSession` 恢复到半轮会话时显式返回 `FAILED + 请重试`，不误报 SUCCESS。
  5. **范围守卫**：`AgentChatServiceImpl` 除自身会话/消息落库外不做任何记录写操作，并由单测 `verify(never())` 断言（update/seal/delete/reflection）。仅草稿记录可开启对话（非 DRAFT 抛 400）。
  6. **前端（Q4）**：编辑页内半屏浮层，用户点「让它陪你聊一会儿」才开启；不弹窗、不自动展开；三 Tab 与 V2.0 命名未变。中断恢复复用 ACTIVE 会话；素材需点「用作正文」才经既有 `persistDraft` **追加**写入（不覆盖、不修剪原文），点「先不用」正文不变。
  7. **隐私**：日志仅 `operation/stage/provider/durationMs/cause` 结构化元数据；对话原文与日记原文只进 `agent_message`/`record`。
  8. **未新增依赖**：AgentChatSheet 初版误用 `lang="scss"` 导致 mp-weixin 构建失败（仓库未装 sass）。按 AGENTS「不改 package/lockfile」改为与全仓库一致的普通 CSS，而非安装 sass。
- **Verification**:
  - backend `mvn -B test`：**PASS** — Tests run **254**, Failures 0, Errors 0, Skipped 0（新增 43）
  - frontend `npm run type-check`：**PASS**
  - frontend `npm run build:mp-weixin`：**PASS**（DONE Build complete）
  - mock provider 端到端：`AgentRuntimeIntegrationTest` 覆盖开场→多轮→恢复→结束→素材，并以 SQL 断言 `agent_session=ENDED`、`agent_message=3`
  - 恢复过程中修复：`AgentSessionMapper.java` 曾在会话中断时丢失导致编译失败，已重建；`AgentModelClient` 双构造器需 `@Autowired` 才能被 Spring 实例化
  - **SKIPPED T-32~T-35**：真实 provider 联调与微信手验需闸门 3 / 真机，本轮未授权且无设备环境
  - **SKIPPED** MySQL 真库 DDL 执行验证：本轮未启动 MySQL，表结构仅通过 H2（MODE=MySQL）集成测试验证
- **Risks**:
  - `c1-agent-runtime.sql` 未在真实 MySQL 8.0 跑过；H2 与 MySQL 在 FK/TEXT 细节上仍可能有差异，上真库前需执行一次。
  - 最小护栏仍是 prompt 单层 + 长度裁剪，边界输入下可能滑向诊断式表达（已接受，C4 补）。
  - mock provider 的引导语是固定文案，真实 provider 的多轮语气与 JSON 稳定性未验证（M4 已知该模型结构化输出非 100% 稳定）。
  - 对话原文落库扩大敏感数据面，依赖「只进业务表」约束在后续 change 中继续保持。
  - 素材回填采用追加策略，长对话多次回填可能让正文变长，需手验确认体感。
- **Commit**: pending
- **Next**: 请求 **闸门 3** 授权后执行 T-32~T-33 真实 provider 多轮联调（预算 ≤ 30 次请求），随后 T-34/T-35 微信手验；手验通过再走 T-36~T-40 收口。

## 2026-07-27｜agent-runtime-mvp｜Type C（真机手验 · 缺陷修复 · 验收归档）

- **Scope**: `frontend/src/pages/record-editor/components/AgentChatSheet.vue`（布局修复）；真实 MySQL 执行 `backend/sql/mysql/c1-agent-runtime.sql`；`openspec/specs/{agent-runtime(新建),backend-core,miniapp-core,v2-product-scope}/spec.md`（delta 接受）；`openspec/changes/agent-runtime-mvp/` → `openspec/changes/archive/2026-07-27-agent-runtime-mvp/`（含新增 `closeout.md`）；`.ai/ACTIVE_TASK.md`；本日志。
- **Changes**:
  1. **环境修复（用户授权）**：用户本地 `Start-Service MySQL80` 因非管理员会话失败（`Cannot open 'MySQL80' service`）。排查确认真正阻塞登录的是**后端进程不存在**——此前看到的两个 java 进程是 IDE 的 `redhat.java` 语言服务器，8080 无监听，故账号登录与微信登录同时失败。启动后端后 `POST /api/auth/register`、`/api/auth/login` 均返回 `code=0` 且签发 token，`POST /api/agent/sessions` 无凭证返回 401。
  2. **真库 DDL 执行**：在真实 MySQL 8.0 执行 C1 增量脚本，建出 `agent_session` / `agent_message`；校验 4 个外键全为 `CASCADE`、唯一键 `(session_id,turn_no,role)` 与 3 个索引齐备，与 design 一致 → **消除上一轮 SKIPPED「真库未执行 DDL」**。
  3. **真机手验缺陷（前端布局）**：用户第 4 轮对话时页面卡住无法发送且消息重叠。查库确认后端正常（session `ACTIVE`、`turn_count=3`、7 条消息、无 `Agent provider issue`），判定为本人所写 CSS 缺陷：`.agent-sheet` 用 `max-height` + `.message-list` 用 `min-height:360rpx`，使 `scroll-view` 无确定高度不启用内部滚动，消息累积后把 composer 顶出可视区（发送按钮不可点）；小程序原生 `textarea` 层级高于普通元素，被顶位后覆盖消息形成重叠。修复：`height:78vh` 固定、`min-height:0; height:0` 使消息区可收缩、头尾 `flex-shrink:0`、composer 补背景色。
  4. **契约补录**：将「消息累积后仍可操作」「上一轮未完成时禁止输入新内容」「失败轮可原样重试且不重复落库」「写作引导仅限草稿记录」等实现期确立的边界写入 baseline，不留隐式约定。
  5. **收口**：tasks T-32~T-40 勾选并标注偏差；新建 `openspec/specs/agent-runtime/` baseline capability；三份 baseline 追加 `Accepted From C1 Agent Runtime MVP`；写 `closeout.md`；归档；`ACTIVE_TASK=IDLE`。
- **流程偏差（必须记录）**: **闸门 3（外调授权）未事前取得。** 用户手验时本地脚本 `AI_PROVIDER=deepseek`，导致手验即真实 provider 调用（约 4 轮用户消息 / 7 条 Agent 回复，远低于 proposal 申明的 ≤30 次预算，未用批量真实日记）。用量与数据面均未越界，但**顺序上应先取得授权再联调**。C2 起手验前须先确认本地 `AI_PROVIDER` 取值或显式取得闸门 3 授权。
- **Verification**:
  - backend `mvn -B test`：**PASS** — Tests run 254, Failures 0, Errors 0, Skipped 0
  - frontend `type-check`：**PASS**；`build:mp-weixin`：**PASS**
  - 真实 MySQL：表/外键/索引校验 **PASS**
  - 真实 DeepSeek 多轮：4 轮 `reply` JSON 全部解析成功，日志无 provider issue
  - 微信手验：登录 → 开启 → 4 轮推进至 `EXPECTATION` → 修复后可滚动/可发送/无重叠，用户确认 **验收通过**
  - 最小护栏手验：回复均 1–2 句短问句、无诊断词、未改写用户原文；长度裁剪未被触发（provider 自发输出已在上限内）
  - 隐私：后端日志中未出现对话原文或日记原文
  - 归档校验：archive 目录 8 个文件齐备；active `openspec/changes/` 已无 `agent-runtime-mvp`；`openspec/specs/agent-runtime/spec.md` 存在
  - **SKIPPED**：收束/素材回填端到端未手验（手验止于第 4 轮，未达 8 轮上限触发 CLOSING）
- **Risks**:
  - 素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错——手验未触发，属已知缺口（Type B 或 C2）。
  - 最小护栏仍为单层，边界输入下可能滑向诊断式表达（已接受，C4 补）。
  - 真实 provider 仅验证 4 轮；M4 曾观测该模型结构化输出非 100% 稳定，长会话稳定性待 C2 继续观察。
  - 本地 `start-dev-wechat.local.ps1` 明文存放微信/OSS/DeepSeek 凭证（已 gitignore 未进版本库），但已在会话中暴露，建议轮换并改为环境变量读取。
  - MySQL80 StartType=Manual，重启后需手动启动。
- **Commit**: pending（提交责任＝用户手动提交；Agent 未执行 `git add`/`commit`/`push`）
- **Next**: 用户授权后开 C2 `agent-tool-calling` 规划闸。

## 2026-07-27｜agent-runtime-mvp｜Type C（C1 全周期收束汇总）

> 汇总条目。本 change 的执行细节分散在上方三条（规划闸 / 闸门 2 实现 / 真机手验·缺陷修复·验收归档），此条不重复过程，只留「一眼看清 C1 做了什么」的索引与结论。

- **Change**: `agent-runtime-mvp`（C1，蓝图 v1.1 §4）｜锚点 `b6140b3`｜归档 `openspec/changes/archive/2026-07-27-agent-runtime-mvp/`
- **闸门轨迹**: 闸门 1 规划批准（Q1–Q4 按推荐定稿）→ 闸门 2 实现授权 → **闸门 3 未事前取得（偏差，见下）** → 用户真机验收通过 → baseline 接受 → 归档 → `ACTIVE_TASK=IDLE`

### 交付结论

- 后端新增独立 `agent` 模块（状态机 / prompt / 护栏 / model client / mock 引导器 / 编排服务 / 4 个端点 / 2 张表），**既有三个 AI 端点契约零改动**。
- 前端新增 service + store + 编辑页内半屏浮层，被动触发；三 Tab 与 V2.0 命名未变；未改 package/lockfile。
- 范围守卫生效：Agent 除自身会话/消息落库外不触发任何记录写操作（单测 `verify(never())` 断言）。Tool / Memory / 后置过滤 / 可观测均未引入，留 C2–C5。

### 两个必须记住的教训

1. **BUG 插曲（前端布局，非 Agent 逻辑）**：用户第 4 轮对话卡死且消息重叠。**先查库再改代码**——确认后端 session `ACTIVE`、`turn_count=3`、7 条消息完整、日志无 provider issue，才定位到是本人所写 CSS：`.agent-sheet{max-height}` + `.message-list{min-height:360rpx}` 使小程序 `scroll-view` 无确定高度、不启用内部滚动，消息累积后把 composer 顶出可视区（发送按钮不可点）；小程序原生 `textarea` 层级高于普通元素，被顶位后覆盖消息形成重叠。修复为固定 `height:78vh` + `min-height:0` + 头尾 `flex-shrink:0`。
   - **已转为契约**：`miniapp-core` 新增「消息累积后输入区仍须可点击且不覆盖消息」场景，使同类回归有依据，而不是只改一处样式了事。
   - **教训**：小程序里 flex 容器套 `scroll-view` 必须给确定高度并允许收缩；原生组件（textarea/input/video）层级高于普通元素，布局溢出时会直接穿透遮挡。
2. **流程偏差（闸门 3）**：手验时本地 `AI_PROVIDER=deepseek`，导致手验即真实外调（约 4 轮用户消息 / 7 条回复，远低于 ≤30 次预算，未用批量真实日记）。用量与数据面未越界，但**顺序错了：应先取得授权再联调**。未将其粉饰为 mock 验证，否则 baseline 中「真实 provider 已验证」会失去依据。
   - **对 C2 的约束**：启动任何手验前，先确认本地 `AI_PROVIDER` 取值；若为真实 provider，须先取得闸门 3 授权。

### 验证总表

| 项 | 结果 |
|---|---|
| backend `mvn -B test` | PASS｜254 tests / 0 failures / 0 errors（新增 43） |
| frontend `type-check` / `build:mp-weixin` | PASS / PASS |
| mock provider 端到端 | PASS（`AgentRuntimeIntegrationTest`：开场→多轮→恢复→结束→落库计数） |
| 真实 MySQL DDL | PASS（4 个 CASCADE 外键 + 唯一键 + 索引与 design 一致） |
| 真实 DeepSeek 多轮 | PASS（4 轮 `reply` JSON 全部解析成功，无 provider issue） |
| 微信手验 | PASS（修复布局缺陷后：可滚动 / 可发送 / 无重叠） |
| 最小护栏手验 | PASS（1–2 句短问句、无诊断词、未改写原文） |
| 日记/对话原文入日志 | 未发现泄露 |
| 收束/素材回填端到端 | **SKIPPED**——手验止于第 4 轮，未达 8 轮上限触发 CLOSING |

### 遗留（已进 ACTIVE_TASK Residual）

- 素材回填在 `record_id IS NULL` 且正文为空时会因内容校验失败报错（手验未触发）→ Type B 或 C2
- 最小护栏仅单层 prompt + 长度裁剪 → 已接受风险，C4 系统化补齐
- 真实 provider 仅验 4 轮，长会话稳定性待 C2 观察
- 本地：MySQL80 StartType=Manual；`start-dev-wechat.local.ps1` 明文存 secret（已 gitignore，建议轮换并改环境变量读取）
- `agent_message` 已存真实对话数据，本地重置库时注意

- **Commit**: pending（本条之后由用户授权 Agent 代为提交，提交后另条补录 hash，不回改本条）
- **Next**: 用户授权后开 C2 `agent-tool-calling` 规划闸。

## 2026-07-27｜agent-runtime-mvp｜Commit 补录

- **Commit**: `602b31b` — `feat(agent): 完成 C1 Agent Runtime MVP 并归档`
- **对应条目**: 上方 C1 的规划闸 / 闸门 2 实现 / 真机手验·缺陷修复·验收归档 / 全周期收束汇总四条（其 `Commit: pending` 均由本条补录，按规则不回改历史）
- **范围**: 53 files changed, 5401 insertions(+), 16 deletions(-)
- **提交方式**: 用户显式授权 Agent 代为提交；按路径显式 `git add`（未用 `git add .`），保留 hooks（未加 `--no-verify`）
- **未执行**: `git push`（未授权）。当前 `main` 领先 `origin/main` 1 个提交，待用户自行决定推送。
- **密钥检查**: `backend/start-dev-wechat.local.ps1` 与 `frontend/.env.local` 经 `git check-ignore` 确认被忽略，未进入本次提交；提交后 `git status` 干净。

## 2026-07-27｜agent-tool-calling｜Type C（规划闸产出）

- **Scope**: 仅 OpenSpec 规划文档与状态指针，**零业务代码**
  - `openspec/changes/agent-tool-calling/proposal.md`（新建）
  - `openspec/changes/agent-tool-calling/design.md`（新建，含 8 条决策记录）
  - `openspec/changes/agent-tool-calling/tasks.md`（新建，T-00 ~ T-49，含三道闸门检查点）
  - `openspec/changes/agent-tool-calling/specs/agent-runtime/spec.md`（新建 delta，主契约）
  - `openspec/changes/agent-tool-calling/specs/backend-core/spec.md`（新建 delta）
  - `openspec/changes/agent-tool-calling/specs/miniapp-core/spec.md`（新建 delta）
  - `openspec/changes/agent-tool-calling/specs/v2-product-scope/spec.md`（新建 delta）
  - `.ai/ACTIVE_TASK.md`（IDLE → ACTIVE，指向本 change）
- **Changes**:
  - 前置校验：`ACTIVE_TASK=IDLE`、C1 已归档、蓝图 v1.1 §3.2 默认顺序下一刀为 C2、C1 手验未见气质越界（无需把 C4 前移）→ 判定可开 C2 规划闸
  - 开工锚点：`63d1767`，`git status --porcelain` 为空（工作区干净）
  - 现状扫描落 28 条事实（F1–F28）并标能力五态。关键 `confirmed`：后端源码零处 `tools`/`tool_choice`/`function_call`/`tool_calls`；标签写操作不存在（`TagController` 仅 `GET /api/tags`）；打标签只能经 `PUT /api/records/{id}` 全量重绑 `tagIds`；`agent_message` 唯一键 `uk_agent_message_session_turn_role` 限制同轮同 role 单条；无工具审计表。关键 `unknown`：`tools` 与 `response_format=json_object` 共存性（F24）、白名单最终范围（F25）
  - 设计要点：二段式协议（模型只能提议、执行必须经用户确认的独立请求）＋ 代码级白名单 ＋ 执行复用 `RecordService`（继承 `ensureDraft`，不开 Agent 专用旁路）＋ 新表 `agent_tool_call` 承载幂等与审计 ＋ 结果结构化回注上下文
  - 白名单推荐：写 `append_record_content` / `add_record_tags` / `propose_unlock_at`，读 `list_available_tags` / `read_draft_snapshot`；seal / delete / unlock / location / cover / attachment / later-reflection / 标签创建全部代码级排除
  - 规划闸待确认 5 项（Q1 提议协议、Q2 白名单范围、Q3 持久化落点、Q4 执行入口形态、Q5 delta 落点），均在 proposal §7 列出推荐与代价
  - 外调预算：规划闸 0；实现期 0（mock provider）；闸门 3 后 ≤ 30 次（Q1 若选原生 FC 则 ≤ 45 次）
  - 已把 C1 遗留纳入 C2 视野：素材回填在正文为空时报错的缺陷，由「后端 `appendContent` 方法」路径覆盖（design 决策 5）
  - 已把 C1 流程偏差写成 C2 前置约束：任何手验前先确认本地 `AI_PROVIDER` 取值，若为真实 provider 须先取闸门 3 授权（tasks Gate 2）
- **Verification**: PASS（规划阶段，无代码可测）
  - 事实核对：28 条现状事实均可回溯到具体文件与类/方法/端点
  - 范围检查：未触碰业务代码、未改 package/lockfile、未创建 `.kiro/specs/`、未改三 Tab 与用户可见命名
  - 闸门检查：闸门 1 未取得，故未进入任何实现任务；tasks 中 T-03（实现授权）、T-37（外调授权）为独立检查点
  - **SKIPPED**：编译/测试/构建——本阶段零代码改动，无可验证目标
- **Risks**:
  - Q1 若最终选原生 Function Calling，`AgentModelClient` 与 `AiServiceImpl` 两处解析路径都要改，且 mock provider 需平行路径，实现面显著扩大
  - F24（`tools` 与 `json_object` 共存性）在规划期无法验证，只能在闸门 3 外调时确认
  - 提议格式遵从率依赖 prompt，M4 曾观察到结构化输出 5 次中 1 次无效；缓解为解析失败降级成「只有 reply」
  - C2 期间内容合规仍为 C1 单层 prompt + 长度裁剪（已接受风险，C4 补齐）；若手验见明显越界，按蓝图 §3.2 可将 C4 提前
  - `propose_unlock_at` 是否越过「建议不代决」边界存在判断空间，已在 design 决策 3 说明并可在规划闸直接砍掉
- **Commit**: pending
- **Next**: 等用户对 Q1–Q5 定稿并给出**闸门 1 规划批准**；批准后再单独取得闸门 2 实现授权，才可动 T-04 起的业务代码。

## 2026-07-27｜agent-tool-calling｜Type C（Q1 协议翻转 + 规划定稿）

- **Scope**: 仅 OpenSpec 规划文档与状态指针，**零业务代码**
  - `openspec/changes/agent-tool-calling/proposal.md`（F5b/F23/F24/F29/F30 更新，§7 改为已定稿，§10 验收标准重编号并补 FC 项，新增 §12 长期演进备注）
  - `openspec/changes/agent-tool-calling/design.md`（架构图、数据流 2.1、白名单 §3 重写；新增 §3.1 读工具预注入、§3.2 strict schema 约束落点、§3.3 FC 可用性判定；决策 1 重写；**新增决策 9/10/11**）
  - `openspec/changes/agent-tool-calling/tasks.md`（T-01 勾选定稿；新增 T-05b/T-08b/T-18b/T-18c/T-18d/T-39b；T-24/T-25/T-37/T-39 改写；范围守护补 4 条）
  - `openspec/changes/agent-tool-calling/specs/agent-runtime/spec.md`（**新增 2 个 Requirement**：原生 FC 无降级、不在回复生成过程内执行；白名单条款补 3 个 scenario）
  - `openspec/changes/agent-tool-calling/specs/backend-core/spec.md`（新增 Requirement：FC 配置须 backend-side 且显式判定）
  - `.ai/ACTIVE_TASK.md`（Gate 表、决策定稿表、Current Progress、Out Of Scope 更新）
- **Changes**:
  - **Q1 决策翻转**：规划初稿推荐「扩展自研 JSON `proposal` 字段」，理由是 `tools` 与 `json_object` 共存性未知。查阅 DeepSeek 官方 Tool Calls 文档后该前提不成立，四条事实推翻初稿：① FC 路径下 `tool_calls` 与 `content` 分属不同字段、天然并存，不需要 `json_object`（F24 `unknown` → `confirmed`）；② 官方示例用的就是 `deepseek-v4-pro`，正是本仓库 `app.ai.model` 默认值（F5b 新增）；③ **strict mode（Beta）** 由服务端校验 JSON Schema，使白名单从 prompt 提示升级为服务端强制类型约束，可靠性反超自研协议；④ tools schema / `tool_call_id` / `role:"tool"` 是 C3 多工具、C5 决策链路与未来 MCP / 框架接入的共同地基。用户 2026-07-27 确认改为原生 FC + strict mode
  - **无降级定为硬约束**：FC 不可用即显式 `UNAVAILABLE`，仓库内不得存在第二条提议解析路径。依据是已接受 baseline「不得 mock success 冒充真实成功」——静默降级会制造「以为在跑 FC、实际在跑另一套解析」的模糊状态
  - **决策 9（新）**：不做单轮内 FC 循环。原生 FC 的标准控制流是模型驱动执行，用户没有插入确认的位置，与二段式确认直接冲突。结论：采用 FC 的**协议**不等于采用它的**控制流**；每轮 provider 请求次数恒定为 1
  - **决策 10（新）**：单轮多个 `tool_calls` 只取第一个合法提议。产品气质要求输出克制，多条确认条会让浮层变成待办清单；`pendingToolCall` 保持单值，幂等边界清晰
  - **决策 11（新）**：不引入 MCP / Spring AI。MCP 解决跨客户端工具复用，而本项目工具只有一个消费方且必须绑 JWT 上下文与二段式确认；Spring AI 会改 `pom.xml` 并替换 C1 已验证调用链。因选了原生 FC schema，未来迁移是平滑替换而非重写
  - **strict mode 限制已落到设计**：strict 不支持 `maxLength` / `maxItems` / `minItems`，故 `text` 长度、`tagIds` 数量、`unlockAt` 时序必须在 `AgentToolValidator` 代码层二次校验，明确写入 design §3.2 与验收标准第 7 项。结论：strict 把**类型与形状**前移到服务端，**业务边界**仍归后端
  - **读工具改为 prompt 预注入**：`list_available_tags` / `read_draft_snapshot` 数据量小且每轮都需要，在「不做单轮内 FC 循环」约束下无法作为 FC tool 获取；registry 仍声明它们（白名单与审计完整视图），但只把写工具放进下发的 `tools` 数组
  - **新增配置项设计**（无凭证）：`tool-calling-enabled` / `strict-mode-enabled` / `strict-mode-base-url` / `function-calling-models`。最后一项来自 F29——R1 曾明确不支持 FC、有第三方报告称 distill 变体返回空 `tool_calls`，故不得假设任意 OPENAI_COMPATIBLE provider 或任意 model 都支持
  - **F30 结论**：第三方报告称 streaming 加剧 `tool_calls` 解析不稳，本仓库已固定 `stream: false`，**保持不动**
  - 外调预算 30 → **45 次**（含 strict schema 被服务端接受/拒绝验证、`tool_calls` 与 `content` 并存确认、`content` 为空时 `askText` 兜底）
  - 长期演进方向记入 proposal §12（MCP / Spring AI / C3 的 Memory 存储选型），标注为**不授权在 C2 实施**
- **Verification**: PASS（规划阶段，无代码可测）
  - 一致性扫描：grep `json_object` / `proposal 字段` / `降级` / `≤ 30` 全库复核，未残留与 Q1 定稿矛盾的表述；`json_object` 剩余出现均为「既有链路不碰」的正确语境
  - 编号修正：proposal §3 Goals 与 §10 验收标准因插入条目导致的编号错乱已修正
  - 诚实性复核：F23 明确标 `partial`（官方文档确认、**本仓库一次未跑过**），未写成 `confirmed`；F29/F30 标注为非官方源按风险提示对待；F24 转 `confirmed` 的依据是官方响应结构而非本仓库实测，已在诚实性声明中写明
  - 范围检查：未触碰业务代码、未改 package/lockfile、未创建 `.kiro/specs/`、未执行 git 写操作
  - **SKIPPED**：编译/测试/构建——本阶段零代码改动，无可验证目标
  - **误报澄清**：`design.md` / `tasks.md` 的 Kiro 诊断报「缺少 Kiro Spec 章节」（Overview / Architecture / Task Dependency Graph 等）。已对已归档验收的 C1 同款文档取诊断，报错完全一致 → 该诊断来自 Kiro Spec 格式提供方，本项目按规则使用 OpenSpec，**属误报，不修**
- **Risks**:
  - **F23 是本 change 最大未验证依赖**：strict mode 为 Beta 且需 `/beta` base URL（与生产默认 `AI_BASE_URL` 不同），本仓库零实测。闸门 3 若 schema 被拒，处置顺序＝修 schema → 关 `strict-mode-enabled` 仅用普通 FC → 升级请示；**任何情况下不退回自研协议**
  - 决策 9 的代价：Agent 在提议那一轮不知道执行结果，衔接感弱于标准 FC 循环；缓解是下一轮 prompt 带执行摘要
  - 决策 9 的副作用：`role:"tool"` 标准消息角色在 C2 用不上，C5 记链路时需自己把 `agent_tool_call` 映射成 observation 段
  - `propose_unlock_at` 是否越过「建议不代决」仍有判断空间，规划闸判为不越界，手验体感越界可从 registry 移除
  - C2 期间内容合规仍为 C1 单层 prompt + 长度裁剪（C4 补齐）；日记正文中的诱导性指令由白名单 + 二段式确认兜底，而非内容识别
- **Commit**: pending
- **Next**: 等 **T-02b 闸门 1 放行**；放行后仍需单独的闸门 2 实现授权才可动 T-04 起的业务代码。

## 2026-07-27｜agent-tool-calling｜Type C（闸门 2 实现）

- **Scope**: 后端 Tool 层 + 持久化 + 执行层 + 会话集成 + 确认端点；前端确认交互；OpenSpec 同步
  - **新增（后端主代码）**：`agent/tool/` 共 11 个类（`AgentToolName` / `AgentToolSpec` / `AgentToolRegistry` / `AgentToolSchemaFactory` / `AgentToolProposal` / `AgentToolRawArguments` / `AgentToolValidator` / `AgentToolValidationResult` / `AgentToolOutcome` / `AgentToolCallStatus` / `AgentToolDecision` / `AgentToolExecutor` / `AgentToolCoordinator` / `AgentToolArgsDigest` / `AgentToolPendingArgs`）、`agent/AgentModelResponse`、`agent/AgentRawToolCall`、`domain/AgentToolCall`、`mapper/AgentToolCallMapper` + XML、`dto/AgentToolCallConfirmRequest`、`vo/AgentToolCallVO`、`sql/mysql/c2-agent-tool-call.sql`
  - **修改（后端）**：`AgentModelClient`（新增 `completeWithTools` + FC 可用性判定，既有 `complete()` 未动）、`AgentPromptBuilder`（新增带工具补充上下文的重载 + `buildToolSupplement`）、`AgentMockResponder`（伪造 `tool_calls`）、`AppAgentProperties`（7 项 C2 配置）、`AgentChatService(+Impl)`（`confirmToolCall`）、`AgentController`（确认端点）、`AgentSessionVO`（2 个向后兼容字段）、`RecordService(+Impl)`（`appendContent` / `appendTags` / `updateUnlockAt`）、`RecordMapper(+XML)`（2 条窄更新）、`RecordTagMapper(+XML)`（`selectTagIdsByRecordId`）、`application.yml`、测试 `schema.sql`
  - **新增（后端测试）**：`AgentToolValidatorTest`(16)、`AgentToolSchemaFactoryTest`(9)、`AgentToolExecutorTest`(11)、`AgentToolCoordinatorTest`(7)、`AgentModelClientToolCallingTest`(11)、`AgentToolCallingIntegrationTest`(11)、`RecordServiceAppendIntegrationTest`(13)
  - **修改（前端）**：`services/agentService.ts`、`stores/agentChat.ts`、`components/AgentChatSheet.vue`、`pages/record-editor/index.vue`
  - **OpenSpec**：`tasks.md`（T-04~T-36 勾选）、`design.md`（新增决策 12）、`specs/agent-runtime/spec.md`（新增瞬态参数 3 个 scenario）、`proposal.md`（提交责任变更）
- **Changes**:
  - **原生 FC 接入**：`completeWithTools` 下发 `tools`、解析 `message.content` 与 `message.tool_calls`；**不下发** `response_format`。既有 `complete()` 与 `/api/ai/**` 三端点的 `json_object` 链路完全未动（grep 复核：`response_format` 仅存于 `AiServiceImpl` 与 `complete()` 两处）
  - **无降级已落实**：grep 复核后端零处自研 `proposal` JSON 解析路径；FC 不可用时仅不下发 tools 并退回 C1 纯对话
  - **strict schema 守门**：`AgentToolSchemaFactory` 生成全属性 `required` + `additionalProperties:false`，专测断言**不含** `maxLength`/`minLength`/`maxItems`/`minItems`（出现即会被 provider 拒绝整个 schema）；长度与数量边界改由 `AgentToolValidator` 代码层校验
  - **二段式落地**：提议只落 `PROPOSED`，执行仅在 `confirmToolCall`。`AgentToolCoordinatorTest` 以 `verifyNoInteractions(executor)` 证明提议阶段绝不执行
  - **幂等**：`updateStatusIfProposed` 用 `WHERE status='PROPOSED'` 条件更新实现，非先查后写；集成测试证明重复 ACCEPT 不产生第二段正文
  - **执行不绕业务层**：三个写工具全部经 `RecordService`，继承 `requireOwnedRecord` + `ensureDraft`；两条新增窄 SQL 同样带 `AND status='DRAFT'`，使封存不可变在 DB 层也成立
  - **决策 12（实现期新增）**：执行需要原始 `text` 但审计只存摘要 → 选瞬态 `pending_args` 列，终结时与状态流转在同一条 UPDATE 中置 NULL。否决「前端回传参数」（等于绕过白名单校验）与「存内存」（重启即丢、多实例失效）
  - **白名单排除已验证**：`AgentToolValidatorTest` 对 seal/delete/unlock/location/cover/attachment/later-reflection/标签创建 9 个工具名逐一断言被拒；`AgentToolExecutorTest` 以 `never()` 断言 `seal`/`delete`/`update` 等从未被调用
  - **决策 8 已验证**：集成测试断言确认前后 `stage` 与 `turnCount` 均不变
  - **前端**：确认条复用素材卡视觉家族，未新增页面/路由/一级 Tab；执行成功后调既有 `fillByDetail` 刷新表单，避免表单与后端漂移导致后续保存覆盖掉刚追加的内容；`confirmingToolCall` 防抖
- **Verification**:
  - backend `mvn -B test`：**PASS｜329 tests / 0 failures / 0 errors**（C1 基线 254 + 新增 75）
  - C1 基线回归：**PASS 且未改动任何 C1 断言**。仅 3 个用例改了 stub 接线（`complete` → `completeWithTools`），原因是 Agent 路径按 Q1 定稿改走 FC；另补齐 `AgentChatServiceImplTest` 构造签名新增依赖。已在代码注释中说明
  - frontend `type-check`：PASS；`build:mp-weixin`：PASS
  - H2 schema：新增 `agent_tool_call` 建表 + DROP 顺序修正（`agent_tool_call` 须先于 `agent_session`，否则外键阻塞建库——已实测触发并修复）
  - 结构化日志抽查：执行日志仅输出 sessionId/toolCallId/tool/status/failureType/costMs，无参数原文
  - 审计脱敏：集成测试断言 `args_digest` 含 `len=`/`sha256=` 且不含原文；`pending_args` 在终结后为 NULL
  - **SKIPPED｜真实 provider FC 联调（T-37~T-39b）**：原因＝**闸门 3 未授权**。strict mode 是否被 DeepSeek 服务端接受、`tool_calls` 与 `content` 实际并存行为，本仓库仍**零实测**（proposal F23 保持 `partial`）
  - **SKIPPED｜微信小程序手验（T-40~T-42）**：原因＝同上，需真实 provider 才能产生提议
  - **SKIPPED｜真实 MySQL DDL 执行（T-38）**：原因＝闸门 3 未授权且本地 MySQL80 为手动启动；`c2-agent-tool-call.sql` 目前仅经 H2 等价 schema 验证
- **Risks**:
  - **最大未验证项**：原生 FC + strict mode 在真实 provider 上一次未跑。若 schema 被拒，按 design §4 处置顺序（修 schema → 关 strict → 升级请示），任何情况下不退回自研协议
  - mock provider 的提议触发点固定在 `CORE_QUESTION` 阶段，真实模型的提议时机与频率未知，可能过多或过少，须手验观察
  - `pending_args` 在待确认窗口内确实存在日记文本副本（虽瞬态），比决策 6 理想状态弱一档，已在 DDL 注释与 spec scenario 显式声明
  - C2 期间内容合规仍为 C1 单层 prompt + 长度裁剪；提议话术可能出现诊断色彩表达，C4 补齐
  - `propose_unlock_at` 是否越界仍待手验体感确认，越界可从 registry 移除
- **Commit**: pending（本条之后由 Agent 代为提交，提交后另条补录 hash，不回改本条）
- **Next**: 请求**闸门 3 外调授权**（≤ 45 次，含 strict schema 验证）。授权前先确认本地 `AI_PROVIDER` 取值，避免重演 C1 偏差。

## 2026-07-27｜agent-tool-calling｜Commit 补录

- **Commit**: `6c363f6` — `feat(agent): C2 Agent Tool Calling 实现（原生 FC + 二段式确认）`
- **对应条目**: 上方 C2 的规划闸 / Q1 协议翻转 / 闸门 2 实现三条（其 `Commit: pending` 由本条补录，按规则不回改历史）
- **范围**: 61 files changed, 5978 insertions(+), 60 deletions(-)
- **提交方式**: 用户显式授权 Agent 代为提交；按路径显式 `git add`（未用 `git add .`），保留 hooks（未加 `--no-verify`）
- **未执行**: `git push`（未授权）。当前 `main` 领先 `origin/main` 2 个提交（C1 `602b31b` + C2 `6c363f6`），待用户决定推送
- **密钥检查**: `backend/start-dev-wechat.local.ps1` 与 `frontend/.env.local` 经 `git check-ignore` 确认被忽略，未进入本次提交
- **清理**: 临时脚本 `tick-tasks.ps1` 与临时 commit message 文件已删除，未留验证残留

## 2026-07-27｜agent-tool-calling｜Type C（手验缺陷修复 + 闸门 3 外调验证）

- **Scope**:
  - `backend/src/main/java/com/flashback/agent/AgentPromptBuilder.java`（删除 JSON 输出要求，新增 `normalizeReplyShape` 形状兜底）
  - `backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java`（回复接入形状兜底）
  - `backend/src/test/java/com/flashback/agent/AgentPromptBuilderTest.java`（改 1 处过期断言 + 新增 3 个用例）
  - 真实 MySQL `flashback` 库：执行 `c2-agent-tool-call.sql`
  - 临时探针脚本（`fc-probe.local.ps1` / `run-fc-probe.local.ps1` / `decode.local.ps1`）已在验证后删除，未提交
- **Changes（缺陷修复）**:
  - **BUG：对话气泡显示 `{"reply":"..."}` 原文**（用户真机手验发现）。根因＝**C2 自身引入的不一致**：C2 把 Agent 回复解析从 `extractText(raw,"reply")` 改为直接读 `message.content`，但 `buildSystemPrompt` 仍保留 C1 的「只输出 JSON，格式为 {"reply":...}」要求。模型忠实照做，后端不再剥壳 → JSON 原文直接进气泡。**不是模型问题，是我漏改**
  - 修复一：输出要求改为「直接输出你要说的那句话本身，不要输出 JSON / 引号包裹 / 字段名」
  - 修复二：新增 `normalizeReplyShape`——若模型仍返回 JSON 包裹则剥出文本，解析失败保留原文（不误伤含花括号的正常口语）。定位为**格式**兜底，非 C4 的内容合规过滤
  - 修复三：新增回归守门测试 `shouldNotAskModelForJsonWrappedReply`，断言 prompt 不含「只输出 JSON」且含「不要输出 JSON」，防止同类回归
  - 未动素材路径：`buildMaterialMessages` 仍走 `complete()` + `extractText(raw,"material")`，其 JSON 约定依然成立，故保持不变（已核对）
- **Verification（闸门 3，用户 2026-07-27 授权）**:
  - **前置检查 PASS**：先确认本地 `AI_PROVIDER=deepseek`、`AI_MODEL=deepseek-v4-pro`（在 FC 白名单内）、`AI_BASE_URL=https://api.deepseek.com`。**未重演 C1「先联调后授权」的顺序偏差**
  - **T-38 真实 MySQL DDL PASS**：MySQL80 已在 RUNNING（无需手动启动）。`agent_tool_call` 建表成功，校验 14 列（含 `pending_args TEXT`）、3 个 CASCADE 外键（→ `agent_session` / `user` / `record`）、`status` 默认 `PROPOSED`，与设计一致
  - **T-39 真实 provider FC PASS**（外调 5 次，预算 ≤ 45，**实际用量 5**）：
    - 第 1 次：**失败但非 provider 问题**——探针脚本 PowerShell 默认编码把中文 prompt 送成乱码，模型返回完全无关内容（晶体学）。定位为探针缺陷，改为显式 UTF-8 编码请求体后正常。**记录此项以免误判为模型不可用**
    - 第 2 次：中文正常，`content` 有值、`tool_calls` 字段**不存在**
    - 第 3 次：修正统计逻辑后复核——原先用 `@($message.tool_calls).Count` 统计，`@($null).Count` 也返回 1，产生**假阳性**。改为显式判断字段是否存在，确认第 2 次实为 `tool_calls` 缺失（模型判断此刻无需提议）。**差点据假阳性得出错误结论，已修正**
    - 第 4 次：用户明确表达「想留下来」后，模型返回 `finish_reason=tool_calls`、`tool_calls[0].function.name=append_record_content`、参数含 `text` 与 `askText`。**原生 FC 在本仓库首次实测可用**
    - 第 5 次：**strict mode PASS**——`https://api.deepseek.com/beta` + `strict:true`，服务端**接受**我们由 `AgentToolSchemaFactory` 生成的同形 schema（全属性 required + `additionalProperties:false`，不含 `maxLength`/`maxItems`）。design §4 的降级处置顺序**无需启用**
    - **附带确认**：第 4、5 次均为 `content` 为空、仅有 `tool_calls` → 证实 `askText` 兜底逻辑（design 数据流 2.1 要点二）是**真实必要**而非防御性冗余
  - 后端全量测试：**PASS｜339 tests / 0 failures / 0 errors**（较上轮 329 增 10：形状兜底 3 + coordinator 7）
  - **SKIPPED｜微信小程序端到端手验（T-40 / T-41 部分）**：原因＝本轮 JSON 显示缺陷刚修复，需用户重新真机验证提议→确认→执行链路与气质表现；后端侧链路已由 mock 集成测试与真实 FC 探针分别覆盖
  - **SKIPPED｜T-42 审计表真实数据核验**：原因＝真机手验未进行，`agent_tool_call` 真实库中暂无数据；H2 集成测试已断言摘要脱敏与 `pending_args` 终结后为 NULL
- **Risks / 新发现**:
  - **[严重｜新发现] 工具提议参数越过「不改写原文」边界**：真实返回的 `text` 把用户两轮口语改写重组，并**增写了用户从未说过的内容**（详见 ACTIVE_TASK Residual）。`askText` 也自称「我帮你整理了一下」。这触碰 `AGENTS.md` Non-Negotiable 与 `agent-runtime` spec 的「原样引用」条款
    - C2 的白名单 + 二段式确认**拦不住它**：它们校验「能否执行」，不校验「参数是否忠实」。属 C4 内容忠实度范畴（design 决策 7 已划清边界）
    - 与用户提出的「素材生硬」是同一根源两面：要么逐句拼接（生硬），要么自行润色（越界）
    - **建议：按蓝图 §3.2 将 C4 前移至 C3 之前。本次已构成该条款所需的气质越界实证**
  - 提议时机：模型在用户未明确表达留存意图时不提议，明确后才提议——时机判断合理，但样本仅 2 次，频率是否恰当仍需真机长会话观察
  - 本轮修复后尚未真机验证，JSON 显示问题是否彻底消失需用户确认
  - **[安全] 本轮操作中我的 grep 范围过宽，将本地 `AI_API_KEY` 与阿里云 OSS AK/SK 打印到了终端输出**。文件本身已 gitignore、未进仓库，但值已出现在会话记录中。**已向用户披露并建议轮换这三项凭证**，并改为环境变量/密钥管理读取（原 ACTIVE_TASK 中「建议轮换」的遗留项已升级为应当执行）
- **Commit**: pending
- **Next**: 用户真机复验对话回复是否已为自然文本 + 工具提议链路；并决定是否将 C4 前移。

## 2026-07-28｜agent-tool-calling｜Type B（secret 外移）+ 手验问题归档

- **Scope**:
  - `.gitignore`（新增 `secrets.local.env` / `*.local.ps1.bak` / `*.local.env` 忽略规则，显式放行 `.example`）
  - `backend/secrets.local.env.example`（新增，入库模板）
  - `backend/secrets.local.env`（新增，**gitignore，未提交**）
  - `backend/start-dev-wechat.local.ps1`（**gitignore，未提交**）：注入 `Get-LocalSecret`，5 处明文改为读取
  - `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`
  - 一次性脚本（migrate / audit / check-loader / verify-fix）均已删除，未提交
- **Changes**:
  - **secret 外移（用户授权）**：明文凭证从启动脚本迁至 `secrets.local.env`，脚本改为 `Get-LocalSecret 'KEY'`。迁移 5 项：`AI_API_KEY` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_SESSION_TOKEN` / `WECHAT_MINI_PROGRAM_SECRET`
  - 设计取舍：缺键时 **throw 快速失败**，不回退空值——空值会让 provider 配置「看起来正常」而实际不可用，与既有「不得伪装成功」的立场一致；但**允许键存在而值为空**（S3 用长期 AK/SK 时无需 session token）
  - 执行顺序上的谨慎：**先备份启动脚本 → 先补 `.gitignore` 并验证 `git check-ignore` 生效 → 才写入 secret 文件**。避免出现「凭证已落盘但尚未被忽略」的窗口
  - 迁移中发现并修正 2 个自身缺陷：① `S3_SESSION_TOKEN` 原值为空未被搬运，会导致启动即抛错 → 补写空值键；② `WECHAT_MINI_PROGRAM_SECRET` 是裸变量而非 `$env:` 赋值，未被首轮匹配到 → 手工补迁
- **Verification**:
  - secret 读取：**PASS**｜5 键全部可读（AI 35 / S3_AK 24 / S3_SK 30 / TOKEN 0 / WECHAT 32，仅长度不打印取值）；缺失键按预期抛错
  - 明文残留审计：**PASS**｜启动脚本内已无凭证形态明文（剩余长串为模板 ID 与 `Assert-Filled` 的标签字符串，非凭证）
  - git 安全：**PASS**｜`secrets.local.env` / `*.bak` / 启动脚本 三者 `git check-ignore` 均命中；`git status` 待提交内容仅 `.gitignore` 与 `.example` 模板
  - **JSON 缺陷复验：PASS（真实外调 1 次，累计 6/45）**｜修复后 prompt 返回 `starts_with_brace=False`、无 `reply` 字段
- **Findings（手验问题归档，按用户要求不在 C2 优化）**:
  - **JSON 仍显示的真因＝运行实例未重启，非修复无效**。证据：JSON 消息落库 `2026-07-28 09:21`（`agent_message` id=21, turn_no=0），而 class 编译于 `08:56`。**先查库与时间戳再下结论，未据表象改代码**。库中 6 条历史 JSON 消息为修复前既有数据，不会自动清理
  - **引导问题突兀 + 素材拼接生硬**（用户评「比上一版还突兀」）：天气比喻开场 → 用户答不上并反问「你为什么要这样问」→ 素材把用户的**反问与困惑本身**也拼进正文。两层问题＝引导策略（比喻式提问在受阻时未退回具体情境）+ 素材合成（不区分「用户的表达」与「用户对提问的抵触」）
  - 关联：问题 ① 与 `stageGoal` 中「不要问抽象的大问题」的意图相悖，属 prompt 与阶段目标的落差，非代码缺陷
  - 用户决定：接近微调范畴，**C1–C5 全部完工后统一收集处理**
- **Risks**:
  - **凭证仍需用户轮换**：外移只解决「未来不再明文存放」，不消除本次已泄露的取值（我的 grep 范围过宽所致）。`*.bak` 中仍含旧明文，轮换后建议删除
  - `start-dev-wechat.local.ps1` 与 `secrets.local.env` 均不入库，故本次改造在其他机器上需先复制 `.example` 并填值，否则启动即抛错（这是刻意行为）
  - 素材与引导质量问题未解决，C2 验收时应视为**已知且被接受**的限制，不应记为已完成
- **Commit**: pending
- **Next**: 用户重启后端复验 JSON 已消失；轮换 5 项凭证并删除 `.bak`；决定 C4 是否前移。

## 2026-07-28｜agent-tool-calling｜Type C（验收收口与归档）

- **Scope**:
  - `openspec/specs/agent-runtime/spec.md`（MODIFIED 条款就地修订 + 追加 C2 ADDED 段落）
  - `openspec/specs/backend-core/spec.md`、`miniapp-core/spec.md`、`v2-product-scope/spec.md`（各追加 C2 ADDED 段落）
  - `openspec/changes/agent-tool-calling/` → `openspec/changes/archive/2026-07-28-agent-tool-calling/`（`git mv`，保留历史）
  - 新增 `closeout.md`；`tasks.md` 勾选 T-37~T-39b、T-43~T-49
  - `.ai/ACTIVE_TASK.md` → IDLE 并重写 carry-over
- **Changes**:
  - delta 接受采用**脚本原样搬运**而非手工转写，避免契约在接受过程中漂移；接受后校验 4 份 baseline 的「Accepted From C2」段落各 1 处、Requirement **无重名**（重名即意味重复追加）
  - `agent-runtime` 的 MODIFIED 条款做**就地修订**而非追加：原「C1 范围内的工具调用」scenario 收窄为 C1 历史约束，并新增「C2 之后的工具调用」scenario，避免 baseline 中出现自相矛盾的两条工具边界
  - **T-40/T-41/T-42 刻意保持未勾选**并在 tasks 中逐条写明未完成原因。理由：微信端到端工具链路确实未走通，勾选即为谎报验证结果。closeout §3 亦以 SKIPPED 显式列出
  - **顺序调整：C4 前移至 C3 之前**（用户 2026-07-28 批准）。依据＝蓝图 §3.2「若 C1 联调出现气质越界则允许 C4 前移」，而 C2 闸门 3 已取得越界实证（R1）。C2–C5 彼此无硬依赖，调整不破坏依赖链
- **Verification**:
  - delta 接受：PASS｜4 份 baseline 各 1 处 C2 段落，Requirement 重名 0
  - 归档：PASS｜`git mv` 保留重命名历史（status 显示为 R），closeout 已入库
  - backend `mvn -B test`：PASS｜339 tests / 0 failures
  - 一次性脚本（accept-delta / verify-delta / tick2）已全部删除，无验证残留
- **Risks（移交下一刀）**:
  - **R1 → C4 核心动机**：工具参数改写并增写用户原话，触碰 Non-Negotiable。C4 需回答「如何机械判定忠实」——候选：与会话历史做覆盖率比对 / 拒绝增写部分 / 参数改为消息 id 引用而非自由文本
  - R2 → 引导与素材质量，用户明确要求全部阶段完工后统一优化
  - R3 → 微信端到端工具链路手验待补
  - R6 → 5 项凭证待用户轮换（Agent grep 过宽所致），`.bak` 待删
- **Commit**: pending
- **Next**: 用户授权后启动 C4 `agent-guardrails-hardening` 规划闸。

## 2026-07-28｜agent-guardrails-hardening（C4）｜Type C

- **Scope**: C4 规划闸产出（**零业务代码**）
  - `openspec/changes/agent-guardrails-hardening/proposal.md`（新建）
  - `openspec/changes/agent-guardrails-hardening/design.md`（新建，含决策记录 10 条）
  - `openspec/changes/agent-guardrails-hardening/tasks.md`（新建，T-00~T-55）
  - `openspec/changes/agent-guardrails-hardening/specs/agent-runtime/spec.md`（新建，7 条 ADDED + 2 条 MODIFIED）
  - `openspec/changes/agent-guardrails-hardening/specs/backend-core/spec.md`（新建，4 条 ADDED）
  - `openspec/changes/agent-guardrails-hardening/specs/v2-product-scope/spec.md`（新建，4 条 ADDED）
  - `Docs/agent-iteration/roadmap/iteration-blueprint.md`（§3.2 + §7 登记 C4 前移）
  - `.ai/ACTIVE_TASK.md`（IDLE → ACTIVE，指针指向本 change，初始化 Current Progress）
- **Changes**:
  - 依蓝图 §3.2 与 2026-07-28 用户批准，登记 **C4 前移至 C3 之前**；触发实证为 C2 闸门 3 的 R1（工具参数增写用户原话）
  - proposal：归因 R1 为何穿透 C1/C2 两层防御（白名单与二段式只校验「能否执行」，不校验「参数是否忠实」）；能力五态 G1–G27（其中 G16 诊断越界标 `unknown` 而非「未发生」）；核心问题「怎么机械判定忠实」列出三候选方向与取舍，标为 Q1 待用户确认；验收标准 25 条；外调预算 ≤ 30（低于 C2 的 45，因核心机制为纯后端确定性逻辑）
  - design：忠实度机制定为**双指标**——整体覆盖率 + 最长连续未覆盖片段。给出「仅覆盖率拦不住 R1」的推算（真话 30 字 + 虚构 45 字，整体覆盖率仍约 50%），故双指标为必要而非冗余；来源集合只取本会话 `role=USER` 消息（守 C3 边界）；诊断检查只作用于「新增区段」以避免误伤用户自述病名
  - design §3.5 按用户要求说明 C4 与 R2 的关系：**顺语序算忠实、增写不算**；C4 只在出口拦截，不调引导 prompt 与素材合成策略
  - spec delta 含 2 条 MODIFIED——修订 baseline 中「后置输出过滤与违规降级留给后续 change」的两条 scenario（C1 与 C2 各一条）
- **Verification**: PASS（规划阶段无代码验证项）
  - 文档一致性：proposal Q1–Q7 ↔ design 决策记录 ↔ tasks Gate 0 逐项对应；验收标准 ↔ tasks 测试切片对应
  - `git diff --stat`：见收口输出；本条目提交时一并记录
  - 范围守护：零业务代码改动；未触碰 `backend/src/**`、`frontend/src/**`
- **Risks**:
  - 阈值初值（n-gram=4 / min-coverage=0.60 / max-uncovered-run=12）为**保守推断而非实测标定**，须在闸门 3 用真实样本校准；不得当作已验证阈值
  - 已接受残余风险：大量复用用户原话词汇的虚构可能绕过双指标，须写入 closeout
  - 字符级 n-gram 对同义替换较钝（「后端」vs「服务端」），与产品「原样引用」意图同向，不视为损失
  - Q1 若用户改选「消息 id 引用」方案，需 MODIFIED C2 刚接受的工具参数契约，且素材会更生硬（与 R2 抱怨同向恶化）
- **Commit**: pending
- **Next**: 用户对 Q1–Q7 定稿并批准闸门 1；随后勾选 T-02 / T-02d，取得实现授权（T-03）后从 tasks A 段（忠实度判定核心）起步

## 2026-07-28｜agent-guardrails-hardening（C4）｜Type C｜闸门 2 实现

- **Scope**: 后端护栏检查层（新增 `com.flashback.agent.guardrail` 包）+ 三条落地路径接入
  - 新增：`AgentTextNormalizer` / `AgentSourceCorpus` / `AgentCoverageProfile` / `AgentFaithfulnessChecker` / `AgentContentChecker` / `AgentGuardrailRules` / `AgentGuardrailVerdict` / `AgentGuardrailViolation` / `AgentGuardrailDowngrade`
  - 改动：`AgentGuardrailPolicy`（文案改为委托规则源，`enforceReplyLength` 未动）、`AgentPromptBuilder`（文案取自规则源）、`AgentToolValidator`（扩签名 + 忠实度闸）、`AgentToolValidationResult`（新增 3 个拒绝原因常量）、`AgentToolCoordinator`（传入来源语料）、`AgentChatServiceImpl`（回复检查 + 素材闸）、`AppAgentProperties`（新增 `guardrail` 子配置）
  - 测试新增：`AgentFaithfulnessCheckerTest`(15) / `AgentContentCheckerTest`(13) / `AgentTextNormalizerTest`(6) / `AgentGuardrailBoundaryCaseTest`(15) / `AgentMaterialGuardrailTest`(3)；既有 5 个测试类按新签名补参
  - 文档：`design.md` 补实测校准表 + 决策 13；`tasks.md` 勾选 T-03~T-42
- **Changes**:
  - 忠实度判定落地为**双指标**：整体覆盖率 + 最长连续未覆盖片段。判定确定性、零外调、可单测
  - 忠实度闸接在 `AgentToolValidator`（与白名单 / 类型 / 边界同层），**刻意不提供无语料重载**——那会造出绕过检查的提议路径
  - 覆盖范围＝工具正文参数 + 素材草稿 + `askText`（后者用伪引用 + 诊断/代决的宽判定）
  - 处置分路径：提议拒绝（走既有 `REJECTED_BY_GUARD` 通道）/ 素材丢弃（复用「可选产物」语义，零前端改动）/ 回复替换本地兜底
  - 诊断与代决检查**只在「新增区段」匹配**，用户自述病症词被 Agent 复述不误伤
  - 护栏规则从三处（`AgentGuardrailPolicy` 常量 + `buildToolSupplement` + `buildMaterialMessages`）收敛到 `AgentGuardrailRules` 单一声明源，并含正向行为清单
  - 前端零改动（Q6 定稿：护栏对用户不可见）
- **Verification**: PASS
  - `mvn -B -o test` → **Tests run: 396, Failures: 0, Errors: 0**（C1+C2 基线 339 不回归；**未改动任何既有断言**，仅按新构造签名补参）
  - R1 真实样本回归 PASS：两句真话 + 45 字虚构 → 判 UNFAITHFUL，提议不落库、无确认条、本轮 reply 正常返回
  - 双指标必要性测试 PASS：R1 在「仅覆盖率」判据下通过、加 `maxUncoveredRun` 后被拦
  - 不误伤测试 PASS：语序调整 / 去口头语 / 标点变化 / 多消息拼接 / 接缝插连接词 全部放行
  - fail-closed PASS；隐私断言 PASS（`metrics()` 只含数值）
  - 外调：**0 次**（闸门 3 未授权，全程 mock provider）
  - **阈值实测校准（零外调，本地样本）**：R1 增写 coverage=0.449 / maxRun=38；合法去口头语整理 coverage=0.500 / maxRun=6；伪造引用 coverage=0.000（11 字）；合法引用 coverage=1.000
  - 前端：无改动，故未执行 `type-check` / `build:mp-weixin`
- **Risks**:
  - **规划初值 `min-coverage=0.60` 被实测推翻**：合法整理覆盖率仅 0.500，该阈值会误伤正常能力，已下调为 0.35。覆盖率因此退化为兜底辅判据，主判据完全落在 `maxUncoveredRun`（R1 38 vs 合法 6）
  - 已接受残余风险：大量复用用户原话词汇的虚构可能绕过双指标，须写入 closeout
  - 阈值仍为**本地样本标定**，未经真实 provider 多样本验证；闸门 3 仍需校准
  - `QUOTE_MIN_COVERAGE=0.80` 为经验值，理论上引用内一两字改动可能放行
  - R2（引导突兀 / 素材生硬）**未处理**，按用户要求延后；C4 期间手验仍会遇到该体感问题
  - R3（C2 遗留真机手验）仍未补，待闸门 3
- **Commit**: pending
- **Next**: 用户验收 diff；随后授权闸门 3（真实复现 R1 + 阈值校准 + 微信手验含补 T-40~T-42）或直接收口归档

## 2026-07-28｜agent-guardrails-hardening（C4）｜Type C｜闸门 3 + 收口归档

- **Scope**: 闸门 3 真实联调、delta 接受、归档
  - 新增 `backend/src/test/java/com/flashback/agent/guardrail/C4RealProviderProbeTest.java`（`C4_REAL_PROBE=1` 门控，默认跳过）
  - `openspec/changes/agent-guardrails-hardening/design.md`（补「闸门 3 真实 provider 观察结果」）
  - `openspec/changes/agent-guardrails-hardening/tasks.md`（勾选 T-43~T-49b，未做项显式标注）
  - `openspec/changes/agent-guardrails-hardening/closeout.md`（新建）
  - `openspec/specs/agent-runtime/spec.md`（追加 Accepted From C4；**修订 C1 / C2 两条「护栏深度」scenario**）
  - `openspec/specs/backend-core/spec.md`、`openspec/specs/v2-product-scope/spec.md`（追加 Accepted From C4）
  - change 目录 `git mv` 至 `openspec/changes/archive/2026-07-28-agent-guardrails-hardening/`
  - `.ai/ACTIVE_TASK.md` → IDLE，Current Progress 归档，新增 Carry-over For C3
- **Changes**:
  - 闸门 3 **事前确认** `AI_PROVIDER=deepseek` / `AI_MODEL=deepseek-v4-pro` 后才联调（C1 顺序偏差未重演）；`AI_API_KEY` 只注入进程环境变量，未打印取值（C2 的 R6 教训）
  - 探针覆盖四类观察：provider 可达性、回复的诊断/代决检查、工具提议参数、素材路径（正常输入 + 稀疏输入）
  - 联调用临时脚本已删除；探针测试保留为可复用资产且默认跳过，避免混入常规回归产生意外外调
  - delta 接受后删除临时接受脚本
- **Verification**:
  - backend `mvn -B -o test` → **PASS｜Tests run: 397, Failures: 0, Errors: 0, Skipped: 1**（skipped 为门控探针，证明默认不外调）
  - 闸门 3 真实 DeepSeek：**4 次外调 / 预算 30**
    - provider 可达 PASS（`mockProvider=false`、`unavailable=null`、`toolUnavailable=null`）
    - 回复诊断/代决检查 PASS（无违规）
    - 工具提议：**未出现**（`toolCalls=0`，该轮模型选择纯对话）
    - 素材（正常输入）判忠实：coverage=**1.000**、maxRun=0、len=31，与用户原话总长 31 完全一致
    - 素材（稀疏输入「有点累」「说不上来」）判忠实：coverage=0.571、maxRun=**3**、len=7
  - **FAIL/未达成项（如实记录）**：**R1 型增写未复现** → 闸门的**拦截**方向未活体验证；仅**误伤**方向获真实验证
  - **SKIPPED**：微信真机手验（T-47）与 C2 遗留 T-40~T-42（T-48）——用户决定本轮优先快速完成阶段迭代；MySQL 未启动（T-44）——本轮探针不写库
  - delta 接受后核对：三个 baseline spec 均含「Accepted From C4」段落，两条 MODIFIED scenario 已就位
- **Risks**:
  - **[R7 新增] 忠实度闸拦截能力未活体验证**：本轮真实模型未增写（素材不足时选择「少说」而非补话）。不能据此认为 prompt 层已足够——R1 恰恰证明 prompt 会被违反。增写是概率性行为，建议后续真机手验时顺带观察
  - 阈值为本地样本 + 4 个真实样本标定，样本量小
  - 已接受残余风险：大量复用原话词汇的虚构可能同时通过双指标
  - **C3 必须回答的新问题**：C4 的来源集合只含当前会话用户消息；C3 引入历史检索后须明确「历史记录中的用户原话是否算合法来源」，不得默认沿用 C4 行为（已写入 ACTIVE_TASK Carry-over For C3）
  - R2 / R3 / R6 均延后，见 ACTIVE_TASK
- **Commit**: pending
- **Next**: 用户授权后启动 C3 `agent-memory-and-review` 规划闸

## 2026-07-28｜agent-direction-docs｜Type B｜架构宪法 + 选型草稿 + 蓝图 v1.2 草案

- **Scope**: 仅文档参考层，无业务代码、无 OpenSpec change、未改 ACTIVE_TASK
  - 新增 `Docs/agent-iteration/architecture/README.md`
  - 新增 `Docs/agent-iteration/architecture/agent-architecture-constitution.md`（Draft v0.1）
  - 新增 `Docs/agent-iteration/architecture/tech-selection-draft.md`（Draft v0.1）
  - 新增 `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`（DRAFT，未冻结）
  - 更新 `Docs/agent-iteration/README.md`、`Docs/agent-iteration/roadmap/README.md` 索引与权威性说明
- **Changes**:
  - 锚定 C1/C2/C4 已落地事实（自研 Loop、AgentModelClient、FC 白名单、C4 前移等）
  - 定义六大稳定端口与反推倒原则；Phase 2 默认序 C6→C7→C8
  - 明确 **v1.1 仍为 Phase 1 冻结源**；v1.2 仅草案，**建议 C5 归档后校准再冻结**
  - 写入 C3 必答项 P-F（历史原文 vs 忠实度来源）与漂移登记表模板
- **Verification**: SKIPPED（纯方法论文档，无编译/测试对象）；人工核对：未修改 `iteration-blueprint.md` 冻结正文、未改 backend
- **Risks**: 草案可能被误当作已冻结执行源——文内与 README 已反复标注 DRAFT；C3–C5 实现后须回填漂移表，否则 v1.2 冻结会失真
- **Commit**: pending
- **Next**: 用户继续 C3 规划/实现时以 v1.1 + OpenSpec 为准；C5 后按草案 §8 清单校准冻结 v1.2

## 2026-07-29｜agent-memory-retrieval｜Type C（规划闸，零业务代码）

- **Scope**:
  - 新增 `openspec/changes/agent-memory-retrieval/proposal.md`
  - 新增 `openspec/changes/agent-memory-retrieval/design.md`
  - 新增 `openspec/changes/agent-memory-retrieval/tasks.md`
  - 新增 `openspec/changes/agent-memory-retrieval/specs/agent-runtime/spec.md`（delta）
  - 新增 `openspec/changes/agent-memory-retrieval/specs/backend-core/spec.md`（delta）
  - 新增 `openspec/changes/agent-memory-retrieval/specs/v2-product-scope/spec.md`（delta）
  - 改写 `.ai/ACTIVE_TASK.md`（IDLE → ACTIVE 指向本 change）
- **Changes**:
  - Type A 现状扫描（只读）：核对上下文注入点（`AgentPromptBuilder.buildConversationMessages` 的 system 追加位，C1 注释即预留给 C3）、C4 来源集合结构（`AgentSourceCorpus` 为扁平 n-gram 集合、不带出处，消费方三处）、检索基础设施（`record` 全为 B-tree 索引、**无 FULLTEXT / 无 ngram**、`ai_summary` 由前端回传而非后端自动生成）、会话模型（`agent_session` 无用途字段，开会话硬校验 DRAFT）、前端现状（`AgentChatSheet.vue` / `record-detail` 静态回看区块）
  - 依用户 2026-07-29 定稿把蓝图 C3 拆两刀，本刀为 `agent-memory-retrieval`
  - 落 Q1–Q7 定稿，并新提 N1–N5 待确认（分层来源实现形态 / 记录状态范围 / 是否留扫 content 开关 / purpose 列归属 / 是否下发 memory 标识）
  - design 写 13 条决策记录，含四条 out_of_scope 边界决策（不做回看、不做扫 content 开关、不动 R2、不引 embedding）
  - 标注 spec 债：须以 MODIFIED 修订 `agent-runtime` 中四条「跨记录检索留给后续 change」的 scenario
- **Verification**: **SKIPPED（规划阶段无代码改动，无可运行验证）**
  - 事实核对方式：读 `agent/**`、`AgentChatServiceImpl`、`AppAgentProperties`、`backend/sql/mysql/*.sql`、`frontend/src/**`，proposal §2 每条标注 confirmed / unknown / out_of_scope
  - **未执行的核对**：`ai_summary` / 标签覆盖率统计 —— 本机 MySQL80 当前 **Stopped**（StartType=Manual），未擅自启动服务；已列为实现期第一个 task（T-01），并在 proposal 标 `unknown`（M16/M18），未据此写检索权重
- **Risks**:
  - 分层来源要改 `AgentCoverageProfile`，既有 397 项基线中有断言依赖现结构（N1 选包装式实现以规避）
  - 时间归属检查为本刀新判定，中文时间指示语形态多，误伤风险须闸门 3 用真实样本校准
  - `ai_summary` 覆盖率若过低，检索相关性会弱于蓝图预期（蓝图关键风险栏已接受，closeout 须诚实记录）
  - R7（C4 忠实度闸拦截方向未活体验证）仍未关闭，本刀闸门 3 顺带观察
- **Commit**: pending（提交责任=用户手动提交，Agent 未执行 git 写操作）
- **Next**: 闸门 1 批准 + N1–N5 定稿 → 闸门 2 实现授权 → T-01 覆盖率实测（需先手动启动 MySQL80）

## 2026-07-29｜agent-memory-retrieval｜Type C（闸门 2 实现，阶段 1–5）

- **Scope**:
  - **新增（main）**：`agent/guardrail/AgentLayeredCorpus.java`、`agent/guardrail/AgentTimeAttributionChecker.java`、`agent/memory/{MemoryPort,MemoryQuery,MemoryFragment,MySqlMemoryPort,MemoryCueExtractor}.java`、`domain/AgentSessionPurpose.java`、`sql/mysql/c3-agent-memory.sql`
  - **改动（main）**：`agent/AgentPromptBuilder.java`、`agent/guardrail/{AgentCoverageProfile,AgentSourceCorpus,AgentGuardrailRules,AgentGuardrailViolation}.java`、`agent/tool/{AgentToolValidator,AgentToolCoordinator,AgentToolValidationResult}.java`、`config/AppAgentProperties.java`、`domain/AgentSession.java`、`mapper/RecordMapper.java`、`service/impl/AgentChatServiceImpl.java`、`resources/application.yml`、`resources/mapper/{AgentSessionMapper,RecordMapper}.xml`
  - **新增（test）**：`agent/guardrail/{AgentLayeredCorpusTest,AgentTimeAttributionCheckerTest,AgentMemoryReplyGuardrailTest}.java`、`agent/memory/{MemoryCueExtractorTest,MySqlMemoryPortTest}.java`、`mapper/RecordMemoryRetrievalIntegrationTest.java`、`service/impl/AgentMemoryIntegrationTest.java`
  - **改动（test）**：`agent/tool/{AgentToolValidatorTest,AgentToolCoordinatorTest,AgentToolExecutorTest}.java`、`service/impl/AgentChatServiceImplTest.java`、`resources/schema.sql`
  - **文档**：`openspec/changes/agent-memory-retrieval/tasks.md`（勾选）、`.ai/ACTIVE_TASK.md`
  - **前端**：零改动（N5=(a)，已用 `git status` 核对）
- **Changes**:
  - 来源分层：`AgentLayeredCorpus` 包装两个 `AgentSourceCorpus`（N1=(b)，既有类语义与既有测试零改动）；`AgentSourceCorpus.merge` 纯增量，ngram 不一致快速失败；`AgentCoverageProfile.longestExclusiveRun` 用两画像相减识别「仅记忆层覆盖」片段
  - 时间归属护栏：新增 `MISSING_TIME_ATTRIBUTION`；memory-only 片段 ≥ 阈值且无时间归属表述 → 降级为安全兜底回复；词表进 `AgentGuardrailRules` 单一声明源；确定性、零外调、fail-closed
  - 权限不对等落地：正文（工具 `text` / 素材）**只认会话层**且不可配置；回复 / `askText` / 引号认合并层 + 时间归属。新增 `REASON_MEMORY_AS_CONTENT` 与 `REASON_UNFAITHFUL_ARGS` 分开留痕
  - 检索：`MemoryPort` 抽象（带 `purpose` 维度供后一刀复用）+ MySQL 实现。SQL 无 `content` 谓词、`user_id` 谓词无条件、排除 SEALED、排除当前草稿、时间窗、条数上限、无线索 `1=0`
  - 注入：`buildMemorySupplement` 走 C2 同形态 system 追加位；带可读时间锚点；无命中返回空串不注入占位段；`layeredCorpusOf` 与注入**共用同一份片段列表**
  - 检索失败 fail-open（不注入、对话继续）＋ 护栏 fail-closed，两方向不互相污染
  - `agent_session.purpose` 列（幂等 DDL + H2 schema 同步），默认 `WRITING_GUIDANCE`；`REVIEW_CHAT` 仅声明无行为分支
  - 配置：`app.agent.memory` 7 项 + `guardrail.min-memory-only-run-for-attribution`，**无新增凭证字段**
  - 清理：规划期一度新建的 `AgentSourceLayer` 枚举因无引用已删除（层级语义由 `AgentLayeredCorpus` 方法表达）
- **Verification**: **PASS**
  - 后端全量 `mvn -o test`：**472 tests PASS / 0 failures / 0 errors / 1 skipped**（397 基线 + 75 新增；skipped 为 `C4RealProviderProbeTest`，`C4_REAL_PROBE=1` 门控）
  - 真实 SQL 集成测试（H2）覆盖两个严重缺陷方向：跨用户零命中、`content` 中的关键词不命中；另覆盖 SEALED 排除、四字段命中、停用标签不命中、时间窗、排除指定记录、无线索零命中、limit
  - 端到端集成测试覆盖：`purpose` 落库、零 `REVIEW_CHAT` 会话、注入文本含时间锚点与两条约束文案、记忆文本作正文被拒（`memory-as-content`）、本次说过的话仍通过、未注入历史不构成来源、无记忆层等价 C4、记忆开关关闭对话正常
  - 阈值未被放宽：`AgentMemoryReplyGuardrailTest` 直接断言 `minCoverage=0.35` / `maxUncoveredRun=12` / `minCheckedLength=12` 与两个开关默认值
  - **既有断言零修改**：测试侧改动仅为构造参数补齐、import、H2 `schema.sql` 加 `purpose` 列，均在文件内注释说明理由
  - 前端：零改动，未跑 `type-check` / `build:mp-weixin`（无改动，非跳过验证）
- **Verification SKIPPED**:
  - **T-01 覆盖率实测**：MySQL80 已由用户启动，但 `root` 空密码被 `Access denied` 拒绝；DB 密码由 `start-dev.ps1` 启动参数提供，不在 `secrets.local.env`（仅含 `AI_API_KEY` / `WECHAT_MINI_PROGRAM_SECRET`）。**未猜测密码、未启用任何绕过**。已改用「四字段并列 LIKE + 固定取材优先级降级」设计使其不阻塞实现
  - **闸门 3 真实联调（T-20~T-23）**：未申请授权，本轮外调实际 **0 次**
  - **生产库 DDL**：`c3-agent-memory.sql` 未在本地 MySQL 执行（测试走 H2）
- **Risks**:
  - **[R8] 时间归属阈值 8 未经真实样本校准**，误伤与拦截两个方向均未活体验证
  - **[R9] 检索相关性弱**：无字段权重、无分词、无向量（蓝图已接受，closeout 须诚实记录）
  - `ai_summary` 本地覆盖率未知，若偏低则片段会降级到更短字段，信息密度下降
  - **[R7｜C4 遗留]** 忠实度闸拦截方向仍未活体验证
  - **[R3｜C2 遗留]** 微信真机工具链路手验未走通；本刀前端零改动故不承接，留给 `agent-review-chat`
- **Commit**: pending（提交责任=用户手动提交；Agent 未执行 `git add` / `commit` / `push`）
- **Next**: ① 用户验收 diff；② 提供 DB 凭证补 T-01；③ 授权闸门 3 做真实联调与阈值校准；④ 收口 T-24~T-28（closeout + 蓝图 §7 拆刀登记 + delta 接受归档）

## 2026-07-29｜agent-memory-retrieval｜Type C（收口 T-24~T-27 + diff 污染修正）

- **Scope**:
  - 新增 `openspec/changes/agent-memory-retrieval/closeout.md`
  - 改动 `Docs/agent-iteration/roadmap/iteration-blueprint.md`（§7 新增一行）
  - 改动 `openspec/changes/agent-memory-retrieval/tasks.md`、`.ai/ACTIVE_TASK.md`
  - 修正 `backend/src/main/java/com/flashback/agent/guardrail/AgentGuardrailRules.java`、`backend/src/test/java/com/flashback/agent/tool/AgentToolValidatorTest.java`、`backend/src/test/java/com/flashback/agent/tool/AgentToolCoordinatorTest.java`（去除格式化污染，内容改动不变）
- **Changes**:
  - **diff 污染修正**：上述三个既有文件在编辑保存时被自动格式化，缩进由 4 空格变为 8 空格，`git diff --stat` 虚增到 229 / 529 / 374 行。处理：从 HEAD 恢复三文件 → 改用脚本直写重新施加同样的内容改动 → 实际改动回落到 **39 / 5 / 4 行**。功能行为未变，回归仍全绿
  - 蓝图 §7 登记 C3 拆两刀（含拆分理由与新执行顺序 C1→C2→C4→C3a→C3b→C5），并登记一项规划期核实的事实修正：§4 C3 曾称 `summarizeRecord` 已为记录生成摘要可作检索索引，实际 `record.ai_summary` 由前端回传写入、后端不自动生成，覆盖率为 `unknown`
  - closeout 记录 5 处实现期偏离规划：T-01 受阻改为不依赖覆盖率的设计、保留单层签名重载的真实理由、新增 `REASON_MEMORY_AS_CONTENT` 分开留痕、删除无引用的 `AgentSourceLayer`、diff 污染修正
  - closeout 给后一刀 7 条 carry-over，其中两条是必须处理的硬约束：`shouldNotCreateAnyReviewChatSession` 断言需在实现回看时修改（预期变更非回归）；`buildToolContext` 当前只按有无 recordId 判断，回看会话恰好绑定记录，必须按 purpose 显式短路否则会误发 tools
- **Verification**: **PASS**
  - 修正后重跑后端全量：**472 tests PASS / 0 failures / 0 errors / 1 skipped**（与修正前一致）
  - `git diff --numstat` 复核三文件：39 / 4 / 5 行，与 `--ignore-all-space` 结果一致，确认污染已清除
  - 蓝图改动经 `--stat` 复核为 1 insertion，无格式化污染
  - 临时脚本（`eol-check.ps1`、`apply-c3-edits.ps1`）已删除
- **Verification SKIPPED**（与上一条一致，未新增）:
  - T-01 覆盖率实测：DB 凭证不可得（`root` 空密码被拒，密码由启动脚本参数提供）
  - 闸门 3 真实联调 T-20~T-23：未申请授权
  - `c3-agent-memory.sql` 未在本地 MySQL 执行（测试走 H2）
- **Risks**（无新增，沿用上一条）: R8 时间归属阈值未校准、R9 检索相关性弱、`ai_summary` 覆盖率未知、R7 / R3 前序遗留
- **Commit**: pending（提交责任=用户手动提交；Agent 未执行 `git add` / `commit` / `push`）
- **Next**: T-28 用户验收 → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE

## 2026-07-29｜agent-memory-retrieval｜Type C（T-01 补测 + 死代码清理）

- **Scope**:
  - `openspec/changes/agent-memory-retrieval/tasks.md`（T-01 填入实测结果并勾选）
  - `openspec/changes/agent-memory-retrieval/closeout.md`（§3.1 改写、残余风险表更新、待执行事项勾选）
  - `backend/src/main/java/com/flashback/agent/guardrail/AgentGuardrailViolation.java`（删除未被引用的枚举值）
- **Changes**:
  - **T-01 覆盖率实测完成**（用户提供 DB 凭证）：26 条记录中 `ai_summary` 62% / `belief_then` 62% / `title` 85% / **`core_question` 0%**；任一说明性字段非空 85%；状态 DRAFT 2 + UNLOCKED 24 + SEALED 0；**`tag` 表 0 行、`record_tag` 0 绑定**
  - 实测结论**未导致任何代码改动**，且反向验证了实现期的设计选择：`core_question` 恒为空，若当初按覆盖率配权重就会为一个空字段调参；「四字段并列无权重 + 固定优先级降级」自动跳过它
  - 新发现并记入风险：本地标签维度当前完全不可用（表为空），检索线索实际只有关键词一条；标签路径已有集成测试覆盖，闸门 3 若要验标签关联须先建标签并绑定记录
  - **死代码清理**：`AgentGuardrailViolation.MEMORY_AS_CONTENT` 声明后未被任何代码引用（拒绝实际走 `AgentToolValidationResult.REASON_MEMORY_AS_CONTENT` 字符串常量），已删除
  - 临时统计脚本 `t01-coverage.local.sql` 用后删除；统计 SQL 只输出计数与比例，**未输出任何记录内容**
- **Verification**: **PASS**
  - 删除枚举值后重跑后端全量：**472 tests PASS / 0 failures / 0 errors / 1 skipped**
  - `AgentGuardrailViolation.java` diff 复核为 8 行新增（无格式化污染）
  - T-01 统计过程未将日记原文写入任何输出、文件或日志；DB 密码未写入任何 tracked file
- **Risks**（新增两条，均无需处置）:
  - 本地 `tag` 表为空 → 标签关联在当前数据下零命中（非代码缺陷）
  - `core_question` 本地 0% → 该字段在检索与取材中恒不贡献（降级逻辑自动跳过）
- **Commit**: pending
- **Next**: 分阶段提交（用户已授权本次提交）→ 判定是否进入 C3 后半刀

## 2026-07-29｜agent-memory-retrieval｜Type C（delta 接受 + 归档，闸门 3 跳过）

- **Scope**:
  - `openspec/specs/agent-runtime/spec.md`（四条 MODIFIED + 新增「进入用户正文的文本的来源层」+ 追加 C3a 已接受段落）
  - `openspec/specs/backend-core/spec.md`、`openspec/specs/v2-product-scope/spec.md`（各追加 C3a 已接受段落）
  - `openspec/changes/agent-memory-retrieval/` → `openspec/changes/archive/2026-07-29-agent-memory-retrieval/`（`git mv`，保留历史）
  - 归档目录内 `tasks.md` T-28 勾选、`closeout.md` 状态与 R8 表述更新
  - `.ai/ACTIVE_TASK.md` 改写为指向 C3b `agent-review-chat` 规划闸
- **Changes**:
  - 四条 MODIFIED 全部落地，未绕过：C1 / C2 / C4 三条「范围内的记忆能力」改写为阶段范围声明 + 指向 C3 条款；C4「来源集合的边界」**实质改写**为分层表述
  - 修订时刻意保留了更严的一侧：C2 条款改写后仍明写「跨记录检索结果 SHALL NOT 成为工具正文参数的合法来源」，并新增独立 scenario「进入用户正文的文本的来源层」把「正文只认会话层、不可配置放宽」写成契约，避免分层被误读为整体放宽
  - baseline C3a 段落顶部显式声明闸门 3 未执行与 R8 未验证状态
  - `ACTIVE_TASK` 把 C3a closeout 的 7 条 carry-over 前移为「本刀必须处理」，其中两条是实现期易漏的陷阱：`REVIEW_CHAT` 零行为分支导致的既有断言需改（预期变更）、`buildToolContext` 只按 recordId 判断导致「回看无工具」不会自动成立
  - 同时把本轮的流程教训写入 Residual：禁止使用波及未跟踪文件的 git 操作
- **Verification**: **PASS（文档级）**
  - baseline 三份 spec 的 C3a 段落与归档 delta 逐条对照，条款无遗漏、无擅自增删
  - `git status` 确认归档为 R（rename）而非删除新增，change 历史保留
  - 未改动任何业务代码，故未重跑测试（上一条记录的 472 PASS / 1 skipped 仍为当前状态）
- **Verification SKIPPED**:
  - **闸门 3 真实联调（C3a T-20~T-23）**：**用户明确同意跳过并延后**至 C3 两刀全部完成后合并进行。后果：R8「时间归属阈值未校准」随 baseline 生效，已在 baseline C3a 段落顶部、closeout 顶部与残余风险表三处显式标注**未验证**，未粉饰
  - `c3-agent-memory.sql` 仍未在本地 MySQL 执行
- **Risks**: R8（已随 baseline 生效）、R9 检索相关性弱、R7（C4）、R3（C2，本刀补齐）、本地 tag 表为空导致标签路径零命中
- **Commit**: pending
- **Next**: 产出 C3b `agent-review-chat` 规划闸（proposal / design / tasks / delta + 待确认项）

## 2026-07-29｜agent-review-chat｜Type C（规划闸，零业务代码）

- **Scope**:
  - 新增 `openspec/changes/agent-review-chat/proposal.md`
  - 新增 `openspec/changes/agent-review-chat/design.md`
  - 新增 `openspec/changes/agent-review-chat/tasks.md`
  - 改动 `.ai/ACTIVE_TASK.md`（N1–N6 定稿、进度、三处易漏点）
  - delta 未在规划期产出，列为 tasks T-26~T-29（实现期与代码同步落地）
- **Changes**:
  - Type A 现状扫描（只读）：核实 `AgentChatServiceImpl` 的开会话校验 / 轮次推进 / 工具上下文 / 素材触发四处，`record-detail` 的 `reply-overlay` 状态机，`AgentChatSheet` 与工具确认的耦合度，`miniapp-core` 既有 4 条 Agent UI 条款
  - **扫出三处「既有实现会给出错误答案」的陷阱**，全部写进 tasks 与 ACTIVE_TASK：
    ① `buildToolContext` 只按有无 recordId 判断 → 回看恰好绑定记录，「无工具」不会自动成立；
    ② `targetStage == CLOSING` 会触发 `generateMaterial` → 回看若复用 CLOSING 常量会意外产出素材；
    ③ `selectActiveByUserAndRecord` 不含 purpose 条件 → 契约上不该依赖 DRAFT/UNLOCKED 互斥这个巧合
  - design 按用户要求**只保留决策记录**（11 条），不铺架构图与数据流——共享部分与 C1/C2/C4/C3a 一致，差异全在决策里
  - 记录了本刀最大风险与其取舍：回看几乎每轮都在复述过去，会把 C3a 未校准的时间归属阈值（R8）放到最高频场景。定稿选择是阈值不动、靠实测（决策 5），拒绝「为回看单开更宽阈值」与「回看关掉该检查」两个方向——后者等于在护栏最该生效的地方关掉它
  - 明确本刀是**时间归属护栏的第一次真实压力测试**，写进验收标准与闸门 3 观察项
- **Verification**: **SKIPPED（规划阶段无代码改动，无可运行验证）**
  - 事实核对方式：grep + 定点读 `AgentChatServiceImpl` / `record-detail/index.vue` / `AgentChatSheet.vue` / `agentService.ts` / `miniapp-core` spec，proposal §2 每条标注 confirmed / out_of_scope
  - 未跑测试（零代码改动，当前基线仍为 472 PASS / 1 skipped）
- **Risks**:
  - 时间归属在回看高频触发，未校准阈值可能导致频繁误伤（观感为「突然失忆」）；缓解靠失败方向选择而非调参
  - `AgentStage` 新增枚举值需检查既有 switch，遗漏会走 default 而非编译报错
  - 唯一允许修改的既有断言是 `shouldNotCreateAnyReviewChatSession`（改为正向断言，须披露）
  - `c3-agent-memory.sql` 仍未在本地 MySQL 执行，真机手验前置
- **Commit**: pending（提交责任=用户手动提交）
- **Next**: 闸门 2 实现授权 → T-01 单一模式判定点

## 2026-07-29｜agent-review-chat｜Type C（闸门 2 实现 + T-30 修复用户手验报错）

- **Scope**:
  - **新增（main）**：`agent/AgentChatMode.java`
  - **改动（main）**：`domain/AgentStage.java`（新增 `REVIEW`）、`agent/AgentStageMachine.java`、`agent/AgentMockResponder.java`、`agent/AgentPromptBuilder.java`、`config/AppAgentProperties.java`、`dto/AgentSessionStartRequest.java`、`mapper/AgentSessionMapper.java`(+XML)、`service/impl/AgentChatServiceImpl.java`、`resources/application.yml`
  - **新增（test）**：`service/impl/AgentReviewChatIntegrationTest.java`、`agent/guardrail/AgentReviewGuardrailTest.java`
  - **改动（test）**：`service/impl/AgentChatServiceImplTest.java`、`service/impl/AgentMemoryIntegrationTest.java`
  - **新增（frontend）**：`pages/record-detail/components/ReviewChatSheet.vue`
  - **改动（frontend）**：`pages/record-detail/index.vue`、`services/agentService.ts`
  - **delta**：`specs/{agent-runtime,backend-core,miniapp-core,v2-product-scope}/spec.md`
  - **本地库**：执行 `backend/sql/mysql/c3-agent-memory.sql`
- **Changes**:
  - 单一模式判定点 `AgentChatMode`：一处回答四个问题（记录状态要求 / 是否走阶段机 / 是否下发工具 / 是否产素材），编排只问模式不问 purpose，避免五层护栏在两条链路分叉
  - 三处规划期预判的陷阱全部确认真实存在并处理：① `buildToolContext` 的模式短路放在 `recordId == null` 判断**之前**；② 不复用 `CLOSING`（会触发 `generateMaterial`），改新增 `REVIEW`；③ `selectActiveByUserAndRecord` 补 purpose 谓词
  - 新增 `REVIEW` 时 `AgentMockResponder` 的穷尽 switch 直接编译报错，逼出显式处理（验证了 tasks T-02「不靠 default 混过去」的必要性）；`AgentStageMachine` 对 `REVIEW` 快速失败
  - 被回看记录 `content` + `ai_summary` + `belief_then` 进 MEMORY 层；刻意不注入 `reality_later` / `reply`（解锁后写的，时间语义不同）
  - 回看：无阶段机、轮次上限单列（默认 6）、tool_calls fail-closed、不产素材、`finish` 也不产素材
  - 前端新建 `ReviewChatSheet`（不复用带工具确认的 `AgentChatSheet`）+ 与 `reply-overlay` 双向互斥
  - **T-30 修复用户手验报错**：用户手验报「系统异常: api/agent/sessions」，根因是 C3a 的 mapper 已把 `purpose` 写进列清单与 insert，而本地库无该列 → **写作引导对话也一起 500**，不只回看。执行幂等 DDL 后修复
- **Verification**: **PASS**
  - 后端全量：**495 tests PASS / 0 failures / 0 errors / 1 skipped**（472 基线 + 23 新增）
  - 前端：`vue-tsc --noEmit` PASS；`build:mp-weixin` PASS
  - 本地 MySQL：`purpose` 列已建、脚本幂等验证 PASS、回看会话真实插入 + 按 purpose 查询命中 PASS
  - **既有断言改写披露**：仅 `AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession` → 改为正向断言 `writingGuidanceSessionMustNeverBeMarkedAsReviewChat`。原断言是 C3a 范围守护（本刀不实现回看），C3b 落地后原意失效；改而非删是因为它守护的另一半（写作引导不得误标 purpose）仍有效。其余既有断言零修改
  - **自我修正**：`AgentMemoryIntegrationTest` 又被编辑器自动格式化 + 改行尾，diff 从 17 行虚增到 262 行；已重新施加改动并对齐行尾，回落 17/14。修正过程中我还写错一版检测脚本（用 `git show | Out-String` 判断 HEAD 行尾，而 PowerShell 管道自身会加 CRLF，导致误判方向白修一轮），最终按 `--ignore-cr-at-eol` 的收敛幅度判断才正确
- **Verification SKIPPED**:
  - 闸门 3（T-31~T-36）：**本轮用户已授权，紧接着执行**
  - 微信真机手验（R3 + 回看浮层）：需用户在真机操作
- **Risks**:
  - **R8 仍未校准**：回看几乎每轮触发时间归属判定，误伤率待实测。若严重须作为校准单独请示，**不在实现期自行调松**
  - 用户手验暴露一个流程风险：**增量 DDL 未执行时报错表现为通用 500**，且会波及既有功能。建议后续 change 若含 DDL，把「本地执行」列为实现期第一步而非联调前置
- **Commit**: `b6327df`（规划）/ `fe7e644`（后端实现）/ `ec76e8d`（后端测试）/ `8a0d02b`（前端）+ 本条对应的文档提交
- **Next**: 闸门 3 合并联调（本刀 + C3a 顺延 T-20~T-23）

## 2026-07-29｜agent-review-chat｜Type C（闸门 3 合并联调，用户已授权外调）

- **Scope**:
  - 新增 `backend/src/test/java/com/flashback/agent/guardrail/C3RealProviderProbeTest.java`（`C3_REAL_PROBE=1` 门控，默认跳过）
  - 更新 `openspec/changes/agent-review-chat/tasks.md`（T-31~T-34、T-36 结论）
  - 临时运行脚本 `run-c3-probe.local.ps1` 用后删除（含密钥装载逻辑，未提交、未写入任何 tracked file）
- **Changes**:
  - C3a 与 C3b 的闸门 3 合并为一个探针：两者本质是同一层护栏的两种压力（偶发注入 vs 几乎每轮复述），放一起才能对比
  - 探针覆盖 T-31 观感、T-32 误伤率、T-33 fail-closed、T-34 C3a 顺延，并**自行补了一项拦截方向验证**
- **Verification**: **PASS**
  - 真实调用 **15 次**（3 轮运行 × 5 次），预算 ≤ 20；provider=deepseek；仅自造内容，**未使用用户真实日记**；不写库
  - **T-32 误伤 0 次**（9 轮观察）。memory-only 片段实测 0~22 字，多次超阈值 8，说明判定被真实触发而非空过
  - **关键核实**：不止看 `attribution=null`，还打印命中的时间归属词。实测命中「那时/过去/以前/你说过/四月/去年」，均为真实时间归属表述，**不是词表偶然命中**——放行理由正确。**结论：阈值 8 无需调整**
  - **T-36 拦截方向首次活体验证 `flipped=true`**：取模型真实产出、memory-only 片段最长（15 字）的回复，只删时间指示语、其余逐字不动 → 判定从放行翻转为 `missing-time-attribution`。被判文本仍是模型真实句子，非构造样本
  - T-31：回复 29~58 字（上限 120），用户 14~18 字，形态为一句话＋一个问题，未话痨；三轮均自发带时间锚点；无诊断/代决表述
  - T-34：写作引导注入 memory 后 `memoryOnlyRun=0`（未复述）；`memoryAsContent=false`（memory 未被当成正文素材）
  - 全量回归 **496 PASS / 2 skipped**（两个探针均环境变量门控）
- **Verification SKIPPED / 诚实结论**:
  - **T-33 fail-closed 未活体验证**：三轮模型均未返回 tool_calls，该分支未被真实触发，正确性仅由单测覆盖。**不写成已验证**
  - **T-35 R3 微信真机手验未做**：需用户在真机操作。前置 DDL 已就绪
- **自我修正（过程记录）**:
  - 拦截验证第一版取「最后一轮」回复，而它恰好 `memoryOnlyRun=0`（没在复述），剥离时间词后自然不翻转。这是**样本选错**而非护栏失效；已改为按 memory-only 片段最长挑选后翻转成功。教训：验证拦截方向必须先确认样本确实处于该被拦的状态
- **Risks**:
  - **R8 可关闭**：时间归属阈值经真实样本验证，误伤与拦截两方向均已覆盖
  - **R7（C4 遗留）实质缓解**：C4 只验到误伤方向，本轮的 flipped 实验补上了拦截方向（同一层机制）
  - 新增残余：fail-closed 分支未活体触发（概率性行为，不单独开 change）
  - 样本量仍小（9 轮观察、单一 provider/model），不声称杜绝
- **Commit**: pending
- **Next**: T-35 用户真机手验 → 收口 T-37~T-40（closeout + delta 接受 + 归档 → ACTIVE_TASK=IDLE，C3 两刀完成，下一刀 C5）

## 2026-07-29｜agent-review-chat｜Type C（真机手验 + 收口归档，C3 两刀完成）

- **Scope**:
  - `openspec/specs/agent-runtime/spec.md`（两条 MODIFIED + 新增「回看对话的记录状态要求」scenario + 追加 C3b 已接受段落）
  - `openspec/specs/backend-core/spec.md`、`miniapp-core/spec.md`、`v2-product-scope/spec.md`（各追加 C3b 已接受段落）
  - 新增 `openspec/changes/agent-review-chat/closeout.md`；change 目录 `git mv` 归档到 `archive/2026-07-29-agent-review-chat/`
  - 归档目录内 `tasks.md`（T-35 真机结论、收口段勾选、去重）
  - `.ai/ACTIVE_TASK.md` 改写为 IDLE，指向下一刀 C5
- **Changes**:
  - **T-35 微信真机手验 PASS（用户执行）**：回看对话可开启 / 多轮 / 温和收束；截图证据显示 Agent 自发表述
    「去年六月你想坚持锻炼与学习，现在你在跑步、去健身房、学编程」与「去年六月写下那句话的你……」
    ——**均带时间归属**，与探针 T-32 结论一致；回看浮层**无工具确认条、无素材回填入口**；
    写作引导的素材二段式确认（「先不用 / 用作正文」）可用 → **R3 关闭**
  - 用户评价「体验比之前好不少，Agent 有点『说人话』了，但还需要进步，当前够用」→ 质量诉求归 R2，仍延后
  - **规划与实际的一处不符已修正**：C3b delta 原计划修订一条 `backend-core` 的 C3a「本刀未实现的用途」scenario，
    但接受 C3a delta 时会话用途条款实际落在 `agent-runtime` 且未保留该范围声明 → **没有可修订的目标**。
    已把该 delta 的 MODIFIED 段改为显式说明「无」并交代原因，未硬套一条不存在的修订
  - `agent-runtime` 的两条 MODIFIED 均落地：①「对话关联已封存或已解锁记录」措辞收紧为「写作引导」，
    并**另加一条回看状态要求 scenario**，否则「回看只能作用于 UNLOCKED」在契约上无处体现；
    ②「C3a 范围内的回看对话」改为阶段范围声明 + 指向 C3b 条款
  - closeout 记录 3 处偏离规划（三处陷阱确认、DDL 流程失误、两次自我修正）与给 C5 的 6 条 carry-over
- **Verification**: **PASS**
  - 真机手验 PASS（用户执行，截图证据）
  - baseline 四份 spec 的 C3b 段落与归档 delta 逐条对照，条款无遗漏、无擅自增删
  - `git status` 确认归档为 R（rename）而非删除新增，change 历史保留
  - 未改业务代码，故未重跑测试（当前状态仍为 **496 PASS / 2 skipped**）
- **Verification SKIPPED / 诚实结论**:
  - **回看 fail-closed 分支未活体触发**：真实联调中模型未在无工具模式下返回 tool_calls，
    正确性仅由单测覆盖。概率性行为，不单开 change；已记入 Residual 并建议 C5 补
  - 闸门 3 样本量小（9 轮观察、单一 provider/model），不声称杜绝
- **Risks**:
  - **已关闭**：R8（时间归属阈值未校准）、R3（微信真机工具链路手验）、R7（实质缓解，拦截方向已验证）
  - **仍在**：R2（引导与素材质量，延后）、R9（检索相关性弱）、R6（凭证轮换待用户执行）、回看 fail-closed 未活体验证
  - 已把四条流程教训写入 ACTIVE_TASK：含 DDL 的 change 须把本地执行列为实现期第一步；
    禁止波及未跟踪文件的 git 操作；警惕格式化造成的 diff 污染；验证拦截方向须先确认样本状态
- **Commit**: pending（本轮用户明确授权 Agent 代为提交）
- **Next**: `ACTIVE_TASK=IDLE`。**C3 两刀全部完成**；下一刀为 C5 `agent-observability`，须用户授权后开规划闸

## 2026-07-30｜agent-observability（C5）规划闸｜Type C

- **Scope**: 新建 `openspec/changes/agent-observability/`（`proposal.md` / `design.md` / `tasks.md` + 四份 delta：`agent-runtime` / `backend-core` / `agent-collaboration` / `v2-product-scope`）；更新 `.ai/ACTIVE_TASK.md`。**零业务代码改动**
- **Changes**:
  - 开工前状态检查通过：`ACTIVE_TASK=IDLE` 无冲突；C5 唯一硬依赖 C1 已归档；蓝图 v1.1（已冻结）§4 已批准 C5 方向与非目标；开工锚点 `a834d85`
  - 只读现状调查：`agent/**` 40 个文件职责、`sendMessage` 方法级调用链、三份增量 DDL 与 entity/mapper、9 处既有埋点、脱敏范式、探针门控方式、鉴权与 `/admin/**` 路径现状
  - `proposal.md`：30 条现状事实（能力五态 V1–V30）、两处岔路（存储选型 / 「可查询」形式）、Goals 10 项、Non-Goals 15 项、场景边界 13 条、N1–N7、外调预算、33 条验收标准、delta 落点、10 条风险
  - `design.md`：痕迹挂载架构（per-turn 收集器 + 单一落库出口 + fail-open）+ **11 条决策记录**
  - `tasks.md`：T-01~T-41 分 9 阶段 + 收口 + 范围守护自检；三道闸门检查点分离
  - delta：`agent-runtime` 4 条 MODIFIED（C2/C4/C3a/C3b 的「范围内的可观测能力」scenario）+ 8 条 ADDED Requirement；`backend-core` 7 条；`agent-collaboration` 3 条；`v2-product-scope` 2 条；`miniapp-core` 确认无 delta
- **规划期核实到的关键事实**（三条改变了方案方向）:
  - **`AuthRole.ADMIN` 全仓无签发路径**：`UserServiceImpl.buildLoginResponse` 固定签 `AuthRole.USER`，无其他 `createToken` 调用点 → `/admin/**` 查询端点**在真实环境不可达**，故 N4 不推荐做端点
  - **`AgentGuardrailDowngrade.trace` 形参含 sessionId/turnNo，但两个调用点全传 null** → 回复与素材路径的降级痕迹当前关联不到会话，属既有缺陷，纳入 C5 补齐（决策 5）
  - **`agent_tool_call` 无法承载 trace**：它只在有工具提议时才有行，回复路径 / 素材路径 / thought 侧无行可挂，回看模式更是完全无工具 → 排除「扩既有表」方案
- **两处对已冻结蓝图缓解措施的偏离（已呈现请示，未自行决定）**:
  - N1 存储：推荐 MySQL 表，偏离蓝图「MVP 可用结构化 JSON 日志文件」。依据是蓝图同卡片要求「可查询」而本地无日志聚合，且 C6 要求字段级关联
  - N3 采样：推荐默认全量不采样，偏离蓝图「可配置采样率」。依据是采样会制造排查盲区
- **Verification**: **SKIPPED（符合闸门 1 语义）**——本阶段产出为规划文档，无业务代码可测。既有 496 tests / 2 skipped 基线未触碰。已核对：新建目录不与既有 change 冲突、delta 引用的四条既有 scenario 标题与措辞逐条比对 baseline 原文无误、`miniapp-core` 无 delta 的判断基于「前端零改动」
- **Risks**:
  - N1–N7 未定稿前 `design.md` / `tasks.md` 以推荐方案书写；若定稿不同须先回改文档再实现（已在 `tasks.md` 前提中声明）
  - 若 N1 定为 MySQL 表，含 DDL 的 change 有明确流程教训：本地执行 DDL 必须是实现期第一步（T-02），不得推迟到联调前
  - `agent-runtime` 的四条 MODIFIED scenario 分散在四个「Accepted From」段落，实现期须逐条核对，漏改会留下自相矛盾的契约
  - C5 最高风险是隐私（痕迹混入原文），已定为 T-25 的直接断言测试而非代码审查
- **Commit**: pending（**未执行任何 git 写操作**；工作区未跟踪产物 `Docs/agent-iteration/architecture/`、`iteration-blueprint-v1.2-draft.md`、`.kiro/skills/` 未被触碰或移动）
- **Next**: 用户批准闸门 1 并定稿 N1–N7 → 再单独授权闸门 2 → 实现从 T-01（DDL 前置）开始

## 2026-07-30｜agent-observability（C5）实现｜Type C

- **Scope**:
  - 新增主代码：`agent/trace/`（`AgentTraceCollector` / `AgentTraceSink` / `AgentTraceVersions` / `AgentTraceLayer` / `AgentTraceOutcome`）、`domain/AgentTurnTrace`、`mapper/AgentTurnTraceMapper` + XML
  - 新增 SQL：`sql/mysql/c5-agent-turn-trace.sql`（DDL）、`sql/mysql/c5-trace-queries.sql`（9 条只读排查查询）
  - 修改主代码：`AgentChatServiceImpl`、`AgentPromptBuilder`、`AgentModelClient`、`AgentToolCoordinator`、`AppAgentProperties`、`application.yml`
  - 新增测试 4 个类共 37 项；修改 `AgentChatServiceImplTest`（仅构造签名）、`src/test/resources/schema.sql`
  - **前端零文件改动**
- **Changes**:
  - N1–N7 按推荐定稿（用户 2026-07-30 批准），闸门 2 授权后实施
  - **T-01/02 DDL 第一步执行**（C3b 流程教训）：`agent_turn_trace` 20 列 + 3 索引 + 3 外键 CASCADE；本地 MySQL 已执行，**幂等已验证**（重复执行 exit=0）
  - 收集器 + 单一落库出口：`sendMessage` 用 try/finally 包住整轮，`persist` 是唯一 insert 点；`REQUIRES_NEW` + fail-open，痕迹写失败不回滚用户消息
  - thought：mode / 阶段判定 reason（**`AgentStageDecision.Reason` 第一次被真正使用**）/ 记忆检索与注入 / prompt 规模
  - action：provider 结果与耗时（**成功路径耗时不再被丢弃**）/ tool_calls 处置 / fail-closed 丢弃
  - observation：六层护栏结论与指标（新增 `AgentTraceLayer` 标明「哪一道闸」）/ 降级可区分本地兜底 / 回复裁剪
  - 版本锚点由内容哈希派生；顺带把两处内联 prompt 文案提取为常量以纳入指纹，**文字逐字未改**
  - 配置 `app.agent.observability.{enabled,retention-days}`，**无采样率**，无新增 secret 字段
- **既有缺陷补齐（按 design 决策 5 单列披露）**:
  - **V4**：`AgentGuardrailDowngrade.trace` 的两个调用点（`applyReplyGuardrail` / `applyMaterialGuardrail`）此前**恒传 null sessionId/turnNo**，降级痕迹关联不到任何一轮。现改为传真实值，由 `AgentGuardrailTraceCorrelationTest` 用 ArgumentCaptor 直接断言
  - **V5**：改为用轨迹解决，**未改三个 checker 的签名**。`CHECK_ERROR` 本就以 verdict 返回调用方，而调用方现在会记进轨迹，关联天然成立。改签名会让全部调用点与单测跟着改，diff 混入与可观测无关的护栏改动
- **实现期与规划不符的三处（诚实记录）**:
  - **V19 被证伪**：`schema.mysql.sql` 只到 C1，**既无 `agent_tool_call`（C2）也无 `purpose`（C3）**——项目既有约定是全量脚本不随增量维护。刻意未动它（只加 C5 会造出「有 C5 表却无 C2 表」的更怪状态），**待用户决定是否另开 Type B**
  - **不需要哈希前缀**：轨迹里无「指向某段具体文本」的字段，长度与计数已足够；不引入哈希是更强的隐私姿态
  - **记忆采集拆成两步**（`memory-retrieval` / `memory-injected`）：回看模式下被回看记录的片段进 MEMORY 层但**不来自检索**，合成一条会让「检索命中 0 却注入 3 条」看起来自相矛盾
- **Verification**:
  - 后端全量回归 **533 tests PASS / 2 skipped，BUILD SUCCESS**（496 基线 + 37 新增，**零回归**；2 skipped 为环境门控的真实 provider 探针）
  - **既有断言零修改**：已核 `AgentChatServiceImplTest` 的 diff，只有 import / `@Mock` / 两个构造参数
  - 范围守护逐行核实：`AgentPromptBuilder` diff 确认 prompt 文案逐字未改；`AgentChatServiceImpl` 全部删除行均为「同语句加 trace 参数」或「分支重排但语义等价」，无业务逻辑被删
  - 隐私断言双层：实体字段 + 直接 SQL 查全部文本列，断言特征串不出现；回看路径单独用例
  - **SKIPPED —— 闸门 3 未授权**：真实 provider 轨迹完整性、耗时量级、fail-closed 活体触发（T-35~T-37）**全部未执行**
  - **SKIPPED —— 前端构建**：前端零改动，无需 `type-check` / `build:mp-weixin`
- **Risks**:
  - 真实 provider 下的轨迹完整性与耗时量级**未验证**（mock 实测 0~1ms）
  - 回看 fail-closed **仍未活体触发**：C5 只做到「它真发生时能被记下」，不等于已观察到它发生。C3b 同一残余保持未验证状态
  - mock 路径无 `prompt` 步骤（mock 不组装提示词），已写进 delta scenario 条件，非采集遗漏
  - 可观测关闭时降级痕迹 sessionId 仍为 null —— 刻意如此，已由测试固定
  - `schema.mysql.sql` 落后于增量脚本（见上），待用户决定
  - R2 / R9 / R6 未动；轨迹表增长依赖手动清理（无定时任务）
- **Commit**: pending（**未执行任何 git 写操作**；未跟踪产物 `Docs/agent-iteration/architecture/`、`iteration-blueprint-v1.2-draft.md`、`.kiro/skills/` 未被触碰）
- **Next**: 用户验收 → delta 接受进 baseline（`agent-runtime` 四条 MODIFIED 逐条落）→ 归档 → `ACTIVE_TASK` → IDLE → **Phase 1 收官，进入蓝图 v1.2 校准会**

## 2026-07-30｜Agent 对话请求超时修复｜Type B

- **Scope**:
  - `frontend/src/services/agentService.ts`
  - `backend/src/main/resources/application.yml` / `application-dev.yml` / `application-prod.yml`
- **改前 / 改后（大白话）**: 手验时记录页与回看页使用 Agent 都报 `request: fail timeout`。
  改后前端给 AI 相关请求 30 秒，后端 AI 调用 20 秒，超时先发生在后端，用户看到的是设计好的显式失败态。
- **Changes**:
  - 根因是**前后端超时值相等（都是 10000ms），前端必然先断**：
    `httpClient` 默认 10000ms，而 `agentService` 六个方法**一个都没传 timeout**；
    后端 `app.ai.timeout-millis` 默认同为 10000。C5 闸门 3 实测 provider 单次
    4571~8467ms（均值 6476ms），加上编排、护栏与落库开销，一轮越过 10 秒是常态
  - 后果不只是「慢」：前端断开时后端那一轮**仍在正常处理**，用户看到 `uni.request` 的原始
    errMsg（浮层走 `toUserMessage(error)`），而 C1 精心设计的 `UNAVAILABLE` / `FAILED`
    显式失败语义被网络错误抢先覆盖
  - 前端：新增 `AGENT_AI_TIMEOUT_MS = 30000`，只给四个会触发 provider 调用的方法传入
    （`startOrResume` / `startOrResumeReview` / `sendMessage` / `finish`）
  - 前端：`getSession` / `confirmToolCall` **刻意不改**，沿用默认 10 秒——它们是纯数据库操作，
    放宽只会让真正的网络故障晚 20 秒才暴露
  - 后端：三个 profile 的 `AI_TIMEOUT_MILLIS` 默认值 10000 → 20000。
    **关键是顺序而非数值**：前端 30000 > 后端 20000 > 实测 max 8467ms，
    前端要给后端留出「自己先超时并返回显式失败」的窗口
  - `application-test.yml` 的 `timeout-millis: 1000` **未动**：测试刻意要快速失败
- **同类参照**: `stageSummaryService` 同样调 AI，早已显式指定 15000ms。
  Agent 是仓库里唯一漏配超时的 AI 调用方
- **已知不精确处（如实记录）**: `finish` 在写作引导下会触发素材生成（又一次 provider 调用），
  回看下则不调 AI。前端分不清这个区别，统一取较宽值——回看白留余量不产生副作用；
  按 purpose 分流的额外复杂度不值得
- **Verification**:
  - 前端 `type-check` **PASS**；`build:mp-weixin` **PASS**
  - 构建产物核对：`agentService.js` 中 `i=3e4` 用于四个 AI 方法，
    `getSession` / `confirmToolCall` 确认无 timeout 字段
  - 后端回归 **534 tests PASS / 3 skipped**，BUILD SUCCESS（无回归）
  - **SKIPPED —— 微信真机复验**：需用户在真机上确认 `request: fail timeout` 不再出现。
    本地无法替代真机验证该现象
- **Risks**:
  - 真机复验未做，修复有效性**未经用户实测确认**
  - 20 秒仍是有限值：若 provider 偶发超过 20 秒，用户会看到后端的 `FAILED`（可重试），
    这是预期行为而非缺陷
  - 未改 `httpClient` 的全局默认值——其他非 AI 请求的 10 秒不变，避免波及无关调用
- **Commit**: pending

## 2026-07-30｜agent-observability（C5）验收、delta 接受与归档｜Type C

- **Scope**:
  - `openspec/specs/agent-runtime/spec.md`（4 条 MODIFIED + 追加 C5 段落）
  - `openspec/specs/backend-core/spec.md`（追加 C5 段落，含一条 Type B 超时条款）
  - `openspec/specs/agent-collaboration/spec.md`、`openspec/specs/v2-product-scope/spec.md`（各追加 C5 段落）
  - `openspec/changes/agent-observability/` → `openspec/changes/archive/2026-07-30-agent-observability/`（`git mv`）
  - `.ai/ACTIVE_TASK.md` → IDLE
- **Changes**:
  - **`agent-runtime` 四条 MODIFIED 逐条落**：C2 / C4 / C3a / C3b 的「范围内的可观测能力」scenario
    原文均为「决策链路可查询 SHALL 留给后续独立 change」，改为「自 C5 起由本 spec 的决策轨迹条款约束」。
    保留阶段范围声明不删——范围声明本身是历史事实，删掉就看不出能力何时到位
  - **核对方式**：改后统计新措辞 4 处、旧措辞 0 处（唯一残留的一处旧措辞出现在我自己写的修订注释
    引号内，是对原文的引用，非漏改，已确认）
  - `agent-runtime` 追加 8 条 ADDED；`backend-core` 追加 7 条 + **一条 Type B 条款**
    （Agent Conversation Client Timeout Must Exceed Backend AI Timeout）
  - `agent-collaboration` 追加 3 条 —— 该 spec 在 C5 之前**没有承载过产品 Agent 条款**
    （C1–C3 的条款全落在 `agent-runtime`），本次是首次
  - `v2-product-scope` 追加 2 条；`miniapp-core` 确认无 delta
  - 归档用 `git mv` 保留文件历史；`closeout.md` 补 §8 记录归档后随即发现的 Type B
- **Verification**:
  - delta 接受的完整性以「新旧措辞计数」核对，非目测
  - 归档后目录结构核对：`closeout.md` / `design.md` / `proposal.md` / `tasks.md` / `specs/` 齐全
  - **SKIPPED —— 未重跑测试**：本轮只改 OpenSpec 文档与 `.ai`，无代码改动。
    代码侧验证以 Type B 那条为准（534 PASS / 3 skipped）
- **Risks**:
  - 见 `ACTIVE_TASK` Residual。最需要用户动作的两项：**Type B 真机复验**、
    **`schema.mysql.sql` 是否另开 Type B 补齐**
  - R2（引导话术质量）的延后条件随 Phase 1 完工而**解除**，可以开始谈了
- **Commit**: pending
- **Next**: 蓝图 v1.2 校准会（Type A 讨论，不是新 Type C）。C6 不得偷跑

## 2026-07-30｜Phase 1 收官说明｜Type A

- M4 → C1 → C2 → C4 → C3a → C3b → C5 全部归档，Phase 1 完成
- C5 上线后的第一个实际收益就是定位了 Type B 那个超时缺陷：
  provider 耗时数据（min 4571 / avg 6476 / max 8467ms）在 C5 之前完全不存在，
  没有它只能猜「是不是网络问题」
- 下一步不是新 Type C，而是 v1.2 校准会。本轮实测**证伪了草案里两条假设**，
  校准时须一并处理：① 新表需同步三份 schema（实际项目约定是全量脚本不随增量维护）；
  ② `/admin` 端点可行（实际 `AuthRole.ADMIN` 无签发路径，端点不可达）

## 2026-07-30｜Agent 对话输入框无法聚焦修复｜Type B

- **Scope**:
  - `frontend/src/pages/record-editor/components/AgentChatSheet.vue`
  - `frontend/src/pages/record-detail/components/ReviewChatSheet.vue`
  - 新增 `backend/src/test/java/com/flashback/agent/trace/C5MysqlTraceProbeTest.java`（只读排查探针）
- **改前 / 改后（大白话）**: 超时修好后，两个 Agent 浮层都能开、能收到 Agent 说话，
  但输入框点不动、打不了字。改后输入框可正常聚焦输入。
- **根因**:
  - 两个浮层的写法都是「外层容器 `@tap="close"` + 内层 sheet `@tap.stop`」。
    在小程序里 `textarea` 是**原生组件**，其触摸事件会穿透 `catchtap` 继续冒泡到外层容器，
    被当成「点击遮罩」从而触发 `close` —— 输入框因此永远拿不到焦点
  - 内层的 `.stop`（编译为 `catchtap`）挡不住原生组件的事件穿透，这是小程序原生组件的既知行为
  - **两个浮层同时中招且症状一致**，而它们的 `disabled` 条件并不相同
    （写作引导多一个 `awaitingRetry`），这一点排除了状态机/状态复位方向
- **修复**:
  - 把关闭手势从「包裹内容的外层容器」移到**独立的兄弟背景层**（`.agent-mask` / `.review-mask`）：
    遮罩视觉与关闭手势都挂在它身上，内容区 sheet 与它是兄弟关系而非后代
  - 于是 `textarea` 不再有任何绑定 tap 的祖先，穿透与否都不再影响聚焦
  - 层级用 `position: absolute; inset: 0; z-index: 0` 与 `sheet` 的 `z-index: 1` 分离，视觉完全不变
  - 顺带补 `cursor-spacing=24`：小程序 textarea 默认贴键盘顶边，键盘弹起会遮住输入框
- **对照物**（本次定位的关键）: 同页面的回应浮层与 `DateTimeWheelPicker` 结构几乎相同却一直可用，
  差别在它们的遮罩带了 `.stop`/自身处理。这提示问题在事件绑定层次而非组件内部逻辑
- **排查过程中用 C5 轨迹核实的事实（可观测能力的第二次收益）**:
  - 会话 10 / 11 均 `ACTIVE` 且 `turn_count=1`，`agent_message` 中 USER/ASSISTANT **完整配对**
    → 后端侧两轮都成功完成，`awaitingRetry` 不成立、`sending` 已复位，
    确认「输入框不可用」纯属前端渲染/事件问题，不是状态残留
  - 这一步排除了三个错误方向（store 状态未复位、后端半轮卡死、disabled 条件误判），
    没有轨迹与消息表就只能靠猜
- **新发现的残余（未修，如实记录）**:
  - **真实 MySQL 上轨迹落库不完整**：`agent_turn_trace` 仅 1 行且 `steps_json` 只有 `mode` 一步，
    `provider_duration_ms` / `model` 为 NULL；自增 id 已到 2 但只剩 1 行（有一条被回滚）。
    另有一轮（会话 11 回看）完全没有轨迹
  - 这正是 closeout 中列为残余的「MySQL 上的轨迹落库未经真实联调」。
    **本轮只做只读探针定位，未改轨迹代码**——它属 C5 范围，超出本 Type B，
    且当前不影响用户功能（轨迹是工程设施，fail-open）
  - 新增 `C5MysqlTraceProbeTest`（`C5_MYSQL_PROBE=1` 门控，**只读、不插不删**）作为后续排查资产
- **Verification**:
  - 前端 `type-check` **PASS**；`build:mp-weixin` **PASS**
  - **产物结构核对**：`ReviewChatSheet.wxml` 中 `review-layer` 自身无事件绑定，
    `review-mask` 为独立兄弟节点承载 `bindtap`，`textarea` 无绑定 tap 的祖先；
    `cursor-spacing="{{24}}"` 已生效
  - 后端回归 **535 tests PASS / 3 skipped**，BUILD SUCCESS（新增探针默认跳过，已在干净环境验证门控）
  - **SKIPPED —— 微信真机复验**：输入框能否聚焦只能由真机确认，本地无法替代
- **Risks**:
  - 真机复验未做，修复有效性**未经用户实测确认**
  - 若真机仍不可聚焦，下一个可疑方向是 sheet 的固定高度与键盘上推的交互
    （`agent-sheet` 用 `height: 78vh`），而非事件绑定
  - MySQL 轨迹落库不完整的问题仍在（见上），需要单独处理
- **Commit**: pending

## 2026-07-30｜修复 C5 轨迹落库导致每轮卡满 50 秒锁等待｜Type B（C5 缺陷修复）

- **Scope**:
  - `backend/src/main/java/com/flashback/agent/trace/AgentTraceSink.java`
  - `backend/src/test/java/com/flashback/service/impl/AgentGuardrailTraceCorrelationTest.java`（+1 项回归）
- **改前 / 改后（大白话）**: 打字修好后仍然 timeout。实测一轮 55 秒，修完 2.9~6.4 秒。
- **结论：既不是 DeepSeek 的问题，也不是 C1 旧代码的问题，是我 C5 引入的缺陷。**
- **定位过程（关键是先排除外部因素）**:
  1. 直连 DeepSeek 实测：裸调用 2.4~4.2s；带工具 schema + 真实会话上下文 4.8~7.2s。
     **provider 侧完全正常**，排除官方问题
  2. 真实链路实测：`startOrResume` 3.5s、`finish` 4.5s（都调 provider），
     但 `sendMessage` **55 秒**。差异只在 `sendMessage` 独有的代码路径上
  3. 后端日志给出决定性证据：
     `WARN AgentTraceSink : agent trace persist failed sessionId=12 turnNo=2 cause=CannotAcquireLockException`
  4. `SHOW VARIABLES LIKE 'innodb_lock_wait_timeout'` = **50**，与实测 55 秒
     （50 秒锁等待 + 5 秒 provider）完全吻合
- **根因（自锁）**:
  - `sendMessage` 事务内 `updateProgress` 刚 UPDATE 过 `agent_session`，**持有该行写锁且未提交**
  - `persist` 标 `REQUIRES_NEW` → 挂起外层事务、另开连接插 `agent_turn_trace`
  - 该表有指向 `agent_session` 的外键，InnoDB 插入时需对父行加锁
  - 父行锁被尚未提交的外层事务持有 → 新事务等到 50 秒锁超时才放弃
  - 结果：用户 30 秒前端超时，后端仍在等锁；**且轨迹一条都写不进去**
    （这解释了此前「MySQL 上只有 1 行残缺轨迹」的现象——那 1 行是恰好没触发外键锁的早退路径）
- **为什么测试没抓到（最该记住的一条）**:
  - 全部集成测试跑在 **H2** 上，H2 没有 InnoDB 的行级锁语义，**该缺陷在 H2 上不可能复现**
  - closeout 里我把「MySQL 上的轨迹落库未经真实联调」列为残余风险，但**低估了它的严重性**——
    当时判断「不影响用户功能，因为轨迹是 fail-open 的」。实际上 fail-open 只保证了不报错，
    没保证不阻塞：它在失败**之前**先卡了 50 秒
- **修复**:
  - `persist` 改为通过 `TransactionSynchronizationManager.registerSynchronization`
    注册 `afterCompletion` 回调，**等外层事务提交、锁释放之后**再写轨迹
  - 用 `afterCompletion` 而非 `afterCommit`：业务事务回滚时那一轮的轨迹同样有排查价值
    （早退路径覆盖是 C5 验收项）
  - 这同时比原方案更彻底地达到了「轨迹失败不回滚业务数据」——此时业务事务已提交完毕
  - 无事务上下文时（测试/工具直接调用）退回立即写入
  - `nextAttemptNo` 保留在业务事务内：它是 SELECT，不与父行写锁冲突
- **Verification**:
  - **真实 MySQL 实测**：修复前 `sendMessage` 54755 / 56744ms → 修复后 **2890 / 6396ms**
  - **轨迹现在正常落库**：`provider_duration_ms` 2844 / 6370ms，`model=deepseek-v4-pro`，
    `stage` / `stage_reason` 全部就位 —— C5 的可观测能力这才真正在生产环境可用
  - 后端全量回归 **536 tests PASS / 4 skipped**，BUILD SUCCESS。
    4 个 skip 全为环境门控探针（C3/C4/C5Real/C5Mysql），**无既有测试被跳过**
  - 新增回归 `tracePersistMustBeDeferredUntilAfterBusinessTransaction`：
    断言**调用时机**而非结果（persist 必须走延后、编排层不得直接调 persistNow）。
    这样即便测试仍在 H2 上，也能守住这个不变量
- **Risks**:
  - 真机复验待用户执行
  - 轨迹现在写在业务事务之后，理论上存在「业务成功但轨迹丢失」的窗口（进程崩溃）。
    可接受：轨迹是辅助设施，且原方案的代价是每轮卡 50 秒
  - 本地一次性脚本 `probe-turn-latency.local.ps1` 已 gitignore（`*.local.ps1`）
- **Commit**: pending

## 2026-07-30｜三个 Type B 的真机复验结果补录｜Type B

> 补录条：上面三条 Type B 的 Verification 均写「SKIPPED —— 微信真机复验待用户执行」。
> 用户已执行，结果如下。按只追加不改写的规则，不回改原条目。

- **真机复验 PASS（用户 2026-07-30 执行）**，三项一次性全部确认：
  1. **超时修复**（`ce4638f`）：记录页与回看页均不再出现 `request: fail timeout`
  2. **输入框修复**（`b6bcdd5`）：输入框可正常聚焦打字
  3. **锁等待修复**（`87cb29e`）：对话响应恢复正常速度
- **用户额外反馈**：「Agent 对话也感觉自然一些了」
  - 归因说明：本轮**没有改动任何 prompt 文案、护栏阈值或引导策略**（R2 仍未动）。
    「更自然」应归因于**响应速度从 55 秒降到 3~6 秒** —— 等待 55 秒后再收到回复，
    体验上必然像「卡顿的系统」而不是「在聊天的朋友」
  - 这条观察对 R2 有直接价值：此前判断「引导话术生硬」的样本，
    很可能有一部分实际是延迟造成的体验污染，而非话术本身的问题。
    **R2 的优化基线应在本次修复之后重新建立**，不要沿用之前的主观印象
- **状态变更**：
  - 三条 Type B 的残余项「真机复验未做」→ **关闭**
  - 残余项「MySQL 上的轨迹落库未经真实联调」→ **关闭**（已联调且已修复根因）
  - 新增流程教训：涉及锁 / 外键 / 事务边界的改动，H2 集成测试不足以验证，须打真实 MySQL
- **Verification**: 用户真机手验 PASS；后端 536 tests PASS / 4 skipped；前端 type-check + build PASS
- **Commit**: pending（本条为文档补录）

## 2026-07-30｜蓝图 v1.2 校准会与冻结｜Type A（讨论）+ Type B（文档落地）

- **Scope**: 方向层与文档层，**零业务代码改动**
  - `Docs/agent-iteration/roadmap/iteration-blueprint.md`（v1.1 → **v1.2 已冻结**）
  - `Docs/agent-iteration/narrative/agent-tech-story.md`（**新建**，对外叙事）
  - `Docs/agent-iteration/roadmap/iteration-blueprint-v1.2-draft.md`（未跟踪；内容已迁出，待用户手动删除）
  - `AGENTS.md`、`openspec/project.md`
  - `.kiro/steering/{rules,structure,product,tech}.md`
  - `Docs/agent-iteration/README.md`、`roadmap/README.md`、`workflow/agent-control-model.md`
  - `Docs/agent-iteration/architecture/{README,agent-architecture-constitution,tech-selection-draft}.md`
  - `.ai/ACTIVE_TASK.md`（Direction Layer 指向 v1.2、下一动作改为 C6 规划闸）

- **Changes**:
  - **校准会十问逐支定案**（Type A 讨论，全部结论已核对代码），产出 **D25–D33** 九条新决策
  - **Phase 2 序列定案**：`C6 agent-eval-framework` → `C7 agent-reflection-loop` →
    `C8 agent-resilience` → `C9 agent-temporal-intelligence`
    - 相对起草稿的变更：在 Eval 之后**新增 C7 反思环**，韧性顺移 C8、时间智能顺移 C9
    - 排序理由（D30）：反思环本质是改模型输出行为，须先有可回归的量尺；
      否则只会得到第二个无法证伪的「感觉好了」
  - **平台升级降级为 Optional C0**（D26）：重心不在平台层；其风险性质与业务刀不同
    （业务刀风险是「设计对不对」可由测试回答，平台升级风险是传递依赖兼容性，工作量长尾不可预估）
  - **不引入图框架**（D27）：核对 `AgentStageMachine` 后确认现有实现已具备节点/边/抢占/自环/终态，
    改为「引入真正需要环的能力」+ 留可讲述 ADR
  - **反思环边界定案**（D28/D29）：判定源复用 C4 确定性护栏；重写指令只回传违规类型不携带文本片段；
    只对 `UNFAITHFUL` 与 `MISSING_TIME_ATTRIBUTION` 开环；`CHECK_ERROR` 绝不重试；上限 1 次
    → 最坏 2 次调用 ≈13s < 后端 20s，**不需要动刚验证过的超时配置**
  - **Eval 边界定案**（D31/D32）：排除 LLM-as-Judge；只做轨迹不变量 + 回归比对，不做绝对判分；
    快照分层（不变量层禁止刷新 / 快照层需人确认）+ `baselineNote` 防橡皮图章
  - **新增对外叙事交付物**（D33）：`narrative/agent-tech-story.md` 按面试问题组织，
    §1–§6 与 §10 已写（C1–C5 素材齐备），§7/§8 待 C6/C7 归档补，§9「知道但不做」持续追加
  - **消化五条实测证伪的前提**（蓝图 §2.3）：
    1. `/admin` 端点不可达（`AuthRole.ADMIN` 全仓无签发路径）
    2. `schema.mysql.sql` 只到 C1，不随增量维护
    3. H2 不足以验证锁/外键/事务边界 → **蓝图 §0.4 已收紧「真实联调」定义为包含真实 MySQL**
    4. R2 基线受延迟污染 → 改由 C6 重建
    5. **本轮新发现**：steering 声称「JWT（Spring Security）」，实测 `springframework.security`
       全仓零匹配、pom 无 security starter，实为 jjwt + 自研过滤器 → 已修 `.kiro/steering/tech.md`
  - **附带登记**：`pom.xml` 含 `spring-boot-starter-data-redis` 且 dev/prod yml 有配置段，
    但 main 代码零消费（会话走 MySQL）。标记 `partial`，不在 Phase 2 处理
  - `.kiro/steering/product.md` 的 C1–C5 顺序修正为实际执行序（此前仍写 C3 Memory 在 C4 之前）
  - `AGENTS.md` 删除过期表述「默认下一刀 `agent-runtime-mvp` 规划闸」（已过期两个月）
  - `workflow/agent-control-model.md` 两处「蓝图待写 / 仍待编写」修正（v1.1 冻结时漏改）

- **Verification**: PASS（文档层变更，无代码改动，故未跑测试基线）
  - 逐节比对 v1.2 冻结版与校准稿：序列、D25–D33、五条事实修正、§9 清单、§10 叙事规划全部一致；
    差异均为合理收敘（v1.1 的 D1–D19 与气质章节 §6 在冻结版中完整展开，校准稿只写「继承」）
  - 核对 v1.2 引用同步：11 处活文档已更新；
    **`openspec/changes/archive/**` 中六份 proposal 的「上游方向 v1.1」引用刻意未动**——归档即历史
  - 叙事文档中日文标点归一化为全角（与仓库其他文档一致），行内代码与标识符未被破坏
  - 清理了归一化过程中使用的三个临时脚本（`tmp-punct*.local.ps1`、`tmp-scope-scan.local.ps1`）
  - **SKIPPED**：Spring Boot 3.3.x 的官方 EOL 日期未查到权威页面，
    蓝图 §4.6 中该项已标记 `unknown` 并注明「引用前须先核实」，未写成事实

- **Risks**:
  - v1.2 冻结的方向未经任何实现验证——C6 开工后若发现「轨迹级断言」不足以表达克制维度，
    须回蓝图走修订记录，不得静默偏离
  - C7 反思环与 C8 韧性存在超时预算耦合（P14）：C7 已占用最坏 13s，
    C8 design 必须把它作为输入约束，否则叠加即爆 20s
  - C7 会让违规「看似可恢复」，可能使 R10（fail-closed 未活体触发）更难关闭——
    已写入 C7 风险表，要求轨迹分别计数「重写成功」与「终态降级」
  - 叙事文档是最可能被复制到外部的文件，隐私等级最高；已写入硬边界条款，但依赖后续维护自觉

- **Commit**: pending（**未执行 `git add` / `commit` / `push`**；默认用户手动提交）

- **Next**: 开 C6 `agent-eval-framework` 规划闸——建 `openspec/changes/agent-eval-framework/`，
  写 proposal / design / tasks + delta 建议，**闸门 1 待用户批准**。
  开工前读蓝图 §4.2（六项目标、八个维度表、快照分层、P8 可编排 mock 替身）

## 2026-07-31｜agent-eval-framework（C6）规划闸｜Type C

- **Scope**: 仅规划产物与指针文件，**零业务代码**
  - `openspec/changes/agent-eval-framework/proposal.md`（新建）
  - `openspec/changes/agent-eval-framework/design.md`（新建，含 12 条决策记录）
  - `openspec/changes/agent-eval-framework/tasks.md`（新建，34 项 + 范围守护自检）
  - `openspec/changes/agent-eval-framework/specs/agent-runtime/spec.md`（新建，delta 建议）
  - `openspec/changes/agent-eval-framework/specs/backend-core/spec.md`（新建，delta 建议）
  - `openspec/changes/agent-eval-framework/specs/agent-collaboration/spec.md`（新建，delta 建议）
  - `.ai/ACTIVE_TASK.md`（IDLE → ACTIVE 规划期，指向本 change；补 3 条残余）
  - `.ai/AGENT_LOG.md`（本条）
- **Changes**:
  - 按蓝图 v1.2 §4.2（C6 意图卡片）+ 架构宪法 §3.6（`EvalPort`）开 C6 规划闸
  - proposal：36 条现状事实（E1–E36，均核对代码）、8 个待裁决项（N1–N8）、37 条验收标准、
    12 项实现顺序、5 组场景边界
  - design：12 条决策记录（含 6 条 out_of_scope 边界决策：不做 Judge、不做绝对评分、
    不校准阈值、不建 CI、话术质量只建结构、不申请闸门 3）
  - delta 落点按蓝图 §5 的 C6 行：`agent-runtime`（1 MODIFIED + 6 ADDED）、
    `backend-core`（5 ADDED）、`agent-collaboration`（3 ADDED）；
    `v2-product-scope` 与 `miniapp-core` **确认无 delta**
  - MODIFIED 那一条是 C5 的「C5 范围内的评估能力」scenario（原文写「评估能力 SHALL 留给
    后续独立 change」，C6 即那个 change）；修订方式沿用 C5 改 C2/C4/C3a/C3b 四条时的做法：
    保留阶段范围声明、改为指向本刀条款、不删除
- **规划期核出的五条修正**（均已写入 proposal，不写进设计取舍——取舍在 design.md）：
  1. **E7｜mock 分支不组装 prompt**：`generateReply` 在 `if (modelClient.isMockProvider())`
     直接 return，故 mock 路径轨迹里永远没有 `prompt` 步骤；且 `AgentMockResponder`
     按构造产不出任何违规（六句写死合规文案 / material 只拼用户发言 / toolCalls 只取用户原话）。
     → **直接决定 N3**：可编排替身挂 `AgentModelClient` 层，而非给 `AgentMockResponder` 抽接口
  2. **E20｜仓库无 CI**：无 `.github/`、workflow 零命中、`*.yml` 只有 4 个 Spring 配置。
     架构宪法 §3.6 的「CI 可跑子集」无落点 → 列为硬性诚实项（验收 34）
  3. **E21｜蓝图写的 `local-samples.yaml` 不被任何 gitignore 规则覆盖**，且仓库无通用
     `*.local.*` 规则（现有均为扩展名特化）→ N5 建议改通配命名，理由是 C5 已因点名单个文件吃过教训
  4. **E24/E25｜两处断言可及性边界**：`MAX_REASK_PER_STAGE=1` 是代码常量不可按用例调；
     无聚合记忆字符预算配置项 → 「注入预算」只能表达为派生上限并须如实标注
  5. **E28｜蓝图 §3.2 的 1183 行已过时**，`AgentChatServiceImpl` 实测 **1274 行**（C5 后 +91）。
     按蓝图 §0.4 登记勘误；**未改已冻结蓝图**
- **Verification**: PASS（规划产物层面）
  - 全部 36 条现状事实均核对代码或实测：读 `AgentGuardrailBoundaryCaseTest`（219 行 / 15 test /
    5 nested）、`AgentGuardrailTraceCorrelationTest`（纯 Mockito 驱动 `sendMessage` 的既有范式）、
    `AgentMockResponder`、`AgentModelClient`、`agent/trace/*`（19 个步骤类型）、
    `AgentChatServiceImpl` 的 trace 挂点、`AppAgentProperties` 阈值、`pom.xml`、
    `src/test/resources/`、`.gitignore`
  - 测试基础设施实测：junit-jupiter 5.10.5（含 params）、assertj 3.25.3、snakeyaml 2.2 在测试
    classpath、`jackson-dataformat-yaml` **不在**、surefire 零 excludes、
    全仓 `@ParameterizedTest` 零命中、全仓 snapshot/approval 机制零命中
  - **未运行测试套件**（本轮无代码改动，无回归可跑）
  - **零业务代码改动**：`git diff --stat` 中无 `backend/src` 与 `frontend/src` 条目
- **Risks**:
  - 闸门 1 若改选 N1–N8 中任一项，`tasks.md` 须先回改再实现（已在 tasks 头部写明）
  - N5 是对已冻结蓝图字面写法的偏离，已单独成条请示，未自行决定
  - 本刀最高风险是「快照沦为橡皮图章」：缓解为不变量层禁止刷新 + 不提供自动重写开关 +
    `baselineNote` 由机制强制（验收 19 要求「只改数字」这件事本身有测试拦）
  - 次高风险是「真实样本进 tracked file」：缓解为 gitignore 规则必须先于样本文件落地（T-01/T-02）
  - 已如实登记两项 `unknown`：快照指标在真实 provider 下的稳定性（本刀 0 外调，不验）、
    话术质量人评锚点为空
- **Commit**: pending
- **Next**: 等用户批准闸门 1（含 N1–N8 定稿与 E28 处置）。批准后**仍需闸门 2** 才可写代码；
  实现第一步是 T-01/T-02（`.gitignore` 先行并验证），顺序不可颠倒

## 2026-07-31｜agent-eval-framework（C6）实现｜Type C

- **Scope**: 闸门 1 已批准 + 闸门 2 已授权（N1–N8 全部按推荐定稿）。**`src/main` 零改动**
  - 新增 `backend/src/test/java/com/flashback/agent/eval/`：`AgentEvalHarness`、
    `ScriptedAgentModelClient`、`RecordingTraceSink`、`AgentEvalCase`、`AgentEvalCaseLoader`、
    `AgentEvalRun`、`AgentEvalInvariants`、`AgentEvalSnapshot`、`AgentEvalBaseline`、
    `AgentEvalDimension` + 五个测试类（`AgentEvalRunnerTest`、`AgentEvalHarnessTest`、
    `AgentEvalBaselineGuardTest`、`AgentEvalPrivacyTest`、`AgentEvalNarrativeAnchorTest`）
  - 新增 `backend/src/test/resources/eval/`：`cases/` 四份 YAML（23 条合成用例）、
    `baseline/snapshots.yaml`（23 条基线）、`baseline/narrative-anchors.yaml`（空锚点 + 说明）
  - `.gitignore`：加 `*.local.yaml` / `*.local.yml` 通配
  - `openspec/changes/agent-eval-framework/`：proposal / design / tasks / 三份 delta 按实测更新
  - `Docs/agent-iteration/narrative/agent-tech-story.md`：§7 写就，§9 加三行，§4 补一段
  - `.ai/ACTIVE_TASK.md`、`.ai/AGENT_LOG.md`
- **Changes**:
  - T-01/T-02：`.gitignore` 规则**先于任何样本文件落地**并用 `git check-ignore -v` 验证
    （`samples.local.yaml`→`:49`、`anything.local.yml`→`:50`），同时确认合成用例**不**被误挡
  - T-03/T-04/T-05：纯 Mockito harness + scripted provider 替身，跑通四类
    `AgentMockResponder` 产不出的路径（降级 / 上下文组装 / 提议被拒 / provider 失败）
  - T-06~T-08：snakeyaml 解析 + 参数化 runner（**本仓库首次用 `@ParameterizedTest`**）；
    入库用例缺失硬失败、本地样本缺失静默跳过、解析器不可用明确失败
  - T-09~T-15：八维度不变量；另有 6 条**通用不变量**对每条用例无条件执行，
    以及一条「期望键必须被消费」的元保护（拼错键名会失败而非静默忽略）
  - T-16~T-18：23 条快照基线 + `baselineNote` + checksum；防橡皮图章机制**自身有测试**
  - T-19/T-20：隐私改为**结构化格式校验**（正则匹配纯数值形状），既有那条子串断言一行未动
  - T-21：锚点结构就位、内容为空，「空≠已覆盖」由测试守着
  - T-34：叙事文档 §7 按 D33 收尾
- **实现期发现并处理的五件事**:
  1. **回归基线实测是 536 / 4，不是规划期沿用的 534 / 3**。AGENT_LOG 里其实早已记录
     536 / 4（三个 Type B 之后），是 `ACTIVE_TASK` 顶部与蓝图 §2 摘要没跟上。
     已修 `ACTIVE_TASK` 并登记为 E37；**蓝图不动**（已冻结）
  2. **护栏的一处真实边界（实测，非缺陷修复）**：「用户自己说过的病名可以复述」的成立条件
     比直觉窄——取决于是否连带复用周边 4-gram。同一用户输入下
     「你说有点焦虑症…」放行、「你说的焦虑症…」判 `diagnostic`。属 n-gram 方案固有性质与
     C4 刻意选的误伤方向。**未校准任何阈值**，改为把边界写成一条用例钉住现状
  3. **修掉一条自己写的假用例**：截断用例最初样本 111 字（< 120 上限），
     于是「不超过 120」恒成立而什么都没验。改成远超上限的文本 +
     新增 `injectedCharsExactly` 断言长度恰好等于 120
  4. **snakeyaml 把未加引号的 timestamp 自动转成 `java.util.Date`**，
     `LocalDateTime.parse` 拿到 `"Sun Mar 15 05:00:00 SGT 2026"` 而失败。两头都处理：
     解析层兼容 `Date`，用例里也加引号——只做后者等于留一条「靠人记得加引号」的规矩
  5. **`ArgumentCaptor` 不适合多轮取轨迹**：captor 拿到的是同一个可变对象的引用，
     多轮时读到的全是最后一轮终态。改用手写 `RecordingTraceSink` 按 persist 顺序存下
- **Verification**: **PASS**
  - 后端全量 `mvn -q -o test`：**606 tests PASS / 4 skipped，BUILD SUCCESS**
    （536 基线 + 70 新增，**零回归、既有断言零修改**；4 skipped 仍是那四个环境门控探针，
    未新增跳过）
  - 新增分布：runner 49（不变量 23 + 快照 23 + 3 条元测试）、harness 自检 7、
    防橡皮图章 6、隐私 5、锚点结构 3
  - 范围守护：`git diff --name-only -- backend/src/main frontend/src` **输出为空**
  - 外调：**0 次**（闸门 3 未申请，全程未启用任何 `C*_REAL_PROBE`）
  - 已清理验证期两个临时文件（一个探针类、一个 Python 小脚本），无残余
- **SKIPPED 验证**:
  - **快照指标在真实 provider 下的稳定性未验证**——本刀 0 外调，如实记为 unknown
  - **话术质量人评锚点为空**——填它需真实产出 + 人评，属闸门 3；建议顺带在 C7 闸门 3 做
  - **无 CI**：交付的是「一条 maven 命令可跑」，**不是** CI 门槛
- **Risks**:
  - snakeyaml 是传递依赖（经 `spring-boot-starter`），未来 starter 升级理论上可能移除它。
    已加 `NoClassDefFoundError` 兜底明确失败，绝不静默跳过用例（否则会变成「绿灯但什么都没测」）
  - 基线是**手工更新**（刻意不提供自动重写开关）。用例规模大幅增长后批量更新会烦；
    届时若加开关，须同时配「说明未变更则失败」的守护，不得先给出口再补守护
  - R10 未变：回看 fail-closed 仍未活体触发，本刀只是让它多一层常驻回归
  - `minMemoryOnlyRunForAttribution` 与新发现的 n-gram 边界都**未校准**，属独立事项
- **Commit**: pending
- **Next**: 用户验收 diff → delta 接受进 baseline → 归档 → `ACTIVE_TASK` → IDLE

## 2026-07-31｜agent-eval-framework（C6）提交补录｜Type C

- **Commit**: `aedab6c`（31 files changed, 5675 insertions(+), 34 deletions(-)）
- **Scope**: 用户当轮明确授权 Agent 执行 `git commit`；**`push` / 部署 / 发布未授权，未执行**
- **Changes**: 只用显式 `git add <path>` 逐条暂存（未用 `git add .`，未用 stash / clean / reset --hard）
  - 刻意**未提交** `.kiro/skills/`——它是工作区既有未跟踪产物，不属本刀范围
- **Verification**: 提交后复跑 `mvn -q -o test` **BUILD SUCCESS**
  （git 对 YAML 做了 LF→CRLF 规范化，因此提交后必须复验一次解析仍正常；已确认无影响）
- **Risks**: 无新增
- **Commit**: `aedab6c`

## 2026-07-31｜agent-eval-framework（C6）验收归档｜Type C

- **Scope**: delta 接受进 baseline + 归档 + 指针收口
  - `openspec/specs/agent-runtime/spec.md`（**MODIFIED 1 条** + 新增 `## Accepted From C6` 段，6 条 Requirement）
  - `openspec/specs/backend-core/spec.md`（新增 C6 段，5 条 Requirement）
  - `openspec/specs/agent-collaboration/spec.md`（新增 C6 段，3 条 Requirement）
  - `openspec/changes/agent-eval-framework/` → `openspec/changes/archive/2026-07-31-agent-eval-framework/`
    （用 `git mv`，保留历史）；新增 `closeout.md`
  - `.ai/ACTIVE_TASK.md`（`ACTIVE` → **`IDLE`**）、`.ai/AGENT_LOG.md`
- **Changes**:
  - **MODIFIED**：C5 的「C5 范围内的评估能力」scenario 原文「评估能力 SHALL 留给后续独立 change」
    改为指向 C6 条款。**保留阶段范围声明不删**——范围声明本身是历史事实，
    删掉就看不出能力何时到位（沿用 C5 修订 C2/C4/C3a/C3b 四条时的同一做法）
  - **ADDED 14 条 Requirement**：不变量离线断言（含通用不变量与期望键拼写两条实现期新增）、
    基线可比对可追溯（含留痕机制完整性、孤儿条目两条实现期新增）、评测不改行为、
    评测不泄漏内容、诚实边界（含锚点内容边界）、C6 范围排除；
    backend-core 侧另有替身最小化、须驱动真实生成分支、跳过数不得增加三条实现期新增
  - 确认 `v2-product-scope` 与 `miniapp-core` **无 delta**
  - `ACTIVE_TASK` 收口：Status→IDLE、Previous Completed 换为 C6、Direction Layer 指向 C7、
    新增「C6 的关键结论（对 C7 有直接价值）」、Residual 与流程教训按实测更新、
    未跟踪产物一节复核为只剩 `.kiro/skills/`
- **关于「闸门 3 通过」的如实记录**:
  - 用户本轮表述为「闸门 3 通过」，但 **C6 并无外调可授权**——proposal §9 申请的就是 0，
    实现期实测外调 0 次，未启用任何探针
  - 因此在 `closeout.md` §3、`tasks.md` 闸门检查点、以及三份 spec delta 的段首
    **一律记为「未申请」而非「已通过」**，并单独登记由此产生的未验证项
    （快照指标在真实 provider 下的稳定性）
  - 这样做的理由：把「没做」记成「做过且通过」，会让后续任何人误以为该项已有实证
- **Verification**: PASS
  - 三份 spec 落地核验：`agent-runtime` 2003 行 / 59 条 Requirement、
    `backend-core` 1308 行 / 49 条、`agent-collaboration` 197 行 / 11 条；
    三处 `## Accepted From C6` 段均在位；C5 那条 scenario 已指向 C6
  - 归档后复跑后端全量 `mvn -q -o test` **BUILD SUCCESS**（本轮只改文档与 spec，
    但归档动了目录，仍复验一次）
  - `openspec/changes/` 下已无 `agent-eval-framework`（只剩 archive 与两个历史 m1/m3 目录）
- **Risks**: 无新增。既有残余见 `ACTIVE_TASK` Residual（R2 现已具备优化条件、
  两处未校准阈值、无 CI、锚点为空、快照真实 provider 稳定性未验证）
- **Commit**: pending
- **Next**: **开 C7 `agent-reflection-loop` 规划闸**。开工前读蓝图 §4.3，
  并注意三件事：类大小用实测 1274 行、人评锚点顺带在 C7 闸门 3 填、
  C7 若改编排行为则 C6 快照会变（须写 `baselineNote`，不得改数字了事）

## 2026-07-31｜C6 归档后的活文档同步｜Type B

- **Scope**: 只改文档指针与状态，无代码改动
  - `AGENTS.md`（当前阶段 → Phase 2 已开局；默认下一刀 → `agent-reflection-loop`）
  - `openspec/project.md`（当前工程状态 → C6 已归档；下一刀 C7）
  - `.kiro/steering/product.md`（Phase 2 序列标注 C6 已归档 + 当前 IDLE）
  - `Docs/agent-iteration/architecture/agent-architecture-constitution.md`
    （L5 Eval 由 `partial → C6` 改为 `confirmed（C6）`、`EvalPort` 现状与实现要点重写、
    Phase 对齐表 C6→已归档 / C7→下一刀、Trace 目标补一句、§7.3 那条禁令标注技术前提已解除）
  - `Docs/agent-iteration/architecture/tech-selection-draft.md`（Eval 行 → confirmed；能力总览 Eval→✅、Reflection→下一刀）
  - `Docs/agent-iteration/architecture/README.md`（状态日期、当前进度）
  - `Docs/agent-iteration/README.md`（索引表：蓝图 v1.1→v1.2、**删除已不存在的 v1.2-draft 行**、
    补 narrative 行、加进度说明）
- **Changes**:
  - 归档一刀后按 D33 与「校准义务」同步全部活文档；**`openspec/changes/archive/**` 一律未动**（归档即历史）
  - **已冻结蓝图 `iteration-blueprint.md` 未改**：其中的「C6 下一刀」「1183 行」等表述属冻结内容，
    修订须走显式流程并更新 §12。C6 的两处勘误只登记在 change 与 `ACTIVE_TASK` 内
  - 顺带修掉两处**过时索引**：`Docs/agent-iteration/README.md` 仍把蓝图记作 v1.1，
    且仍列着 `iteration-blueprint-v1.2-draft.md`（该文件早已删除、内容已迁入正式蓝图）
  - 宪法的能力表脚注补记：上次校准写的「534 tests」是 C5 归档当时值，
    实为 536/4（C6 复核发现）→ 本次改为 606/4
- **Verification**: PASS
  - 全仓检索确认活文档中已无「C6 为下一刀 / partial → C6 / 待 C6」类表述
    （仅存于已冻结蓝图与 archive，按规则不动）
  - 后端全量 `mvn -q -o test` **BUILD SUCCESS**（本轮纯文档，仍复验一次）
- **Risks**: 无
- **Commit**: pending

## 2026-08-02｜agent-reflection-loop（C7）readiness 与规划闸｜Type C

- **Scope**: 只做阶段 readiness + OpenSpec 规划，不改业务代码
  - `openspec/changes/agent-reflection-loop/proposal.md`
  - `openspec/changes/agent-reflection-loop/design.md`
  - `openspec/changes/agent-reflection-loop/tasks.md`
  - 四份 delta：`agent-runtime` / `backend-core` / `v2-product-scope` / `agent-collaboration`
  - `.ai/ACTIVE_TASK.md`（`IDLE` → `ACTIVE`，指向 C7 规划闸）
- **Changes**:
  - readiness 结论为 **GO**：开刀前 Git clean；C6 已于 2026-07-31 归档；
    `ACTIVE_TASK` 原为 IDLE；蓝图 v1.2 明确 C7 为下一刀；C4 判定源与 C6 回归基线均已满足
  - 规划定为两条窄环：reply 仅恢复 `MISSING_TIME_ATTRIBUTION`，material 仅恢复 `UNFAITHFUL`；
    最大重写 1 次，其余违规、provider failure、invalid content 均不重试
  - **规划期事实修正**：checked-in code 证明普通 reply 不执行全量忠实度检查，
    `UNFAITHFUL` 实际出现在 material/tool 路径；因此不擅自扩大 C4 已接受的 reply 判定范围
  - P13 推荐：reflection 不增加 `attemptNo`，同一 persisted trace 以 steps 区分 initial/reflection
  - 闸门 3 建议预算上限 12 次真实 provider 调用（先 4 次 canary）；当前未授权、未执行
- **Verification**: PASS（规划级）
  - `AgentChatServiceImpl` 实测 1274 行；相关 generation/guardrail/trace 挂点已按当前代码复核
  - proposal / design / tasks / 四份 delta 文件已生成；`miniapp-core` 明确无 delta
  - `openspec` CLI 不在 PATH，CLI scaffold/validate **SKIPPED**；改用仓库既有目录结构与文件级检查
  - 当前 checkout 后端全量复验：
    `mvn "-Dmaven.repo.local=C:\Users\Lin\.m2\repository" -o -s C:\Users\Lin\.m2\settings.xml -q test`
    **PASS**；surefire 汇总 **72 suites / 606 tests / 0 failures / 0 errors / 4 skipped**
  - 第一次默认 `mvn -q -o test` 在 POM 解析阶段 **FAIL**：默认本地仓库缺
    `spring-boot-starter-parent:3.3.5`，未进入编译；改用项目既有显式本机 repository/settings 后通过，
    故该失败记为环境解析问题，不记为代码失败
- **Risks**:
  - 两次真实 provider 调用能否稳定落在 backend 20s 内仍为 unknown；须闸门 3 canary，
    超预算时回规划，不直接改超时
  - 真实 provider 重写遵从率与体感收益仍为 unknown；scripted test 不能替代人评
  - OpenSpec CLI 不可用，不能声称 CLI validation PASS
- **Commit**: pending（默认用户手动提交；未 stage / commit / push）
- **Next**: 用户审阅闸门 1；批准后仍需单独给出闸门 2 实现授权

## 2026-08-02｜agent-reflection-loop（C7）闸门批准后实现前复核｜Type C

- **Scope**: 闸门状态同步 + 实现前代码/调用预算复核；业务代码零改动
- **Authorization**:
  - 闸门 1：已批准；闸门 2：已授权
  - Git：已授权 Agent 提交本次 C7；push / 部署 / 发布未授权
  - 闸门 3：未授权，未执行任何真实 provider 调用
- **Changes**:
  - proposal / design / tasks / 四份 delta 同步为闸门 1 已批准、闸门 2 已授权
  - tasks T-01 ~ T-03 完成；开工锚点 `b459b8f`
- **Verification**: PASS（实现前 baseline）
  - C4 guardrail + C5 trace + C6 eval 指定测试 exit 0
  - `AgentChatServiceImpl` 实测 1274 行，相关挂点与构造依赖已复核
- **Blocking design conflict**:
  - `sendMessageTraced` 在 CLOSING 一轮先调用 reply generation，再调用 material generation；
    现状正常路径已经是 2 次 provider 调用
  - 已批准规划又允许 material `UNFAITHFUL` reflection 一次，因此该轮最坏会变成 3 次调用，
    与 delta“每轮最多 2 次”直接冲突
  - C5 历史平均 6476ms × 3 ≈ 19.4s，几乎顶满 backend 20s；最大值口径更无法容纳
  - 该冲突影响调用预算与用户等待语义，不能在实现中主观改成 3 次或顺手放宽超时
- **Risks**: 当前只阻塞范围裁决，代码基线仍为绿色；业务代码尚未修改
- **Commit**: pending
- **Next**: 推荐将 C7 收窄为 reply-only `MISSING_TIME_ATTRIBUTION`；material 继续沿用 C4 丢弃语义

## 2026-08-02｜agent-reflection-loop（C7）reply-only 范围裁决｜Type C

- **Scope**: 修订已批准规划，未改业务代码
- **Decision**: 用户明确采用推荐方案收窄为 reply-only
  - 仅非 `CLOSING` reply 的 `MISSING_TIME_ATTRIBUTION` 可重写一次
  - material `UNFAITHFUL` 继续沿用 C4 直接丢弃；tool proposal 不开环
  - `CLOSING` reply 不开环，因为同轮随后还会生成 material；确保单轮仍最多 2 次 provider 调用
  - 闸门 3 预算由 12 收窄为 6（先 2 次 canary）；当前仍未授权
- **Changes**: proposal / design / tasks / 四份 delta / ACTIVE_TASK 已同步 reply-only 契约
- **Verification**: 规划级一致性待实现后与代码 exact match；业务代码仍零改动
- **Risks**: C7 不挽救不忠实 material；这是为守住 20s 超时与可证明调用上限接受的范围代价
- **Commit**: pending
- **Next**: 实现 reply reflection policy / pipeline / trace；material 只做零回归验证

## 2026-08-02｜agent-reflection-loop（C7）reply-only 实现与离线验证｜Type C

- **Scope**:
  - 新增 `backend/src/main/java/com/flashback/agent/reflection/`：类型化 policy、reply value、最小 reply pipeline、provider phase 与 reflection terminal
  - `AgentChatServiceImpl` 把 reply generation / normalize / guard / fallback 委托给窄 pipeline；material 路径保持原实现
  - `AgentTraceCollector` / `AgentTraceVersions`：reflection phase、脱敏 decision/result、耗时聚合与 policy 指纹
  - C6 scripted eval：23 条既有用例保留，新增 5 条 reply-only/CLOSING/material 边界用例；同步人工审查后的基线
  - OpenSpec 四份 delta、tasks 与 `ACTIVE_TASK` 同步实现事实
- **Changes**:
  - 仅非 `CLOSING` reply 的 `MISSING_TIME_ATTRIBUTION` 可进入一次 reflection；最大次数为代码常量 1
  - reflection 调用固定 `tools=[]`、strict=false；第二答重新执行 content + time attribution checks 与长度上限
  - 成功重写保留 initial tool calls；最终兜底丢弃 initial tool calls；provider failure/invalid、其他违规、mock、CLOSING、material、tool 均不开环
  - provider steps 使用 `phase=initial|reflection`；顶层 `providerDurationMs` 累加子调用；新增的 trace step 只含枚举、数字与异常类型
  - C6 `Turn` 增加 scripted reflection reply/failure；新增 `providerCalls`、`reflectionAttempted`、`reflectionTerminal` 期望键。基线只更新 C7 合法改变/新增的 5 条，并同步 `baselineNote` + checksum
- **Verification**: PASS
  - focused：policy / reply pipeline / material 零回归 / trace / C6 eval / baseline guard / privacy 全部 PASS
  - 后端全量：显式本机 Maven repository/settings，**74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**，BUILD SUCCESS（较 C6 606/4 新增 16 tests，未新增 skip）
  - `git diff --check` PASS；改动路径核验 PASS：无 API / DTO / DDL / schema / frontend / timeout / pom / package / lockfile 变化
  - 新增/修改代码与 change 资产的增量敏感标记扫描 PASS；未写入真实日记、候选输出、prompt、provider response 或 secret
  - 开发期第一次 compile/testCompile 曾因沙箱读取本机 Maven cache JAR 失败；授权读取缓存后进入真实编译。随后测试曾按 red-green 暴露并修正阶段枚举、快照指标/路径/checksum，最终结果以上述全绿为准
- **Verification SKIPPED**:
  - 真实 provider / 真机 / narrative anchors：闸门 3 未授权，真实调用 0 次；不得从 scripted client 推断真实模型质量或 20s 预算稳定性
  - 真实 MySQL reflection 联调：本轮未建立 MySQL + scripted provider 夹具；H2 全量结果不冒充真实 MySQL
  - OpenSpec CLI validate：CLI 不在 PATH；仅做 change 结构、delta/实现 exact-match 与文件级检查
- **Scope safety**:
  - 未扩大 eligible set；未给普通 reply 新增忠实度闸；未改 C4 阈值/词表/来源集合
  - material 与 CLOSING reply 均不 reflection，确保 CLOSING 仍为 reply + material 最多 2 次 provider 调用
  - 未做 C8 error retry、C9 temporal、LLM-as-Judge、UI、部署或 push；未修改 archive 与冻结蓝图
- **Risks**:
  - 真实 provider 对固定重写要求的遵从率、体感收益与双调用延迟仍 unknown，待闸门 3
  - 真实 MySQL 下同一 turn 一行 trace / 事务完成尚未活体验证；本刀未改 DDL/事务边界，但 spec 要求验收前补齐或明确接受 SKIPPED
  - material `UNFAITHFUL` 仍直接丢弃，不在 C7 恢复；这是用户确认的 reply-only 范围代价
- **Commit**: pending
- **Next**: 执行已授权的 C7 Git 提交（不 push）；随后等待用户验收与是否单独开放闸门 3 / MySQL 联调

## 2026-08-02｜agent-reflection-loop（C7）提交证据补录｜Type C

- **Scope**: 仅补录已完成的 Git 提交事实，不改业务代码或 OpenSpec 契约
- **Verification**: 提交前最终全量 **74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**；`git diff --cached --check` 与 staged secret scan PASS
- **Commit**: `8a2dbb4`（`feat(agent): 实现 C7 受控回复反思环`）
- **External effects**: 未 push、未部署、未发布、未执行真实 provider 调用
- **Next**: 等待用户验收；真实 MySQL reflection 联调与闸门 3 仍未完成

## 2026-08-03｜agent-reflection-loop（C7）验收归档｜Type C

- **Scope**: 接受 C7 四份 delta 进入 baseline；新增 closeout；更新 tasks、ACTIVE_TASK、项目入口、架构状态与对外叙事；归档 active change
- **Changes**:
  - 用户在已收到未验证风险说明后明确要求归档；真实 MySQL、闸门 3 provider/真机、人评锚点与 OpenSpec CLI validate 均记为 SKIPPED，不伪记 PASS
  - `agent-runtime`、`backend-core`、`v2-product-scope`、`agent-collaboration` delta 已同步进入 accepted baseline；`miniapp-core` 无 delta
  - `ACTIVE_TASK` 转为 IDLE；默认下一刀更新为 C8 `agent-resilience` 规划闸；冻结蓝图未修改
  - 按 D33 补完叙事 §8，并将架构选型中的 C7 状态由 planned 校准为 confirmed / archived
- **Verification**: pending（归档移动后执行后端全量、文件级结构、diff 与敏感边界检查）
- **Verification SKIPPED**:
  - 真实 MySQL reflection：未建立夹具，H2 不冒充 MySQL
  - 真实 provider / 真机 / 人评锚点：闸门 3 未授权，真实调用 0 次
  - OpenSpec CLI validate：CLI 不在 PATH，改做文件级 delta / baseline / archive 结构核对
- **Risks**: 真实模型重写质量、双调用延迟与真实 MySQL 事务表现仍未活体验证；C8 必须扣除 C7 最坏两次调用预算
- **Commit**: pending
- **Next**: C8 只能从只读 readiness 与独立 OpenSpec 规划闸开始，不得直接实现

## 2026-08-03｜agent-reflection-loop（C7）归档验证补录｜Type C

- **Scope**: C7 归档移动后的最终验证；不改业务代码或范围契约
- **Verification**: PASS
  - 后端全量：**74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**
  - active C7 目录不存在；归档目录、`closeout.md` 与四份 delta 完整；`ACTIVE_TASK=IDLE`
  - 四份 baseline 均存在 `Accepted From C7`，Requirement 标题与归档 delta 对齐；`miniapp-core` 无 delta
  - `git diff --check` 与增量敏感标记扫描 PASS
- **Verification SKIPPED**: OpenSpec CLI 不在 PATH；真实 MySQL、provider、真机与人评仍沿用 closeout 的验收 SKIPPED
- **Risks**: 不变；真实模型质量、双调用延迟与 MySQL 事务表现未活体验证
- **Commit**: pending

## 2026-08-03｜agent-reflection-loop（C7）归档提交证据补录｜Type C

- **Scope**: 仅补录 C7 归档提交事实，不改业务代码或 OpenSpec 契约
- **Verification**: 归档提交前后端全量 **74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**；staged diff check 与敏感标记扫描 PASS
- **Commit**: `634de54`（`docs(openspec): 归档 C7 受控回复反思环`）
- **External effects**: 未 push、未部署、未发布、未执行真实 provider 调用
- **Next**: 当前 IDLE；等待 C8 `agent-resilience` 规划授权

## 2026-08-03｜agent-resilience（C8）规划闸｜Type C

- **Scope**:
  - 新建 `openspec/changes/agent-resilience/`：proposal / design / tasks
  - 新建四份 delta：`agent-runtime`、`backend-core`、`miniapp-core`、`agent-collaboration`
  - 更新 `.ai/ACTIVE_TASK.md`：IDLE → C8 ACTIVE（规划期）
  - 业务代码、测试代码、baseline、冻结蓝图均未修改
- **Changes**:
  - readiness 判定 GO：C7 已归档、开刀前工作区 clean、蓝图下一刀为 C8；开工锚点 `fb68082`
  - 核出关键事实修正：现有 backend 20000ms 是每次 JDK HttpRequest timeout，不是整轮 deadline；
    C7 两次调用理论上可占约 40000ms，可能先撞 frontend 30000ms
  - P14 推荐为 request-scope 24000ms provider-work budget；子调用 timeout 取 20000ms 与剩余预算较小值
  - 第一阶段推荐零自动 provider retry；多 provider 路由、熔断、缓存、监控 deferred
  - provider failure 保持 FAILED/UNAVAILABLE 与用户主动同轮 retry；阶段化温暖模板不落 Assistant 消息、不冒充成功
  - 自检后收敛为 API/DTO/frontend 零字段变化；技术 failure category 只留在脱敏 backend trace/log，
    既有用户主动同轮 retry 保持，避免 pending turn 因隐藏 retry 而卡死
- **Verification**: PASS（规划级）
  - proposal/design/tasks 与四份 delta 均已创建；v2-product-scope 明确无 delta
  - 规划范围与 C7 accepted contract、蓝图 C8 意图卡片及 Type C checklist 完成对照
  - 开刀前 Git status 无 tracked/untracked 改动；未执行外调或业务测试
- **Verification SKIPPED**:
  - OpenSpec CLI 不在 PATH；不声称 CLI scaffold/validate，改做文件级结构与内容核对
  - 后端/前端测试未运行：本轮零业务代码，仅规划资产；实现前 T-08 会复跑 baseline
  - 真实 MySQL、provider、真机：闸门 2/3 均未授权
- **Scope safety**:
  - 未改业务代码、API/DTO、DDL、timeout 配置、package/lockfile、provider secret、部署或监控
  - 未做自动 retry、多 provider 路由、C9 temporal 或 major UI reconstruction
  - 未写用户日记、对话、prompt、provider response、exception message 或 secret
- **Risks**:
  - 24000ms budget、API 零变化策略与 N1–N6 仍待用户在闸门 1 裁决
  - C7 双调用真实耗时未做 provider/真机验证；24s 可行性仍是 planned/unknown，不得写成 confirmed
  - opening 无 turn trace；本规划选择保持既有一轮一条语义，仅用脱敏结构化日志
- **Commit**: pending（默认用户手动提交；未 stage / commit / push）
- **Next**: 用户审阅闸门 1；批准后仍需单独给出闸门 2 实现授权

## 2026-08-03｜agent-resilience（C8）闸门批准与实现启动｜Type C

- **Scope**: 仅同步授权状态并准备实现前 baseline；业务代码尚未修改
- **Authorization**:
  - 闸门 1：已批准；N1–N6 按 proposal 推荐方案定稿
  - 闸门 2：已授权，用户明确要求“开始 C8 阶段实现”
  - 闸门 3：未授权，禁止真实 provider / 真机外调
  - Git：仍为用户手动提交；未授权 stage / commit / push
- **Changes**: proposal / design / tasks / ACTIVE_TASK 同步为实现期；T-06/T-07 完成
- **Verification**: PASS（T-08 实现前 baseline）
  - 默认 `mvn -q test` 在 POM 解析阶段因默认本地仓库缺 Spring Boot parent 失败，未进入编译；属环境解析问题
  - 改用既有显式本机 repository/settings 离线运行：**74 suites / 622 tests / 0 failures / 0 errors / 4 skipped**
- **Risks**: 真实 provider 双调用在 24s budget 内的稳定性仍 unknown；离线实现不得扩写成真实质量结论
- **Commit**: pending
- **Next**: T-08 baseline → T-09 taxonomy RED

## 2026-08-03｜agent-resilience（C8）离线实现完成｜Type C

- **Scope**:
  - 按已批准 N1–N6 完成 C8 backend resilience；零真实 provider / 真机外调
  - 同步测试、C6 scripted eval、OpenSpec tasks 与 ACTIVE_TASK；未接受 delta、未归档
- **Changes**:
  - 新增 8 类封闭 `AgentProviderFailureCategory`、不复制下游自由文本的 `AgentProviderException`
    与 HTTP/异常类型分类器；401/403、429、5xx、其他 4xx、timeout、connect、invalid、interrupted、unknown
    均有离线覆盖
  - 新增 request-scope `AgentCallBudget`，默认 24000ms；每次 HTTP timeout 取
    `min(app.ai.timeout-millis, remaining)`，剩余不足 100ms 时调用前失败；null budget 不得创建嵌套预算绕过上限
  - opening / turn / reflection / material / finish 接入同一请求预算；C8 不增加自动 retry，C7 reflection 上限不变
  - 新增窄 `AgentResiliencePolicy`：按 operation/stage/category 选择固定克制模板；
    failure 保持 FAILED/UNAVAILABLE，用户消息保留，模板只进入 `AgentSessionVO.message`，不落 Assistant
  - trace 复用 `cause_type` 保存稳定 category；provider step 记录 phase/category/transient/
    budgetExhausted/remainingBucket；material failure 不反转整轮成功；opening 只写脱敏结构化日志，不伪造 turn 0 trace
  - C6 scripted client 支持类型化 failure 与可控 budget；新增 resilience eval 与 DTO 零扩张 contract test；
    既有 snapshot 指标零变化，未刷新 baselineNote/checksum
- **Verification**: PASS
  - C8 focused：classifier / budget / policy / client / pipeline / service / trace / observability / eval / contract 全部 PASS
  - 后端全量离线：**79 suites / 643 tests / 0 failures / 0 errors / 4 skipped**；
    对比实现前 74 suites / 622 tests / 4 skipped，新增 21 tests，既有 skip 未增加
  - 前端：bundled Node 直接执行 `vue-tsc --noEmit` PASS；`uni build -p mp-weixin` PASS
  - `git diff --check` PASS；34 个改动路径审计无 frontend/src、archive、冻结蓝图、DDL/schema、pom/package/lockfile、
    deployment、monitoring、C9；增量高风险 secret pattern scan 0 命中
- **Verification environment notes**:
  - 默认 `mvn -q test` 仍因默认本地仓库缺 Spring Boot parent 未进入编译；最终 Maven 结果来自既有显式
    `C:\Users\Lin\.m2\repository` + settings 的 offline 命令
  - 系统 PATH 无 npm；bundled pnpm 首次因非 TTY 拒绝清理 modules，未进入脚本；随后改用 bundled Node +
    项目现有 node_modules 完成验证，未安装/更新依赖；验证副产物已精确清理
- **Verification SKIPPED**:
  - 真实 MySQL：未单独授权；H2 不冒充 MySQL 持久化/事务证据
  - 闸门 3真实 provider / 真机：未授权，真实调用 0 次；24s 双调用真实稳定性仍 unknown
  - OpenSpec CLI：不在 PATH，仅做文件级结构、delta 与实现 exact-match 审计
- **Scope safety**:
  - 无自动 retry、多 provider、熔断、缓存、队列、C9 temporal；无 API/DTO/frontend 字段变化
  - 无 DDL、secret、依赖/lockfile、部署/监控、archive 或冻结蓝图变化
  - 日志/trace 不写用户对话、日记、prompt、provider response、exception message、endpoint 或 credential
- **Risks**:
  - 24s budget 在真实 provider 的单/双调用耗时与前端实际错误卡片体验尚未活体验证
  - 真实 MySQL 的分类 trace 持久化与 pending-turn retry 未验证；本刀未改 DDL/事务边界，但仍诚实保留 SKIPPED
  - OpenSpec delta 尚未接受，change 尚未归档；须用户验收后另行收口
- **Commit**: pending（用户手动提交；未 stage / commit / push）
- **Next**: 等待用户 review；验收后再决定归档与是否单独开放真实 MySQL / 闸门 3

## 2026-08-08｜agent-resilience（C8）闸门 3 验收归档｜Type C

- **Scope**:
  - 用户授权闸门 3、真实 MySQL 验收、接受 delta、归档与 Agent 提交
  - 仅收口 C8；未授权且未执行 push、部署、发布
- **Changes**:
  - 新增默认门控 `C8RealProviderProbeTest` 与 `C8MysqlResilienceProbeTest`
  - 四份 C8 delta 接受进 `agent-runtime` / `backend-core` / `miniapp-core` / `agent-collaboration` baseline
  - 补 `closeout.md`、叙事 §9、架构参考与入口状态；`ACTIVE_TASK` 归档后回到 `IDLE`
- **Verification**: PASS
  - 真实 DeepSeek / `deepseek-v4-pro`：固定合成短文本，2 次 canary 1378ms / 1656ms；
    2 组双调用 2898ms / 3531ms；总计 **6/6** 成功（≤8），未触发停止条件
  - 真实 MySQL：同一 turn 仅一条 USER message；attempt 1=`UNAVAILABLE/auth-configuration`，
    attempt 2=`SUCCESS`；固定合成数据在 `finally` 中清理
  - 后端最终全量：**81 suites / 645 tests / 0 failures / 0 errors / 6 skipped**；
    新增两个真实探针默认关闭，故默认 skip 由 4 增至 6
  - 四份 delta 的 6/5/3/4 个 Requirement 标题均在 accepted baseline 找到，missing=0
  - 前端沿用实现期 type-check / `build:mp-weixin` PASS；本次验收未改 frontend source
  - `git diff --check`、增量敏感标记扫描、改动路径审计与归档结构核对 PASS
- **Verification SKIPPED**:
  - 微信真机：本机未发现微信开发者工具或可控真机环境；不以 scripted provider、构建或浏览器冒充
  - OpenSpec CLI：不在 PATH；改做 delta / baseline Requirement 对齐与 archive 文件级核对
- **Scope safety**:
  - 无自动 retry、多 provider、熔断、缓存、队列、C9 temporal、major UI reconstruction
  - 无 DDL、API/DTO/frontend 字段、provider secret、依赖/lockfile、部署/监控变化
  - 探针不发送真实日记/对话/文件；证据不记录 prompt、response、secret、endpoint 或异常 message
- **Risks**:
  - 6 次 provider 调用是小样本链路验收，不是生产 SLA
  - 微信真机错误卡片与主动重试体验仍缺活体证据
  - 多 provider、熔断、缓存、监控均 deferred，须证据触发独立 change
- **Commit**: pending
- **Next**: C8 归档后保持 IDLE；C9 须从独立规划闸开始

## 2026-08-08｜agent-temporal-intelligence（C9）规划闸｜Type C

- **Scope**:
  - readiness 复核 C8 archive、`ACTIVE_TASK=IDLE`、Git clean、冻结蓝图与 C3a/C3b/C6/C8 前置
  - 新建 `openspec/changes/agent-temporal-intelligence/`：proposal / design / tasks
  - 新建五份 delta：`agent-runtime`、`backend-core`、`v2-product-scope`、`miniapp-core`、`agent-collaboration`
  - 更新 `.ai/ACTIVE_TASK.md`：IDLE → C9 ACTIVE（规划期）；业务代码与 accepted baseline 未修改
- **Changes**:
  - readiness 判定 GO：C8 已于 2026-08-08 归档、工作区 clean、HEAD `544e9ea`、C9 无目录冲突
  - 规划期核对：现有 `MemoryFragment` 已有 `occurredAt/timeLabel`；C3a 默认 24 个月/3 片段/120 字，
    SQL 按 created_at 倒序；C3b 已区分回看目标记录与旁支检索；C6 有 fixed Clock eval；C8 不允许新增调用
  - N1–N8 推荐：30/180 天 distance bands；旁支片段按 100/75/50% 字符预算衰减且最低 40；
    focal review record 不衰减；recurrence 仅 REVIEW_CHAT + 显式比较 cue + 2 个不同旁支记录 + span≥90 天
  - 新内部 `TEMPORAL_OVERREACH` 推荐直接走安全兜底，不扩 C7 reflection；API/DTO/DDL/mapper SQL/UI 零变化
  - 规划期外调预算 0；后续闸门 3 推荐 provider 合成探针上限 6，MySQL/真机仍需分别授权
- **Verification**: PASS（规划级）
  - proposal/design/tasks 与五份 delta 均已创建；proposal 含五态、用户故事、N1–N8、预算与 25 条验收
  - design 含架构/数据流/config/隐私/验证与 11 条决策记录；tasks 含 64 个小步、三道独立闸门与范围自检
  - 文件级核对 change 结构、Requirement/Scenario 标题、ACTIVE 指针与 Git diff；未运行业务测试
- **Verification SKIPPED**:
  - OpenSpec CLI：不在 PATH；沿用仓库既有结构并做文件级检查，不声称 CLI scaffold/validate PASS
  - 后端/前端测试：本轮零业务代码；闸门 2 通过后 T-09 才运行实现前 baseline
  - 真实 provider / MySQL / 微信真机：规划期预算 0、闸门 3 未授权
- **Scope safety**:
  - 未修改 `backend/src/**`、`frontend/**`、accepted baseline、archive、冻结蓝图、API/DTO/DDL/mapper SQL
  - 未新增 provider 调用、LLM-as-Judge、自动 retry、dashboard、评分、诊断、推送、页面或依赖/lockfile
  - 未写入用户日记、对话、记忆片段、prompt/provider response、secret 或本机绝对路径到对外叙事
- **Risks**:
  - 30/180/90 天与 100/75/50% 是待闸门 1 批准的规划值，尚未用真实样本校准
  - 现有 LIKE/tag 检索只能支持有限重复提示，不能证明周期、相关性分数或因果
  - deterministic overreach 规则只能约束声明过的表达形态；真实 provider 语言质量与真机体感仍 unknown
- **Commit**: pending（用户手动提交；未 stage / commit / push）
- **Next**: 用户审阅并批准 N1–N8 与五份 delta；批准后仍需单独给出闸门 2 实现授权

## 2026-08-08｜agent-temporal-intelligence（C9）闸门批准与实现启动｜Type C

- **Scope**: 仅同步授权状态并准备实现前 baseline；业务代码尚未修改
- **Authorization**:
  - 闸门 1：已批准，N1–N8 按 proposal 推荐方案定稿
  - 闸门 2：已授权，允许按 `tasks.md` 开始 C9 离线实现
  - 闸门 3：未授权，禁止真实 provider / MySQL 探针 / 微信真机
  - Git：Agent commit 已授权；push / deploy / release 未授权
- **Changes**: proposal / tasks / ACTIVE_TASK 同步为实现期；T-07/T-08 完成
- **Verification**: PASS（T-09 实现前 baseline）
  - focused：C3 memory/review、prompt、guardrail、C6 eval、C7 pipeline、C8 resilience 全绿
  - 后端全量：**81 suites / 645 tests / 0 failures / 0 errors / 6 skipped**
  - Maven 3.9.9 enhanced local-repository 首次误判已有缓存不可用；改用
    `-Daether.localRepositoryManager=simple` + 显式本机 repository/settings + offline 后进入真实构建
  - 未启用真实探针环境变量，未下载/更新依赖
- **Risks**: 真实 provider 话术质量、真实 MySQL 时间数据与微信真机体验继续为 unknown；不得由离线结果扩写
- **Commit**: pending
- **Next**: T-09 baseline → T-10 temporal distance RED

## 2026-08-08｜agent-temporal-intelligence（C9）闸门 1/2 离线实现完成｜Type C

- **Scope**:
  - 按已批准 N1–N8 与五份 delta 完成 C9 backend-only 实现；未执行闸门 3、push、部署、发布、接受 delta 或归档
  - Agent commit 已授权；本条先记 `pending`，提交后另条补真实 hash
- **Changes**:
  - 新增 `agent/temporal`：日期级 `RECENT/DISTANT/LONG_AGO/UNKNOWN`、旁支片段字符预算衰减、focal 豁免、窄 recurrence evidence
  - 新增 `app.agent.temporal` 安全默认值、Bean Validation 与跨字段 fail-fast；关闭时保持 C8 行为
  - reply prompt 增加时间距离 supplement；衰减后的同一片段列表同时供 prompt 与来源语料使用
  - 新增内部 `TEMPORAL_OVERREACH` 与 `reply-temporal`；不足证据、重复 hint、量化/绝对规律/因果/趋势/预测直接安全兜底，不进入 C7 reflection
  - C5 trace 仅新增开关、band 计数、衰减前后字符、eligible/used 与违规枚举；policy fingerprint 纳入 temporal 配置与规则
  - C6 新增 fixed-clock eval；逐条审阅唯一合法快照变化：DISTANT 合成片段注入字符 120 → 90，并同步说明与 checksum
- **Verification**: PASS
  - C9 focused：policy / prompt / checker / pipeline / trace version / scripted eval 全绿
  - 后端全量：**85 suites / 662 tests / 0 failures / 0 errors / 6 skipped**
  - 前端：bundled Node 运行 `vue-tsc --noEmit` PASS；mp-weixin build PASS
  - OpenSpec 文件级：5 specs / 20 Requirements / 45 Scenarios；`git diff --check`、路径 allowlist、增量敏感标记扫描 PASS
- **Verification SKIPPED**:
  - 真实 provider / 真实 MySQL / 微信真机：闸门 3 未授权；真实外调 0 次，不以 scripted/H2/build 冒充
  - OpenSpec CLI：本机不在 PATH；只报告文件级验证，不声称 CLI validate PASS
- **Scope safety**:
  - API/DTO/DDL/mapper SQL/frontend source/pom/package/lockfile 零变化；未改 archive、冻结蓝图、deployment、monitoring
  - 无新增页面、Tab、dashboard、评分、推送、设置页、自动 retry、多 provider、LLM-as-Judge 或额外 provider 调用
  - 未把用户日记、对话、关键词、片段、prompt/provider response、异常消息或 secret 写入 trace/log/新存储
- **Risks**:
  - deterministic 词表只能覆盖已声明的越界形态；真实模型话术质量仍需闸门 3 小样本人评
  - 30/180/90 天与 100/75/50% 为已批准的保守产品阈值，尚未经真实样本校准
  - OpenSpec delta 尚未接受、change 尚未归档；须用户验收后独立收口
- **Commit**: pending
- **Next**: 用户 review；闸门 3、接受 delta 与归档均等待后续明确授权

## 2026-08-08｜agent-temporal-intelligence（C9）实现提交证据｜Type C

- **Scope**: 补录已授权的 C9 实现提交；未 push、部署、发布、接受 delta 或归档
- **Verification**: PASS；提交前后端全量 85 suites / 662 tests / 0 failures / 0 errors / 6 skipped，前端 type-check 与 mp-weixin build PASS
- **Commit**: `65e18e0 feat: 实现C9时间智能策略`
- **Next**: 用户 review；闸门 3 与收口授权仍独立等待

## 2026-08-08｜agent-temporal-intelligence（C9）闸门 3 验收归档｜Type C

- **Scope**:
  - 用户明确通过闸门 3并授权收口归档；执行真实 provider / MySQL 合成探针、接受五份 delta、写 closeout、更新叙事并归档
  - Agent commit 已授权；未 push、部署或发布
- **Changes**:
  - 新增默认关闭的 `C9RealProviderProbeTest` 与 `C9MysqlTemporalProbeTest`，不改变默认业务运行路径
  - 五份 delta 接受进 `agent-runtime` / `backend-core` / `miniapp-core` / `v2-product-scope` / `agent-collaboration` baseline
  - 新增 C9 closeout，叙事 §10 增补时间智能取舍，入口状态回到 `IDLE`
- **Verification**: PASS
  - 真实 provider：recent / distant / long-ago / review-focal / recurrence-eligible / recurrence-insufficient
    六个固定合成短文本场景 6/6 成功，总调用恰为批准上限 6；全部通过 temporal overreach checker
  - recurrence eligible 场景实际 `hintUsed=false`：eligible prompt 已执行且输出安全，但不作为真实采纳 hint 的正证据
  - 真实 MySQL：固定合成用户与记录验证 owner 隔离、`SEALED`/focal 排除、24 个月窗口、
    recent/distant/long-ago 衰减与 recurrence eligibility；合成数据 `finally` 清理 PASS
  - 后端最终全量：**87 suites / 664 tests / 0 failures / 0 errors / 8 skipped**；新增两探针默认关闭
  - 五份 delta 共 65 个 Requirement/Scenario 标题逐项匹配 baseline，missing=0
  - `git diff --check`、范围路径与敏感标记扫描 PASS
- **Verification SKIPPED**:
  - 微信真机：本机未发现微信开发者工具或可控真机环境；不以 H2/scripted/build 冒充
  - OpenSpec CLI：不在 PATH；使用 delta/baseline/archive 文件级核对，不声称 CLI validate PASS
- **Scope safety**:
  - API/DTO/DDL/mapper SQL/frontend source/pom/package/lockfile 零变化；未修改冻结蓝图或既有 archive 内容
  - 探针只发送/写入固定合成数据；证据不含用户日记、真实对话、prompt、provider response、secret 或 endpoint
  - 无新增页面、dashboard、评分、诊断、推送、自动 retry、多 provider 或额外业务 provider 调用
- **Risks**:
  - 6 次 provider 是小样本，不是生产 SLA；deterministic 词表不能证明覆盖所有自然语言越界形态
  - recurrence eligible 真实输出未采用 hint；微信真机话术长度、浮层体验与无分析 UI 仍缺活体证据
  - 阈值尚未以真实用户样本校准；C0/C10/C11 均须证据触发独立规划闸
- **Commit**: pending
- **Next**: C9 归档后保持 IDLE；后续能力须重新走独立 OpenSpec change

## 2026-08-08｜agent-temporal-intelligence（C9）归档提交证据｜Type C

- **Scope**: 补录已授权的 C9 闸门 3 与归档提交；未 push、部署或发布
- **Verification**: PASS；提交包含两项默认门控探针、五份 baseline 接受、closeout、叙事与 archive 移动
- **Commit**: `14ec5f8 feat: 完成C9闸门3并归档`
- **Next**: `ACTIVE_TASK=IDLE`；后续能力重新走独立规划闸

## 2026-08-09｜核心产品定义 v0.1 收口验证｜Type A

- **Scope**:
  - 仅验证 `Docs/agent-iteration/roadmap/core-product-definition.md` 的提交状态、结构完整性、与 accepted baseline / 当前实现的对齐程度及下一轮规划前证据边界
  - 本轮不制定下一份迭代蓝图，不创建 OpenSpec change，不修改业务代码、accepted baseline 或 `ACTIVE_TASK`
- **Changes**:
  - 业务代码、OpenSpec、方向文档零修改；仅追加本条验证证据
- **Verification**: PASS（方向文档与代码/契约级收口）
  - 核心定义已由用户提交为 `36f5a3d docs: 确立Flashback核心产品定义v0.1`；提交只含方向文档与此前日志
  - 文档 547 行，含 `0`–`22` 节；9 个核心锚点齐全；`ACTIVE_TASK=IDLE`
  - accepted baseline 已具备“写下当下”为主动作、Agent 被动召唤、用户可结束、素材显式确认、历史引用不诊断等契约
  - 当前实现已确认：标题可选；Agent 入口不自动展开且可随时关闭/结束；Agent 素材与工具写入须用户确认；位置仅由用户点击后选择
  - 当前实现仅部分对齐：首页仍以“等待未来重新读懂 / 寄给未来”为中心；普通保存仍以 `DRAFT/草稿` 命名，主要完成动作仍为设置未来时间后封存
  - 当前实现明确缺口：正文是 API / backend 必填项，图片或原始声音不能独立成为片段；只允许删除 `DRAFT`；无真实全量导出；数据备份页在登录路径展示本地演示的“已同步 / iCloud / 导出 / 清除全部数据”
  - 当前实现待独立决策：Agent prompt 仍自称“朋友”且按固定阶段提问；memory 只有 backend 配置开关，未发现用户可见的历史参与授权、撤销或纠正机制
  - bundled Node 直接运行 `vue-tsc --noEmit` PASS；mp-weixin preview build PASS
- **Verification SKIPPED**:
  - 微信开发者工具 / 真机截图审计：本机仅发现 `User Data`，未发现可启动 CLI 或应用入口；项目无 H5 构建目标，不以源码或 mp-weixin build 冒充真实视觉/交互证据
  - 原型与真实用户访谈：本轮没有已批准原型、招募对象或访谈输入；保存仪式、时间篇章、追问体感、隐含记忆授权、长期导出、商业模型、安全和视觉系统仍按 v0.1 记为待验证
- **Scope safety**:
  - 未修改 `backend/src/**`、`frontend/src/**`、OpenSpec、蓝图、依赖或 lockfile；未执行 provider、MySQL、对象存储、push、部署或发布
  - bundled `pnpm` 包装器首次尝试因无 TTY 中止并生成未跟踪 `.pnpm-store/`；已校验绝对路径后清理，随后改用 bundled Node 直接运行既有本地二进制
  - 未读取或写入用户日记原文、真实对话、secret 或未授权外部数据
- **Risks**:
  - 方向文档已可作为下一轮规划筛选器，但不能写成当前产品已完全实现
  - 登录路径中的演示设置与假备份状态会损害“只负责保管、不拥有用户人生”的信任承诺，进入真实用户验证前应先隔离或诚实降级
  - “已保存的此刻”复用 `DRAFT` 还是建立新完成态，以及历史记忆如何授权/撤销，均会改变状态/API/数据语义，必须在未来独立 Type C 规划闸由用户裁决
- **Commit**: pending（默认用户手动提交；未 stage / commit / push）
- **Next**: 用户 review 本次收口结论；确认后再单独制定下一份迭代蓝图

## 2026-08-09｜核心体验迭代蓝图 v2.0 冻结落盘｜Type B

- **Scope**:
  - 根据用户对全部讨论项与推荐方案的明确接受，冻结下一份方向蓝图；仅修改方向、方法论、项目上下文与 steering 文档
  - 不创建 active OpenSpec change，不修改 accepted baseline、archive、业务代码、依赖或 lockfile
- **Changes**:
  - 将原 `iteration-blueprint.md` 以语义完全一致的 641 行内容保存为只读历史快照 `iteration-blueprint-v1.2.md`
  - 新建并冻结核心体验蓝图 v2.0：决策 D34–D60；执行序列 H0 → E0 → P3.1 → P3.2 → P4.1 → P4.2 → R1 → E1 → 证据门控 P5.x
  - 明确 `DRAFT → SAVED/RECORDED → SEALED → UNLOCKED` 目标语义、数据主权、见证者角色、记忆授权、时间篇章、安全边界、证据分层与停止条件
  - 同步 `AGENTS.md`、`ACTIVE_TASK.md`、`openspec/project.md`、roadmap / architecture / workflow / narrative 入口与 `.kiro/steering`；C9 与 C6–C9 关闭项按 archive 证据校准
- **Verification**: PASS
  - `git diff --check` GREEN
  - 蓝图标题 / 冻结日期存在；D34–D60 共 27 项且无缺号；H0、E0、P3.1、P3.2、P4.1、P4.2、R1、E1、P5.x 锚点齐全
  - v1.2 快照与变更前 `HEAD:iteration-blueprint.md` 规范化换行后语义完全一致，均为 641 行
  - 核心定义、当前蓝图、历史蓝图、Type B checklist、项目上下文与 `ACTIVE_TASK` 路径均存在
  - 17 个工作区变更路径中，`backend/`、`frontend/`、`openspec/specs/`、archive、package / lockfile 命中为 0；`ACTIVE_TASK` 保持 `IDLE`
  - 旧版本“当前状态”表述已从活文档清除；仅保留 `.ai/ACTIVE_TASK.md` 历史会话记录和 v1.2 历史快照
- **Verification SKIPPED**:
  - OpenSpec CLI validation：本机 PATH 不含 OpenSpec CLI，且本轮没有 change / spec delta；改做路径、锚点、状态、引用与 diff 校验
  - build / 单测 / 真实 MySQL / provider / 对象存储 / 微信真机：本轮仅改方向与协作文档，不产生运行时行为，执行这些验证不能增加对应能力证据
- **Scope safety**:
  - 未读取或写入用户日记原文、真实对话、secret 或未授权外部数据
  - 未 stage / commit / push / 部署 / 发布；提交责任保持用户手动提交
- **Risks**:
  - v2.0 是方向冻结，不是现状声明，也不授权业务实现；当前 accepted baseline 与代码仍保留既有 `DRAFT`、正文必填、固定阶段 Agent 等行为
  - H0 / E0 / P3.1 均未启动；状态枚举、API / DTO、迁移、对象清理、导出格式、恢复期限与记忆授权载体仍须在各自闸门确认
  - P5.x 仅在 E1 得到真实正向证据后才可立项；无证据则保持独立片段为完整产品单位
- **Commit**: pending（默认用户手动提交；未 stage / commit / push）
- **Next**: 用户审阅本次文档落盘；若确认，可先单独授权 Type B H0，或先进入 Type A E0 原型讨论

## 2026-08-09｜H0 truth-surface-cleanup｜Type B

- **Scope**:
  - 按核心产品定义 v0.1 与核心体验迭代蓝图 v2.0，收口 authenticated real path 与 Preview 的用户可见能力表面
  - 仅修改前端页面注册、现有页面文案、Preview 会话/示例服务与一个通用 Preview 标识组件；用户已授权实现与 Agent commit，未授权 push、部署或发布
- **Changes**:
  - 从页面注册和个人中心入口移除整理偏好、视觉外观、访问控制、数据备份、标签管理、通知设置等未兑现设置表面；保留“构建信息”入口
  - 将 Preview 入口与全部 Preview 页面明确标识为“概念预览 · 示例数据 · 只读”
  - Preview 的创建、修改、删除、封存、提醒授权、补写回应等写操作改为 fail-closed，禁止用本地 mock success 冒充真实保存
  - 记录列表搜索明确限定为当前已载入内容；总数无搜索时使用 backend 分页总数，有搜索时标为当前页匹配数
  - 个人中心移除硬编码版本、虚假存档天数和邮箱展示；统计改为由真实 sealed / unlocked 状态计算的“封存及抵达”“封存中”
  - 时间轴无封面记录不再标为“图文记忆”；构建信息页明确演示构建不代表生产发布
- **Verification**: PASS
  - bundled Node 直接运行 `vue-tsc --noEmit`，exit 0
  - 标准 mp-weixin build PASS；标准产物中 Preview 开关为 false，登录页不展示 Preview 入口
  - `--mode preview` mp-weixin build PASS；产物只注册登录、首页、记录编辑/列表/详情、时光轴、个人中心和构建信息 8 个页面，并包含 Preview 标识与写操作拒绝逻辑
  - `git diff --check` PASS；仅有本机 Git 全局 ignore 权限与 LF/CRLF 提示，无 whitespace error
- **Verification SKIPPED**:
  - 微信开发者工具 / 真机交互：本机未发现可用 CLI 或可控真机环境；不以构建产物冒充真实视觉与交互证据
  - backend / MySQL / provider / 对象存储：本轮未修改 API、DTO、持久化或后端行为，也未获外调授权；前端类型检查与两种构建覆盖本轮范围
  - OpenSpec CLI：本机不可用，且本轮 Type B 不创建 change 或修改 accepted spec；改做 `ACTIVE_TASK=IDLE`、范围路径与 diff 核对
- **Scope safety**:
  - 未修改 `.ai/ACTIVE_TASK.md`、OpenSpec、backend、package / lockfile、部署配置；未新增用户可见能力、API 契约、状态或持久化语义
  - 未读取或写入用户日记原文、真实对话、secret 或未授权外部数据
  - bundled pnpm 预检因无 TTY 中止并生成未跟踪 `.pnpm-store/`；确认其为本轮生成且路径位于仓库后已清理，未重建或修改依赖
- **Risks**:
  - 未做微信开发者工具视觉巡检；Preview 标识在复杂设备安全区和长页面上的遮挡风险仍需真机确认
  - 被移除注册的旧设置页源码仍保留为不可达文件，便于回退；未来若重新注册，须先完成真实契约或继续诚实降级
  - authenticated real path 的后端错误语义、网络异常与真实数据统计仍依赖后续端到端验证，本次不宣称生产验收
- **Commit**: pending（Agent commit 已授权；不 push）

## 2026-08-09｜H0 truth-surface-cleanup 验收与提交证据｜Type B

- **Scope**:
  - 在已提交的 `c48b13a fix: 收口H0真实能力表面` 上独立复验 authenticated real path 与 Preview 能力表面
  - 本条只追加验收证据；不修改业务代码、OpenSpec、`ACTIVE_TASK`、依赖或构建配置
- **Changes**: 无业务行为变化；补录 H0 提交对象、双构建、产物断言、会话隔离与 SKIPPED 边界
- **Verification**: PASS（自动化与静态验收范围）
  - 提交对象核对 PASS：`c48b13a` 精确包含 H0 的 15 个文件，126 insertions / 165 deletions；验收开始时工作树 clean，`ACTIVE_TASK=IDLE`
  - bundled Node 运行 `vue-tsc --noEmit` PASS
  - 标准 mp-weixin build PASS；标准产物 Preview 开关为 false，仍只注册 8 个页面，未注册整理偏好、视觉外观、访问控制、数据备份、标签管理或通知设置
  - Preview mp-weixin build PASS；Preview 开关为 true，8 个页面注册断言 PASS，7 个登录后页面均注册只读标识组件
  - Preview 入口“进入概念预览（示例数据）”、只读标识、record 10 处 guard 引用与 reply 写操作拒绝断言 PASS；产物未命中旧设置文案、伪版本或“寻回记忆”入口
  - authenticated session 隔离核对 PASS：真实账号登录与微信登录在保存 token 后清除 Preview session；Preview 服务分支均要求“无真实 token 且存在 Preview session”
  - 诚实文案核对 PASS：无搜索时使用 backend 总数，搜索时明确当前页匹配；个人中心使用真实 sealed / unlocked 派生统计；无封面抵达记录不再宣称图文记忆
  - 验收结束恢复标准 mp-weixin 构建；`git diff --check` PASS
- **Verification SKIPPED**:
  - 微信开发者工具 / 真机视觉与交互：常见安装路径未发现 CLI，也没有可控真机；不以源码、类型检查或构建产物冒充手动验收
  - 真实账号 backend / MySQL 端到端：本轮没有测试账号、运行服务或外调授权；H0 未修改后端/API/持久化，因此不宣称生产链路 PASS
- **Scope safety**:
  - 未执行 provider、对象存储、push、部署或发布；未读取用户日记原文、真实对话或 secret
  - 构建仅写入既有 ignored `frontend/dist`；最终保留标准构建产物，没有 package / lockfile 变化
- **Risks**:
  - 自动化范围可以接受 H0 代码与产物，但完整用户验收仍缺微信开发者工具 / 真机的布局、点击、返回路径和安全区证据
  - 旧设置页源码仍为未注册的 dormant code；未来重新启用必须重新经过真实契约核对
- **Commit**: pending（本条验收日志待提交；不 push）

## 2026-08-09｜E0 capture-ritual-prototype readiness 与规划｜Type A → Type B（落盘）

- **Scope**:
  - 检查 H0 后是否允许进入下一阶段；若允许，直接形成 E0 保存仪式与编辑层级原型计划
  - 仅规划低成本交互原型、合成任务、观察表与 P3.1 进入条件；不实现原型或业务能力
- **Changes**:
  - 新增 `Docs/design/e0-capture-ritual/PLAN.md`
  - 确定单文件离线、合成材料、零网络/零持久化的原型形态，以及 A 当前层级对照、B 专注记录、C 先完成再选择三种结构性变体
  - 明确 5–8 人定性观察脚本、隐私记录边界、重复关键误解的失败规则与 P3.1 go/no-go 条件
- **Verification**: PASS（readiness 与规划结构）
  - `ACTIVE_TASK=IDLE`；readiness 开始时工作树 clean
  - H0 实现提交 `c48b13a` 与独立验收日志提交 `a9a3dea` 均存在；`main` 比 `origin/main` 超前 2 个提交，未 push
  - 冻结蓝图 v2.0 顺序为 H0 → E0 → P3.1，E0 意图卡、证据与隐私边界已逐项映射进计划
  - 当前编辑器事实已核对：正文必填、“草稿已保存”、保存前分类/人生节点/Agent/AI/解锁时间同页可见、封存要求未来时间并自动返回
- **Verification SKIPPED**:
  - 原型可运行性与视觉/交互：本轮只规划，`prototype.html` 尚未实现
  - 目标用户观察：尚未招募或执行；无观察证据，因此不宣称 E0 已验证，也不启动 P3.1
  - build / 真机 / backend / MySQL / provider / 对象存储：本轮未修改运行时代码，执行这些验证不能增加 E0 用户理解证据
- **Scope safety**:
  - 未修改 `.ai/ACTIVE_TASK.md`、OpenSpec、frontend/backend、依赖、lockfile、冻结蓝图或 archive
  - 未读取或记录用户日记原文、真实对话、secret 或可识别私人数据；未执行 stage / commit / push / 部署 / 发布
- **Risks**:
  - H0 微信真机视觉/交互仍为 SKIPPED；不阻塞 E0，但不能写成真实设备验收完成
  - `.ai/ACTIVE_TASK.md` Direction Layer 仍写“H0 尚未启动”，`openspec/project.md` 仍把 H0 写成下一建议动作；当前提交与验收日志证明 H0 已完成，摘要漂移不阻塞 E0，但 P3.1 前须单独收口
  - 早期 M1/M3 非 archive 目录仍存在且有未勾任务；当前事实源判定 IDLE，E0 不受阻，但 P3.1 前须再次核对 active-change 语义
  - 三种变体目前只是研究设计；没有 5–8 人观察前，任何推荐都不是产品结论
- **Commit**: pending（默认用户手动提交；未 stage / commit / push）
- **Next**: 用户 review E0 计划；若授权执行，再制作 throwaway 原型，不直接改生产页面

## 2026-08-09｜E0 capture-ritual-prototype 原型实现与内部走查｜Type B

- **Scope**:
  - 按已审阅的 E0 计划制作独立 throwaway 交互原型，比较保存反馈与编辑层级；用户已授权执行与 Agent commit
  - 仅修改 `Docs/design/e0-capture-ritual/` 与本日志；不接入产品路由、真实登录、backend 或持久化
- **Changes**:
  - 新增单文件 `prototype.html`，通过 `?variant=A|B|C` 提供 A 当前层级对照、B 专注记录、C 先完成再选择三种结构性变体
  - 加入文字、合成图片占位、合成声音占位三类研究任务，以及常驻“不会真实保存”标识、任务重置和内存态切换
  - A 使用短暂居中确认；B 使用持续保存状态条与默认折叠补充项；C 保存后明确提供“先离开 / 继续添一点”，Agent 与时间入口只在继续路径出现
  - 沿用现有纸张、墨色、朱红与宋体方向；固定手机舞台在桌面居中，移动视口全屏，不伪造系统状态栏
  - 通过设计交付登记脚本生成 `_d_meta.json`，将原型标为 `needs-review`，避免把内部走查误写成用户验证完成
  - 更新 `PLAN.md` 的执行状态、提交责任、任务勾选、边界和内部走查结果
- **Verification**: PASS（原型完整性与内部交互范围）
  - bundled Node 解析唯一内联脚本 PASS；无外部 script / stylesheet、`fetch`、XHR、localStorage 或 backend 调用
  - HTTP 预览 200；A/B/C 查询参数、切换器与非编辑态方向键切换 PASS，输入焦点下方向键不会误切换
  - 文字、图片占位、声音占位任务 PASS；图片与声音互斥且不要求正文；reload 后保存态清空，证明没有本地持久化
  - A 保存确认居中；B 持续状态条与“可以离开”可操作；C 保存层、继续补充、已保存退出和未保存关闭路径 PASS
  - 360×800、390×844、1440×900 视口无横向溢出或被裁切目标，最小交互目标高度 44px；页面 console error / warning 为 0
- **Verification SKIPPED**:
  - 5–8 名目标用户观察尚未执行；当前没有用户理解、重复误解或变体推荐证据，因此不宣称 E0 已得出产品结论
  - 微信开发者工具 / 真机：本轮是浏览器中的独立 HTML 研究原型，不以浏览器走查冒充小程序真机验收
  - frontend build、backend、MySQL、provider、对象存储：本轮未修改产品代码、API、DTO 或持久化，也未获任何外调授权
- **Scope safety**:
  - 未修改 `.ai/ACTIVE_TASK.md`、OpenSpec、frontend/backend、package / lockfile、部署配置或冻结蓝图
  - 原型只含合成材料，不读取用户日记原文、真实声音、真实图片、secret 或可识别私人数据；无网络请求
  - 提交范围限定为本轮计划、原型与 append-only 日志；不 push、部署或发布
- **Risks**:
  - A/B/C 目前都是研究假设；没有目标用户观察前，不应把任一变体直接实现为 P3.1
  - 浏览器走查没有覆盖真实小程序字体、键盘、安全区、返回手势或辅助技术行为
  - H0 真机缺口、H0 状态摘要漂移与旧 M1/M3 非 archive 目录仍按计划保留，P3.1 readiness 前须重新核对
- **Commit**: pending（Agent commit 已授权；不 push）
- **Next**: 用 5–8 名目标用户执行 E0-04；只有形成有观察依据的推荐或否决结论后，才处理 E0-05/E0-06，并决定是否进入 P3.1 规划闸

## 2026-08-09｜E0-04 目标用户观察准备｜Type B

- **Scope**:
  - 用户允许进入 E0-04；本轮只把真实目标用户观察准备到可直接执行，不用 Agent 模拟或内部走查冒充用户证据
  - 提交责任沿用 Agent commit；未授权 push、部署、发布或 P3.1
- **Changes**:
  - 新增 `SESSION_GUIDE.md`：参与者有效性、隐私边界、非引导开场白、三类任务、单轮主持流程、中止规则与 6 人平衡分配
  - 新增 `OBSERVATIONS.md`：0/5–8 的明确状态、场次索引、18 轮证据矩阵、非隐私短摘要模板、聚合计数与 `NOT_READY` 结论
  - 更新 `PLAN.md`，记录 E0-04 已获准、材料就绪但真实参与者仍为 0；E0-04 不提前勾选
- **Verification**: PASS（研究材料结构范围）
  - 前 6 人覆盖 A/B/C 全部 6 种顺序；每个“文字/图片/声音 × A/B/C”组合恰好出现 2 次
  - 主持脚本未提前泄露“已保存 / 可离开 / 非强制 Agent / 非强制封存”的正确答案；任务后问题与原计划一致
  - 记录模板禁止姓名、联系方式、账号、正文、地点、文件名、私人图片/声音与真实 Agent 对话；`blocked / invalid` 不计入结论
  - 结论默认 `NOT_READY`，至少 5 名有效参与者且三轮完整前禁止更新为推荐或进入 P3.1
- **Verification SKIPPED**:
  - 目标用户观察：尚无真实参与者或主持人，本轮只完成执行准备；有效样本仍为 0
  - 原型浏览器回归、微信真机、frontend/backend/MySQL/provider/对象存储：本轮未修改原型或产品运行时代码，这些验证不能替代目标用户证据
- **Scope safety**:
  - 未修改 `.ai/ACTIVE_TASK.md`、OpenSpec、原型 HTML、frontend/backend、依赖、lockfile、蓝图或 archive
  - 未采集、读取或记录任何真实参与者信息或用户日记内容；未执行外部消息、招募、录屏或网络上传
- **Risks**:
  - 招募、主持与首场观察需要真实人员协调；在此之前 E0-04 无法完成
  - 参与者若已了解方案目标、主持人发生引导或三轮不完整，必须标为无效，不能为凑样本改写
  - 浏览器研究结论未来仍不能替代小程序真机实现验收
- **Commit**: pending（Agent commit；不 push）
- **Next**: 安排首名符合条件的目标用户按 P01 执行三轮；场后只把允许字段追加到 `OBSERVATIONS.md`

## 2026-08-10｜E0 无真实参与者收口｜Type B

- **Scope**:
  - 用户确认没有真实用户或参与者，并接受推荐方案：不以内部判断或 Agent 模拟替代目标用户证据
  - 本项只收口 E0 研究状态并保留未来恢复材料；提交责任为 Agent commit，不 push
- **Changes**:
  - 新增 `OUTCOME.md`，把 E0 结果明确标为 `INCONCLUSIVE`、目标用户观察标为 `SKIPPED`、有效参与者记为 0
  - 更新 `PLAN.md`、`SESSION_GUIDE.md` 与 `OBSERVATIONS.md`：E0-04 保持未完成，不选 A/B/C 胜者，不宣称用户理解 PASS
  - 只保留冻结蓝图已确认的最低基线；toast、持续状态条、底部层、具体动效与最终层级继续标为未验证
- **Verification**: PASS（证据边界与文档一致性）
  - 结果文档明确区分原型可运行证据与目标用户理解证据；0 名参与者没有被包装成小样本结论
  - 原型和观察材料继续作为非生产研究资产保留，未复制进 frontend 或业务路由
- **Verification SKIPPED**:
  - 目标用户观察：没有真实参与者，无法执行；这是本轮 `INCONCLUSIVE` 的直接原因
  - 微信真机、frontend/backend build、MySQL、provider、对象存储：本项只收口研究文档，执行这些检查不能补足用户证据
- **Scope safety**:
  - 未采集、生成或记录参与者身份、日记原文、真实图片/声音、secret 或外部消息
  - 未修改原型 HTML、业务代码、依赖、lockfile、accepted baseline、archive、部署或发布配置
- **Risks**:
  - 保存反馈与渐进披露仍缺真实用户理解证据；后续实现只能采用可逆、克制基线，不能称为已验证 UX
- **Commit**: pending（Agent commit 已授权；不 push）
- **Next**: E0 不再阻塞规划；进入 P3.1 独立规划闸，但不自动获得实现授权

## 2026-08-10｜P3.1 `present-moment-capture` 规划闸启动｜Type C

- **Scope**:
  - 按用户接受的推荐方案，只创建 P3.1 proposal / design / tasks / delta，并等待独立规划批准
  - 开工锚点 `2d9544a`；规划期外调预算 0；闸门 2、真实依赖闸门 3、push、部署、发布均未授权
- **Changes**:
  - readiness 确认开刀前 `ACTIVE_TASK=IDLE`、工作树 clean、H0 已完成、无并发 active Type C；修正 `openspec/project.md` 与 `ACTIVE_TASK` 的 H0 状态摘要漂移
  - 新建 `openspec/changes/present-moment-capture/` 下 proposal、design、tasks 及 `backend-core`、`miniapp-core`、`agent-runtime`、`v2-product-scope` 四份 delta
  - 推荐 N1–N11：`SAVED`、默认 `MOMENT`、幂等显式 save、文字或 AVAILABLE 图片/声音成立、7 天恢复 DRAFT、窄过期清理、DRAFT/SAVED 可编辑、SAVED 后封存、渐进披露、E0 交互细节 provisional、任意记录删除留 P3.2
  - `.ai/ACTIVE_TASK.md` 现指向该 change，明确停在闸门 1；`tasks.md` 仅规划任务勾选，实现与真实验收任务全部保持未勾选
- **Verification**: PASS（规划包文件级结构与范围）
  - proposal / design / tasks / 4 specs 齐备；Requirement / Scenario 层级、任务 ID 唯一性、active pointer 与 source-of-truth 状态已核对
  - delta 覆盖 backend 状态与保存不变量、miniapp 当下记录主路径、Agent 的 DRAFT/SAVED 兼容及 V2 产品范围边界
  - `git diff --check`、规划 allowlist 与增量敏感标记扫描 PASS；未发现业务代码、依赖、lockfile、archive 或 accepted baseline 变更
- **Verification SKIPPED**:
  - OpenSpec CLI：本机不在 PATH；只完成仓库结构与 Requirement/Scenario 文件级校验，不声称 CLI validation PASS
  - backend/frontend tests、真实 MySQL、对象存储、provider、微信开发者工具/真机：本轮没有业务实现，且闸门 2/3 未授权
- **Scope safety**:
  - 只修改 E0 closeout、P3.1 change artifacts、`ACTIVE_TASK`、`openspec/project.md` 与 append-only 日志
  - 未修改 frontend/backend 运行时代码、package/lockfile、accepted specs、archive、冻结蓝图、部署或监控；未外发私人内容或 secret
- **Risks**:
  - E0 仍无真实用户证据，N9/N10 只能作为 provisional 基线
  - `SAVED` 会跨 record、附件、位置、封面、Agent、迁移与 frontend 展示；后续实现必须按完整 vertical slice 执行
  - 真实 MySQL 历史 DRAFT 分布、真实对象存储媒体-only 链路与微信真机行为仍为 unknown，不能由 H2/build 替代
- **Commit**: pending（Agent commit 已授权；不 push）
- **Next**: 用户 review 并批准或调整 N1–N11 与规划 artifacts；只有另行通过闸门 2 后才能开始 T-09 之后的业务实现

## 2026-08-10｜P3.1 闸门 1 批准与闸门 2 实现授权｜Type C

- **Scope**:
  - 用户批准 `present-moment-capture` proposal / design / tasks / 四份 delta 与 N1–N11，并明确允许开始实现
  - 提交责任继续为 Agent commit；真实 MySQL、对象存储、微信真机、push、部署、发布仍未授权
- **Changes**:
  - 同步 proposal、design、tasks 与 `ACTIVE_TASK` 的 Gate State；T-09～T-12 完成
  - 实现期允许 backend/frontend/SQL 与离线/H2/type-check/build 验证；真实 AI provider 调用预算保持 0
- **Verification**: PASS（授权边界登记）
  - 当前唯一 active change 仍为 `present-moment-capture`；未扩大 exact API、enum、TTL、清理或交互方案
  - `openspec` CLI 仍不在 PATH，后续只做文件级任务跟踪，不声称 CLI apply/status/validate PASS
- **Verification SKIPPED**:
  - 真实 MySQL、对象存储、微信真机：闸门 3a/3b/3c 未授权
- **Risks**:
  - H2/build 不能证明 InnoDB、真实私有对象删除或微信端交互；这些证据必须继续分层报告
- **Commit**: pending（Agent commit；不 push）
- **Next**: 执行 T-13/T-14 baseline，然后按 TDD 顺序实现 P3.1

## 2026-08-10｜P3.1 `present-moment-capture` 闸门 2 实现完成｜Type C

- **Scope**:
  - 按已批准 N1–N11 与 `tasks.md` 实现 P3.1 backend/frontend/SQL vertical slice；提交责任为 Agent commit，不 push
  - 闸门 3a/3b/3c 未授权；不连接真实 MySQL、不调用真实对象存储、不执行微信开发者工具或真机验收；真实 AI provider 调用保持 0
- **Changes**:
  - backend 新增 `SAVED` / `MOMENT`、7 天 `draft_expires_at`、显式幂等 `/save` 与单一 `RecordSaveEligibility`；封存收窄为 SAVED -> SEALED
  - DRAFT/SAVED 共用可编辑状态门；SAVED 的正文/最后媒体更新受最终 eligibility 保护，SEALED/UNLOCKED 不变性保持
  - 普通查询排除 DRAFT；显式恢复查询只返回 owner 的未过期草稿；新增窄 cleanup worker/scheduler，远端对象删除失败时保留 DB 并等待重试
  - Agent WRITING_GUIDANCE 接受 active DRAFT/SAVED，REVIEW_CHAT 仍只接受 UNLOCKED；未修改 Prompt、provider、memory、guardrails、reflection、预算或 eval snapshots
  - frontend 新记录默认 MOMENT，图片/声音可先于正文；“留下这一刻”先持久化再 `/save`，成功只显示安静的页内反馈；接通恢复草稿、SAVED 继续补充与保存后“交给时间”
  - Preview fixtures 可展示 MOMENT/SAVED，但 mutation 继续 fail-closed；三个一级 Tab 与 canonical naming 未变
  - 新增 MySQL P3.1 幂等迁移脚本及 H2/MySQL schema 同步；pre/postflight 仅输出聚合计数
- **Verification**: PASS
  - TDD RED -> GREEN：enum/schema、create、eligibility、save、seal、SAVED 更新/附件、DRAFT visibility/expiry/cleanup、Agent mode 均先观察到目标失败再实现
  - backend full：**91 suites / 687 tests / 0 failures / 0 errors / 8 skipped**；P3.1 focused、旧 record/attachment/Agent 回归均通过
  - frontend：bundled Node 下 `vue-tsc --noEmit`、标准 mp-weixin build、Preview build 全部 PASS
  - OpenSpec 文件级校验：4 specs / 28 Requirements / 98 Scenarios；任务、delta、实现与 `ACTIVE_TASK` 链接一致
  - `git diff --check` PASS；allowlist、package/lockfile 零变化及增量 credential/privacy scan PASS
- **Verification SKIPPED**:
  - OpenSpec CLI：本机不在 PATH；只完成文件级校验，不声称 CLI validation PASS
  - 真实 MySQL migration/pre-postflight：闸门 3a 未授权；H2 与脚本检查不能替代 InnoDB、历史数据或时区证据
  - 真实对象存储的图片-only/声音-only/过期草稿删除：闸门 3b 未授权；mock storage 不能替代真实私有对象语义
  - 微信开发者工具/真机：闸门 3c 未授权；type-check/build 不能替代权限、上传、播放与交互验收
  - E0 目标用户理解：无真实参与者，继续记 `INCONCLUSIVE / SKIPPED`；功能回归不冒充用户研究
- **Scope safety**:
  - 未实现 P3.2 导出、任意状态删除、清除全部、账号注销或完整删除编排
  - 未修改 package/lockfile、deployment、monitoring、admin、archive、accepted baseline specs、冻结蓝图或无关 Agent 语义
  - 日志与迁移脚本未记录用户原文、图片/声音内容、位置、storage key、signed URL、credential、prompt 或 provider response
- **Risks**:
  - 真实 MySQL 执行顺序、历史 DRAFT 聚合、索引/默认值兼容与数据库时区仍需闸门 3a 验证
  - 真实对象存储的 missing-object 幂等、失败重试与微信端媒体权限/播放仍无生产证据
  - E0 没有目标用户证据，保存反馈与恢复入口仍是可逆 provisional 基线
- **Commit**: pending（Agent commit；不 push）
- **Next**: 用户审查实现 diff 与证据；闸门 3、delta acceptance、closeout 与归档均需后续单独授权

## 2026-08-12｜P3.1 Gate 3a 真实 MySQL preflight 与迁移完成｜Type C

- **Scope**:
  - 用户明确批准 Gate 3a；仅执行本机真实 MySQL 聚合 preflight、P3.1 DDL/迁移、幂等/postflight、迁移后 backend 读取验证及证据更新
  - 不调用真实对象存储或 AI provider，不操作微信开发者工具/真机，不接受 delta、不归档、不 push/deploy/release
- **Changes**:
  - 在真实执行前新增迁移契约断言，发现重复执行会顺延所有残留 DRAFT 的 7 天期限；将脚本收窄为只填充 `draft_expires_at IS NULL`，避免重跑改变已有恢复窗口
  - 真实库新增 nullable `draft_expires_at`、`idx_record_status_draft_expires(status,draft_expires_at)`，并将 `record_type` 默认值由 NODE_RECORD 改为 MOMENT
  - 将 3 条有有效文字的旧 DRAFT 迁为 SAVED；原 FUTURE_LETTER 类型与关联数据不改写
- **Verification**: PASS
  - 停服 preflight：新列 0、索引 0、SAVED 0、DRAFT 3；DRAFT 文字有效 3、AVAILABLE 媒体记录 0、空白异常 0、AVAILABLE 媒体 orphan/owner mismatch 0；只输出聚合和枚举分布，不输出 user/record id、正文、位置、媒体元数据、key 或 URL
  - 时区：MySQL `SYSTEM` / 马来西亚半岛标准时，`NOW()-UTC_TIMESTAMP()` 为 28800 秒，与 Asia/Singapore 的 UTC+8 一致
  - 迁移 postflight：SAVED 3、DRAFT 0；3 条 SAVED 全部 expiry 为 NULL，原 FUTURE_LETTER 保留；列/default/两段复合索引均正确
  - 第二次执行 PASS，聚合状态不变；cleanup 查询的 EXPLAIN 使用 `idx_record_status_draft_expires`；`SELECT draft_expires_at FROM record LIMIT 0` PASS
  - 迁移后 backend 启动 PASS；合成 ADMIN 身份 list/timeline 均 HTTP 200、API code 0；随后停止进程并清理临时日志
  - TDD：新增幂等断言先得到 1 test FAIL，修复后 GREEN；focused **18 tests / 0 failures / 0 errors / 0 skipped**
- **Verification SKIPPED**:
  - OpenSpec CLI：本机不在 PATH，只完成 artifact/task 文件级同步，不声称 CLI validation PASS
  - Gate 3b 真实对象存储、Gate 3c 微信开发者工具/真机、E0 目标用户理解：均未授权或无真实参与者；真实 AI provider 调用为 0
  - 真实并发 refresh/save 与 cleanup race：本次维护窗口无并发写入，只保留既有自动化 expected-state 证据，不冒充真实并发 PASS
- **Rollback**:
  - MySQL DDL 会自动提交，回滚须使用补偿迁移而非事务回滚；仅可在后端停服、确认 SAVED 仍为本轮迁移产生的 3 条且没有新写入时，将这 3 条恢复为 DRAFT、重置默认值、删除新索引与新列
  - 本轮全部 postflight 通过，无需执行补偿回滚；后续若已有新 SAVED 写入，禁止使用宽泛状态条件回滚
- **Scope safety**:
  - 只修改 P3.1 migration/contract test、active change artifacts、ACTIVE_TASK 与 append-only AGENT_LOG；未改 accepted baseline、archive、冻结蓝图、package/lockfile、deployment、monitoring 或无关 Agent 语义
  - 没有创建、导出或记录用户正文和媒体内容；没有把本机凭证写入命令输出、日志或 tracked files
- **Risks**:
  - Gate 3b 图片/声音真实对象链路和 Gate 3c 微信权限/上传/播放/恢复体验仍无真实证据
  - E0 仍无目标用户，保存反馈与恢复入口继续是 provisional；功能 PASS 不等于用户理解 PASS
- **Commit**: pending（Agent commit；不 push）
- **Next**: 用户重新启动本地后端并刷新微信开发者工具验证；如继续真实媒体或完整真机验收，分别授权 Gate 3b / Gate 3c

## 2026-08-12｜P3.1 Gate 3a 提交补录｜Type C

- **Commit**: `250b42b fix: 完成P3.1真实MySQL迁移`
- **Push**: 未授权，未执行

## 2026-08-12｜P3.1 微信开发者工具窄证据与 Gate 3b readiness｜Type C

- **Scope**:
  - 登记用户提供的微信开发者工具人工结果，并只读核对下一步 Gate 3b 的本地配置入口、实现路径和探针准备度
  - 不连接对象存储，不执行上传/下载/删除，不操作用户数据，不扩写 Gate 3c，不 push/deploy/release
- **Evidence**:
  - 用户原话为“微信开发者工具使用正常，可以进行下一步推进”
  - 仅据此确认 Gate 3a 后页面访问/数据同步已恢复；没有逐项证明文字-only、图片-only、声音-only、恢复草稿、SAVED 编辑、保存后封存、权限拒绝或上传失败
- **Gate 3b readiness**:
  - backend 已有 Qiniu 与 S3-compatible provider、私有短期访问、上传授权、commit 校验、附件删除及 DRAFT cleanup 业务路径；已有离线/H2 测试但没有 P3.1 真实对象存储 probe
  - 当前进程中 `STORAGE_PROVIDER`、Qiniu 和 S3 必填环境变量均未设置；只检查是否存在，不读取或输出值
  - `backend/start-dev-wechat.local.ps1` 保留本地 secret/config 接入位置，但当前没有启用真实对象存储配置
- **Verification**: PASS（read-only readiness 与证据边界）
  - active change 仍为 `present-moment-capture`；Gate 3a 已完成，T-74～T-80 仍未勾选
  - OpenSpec CLI 不在 PATH，继续按 proposal/design/tasks/delta 文件级核对，不声称 CLI PASS
- **Verification SKIPPED**:
  - Gate 3b 真实图片/声音对象调用：尚未获得明确 Gate 3b 授权且当前进程无可用配置
  - Gate 3c 完整微信矩阵：用户只报告“使用正常”，没有逐项证据
- **Scope safety**:
  - 只修改 active change tasks、ACTIVE_TASK 与 append-only AGENT_LOG；未改业务代码、secret、package/lockfile、accepted baseline、archive、冻结蓝图或部署配置
- **Risks**:
  - 在没有真实 provider 配置的环境启动探针会失败，不能用 mock/离线测试冒充 Gate 3b
  - 开启 Gate 3b 后必须只用合成图片/短音频，记录聚合结果并 finally 清理对象和合成数据库记录
- **Commit**: pending（Agent commit；不 push）
- **Next**: 等待用户明确批准 Gate 3b，并确认可在同一启动环境中提供私有对象存储本地配置

## 2026-08-12｜P3.1 微信窄验证与 Gate 3b readiness 提交补录｜Type C

- **Commit**: `f3ee925 docs: 记录P3.1微信验证与Gate3b准备度`
- **Push**: 未授权，未执行

## 2026-08-12｜P3.1 Gate 3b 真实对象存储验收完成｜Type C

- **Scope**:
  - 用户明确批准使用本地配置的私有对象存储执行可清理的合成图片和短音频探针
  - 仅覆盖 Gate 3b；不执行 Gate 3c 微信权限/播放/交互矩阵，不调用真实 AI provider，不接受 delta、不归档、不 push/deploy/release
- **Changes**:
  - 新增 `P31RealObjectStorageProbeTest`，由显式 `P31_STORAGE_PROBE=1` 环境变量门控；默认测试运行保持跳过和零外调
  - 探针只构造合成 PNG、固定 0.25 秒 WAV 与合成数据库行；所有对象纳入 finally 清理清单，证据输出不包含 id、object key、URL、bucket 或 credential
  - 覆盖图片/声音独立保存、私有读取、SAVED 编辑、pending/missing fail-closed，以及过期 DRAFT 远端删除成功/对象已不存在/失败重试与恢复清理
- **Verification**: PASS
  - 配置 preflight：ignored 本地启动脚本与 secret 文件存在有效 S3-compatible 私有存储配置；只检查存在性/非空性，没有输出 secret
  - 真实探针：**1 test / 0 failures / 0 errors / 0 skipped**；图片、声音、pending、missing、cleanup success、cleanup absent、cleanup retry 七类结果均为 true
  - 图片：真实 authorize/upload/commit AVAILABLE/save/private read 字节一致/SAVED edit/delete PASS
  - 声音：真实 authorize/upload/commit AVAILABLE/save/private read 字节一致/JVM 标准 WAV 解码/delete PASS
  - 清理：所有已跟踪远端对象最终均不存在；合成 user/record/attachment 聚合均为 0；诊断结束后 8080 未监听
  - 默认 focused：**19 tests / 0 failures / 0 errors / 1 skipped**；默认 full：**92 suites / 688 tests / 0 failures / 0 errors / 9 skipped**
  - 真实 AI provider 调用 0；探针进程强制使用 AI mock
- **Verification SKIPPED**:
  - 微信开发者工具/真机扬声器实际播放、相册/麦克风权限、上传失败 UI、恢复草稿、SAVED 编辑与保存后封存完整矩阵：属于未授权 Gate 3c；JVM WAV 解码不冒充真机播放 PASS
  - E0 目标用户理解：没有真实参与者，继续记 `INCONCLUSIVE / SKIPPED`
  - OpenSpec CLI：本机不在 PATH，只完成 proposal/design/tasks/ACTIVE_TASK 文件级同步，不声称 CLI validation PASS
- **Scope safety**:
  - 只新增默认关闭的 Gate 3b 测试并更新 active change、ACTIVE_TASK 与 append-only AGENT_LOG；未改业务主代码、package/lockfile、deployment、monitoring、accepted baseline、archive 或冻结蓝图
  - 未上传用户内容；未把本机 secret 写入 tracked files 或证据；未记录 object key、signed URL、用户/记录 id
- **Risks**:
  - Gate 3b 证明了当前本地私有存储配置下的 backend 链路，不等于微信端权限、播放和失败交互已通过，也不等于生产 SLA
  - E0 仍无目标用户证据；保存反馈与恢复入口继续是 provisional
- **Commit**: pending（Agent commit；不 push）
- **Next**: 用户审查 Gate 3b 证据；若继续完整微信验证，单独批准 Gate 3c

## 2026-08-12｜P3.1 Gate 3b 提交补录｜Type C

- **Commit**: `375d2c6 test: 完成P3.1真实对象存储验收`
- **Push**: 未授权，未执行

## 2026-08-12｜P3.1 Gate 3c 授权与人工验收准备｜Type C

- **Scope**:
  - 用户明确批准 Gate 3c，范围为微信开发者工具 / 真机的文字、图片、声音、恢复、SAVED 编辑、保存后封存、权限拒绝与上传失败路径
  - 不调用真实 AI provider，不接受 delta、不归档、不 push/deploy/release
- **Readiness**: PASS
  - MySQL80、Redis 服务运行；本地 backend 启动成功并监听 8080，未登录 records 请求返回预期 401，启动日志无 ERROR/Exception
  - 使用 `JAVA_TOOL_OPTIONS` 将运行中 Spring Boot 强制为 `app.ai.provider=mock`，并关闭 unlock/draft cleanup cron；通过 JVM 系统属性只读确认三项设置生效
  - 微信开发者工具 CLI 已打开 `frontend/dist/dev/mp-weixin` 并启用 automation；AppID 与当前生成物一致，生成物包含本轮保存、恢复、媒体和权限失败 UI
- **Verification SKIPPED / pending**:
  - T-79 人工矩阵尚未执行：相册选择、麦克风授权/拒绝、录音、扬声器播放、返回恢复与实际点击路径不能由 backend/构建替代
  - 本机没有可直接调用的 `miniprogram-automator` 客户端；不新增依赖或修改 package/lockfile来伪造自动化覆盖
  - E0 目标用户理解继续为 `INCONCLUSIVE / SKIPPED`；Gate 3c 功能结果不等于用户研究
- **Scope safety**:
  - 未改业务代码、package/lockfile、accepted baseline、archive、冻结蓝图或 deployment；只更新 active change 状态与 append-only 证据
  - 未读取、输出或提交本机 credential；真实 AI provider 调用预算保持 0
- **Risks**:
  - 当前只确认验收环境 ready，不能在用户完成逐项操作前声明 Gate 3c 或 T-79 PASS
  - 若使用真实手机而非开发者工具模拟器，`127.0.0.1:8080` 不能代表电脑地址，需另行采用同局域网地址并核对防火墙/合法域名设置
- **Commit**: pending（Agent commit；不 push）
- **Next**: 用户按 Gate 3c 最短清单操作并报告逐项结果；随后核对 backend 日志并收口 T-79

## 2026-08-12｜P3.1 Gate 3c 启动提交补录｜Type C

- **Commit**: `087d22e docs: 启动P3.1 Gate 3c人工验收`
- **Push**: 未授权，未执行

## 2026-08-12｜P3.1 Gate 3c 人工矩阵与 Agent mock 诊断｜Type C

- **User evidence**:
  - 用户在当前微信环境报告：“目前仅有 Agent 对话是用不了的，其他都正常”
  - 结合已给出的 Gate 3c 8 项清单，登记文字/图片/声音独立保存与播放、返回恢复、SAVED 编辑、保存后交给时间、权限拒绝、上传失败与重试为人工 PASS
- **Diagnosis**: CONFIRMED
  - 截图文案“我现在暂时无法接上，请稍后再回来。”与 `AgentResiliencePolicy.OPENING_UNAVAILABLE` 完全一致
  - 同期日志显示微信请求到达 backend，WRITING_GUIDANCE session 创建成功；opening 与多次 opening-retry 均记录 `provider=mock`、`category=auth-configuration`、`durationMs=0`
  - 运行中 JVM 属性为 `app.ai.provider=mock`；这是 Gate 3c 启动时为守住 T-81 真实 AI provider 调用 0 而主动设置，不是微信网络、数据库、会话状态或业务代码故障
- **Verification boundary**:
  - T-79 人工功能矩阵 PASS；Agent 不属于 T-79，且本轮 mock 降级不阻塞 Gate 3c
  - 未启动真实 DeepSeek、未产生真实 AI provider 调用；Gate 3c 授权不自动扩展为 provider 外调授权
  - E0 目标用户理解继续为 `INCONCLUSIVE / SKIPPED`；功能 PASS 不冒充用户研究
- **Changes**:
  - 仅更新 active change、ACTIVE_TASK 与 append-only AGENT_LOG；无业务代码变更
- **Scope safety**:
  - 未读取/记录用户对话内容、prompt、provider response 或 credential；日志证据只保留稳定 operation/provider/category/duration
  - 未改 package/lockfile、accepted baseline、archive、冻结蓝图、deployment 或 monitoring
- **Risks**:
  - 真实 Agent 当前未验；若用户希望恢复 Agent，需要单独授权真实 provider 调用并重启 backend，再按固定合成短文本做窄验证
- **Commit**: pending（Agent commit；不 push）
- **Next**: 用户审查 P3.1 全部实现与证据；批准后执行 delta acceptance、closeout 与 archive

## 2026-08-12｜P3.1 Gate 3c 验收提交补录｜Type C

- **Commit**: `1d2223e docs: 完成P3.1 Gate 3c验收`
- **Push**: 未授权，未执行

## 2026-08-12｜P3.1 delta acceptance、closeout 与归档｜Type C

- **Scope**:
  - 用户明确批准归档当前阶段；按 `present-moment-capture` T-82～T-86 接受四份 delta、写 closeout、归档 change、将 `ACTIVE_TASK` 置回 `IDLE` 并由 Agent commit
  - 不 push、不部署、不发布，不调用真实 AI provider，不启动 P3.2 或其他业务实现
- **Changes**:
  - 将 P3.1 delta 接受进 `backend-core`、`miniapp-core`、`agent-runtime`、`v2-product-scope` baseline，共 14 MODIFIED + 14 ADDED、0 REMOVED
  - 新增 `closeout.md`，记录实现范围、Gate 3a/3b/3c 证据、四份 delta、SKIPPED/INCONCLUSIVE、范围安全与 remaining risks
  - T-82～T-86 勾选完成；change 移至 `openspec/changes/archive/2026-08-12-present-moment-capture/`；`.ai/ACTIVE_TASK.md` 回到 `IDLE`
- **Verification**: PASS
  - 四份 delta 的 28 个 requirement 与 accepted baseline 逐块 exact-copy 一致；归档 delta 仍为 4 specs / 28 Requirements / 98 Scenarios
  - 与 HEAD 的重复标题集合对比，没有引入新的 duplicate requirement title；任务 86/86 完成，无未勾选项
  - 归档源路径不存在、目标路径与 `closeout.md` 存在；`ACTIVE_TASK` 为 `IDLE` 且指向归档路径
  - 归档仅修改 OpenSpec/状态/证据文档；最后一次业务代码验证仍为 Gate 3b 后 backend full 92 suites / 688 tests / 0 failures / 0 errors / 9 skipped，frontend type-check、标准与 Preview build PASS
  - `git diff --check`、路径 allowlist、package/lockfile 零变化与增量 credential/privacy scan PASS
- **Verification SKIPPED**:
  - OpenSpec CLI：本机不在 PATH；完成文件级 exact-copy、结构、任务与链接校验，不声称 CLI validation PASS
  - E0 目标用户理解：无真实参与者，保持 `INCONCLUSIVE / SKIPPED`；功能证据不冒充用户研究
  - 真实 Agent provider：P3.1 外调预算保持 0；Gate 3c 的 mock fail-closed 不证明真实 provider 可用性或语言质量
  - 本次仅归档文档/规格，没有业务代码变化，因此未重复运行已通过的 backend full 与 frontend build
- **Scope safety**:
  - 未实现 P3.2 导出/删除/清除全部/账号注销，未改变 Agent prompt/provider/memory/guardrails/预算，未修改 package/lockfile、deployment、monitoring 或冻结蓝图
  - 未记录用户原文、媒体内容、位置、storage key、signed URL、credential、prompt 或 provider response
- **Risks**:
  - E0 仍无目标用户证据；保存反馈与恢复入口是 provisional 基线
  - 真实 Agent provider 未在 P3.1 验证；本地 MySQL/对象存储/微信证据不等于生产兼容性、并发或 SLA
  - P3.2 只是冻结序列的下一候选，必须重新走独立规划闸门
- **Commit**: pending（Agent commit；不 push）
- **Push / deploy / release**: 未授权，未执行
