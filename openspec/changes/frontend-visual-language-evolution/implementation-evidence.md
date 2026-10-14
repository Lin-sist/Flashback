# 正式迁移实施证据：A 批样板

## 2026-10-14 授权与开工

- 用户表示已完整阅读规划，并明确批准开始后续实现；分别登记 F-10 规划批准、F-11 按任务实现授权。
- 用户明确允许 Agent 提交简短 commit；本批仅本地提交。真实业务外调预算仍为 0，不包含 push、PR、部署、接受 delta 或归档。
- clean 开工 HEAD：`4a5acfb`。本批 allowlist：三个样板页面、追加式展示 tokens、`frontend/scripts/visual-samples-offline.cjs`、当前 change 的状态/证据与 `.ai` 记录。
- 只转译 demo 构图/材质，不导入 demo-store/demo-flow/motion-samples；不新增路由、状态、API 或依赖。

## F-12 页面与状态矩阵

对照 accepted `miniapp-core` 的核心闭环/状态命名、M4 媒体与筛选、P3.1 保存/恢复/Preview、P3.2 所有权、P4.1/P4.2 Agent 确认与授权、P5 篇章要求，以及下列实际模板/服务。`recordService`、`httpClient`、`stores/record.ts` 与既有类型负责成功、失败和只读语义，展示迁移不改它们。

| demo / 批次 | 正式位置 | 必须保留的已有状态/动作 | 本批实施/证据 |
|---|---|---|---|
| home / A | `pages/home/index.vue` | 三个独立概览 loading/ready/error；SAVED/SEALED/UNLOCKED；重试、封面失败、三 Tab | 主动作、已留下入口、纸面概览；脚本合成回归 PASS，微信待验 |
| writing / A | `pages/record-editor/index.vue` | initializing/initFailed；DRAFT 恢复、SAVED；媒体 pending/AVAILABLE/失败；可选位置/封面；忙碌、防重、退出、见证者入口 | 原业务脚本字节不改；正文/动作样板；保存合成回归 PASS，键盘待验 |
| timeline / A | `pages/timeline/index.vue` | 初载/空/失败/旧数据提示；标签/年/月/日；分页失败/重试/去重；SAVED/SEALED/UNLOCKED | canonical 标题、无封面排印条目/有封面卡片、静态封存层次；合成回归 PASS，微信滚动待验 |
| saved / B | 编辑器 `savedFeedback` | 服务 save 响应后显示；失败留输入；输入变更后不保留旧成功反馈 | 保留现有条件；只提高已有静态提示可读性，F-24 完整验收待办 |
| look / B | `pages/record-detail/index.vue` SAVED/UNLOCKED | invalid ID/not found/load failed；只读媒体错误；编辑入口权限、回信/回看、来源、记忆排除 | 原样保护，F-25 待办 |
| sealed / B | 同一详情页 SEALED | 只能展示既有允许字段；不得改位置/附件/封面；未抵达不开放回看正文 | 原样保护，静态霜不能替代权限 |
| records / C | `pages/record-list/index.vue` | loading/empty/error/stale；筛选分页、计数、记录/篇章切换、主动选择成员 | 原样保护，F-30 待办 |
| chapter / C | `pages/time-chapter-detail/index.vue` | 加载/失败/空成员/分页；ACTIVE/ENDED；显式结束/重开、版本冲突、删除不删记录 | 原样保护，F-30 待办 |
| user-center / D | `pages/user-center/index.vue` | 初载/失败/旧数据、身份信息、退出、近况入口 | 原样保护，F-31 待办 |
| ownership / D | `pages/user-center/data-backup/index.vue` | prepare/confirm；操作阶段/进度/失败恢复、封存策略、下载；clear-all 冻结 | 原样保护；真实 httpClient 冻结写入离线回归 PASS |
| summary / D | 个人中心 `stageSummary` | 请求忙碌/失败与 SUCCESS/FALLBACK 来源；不建结果路由 | 原样保护，F-31 待办 |
| agent-choose / E | `AgentIntentChooser.vue` | LISTEN/UNTANGLE 显式选择、取消；未选择不启动 | 原样保护，F-32 待办 |
| agent-writing / E | `AgentChatSheet.vue` | loading/sending/finishing/错误/重试；工具/素材用户确认；意图与记忆授权/来源 | 原样保护，F-32 待办 |
| agent-review / E | `ReviewChatSheet.vue` | 只读回看；忙碌/失败/结束；来源与记忆授权；与回信互斥 | 原样保护，F-32 待办 |
| 登录候选 | `pages/login/index.vue` | 既有认证、注册、微信、Preview | denylist，不迁移新登录视觉 |

矩阵不把未来样式称为已实现。没有改业务状态或 canonical mapping。技术 DRAFT 不作为普通记录计数来源；首页的 `draftState/latestDraft` 是既有变量名，其真实查询仍为 SAVED。时光轴 DTO 仅提供 cover 等有限字段；“无封面排印”不等于断言该记录没有声音或其它媒体，不推测附件类别，不新增 DTO。

## 材质与静态策略

