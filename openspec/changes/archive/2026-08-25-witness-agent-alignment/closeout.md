# P4.1 `witness-agent-alignment` Closeout

## 1. 结论

- Change：P4.1 `witness-agent-alignment`
- 开工锚点：`0aea558`
- 主实现 checkpoint：`09304a1 feat(agent): 实现P4.1见证者对齐并记录验收状态`
- 收口日期：2026-08-25
- 授权：规划、实现、Gate 3a/3b/3c、delta acceptance、归档与 Agent 本地提交均由用户明确批准
- 结果：**ACCEPTED / ARCHIVED**。五份 delta 已接受进 baseline；归档后 `ACTIVE_TASK` 回到 `IDLE`
- 外部副作用：未 push、未创建 PR、未部署、未发布；真实 provider 调用严格保持 8 次总预算

P4.1 已把写作 Agent 从固定四阶段的“朋友式访谈”收敛为用户可控的见证者：用户先选择 `LISTEN` 或 `UNTANGLE`，新写作会话使用 `WITNESS`，LISTEN 不主动提问，UNTANGLE 正常输入每轮至多一个可跳过的问题，极短输入与结束状态问题上限为 0。用户可切换意图或随时结束；既有工具、素材确认、用户原文、Preview、owner scope、失败恢复与时间边界继续成立。

## 2. 已交付范围

- WRITING_GUIDANCE 新增 `LISTEN|UNTANGLE` 会话意图并持久化；缺省为更少打扰的 LISTEN，REVIEW_CHAT 保持 null
- 新会话统一使用 `WITNESS`；历史四阶段只读兼容，不回写 message/trace 历史事实
- 后端 `AgentWitnessTurnPolicy` 输出 `REFLECT_ONLY|MAY_ASK_ONE|CLOSE` 与 0/1 问题上限
- `EXCESSIVE_QUESTIONS` 进入一次 content-free typed reflection；仍越界时使用无问题的本地克制 fallback
- Prompt 角色改为有温度的见证者，移除“一个朋友”、固定阶段目标、默认追问与关系承诺
- intent switch 为 owner-scoped backend 权威状态；不调用 provider、不推进 turn/stage、不执行工具、不生成素材，失败不乐观更新
- 小程序既有被动入口先展示两项同权选择；浮层显示当前意图而非阶段旅程，保留关闭、结束、retry、工具/素材确认
- Preview 在 service boundary fail-closed，不提供假对话或假切换成功
- C6 fixtures、硬不变量、snapshot 与 `baselineNote` 已按 P4.1 逐项归属更新

## 3. 验收证据

### 自动化、H2 与构建

- 收口 backend full：**104 suites / 728 tests / 0 failures / 0 errors / 13 skipped**
- 13 skipped 包含默认关闭的真实 provider 与真实 MySQL 探针；真实探针只在显式 Gate 环境变量下运行
- frontend：`vue-tsc --noEmit`、标准 mp-weixin build、Preview build 均 PASS；package/lockfile 零变化
- `git diff --check` PASS；delta exact-copy、operation、Requirement/Scenario 与范围文件级校验 PASS

### Gate 3a：真实 provider 与结构化人评

- 2 次 canary + 6 个固定合成场景，共 8 scenarios / 8 provider calls / 0 reflection；达到硬预算后停止继续外调
- 问题上限、长度与关系/结论 marker 自动边界 PASS
- witness role、user control、question restraint、brief restraint、uncertainty、no conclusion 六项结构化人评均 PASS
- tracked evidence 不保存合成输入、prompt 或 provider 回复文本；小样本 PASS 不等于生产稳定性或所有自然语言表现均成立

### Gate 3b：微信开发者工具

- 微信登录真实链路 PASS：`wx.login` → backend `wechat-login` → code2session → 既有账号 → 首页数据加载；未记录 token、openid 或用户内容
- 登录报错根因是本机 backend 未运行、8080 无监听；使用 ignored 本地 secret 启动脚本恢复 Spring Boot/MySQL 连接后成功，不是登录业务代码缺陷
- automation 工具链使用 IDE HTTP `--port 9421`、automation `--auto-port 9420` 与 `ws://[::1]:9420`；此前把两类端口混用且只尝试 IPv4，现已纠正
- Standard：真实标准构建与真实登录边界下，Agent API 使用合成 `wx.request` 返回以守住 provider 预算；入口、两项同权选择、textarea、长消息滚动、切换失败保持原 intent、再次切换成功、结束态均 PASS；新增 provider 调用 0
- Preview：入口可见，选择器/对话层不打开，真实 Agent 请求 0，PASS
- 开发者工具 compile cache 曾继续运行旧构建；按项目仅清 `compile` 缓存并重启后加载当前产物，未清 auth、storage、network 或其他项目缓存

