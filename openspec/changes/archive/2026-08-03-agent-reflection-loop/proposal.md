# Agent Reflection Loop（C7）

> Change ID：`agent-reflection-loop`
> 类型：Type C（改变 Agent 输出编排行为）
> 阶段：**闸门 1 已批准、闸门 2 已授权，进入实现**（2026-08-02）
> 提交责任：用户已授权 Agent 提交本次 C7 变更；`push` / 部署 / 发布未授权

## 1. Why Now

C6 已把当前 Agent 编排行为固化为离线不变量与 23 条可追溯快照，C4 的确定性护栏也已能给出结构化违规类型。当前仍缺少的是：当模型产出落入一类“可明确修正”的违规时，系统只能直接降级，不能要求模型在同一轮内按固定要求重写一次。

C7 在不引入图框架、不引入 LLM 自检器、不放宽护栏的前提下，为回复路径的
`MISSING_TIME_ATTRIBUTION` 增加一个受控环：要求补明“那是过去哪个时候”的归属后重写一次。
`CLOSING` 后还要同步生成 material，为守住单轮调用预算，该阶段不开环。

若重写仍违规、provider 失败或检查器异常，立即回到 C4 已接受的本地兜底/丢弃语义。最大重写次数由后端常量固定为 1，不由模型决定。

## 2. Readiness Verdict

结论：**GO，可以进入 C7 规划；尚不可进入实现。**

依据：

1. `.ai/ACTIVE_TASK.md` 为 `IDLE`，没有并行 Type C change。
2. C6 已归档至 `openspec/changes/archive/2026-07-31-agent-eval-framework/`，三份 delta 已进入 baseline。
3. C6 后端全量基线为 606 tests PASS / 4 skipped，并已建立 23 条编排快照。
4. Git 工作区在开刀前为 clean（`main...origin/main`）。
5. 蓝图 v1.2 已冻结，明确序列为 C6 → C7 → C8 → C9，C7 硬依赖 C4 + C6 均已满足。
6. 现有 M1/M3 目录是历史未归档目录，蓝图明确列为旁支清理项；`ACTIVE_TASK` 未指向它们，不阻塞 C7。

## 3. 现状事实（能力五态）

### confirmed

- F1：`AgentChatServiceImpl` 当前实测 1274 行；蓝图中的 1183 行已过时，本规划采用实测值。
- F2：回复生成、检查、降级均集中在 `generateReply` / `applyReplyGuardrail`，后置检查当前不持有 provider 调用能力。
- F3：回复路径当前执行 `AgentContentChecker` 与 `AgentTimeAttributionChecker`；可产生 `DIAGNOSTIC`、`FAKE_ACTION`、`MISSING_TIME_ATTRIBUTION`、`CHECK_ERROR`，**不执行回复忠实度检查**。
- F4：`UNFAITHFUL` 当前由 `AgentFaithfulnessChecker` 在素材路径与工具参数路径产生；素材失败语义是丢弃，工具失败语义是拒绝提议。
- F5：C4 明确拒绝给普通回复做全量忠实度闸，因为 Agent 的提问本来就是自己的话；贸然增加会把正常提问判死。
- F6：`AgentGuardrailVerdict` 只携带违规类型和数值指标，类契约明确禁止候选文本、用户原话和未覆盖片段。
- F7：C5 一条 persisted trace 对应一次用户轮次/HTTP 尝试；`attemptNo` 用于同轮请求重试，不是 provider 子调用序号。
- F8：轨迹步骤目前能记录多次 provider / guardrail step，但顶层 `providerDurationMs`、`downgradePath`、`violation` 只保留最终汇总值。
- F9：C5 实测单次 provider 耗时 min 4571 / avg 6476 / max 8467ms；后端 AI 超时 20s，前端 Agent 请求超时 30s。
- F10：C6 的 scripted provider 替身走真实生成分支，能按轮次编排响应；现有 23 条快照有 `baselineNote` + checksum 守护。
- F11：C6 人评锚点结构已就位但内容为空；填充需真实产出与人工评级。
- F12：仓库无 CI；当前只能承诺 Maven 默认测试路径，不可称 CI gate。

### partial / unknown