- 暖纸 `#fcf9f4`、实色动作底 `#f3eee4`、墨色正文、局部边线和静态投影；新增 `--fb-letter-*` 仅由三个样板消费，不覆盖旧 token。
- 首页/时光轴复用现有 `AppTopSafeBar` 的微信胶囊/状态栏测量；不改该公共组件或全站壳层。底部三 Tab 保持原路由，留安全区。
- 首页减少封存仪式的主导，突出留下此刻；已留下记录入口读取已有 SAVED 查询结果。封存/抵达加载失败仍分别可重试。
- 编辑器保留原生 input/auto-height textarea，新增可读标签和 cursor-spacing；保存按钮用原生 disabled 表达已有 loading，不改保存条件/调用。声音保持原始录音，不做转写。
- 时光轴保留自然滚动与分页事件；筛选使用原生纵向 scroll-view 承载，封存条目使用静态暖白渐层；无封面条目不展示虚构照片占位。
- 平台支持/性能未确认，第一版不加入纸纹 SVG 噪声、背景模糊、回弹、持续呼吸、翻页或外链字体。无动画就是完整终态；F-15 的触摸/取消/后台/字体实测仍待补，不称玻璃已跨平台验收。

## F-13 / F-14 环境与验证

| 检查 | 结果 | 范围/限制 |
|---|---|---|
| clean HEAD / 类型基线 | PASS | Node v24.19.0；项目既有 vue-tsc --noEmit |
| 改前 Standard / Preview mp-weixin build | PASS | 两条 uni build 退出 0 |
| 改后 type-check / 两模式构建 | PASS | 最终命令结果见 AGENT_LOG；构建产物不入库 |
| 离线状态回归 | PASS | 13/13，真实 SFC 脚本/recordService/httpClient＋固定合成响应；无浏览器/微信渲染 |
| Git 范围与日志前缀 | PASS | 最终检查见 AGENT_LOG；不改保护业务文件 |
| OpenSpec CLI | SKIPPED | 不在 PATH；不安装依赖，文件级工件/Scenario 检查另列 |
| 微信工具库存 | confirmed | 安装版本 2.01.2510290；CLI --help 在受限模式出现 EEXIST，获准配置访问后成功 |
| 微信实际画面/输入 | SKIPPED | 无可连接自动化会话；运行端口 9420 未开启，未找到直接可用的 miniprogram-automator 依赖；当前无可控原生 UI，未启动新项目/登录或真实请求 |
| 真机/帧率/性能 | SKIPPED | 无物理设备/测量环境，不从源码或构建推导 |
| 真实服务/provider/对象存储 | SKIPPED | Gate 3 未授权，本轮真实业务外调 0 |

构建有既有 caniuse-lite 数据过期提示；未更新 package/lockfile。CLI 配置访问成功只证明工具可运行，不等于渲染/键盘/动效通过。

离线运行命令（frontend 目录）：

```text
node node_modules/vue-tsc/bin/vue-tsc.js --noEmit
node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build -p mp-weixin
node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build -p mp-weixin --mode preview
node scripts/visual-samples-offline.cjs
```

13 项回归：独立首页状态、加载等待、分页失败/去重/重试、筛选失败保留、过时响应隔离、节点状态/导航、草稿创建失败、保存失败、保存成功/防重、空内容/未完媒体/Preview拒绝、10种记录服务Preview写入拒绝、真实token优先、所有权清除冻结。uni.request 被本地同步合成响应替代，不连接后端；无真实持久化、媒体上传或设备证据。

## 待补微信审阅清单与下一批闸门

同一尺寸、相同固定合成内容，分别采集改前/改后；标明基础库、渲染器、开发工具版本和屏幕尺寸。不得使用真实日记、凭证或媒体作截图/夹具。

| 页面 | 必须实际操作/截图 |
|---|---|
| 首页 | loading/空/SAVED/SEALED/UNLOCKED/独立失败；长标题、封面失败；主动作和三 Tab；小屏/安全区 |
| 编辑器 | 初始化失败重试、继续恢复、中文输入/软键盘、长文；图片/声音/位置/封面既有控件；忙碌防重、失败留输入、成功可立即退出 |
| 时光轴 | 筛选取消/应用失败、日期与标签、分页末项可达/失败重试；无封面/有封面/封存；长标题、自然滚动 |
| 静态候选 | 字体缺失、纹理/模糊不存在、减少动态、触摸取消、后台恢复、快速重复导航；操作不等动画 |

F-20/F-21/F-22 代码已完成，页级微信验收未完成，复选框保留 pending。F-23 公共抽取、B 批详情及 C–E 批不在本次提交中提前实施；完成样板平台证据后按 tasks 顺序推进。F-27/F-45 用户审阅仍是独立任务，不因本地 commit 自动通过。

回退锚点：`4a5acfb` 为本批改前 clean HEAD；本批实现 commit 是可审阅点，不是已验收点。若发现保存被吞、Preview放开、封存暴露或键盘/滚动不可用，停止扩展并撤回本批展示改动；不删除数据、不迁移数据库。