### Gate 3c：真实 MySQL

- MySQL 8.0.41；只读 preflight 仅检查 schema 与 purpose/intent/stage 聚合，不读取或输出 message、日记或 prompt
- P4.1 migration 连续执行两次 PASS；`conversation_intent` nullable，历史 WRITING_GUIDANCE 归一到 LISTEN/WITNESS，REVIEW_CHAT intent 保持 null
- 默认关闭的 `P41RealMySqlWitnessProbeTest` 显式运行 PASS：schema、恢复、intent switch、review null、owner scope 全部成立
- 探针 finally cleanup PASS：合成 users、records、sessions 与 blocking operations 均为 0

## 4. Delta acceptance

五份 delta 已按 operation 接受进 baseline，并完成逐 requirement exact-copy 校验：

- `agent-runtime`：1 MODIFIED + 5 ADDED，6 Requirements / 26 Scenarios
- `backend-core`：4 ADDED，4 Requirements / 15 Scenarios
- `miniapp-core`：1 MODIFIED + 5 ADDED，6 Requirements / 19 Scenarios
- `v2-product-scope`：4 REMOVED + 1 MODIFIED + 5 ADDED，10 Requirements / 24 Scenarios
- `agent-collaboration`：5 ADDED，5 Requirements / 17 Scenarios

归档 delta 合计 **5 specs / 31 operation blocks / 101 Scenarios**；MODIFIED/ADDED exact-copy 与 REMOVED absence 校验 PASS。

## 5. 明确保留的 PARTIAL / SKIPPED

- T-16 保持 **PARTIAL**：实现前只留有 frontend type-check baseline，未单独保存 standard/Preview build baseline；不倒签。实现后与收口双构建均 PASS
- 物理真机：**SKIPPED**；Gate 3b 使用微信开发者工具完成 Standard/Preview 交互矩阵，不把模拟器写成真机证据
- OpenSpec CLI：本机不在 PATH；采用 artifacts、delta exact-copy、任务、链接、结构与文件级校验，不声称 CLI status/validate PASS
- Standard 的短答/错误/结束 UI 使用合成 Agent response，新增真实 provider 调用 0；真实语言质量由独立 Gate 3a 的 8-call 小样本人评承担
- 本机 MySQL、开发者工具与单账号登录 PASS 不等于生产容量、并发、长期可用性或 SLA

## 6. 范围安全

- 未实现 P4.2 memory authorization/retrieval rewrite、图、画像、评分、诊断、建议清单或关系养成
- 未新增工具、扩大工具白名单、削弱工具/素材/用户原文显式确认
- 未改变 SEALED location/attachment/cover 不可变性、三个一级 Tab 或 V2.0 canonical naming
- 未修改 package/lockfile、deployment、monitoring、admin portal 或冻结蓝图；未做大规模 backend rewrite 或 major visual reconstruction
- tracked evidence 不含用户日记、对话、provider 回复、prompt、token、openid、secret 或本机凭证明文

## 7. Remaining risks 与下一步

- 物理真机的键盘顶起、滚动触感与不同系统尺寸仍未验证；若进入发布准备，须独立安排真机矩阵
- 本地开发环境依赖手动启动 MySQL 与 backend；本次登录故障表明运行前置缺少显式健康提示，可作为后续独立开发体验小修评估
- dev profile 的 MyBatis DEBUG 会输出 SQL 参数，已观察到账号标识；本轮未把该内容写入 tracked evidence，但普通日志隐私边界需要独立 Type B 安全收口，且不得记录用户日记参数
- 冻结蓝图的下一候选为 P4.2 `memory-agency`；P4.1 归档后仍须重新走独立规划闸，不继承实现、外调、提交或发布授权
- push、PR、部署、发布仍需独立授权