- F13：两次真实 provider 调用在同一请求内是否稳定落在 20s 以内为 **unknown**；用历史平均值推算约 13s，但历史最大值双倍会超过 16s，且尚无 C7 实测。
- F14：真实 provider 对时间归属重写要求的遵从率为 **unknown**。
- F15：重写相对直接降级是否改善用户体感为 **unknown**，须闸门 3 小样本 + 人评验证。
- F16：真实 MySQL 下新增多步轨迹的事务/持久化表现为 **unknown**，H2 不能替代该验证。

## 4. 规划期事实修正

蓝图把 `UNFAITHFUL` 与 `MISSING_TIME_ATTRIBUTION` 都描述在“回复重写”语境下，但 checked-in code 显示：普通回复不做全量忠实度检查，`UNFAITHFUL` 的真实可恢复挂点是素材路径。

C7 不借机新增回复忠实度语义；否则会同时改变 C4 已接受的判定范围与普通提问能力。
用户 2026-08-02 裁决按推荐方案收窄为 **reply-only**：

- Reply Reflection：只恢复非 `CLOSING` 阶段的 `MISSING_TIME_ATTRIBUTION`；
- Material `UNFAITHFUL`：继续沿用 C4 的直接丢弃语义，不开环；
- Tool proposal 不开环，因为拒绝提议已是低损失 fail-closed 路径；
- `CLOSING` 回复不开环，因为同轮随后还会生成 material，否则请求会达到 3 次 provider 调用。

这属于规划期对实现事实的校准，不修改已冻结蓝图；在本 change 内留下可审计偏离。

## 5. Goals

1. 抽出窄职责的生成-检查-一次重写协作者，降低在 1274 行服务类内继续堆叠分支的风险。
2. 只对非 `CLOSING` 回复的 `MISSING_TIME_ATTRIBUTION` 开环。
3. 重写指令由违规枚举映射到后端固定文案，只传类型化要求，不传候选文本片段。
4. 重写最多一次；第二次仍不合格即执行现有降级。
5. `CHECK_ERROR`、`DIAGNOSTIC`、`FAKE_ACTION`、`FABRICATED_QUOTE` 绝不重试。
6. provider 调用失败/无效内容绝不触发 reflection 重试；错误分类与一般重试留给 C8。
7. 轨迹在同一个 turn/attempt 内区分 initial / reflection / terminal downgrade，且不新增文本字段。
8. C6 不变量继续全绿；预期快照变化必须逐条人工确认并更新 `baselineNote`。
9. 通过真实 MySQL 与真机小样本验证耗时、事务边界和实际重写效果。

## 6. Non-Goals

- 不引入 LangGraph、Spring AI 图编排或任何图框架。
- 不新增 LLM-as-Judge、自检模型、绝对质量评分或 A/B 平台。
- 不给普通回复新增全量忠实度闸，不改变 C4 阈值、词表或来源集合。
- 不对 `CLOSING` 回复或 material 开环；`UNFAITHFUL` material 继续直接丢弃。
- 不对工具提议、`DIAGNOSTIC`、`FAKE_ACTION`、`FABRICATED_QUOTE`、`CHECK_ERROR` 开环。
- 不做 provider/network 错误重试、错误分类、熔断、多 provider 路由；这些属于 C8。
- 不修改前后端 20s/30s 超时配置；若实测超预算，停止并回到规划，不靠放宽超时掩盖。
- 不改 API/DTO/数据库 schema/前端 UI/工具白名单/阶段机/记忆检索。
- 不自动刷新 C6 快照，不写入真实日记、候选输出或 prompt 全文。
- 不顺手处理 schema.mysql.sql、CI、R9 检索、护栏阈值校准等旁支。

## 7. 用户故事

- 回看回复缺少时间归属时，用户优先得到一条补明“那时”的合格回复；若仍不合格，体验与当前一致，收到本地安全兜底。
- 收束素材增写了用户未表达的内容时，系统先要求只基于原话重整一次；若仍不忠实，素材仍被丢弃，不进入记录正文。
- 开发者能从脱敏轨迹看出首次生成、重写原因、重写结果与最终降级，而无需查看用户文本。

