# Closeout：Agent Review Chat（C3 后半刀）

> Change ID：`agent-review-chat`
> 状态：**已归档**（2026-07-29 用户验收，delta 已接受进 baseline）
> 闸门 3：**已执行**（与 C3a 合并），含真实 provider 联调 + 微信真机手验
> 提交责任：用户手动提交（本轮末段由用户明确授权 Agent 代为提交）

**C3 两刀至此全部完成。** 下一刀为 C5 `agent-observability`。

---

## 1. 交付了什么

| 目标（proposal §4） | 状态 | 落点 |
|---|---|---|
| 1 回看会话 | **done** | `purpose=REVIEW_CHAT` 复用既有会话与消息持久化 |
| 2 无阶段自由多轮 | **done** | `stage=REVIEW`、轮次上限单列（默认 6）、上限或 `finish` 结束 |
| 3 回看上下文 | **done** | `content` + `ai_summary` + `belief_then` + Memory 检索，均带时间锚点 |
| 4 完全无工具 | **done** | 模式短路 + tool_calls fail-closed |
| 5 不产可回填素材 | **done** | 模式短路 `generateMaterial`，`finish` 路径同样短路 |
| 6 护栏全量继承 | **done** | 六层全部生效，阈值未放宽 |
| 7 前端回看浮层 | **done** | `ReviewChatSheet` + 与 `reply-overlay` 互斥 |
| 8 R3 补齐 | **done** | 真机手验通过，R3 关闭 |

新增 1 个生产类（`AgentChatMode`）+ 1 个前端组件 + 3 个测试类；`AgentStage` 新增 `REVIEW`。

---

## 2. 验证结果

- 后端全量：**496 tests PASS / 0 failures / 0 errors / 2 skipped**（472 基线 + 24 新增；2 skipped 为 C4 与 C3 的真实 provider 探针，均由环境变量门控）
- 前端：`vue-tsc --noEmit` PASS、`build:mp-weixin` PASS
- 本地 MySQL：`c3-agent-memory.sql` 已执行、幂等验证 PASS、回看会话真实插入 + 按 purpose 查询命中
- 闸门 3 真实联调：**15 次调用**（预算 ≤ 20），provider=deepseek，仅自造内容，不使用用户真实日记，不写库
- **微信真机手验 PASS**（用户执行）：回看对话可开启 / 多轮 / 收束；Agent 自发带时间归属（截图见 §4）；浮层无工具确认条与素材入口；写作引导的素材二段式确认可用

### 唯一修改的既有断言

`AgentMemoryIntegrationTest.shouldNotCreateAnyReviewChatSession` → `writingGuidanceSessionMustNeverBeMarkedAsReviewChat`。
原断言是 C3a 的范围守护（「本刀不实现回看」），C3b 落地后原意失效；改为正向断言而非删除，
因为它守护的另一半（写作引导不得被误标 purpose）仍然有效。已在 AGENT_LOG 显式披露。其余既有断言零修改。

---

## 3. 闸门 3 的核心结论

### 3.1 R8 可以关闭：时间归属阈值经真实验证，两个方向都覆盖了

**误伤方向：0 次**（3 轮 × 3 次运行 = 9 轮观察）。memory-only 片段实测 0~22 字，多次超过阈值 8，
说明判定确实被触发，而非因未达阈值而空过。

这里有一处**差点被误读的地方**：只看 `attribution=null` 的汇总会得出「护栏很好」，但放行有两种可能——
模型真的说清了时间，或者词表偶然命中了某个字。两者含义相反。因此额外打印了命中词，
实测命中「那时 / 过去 / 以前 / 你说过 / 四月 / 去年」，**均为真实的时间归属表述**，放行理由正确。

**结论：阈值 8 无需调整。** 未发生「为回看单开更宽阈值」或「回看关掉该检查」——两者都是 design 决策 5 明确否决的方向。

### 3.2 拦截方向首次活体验证（本刀最有价值的结论，顺带补上 R7）

全部放行只证明了不误伤，**没有证明护栏有效**——C4 的 R7 就一直悬在这个位置。

做法：取模型真实产出、且 memory-only 片段最长（15 字）的那条回复，
**只删掉其中的时间指示语、其余逐字不动**，重新判定。

结果：`original=null → stripped=missing-time-attribution`，**`flipped=true`**。
被判定的文本仍是模型真实写出的句子，不是构造样本。

**这同时补上了 R7 缺的那一半**（C4 只验到误伤方向）——两者是同一层机制。

### 3.3 诚实记为未验证的一项

模型三轮均未返回 tool_calls，**回看的 fail-closed 分支未被真实触发**，
其正确性仅由单测覆盖。这是概率性行为，不为它单开 change。

---

## 4. 真机手验的证据要点

用户截图显示的两处，正是本刀两条核心约束在真实链路上的体现：

1. **回看页**：Agent 说「**去年六月**你想坚持锻炼与学习，现在你在跑步、去健身房、学编程。
   这些事从『想』变成『在做』，感觉有什么不一样？」——复述过去的内容时**自发带了时间归属**，
   与探针结论一致。末句「**去年六月**写下那句话的你，会不会对『坚持』本身也抱了太多想象？」同样带锚点。
   浮层顶部明写「这段对话不会改动这条记录」，**无工具确认条、无素材回填入口**。
