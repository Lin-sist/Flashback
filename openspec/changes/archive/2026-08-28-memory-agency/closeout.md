# P4.2 `memory-agency` Closeout

## 1. 结论

- Change：P4.2 `memory-agency`
- 开工锚点：`42548ce`
- 收口日期：2026-08-28
- 授权：Gate 1、Gate 2、Gate 3a/3b、delta acceptance 与归档均已由用户明确批准
- 结果：**ACCEPTED / ARCHIVED**。五份 delta 已接受进 baseline
- 提交边界：本轮未取得新的 Agent commit 授权，未 stage/commit/push/PR/deploy/release
- Provider：真实调用 0；本刀不声称语言质量或生产稳定性

P4.2 已把跨记录记忆从“配置开启且有 cue 即自动使用”收敛为按 session 默认关闭、用户可见授权、实际来源可见、未来可撤销的能力。REVIEW_CHAT 目标记录与其他历史分层；来源只保存结构化关系，不复制正文、片段、摘要、prompt 或回复。

## 2. 已交付范围

- session 级 `crossRecordMemoryEnabled` 默认 false；backend config 与用户 consent 同时为 true 才允许跨记录检索
- owner-scoped 授权 API，ACTIVE/pending retry fail-closed；切换不调用 provider、不推进 turn/stage、不执行工具或生成素材
- record 级 `agentMemoryExcluded` 与用户填写的 `agentMemoryContextNote`；AI 无写权限
- assistant message 与实际 final injected source 同主事务持久化；来源删除后 `SET NULL` 并显示 unavailable
- REVIEW_TARGET 与 CROSS_RECORD 分层；撤销、排除、删除对未来轮次立即生效
- Mini Program 增加局部授权、来源 chip、记录排除与说明；Preview 请求为 0，不伪造成功
- C6 固定场景覆盖 off/on/exclude/review-target/source；直接测试与真实 MySQL 覆盖 revoke/delete/owner/status/transaction

## 3. 验收证据

### 自动化与构建

- backend full：**106 suites / 756 tests / 0 failures / 0 errors / 14 skipped**
- frontend Standard/Preview `mp-weixin` build PASS；此前 Gate 2 的 type-check PASS
- `git diff --check` PASS；package/lockfile 与 denylist 范围检查 PASS
- OpenSpec CLI 不在 PATH：采用 artifact、task、delta、Requirement/Scenario 与 exact-copy 文件级校验，CLI validation 为 SKIPPED

### Gate 3a：真实 MySQL

- MySQL 8.0.41 preflight PASS；P4.2 migration 连续执行两次 PASS
- session/record policy columns、source table、默认值与 `ON DELETE SET NULL` exact-match
- 合成矩阵覆盖 authorization default/off/on/revoke、owner scope、status/exclusion/deletion eligibility、future-turn effect、source available/unavailable 与同事务 rollback
- finally cleanup 后 synthetic user/record/session/message/source/operation 聚合均为 0

### Gate 3b：微信开发者工具

- Standard：真实微信登录边界；默认关闭、开启/关闭、失败保持原状态、来源 available/unavailable、来源跳转、record policy/note 与删除入口 PASS
- Standard 的业务响应为 scripted interception，provider calls=0；只证明 UI/状态编排
- Preview：entry=true、sheet=false；total requests=0、memory-agency requests=0
- 物理真机 SKIPPED；开发者工具证据不外推为真机证据

## 4. Delta acceptance

五份 delta 均为 ADDED requirements，并已 exact-copy 进入 accepted baseline：

- `agent-collaboration`：5 Requirements / 12 Scenarios
- `agent-runtime`：5 Requirements / 14 Scenarios
- `backend-core`：5 Requirements / 16 Scenarios
- `miniapp-core`：4 Requirements / 12 Scenarios
- `v2-product-scope`：4 Requirements / 10 Scenarios

合计 **23 Requirements / 64 Scenarios**；既有 accepted requirements 未删除或改写。

## 5. PARTIAL / SKIPPED

- T-53 为 **PARTIAL**：revoke/delete/owner/status/transaction 已有直接测试与真实 MySQL 证据，但未全部复制成 C6 fixed fixtures；这不削弱已验证的运行时/数据库契约，仍作为评测覆盖债务保留
- 物理真机 SKIPPED；未验证键盘、滚动、触控与多尺寸矩阵
- 真实 provider 调用 0；未验证来源表述自然度、真实语言质量或长期隐私效果
- OpenSpec CLI SKIPPED；不声称 CLI status/validate PASS
- 本地合成样本与单账号开发者工具 PASS 不等于生产并发、容量、长期可用性或 SLA

## 6. 范围安全

- 未引入向量库、全文索引、画像、趋势、评分、诊断、建议清单、全局永久授权或 LLM-as-Judge
- 未新增 Agent 工具或扩大工具白名单；未削弱工具/素材/用户原文确认
- 保留三个一级 Tab、canonical naming、witness role 与 SEALED 不可变边界
- 未修改 package/lockfile、deployment、monitoring、admin portal 或冻结蓝图
- tracked evidence 不含用户日记、对话、context note、provider reply、prompt、source ID 列表、token、openid 或 secret

## 7. Remaining risks 与下一步

- T-53 的 C6 fixture 完整度可作为后续独立测试债务处理，不应与 R1 安全响应混改
- 物理真机交互仍需在发布准备阶段单独安排
- dev profile MyBatis DEBUG 参数日志风险仍是独立 Type B 隐私事项
- 冻结蓝图下一项为 R1 `safety-response-minimum`；必须以新的 Type C change 规划并重新核验地区资源
- commit、push、PR、部署与发布均未执行
