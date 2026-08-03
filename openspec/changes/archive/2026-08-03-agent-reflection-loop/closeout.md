# Closeout：Agent Reflection Loop（C7）

> Change ID：`agent-reflection-loop`
> 开工锚点：`b459b8f`｜实现提交：`8a2dbb4`（+ `1eb87f2` 证据补录）
> 闸门：1 已批准 → 2 已授权 → **3 未授权、未执行**
> 归档日期：2026-08-03｜**Phase 2 第二刀**

## 1. 交付结果

C7 在后端建立了一个 **reply-only、最多一次、由确定性护栏驱动**的受控反思环。
只有非 `CLOSING` reply 的 `MISSING_TIME_ATTRIBUTION` 可以进入重写；material、tool、
`CLOSING` reply、不可恢复违规与 provider error 均不开环。

- 新增类型化 `AgentReflectionPolicy` 与最小 `AgentReplyPipeline`。
- reflection 调用固定 `tools=[]`、strict=false，重写后重新运行完整 reply 护栏。
- 同一请求仍是一条 turn trace、同一 attempt；provider step 以 `initial|reflection` 区分。
- C6 合成用例 23→28；只接受 5 条由 C7 合法引起或新增的快照变化，并同步 `baselineNote` 与 checksum。
- 不改 API、DTO、DDL、前端、超时、依赖或 lockfile。

## 2. 验证结果

- focused policy / pipeline / trace / material 边界 / C6 eval：PASS。
- 后端全量：**622 tests PASS / 4 skipped，0 failures / 0 errors**。
- provider 调用预算测试：非 `CLOSING` 普通≤1、eligible≤2；`CLOSING` 仍≤2。
- `git diff --check` 与敏感内容边界扫描：PASS。
- 真实 provider 调用：**0 次**。

## 3. 验收接受的 SKIPPED 与残余风险

用户于 2026-08-03 在已收到以下风险说明后明确要求归档；这些项目仍是 **SKIPPED**，
不得在后续文档中写成 PASS：

| 项 | 状态与影响 |
|---|---|
| 真实 MySQL reflection 联调 | 未建立夹具、未执行；H2 结果不冒充 MySQL |
| 闸门 3 真实 provider canary | 未授权、未执行；真实网络延迟与模型重写效果未活体验证 |
| 真机 C7 验收 | 未执行；前端协议/UI 零改动仅由代码边界与回归测试支持 |
| C6 narrative anchors 人评 | 仍为空；没有语言质量绝对结论 |
| OpenSpec CLI validate | CLI 不在当前环境 PATH；仅完成文件级结构与 delta 对齐检查 |

## 4. 关键取舍

1. **由确定性 checker 判定，不增加 LLM 自检器**：避免额外一次约 6.5s 调用，也不以不确定判定替换确定性护栏。
2. **最终收窄为 reply-only**：`CLOSING` 原本已有 reply + material 两次调用；material 再重写会达到三次并逼近 20s 后端上限。
3. **eligible 集合封闭**：仅 `MISSING_TIME_ATTRIBUTION`；`CHECK_ERROR` 与严重越界继续 fail-closed。
4. **reflection 不是新 attempt**：它是同一业务请求内的 provider 子阶段，保留一轮一条 trace 的语义。
5. **不自动刷新 C6 快照**：变化必须逐条人工归因，防止评测变成橡皮图章。

## 5. Delta 接受位置

| Baseline spec | 接受内容 |
|---|---|
| `agent-runtime` | reply-only 受控环、封闭 eligibility、类型化无内容指令、护栏复检、非错误重试、轨迹与 C6 可比性 |
| `backend-core` | 后端预算、调用边界、单 trace/attempt 与内容安全证据 |
| `v2-product-scope` | 允许恢复的 reply 可重写一次；产品表面与气质不变 |
| `agent-collaboration` | 无内容证据、快照需审查、真实探针独立授权、发现不得扩 scope |
| `miniapp-core` | 无 delta |

## 6. 下一刀

默认进入 C8 `agent-resilience` **规划闸**。C8 设计必须把 C7 已占用的最坏两次 provider 调用预算作为输入，
不得把 provider error retry、超时分类或多 provider 路由倒灌回已归档 C7。
