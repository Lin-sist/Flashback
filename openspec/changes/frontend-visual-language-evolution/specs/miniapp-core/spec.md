# Miniapp Core Delta：正式前端渐进迁移

> 2026-10-13 当前规划：用户确认收口方向；新增正式迁移完整 delta 待 Gate 1 审阅，未接受进 baseline。重写前四方向要求见 history/2026-10-13-before-migration-rewrite/specs/miniapp-core/spec.md；以下 demo 契约保留原批准范围，不表示新视觉/设备验收已通过。

## ADDED Requirements

### Requirement: Visual Direction Must Follow The Latest Explicit Decision / 视觉方向 MUST 服从最新明确裁决

小程序视觉演进 MUST 只采用产品负责人明确锁定的信笺 × 微液态玻璃主方向。历史候选比较及未完成证据 SHALL 保留；被用户明确停止的四方向比较 SHALL NOT 继续作为当前迁移前置条件，亦不得伪写为已完成。候选 SHALL NOT 同时成为多套正式主题。

#### Scenario: The product owner closes the exploration stage / 产品负责人收口探索阶段

- **GIVEN** 用户已确认当前单方向、暂停登录 HTML 与翻页推广，并仅保留回弹候选
- **WHEN** 正式迁移规划更新
- **THEN** 当前任务 SHALL 明确这些范围与未验收证据
- **AND** 重写前工件 SHALL 作为历史保留，旧待办 SHALL NOT 自动勾选

#### Scenario: Formal implementation is requested / 请求正式实现

- **WHEN** 本次完整正式迁移工件尚未审阅或新任务实现授权未明确
- **THEN** SHALL NOT 开始正式业务代码修改
- **AND** 历史授权、文档提交或 demo 实施 SHALL NOT 替代当前两道闸门

### Requirement: Demo Surfaces Must Map To Existing Formal States / demo 表面 MUST 映射至既有正式状态

正式迁移 SHALL 以 design.md 的页面/状态映射为范围；saved SHALL 承接现有编辑器成功态，look/sealed SHALL 承接详情状态，Agent 与近况结果 SHALL 承接既有组件/页面状态。正式路径 SHALL 保留已有服务/store/types/权限与真实响应语义，不导入演示存储或 DOM 脚本。

#### Scenario: A saved page is translated / 转译保存反馈

- **WHEN** 用户在正式编辑器请求保存
- **THEN** 确认反馈 SHALL 由现有真实保存响应触发，失败 SHALL 保留输入及可见错误/重试路径
- **AND** SHALL NOT 以动画结束、本地写入或静态样本冒充真实保存
- **AND** SHALL NOT 仅为复制 demo 页面而新增保存路由或改变记录状态契约

#### Scenario: Existing state semantics cannot fit the visual reference / 视觉参照与既有语义冲突

- **WHEN** 映射需要改变路由、前端可见业务状态、API/DTO/权限或持久化语义
- **THEN** 对应实施 SHALL 停止并回规划确认
- **AND** SHALL NOT 以样式精修名义绕过契约确认

### Requirement: Formal Migration Must Advance In Verified Slices / 正式迁移 MUST 按已验证切片推进

正式迁移 SHALL 先做首页/编辑器/时光轴样板及保存/时间回看闭环，再分批扩展 C–E。每批 SHALL 明确文件范围、状态覆盖、验证证据、回退点和剩余风险；SHALL NOT 一次性重构全站。当前 SHALL NOT 迁移翻页、登录新视觉/薄雾或引入依赖。

#### Scenario: A batch is ready to expand / 拟扩大迁移批次

- **WHEN** 当前样板拟扩展至其它页面
- **THEN** SHALL 先记录 type-check、Standard/Preview 构建、实际微信画面/交互与范围检查
- **AND** loading/empty/error/busy/封存/只读/Preview 语义与无动画终态 SHALL 保持完整
- **AND** 未取得平台证据 SHALL 保留 SKIPPED/风险，不把样板标为验收通过

#### Scenario: Glass support or motion is unavailable / 玻璃支持或动画不可用

- **WHEN** 背景模糊/纹理不支持、动画关闭、中断或性能不符合可用性要求
- **THEN** SHALL 提供实色/静态可读终态与即时可操作控件
- **AND** 玻璃回弹 SHALL 仅是可选增强，不阻断保存、输入、滚动或导航
- **AND** SHALL NOT 宣称 demo 效果已经证明微信/真机支持或固定帧率

### Requirement: Motion Must Preserve Meaning Without Motion / 动效 MUST 在无动画时保留完整语义

