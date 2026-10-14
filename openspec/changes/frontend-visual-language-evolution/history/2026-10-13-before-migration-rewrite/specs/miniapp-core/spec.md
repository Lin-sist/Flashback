# Miniapp Core Delta：Frontend Visual Language Evolution

## ADDED Requirements

### Requirement: Visual Direction Must Be Selected Through Comparable Prototypes / 视觉方向 MUST 通过可比较原型选择

在生产界面实现前，小程序视觉演进 MUST 使用相同信息架构、相同合成内容和相同页面切片比较多个候选方向，并由产品负责人明确锁定一个主方向。候选方向 MUST NOT 作为多套生产主题同时上线。

#### Scenario: Candidate directions are compared / 比较候选方向

- **WHEN** 视觉方向进入评审
- **THEN** 每个方向 SHALL 包含首页、记录编辑器、时光轴的对应关键帧
- **AND** 每个方向 SHALL 使用相同合成内容、相同业务状态和相同主次动作
- **AND** 评审 SHALL 能区分视觉语言差异而不是数据或功能差异

#### Scenario: Production implementation is requested / 请求生产实现

- **WHEN** 产品负责人尚未锁定一个主方向
- **THEN** 小程序业务代码 SHALL NOT 开始视觉实现
- **AND** 系统 SHALL NOT 把全部候选方向实现为生产主题切换器

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

demo的look/writing MAY 显式加载信纸开启和玻璃反馈样本；不得推广到其它页面或真实小程序。内容仍为纸面，玻璃只用于动作；不得引入持续渲染、WebGL、新依赖或后台调用。

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

### Requirement: Login Demo Entry Must Preserve Authentication Truth / 登录演示 MUST 保留认证事实（2026-10-13 规划草案，未批准/未接受进baseline）

demo MAY 新增纸面为主、玻璃为辅的登录视觉表面和一次薄雾入场；MUST 明确不验证账号的本地预览，不调用正式认证/注册/微信/SMS，不改变业务权限或既有sessionStorage文字演示。

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

