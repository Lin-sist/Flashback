# P3.1 `present-moment-capture` Closeout

## 1. 结论

- Change：P3.1 `present-moment-capture`
- 开工锚点：`2d9544a`
- 主实现提交：`3793f1a feat: 实现P3.1当下记录主路径`
- 收口日期：2026-08-12
- 授权：规划、实现、Gate 3a、Gate 3b、Gate 3c 与归档均由用户分别明确批准
- 结果：**ACCEPTED / ARCHIVED**。四份 delta 已接受进 baseline；`ACTIVE_TASK` 回到 `IDLE`
- 外部副作用：未 push、未部署、未发布；真实 AI provider 调用保持 0

P3.1 已交付“留下这一刻”的最小真实纵切：用户可以用文字、图片或声音形成可恢复的编辑中记录，将其显式保存为 `SAVED`，继续补充，最后“交给时间”进入既有封存边界。它没有扩展为数据所有权、批量管理或新的 Agent 语义。

## 2. 已交付范围

- 新增 `SAVED` 状态与 `MOMENT` 类型；显式、幂等的 `/save` 只接受有文字或 owner-scoped `AVAILABLE` 图片/声音的记录
- `DRAFT` 提供 7 天可恢复窗口与窄范围过期清理；旧有效 DRAFT 迁移到 `SAVED`，旧 `FUTURE_LETTER` 类型不被改写
- `DRAFT` / `SAVED` 支持既定字段、附件、封面与位置编辑；`SEALED` 后继续禁止修改 location、attachments、cover
- 小程序接通“留下这一刻”、安静的保存反馈、恢复草稿二选一、media-first、继续补充与“交给时间”；Preview 继续只读、fail-closed
- Agent 仅让 WRITING_GUIDANCE 复用 `DRAFT` / `SAVED` 可编辑资格；未修改 prompt、provider、memory、guardrails、reflection、预算或 eval snapshot 语义

## 3. 验收证据

### 自动化与构建

- 实现期 backend full：91 suites / 687 tests / 0 failures / 0 errors / 8 skipped
- Gate 3b 后默认 backend full：**92 suites / 688 tests / 0 failures / 0 errors / 9 skipped**
- Gate 3b 默认 focused：19 tests / 0 failures / 0 errors / 1 skipped
- Gate 3b 真实对象存储探针：1 test / 0 failures / 0 errors / 0 skipped
- frontend：`vue-tsc --noEmit`、标准 mp-weixin build、Preview build 均 PASS

### Gate 3a：真实 MySQL

- preflight 仅输出聚合：3 条有效旧 DRAFT、0 条 owner-scoped AVAILABLE 媒体、0 条空白异常、0 条 orphan/owner mismatch
- 真实迁移后 3 条旧 DRAFT -> `SAVED`，原 `FUTURE_LETTER` 保留；`draft_expires_at`、复合索引与 `MOMENT` 默认值符合契约
- 第二次执行聚合状态不变；索引 EXPLAIN、backend list/timeline 读取均 PASS
- 数据库与新加坡时区同为 UTC+8；focused 18 tests / 0 failures / 0 errors / 0 skipped

### Gate 3b：真实私有对象存储

- 合成 PNG 与固定短 WAV 的 authorize/upload/commit AVAILABLE/save/private read/删除 PASS；图片字节一致，声音可被 JVM 标准解码
- pending、missing object 均不能保存；过期 DRAFT 的远端删除成功、对象已不存在幂等、鉴权失败 RETRY 与恢复后 DELETED 均 PASS
- 探针结束后远端对象全部不存在，合成 user/record/attachment 聚合均为 0；未上传用户内容，未记录 credential、object key 或 signed URL

### Gate 3c：微信开发者工具 / 真机人工矩阵

- 用户报告除 Agent 对话外其余清单均正常；据此登记文字、图片、声音独立保存与播放、返回恢复、`SAVED` 编辑、保存后交给时间、权限拒绝、上传失败与重试为人工 PASS
- Agent 请求到达 backend、会话创建成功，但验收进程被主动强制为 `app.ai.provider=mock`，opening/retry 均以 `auth-configuration` 0ms fail-closed；这是守住本 change 真实 provider 调用 0 的预期边界，不是 P3.1 回归

## 4. Delta acceptance

四份 delta 已逐 requirement 精确接受进 baseline，未引入新的重复 requirement 标题：

- `backend-core`：6 MODIFIED + 3 ADDED
- `miniapp-core`：6 MODIFIED + 5 ADDED
- `agent-runtime`：2 MODIFIED + 1 ADDED
- `v2-product-scope`：5 ADDED

归档 delta 合计 4 specs / 28 Requirements / 98 Scenarios；没有 REMOVED requirement。

## 5. 明确保留的 SKIPPED / INCONCLUSIVE

- E0 目标用户理解：没有真实参与者，保持 **INCONCLUSIVE / SKIPPED**；内部走查、自动化和真机功能 PASS 不冒充用户研究
- 真实 Agent provider：P3.1 未获真实 provider 外调授权，调用数为 0；mock fail-closed 不证明真实 provider 可用性或语言质量
- OpenSpec CLI：本机不在 PATH，只完成 delta exact-copy、任务完整性、链接、结构与文件级校验，不声称 CLI validation PASS
- 本地 MySQL、私有对象存储与当前微信环境的通过结果不等于生产兼容性、并发行为或 SLA 已证明

## 6. 范围安全

- 未实现 P3.2 的导出、任意状态删除、清除全部、账号注销或完整数据删除编排
- 未新增 STT、声音分析、自动标签、评分、诊断、dashboard、推送、设置页或新的 AI 能力
- 未修改三个一级 Tab、canonical naming、Preview 隔离或 SEALED/UNLOCKED 不变性
- 未修改 package/lockfile、deployment、monitoring、admin portal 或冻结蓝图
- 未把用户日记原文、媒体内容、位置、storage key、signed URL、secret、prompt 或 provider response 写入 tracked evidence

## 7. Remaining risks 与下一步

- 保存反馈、恢复入口与产品语言仍是缺少真实参与者证据的 provisional 基线
- 真实 Agent provider 可用性未在本 change 验证；若未来验证，必须单独授权外调并使用固定合成短文本
- P3.2 `data-ownership-foundation` 仍只是冻结蓝图中的下一候选阶段；必须重新走独立规划闸门，不因本次归档自动 active
- push、部署、发布仍需独立授权
