# 《时光回序》Flashback V2.0 项目事实报告

> **报告性质**：基于代码、配置、测试与 OpenSpec 历史工件的客观事实审计，用于求职简历优化、技术深度对齐与面试深度追问准备。  
> **审查准则**：代码所见即所得，不推测、不美化、不粉饰。不确定的如实标为“未确认”或“未实现”。  
> **基线分支**：`main`（Commit: `b6afb01`）｜ **审计日期**：2026-10-11

---

## 报告摘要（Executive Summary）

1. **项目定位**：基于微信小程序（Uniapp/Vue3）与 Spring Boot 3.3.5 的安静克制情绪胶囊与见证者日记产品，强调当下记录、时间封存与数据主权。
2. **AI 定位与边界**：自研轻量 Agent Loop（见证者/友人回看），拒绝效率诊断/大盘/主动推送；坚守“AI退到幕后，记录属于用户”。
3. **技术栈事实**：Java 17 + Spring Boot 3.3.5 + MyBatis 3.0.3 + MySQL 8.0 + AWS SDK v2 (S3) + JJWT 0.12.6；**无 Spring AI、无 LangChain、无向量数据库（无 Milvus/Qdrant）**。
4. **AI 核心实现**：原生 Function Calling + 状态机编排；确定性 4-gram 忠实度与内容合规双指标护栏；MySQL 结构化记忆分层检索；受控单次反思环；共享 24s 预算零自动重试韧性。
5. **工程严谨性**：严格所有权（Owner-scoped）与封存不可变约束；远端对象优先的持久化操作（Durable Operation）防孤儿文件；全链路超时阶梯（前端30s > 后端24s > Provider 20s）。
6. **测试与评测**：后端 108 个测试套件，801 项测试用例，16 项环境探针 skipped；自研离线轨迹评测框架（37 条合成用例与快照基线，77 项断言 3.3s 跑完，带 checksum 防橡皮图章）。
7. **关键事实澄清**：**本项目无 Milvus**（用户提到的 Milvus 主键映射属于其另一 RAG 项目或规划期已否决草案，Flashback 唯一存储为 MySQL 8.0）。
8. **代码规模**：后端主代码 22,979 行，后端测试 22,191 行，前端代码 19,354 行；全项目 11 个 Controller，53 个 API 接口。

---

## 1. 项目概况