2. **写作引导页**：素材卡正常呈现「先不用 / 用作正文」，二段式确认可用 → **R3 关闭**。

用户评价：「整体体验比之前好不少，至少让我感觉 Agent 有点『说人话』了，但还需要进步，当前已算够用。」

---

## 5. 实现期偏离规划的地方

### 5.1 三处规划期预判的陷阱全部确认真实存在

这是规划阶段读代码最值钱的产出，实现时逐一验证：

1. **`buildToolContext` 确实会给出错误答案**。它只按「有无 `recordId`」判断是否下发 tools，
   而回看会话恰好绑定一条记录。模式判断放在该判断**之前**显式短路，并写了直接断言
   `AgentChatMode` 属性的测试（而非只测行为副作用，后者重构后易被绕过）。
2. **不复用 `CLOSING` 是对的**。它在写作引导里会触发 `generateMaterial`，复用等于埋一个
   「回看意外产出可回填素材」的坑。改新增 `REVIEW` 后，`AgentMockResponder` 的穷尽 switch
   **直接编译报错**，逼出显式处理——这验证了 tasks T-02「不靠 default 混过去」的必要性。
3. **`selectActiveByUserAndRecord` 补 purpose 谓词**。当前不加也不会错（DRAFT/UNLOCKED 互斥），
   但契约不该依赖巧合。

### 5.2 一处流程失误：DDL 未执行导致用户手验报错

用户手验时记录页与回看页均报「系统异常: api/agent/sessions」。根因是 C3a 的 mapper 已把
`purpose` 写进列清单与 insert，而本地库没有该列 → **写作引导对话也一起 500，不只回看**。
属部署步骤缺失，非代码缺陷。

**教训（已记入 ACTIVE_TASK Residual）**：增量 DDL 未执行时的表现是通用 500，且会波及既有功能。
后续含 DDL 的 change 应把「本地执行」列为**实现期第一步**，而不是像本刀这样列为「真机前置」。

### 5.3 两次自我修正

- **拦截验证的样本选错**：第一版取「最后一轮」回复，而它恰好 `memoryOnlyRun=0`（没在复述），
  剥离时间词后自然不翻转。那是样本选错而非护栏失效，改为按 memory-only 片段最长挑选后翻转成功。
  **教训：验证拦截方向必须先确认样本确实处于该被拦的状态。**
- **diff 污染**：`AgentMemoryIntegrationTest` 又被编辑器自动格式化 + 改行尾，diff 从 17 行虚增到 262 行。
  修正过程中还写错一版检测脚本（用 `git show | Out-String` 判断 HEAD 行尾，而 PowerShell 管道
  自身会加 CRLF，导致误判方向白修一轮），最终按 `--ignore-cr-at-eol` 的收敛幅度判断才正确。

---

## 6. 残余风险

| # | 风险 | 处置 |
|---|---|---|
| — | **回看 fail-closed 分支未活体触发** | 概率性行为；单测覆盖；不单开 change |
| — | 闸门 3 样本量小（9 轮观察、单一 provider/model） | 已接受；不声称杜绝 |
| **R9** | 检索相关性弱（标签 + 说明性字段 LIKE，无权重 / 分词 / 向量） | 蓝图 C3 风险栏已接受；升级留独立 change |
| — | 本地 `tag` 表为空 → 标签关联路径零命中 | 非代码缺陷；建标签后可验 |
| — | `core_question` 本地 0% 非空 | 固定优先级降级自动跳过 |
| **R2** | 引导话术与素材合成质量 | 用户本轮再次确认「还需要进步，当前够用」→ 仍延后到 C1–C5 全部完工后统一优化 |

**已关闭**：R8（时间归属阈值未校准）、R3（微信真机工具链路手验）、R7（实质缓解——拦截方向已验证）。

---

## 7. 给 C5 `agent-observability` 的 carry-over

1. **已有可复用的结构化痕迹**：`AgentGuardrailVerdict.metrics()`（覆盖率 / 最长未覆盖片段 / 受检长度）、
   `AgentGuardrailDowngrade.trace()`（路径 / 违规类型 / fallback 标识）、`agent_tool_call` 的
   `status` / `failure_type` / `args_digest`。C5 应当**消费它们**，而不是另起一套埋点。
2. **两个探针可作为 C5 的观察起点**：`C4RealProviderProbeTest` / `C3RealProviderProbeTest` 已经在做
   「打印结构化指标、不打印原文」的事，C5 的 trace 格式可以与它们的输出对齐。
3. **隐私红线已经确立且必须延续**：日记原文、对话原文、memory 片段一律不进日志 / 审计 / 痕迹；
   `MemoryFragment` 覆写 `toString` 就是为了防止不经意的日志拼接泄露。C5 是最容易破这条线的一刀。
4. **会话用途维度已就位**：`agent_session.purpose` 与 `AgentChatMode` 让 trace 可以按模式区分——
   写作引导与回看的决策链路形态不同（后者无阶段推进、无工具）。
5. **fail-closed 分支缺活体证据**：C5 若能记录「模型在无工具模式下返回提议」这类事件，
   正好能把本刀未验证的那一项补上。
6. **R2 的归属**：C1–C5 全部完工后才统一优化引导与素材质量，C5 同样不动。
