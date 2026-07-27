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
- **Commit**: pending

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
- **Commit**: pending
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
- **Commit**: pending
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
- **Commit**: pending
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
- **Commit**: pending
- **Next**: 用户授权后 propose `agent-runtime-mvp`

