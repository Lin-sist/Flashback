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

