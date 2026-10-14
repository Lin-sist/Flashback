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

## 2026-10-13 增补：五核心页 demo 精修（已授权实施，视觉验收未完成）

> 本批唯一执行说明为 `demo/REFINEMENT_PLAN.md` v1.0。主方向依最新裁决为信笺纸×微液态玻璃；旧四方向任务保持历史未完成状态，不因本增补伪勾。本批只针对 demo，不启动 T-26—T-43 的正式前端迁移。

- [x] **R-01** 读取当前指南/源码/active change 与相关日志；公开官方资料研究；用户确认五页＋九页回归、接通文字演示、sessionStorage；形成规划增补
- [x] **R-02 GATE 1** 用户批准本规划、proposal/design/tasks/miniapp delta 的新增 demo 契约与候选参数
  - 2026-10-13：用户在完整规划交付后明确「可以开始在demo实现了」，接受该具体规划；不继承生产/外调授权。
- [x] **R-03 GATE 2** 用户另行明确允许按 R-04—R-20 实现；不能将 R-02 默认勾成 R-03
  - 2026-10-13：同条用户指令明确授权在 demo 实施；提交仍默认用户手动。
- [x] **R-04** 记录开工 HEAD/dirty 状态、allowlist 文件哈希与基线；保留已有用户改动
  - HEAD b6afb01；保留前轮规划 diff 与两份既有 untracked 文档；基线哈希另存本地验证目录，用户已自行备份 demo。
- [ ] **R-05** 获得允许环境中的五页稳定截图、字体状态与九页基线；策略阻断则如实 SKIPPED，不能绕过或称视觉过审
- [ ] **R-06** 首页/书写局部比较字色、字重、纸纹与药丸；确定最终参数，不重开品牌方向
- [x] **R-07** 五页渐进页面壳层、滚动与安全区；九页默认不改 HTML
- [x] **R-08** 精修 home/writing 样板，保持结构/宋体/朱砂/三 Tab；增强语义控件与点击区
- [ ] **R-09** 收口共享 tokens/components 参数，九页初回归；失败先限定选择器
- [x] **R-10** 实现 demo-store：版本/单草稿/最新记录/回执、结构校验、异常与自有 key 清除；合成逻辑测试
- [x] **R-11** 实现 writing 原生输入、中文 IME、恢复、写入读回与失败保留；无真实请求
- [x] **R-12** 实现 saved/timeline/look 同一条动态演示记录与空态；textContent、防重复与 BFCache 恢复
- [x] **R-13** home/index 演示入口说明与重置；只删除演示数据，未接通控件明确展示状态
- [x] **R-14** 精修 saved/timeline/look 阅读层次、自然照片样本、长文与末项可达
- [ ] **R-15** 四枚既有动效/按压反馈与减少动态/实色降级；动画不决定成功
- [ ] **R-16** 五页真实浏览器闭环/错误路径/刷新/回看/删除＋十四页链接与九页共享样式回归
- [ ] **R-17** 390/375/360/320、200% 放大、字体回退、无玻璃、减动态、移动键盘；区分真实与合成证据
- [x] **R-18** denylist/无正文网络日志与 URL/无长期存储/未改真实 Preview；git diff --check
- [ ] **R-19** 交付同条件前后图、实际参数、验证矩阵与 SKIPPED 原因；用户验收
- [x] **R-20** 更新实施指南/Current Progress，追加 AGENT_LOG；提交/接受 delta/归档/发布另行授权

### 2026-10-13 本批实施证据

- R-07/R-08/R-14：五页布局与排版代码已实施；视觉参数仍是暂行候选，不是已截图定稿。
- R-10—R-13：存储与页面接线完成，26项 Storage/简化 DOM 合成用例通过；不替代真实浏览器或中文输入法手验。
- R-18：15个 HTML / 124个本地资源链接静态检查、无业务网络/长期存储/正文日志、9页 HTML 与图片/既有未跟踪文档哈希保护、git diff --check 通过。
- R-05/R-06/R-09/R-16/R-17/R-19 保持 pending：无实际画面、候选对照、全页渲染、键盘/字体/玻璃降级与用户验收证据；既有 file:// 策略拒绝不能换地址/工具/服务绕过。
- R-15 保持 pending：落印只消费一次保存回执，减少动态与实色降级代码已补；融霜仍为预留样板，真实解锁不在本轮范围，实际动效未验收。
- R-20：实施指南/规划/Current Progress/AGENT_LOG 更新；没有 git add/commit/push/归档。

## 2026-10-13 动效样本任务（M，独立于R的待补视觉验收）

- [x] **M-01 GATE 1** 用户接受完整调研中的回看信纸＋玻璃药丸具体切片；登记增补范围与决策
- [x] **M-02 GATE 2** 用户明确「可以先尝试落地几页看看落实效果」，授权两页demo实施；提交仍用户手动
- [x] **M-03** 读取当前状态/两页/共享样式/存储接线；开工哈希保护既有差异；限两页显式加载
- [x] **M-04** 回看双面开启/纸边微折/移动阴影/主动重播，保留自然正文；空态/长文/无脚本/无3D静态降级
- [x] **M-05** 两页药丸轻压/一次恢复/触点光；无点击拦截或保存延迟；偏好/中断/BFCache清理
- [x] **M-06** JS合成中断/重播/偏好/长文测试、既有存储回归、静态资源与范围/哈希/diff检查
- [ ] **M-07** 允许环境的实际两页动作/观感/降级/真机；不可访问如实SKIPPED，不能绕过旧拒绝
- [x] **M-08** 指南/Current Progress/AGENT_LOG交付记录，说明未验证项；不提交/迁移/归档

本批M-01—M-06/M-08共7/8完成；M-07 SKIPPED/pending，R批7项旧视觉待补不变。21项motion/flow合成＋26项Storage回归PASS；15页HTML/128链接/CSS解析/JS语法/23个保护哈希与diff检查PASS。无实际绘制/帧率/触控/真机证据，不勾M-07或R-15—R-17。

## 2026-10-13 登录页与薄雾入场任务（L，待批准）

- [x] **L-01** 读取当前状态/登录方式/用户反馈，按原第二图生成减色/减笔触/减按钮填色修订图；形成LOGIN_PLAN与决策/delta
- [ ] **L-02 GATE 1** 用户确认修订图、LOGIN_PLAN与proposal/design/tasks/miniapp delta新增范围
- [ ] **L-03 GATE 2** 用户另行明确按L任务实施；不默认从原图偏好或R/M授权推出
- [ ] **L-04** 开工HEAD/dirty/范围哈希；保护其它页/共享样式/store/flow/图片/用户文档与日志历史
- [ ] **L-05** 生成独立场景与透明雾资产；新增login真实DOM/两字段/展示辅助入口/明确预览说明与静态降级
- [ ] **L-06** 按压→条件成立→薄雾→暖纸白→首页一次入场；不持久化/外发凭证，失败不扩散，减动态/素材失败/中断/BFCache降级
- [ ] **L-07** index入口与home最小接线，普通首页不播放新转场，不改变既有会话文字演示
- [ ] **L-08** 合成状态/异常/重复操作/参数标记与密码清理、静态资源/哈希/范围/JS语法/diff检查
- [ ] **L-09** 获准环境实际登录/跨页/小屏/键盘/偏好/连续动作验收；不可访问如实SKIPPED，不绕过旧拒绝
- [ ] **L-10** 指南/Current Progress/AGENT_LOG交付，列明图稿/合成/浏览器证据；不自动提交/迁移/接受delta/归档
