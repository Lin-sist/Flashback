# Proposal：Frontend Visual Language Evolution

## 当前状态与授权（2026-10-13）

2026-10-14 实施更新：用户确认已完整阅读本规划并明确批准后续实现；Gate 1 规划批准和 Gate 2 实现授权分别登记为通过。本批从 clean HEAD 4a5acfb 启动 A 批首页/编辑器/时光轴展示样板，用户同时授权 Agent 简短本地 commit。平台验收未完成不扩大 C–E；真实业务外调预算仍为 0。以下 2026-10-13 描述保留为规划形成时的状态，当前执行状态以 tasks 与 implementation-evidence.md 为准。

唯一 active change 保持 `frontend-visual-language-evolution`。用户已批准按当前状态收口、重写正式迁移阶段，并明确授权 Agent 按历史修改分组进行本地 Git 提交。此次授权覆盖文档和历史成果入库；不启动正式前端实现，不接受 delta、不归档，不含 push/PR/部署/发布或真实外调。

重写前 proposal/design/tasks/delta 完整保存在 `history/2026-10-13-before-migration-rewrite/`；AGENT_LOG 历史只追加。旧 T-24a/T-25 授权保留为历史，不将旧四方向闸门或 demo R/M 授权自动当成本次具体正式迁移任务的放行。

## Why Now / 改前与改后

当前 demo 已有十四页信笺原型，五页接通了隔离的 sessionStorage 文字练习；正式前端已有 Uniapp 页面、认证、记录服务、媒体与位置、篇章、数据所有权和只读 Preview 接线。两者尚未建立经过验收的完整映射；把 HTML、演示存储或静态样本直接搬入正式页面会丢失业务状态与失败语义。

计划把「纸为内容、微液态玻璃为动作、霜为封存时间」转译为现有微信小程序的展示层。迁移后用户仍沿现有认证、留下此刻、保存、时光轴与时间回看路径操作；服务响应决定成功，动画只表达已经确认的状态。

## Goals / 已确认方向与拟定工程目标

- 主方向沿用 2026-09-06 用户裁决的信笺 × 微液态玻璃；停止四方向重选，保持三个一级 Tab 与 canonical naming。
- 以首页、编辑器、时光轴三个样板页为首批，保存反馈与时间回看组成核心闭环；样板稳定后分批收口公共展示组件和其余核心页面。
- 保留 Pinia/services/types、真实业务状态、既有路由与 Preview fail-closed，不做一次性 major frontend visual reconstruction。
- 每批有文件 allowlist、状态覆盖、静态降级、源码/编译/微信开发者工具证据与独立回退点；真机证据另列。

## Scope

### 当前收口（用户已授权）

- 保存旧工件快照，重写正式迁移的 proposal/design/tasks/miniapp delta，更新 ACTIVE_TASK、追加 AGENT_LOG。
- demo 停在当前十四页版本；保留五页文字演示与两页动效样本代码作为研究证据，不继续精修。
- 登录 HTML 原型暂不实现；保留线稿方向和 LOGIN_PLAN 历史，L 系列任务暂停。
- 翻页因用户反馈不流畅暂停推广，不删除既有局部样本；玻璃回弹保留待验证候选。
- 既有 PROJECT_REPORT.md、UI_DESIGN_CONTEXT.md 原文入库为历史报告/设计参考；不把其测试数量和能力声明当成本轮重新验收。

### 后续正式迁移（完整规划与实现闸待审）

- 材质、排版、间距、动作按钮、状态反馈与轻量动效；现有安全区与导航基础上的页面模板/样式。
- 首页、编辑器及其保存成功态、时光轴、详情的 SAVED/SEALED/UNLOCKED 展示；后续我的记录、篇章、个人中心、数据与所有权、近况结果和 Agent 浮层的展示收口。
- 微信小程序为唯一工程验收目标；H5/浏览器 demo 作为视觉参照，不能替代目标平台验证。
- 具体页面对应关系、文件范围、分批退出条件见 design.md；逐项执行入口为 tasks.md 的 F 系列。

### Non-goals

- 本轮不修改 frontend/backend；后续不改变 API/DTO/枚举、权限、持久化、封存、附件/位置/封面、Agent/runtime/memory/tools/guardrails 或数据所有权契约。
- 不把 demo sessionStorage/DOM 脚本引入正式前端，不放开 Preview mutation，不用 mock/local success 冒充后端成功。
- 不新增保存页/浮层/整理结果路由；现有页面状态与组件承接。若确需新路由或业务状态，先回规划闸。
- 暂不实现 demo 登录 HTML；正式登录保留既有认证和展示，新登录视觉/薄雾后续另行确认，不阻塞核心迁移。
- 不推广翻页，不引入软页引擎、WebGL、3D 场景、重滤镜依赖、自动播放、滚动劫持、字体外链或新主题切换器。
- 不改 package/lockfile、deployment/monitoring/admin/SMS/生产通知/设置页；不改 accepted baseline、旧 change 或 archive。
- 不外发或入库真实日记、账号、token、密码、位置、用户媒体或本机敏感参考截图。

## 能力五态（截至本次源码检查）

| 能力 | 状态 | 证据与边界 |
|---|---|---|
| 十四页 demo、五页文字演示、两页动效代码 | confirmed | 文件存在；实际本轮全页渲染/输入/动效性能尚未验收 |
| demo 视觉定稿与流畅度 | partial | 历史静态证据不覆盖最新动态版；翻页负反馈已确认，玻璃仅候选 |
| 正式页面、服务/store/类型、导航与 Preview 拒绝写入接线 | confirmed | 源码检查；不表示本轮真实后端/设备测试已通过 |
| 当前前端类型检查 | confirmed | 2026-10-13 用可用 Node 执行项目 vue-tsc --noEmit 成功 |
| 新视觉在正式微信页面上的表现与 Standard/Preview 构建 | unknown | 本轮尚未取得新构建/平台绘制/键盘/真机证据 |
| 渐进迁移、组件收口与完整回归 | planned | 本工件中的未来 F 任务，未实施 |
| 新登录 HTML、翻页推广、业务契约重写、依赖/发布 | out_of_scope | 用户收口决定和仓库边界 |

## Success Criteria

- 完成逐页/逐状态映射，实际数据只来自既有服务；加载/空/失败/过期/封存/只读状态不因新构图丢失。
- 首页主动作、编辑保存与时间回看闭环在动画禁用时完整可用；成功反馈在真实响应后出现，失败留在可重试页面。
- 三个样板先验收后扩展，每批保持安全区、滚动末项可达、长内容、字体回退、触摸/键盘及静态材质可读。
- Standard/Preview type-check/build、Preview 写入拒绝和路由/命名回归通过；微信与物理真机证据分别记录。
- 受保护业务服务/类型、后端、依赖与 accepted baseline 无范围外改动；没有未授权 provider/对象存储调用。

## Gates / 提交与外调

- 已确认：当前设计阶段停止、单方向与动效取舍、同一 change 内文档重写、本地分组提交；提交责任改为本轮 Agent 提交。
- Gate 1：2026-10-14 用户明确已完整阅读并批准当前正式迁移规划，通过；未接受 delta 进 baseline。
- Gate 2：2026-10-14 用户明确允许按当前 F 任务实现，通过；仍遵守样板/批次验收顺序。
- Gate 3：本轮真实业务 API/provider/对象存储调用预算 0；未来如需联调，单独明确合成内容、调用预算与环境。push/PR/部署/发布均未授权。
- dirty 工作区按历史分组提交，随后记录 clean HEAD 为规划基线；未来实施重新建立开工锚点与逐批回退点。
