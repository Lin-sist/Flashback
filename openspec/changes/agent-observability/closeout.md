# Closeout：Agent Observability（C5）

> Change ID：`agent-observability`
> 闸门 1 批准 + 闸门 2 授权：2026-07-30（用户按推荐定稿 N1–N7）
> 闸门 3：**未授权、未执行**
> 开工锚点：`a834d85`
> 状态：**实现与验证完成，待用户验收**

---

## 1. 交付了什么

一轮 Agent 对话现在会留下一条可按会话查询的结构化决策轨迹，覆盖 thought → action → observation
全过程，且**不含一个字**的用户日记原文、对话原文或记忆片段。

具体到能回答哪些以前答不上来的问题：

| 排查场景 | C5 之前 | C5 之后 |
|---|---|---|
| 「Agent 突然说了句无关的话」 | 只有一行 warn，且 sessionId 是 null | `downgrade_path` + `violation` + 数值指标，可定位到轮次 |
| 「正常一轮有多慢」 | 完全不可见（成功路径耗时被丢弃） | `provider_duration_ms` |
| 「这一轮是重试还是新一轮」 | 分不清 | `attempt_no` |
| 「Agent 为什么没提起过去的事」 | 分不清是开关关了、检索挂了还是没命中 | 三种情形在轨迹里可区分 |
| 「改了话术之后行为变了吗」 | 无从对比 | `prompt_version` / `policy_version` 内容哈希 |
| 「回看里模型偷偷提议工具了吗」 | 只有一行 warn | `tools-fail-closed` 步骤 + 专用查询 |

---

## 2. 修改的文件

### 新增（后端主代码）

- `agent/trace/AgentTraceCollector.java` —— per-turn 收集器，隐私在类型层钉死
- `agent/trace/AgentTraceSink.java` —— 唯一落库出口，`REQUIRES_NEW` + fail-open
- `agent/trace/AgentTraceVersions.java` —— 版本锚点，内容哈希派生
- `agent/trace/AgentTraceLayer.java` —— 护栏闸层标识（6 值）
- `agent/trace/AgentTraceOutcome.java` —— 轮次结论（4 值，含 `DOWNGRADED`）
- `domain/AgentTurnTrace.java` / `mapper/AgentTurnTraceMapper.java` / `mapper/AgentTurnTraceMapper.xml`

### 新增（SQL）

- `backend/sql/mysql/c5-agent-turn-trace.sql` —— 幂等 DDL，**已在本地执行并验证**
- `backend/sql/mysql/c5-trace-queries.sql` —— 9 条只读排查查询

### 修改（后端主代码，均为「挂痕迹」而非改行为）

- `service/impl/AgentChatServiceImpl.java` —— try/finally 唯一出口 + 各步骤采集 + **V4 补齐**
- `agent/AgentPromptBuilder.java` —— 新增 `promptTemplateFingerprintSource()`；两处内联文案提取为常量（**文字逐字未改**）
- `agent/AgentModelClient.java` —— 新增 `model()`（只读配置值）
- `agent/tool/AgentToolCoordinator.java` —— `handleProposals` 带 trace 的重载（旧重载保留并委托）
- `config/AppAgentProperties.java` + `application.yml` —— `app.agent.observability.*`

### 新增（测试）

- `agent/trace/AgentTraceCollectorTest.java`（13）
- `agent/trace/AgentTraceVersionsTest.java`（5）
- `service/impl/AgentObservabilityIntegrationTest.java`（14）
- `service/impl/AgentGuardrailTraceCorrelationTest.java`（5）

### 修改（测试）

- `service/impl/AgentChatServiceImplTest.java` —— **仅构造签名补两个依赖，断言零修改**
- `src/test/resources/schema.sql` —— 新表 + DROP 顺序

### 未改动

**前端零文件**。认证与签发逻辑、护栏阈值、记忆检索、工具白名单、回看逻辑、
`uk_agent_message_session_turn_role`、`pom.xml` / lockfile 全部未动。

---

## 3. 验证结果

| 项 | 结果 |
|---|---|
| 后端全量回归 | **533 tests PASS / 2 skipped**，BUILD SUCCESS |
| 相对基线 | 496 + 37 新增 = 533，**零回归** |
| 既有断言修改 | **0 条** |
| 本地 DDL | 已执行；20 列与 3 索引符合设计；**幂等已验证**（重复执行 exit=0） |
| 隐私断言 | 实体层 + SQL 层双重断言特征串不出现，含回看路径单独用例 |
| 前端构建 | **未执行**——前端零改动，无需构建 |
| 闸门 3 | **已授权并执行**：真实调用 6 次 / 预算 10 |

### 闸门 3 结果（2026-07-30，provider=deepseek）

| 观察项 | 结果 |
|---|---|
| T-35 轨迹完整性 | **PASS** —— 6 轮全部三段齐备（七项采集点均命中） |
| T-36 耗时量级 | **PASS** —— min 4571ms / avg 6476ms / max 8467ms |
| T-37 fail-closed 活体触发 | **未触发，如实记为未验证** |
| 隐私复核（真实产出） | **PASS** —— 8 字滑窗扫全部文本列，`leaked=false` |

探针 `C5RealProviderProbeTest` 由 `C5_REAL_PROBE=1` 门控，默认跳过（已验证）。
它刻意走完整 `sendMessage` 而非只调 model client —— C5 要验的是采集点有没有漏，
而漏采集恰恰发生在编排层。附带确认：写作引导三轮均 `reason=ADVANCE`；
回看三轮 `stage_reason=null`（无阶段机不伪造结论）；版本锚点 6 轮稳定同值。

