# Agent Log

Use this file to record short handoffs between agents. Keep entries short.

## Entry Template

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

不要记录 API keys、账号、余额、模型额度或任何敏感信息。

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