所有新增动效 MUST 服务表达确认、时间定位或层级理解；动画被关闭、减少、跳过或运行失败时，内容、状态和操作 MUST 保持完整可理解与可使用。

#### Scenario: Reduced motion is active / 减少动态效果生效

- **GIVEN** 用户选择减少动态或运行环境采用低动态降级
- **WHEN** 页面展示保存、时间节点、图片或列表状态
- **THEN** 页面 SHALL 去除非必要位移、堆叠、视差和逐步绘制
- **AND** SHALL 通过静态布局、即时颜色、文本或图标提供等价状态

#### Scenario: Motion fails or is skipped / 动画失败或被跳过

- **WHEN** 动画未执行、掉帧、被平台限制或用户快速离开动画阶段
- **THEN** 保存成功/失败、SEALED/UNLOCKED、Preview、加载和错误状态 SHALL 仍清晰可见
- **AND** 主要动作 SHALL 仍可立即操作

### Requirement: Mini Program Motion Must Use Progressive Enhancement / 小程序动效 MUST 渐进增强

第一版生产动效 SHALL 优先使用微信小程序稳定支持的 opacity、transform、color 与简单遮罩/展开能力。WebGL、3D 场景、自动播放视频、鼠标 hover、强滚动劫持和依赖重型 filter 的效果 SHALL NOT 成为核心路径依赖。

#### Scenario: A web inspiration effect is adapted / 转译网页灵感动效

- **WHEN** Inspora、Awwwards 或其它网页案例包含 3D、液体、hover、视差或跨页转场
- **THEN** 实现 SHALL 提取其时间层次、揭示或聚焦意图
- **AND** SHALL 使用小程序可降级的轻量机制重新表达
- **AND** SHALL NOT 以像素级复刻为验收目标

#### Scenario: Heavy effect is proposed for the main path / 主路径拟采用重型效果

- **WHEN** 效果需要 WebGL、自动播放视频、滚动劫持或持续高频动画才能成立
- **THEN** 该效果 SHALL 保持为原型观察项或被拒绝
- **AND** “留下此刻 → 保存 → 结束”主路径 SHALL 不依赖该效果

### Requirement: Visual Evolution Must Preserve Existing Truth And Navigation / 视觉演进 MUST 保留既有事实与导航边界

视觉层 SHALL 保持首页、时光轴、个人中心三个一级 Tab，“我的记录”“时光轴”“时间回看”用户可见命名，authenticated/Preview 隔离，以及既有真实业务状态和失败语义。

#### Scenario: Visual prototype or implementation changes page composition / 原型或实现改变页面构图

- **WHEN** 页面布局、卡片、材质、导航呈现或动效发生变化
- **THEN** 页面 SHALL 仍保留三个一级 Tab 与 canonical naming
- **AND** Preview SHALL 继续明确只读并在任何 mutation 前 fail-closed
- **AND** 视觉反馈 SHALL NOT 把 mock、本地状态或动画完成冒充真实后端成功

### Requirement: Interactive Demo Must Remain A Separate Local Exercise / 可交互 demo MUST 保持独立本地练习（2026-10-13 已批准 demo 增补，未接受进 baseline）

`demo/` 的文字演示 MUST 与 authenticated real path 及只读 production Preview 分离，不导入真实业务服务、不放开 Preview mutation。演示 MAY 在同源同标签页 sessionStorage 中保存测试草稿与一条最新演示记录；MUST 明确本地演示、不上传与保留范围。sessionStorage 的通常关闭行为 MUST NOT 被宣传成不可恢复擦除，MUST 提供仅作用于自身 key 的清除入口。

#### Scenario: User saves test text / 保存测试文字

- **GIVEN** 用户处于明确标注的本地 demo 表面
- **WHEN** 用户输入有效测试文字并保存
- **THEN** 演示 SHALL 在自身命名空间写入并确认记录后显示本地演示成功
- **AND** saved、timeline、look SHALL 读取同一记录，保留原文字与换行
- **AND** SHALL NOT 调用业务 API、真实 provider、对象存储或把正文写入 URL、日志、文件
- **AND** SHALL NOT 将本地写入或落印动画称为真实后端保存、封存或解锁

#### Scenario: Storage fails or dynamic record is missing / 存储失败或动态记录不存在

- **WHEN** 存储被拒绝、容量异常、结构/版本错误或动态回看记录不存在
- **THEN** 演示 SHALL 显示可理解的失败/空态与恢复入口
- **AND** 保存失败 SHALL 保留当前输入，不跳成功页、不触发成功落印
- **AND** SHALL NOT 用固定合成 fixture 冒充刚刚保存的记录
- **AND** SHALL NOT 自动改用长期 localStorage 或外部持久化

