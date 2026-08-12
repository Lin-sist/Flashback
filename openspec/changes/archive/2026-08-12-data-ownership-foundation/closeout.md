# P3.2 `data-ownership-foundation` Closeout

## 1. 结论

- Change：P3.2 `data-ownership-foundation`
- 开工锚点：`efd2618`
- 实现提交：`7b48086 feat: 实现P3.2数据所有权基础`
- 真实验收提交：`907feb4 test: 完成P3.2真实依赖验收`
- 收口日期：2026-08-12
- 授权：规划、实现、Gate 3a、Gate 3b、Gate 3c、Agent commit 与归档均由用户分别明确批准
- 结果：**ACCEPTED / ARCHIVED**。五份 delta 已接受进 baseline；归档后 `ACTIVE_TASK` 回到 `IDLE`
- 外部副作用：未 push、未部署、未发布；真实 Agent provider 调用保持 0

P3.2 已把“数据属于用户”从演示文案变成真实纵切：authenticated 用户可以获得离线可读、媒体可校验、Agent 内容分区明确的数据副本，也可以删除 DRAFT / SAVED / SEALED / UNLOCKED 单条记录或清除全部记录。删除先处理私有对象，再清理数据库聚合；失败与中断保留可重试状态，不用假成功掩盖剩余数据。

## 2. 已交付范围

- 新增 owner-scoped durable operation，统一承载 export、single-record deletion 与 clear-all；包含互斥、短期确认、幂等 confirm/retry、stale recovery 与 24h artifact expiry
- 导出包固定包含离线 `index.html`、records、media、agent、manifest 与 README；manifest 校验 bytes / SHA-256，用户原文与 Agent 内容物理分区
- 默认 `RESPECT_SEAL` 遮蔽未解锁 SEALED 内容；用户可显式选 `FULL_CONTENT` 完整取回，但不改变产品内状态或解锁语义
- DRAFT / SAVED / SEALED / UNLOCKED 使用同一删除流；远端对象 success/not-found 后才删除数据库聚合，provider failure 保留 record/item retry anchor
- clear-all 以确认时 owner snapshot 为固定范围，期间冻结 record / attachment / Agent record mutation；retry 不扩大原授权范围
- 小程序个人中心提供真实“数据与所有权”入口、双导出策略、真实状态/重试、四状态单删与 clear-all 强确认；Preview 全部 fail-closed
- 未给 Agent 增加 export/delete/clear-all 工具，未改变 prompt、provider、memory、guardrail、reflection 或调用预算

## 3. 验收证据

### 自动化、H2 与构建

- Gate 2 backend full：97 suites / 701 tests / 0 failures / 0 errors / 9 skipped
- Gate 3 后 backend full：**99 suites / 703 tests / 0 failures / 0 errors / 11 skipped**
- 新增 2 个真实依赖探针在普通测试中默认 skip；只有显式 Gate 环境变量才连接真实 MySQL / private object storage
- frontend：`vue-tsc --noEmit`、标准 mp-weixin build、Preview build 均 PASS；package/lockfile 零变化
- 离线 ZIP exact tree、无外链 HTML、Markdown、双 sealed policy、Agent 分区与 manifest bytes/SHA-256 自动验证 PASS

### Gate 3a：真实 MySQL

- MySQL 8.0.41；只读 preflight 仅输出 schema、状态计数与孤儿/owner mismatch 聚合，关联检查均为 0
- P3.2 migration 连续执行两次成功；postflight 为 2 tables / 4 foreign keys / 9 indexes
- owner scope、cross-owner 拒绝、单删幂等、clear-all 固定 snapshot、mutation freeze、record-linked cascade 与 stale RUNNING recovery 均 PASS
- 合成 user、record、operation 与 operation item 最终全部清理

### Gate 3b：真实私有对象存储

