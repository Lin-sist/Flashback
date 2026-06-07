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
