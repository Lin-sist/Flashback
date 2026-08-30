# Tasks：Frontend Visual Language Evolution

> 当前阶段：用户已于 2026-08-30 批准 Gate 1 规划工件与 Gate 2 实现授权。Open Design 本地运行时/MCP 已可调用，Local Codex 递归加载 Open Design MCP 导致的桌面接管/EPIPE 冲突已做本地隔离修复；主视觉方向尚未锁定，在新任务完成原型并选择方向前不开始业务代码实现。Open Design Cloud 不在本轮范围；commit、push、deploy、release、delta acceptance 或 archive 未授权。

## 阶段 0：Readiness 与范围锁定

- [x] **T-01** 读取 AGENTS、ACTIVE_TASK、Type C checklist、产品宪章、冻结蓝图和 accepted specs
- [x] **T-02** 确认 `ACTIVE_TASK=IDLE`、当前分支 `main...origin/main [ahead 1]` 且工作树无本轮改动
- [x] **T-03** 审计既有 `m1-frontend-visual-foundation` 与 `Docs/design/home-v2/`
- [x] **T-04** 确认本次多风格探索与 M1 单一高保真合同冲突，因此创建后继 change，不回改 M1
- [x] **T-05** 规划期不修改业务代码、package/lockfile、backend、Agent、部署与 accepted baseline
- [x] **T-06** 规划期外部内容仅为公开设计页面；不外发真实用户内容或 secret
- [x] **T-07** OpenSpec CLI 不在 PATH，记录 CLI validation `SKIPPED`，采用文件级 scaffold

## 阶段 1：研究与候选方向

- [x] **T-08** 使用 agent-reach/Jina 读取 Inspora 与 Awwwards；Inspora 动态画廊受 Vercel checkpoint 限制，保留未逐项核验边界
- [x] **T-09** 整理 Awwwards Diary、Elektra、History Scroll、Aerleum、Charles Leclerc、Repeat 的可迁移动效机制
- [x] **T-10** 定义四套方向：纸上留白、私人馆藏、生命剪贴、暮色回声
- [x] **T-11** 定义共同原型切片：主页、记录编辑器、时光轴
- [x] **T-12** 定义 L0–L3 motion levels、减少动态、静态降级与禁止项

## 阶段 2：Open Design 原型准备

- [x] **T-13** 打开 Open Design 官方下载页；当前 Codex 任务仍未发现 `open-design` MCP
- [x] **T-14** 创建只含合成内容与公开参考的 `OPENDESIGN_BRIEF.md`
- [x] **T-15a** 用户完成 Open Design 0.21.0 安装、登录、Codex 5.6 Sol 配置与全局 MCP 注册
- [x] **T-15b** 启动一个新 Codex 任务，验证新快照可调用 `open-design` MCP
- [x] **T-16** 用户确认 Open Design brief：移动应用/小程序应用、一个核心流程、精致产品界面；本轮明确使用 Local Codex，不使用 Open Design Cloud account/credits
- [ ] **T-17** 分别生成四个方向的首页/编辑器/时光轴关键帧；每个方向使用独立逻辑生成
  - 2026-08-30：“纸上留白”Local Codex 单次生成在运行约 48 秒后因 Open Design 本地运行时关闭而 `CANCELED`；项目文件为 0，未形成关键帧或 Preview/Studio 交付，T-17 保持未完成
  - 2026-08-30：已确认取消链路为 Open Design 子 Codex 递归加载全局 `open-design` MCP，触发第二个非 headless 桌面接管并关闭原守护进程；现已为 Open Design 的 Local Codex 配置专用 `CODEX_BIN` 包装器，只在子进程禁用该 MCP。无模型 `/api/agents` 检测通过且未新增桌面进程/EPIPE；完整生成回归仍待新任务
  - 2026-08-30：用户明确要求重试后，新建独立项目并启动一次新的 Local Codex 逻辑生成；run 约 6 秒后以 `DAEMON_RESTARTED` 失败，项目文件 0、`entryFile=null`、无 Preview/Studio 链接。T-17/T-18/T-19 继续 pending；不自动发起第三次生成
- [ ] **T-18** 为每个方向生成或说明保存仪式、时间轴推进两项核心动效及静态降级
- [ ] **T-19** 记录生成链接、版本与证据边界；不得把生成稿写成已实现界面

## 阶段 3：方向评审与 Gate 1

- [ ] **T-20** 用产品气质/表达清晰/阅读无障碍/微信可行性/辨识度矩阵评审四个方向
- [ ] **T-21** 产品负责人选择一个主方向，并明确保留、删除、融合元素
- [ ] **T-22** 冻结色彩、字体、空间、材质、组件和 motion levels；未选方向转为只读探索证据
- [ ] **T-23** 更新 design.md 决策记录与 Docs/design 方向结论
- [x] **T-24a GATE 1** 用户批准 proposal/design/tasks/spec deltas
- [ ] **T-24b GATE 1** 用户在四方向可比原型后批准选定方向

## 阶段 4：Gate 2 实现（未授权）

- [x] **T-25 GATE 2** 用户明确授权按最终选定方向修改前端业务代码；授权已取得，执行须等待 T-24b
- [ ] **T-26** 记录实现 allowlist/denylist 与开工 Git 锚点
- [ ] **T-27** 运行 frontend type-check、Standard/Preview build baseline
- [ ] **T-28** 建立稳定 visual/motion tokens，不新增生产主题切换器
- [ ] **T-29** 实现首页样板，保持“留下此刻”主动作与 Preview 边界
- [ ] **T-30** 实现编辑器样板，保持内容/媒体/见证者/保存业务契约
- [ ] **T-31** 实现时光轴样板，保持记录/篇章/状态和分页契约
- [ ] **T-32** 三个样板稳定后抽取共用页面壳层、底部导航与展示组件
- [ ] **T-33** 分批迁移我的记录、详情、时间篇章、个人中心和登录页；每批独立验证

## 阶段 5：验证（需后续授权与环境）

- [ ] **T-34** type-check、Standard/Preview mp-weixin build、`git diff --check`
- [ ] **T-35** 三个一级 Tab、canonical naming、路由、真实/Preview 隔离和 mutation fail-closed 回归
- [ ] **T-36** 微信开发者工具覆盖安全区、长内容、键盘、滚动、加载/空/失败、减少动态和低动态降级
- [ ] **T-37** 视觉对照：首页、编辑器、时光轴及分批迁移页面
- [ ] **T-38** 性能边界：无 WebGL/自动播放视频/滚动劫持；动画禁用或掉帧不影响操作
- [ ] **T-39** 物理真机有设备才执行，否则明确 `SKIPPED`，不得以开发者工具替代
- [ ] **T-40** package/lockfile、backend、Agent、deployment、monitoring、admin、accepted specs denylist 检查

## 阶段 6：接受与归档（未授权）

- [ ] **T-41** 用户审阅实现与验证证据
- [ ] **T-42** 单独授权后接受 delta、写 closeout、归档 change 并恢复 `ACTIVE_TASK=IDLE`
- [ ] **T-43** commit/push/PR/deploy/release 分别授权；默认用户手动提交