#### Scenario: User clears the exercise / 清除本轮演示

- **WHEN** 用户明确确认清除本轮演示
- **THEN** 演示 SHALL 只删除自有 key/演示记录
- **AND** SHALL NOT 清空其它同源存储、操作真实数据或触发所有权清理 API
- **AND** 后退/前进恢复的页面 SHALL 重新读取演示状态，不继续展示已删除内容

#### Scenario: Demo visual refinement is rolled out / 推广 demo 精修

- **WHEN** 样板参数进入共享样式
- **THEN** 首轮 SHALL 精修 home、writing、saved、timeline、look，并回归其它九页
- **AND** SHALL 保留现有信笺方向、三个一级 Tab、内容纸面与轻量动作玻璃
- **AND** 渲染证据缺失时 SHALL 如实保留 SKIPPED，不以源码或合成测试替代视觉定稿

### Requirement: Demo Motion Samples Must Be Optional And Local / 动效样本 MUST 局部且可跳过（2026-10-13 两页增补，未接受进 baseline）

demo 的 look/writing MAY 保留已实施的信纸开启和玻璃反馈样本作为历史试验；当前 SHALL NOT 推广信纸开启，玻璃回弹仅保留候选；不得推广到其它页面或真实小程序。内容仍为纸面，玻璃只用于动作；不得引入持续渲染、WebGL、新依赖或后台调用。

#### Scenario: Read a letter with optional opening / 可选信纸开启

- **WHEN** 可见回看信卡启用3D且用户允许动态
- **THEN** 主信纸 MAY 一次620ms双面开启，并提供主动重播
- **AND** 正文 SHALL 保持单一DOM、原文与自然纵向滚动，装饰 SHALL 不镜像正文、不接管手势、不参与辅助技术阅读
- **AND** 减动态、无脚本、无3D、超过620px高、离页或中断时 SHALL 直接显示可读终态
- **AND** 重播、BFCache、偏好切换 SHALL 清理计时器/瞬时状态，历史恢复不自动重复播放

#### Scenario: Touch a glass action / 玻璃动作反馈

- **WHEN** 用户按压两页opt-in药丸
- **THEN** 控件 MAY 轻压、释放后一次恢复，边缘光 MAY 响应触点
- **AND** SHALL NOT 拦截原生点击/输入/组合输入或延迟保存；成功仍依赖既有写入读回确认
- **AND** 减动态 SHALL 停止形变，无滤镜或减透明度 SHALL 保留实色可操作控件
- **AND** CSS模糊/高光 SHALL NOT 宣称为已验证实体折射或跨平台等价实现

### Requirement: Login Demo Entry Must Preserve Authentication Truth / 登录演示 MUST 保留认证事实（历史草案，当前暂停实施）

本项仅保留历史草案，当前 SHALL NOT 新增登录 HTML 或薄雾接线；以后若明确重新纳入，经完整规划/实现闸批准的 demo MAY 提供纸面为主、玻璃为辅的登录视觉表面和一次薄雾入场；MUST 明确不验证账号的本地预览，不调用正式认证/注册/微信/SMS，不改变业务权限或既有sessionStorage文字演示。

#### Scenario: Enter clearly labelled demo preview / 进入明确演示预览

- **GIVEN** 登录表面明确不验证账号、仅测试输入且禁止使用真实密码
- **WHEN** 非空测试输入通过演示检查并确认进入概念预览
- **THEN** 页面 MAY 从按钮处扩散极淡灰粉雾、退为暖纸白、短暂留白后进入首页
- **AND** SHALL NOT 将进入预览或动画完成称为真实登录成功
- **AND** 账号/密码 SHALL NOT 进入storage、URL、文件、日志或网络；只允许固定无敏感入场标记
- **AND** 注册/微信展示入口 SHALL 明确未接通，不伪造认证或授权成功

#### Scenario: Invalid input, reduced motion or interruption / 失败与减少动态及中断

- **WHEN** 输入无效、用户减少动态、素材缺失、离页/后台/BFCache或动画中断
- **THEN** 无效输入 SHALL 保留可见表单提示，不扩散或进入首页
- **AND** 其余状态 SHALL 有清理与静态可用终态，减少动态 SHALL 直接进入已确认的预览
- **AND** 重复点击 SHALL NOT 重复进入，历史恢复/刷新 SHALL NOT 残留雾层或自动反复入场
- **AND** 本地file捕获策略拒绝时 SHALL 如实保留渲染SKIPPED，不用合成状态宣称转场顺畅