## 8. 外调预算与闸门 3

规划建议预算：**最多 6 次真实 provider 调用**，仅在闸门 3 单独批准后执行。

- 1 个 reply 场景 × 3 轮重复 × 最多 2 次 provider 调用 = 上限 6；
- 先跑 1 轮 canary（最多 2 次调用）；若出现超时、identity/config 漂移、候选内容泄漏、调用数超限或错误路径重试，立即停止；
- canary 通过后再用剩余额度补重复性观察；
- 同批结果只记录受控评级与结构化指标，可用于填 C6 `narrative-anchors.yaml`，不把回复正文写入 tracked file；
- 真实调用授权不包含 push、部署或发布。

## 9. 验收标准

1. reply 路径具备：首次通过、首次违规后重写通过、重写仍违规后降级。
2. 六类违规逐类证明开环/不开环；`CHECK_ERROR` provider 调用数恒不增加。
3. provider 失败与 invalid content 不触发 reflection。
4. 每个业务请求的 reflection 次数 ≤ 1，且由后端常量约束。
5. 重写指令只由 violation enum 映射，断言不含候选文本、用户文本、记忆文本或未覆盖片段。
6. 同一轮 reflection 不增加 `attemptNo`；轨迹 step 显式包含 `phase=initial|reflection`、reason、result，顶层仍是一条 turn trace。
7. 回复重写不得重复执行工具提议；工具调用以首次生成结果为准或按 design 明确丢弃，测试锁定。
8. `CLOSING` 与 material 不触发 reflection；既有 `materialDraft=null` 丢弃语义不变。
9. C6 23 条不变量全绿；发生快照变化时逐条给出原因并更新 `baselineNote` + checksum。
10. 后端全量回归不低于 606 tests / 4 skipped 基线，不得修改既有断言来换绿。
11. 真实 MySQL 联调覆盖一轮含 reflection 的轨迹持久化与事务完成；H2 结果不冒充 MySQL。
12. 闸门 3 批准后，真机记录每条路径的 provider 调用数、总耗时、终态与受控人评；未批准则明确 SKIPPED。
13. 无 API/DTO/schema/frontend/超时/package/lockfile 改动。
14. 叙事文档 §8 只在归档收尾阶段更新，不在规划期假装能力已交付。

## 10. Spec Delta 落点

- `agent-runtime`：受控环语义、eligible 集合、一次上限、终态降级、C6 回归留痕。
- `backend-core`：后端调用预算、轨迹阶段、无 schema/API/timeout 变化、真实 MySQL 验证边界。
- `v2-product-scope`：用户可见的回复/素材质量行为，但不新增页面或操作。
- `agent-collaboration`：重写指令与证据脱敏、外调预算、快照更新纪律。
- `miniapp-core`：无 delta（前端协议与 UI 不变）。

## 11. 关键风险

| 风险 | 缓解 |
|---|---|
| 双调用接近/超过 20s | 一次硬上限；不对 provider 错误重试；闸门 3 canary 测总耗时；超预算即停，不改超时 |
| 为实现 UNFAITHFUL 误给回复加忠实度闸 | material reflection 已移出 C7；spec 明确禁止扩大回复判定范围 |
| 重写重复产生工具提议 | reflection 调用不下发 tools；工具提议只来自 initial 调用并按终态策略锁定 |
| 轨迹把子调用误记为新 attempt | P13 定案：同一 persisted trace，step 区分 phase；`attemptNo` 只服务 HTTP 同轮重试 |
| 快照被机械改绿 | C6 既有 baseline guard 不变；逐条 `baselineNote` 说明 C7 行为变化 |
| 候选文本进入重写证据 | 映射器输入仅枚举；轨迹只记 reason/phase/count/result；结构化隐私测试 |
| 抽协作者演变成大重构 | 只移动生成/检查所需最小逻辑；不动阶段机、工具执行、API、数据模型 |

## 12. Gate State

- 闸门 1：**已批准**（2026-08-02）。
- 闸门 2：**已授权**（2026-08-02，用户表述“若条件允许则可以进入实现”）。
- 闸门 3：未授权；真实 provider / 真机外调须单独批准，预算上限 6。