2 skipped 是 `C3RealProviderProbeTest` / `C4RealProviderProbeTest`，由环境变量门控，与 C5 无关。

---

## 4. 实现期发现的三处与规划不符（诚实记录）

### 4.1 `schema.mysql.sql` 不随增量维护 —— V19 判断被证伪

规划时我写「新表需同步三处」。实测发现 `backend/sql/mysql/schema.mysql.sql`
**只到 C1**：它既没有 `agent_tool_call`（C2），也没有 `agent_session.purpose`（C3）。

即项目的既有约定是「全量脚本不随增量维护」。**刻意没动它** —— 只往里加 C5 的表
会造出一个「有 C5 表却没有 C2 表」的更奇怪的状态；补齐它需要同时补 C2 + C3，
属本刀范围外的顺手改动。**交由用户决定是否另开 Type B。**

### 4.2 V5 有比「改 checker 签名」更小的解法

规划时打算把 sessionId 传进三个 checker。实现期改为不动它们：`CHECK_ERROR` 本来就以
`AgentGuardrailVerdict` 返回给调用方，而调用方现在会 `trace.guardrail(layer, verdict)` ——
关联天然成立。给三个 checker 加参数会让它们的全部调用点与单测跟着改，
diff 会混进一批与可观测无关的护栏改动，违背「C5 不改 Agent 行为」的自我约束。

**保留的已知缺口**：checker 内部那行 `log.warn(... cause=...)` 本身仍无 sessionId。
它现在是轨迹的冗余副本而非唯一线索，**不再单独处理**。

### 4.3 不需要哈希前缀

规划时打算沿用 `AgentToolArgsDigest` 的「长度 + 哈希前缀」范式表达文本规模。
实现后发现**不需要** —— 轨迹里没有任何需要「指向某段具体文本」的字段，
长度与计数已足够。不引入哈希是更强的隐私姿态：连不可还原的摘要都没有。

---

## 5. 两处对已冻结蓝图的偏离（已获批）

| 项 | 蓝图原文 | 实际做法 | 依据 |
|---|---|---|---|
| 存储 | 「MVP 可用结构化 JSON 日志文件，不需要专用存储」 | 新建 MySQL 表 | 蓝图同卡片要求「可查询」而本地无日志聚合；C6 要求字段级关联 |
| 采样 | 「可配置采样率」 | 默认全量，无采样率 | 采样会制造排查盲区——最想看的那一轮可能恰好没被采到 |

两处均在规划闸显式呈现并由用户 2026-07-30 批准。

---

## 6. 残余风险

- **[R10｜新] 回看 fail-closed 仍未活体触发**：闸门 3 的 6 轮中模型均未在无工具模式返回提议。
  C5 只做到「它真发生时能被记下」，不等于「已观察到它发生」。
  C3b 的同一残余**保持未验证状态**，两刀合计已观察 9 轮均未触发。属概率性行为，不单开 change
- **[新] 真实 provider 耗时偏高**：avg 6476ms / max 8467ms。量级与 C3b 观察一致、判为合理，
  但**这是首次有数据**。若后续认为体验偏慢，它属 C7（韧性）范围，本刀不处理
- **[新] 探针写入 H2 而非 MySQL**：闸门 3 验的是「真实模型 + 真实编排 + 真实落库」，
  换掉的只是数据库实例。**MySQL 上的轨迹落库未经真实联调**，
  但 DDL 已在本地 MySQL 执行且幂等验证过，且 mapper 未用任何 H2 特有语法
- **[新] mock 路径无 `prompt` 步骤**：mock 不组装提示词，该步骤只在真实 provider 路径出现。
  已写进 delta 的 scenario 条件，不是采集遗漏
- **[新] 可观测关闭时降级痕迹的 sessionId 仍为 null**：刻意如此——关闭就该完全不产生采集开销。
  已由测试固定该行为
- **[新] `schema.mysql.sql` 落后于增量脚本**（见 4.1），待用户决定
- **[R2] 引导话术与素材合成质量**：本刀未动。C5 完成后**第一次具备「优化前后可对比」的条件**
- **[R9] 检索相关性弱**：未动，留独立 change
- **[R6｜待用户执行] 凭证轮换**：`AI_API_KEY` / `S3_*` / `WECHAT_MINI_PROGRAM_SECRET`；
  轮换后建议删 `backend/start-dev-wechat.local.ps1.bak`
- **轨迹表增长**：保留期 + 手动清理脚本已就位，无定时任务，需人执行
- **Kiro 诊断误报**：`design.md` / `tasks.md` 报「缺少 Kiro Spec 章节」。
  本项目按规则用 OpenSpec 而非 `.kiro/specs/`，**不修**

---

## 7. 下一步

1. 用户验收 diff
2. delta 接受进 `openspec/specs/`（`agent-runtime` 四条 MODIFIED 要逐条落）
3. 归档到 `openspec/changes/archive/2026-07-30-agent-observability/`，`ACTIVE_TASK` → IDLE
4. **Phase 1 至此收官** → 进入蓝图 v1.2 校准会（v1.2 草案 §0.2 与 §8 的清单）

提交责任：**用户手动提交**。本轮未执行任何 `git add` / `commit` / `push`。