### 1.1 一句话定位与产品哲学
《时光回序》（Flashback）是一个微信小程序日记产品。用户在第一人称当下写下情绪与困惑，把回答权交给时间（封存至未来解锁）。Agent 在其中定位为**“有温度的见证者”与“被动召唤的友人”**，不代替用户决策，不做心理诊断，不打效率标签，数据完全归用户所有。
- 事实依据：[`Docs/agent-iteration/roadmap/core-product-definition.md`](file:///c:/_01_Code/Flashback/Docs/agent-iteration/roadmap/core-product-definition.md) v0.1；[`AGENTS.md`](file:///c:/_01_Code/Flashback/AGENTS.md)。

### 1.2 目标用户
生命处于转折期、情绪波动或需要私密自我对话的用户；需要安全位置留下当下、不希望被社交流或打卡机制绑架的独立记录者。

### 1.3 核心功能实现状态对照表

| 核心功能模块 | 具体能力与用户可见路径 | 实现状态 | 事实依据（代码路径 / 类 / 方法） |
|---|---|---|---|
| **当下记录** | 文字、图片、原始语音留下；草稿与正式保存（SAVED/MOMENT） | **已完整实现** | [`RecordController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/RecordController.java) (`saveRecord`), [`RecordServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java) |
| **时光封存与解锁** | 设置解锁时间、封存不可变（内容/媒体/位置冻结）、每分钟定时解锁调度 | **已完整实现** | [`RecordController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/RecordController.java) (`sealRecord`), [`RecordUnlockScheduler.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/schedule/RecordUnlockScheduler.java) |
| **微信订阅消息通知** | 解锁时调用微信 API 发送服务通知提醒用户 | **已完整实现** | [`WechatSubscribeMessageHttpClient.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/wechat/WechatSubscribeMessageHttpClient.java) |
| **多媒体对象存储** | 图片与原始音频直传凭证签发、下载签名 URL、远端物理删除 | **已完整实现** | [`RecordAttachmentController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/RecordAttachmentController.java), [`S3ObjectStorageProvider.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/storage/s3/S3ObjectStorageProvider.java), [`QiniuObjectStorageProvider.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/storage/qiniu/QiniuObjectStorageProvider.java) |
| **见证者 Agent Loop** | 写作引导会话、多轮推进、意图切换（LISTEN/UNTANGLE）、素材整理 | **已完整实现** | [`AgentChatServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java) (`sendMessage`), [`AgentWitnessTurnPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentWitnessTurnPolicy.java) |
| **Agent 工具调用** | 原生 FC、白名单校验、二段式用户确认（贴标签、追加正文、建议封存） | **已完整实现** | [`AgentToolCoordinator.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/tool/AgentToolCoordinator.java), [`AgentController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/AgentController.java) (`confirmToolCall`) |
| **友人回看对话** | 在已解锁记录上被动召唤回看对话，只读回看目标，无工具、不产素材 | **已完整实现** | [`AgentChatServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java) (`startOrResume`, purpose=`REVIEW_CHAT`) |
| **分层记忆检索** | 基于 MySQL 标签+时间窗+说明性字段匹配，正文不外发 | **已完整实现** | [`MySqlMemoryPort.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MySqlMemoryPort.java), [`RecordMapper.xml`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/RecordMapper.xml) (`selectMemoryCandidates`) |
| **记忆自主权控制** | 会话级授权默认关闭、单条记录排除记忆、回答来源透明化挂载 | **已完整实现** | [`AgentController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/AgentController.java) (`switchMemoryAuthorization`), [`AgentMemorySourceMapper.xml`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/AgentMemorySourceMapper.xml) |
| **时间智能衰减** | 记忆时间距离划分四档（近期/数月前/更早/未知），远期预算衰减，复现提示门控 | **已完整实现** | [`AgentTemporalPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/temporal/AgentTemporalPolicy.java) |
| **确定性护栏** | 4-gram 覆盖率+未覆盖片段双指标拦截、诊断词/假动作拦截、时间归属检查 | **已完整实现** | [`AgentGuardrailPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentGuardrailPolicy.java), [`AgentContentChecker.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentContentChecker.java) |
| **受控反思重写环** | 仅非 CLOSING 的时间归属违规允许重写 1 次，工具关闭，不自检、不无限死循环 | **已完整实现** | [`AgentReplyPipeline.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/reflection/AgentReplyPipeline.java), [`AgentReflectionPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/reflection/AgentReflectionPolicy.java) |
| **最小安全响应** | 高置信自伤风险本地确定性规则抢占（0外调、0记忆、0工具），直出求助热线 | **已完整实现** | [`AgentSafetyPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/safety/AgentSafetyPolicy.java) |
| **决策轨迹与评测** | Versioned 决策轨迹脱敏落库；37 条快照与硬不变量离线评测套件 | **已完整实现** | [`AgentTraceCollector.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/trace/AgentTraceCollector.java), [`AgentEvalHarness.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalHarness.java) |
| **数据所有权管理** | ZIP 离线物理导出、四状态单删、原子清空（远端优先删除 + 崩溃重试恢复） | **已完整实现** | [`DataOwnershipServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/DataOwnershipServiceImpl.java), [`DataOwnershipController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/DataOwnershipController.java) |
| **时间篇章 (P5.x)** | 篇章管理、记录加入/转移、版本号控制、显式结束与重开 | **已完整实现** | [`TimeChapterServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/TimeChapterServiceImpl.java), [`TimeChapterController.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/TimeChapterController.java) |
| **演示模式 (Preview)** | 小程序免登录可体验核心界面，纯本地 Mock 数据拦截，与真实请求绝对隔离 | **已完整实现** | [`frontend/src/features/preview/preview-session.ts`](file:///c:/_01_Code/Flashback/frontend/src/features/preview/preview-session.ts), [`agentService.ts`](file:///c:/_01_Code/Flashback/frontend/src/services/agentService.ts) (`requireRealSession`) |
| **向量数据库与语义检索**| 基于 Milvus / Qdrant / pgvector 的向量 Embeddings 召回 | **明确否决/未实现** | 架构决策否决（见 Blueprint §9.5），`pom.xml` 无依赖，SQL 检索足以覆盖当前量级 |
| **流式打字机输出 (SSE)**| Agent 对话 SSE 或 WebSocket 分块流式下发 | **未实现** | 统一采用普通 HTTP POST 响应，`completeWithTools` 固定 `"stream": false` |
| **语音识别转文字 (STT)**| 录音转写为文本日记 | **架构排除** | `AGENTS.md` 明确非目标：不破坏用户第一人称现场 |
| **心理诊断与情绪仪表盘**| 心理量表评分、情绪走势大盘分析 | **架构排除** | `AGENTS.md` 明确非目标：系统不扮演医生，不做监控式大盘 |
| **API 级全局限流** | 基于 Redis 令牌桶或漏桶的接口限流 | **未实现** | `pom.xml` 引入 Redis 但代码未实际使用，限流仅靠业务层会话轮次约束 |

---

## 2. 技术栈与版本

### 2.1 依赖与版本（Maven `pom.xml`）
- **JDK 版本**：Java 17 ([`backend/pom.xml#L21`](file:///c:/_01_Code/Flashback/backend/pom.xml#L21))
- **核心框架**：Spring Boot `3.3.5` ([`backend/pom.xml#L10`](file:///c:/_01_Code/Flashback/backend/pom.xml#L10))
- **ORM / 持久层**：MyBatis Spring Boot Starter `3.0.3` ([`backend/pom.xml#L38`](file:///c:/_01_Code/Flashback/backend/pom.xml#L38))
- **数据库驱动**：MySQL Connector/J（runtime）([`backend/pom.xml#L42`](file:///c:/_01_Code/Flashback/backend/pom.xml#L42))
- **缓存组件**：Spring Boot Starter Data Redis（**注意：仅引入依赖，代码无实际调用**）([`backend/pom.xml#L47`](file:///c:/_01_Code/Flashback/backend/pom.xml#L47))
- **对象存储 SDK**：AWS Java SDK v2 S3 `2.25.60` (`software.amazon.awssdk:s3`，用于标准 S3 SigV4 兼容存储与私有预签名) ([`backend/pom.xml#L57`](file:///c:/_01_Code/Flashback/backend/pom.xml#L57))
- **鉴权与加密**：JJWT `0.12.6` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) ([`backend/pom.xml#L64`](file:///c:/_01_Code/Flashback/backend/pom.xml#L64))；jBCrypt `0.4` ([`backend/pom.xml#L52`](file:///c:/_01_Code/Flashback/backend/pom.xml#L52))
- **测试依赖**：Spring Boot Starter Test、H2 Database (`test` scope) ([`backend/pom.xml#L85`](file:///c:/_01_Code/Flashback/backend/pom.xml#L85))

### 2.2 前端技术栈（`frontend/package.json`）
- **开发框架**：Uniapp (`@dcloudio/uni-app: 3.0.0-5000720260410001`) + Vue `3.4.21` + Pinia `2.1.7`
- **语言与构建**：TypeScript `^5.4.5` + Vite `5.2.8` + vue-tsc `^2.0.6`
- **目标平台**：微信小程序 (`mp-weixin`)

### 2.3 外部服务与实际配置参数（`application.yml`）
- **LLM Provider**：
  - 默认值：`app.ai.provider: mock`（可通过环境变量 `AI_PROVIDER` 指定）
  - 实际 Base URL：`https://api.deepseek.com` ([`application.yml#L79`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L79))
  - 实际模型：`deepseek-v4-pro` ([`application.yml#L81`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L81))
  - Function Calling 白名单：`deepseek-v4-pro,deepseek-v4-flash` ([`application.yml#L50`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L50))
  - 单次 Provider 调用超时：`20000ms` (20s) ([`application.yml#L87`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L87))
  - 编排共享预算总超时：`24000ms` (24s) ([`application.yml#L31`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L31))
  - 前端请求超时：`30000ms` (30s) ([`agentService.ts#L112`](file:///c:/_01_Code/Flashback/frontend/src/services/agentService.ts#L112))
- **向量数据库**：**无**（未引入任何向量库 SDK 或远端集群）。
- **对象存储 Provider**：
  - 支持 `qiniu`（七牛云）与 `s3`（AWS S3 / MinIO / 兼容 OSS / COS）双实现。
  - 上传与下载签名 TTL 默认均为：`600s` (10分钟) ([`application.yml#L108-L109`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L108-L109))
- **微信小程序服务端**：
  - Code2Session: `https://api.weixin.qq.com/sns/jscode2session` (超时 5000ms)
  - 订阅消息推送: `https://api.weixin.qq.com/cgi-bin/message/subscribe/send`

---

## 3. 架构设计与数据模型

### 3.1 后端模块结构
```text
backend/src/main/java/com/flashback/
├── agent/                  # Agent 运行时与核心编排
│   ├── guardrail/          # 确定性合规、忠实度与时间归属护栏
│   ├── memory/             # 记忆抽取与 MySQL 记忆端口 (MemoryPort)
│   ├── reflection/         # 受控反思重写管线 (AgentReplyPipeline)
│   ├── resilience/         # 8 类封闭故障分类与调用预算 (AgentCallBudget)
│   ├── safety/             # 高置信自伤风险本地抢占策略
│   ├── temporal/           # 时间距离分档与衰减策略
│   ├── tool/               # 原生 FC 工具注册、校验与协调器
│   └── trace/              # 无原文决策轨迹收集与落库
├── common/                 # 统一 API 响应体、通用异常与错误码
├── config/                 # 属性配置类 (AppAiProperties, AppAgentProperties等)
├── controller/api/         # 11 个 API 控制器 (RESTful JSON 交互)
├── domain/                 # 业务实体 (Record, AgentSession, DataOperation 等)
├── dto/ & vo/              # 请求入参 DTO 与视图对象 VO
├── mapper/                 # MyBatis Mapper 接口 (与 resources/mapper XML 映射)
├── schedule/               # 4 个后台定时任务 (解锁、草稿清理、导出清理、恢复任务)
├── security/               # JWT 拦截器、Token 解析与微信认证
├── service/impl/           # 核心业务实现层 (AgentChatServiceImpl 等)
├── storage/                # S3 与 Qiniu 对象存储提供方抽象
└── wechat/                 # 微信 Code2Session 与订阅消息 HTTP 客户端
```

### 3.2 核心调用链路（文字时序）

#### 链路 1：见证者 Agent 对话与受控重写链路
1. **客户端发起**：前端发送 `POST /api/agent/sessions/{id}/messages`（超时 30s）。
2. **鉴权与所有权检查**：`JwtAuthenticationInterceptor` 校验 Token；`assertOwnershipWritable(userId)` 检查用户未被冻结。
3. **预算初始化与同轮重试检查**：启动 `AgentCallBudget(24000ms)`；若检测到上一次请求留下 `pendingUserMessage`，则复用该轮次与输入，避免重复持久化。
4. **安全抢占（R1）**：`AgentSafetyPolicy.assess(content)` 评估当前输入。若命中高置信自伤风险，**立即短路**：落库本地常量救助回复，Provider 调用数、记忆注入数、工具调用数均为 0，直接返回。
5. **记忆检索与时间衰减（C3/C9/P4.2）**：
   - 检查 `memory.enabled` 与用户会话级授权 `crossRecordMemoryEnabled`（双闸开启才执行跨记录检索）。
   - `MemoryCueExtractor` 从最近用户表达中提取 2~6 个关键词。
   - `MySqlMemoryPort` 查库（排除 SEALED 记录、排除当前记录、排除标记排除记录）。
   - `AgentTemporalPolicy` 评估时间跨度，对远期辅助记忆按 75% / 50% 预算裁剪字符；若满足 `>=90天` 且包含“又/反复”等线索，生成 recurrence 提示。
6. **提示词组装（C1/C2）**：`AgentPromptBuilder` 拼接 System 角色设定、五条护栏、可用标签工具定义、记忆片段与最近 12 条正序历史。
7. **模型生成与工具抽取**：`AgentModelClient.completeWithTools` 调用 Provider（`stream: false`，超时取 `min(20000ms, remainingBudget)`）。
8. **护栏校验与反思环（C4/C7）**：
   - `AgentContentChecker` 拦截诊断用词与虚假动作；`AgentFaithfulnessChecker` 进行 4-gram 覆盖率比对。
   - `AgentTimeAttributionChecker` 检查历史复述是否包含时间锚点。
   - 若非 CLOSING 且命中 `MISSING_TIME_ATTRIBUTION`，触发 `AgentReplyPipeline`：由后端注入重写指令，**关闭工具重新调用 1 次 Provider**。若重写仍不合格，降级为本地安全兜底。
9. **工具提议缓冲（C2）**：若模型输出合规的工具调用，`AgentToolCoordinator` 校验参数并以 `PROPOSED` 状态落库 `agent_tool_call`（瞬态参数暂存，**不立即执行**）。
10. **落库与轨迹出口**：落库 `AgentMessage` (ASSISTANT) 及 `AgentMemorySource` 关系；finally 块由独立事务执行 `traceSink.persist(trace)` 记录无原文决策轨迹；返回完整 `AgentSessionVO`。

#### 链路 2：此刻记录 → 封存冻结 → 到期自动解锁 → 微信提醒
1. **记录创建与保存**：用户调用 `POST /api/records` 创建，状态为 `DRAFT` 或 `MOMENT`；可调用 `POST /api/records/{id}/save` 固化为 `SAVED`。
2. **多媒体上传**：调用 `/upload-auth` 获取私有存储上传凭证与 StorageKey，直传对象存储后调用 `/complete` 确认附件可用。
3. **显式封存**：用户选择未来解锁时间后调用 `POST /api/records/{id}/seal`。数据库将记录状态变为 `SEALED`，记录 `sealed_at` 与 `unlock_at`。封存后触发约束：禁止增删改附件、位置与封面。
4. **后台轮询解锁**：`RecordUnlockScheduler` 每分钟（Cron: `0 */1 * * * *`）执行 `runUnlockJob`：
   - 查询 `status = 'SEALED' AND unlock_at <= NOW()` 的到期记录。
   - 批量更新为 `status = 'UNLOCKED'`, `unlocked_at = NOW()`。
5. **微信模板消息下发**：解锁任务提取 `record_reminder` 中的订阅授权，调用微信 `subscribe/send` 接口发送解锁提醒，并写入 `unlock_notice_log`。
6. **友人回看激活**：记录变为 `UNLOCKED` 后，时光轴解锁阅读，用户可在详情页开启 `REVIEW_CHAT` 模式进行时间回看。

#### 链路 3：数据所有权导出与远端优先清除链路
1. **操作创建**：客户端调用 `POST /api/data-ownership/operations`，传入 `operation_type` (`EXPORT` 或 `CLEAR_ALL`)，返回带有效期的确认凭证 Nonce。
2. **操作确认与冻结**：用户提交 Nonce Hash 确认操作，后端生成 `data_operation` 并在 `data_operation_record` 冻结目标记录快照。状态变为 `PENDING`，同时全局冻结当前用户新增日记与 Agent 交互。
3. **远端对象优先删除（针对清除）**：
   - 遍历快照中的所有附件，先调用 S3/Qiniu 接口删除云端物理对象。
   - 必须确认云端返回 `SUCCESS` 或 `NOT_FOUND` 后，才允许推进下一步。
4. **数据库物理级联清理**：远端对象全数删除后，在单个数据库事务内级联删除 `record`、`agent_session`、`record_attachment` 等业务行。
5. **离线 ZIP 生成（针对导出）**：生成 ZIP 归档，物理划分为 `records/`（JSON/Markdown）、`media/`（二进制文件）、`agent/`（对话记录），附带含 SHA-256 校验和的 `manifest.json`。
6. **故障恢复保底**：若进程中途崩溃，`DataOwnershipOperationRecoveryJob`（每 60s）扫描挂起操作并沿着未完成条目继续推进，防止孤儿文件或假完成。

### 3.3 数据库表结构全览（MySQL 8.0 InnoDB）

| 表名 | 业务用途 | 核心关键字段与设计要点 | 索引与唯一键约束 |
|---|---|---|---|
| `user` | 用户主表 | `id`, `openid`, `username`, `status` | `uk_user_openid`, `uk_user_username` |
| `record` | 记录主表 | `id`, `user_id`, `status` (`DRAFT/SAVED/MOMENT/SEALED/UNLOCKED`), `unlock_at`, `sealed_at`, `ai_summary`, `agent_memory_excluded` | `uk_record_id_user_id (id, user_id)` (复合所有权键), `idx_record_status_unlock_at` |
| `record_location` | 位置信息表 | `id`, `record_id`, `user_id`, `latitude`, `longitude`, `name`, `address` | `uk_record_location_record (record_id)` (1对1约束) |
| `record_attachment` | 附件表 (图/音) | `id`, `record_id`, `user_id`, `storage_provider`, `bucket`, `storage_key`, `status` | `uk_record_attachment_storage_key (storage_provider, bucket, storage_key)` |
| `tag` / `record_tag` | 标签及关联 | `name`, `type`, `record_id`, `tag_id` | `uk_tag_name_type`, `uk_record_tag (record_id, tag_id)` |
| `reply` | 解锁后轻回复 | `id`, `record_id`, `content`, `reply_type` | `uk_reply_record_id (record_id)` |
| `record_reminder` | 微信订阅授权 | `id`, `record_id`, `template_type`, `reminder_status` | `uk_record_reminder_record_template (record_id, template_type)` |
| `unlock_notice_log` | 解锁通知审计 | `id`, `record_id`, `user_id`, `notice_type`, `notice_status` | `idx_unlock_notice_record_id` |
| `time_chapter` | 时间篇章表 (P5.x) | `id`, `user_id`, `name`, `note`, `status` (`ACTIVE/ENDED`), `version` (乐观锁), `ended_at` | `uk_time_chapter_id_user_id (id, user_id)`, `ck_time_chapter_status_ended_at` (CHECK 约束) |
| `time_chapter_record` | 篇章与记录关联 | `chapter_id`, `record_id`, `user_id`, `added_at` | `PRIMARY KEY (chapter_id, record_id)`, `uk_time_chapter_record_record_id (record_id)` (第一版1条记录归属单篇章) |
| `agent_session` | Agent 会话表 | `id`, `user_id`, `record_id`, `purpose` (`WRITING_GUIDANCE/REVIEW_CHAT`), `stage`, `status`, `turn_count`, `cross_record_memory_enabled` | `idx_agent_session_user_status`, `idx_agent_session_record_status` |
| `agent_message` | Agent 消息表 | `id`, `session_id`, `user_id`, `role` (`USER/ASSISTANT`), `turn_no`, `stage`, `content` | **`uk_agent_message_session_turn_role (session_id, turn_no, role)`** (重试幂等基石) |
| `agent_memory_source` | 消息记忆来源表 | `id`, `session_id`, `assistant_message_id`, `source_record_id`, `source_kind` (`REVIEW_TARGET/CROSS_RECORD`) | `uk_agent_memory_source_message_record_kind` |
| `agent_tool_call` | 工具提议与审计 | `id`, `session_id`, `turn_no`, `tool_name`, `status` (`PROPOSED/EXECUTED/REJECTED`), `pending_args` (瞬态缓冲), `args_digest` (结构化摘要) | `idx_agent_tool_call_session_status` |
| `agent_turn_trace` | 决策轨迹表 | `id`, `trace_id` (UUID), `session_id`, `turn_no`, `attempt_no`, `prompt_version`, `policy_version`, `outcome`, `steps_json` | `idx_agent_turn_trace_session (session_id, turn_no, attempt_no)` |
| `data_operation` | 数据持久操作表 | `id`, `user_id`, `operation_type`, `status`, `active_slot` (生成列), `artifact_token`, `confirmation_nonce_hash` | **`uk_data_operation_user_active (user_id, active_slot)`** (单用户唯一活动槽), `uk_data_operation_artifact_token` |
| `data_operation_record` | 操作条目快照表 | `id`, `operation_id`, `record_id`, `item_status`, `attempt_count` | `uk_data_operation_record_target (operation_id, record_id)` |

---

## 4. AI 相关核心实现（重点）

### 4.1 Prompt 模板与组装逻辑
- **位置**：[`com.flashback.agent.AgentPromptBuilder`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentPromptBuilder.java)
- **组装流**：
  1. **Role Setting**（常驻见证者人设）：
     > “你是《时光回序》里有温度的见证者。你在场，但不替用户解释、决定或完成表达。先回应用户实际说出的内容；理解不确定时可以说明‘我可能理解得不完全’，把修正权留给用户。不自称朋友或伴侣，不承诺一直陪伴……不把一次表达固化成人格、人生阶段或诊断……”([`AgentPromptBuilder.java#L34-L39`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentPromptBuilder.java#L34-L39))
  2. **Guardrail Clause**：动态拼接 5 条后置硬护栏声明（来自 `AgentGuardrailPolicy.guardrailClause()`）。
  3. **回复长度上限**：“回复长度硬上限：120 个字符以内。”
  4. **本轮交互边界（Typed Turn Instruction）**：
     - `REFLECT_ONLY`: “回应已经听见的内容，留出继续或停下的空间，不提问题。”
     - `MAY_ASK_ONE`: “先回应，再至多问一个具体且可跳过的问题；没有必要就不问。”
     - `CLOSE`: “温和收束，不挽留，不要求结论，也不提新问题。”([`AgentPromptBuilder.java#L393-L399`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentPromptBuilder.java#L393-L399))
  5. **格式硬约束**：明确要求自然语言输出，禁止输出 JSON，禁止分析标签。附带 JSON 剥壳兜底 `normalizeReplyShape` 防止模型包一层 `{"reply":"..."}`。
  6. **只读草稿引用**：若存在当前草稿正文，截取前 300 字符作为只读注入（`DRAFT_EXCERPT_LABEL`）。
  7. **工具补充段 (Tool Supplement)**：注入当前系统支持的标签清单，以及前几轮已执行操作的结构化摘要（防止重复提议）。
  8. **记忆补充段 (Memory Supplement)**：逐条注入带时间元数据的片段：“他以前写下的片段：[-时间标签-·记录X] 片段内容（用户后来说明：语境说明）”。
  9. **时间距离补充段 (Temporal Supplement)**：注入时间理解约束，明确“这是离现在较近/已隔了一段时间/已过去较久，不要把它说成此刻的确定状态”。
  10. **历史消息截取**：采用滑动窗口截取最近 12 条对话消息（`context-message-window: 12`）。

### 4.2 记忆检索逻辑与具体参数

| 逻辑环节 | 实际实现机制 | 代码具体参数值 | 代码位置 |
|---|---|---|---|
| **线索提取 (Cue)** | 仅从用户原话提取，按标点空白正则切词，过滤 44 个高频虚词 | 窗口 `cueMessageWindow=3`<br>最小长度 `minKeywordLength=2`<br>最大关键词 `maxKeywords=6` | [`MemoryCueExtractor.java#L36-L81`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MemoryCueExtractor.java#L36-L81) |
| **检索范围 (Scope)** | MySQL 查库，严格限定 `user_id`；排除 SEALED 记录；排除删除中条目 | 回溯时间 `lookbackMonths=24`<br>候选集倍数 `CANDIDATE_MULTIPLIER=4`<br>硬上限 `CANDIDATE_HARD_CAP=40` (取 12 条) | [`MySqlMemoryPort.java#L44-L68`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MySqlMemoryPort.java#L44-L68) |
| **匹配字段 (Fields)** | `title`, `core_question`, `ai_summary`, `belief_then` 进行 `LIKE %kw%` 或标签 `EXISTS` | **明确不匹配 `content`** (保护高敏正文) | [`RecordMapper.xml#L505-L559`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/RecordMapper.xml#L505-L559) |
| **片段截取优先级** | 信息密度优先：`ai_summary` > `belief_then` > `core_question` > `title` | 最大片段数 `maxFragments=3`<br>单片段最大字符 `maxFragmentChars=120` | [`MySqlMemoryPort.java#L116-L127`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MySqlMemoryPort.java#L116-L127) |
| **时间距离分档** | 基于固定 Clock 计算 `distanceDays`：<br>1) RECENT: <= 30 天<br>2) DISTANT: 31~180 天<br>3) LONG_AGO: > 180 天 | `recentDays=30` (100% 预算)<br>`distantDays=180` (75% 预算, 90字)<br>`longAgo` (50% 预算, 60字)<br>最小片段 `minFragmentChars=40` | [`AgentTemporalPolicy.java#L115-L150`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/temporal/AgentTemporalPolicy.java#L115-L150) |
| **复现提示门控** | 模式为 `REVIEW_CHAT`，用户输入命中 `又/再次/反复/总是/不止一次/类似/重复`，已知非当前记录 `>=2` 条 | 跨度阈值 `recurrenceMinSpanDays=90` | [`AgentTemporalPolicy.java#L81-L97`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/temporal/AgentTemporalPolicy.java#L81-L97) |

### 4.3 护栏判定与忠实度校验参数
- **位置**：[`com.flashback.agent.guardrail.*`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail)
- **算法**：滑动 4-gram 覆盖率比对（`AgentFaithfulnessChecker`）。将候选文本切为 4-gram，比对语料库（SESSION 语料 + MEMORY 语料）。
- **指标与硬阈值**：
  - **工具参数忠实度** (`append_record_content` 入参校验)：
    - `minCoverage = 0.85`（最低 4-gram 覆盖率 85%）
    - `maxUncoveredRun = 12`（最长连续未覆盖字符不超过 12 个）
    - 代码：[`AgentToolValidator.java#L156-L158`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/tool/AgentToolValidator.java#L156-L158)
  - **收束素材忠实度**（`materialDraft` 校验）：
    - `minCoverage = 0.75`
    - `maxUncoveredRun = 15`
    - 代码：[`AgentChatServiceImpl.java#L789-L791`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java#L789-L791)
  - **违规类型短标识**：
    - `DIAGNOSTIC`：命中医学/心理诊断词库（抑郁症、焦虑症、躁狂、人格障碍等）
    - `FAKE_ACTION`：虚假宣称已代替用户保存/封存/解锁/删除
    - `MISSING_TIME_ATTRIBUTION`：复述了 MEMORY 层内容但未带过去时间标记
    - `FABRICATED_QUOTE`：捏造“你说过”引号内容
    - `UNFAITHFUL`：超出语料库编造内容
    - `TEMPORAL_OVERREACH`：在无充足跨度证据下断言反复模式或预测未来

### 4.4 流式、降级、重试与超时实现方式

| 机制 | 是否存在 | 真实工程实现细节 | 事实依据 |
|---|---|---|---|
| **流式输出** | **无** | 明确不支持流式。`completeWithTools` 中请求体固定设置 `"stream": false`。采用标准 HTTP 同步 JSON 往返。 | [`AgentModelClient.java#L179`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentModelClient.java#L179), [`L240`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentModelClient.java#L240) |
| **多路径分流降级** | **有** | 针对不同失败路径差异化降级：<br>1) 工具提议违规 → `REJECTED_BY_GUARD` 丢弃提议，正常下发回复；<br>2) 结语素材违规 → 丢弃素材设为 `null`；<br>3) 对话回复违规 → 替换为本地确定性安全文本常量兜底；<br>4) 上游不可用/超时 → 显式返回 `UNAVAILABLE` 或 `FAILED`，绝不以 Mock 冒充真实成功。 | [`AgentGuardrailDowngrade.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentGuardrailDowngrade.java), [`AgentChatServiceImpl.java#L527-L529`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java#L527-L529) |
| **重试策略** | **零自动重试** | **编排层无任何隐式自动重试循环**。避免级联延迟导致前端断开。<br>失败时已持久化当前 USER 消息，前端提示用户，支持同轮次手动幂等重试（`pendingUserMessage != null` 路径）。 | [`AgentResiliencePolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/resilience/AgentResiliencePolicy.java), [`AgentChatServiceImpl.java#L400-L411`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java#L400-L411) |
| **超时梯队** | **严格阶梯** | 1) 前端请求超时：`30000ms`<br>2) 后端编排共享预算：`24000ms` (`AgentCallBudget`)<br>3) 单次 HTTP 模型调用超时：`20000ms`<br>后端先于前端超时，将结构化错误受控交还用户。 | [`agentService.ts#L112`](file:///c:/_01_Code/Flashback/frontend/src/services/agentService.ts#L112), [`application.yml#L31`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L31), [`L87`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L87) |

### 4.5 Agent 架构、工具调用与多步流程
- **是否存在 Agent**：**是**。自研非框架轻量 Agent Loop，后端主导状态推进。
  - 核心状态机：`AgentWitnessTurnPolicy` 根据意图（LISTEN/UNTANGLE）、用户结束关键词与轮次判定本轮次类型。
  - 为什么不用 LangGraph / Spring AI：产品语义锁在状态机中（追问上限、不逼问、强制收束需单测钉死），自研留 Port 相比框架拥有更高审计权与零依赖代际风险。
- **是否存在工具调用**：**是**。原生 Function Calling，模型白名单（`deepseek-v4-pro,deepseek-v4-flash`）门控。
  - 工具集（3个业务工具）：
    1. `append_record_content(text)`：提议将文字追加进当前草稿正文
    2. `attach_record_tags(tagIds)`：提议为当前记录关联已有标签（禁止新建）
    3. `suggest_record_seal(unlockAt)`：提议将当前记录在指定时间封存
  - **二段式确认安全机制**：Agent 只能输出提议，暂存数据库 `agent_tool_call`（状态 `PROPOSED`），前端弹出确认条；只有用户显式点击调用 `/confirm` 端点时才由后端真正执行写操作。
- **是否存在受控多步自环（Reflection Loop）**：**是**（C7）。
  - 仅非 CLOSING 的 `MISSING_TIME_ATTRIBUTION` 违规允许反思重写；
  - 后端注入固定要求（不传候选文本），关闭工具，重新调用一次 Provider（单轮最多 2 次调用）；若二次依然违规，立即回退本地兜底，不无限循环。

---

## 5. 非 AI 核心工程实现

| 工程领域 | 核心实现方案与机制 | 代码与配置位置 |
|---|---|---|
| **统一鉴权** | 基于 JJWT 0.12.6 生成 HS256 Token，`JwtAuthenticationInterceptor` 统一校验；`/admin/**` 路径强制校验 `ADMIN` 角色；解析出 `AuthUser` 注入 Controller。 | [`JwtAuthenticationInterceptor.java#L42-L76`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/security/interceptor/JwtAuthenticationInterceptor.java#L42-L76) |
| **微信登录** | 小程序端调用 `wx.login` 获取 code，后端调用微信 Code2Session 端点换取 openid；若用户不存在自动注册，存在则更新并签发 JWT。 | [`AuthController.java#L35`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/AuthController.java#L35), [`WechatCode2SessionClient.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/wechat/WechatCode2SessionClient.java) |
| **接口限流** | **未实现系统级限流**。代码无 Redis 计数器或 Guava RateLimiter；业务层仅通过 `max-turns-per-session: 8` 和 `max-user-input-chars: 1000` 进行单会话限制。 | [`application.yml#L27-L41`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L27-L41) |
| **请求幂等** | 1) Agent 消息：`agent_message` 表通过 `uk_agent_message_session_turn_role (session_id, turn_no, role)` 联合唯一键保证同轮消息重复提交不重复插库；<br>2) 数据操作：`data_operation` 通过 `uk_data_operation_user_active (user_id, active_slot)` 保证单用户唯一活动操作，通过 `confirmation_nonce_hash` 防重放；<br>3) 篇章变更：通过 `version` 乐观锁 CAS 更新。 | [`schema.mysql.sql#L248`](file:///c:/_01_Code/Flashback/backend/sql/mysql/schema.mysql.sql#L248), [`L303`](file:///c:/_01_Code/Flashback/backend/sql/mysql/schema.mysql.sql#L303), [`TimeChapterMapper.xml`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/TimeChapterMapper.xml) |
| **数据所有权与租户隔离** | 所有数据库表均带 `user_id` 列；关键业务表设置复合唯一键 `uk_record_id_user_id (id, user_id)`；所有业务 Service 在执行前强制断言 `record.getUserId().equals(userId)`，跨用户直接抛出 `NotFoundException`。 | [`RecordServiceImpl.java#L105`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/RecordServiceImpl.java#L105), [`schema.mysql.sql#L42`](file:///c:/_01_Code/Flashback/backend/sql/mysql/schema.mysql.sql#L42) |
| **异步与定时任务** | 1) `RecordUnlockScheduler`：每分钟扫描已到期封存记录批量更新为 UNLOCKED 并下发微信通知；<br>2) `DraftCleanupScheduler`：清理 7 天未操作的过期草稿；<br>3) `DataOwnershipOperationRecoveryJob`：每 60 秒轮询恢复卡死或崩溃的数据导出/删除操作；<br>4) `DataOwnershipArtifactCleanupJob`：每小时清理过期的导出 ZIP 文件。 | [`backend/src/main/java/com/flashback/schedule/`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/schedule) |
| **对象存储与私有防盗链** | 基于 AWS SDK v2 的 `S3Presigner` 和七牛云 Auth 签发预签名上传/下载 URL（默认有效期 600 秒），媒体文件不存公网，下载带临时签名。 | [`S3ObjectStorageProvider.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/storage/s3/S3ObjectStorageProvider.java), [`QiniuObjectStorageProvider.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/storage/qiniu/QiniuObjectStorageProvider.java) |

---

## 6. 测试与评测体系

### 6.1 现有测试类型与覆盖内容
- **测试规模**：后端全量测试共 **108 个测试类，801 个测试用例**。
- **运行结果**：**801 passed, 0 failures, 0 errors, 16 skipped**。
  - 16 项 skipped 均为环境门控测试（需要宿主机运行中的真实 MySQL 8.0 容器、真实 DeepSeek API Key、真实微信自动化工具，在离线默认环境中安全跳过）。
- **测试分层覆盖**：
  1. **纯逻辑单元测试**：状态机推进、见证策略、提示词组装、切词提取、距离衰减、护栏规则。
  2. **Web 与 Controller 集成测试**：Spring MVC MockMvc 覆盖 11 个 Controller 的 53 个端点，验证未登录 401、跨用户 404、参数校验 400。
  3. **数据层测试**：MyBatis 与 H2 内存库结合，覆盖复合所有权级联删除、操作快照冻结、XML 动态 SQL。
  4. **隐私与安全不变量断言**：专门测试断言指标串、日志收集器与轨迹表绝不包含用户日记原文与对话原文。

### 6.2 离线评测框架（Agent Eval Framework）
- **评测定位**：零外调、完全离线、生产代码零改动的自动化回归与轨迹不变量量尺。
- **核心文件**：
  - 评测启动器：[`AgentEvalRunnerTest.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalRunnerTest.java)
  - 评测装置：[`AgentEvalHarness.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalHarness.java)
  - 硬不变量规则：[`AgentEvalInvariants.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalInvariants.java)
  - 用例定义集：`backend/src/test/resources/eval/cases/*.yaml`（4 个用例文件）
  - 基线快照库：[`backend/src/test/resources/eval/baseline/snapshots.yaml`](file:///c:/_01_Code/Flashback/backend/src/test/resources/eval/baseline/snapshots.yaml)
- **评测规模与维度**：
  - **37 条正式快照用例**（涵盖阶段推进、克制追问、记忆检索、护栏拦截、反思重写、记忆授权、自伤安全拦截等全部场景）。
  - **8 大评测维度**：阶段推进合法性、追问克制性、记忆三态区分、注入预算合规、护栏处置准确性、长度硬约束、工具 Fail-Closed、话术回归比对。
- **计算方式与指标集**：
  - 每条用例运行提取 13 项指标：`turns`, `stagePath`, `stageReasons`, `outcome`, `providerCalls`, `materialCalls`, `injectedCount`, `injectedChars`, `promptMessageCount`, `downgradeLayer`, `violation`, `replyLength`, `replyToInputRatio`。
- **防橡皮图章与防篡改机制**：
  1. **Checksum 派生**：每条基线的 checksum 由 `metrics + baselineNote` 共同哈希派生（SHA-256 前 12 位）。只改指标不改说明，checksum 校验直接失败。
  2. **强制 baselineNote**：必须记录是哪一刀修改的、修改原因与设计取舍，否则拒绝加载。
  3. **禁用自动覆盖**：框架严禁提供 `-Dupdate` 自动刷平基线的开关，必须由开发者人工核验差异并手工维护。
- **人评锚点现状**：
  - [`narrative-anchors.yaml`](file:///c:/_01_Code/Flashback/backend/src/test/resources/eval/baseline/narrative-anchors.yaml) 当前结构已建立，但内容**为空 (`anchors: []`)**。
  - 为什么空：填补锚点需要真实 Provider 调用与人工主观打分；本项目拒绝引入 LLM-as-Judge（因为外发用户日记侵犯隐私、批量打分预算不可承受、打分波动不可复现）。
- **实测运行数据（本次实际执行 `AgentEvalRunnerTest`）**：
  ```text
  [INFO] Running com.flashback.agent.eval.AgentEvalRunnerTest
  [INFO] Tests run: 77, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.336 s
  [INFO] BUILD SUCCESS (Total time: 24.227 s)
  ```

---

## 7. 已知问题、技术债务与真实风险

### 7.1 关于“Milvus 主键映射问题”的真实定位
- **核查结论**：**本项目《时光回序》Flashback 的真实代码库中完全不存在 Milvus 组件或任何向量数据库依赖！**
- **证据证明**：
  1. [`backend/pom.xml`](file:///c:/_01_Code/Flashback/backend/pom.xml) 中无任何 Milvus, Qdrant, Chroma, pgvector 依赖。
  2. 检索全库代码，仅在架构反思总结与规划文档中出现（如 `Docs/archive/v2-summary/时光回序V2.0阶段项目总结.md:335` 明确声明：“`pom.xml`：无任何向量数据库依赖”；`Docs/agent-iteration/narrative/agent-tech-story.md:37` 明确说明作者拥有双项目，RAG 项目负责检索评测，而 Flashback 负责 Agent Loop 与工程韧性）。
  3. 记忆检索唯一实现为 [`MySqlMemoryPort.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MySqlMemoryPort.java)，直接执行 MySQL 标签与 LIKE 匹配。
- **建议**：该问题属于用户另一个 RAG 知识库项目的已知缺陷，或取自通用面试模板。在 Flashback 的简历描述中切勿出现 Milvus 字眼，应如实包装为“在轻量级业务场景下，以 MySQL 结构化字段与标签检索构建 MemoryPort 抽象，通过受控预算注入替代重型向量库”。

### 7.2 真实存在的代码债务与风险点
1. **Redis 依赖空挂**：
   - 现状：`pom.xml` 引入了 `spring-boot-starter-data-redis`，`application-dev.yml` 配有 6379 端口，但在 `com.flashback` 业务代码中无一处注入 `RedisTemplate` 或 `StringRedisTemplate`。
   - 风险：简历若宣称“基于 Redis 实现分布式缓存或限流”，会被源码事实直接戳穿。
2. **缺少 API 级全局防刷限流**：
   - 现状：未实现针对 IP 或用户维度的 HTTP 请求速率限制中间件。
   - 风险：高并发或恶意恶意刷接口可能击穿后端，主要依赖外部网关层防护。
3. **H2 与真实 MySQL 行级锁语义不一致风险**：
   - 历史真实事故证据：C5 阶段曾发生轨迹入库使用 `REQUIRES_NEW` 挂起外层事务导致与外键父行死等 50 秒锁超时。该问题在 H2 内存库上因无行锁机制测试全绿，直到打真实 MySQL 才暴露（见 [`agent-tech-story.md#L211-L215`](file:///c:/_01_Code/Flashback/Docs/agent-iteration/narrative/agent-tech-story.md#L211-L215)）。
4. **中文切词无专业分词器**：
   - 现状：[`MemoryCueExtractor.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MemoryCueExtractor.java) 仅靠中英文标点空白切词，虽然保护了免加依赖，但切出的中文片段偏长，召回率受限。
5. **人评锚点文件为空**：
   - [`narrative-anchors.yaml`](file:///c:/_01_Code/Flashback/backend/src/test/resources/eval/baseline/narrative-anchors.yaml) 中 `anchors: []` 为空列表，语言语气质量尚未沉淀真实的生产人评打分样本。
6. **缺少 CI 自动化流水线**：
   - 仓库内未见 `.github/workflows` 等 CI 配置，测试与构建依赖本地手工执行 Maven 命令。

---

## 8. 可量化的素材与实测数据

### 8.1 代码与架构规模统计
- **代码物理行数**：
  - 后端业务代码（`backend/src/main/java`）：**22,979 行**（291 个 Java 文件）
  - 后端测试代码（`backend/src/test/java`）：**22,191 行**（108 个测试类）
  - 前端小程序代码（`frontend/src`）：**19,354 行**（.vue, .ts, .json）
  - **总代码行数**：约 **64,524 行**
- **接口与测试用例数**：
  - API 控制器：**11 个**
  - RESTful 接口端点：**53 个**（其中 Agent 核心端点 7 个，记录主流程端点 15 个，数据所有权端点 7 个，篇章端点 9 个）
  - 测试套件：**108 个类，801 个测试用例**（全量通过，16 skipped）
  - 离线评测集：**37 条基线用例，77 项自动化断言**

### 8.2 实测延迟与性能素材
- **离线评测吞吐**：`AgentEvalRunnerTest` 运行 37 条复杂多轮用例（包含状态机流转、记忆注入、护栏比对、反思重写），执行 77 项深度断言，**总耗时仅 3.336 秒**（平均单断言 43ms），单机完全离线执行。
- **真实 Provider 调用延迟（历史归档实测值）**：
  - C5 阶段真实 DeepSeek 调用 6 次实测：**Min 4571ms / Avg 6476ms / Max 8467ms**。
  - C8 阶段真实 DeepSeek Canary 短探针实测：单调用 **1378ms ~ 1656ms**，双调用 **2898ms ~ 3531ms**。
- **数据所有权 ZIP 离线打包**：离线单条记录导出在 20ms 内完成，全量导出支持流式写入 `ZipOutputStream` 结合 SHA-256 哈希计算。

### 8.3 最小成本实测步骤指南
若想在面试前获取属于自己的实时量化数据，推荐以下两步最小成本实测：
1. **0 成本实测离线评测速度**：
   ```powershell
   cd c:\_01_Code\Flashback\backend
   mvn test -Dtest=AgentEvalRunnerTest
   ```
   *预期产出*：控制台输出 `Tests run: 77, Failures: 0, Time elapsed: ~3.3s`，可直接作为“自研轻量级离线评测套件，单机 3.3 秒完成 77 项轨迹断言”的简历素材。
2. **极低成本（<0.01元）实测真实模型探针**：
   在本地环境变量配置真实 DeepSeek API Key 后，运行单个真实探针测试：
   ```powershell
   mvn test -Dtest=R1RealProviderBoundaryProbeTest -DAI_API_KEY=your_key
   ```
   *预期产出*：1 次真实网络往返，耗时约 1.5s，验证本地安全规则对普通情绪表达不产生误拦截。

---

## 9. 面试追问清单（15 个深度硬核技术问题）

### Q1：为什么在这个项目里选择自研 Agent Loop，而不是直接上 LangGraph 或 Spring AI？
- **答案要点**：
  1. **产品语义强绑定状态机**：产品的核心价值在于克制见证，包括“同阶段追问上限 1 次”、“回避型极短回答时不逼问”、“强制收束”等，这些必须能被单元测试 100% 确定性钉死，框架的黑盒编排会将确定性降级为模型概率行为。
  2. **框架收益与现状不匹配**：状态机本质上已经是一张图（包含节点、边、抢占、自环），MySQL 会话表天然实现了 Durable Checkpoint，可视化对当前 6 节点线性流是冗余复杂度。
  3. **架构资产与代际升级风险**：Spring AI 2.0 强制要求 Spring Boot 4.x，迁移会引入不兼容依赖代际风险；且会吃掉项目自研的 ToolRegistry、MemoryPort、Guardrail 和 Trace 采集点。
- **代码位置**：[`AgentStageMachine.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentStageMachine.java), [`AgentWitnessTurnPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentWitnessTurnPolicy.java), [`Docs/agent-iteration/narrative/agent-tech-story.md#L46-L79`](file:///c:/_01_Code/Flashback/Docs/agent-iteration/narrative/agent-tech-story.md#L46-L79)。

### Q2：工具调用怎么保证安全？如果模型胡乱提议或者不支持 Function Calling 怎么处理？
- **答案要点**：
  1. **严格白名单门控**：只有配置明确支持 FC 的模型（`deepseek-v4-pro,deepseek-v4-flash`）才下发 `tools`，不支持时安全退回纯对话，**坚决不做自研 Prompt 假降级**（防止自己解析出伪工具冒充成功）。
  2. **二段式显式确认**：模型在会话中只能提议（`PROPOSED`），写操作绝不直接执行；参数经瞬态缓冲保存，必须由用户在前端点击确认后，通过独立端点触发真正执行。
  3. **参数护栏双校验**：工具提议正文必须通过 4-gram 忠实度检查（覆盖率>=85%），且标签只能从已有可用标签中选择，严禁模型自行造新标签。
- **代码位置**：[`AgentModelClient.java#L72-L92`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentModelClient.java#L72-L92), [`AgentToolCoordinator.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/tool/AgentToolCoordinator.java), [`AgentController.java#L94-L103`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/controller/api/AgentController.java#L94-L103)。

### Q3：护栏校验为什么不用 LLM-as-Judge 审核官，而是自研确定性规则？
- **答案要点**：
  1. **延迟瓶颈**：实测 Provider 单次调用均值 6476ms；若“生成 + 审核 + 重写”串行执行将耗时近 20s，直接逼死后端 20s 超时上限。
  2. **确定性与可测试性**：LLM 审核不可复现；而确定性 4-gram 覆盖率加正则检查被数百条单测锁死，给定输入必然得到确定违规枚举。
  3. **数据隐私合规**：用户日记原文属于高敏数据，送给第二个模型打分属于未经授权的外发。
- **代码位置**：[`AgentContentChecker.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentContentChecker.java), [`AgentFaithfulnessChecker.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentFaithfulnessChecker.java), [`AgentGuardrailPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/AgentGuardrailPolicy.java)。

### Q4：忠实度检查的双指标（覆盖率 + 最长未覆盖片段）有什么讲究？只看一个不行吗？
- **答案要点**：
  1. **单看覆盖率会漏掉关键编造**：模型可能 90% 的字都摘自原文，但中间偷偷塞了一句致命的编造或事实扭曲（“大部分照抄，局部造谣”）。
  2. **单看未覆盖片段会误伤正常表达**：模型正常润色语气、补充转折连词时，未覆盖比例很低，但局部连续字符可能略微超标。两者结合才能既卡住编造长度，又保证整体忠实。
- **代码位置**：[`AgentFaithfulnessChecker.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentFaithfulnessChecker.java), [`AgentGuardrailBoundaryCaseTest.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/guardrail/AgentGuardrailBoundaryCaseTest.java)。

### Q5：项目中为什么没有使用向量数据库（Milvus / Qdrant）？记忆检索怎么保证不超时和数据隔离？
- **答案要点**：
  1. **规模匹配与架构减复杂度**：单用户日记量级在数百到数千条，引入向量数据库会带来额外的集群运维、数据同步一致性、网络开销与隐私外发面。
  2. **抽象解耦 (MemoryPort)**：以 `MemoryPort` 接口承接检索，当前以 MySQL 标签与元数据 LIKE 即可验证“记得过去”的产品心智，后续若需要向量只需换实现类，不影响上层编排。
  3. **严格用户隔离与数据安全**：SQL 检索无条件绑定 `user_id`；明确排除 `SEALED`（用户自己都未到期看的内容绝不注入）；**明确不检索日记正文 `content`**，只检索结构化摘要与困惑字段。
- **代码位置**：[`MemoryPort.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MemoryPort.java), [`MySqlMemoryPort.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/memory/MySqlMemoryPort.java), [`RecordMapper.xml#L505`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/RecordMapper.xml#L505)。

### Q6：你在可观测性排查中踩过什么印象深刻的坑？
- **答案要点**：
  1. **`REQUIRES_NEW` 导致的自锁卡死**：C5 阶段为了让轨迹异步写入不影响主事务，在落库时使用了 `Propagation.REQUIRES_NEW`。但它在挂起外层事务时**并没有释放外层持有的行锁**，新事务通过外键去碰同一批 `agent_session` 行，与外层事务死等直到 50 秒锁超时。
  2. **H2 测试的局限性**：由于 H2 内存库不具备 InnoDB 真实的行锁与间隙锁语义，这 37 个测试在 H2 上全部绿灯。该缺陷警示：涉及锁、外键与事务边界的变更，必须使用真实 MySQL 进行集成探针验证。
- **代码位置**：[`Docs/agent-iteration/narrative/agent-tech-story.md#L211-L216`](file:///c:/_01_Code/Flashback/Docs/agent-iteration/narrative/agent-tech-story.md#L211-L216), [`AgentTraceSink.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/trace/AgentTraceSink.java)。

### Q7：什么是反思环（Reflection Loop）？为什么只允许重写 1 次？
- **答案要点**：
  1. **受控自环而非自由探索**：当且仅当非 CLOSING 回复命中 `MISSING_TIME_ATTRIBUTION`（复述历史未标明时间）时，后端给模型注入固定重写约束，关闭工具重新生成。
  2. **严格预算防超时**：单次模型生成耗时 ~6.5s，若允许无限反思或自检，2 次重写直接突破 20s 后端超时；因此硬性约束为单轮最多额外重写 1 次，仍违规直接降级为本地安全常量。
- **代码位置**：[`AgentReflectionPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/reflection/AgentReflectionPolicy.java), [`AgentReplyPipeline.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/reflection/AgentReplyPipeline.java)。

### Q8：为什么后端坚持“零自动重试（Zero Automatic Retry）”？
- **答案要点**：
  1. **尾延迟爆炸**：单次调用均值 6.5s，若遇到瞬时网络抖动再自动重试一次，总耗时极易超过前端 30s 门槛，导致前端断连而用户看到网络报错。
  2. **状态可恢复性**：在调用模型前，用户的输入已经落库 `agent_message`，即使模型调用失败，状态机不会推进，数据不丢失；将重试权力交由用户显式触发，既透明又保住了系统的确定性。
- **代码位置**：[`AgentResiliencePolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/resilience/AgentResiliencePolicy.java), [`AgentChatServiceImpl.java#L399-L411`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java#L399-L411)。

### Q9：前端、后端、模型的超时时间是怎么设置的？为什么不能设成一样的？
- **答案要点**：
  1. **数值大小**：前端请求超时 `30000ms` > 后端共享编排预算 `24000ms` > 单次模型 HTTP 超时 `20000ms`。
  2. **先后顺序不能颠倒**：若两边都设为 30s 或前端更短，网络中断会先发生在前端，用户只能看到灰色的 `request: fail timeout`；而让后端先超时，系统就能捕获异常并将经过翻译的业务状态码（`UNAVAILABLE` 或 `FAILED`）返回给前端展示。
- **代码位置**：[`agentService.ts#L101-L112`](file:///c:/_01_Code/Flashback/frontend/src/services/agentService.ts#L101-L112), [`application.yml#L31`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L31), [`L87`](file:///c:/_01_Code/Flashback/backend/src/main/resources/application.yml#L87)。

### Q10：评测框架中如何防止基线快照沦为“橡皮图章”？
- **答案要点**：
  1. **Checksum 派生约束**：快照文件中的 checksum 由指标内容和开发者写的 `baselineNote` 共同哈希计算得出。修改指标必须同步阐述原因，否则 checksum 不匹配。
  2. **坚决不提供 `-Dupdate` 开关**：不允许一键将当前运行值刷成期望值，强迫开发者在指标变动时停下来审查编排逻辑是否发生非预期漂移。
  3. **反射禁止重写方法**：测试中通过反射断言评测代码中禁止出现任何 `write/update/accept` 等自动化修改基线文件的方法。
- **代码位置**：[`AgentEvalSnapshot.java#L24-L32`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalSnapshot.java#L24-L32), [`AgentEvalBaselineGuardTest.java`](file:///c:/_01_Code/Flashback/backend/src/test/java/com/flashback/agent/eval/AgentEvalBaselineGuardTest.java)。

### Q11：高风险自伤场景（R1）是如何做到高置信抢占的？
- **答案要点**：
  1. **零外调抢占分支**：在进入任何模型调用、记忆检索或工具组装之前，本地规则引擎 `AgentSafetyPolicy` 优先对用户输入进行确定性匹配。
  2. **明确的系统能力边界声明**：命中自伤风险时，不仅提供官方求助电话（120、110、12356心理热线），而且坦诚声明“系统不是专业心理咨询或危机救援人员，无法代为报警”，消除用户的危险幻觉。
- **代码位置**：[`AgentSafetyPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/safety/AgentSafetyPolicy.java), [`AgentChatServiceImpl.java#L465-L481`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/AgentChatServiceImpl.java#L465-L481)。

### Q12：数据所有权（删除全部/单条删除）为什么不能直接用一个数据库 `@Transactional` 事务包住？
- **答案要点**：
  1. **分布式异构资源的不可回滚性**：数据库事务无法回滚已经完成的对象存储（S3/Qiniu）物理删除。如果先删数据库再删对象存储，网络一旦中断就会留下失去元数据索引的永久孤儿私有文件。
  2. **远端优先与状态机持久化**：采用 `data_operation` 持久化操作状态机，先逐一删除远端私有对象（确保成功或 `NOT_FOUND` 幂等），最后才提交数据库级联删除；中途崩溃由后台 Recovery 任务无缝接续。
- **代码位置**：[`DataOwnershipServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/DataOwnershipServiceImpl.java), [`DataOwnershipOperationRecoveryJob.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/schedule/DataOwnershipOperationRecoveryJob.java)。

### Q13：时间智能（Temporal Intelligence）是如何防止模型“把过去的记忆冒充成现在”的？
- **答案要点**：
  1. **时间距离强制标签化**：注入 System Prompt 的记忆片段被强制打上 `[时间标签·记录ID]` 物理标记。
  2. **时间归属护栏硬拦截**：`AgentTimeAttributionChecker` 检查模型的生成回复，如果包含过去记忆中的核心事实但没有使用任何过去时态引导词，直接判定为 `MISSING_TIME_ATTRIBUTION` 并触发重写或降级。
  3. **远期记忆衰减**：半年以前的记忆注入字符预算强制削减至 50%，弱化远期事件在当前上下文中的注意力权重。
- **代码位置**：[`AgentTemporalPolicy.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/temporal/AgentTemporalPolicy.java), [`AgentTimeAttributionChecker.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/agent/guardrail/AgentTimeAttributionChecker.java)。

### Q14：演示模式（Preview Mode）是如何做到既能体验又不污染生产数据的？
- **答案要点**：
  1. **前端隔离网关**：在服务层入口通过 `requireRealSession` 拦截。当检测到当前为 Preview 会话且无真实 Token 时，直接在本地以 Mock 数据驱动 UI 状态机。
  2. **零后端请求与 fail-closed**：在 Preview 模式下，针对写操作（创建日记、Agent 对话、篇章修改）直接抛错或拦截，`totalRequests = 0`，绝不向后端发送假数据，也不用 Mock 冒充真实鉴权成功。
- **代码位置**：[`frontend/src/services/agentService.ts#L114-L123`](file:///c:/_01_Code/Flashback/frontend/src/services/agentService.ts#L114-L123), [`preview-session.ts`](file:///c:/_01_Code/Flashback/frontend/src/features/preview/preview-session.ts)。

### Q15：时间篇章（Time Chapter）中如何处理记录归属的并发冲突？
- **答案要点**：
  1. **版本号乐观锁**：篇章主表设置 `version` 字段，增减记录时通过 CAS 更新 `version = expectedVersion + 1`，检测到并发修改返回 409 Conflict。
  2. **全局统一加锁顺序**：涉及跨篇章记录转移时，强制按照 `chapterId` 从小到大依次获取行锁，彻底杜绝双向转移引发的 InnoDB 死锁。
- **代码位置**：[`TimeChapterServiceImpl.java`](file:///c:/_01_Code/Flashback/backend/src/main/java/com/flashback/service/impl/TimeChapterServiceImpl.java), [`TimeChapterMapper.xml`](file:///c:/_01_Code/Flashback/backend/src/main/resources/mapper/TimeChapterMapper.xml)。

---

## 10. 优先攻克的 10 个核心代码与架构设计点

为在简历与技术面试中展现深度掌控力，按重要度从高到低建议优先吃透以下 10 处核心设计：

1. **`AgentChatServiceImpl.java` (编排中枢)**：
   - *为什么最重要*：系统 1526 行的调用总枢纽，串联了会话恢复、安全拦截、记忆检索、时间策略、提示词组装、Provider 调用、护栏校验、反思重写与轨迹持久化的完整生命周期。
2. **`AgentStageMachine.java` & `AgentWitnessTurnPolicy.java` (状态机驱动)**：
   - *为什么重要*：面试论证“为什么自研 Loop 而不上 LangGraph”的直接证据，掌握如何用纯逻辑类把控追问上限与尊重沉默。
3. **`AgentGuardrailPolicy.java` & `AgentFaithfulnessChecker.java` (确定性护栏)**：
   - *为什么重要*：展示 AI 工程化中的严谨风控能力，掌握 4-gram 覆盖率加未覆盖长度双指标拦截编造的具体实现与边界。
4. **`AgentModelClient.java` & `AgentCallBudget.java` (Provider 客户端与弹性)**：
   - *为什么重要*：理解 JDK HttpClient 原生调用、OpenAI-compatible FC 协议下发、8 类封闭故障分类和 24s 共享单调时钟预算机制。
5. **`AgentReplyPipeline.java` & `AgentReflectionPolicy.java` (受控反思环)**：
   - *为什么重要*：展示对 Agent 闭环控制与尾延迟的敬畏，理解为何仅对时间归属开启单次受控重写，而不使用图框架或开环自检。
6. **`MySqlMemoryPort.java` & `MemoryCueExtractor.java` (分层记忆架构)**：
   - *为什么重要*：展现架构 Pragmatism（实用主义），掌握在小规模业务下如何用 MySQL 索引与元数据设计替代高成本向量库，并守住正文不外发的隐私红线。
7. **`AgentEvalRunnerTest.java` & `AgentEvalSnapshot.java` (离线评测与防伪)**：
   - *为什么重要*：极其稀缺的 AI 评测工程实践，掌握硬不变量校验、快照指标提取以及基于 Checksum + baselineNote 的防橡皮图章机制。
8. **`AgentTraceCollector.java` & `AgentTraceSink.java` (可观测性与隐私)**：
   - *为什么重要*：理解如何在类型系统层面杜绝日记原文外泄，以及如何用独立事务/事务同步回调解决 `REQUIRES_NEW` 死锁的真实事故。
9. **`DataOwnershipServiceImpl.java` (数据所有权与远端优先清除)**：
   - *为什么重要*：掌握分布式资源删除的最佳实践（远端对象优先 + not-found 幂等 + persisted operation），理解事务无法回滚云存储的本质痛点。
10. **`schema.mysql.sql` (数据库约束与索引体系)**：
   - *为什么重要*：理解复合所有权唯一键 `uk_record_id_user_id`、消息联合唯一键 `uk_agent_message_session_turn_role`、活动槽生成列 `active_slot` 对整个系统幂等性与安全性的底层支撑。
