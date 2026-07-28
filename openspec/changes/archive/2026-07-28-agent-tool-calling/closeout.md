# C2 `agent-tool-calling` Closeout

> 验收日期：2026-07-28
> 结果：**已验收并归档**
> 提交：`6c363f6`（实现）、`98c764a`（JSON 缺陷修复 + 闸门 3）、`8587893`（secret 外移 + 问题归档）
> 开工锚点：`63d1767`

---

## 1. 交付了什么

把 Agent 从「只会聊」变成「聊完能做事，但每一步都要用户点头」。

| 能力 | 落点 |
|---|---|
| 原生 function calling 接入 | `AgentModelClient.completeWithTools`（新增，既有 `complete()` 未动） |
| 代码级工具白名单 | `agent/tool/AgentToolRegistry` + `AgentToolSchemaFactory`（生成 strict 合规 schema） |
| 二段式确认 | 提议落 `PROPOSED` → `POST /api/agent/sessions/{sid}/tool-calls/{tid}/confirm` 才执行 |
| 受控执行 | `AgentToolExecutor` → 既有 `RecordService`（新增 `appendContent` / `appendTags` / `updateUnlockAt`） |
| 审计与幂等 | 新表 `agent_tool_call` + `updateStatusIfProposed` 条件更新 |
| 前端确认交互 | `AgentChatSheet.vue` 确认条 + `agentChat` store 的 `confirmToolCall` |

白名单最终为 3 写（`append_record_content` / `add_record_tags` / `propose_unlock_at`）+ 2 读（改为 prompt 预注入）。
seal / delete / unlock / location / cover / attachment / later-reflection / 标签创建**代码级排除**。

## 2. 规划期的关键转向

**Q1 从「自研 JSON 提议协议」翻转为「原生 FC + strict mode」**。初稿推荐自研协议，理由是 `tools` 与 `response_format` 共存性未知；查阅官方文档后该前提不成立：二者分属 `tool_calls` 与 `content` 字段，天然并存。更重要的是 strict mode 让 schema 由 provider 服务端校验，白名单从「prompt 提示」升级为「服务端强制类型约束」，可靠性反超自研方案。长期上 tools schema / `tool_call_id` / `role:"tool"` 也是 C3 多工具、C5 决策链路与未来 MCP 接入的共同地基。

**但只取协议、不取控制流**（决策 9）。标准 FC 循环是模型驱动执行，用户没有插入确认的位置，与「建议不代决」正面冲突。因此后端不做单轮内 FC 循环，每轮 provider 请求恒定 1 次。

## 3. 验证结果

| 项 | 结果 |
|---|---|
| backend `mvn -B test` | PASS｜**339 tests / 0 failures**（C1 基线 254 未回归，且未改动任何 C1 断言） |
| frontend `type-check` / `build:mp-weixin` | PASS / PASS |
| 真实 MySQL DDL | PASS｜14 列 + 3 个 CASCADE 外键与设计一致 |
| 真实 provider 原生 FC | PASS｜本仓库首次实测可用（`finish_reason=tool_calls`） |
| strict mode（`/beta` + `strict:true`） | PASS｜服务端**接受**我们生成的 schema，design §4 降级处置无需启用 |
| mock provider 端到端 | PASS｜提议 → 确认 → 执行 → 拒绝 → 幂等 → 封存后失败 |
| 审计脱敏 | PASS｜`args_digest` 仅含长度与哈希前缀；`pending_args` 终结后为 NULL |
| 外调用量 | 6 次 / 预算 45 |

**SKIPPED**：微信端到端手验的完整链路（提议→确认→执行）未在真机走通——手验期间因运行实例未重启，注意力集中在 JSON 显示缺陷上。后端侧已由 mock 集成测试与真实 FC 探针分别覆盖。

## 4. 实现期新增决策

**决策 12：待确认提议的执行参数存哪里**。规划时定了「审计只存摘要」，落地才暴露：执行 `append_record_content` 需要原始 `text`，而摘要不可还原。否决了「前端确认时回传参数」（等于让客户端绕过白名单校验）与「存内存」（重启即丢、多实例失效），选了瞬态 `pending_args` 列，终结时与状态流转在同一条 UPDATE 中清空。

代价已声明：待确认窗口内审计表确实存在一份日记文本副本，比理想状态弱一档。已写入 DDL 注释与 spec scenario，未悄悄放宽。

## 5. 手验发现的缺陷与修复

**对话气泡显示 `{"reply":"..."}`**——C2 自身引入的缺陷。解析路径改为读 `message.content` 后，system prompt 仍保留 C1 的「只输出 JSON」要求，模型照做而后端不再剥壳。修复：输出要求改为直出自然语句 + `normalizeReplyShape` 形状兜底 + 回归守门测试。

用户二次复验仍见 JSON，经查为**运行实例未重启**（消息落库 `07-28 09:21` 晚于 class 编译 `08:56`），非修复无效；另用真实 provider 单独验证修复有效。

## 6. 遗留与已接受的限制

| # | 事项 | 归属 |
|---|---|---|
| R1 | **工具参数改写并增写用户原话**：真实返回的 `text` 含用户从未说过的句子，`askText` 自称「我帮你整理了一下」。触碰「不改写原文」硬约束。白名单与二段式只校验「能否执行」，不校验「参数是否忠实」 | **C4**（内容忠实度，建议前移） |
| R2 | 引导问题突兀（天气比喻在受阻时未退回具体情境）+ 素材把用户的反问与困惑也拼进正文 | 全阶段完工后统一优化（用户决定） |
| R3 | 微信端到端手验未走通完整工具链路 | 下一次真机验证时补 |
| R4 | 内容合规仍为 C1 单层 prompt + 长度裁剪 | C4 |
| R5 | `propose_unlock_at` 是否越过「建议不代决」仍待体感确认 | 可随时从 registry 移除 |
| R6 | 本地 5 项凭证需用户轮换（本轮 grep 范围过宽曾打印到终端）；`*.bak` 含旧明文 | 用户执行 |

## 7. 范围守护自检

- 未让 Agent 获得 seal / delete / unlock / location / cover / attachment / later-reflection 任何路径
- 未做后置内容过滤或违规降级（C4）；未做 Memory / 历史检索（C3）；未做决策链路查询端点（C5）
- 未改 C1 四个既有端点字段语义；未改 `complete()` 与三个单轮 AI 端点的 `json_object` 链路
- 未引入 FC → 自研协议的降级路径；未在回复生成过程内执行工具或回灌 tool 结果
- 未引入 MCP / Spring AI / LangChain4j；未改 package / lockfile
- 未改三 Tab、未改用户可见命名、未做视觉大改
- 未把日记原文长期写入审计表或日志