- 使用可清理的合成 1x1 PNG 与短 WAV，真实 upload/private read/export/delete；ZIP 原始 bytes 与 manifest SHA-256 一致
- 读取鉴权失败进入 `RETRY_REQUIRED` 且无 partial artifact；恢复凭据后 retry 成功
- 删除覆盖 success、not-found + stale restart、鉴权失败保留 retry anchor、恢复后 retry；artifact expiry 后状态为 `EXPIRED`
- finally cleanup 后合成对象、artifact、user、record 与 operation 全部移除

### Gate 3c：微信开发者工具

- standard 模式验证真实 `RETRY_REQUIRED`、clear-all 期间写入冻结、原操作重试、默认/完整导出、四状态单删与 clear-all 强确认
- 真实 ZIP 通过微信 `wx.saveFile` 保存，saved file 大小大于 0；验收后移除，没有用桌面下载或 build 代替
- Preview 显示 1/2/2/1 演示计数；默认/完整导出、单删与 clear-all 均 fail-closed，saved file 数不变，同一时间窗真实 MySQL 新增 operation 为 0
- standard 产生的 2 个 export、4 个 single-delete、2 个 clear-all operation 均为 `SUCCEEDED`；合成账号、记录、operation 与 2 个临时 ZIP 随后全部清理

## 4. Delta acceptance

五份 delta 已逐 requirement 精确接受进 baseline；本 change 只有 ADDED，没有 MODIFIED、REMOVED 或 RENAMED：

- `backend-core`：7 Requirements / 20 Scenarios
- `miniapp-core`：4 Requirements / 9 Scenarios
- `v2-product-scope`：3 Requirements / 8 Scenarios
- `agent-runtime`：3 Requirements / 6 Scenarios
- `agent-collaboration`：3 Requirements / 8 Scenarios

归档 delta 合计 **5 specs / 20 Requirements / 51 Scenarios**；exact-copy 校验 PASS，未引入新的重复 requirement 标题。

## 5. 明确保留的 PARTIAL / SKIPPED

- T-36 保持 **PARTIAL / 未勾选**：合成 18 个媒体 / 301,989,888 logical bytes 构包 PASS，artifact 299,156 bytes / 963ms；样本高度可压缩，未取得不可压缩真实媒体的构建过程内存/磁盘峰值
- OpenSpec CLI：本机不在 PATH；只完成 artifact、delta exact-copy、任务、链接、结构与文件级校验，不声称 CLI status/validate PASS
- 物理真机：未使用；Gate 3c 以授权范围内的微信开发者工具完成 `wx.saveFile` 与完整交互矩阵
- 真实 Agent provider：本 change 不改变 Agent 生成语义，验收进程强制 mock，真实外调 0；不证明 provider 可用性或语言质量
- 当前本机 MySQL、私有对象存储和微信开发者工具的小样本 PASS 不等于生产容量、并发、长期可用性或 SLA

## 6. 范围安全

- 未实现账号注销、身份解绑、自动云备份、恢复/import、iCloud、跨设备同步、PDF-only 发布、公开分享或生产灾备
- 未修改三个一级 Tab、“我的记录/时光轴/时间回看”命名、SEALED/UNLOCKED 内容不变性或 Preview 隔离
- 未新增 STT、声音分析、AI 评分/诊断/dashboard、设置页、生产通知中心、campaign 或新的 Agent 工具
- 未修改 package/lockfile、deployment、monitoring、admin portal 或冻结蓝图
- tracked evidence 不含用户日记原文、媒体内容、位置详情、storage key、signed URL、download/artifact token、credential、prompt 或 provider response

## 7. Remaining risks 与下一步

- 当前导出在单 worker 进程内聚合并构包；不可压缩大媒体的内存/磁盘峰值、微信文件上限与超时边界仍 unknown，必须由未来独立容量 change 处理
- 历史 `record_id IS NULL` 且缺少可信归属的派生数据不会通过文本猜测关联；当前策略是 fail-closed / repair-required
- 冻结蓝图的下一候选为 P4.1 `witness-agent-alignment`，但本次归档不授权下一阶段规划或实现
- push、PR、部署、发布仍需独立授权
