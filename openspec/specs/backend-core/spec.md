# Backend Core Specification

## Purpose

Define the accepted backend core constraints for Flashback V2.0 demo work.

The backend core exists to protect the private record lifecycle, user ownership, Mini Program API contracts, stable record queries, safe AI fallback behavior, and the demo-scoped unlock reminder foundation.

## Requirements

### Requirement: Backend Capability Fact Source

The system SHALL maintain a backend capability fact source for Flashback V2.0 that distinguishes confirmed implementation from partial, planned, unknown, and out-of-scope capabilities.

#### Scenario: Confirmed capability is documented

- WHEN a backend capability is verified in code
- THEN it SHALL be documented as confirmed
- AND the relevant module or API behavior SHALL be noted

#### Scenario: Planned feature is not treated as implemented

- WHEN a capability exists only in design notes or future plans
- THEN it SHALL be documented as planned or out of scope
- AND it SHALL NOT be used as an acceptance dependency for the current backend optimization phase

#### Scenario: Unknown capability requires follow-up

- WHEN code and documentation do not clearly confirm a capability
- THEN it SHALL be marked as unknown
- AND a follow-up verification task SHALL be recorded

### Requirement: Record Lifecycle State Machine

The system SHALL support the core record lifecycle using the states DRAFT, SEALED, and UNLOCKED.

#### Scenario: Draft record is editable

- GIVEN a record is in DRAFT state
- AND the authenticated user owns the record
- WHEN the user updates the draft through a supported API
- THEN the system SHALL allow the update
- AND the record SHALL remain owned by the same user

#### Scenario: Draft record is sealed

- GIVEN a record is in DRAFT state
- AND the authenticated user owns the record
- WHEN the user seals the record
- THEN the system SHALL transition the record to SEALED
- AND the system SHALL preserve the user original content
- AND the system SHALL store or preserve the intended unlock time

#### Scenario: Sealed record is immutable

- GIVEN a record is in SEALED state
- WHEN a normal user update request attempts to modify the sealed record content
- THEN the system SHALL reject the update
- AND the sealed original content SHALL remain unchanged

#### Scenario: Sealed record becomes unlocked

- GIVEN a record is in SEALED state
- AND its unlock time has arrived
- WHEN the unlock process runs
- THEN the system SHALL transition the record to UNLOCKED
- AND the unlocked record SHALL become available through supported unlocked-record or detail APIs for the owner

#### Scenario: Unlock operation is idempotent

- GIVEN a record is already in UNLOCKED state
- WHEN the unlock process runs again for the same record
- THEN the system SHALL NOT create duplicate unlock effects
- AND the record SHALL remain in UNLOCKED state
- AND the operation SHALL be safe to repeat

### Requirement: Record Type Support

The system SHALL support the record types FUTURE_LETTER, NODE_RECORD, and EMOTION_NOTE as current V2.0 user-side record types.

#### Scenario: Supported record type is accepted

- GIVEN an authenticated user creates or updates a record
- WHEN the record type is FUTURE_LETTER, NODE_RECORD, or EMOTION_NOTE
- THEN the system SHALL accept the type if all other validation passes

#### Scenario: Unsupported record type is rejected or safely ignored

- GIVEN an authenticated user submits an unsupported record type
- WHEN the backend validates the request
- THEN the system SHALL reject the unsupported type or safely handle it according to documented validation behavior

### Requirement: User Authentication and Ownership

The system SHALL protect user records through authentication and ownership checks.

#### Scenario: Unauthenticated request is rejected

- GIVEN a request requires user identity
- WHEN the request does not contain valid authentication
- THEN the system SHALL reject the request

#### Scenario: User lists only own records

- GIVEN an authenticated user requests a record list
- WHEN the system returns records
- THEN every returned record SHALL belong to the authenticated user

#### Scenario: User reads only own record detail

- GIVEN an authenticated user requests record detail
- WHEN the requested record belongs to another user
- THEN the system SHALL reject the request or return a safe not-found response
- AND the response SHALL NOT expose private record content

#### Scenario: User mutates only own records

- GIVEN an authenticated user attempts to update, seal, unlock-related-read, or reply to a record
- WHEN the record belongs to another user
- THEN the system SHALL reject the operation
- AND the target record SHALL remain unchanged

### Requirement: Private Record Data Boundary

The system SHALL treat records as private user-owned emotional content by default.

#### Scenario: Sensitive content is not logged unnecessarily

- GIVEN the system processes record content, token data, or user identifiers
- WHEN logs are written
- THEN logs SHALL NOT include unnecessary sensitive record content or authentication secrets

#### Scenario: Timeline respects ownership

- GIVEN an authenticated user requests timeline data
- WHEN timeline entries are returned
- THEN all entries SHALL be scoped to the authenticated user

#### Scenario: Shared tag definitions are documented

- GIVEN the V2 demo uses system-shared/global tag definitions
- WHEN an authenticated user requests the tag list
- THEN the backend MAY return shared enabled tags
- AND this behavior SHALL be documented as the current V2 demo tag model

#### Scenario: Tag filtering respects record ownership

- GIVEN tag definitions are shared
- WHEN an authenticated user filters records by tag
- THEN the backend SHALL return only records owned by the authenticated user
- AND record-tag relationships SHALL NOT expose another user's private records

### Requirement: Frontend API Contract Alignment

The backend SHALL provide or document API behavior required by the V2.0 Mini Program frontend mental model.

#### Scenario: My Records page contract is supported

- GIVEN the frontend displays 我的记录
- WHEN it requests record list data
- THEN the backend SHALL provide enough information to distinguish draft, sealed, and unlocked records
- AND the response SHALL support stable pagination and sorting

#### Scenario: Timeline page contract is supported

- GIVEN the frontend displays 时光轴
- WHEN it requests timeline data
- THEN the backend SHALL provide user-scoped timeline entries
- AND the ordering behavior SHALL be stable and documented

#### Scenario: Time review detail contract is supported

- GIVEN the frontend displays 时间回看 or unlocked detail
- WHEN the owner requests an unlocked record
- THEN the backend SHALL return the record detail required for review
- AND it SHALL support reply-related behavior where implemented

#### Scenario: Frontend mock dependency is identified

- GIVEN a V2.0 frontend page depends on mock data or compatibility-only fields
- WHEN backend contract alignment is reviewed
- THEN the dependency SHALL be documented
- AND a backend or frontend follow-up task SHALL be created if it affects the core demo flow

### Requirement: List, Timeline, and Tag Query Stability

The backend SHALL provide stable behavior for list, timeline, and tag-related queries used by the Mini Program demo.

#### Scenario: Record list pagination is stable

- GIVEN an authenticated user requests records with pagination
- WHEN multiple records exist
- THEN the backend SHALL return deterministic ordering
- AND record-list sorting by creation time SHALL use a stable tie-breaker such as `created_at DESC, id DESC`
- AND pagination SHALL not duplicate or skip records under normal query conditions

#### Scenario: State filtering is supported or documented

- GIVEN an authenticated user filters records by state
- WHEN the state is DRAFT, SEALED, or UNLOCKED
- THEN the backend SHALL return records matching the requested state or document the current limitation

#### Scenario: Tag filtering is supported or documented

- GIVEN an authenticated user filters records by tag
- WHEN matching records exist
- THEN the backend SHALL return only user-owned matching records or document the current limitation

#### Scenario: Empty result is safe

- GIVEN a list, timeline, or tag query has no matching results
- WHEN the backend responds
- THEN the response SHALL use a safe empty-result structure
- AND SHALL NOT be treated as an error unless the request itself is invalid

### Requirement: Unlock Task Safety

The scheduled unlock mechanism SHALL be safe, repeatable, and scoped to eligible records.

#### Scenario: Unlock task only processes eligible records

- GIVEN the unlock task runs
- WHEN records are selected for unlocking
- THEN only eligible SEALED records whose unlock time has arrived SHALL be processed

#### Scenario: Unlock task can run repeatedly

- GIVEN the unlock task runs multiple times
- WHEN the same records are encountered
- THEN the task SHALL avoid duplicate side effects
- AND records already UNLOCKED SHALL remain valid

#### Scenario: Unlock task limitation is documented

- GIVEN missed unlock recovery, timezone behavior, or scheduling precision is not fully implemented
- WHEN backend optimization reviews the unlock mechanism
- THEN the limitation SHALL be documented
- AND the demo impact SHALL be assessed

### Requirement: WeChat Subscription Message Foundation

The system SHALL include a minimal WeChat Mini Program subscription message foundation for record unlock reminders when implemented in V2.0.

This foundation SHALL remain demo-scoped and SHALL NOT become a production notification center, SMS reminder system, campaign system, or admin-managed template platform.

#### Scenario: WeChat identity capability is classified

- WHEN the backend capability fact source is established
- THEN the system SHALL document whether the current user model supports `openid`
- AND it SHALL document whether local-account users can later bind a WeChat identity
- AND missing bind/login behavior SHALL be marked as partial, planned, or unknown instead of assumed implemented

#### Scenario: Preview bypass avoids real subscription delivery

- GIVEN a user is using preview bypass or no-login demo mode
- WHEN the frontend reaches the subscription authorization or reminder-delivery path
- THEN the system SHALL skip real WeChat subscription authorization and delivery
- AND it SHALL keep only demo-safe fallback behavior

#### Scenario: Seal flow can request subscription authorization

- GIVEN an authenticated user seals a draft record
- WHEN the seal operation succeeds
- THEN the frontend contract SHALL have a documented point where Mini Program subscription authorization can be requested
- AND refusal or unavailability of subscription authorization SHALL NOT undo the seal operation

#### Scenario: Unlock reminder send is non-blocking

- GIVEN a SEALED record becomes UNLOCKED through the unlock task
- WHEN the subscription reminder foundation attempts to enqueue or send an unlock reminder
- THEN notification failure SHALL be recorded if logging exists
- AND the unlock transition SHALL remain successful
- AND the unlock task SHALL continue processing eligible records

#### Scenario: Successful reminder send is idempotent

- GIVEN an unlock reminder has already been successfully sent for a record and template type
- WHEN the reminder path runs again for the same `record_id + template_type`
- THEN the system SHALL NOT send a duplicate successful message
- AND it SHALL preserve a deterministic success record or idempotency marker

#### Scenario: Notification persistence is minimal and explicit

- WHEN the current backend lacks sufficient reminder persistence
- THEN the design SHALL propose the smallest required model, such as `user_wechat_identity` or `user.openid`, `record_reminder` or `notification_outbox`, and `notification_log`
- AND it SHALL avoid schema expansion unrelated to unlock reminders

#### Scenario: Sensitive information is not logged

- GIVEN subscription authorization, openid binding, enqueue, send, or failure handling occurs
- WHEN logs or notification records are written
- THEN they SHALL NOT include record content, authentication tokens, or unnecessary sensitive identifiers

### Requirement: Minimal AI Fallback Boundary

The system SHALL treat AI fallback as a supporting capability rather than a dependency of the core record lifecycle.

#### Scenario: AI fallback failure does not block draft

- GIVEN AI fallback is unavailable or fails
- WHEN the user creates or updates a draft
- THEN the draft operation SHALL continue if all non-AI validation passes

#### Scenario: AI fallback failure does not block seal

- GIVEN AI fallback is unavailable or fails
- WHEN the user seals a record
- THEN the seal operation SHALL continue if all non-AI validation passes

#### Scenario: AI fallback does not mutate original content

- GIVEN AI fallback generates summary, suggestion, or auxiliary content
- WHEN the backend stores or returns AI-related output
- THEN the user original content SHALL remain preserved
- AND AI output SHALL NOT replace the original record text

#### Scenario: AI fallback does not alter lifecycle state

- GIVEN AI fallback runs during a record operation
- WHEN AI fallback succeeds or fails
- THEN it SHALL NOT independently change the record lifecycle state

## Accepted From M4 Real Capability Completion

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-m4-real-capability-completion/. Wording below is the accepted M4 delta requirement body.

### Requirement: M4 Real Paths Must Not Depend On Mock Success

Authenticated backend behavior in M4 SHALL use real implementation paths or explicit unavailable/failure states.

#### Scenario: Real user invokes an integration-backed feature

- GIVEN an authenticated non-preview user invokes AI, storage, media, location, timeline, or time-review behavior
- WHEN the required real integration is unavailable
- THEN the backend SHALL return an explicit unavailable or failure state
- AND it SHALL NOT record or return mock success as if the real operation succeeded

#### Scenario: Preview behavior is used

- GIVEN the request is explicitly in preview or development mode
- WHEN mock or preview data is used
- THEN the response SHALL remain isolated from authenticated real user data
- AND the behavior SHALL NOT be treated as production-ready verification

### Requirement: M4 AI Must Use Configured Real Provider In Real Mode

The backend SHALL support real AI provider calls for M4 AI-supported features.

#### Scenario: AI provider is configured

- GIVEN a supported AI provider base URL, model, and API key are configured
- WHEN the user triggers an AI-supported operation
- THEN the backend SHALL call the configured provider
- AND it SHALL return provider-backed output if the call succeeds
- AND it SHALL preserve the user's original record content

#### Scenario: AI provider is missing or fails

- GIVEN AI provider configuration is missing or the provider call fails
- WHEN the user triggers an AI-supported operation
- THEN the backend SHALL return explicit unavailable or failed behavior
- AND it SHALL NOT return mock output as real provider output
- AND it SHALL NOT block record save or seal when AI is not required

#### Scenario: AI secrets are handled

- GIVEN AI provider credentials exist
- WHEN frontend code or tracked files are reviewed
- THEN API keys SHALL NOT appear in Mini Program code or tracked repository files
- AND provider credentials SHALL be read from backend-side configuration or secret management only

### Requirement: M4 Configurable Object Storage Must Use Private Object Access

The backend SHALL integrate record media through a provider-neutral private object-storage contract. It SHALL support Qiniu and an S3-compatible provider selected by backend configuration.

#### Scenario: Upload token is requested

- GIVEN an authenticated user owns a DRAFT record
- WHEN the user requests an upload token for an image or voice attachment
- THEN the backend SHALL validate ownership, record state, media type, count limits, size policy, and key policy
- AND it SHALL return a short-lived provider-neutral upload authorization without exposing provider secret keys

#### Scenario: Uploaded object is committed

- GIVEN the Mini Program reports an object uploaded to the configured provider for a record
- WHEN the backend commits or verifies the attachment
- THEN the backend SHALL verify that the object exists through the provider recorded for the attachment
- AND it SHALL validate object key, size, type, record ownership, and record state before marking the attachment available

#### Scenario: Media is accessed

- GIVEN an authenticated user requests a record image or voice file
- WHEN the user owns the record and attachment
- THEN the backend SHALL provide a private-access-safe URL such as a signed short-lived URL
- AND it SHALL NOT require the object-storage bucket to be public

#### Scenario: Active provider is switched

- GIVEN Qiniu and/or S3-compatible provider credentials are configured backend-side
- WHEN `app.storage.provider` is changed
- THEN new upload authorizations SHALL use the selected provider without frontend business-flow changes
- AND existing attachments SHALL continue to route by their persisted `storageProvider` while that provider remains configured

### Requirement: M4 Attachments Must Respect Limits And Lifecycle

The backend SHALL support record image and voice attachments with explicit limits and lifecycle rules.

#### Scenario: Attachment limits are enforced

- GIVEN a user adds attachments to a DRAFT record
- WHEN the operation would exceed 9 images, 9 voice files, 40 MB per file, or 300 MB total attachments per record
- THEN the backend SHALL reject the operation
- AND the record attachment state SHALL remain valid

#### Scenario: Draft attachment is deleted

- GIVEN a DRAFT record has an attachment
- AND the authenticated user owns the record
- WHEN the user deletes the attachment through a supported API
- THEN the backend SHALL remove or mark the attachment unavailable according to the implementation policy
- AND if the deleted image is the current cover, the cover SHALL be cleared or replaced only through a valid draft cover operation

#### Scenario: Sealed attachment mutation is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN a user attempts to add, delete, replace, or re-record an attachment
- THEN the backend SHALL reject the operation
- AND existing attachment metadata SHALL remain unchanged

#### Scenario: Cross-user attachment access is rejected

- GIVEN an attachment belongs to another user's record
- WHEN an authenticated user attempts to read or mutate it
- THEN the backend SHALL reject the operation or return a safe not-found response
- AND private media metadata or URLs SHALL NOT be exposed

### Requirement: M4 Cover Must Come From Same Record Image Attachment

The backend SHALL support an optional record cover selected from the record's own image attachments.

#### Scenario: Valid cover is selected

- GIVEN a DRAFT record has an IMAGE attachment owned by the same user and record
- WHEN the user selects that attachment as cover
- THEN the backend SHALL accept the cover selection
- AND list, timeline, home, or detail responses MAY expose cover metadata or a private-access-safe cover URL

#### Scenario: Invalid cover is rejected

- GIVEN the selected attachment is voice, belongs to another record, belongs to another user, or does not exist
- WHEN the user attempts to set it as cover
- THEN the backend SHALL reject the operation
- AND the previous cover SHALL remain unchanged

#### Scenario: Cover mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN the user attempts to change or clear the cover
- THEN the backend SHALL reject the operation
- AND the cover SHALL remain unchanged

### Requirement: M4 Location Must Support Three Input Sources

The backend SHALL support record location through current location, map picker, and manual input.

#### Scenario: Draft location is saved

- GIVEN an authenticated user owns a DRAFT record
- WHEN the user saves a location with source CURRENT_LOCATION, MAP_PICKER, or MANUAL
- THEN the backend SHALL persist the location if validation passes
- AND manual input MAY omit latitude and longitude

#### Scenario: Draft location is deleted or updated

- GIVEN an authenticated user owns a DRAFT record with location
- WHEN the user updates or deletes location through a supported API
- THEN the backend SHALL allow the mutation

#### Scenario: Location mutation after seal is rejected

- GIVEN a record is SEALED or UNLOCKED
- WHEN a user attempts to add, update, or delete location
- THEN the backend SHALL reject the operation
- AND the sealed location state SHALL remain unchanged

#### Scenario: Unlocked detail includes location

- GIVEN an authenticated user opens time review for an UNLOCKED record
- WHEN the record has location
- THEN the backend SHALL return location data needed for the Mini Program to display it
- AND it SHALL remain scoped to the record owner

### Requirement: M4 Timeline Must Support Focused Filtering And Pagination

The backend SHALL provide owner-scoped timeline filtering by one tag and record creation year/month/day, with stable record-level pagination.

#### Scenario: User filters by tag and date

- GIVEN an authenticated user requests timeline records with a valid `tagId` and valid year/month/day granularity
- WHEN matching records exist
- THEN the backend SHALL return only that user's records matching both the tag and created-time range
- AND date filtering SHALL use `createdAt` in the `Asia/Shanghai` business timezone

#### Scenario: Timeline records are paginated

- GIVEN more records match than the requested page size
- WHEN the user requests a timeline page
- THEN records SHALL be ordered by `created_at DESC, id DESC`
- AND pagination SHALL be applied before records are grouped by year-month
- AND `TimelinePageVO` SHALL expose groups, record-level total, page number, page size, and `hasMore`

#### Scenario: Date filter is invalid

- GIVEN month is supplied without year, day is supplied without year/month, or the selected calendar date is impossible
- WHEN the timeline request is validated
- THEN the backend SHALL reject it with explicit bad-request behavior

#### Scenario: Valid filter has no matches

- GIVEN a valid tag/date filter has no matching user-owned records
- WHEN the backend responds
- THEN it SHALL return a successful empty `TimelinePageVO`
- AND it SHALL NOT expose whether another user has matching records

## Accepted From C1 Agent Runtime MVP

> Accepted and archived on 2026-07-27 from openspec/changes/archive/2026-07-27-agent-runtime-mvp/. Wording below is the accepted C1 delta requirement body. Agent 完整契约见 `openspec/specs/agent-runtime/spec.md`。

### Requirement: Agent Conversation Endpoints Must Be Authenticated And Owner Scoped

后端 SHALL 在 `/api/agent/**` 下提供 Agent 对话端点，纳入既有 `/api/**` 鉴权链路，并使用既有统一响应与错误码体系。

#### Scenario: 已登录用户访问 Agent 端点

- GIVEN 一个携带有效凭证的用户
- WHEN 该用户开启会话、读取会话、追加消息或结束会话
- THEN 后端 SHALL 校验会话归属于该用户
- AND 响应 SHALL 使用既有统一响应包装

#### Scenario: 未登录或跨用户访问 Agent 端点

- GIVEN 请求没有有效凭证，或目标会话属于其他用户
- WHEN 访问任一 Agent 对话端点
- THEN 后端 SHALL 返回未授权、拒绝或安全的未找到响应
- AND 后端 SHALL NOT 泄露其他用户的会话内容

### Requirement: Agent Runtime Must Not Change Existing AI Endpoint Contracts

引入 Agent Runtime SHALL NOT 改变既有单轮 AI 能力的对外契约。

#### Scenario: 既有 AI 端点被调用

- GIVEN Agent Runtime 已上线
- WHEN 客户端调用既有写作提示、记录整理或阶段总结端点
- THEN 请求与响应契约 SHALL 保持不变
- AND 既有的成功、不可用、失败与本地兜底语义 SHALL 保持不变

### Requirement: Agent Session Persistence Must Follow Owner Scoped Business Storage

Agent 会话与消息 SHALL 作为归属用户的业务数据持久化。

#### Scenario: 会话数据被持久化

- GIVEN 用户开启 Agent 会话并产生消息
- WHEN 后端持久化会话与消息
- THEN 每条记录 SHALL 携带所属用户标识
- AND 时间语义 SHALL 使用既有业务时区约定
- AND 用户被删除时其会话与消息 SHALL 被级联清理

#### Scenario: 会话内容出现在日志中

- GIVEN 后端记录 Agent 相关日志
- WHEN 日志被写出
- THEN 日志 SHALL NOT 包含对话原文或用户日记原文

## Accepted From C2 agent-tool-calling

> 来源：`openspec/changes/archive/2026-07-28-agent-tool-calling/`（C2，2026-07-28 用户验收）。
> C2 范围为原生 function calling 工具调用 + 代码级白名单 + 二段式用户确认；
> Memory / 系统化护栏 hardening / 决策链路可观测分别留给 C3 / C4 / C5。

### Requirement: Agent Tool Confirmation Endpoint Must Be Authenticated And Owner Scoped

后端 SHALL 在 `/api/agent/**` 下提供工具提议确认端点，纳入既有 `/api/**` 鉴权链路，并使用既有统一响应与错误码体系。

#### Scenario: 已登录用户确认工具提议

- GIVEN 一个携带有效凭证的用户拥有目标会话与目标提议
- WHEN 该用户确认或拒绝该提议
- THEN 后端 SHALL 校验会话与提议均归属于该用户
- AND 响应 SHALL 使用既有统一响应包装

#### Scenario: 未登录或跨用户确认工具提议

- GIVEN 请求没有有效凭证，或目标提议属于其他用户
- WHEN 访问工具确认端点
- THEN 后端 SHALL 返回未授权、拒绝或安全的未找到响应
- AND 后端 SHALL NOT 泄露其他用户的提议内容

### Requirement: Agent Tool Calling Must Not Change Existing Endpoint Contracts

引入工具调用 SHALL NOT 改变既有端点的对外契约。

#### Scenario: 既有 Agent 对话端点被调用

- GIVEN 工具调用能力已上线
- WHEN 客户端调用既有的开启会话、读取会话、追加消息或结束会话端点
- THEN 既有请求字段与响应字段语义 SHALL 保持不变
- AND 新增的工具相关字段 SHALL 为向后兼容的追加

#### Scenario: 既有单轮 AI 端点被调用

- GIVEN 工具调用能力已上线
- WHEN 客户端调用既有写作提示、记录整理或阶段总结端点
- THEN 请求与响应契约 SHALL 保持不变

#### Scenario: 既有记录端点被调用

- GIVEN 为工具执行新增了记录侧业务方法
- WHEN 客户端调用既有记录创建、更新、封存、位置、封面、附件端点
- THEN 这些端点的对外行为 SHALL 保持不变

### Requirement: Agent Tool Writes Must Reuse Record Business Layer

Agent 触发的记录写入 SHALL 经由记录业务层完成，SHALL NOT 直接绕过其归属与状态校验。

#### Scenario: 工具写入通过业务层

- GIVEN 一次已确认的工具执行需要修改草稿记录
- WHEN 后端执行该写入
- THEN 写入 SHALL 经由记录业务层方法完成
- AND 归属校验与草稿状态校验 SHALL 生效

#### Scenario: 审查数据访问路径

- GIVEN 工具执行层实现完成
- WHEN 审查其数据访问路径
- THEN SHALL NOT 存在跳过记录业务层校验的 Agent 专用写入路径

### Requirement: Provider Function Calling Configuration Must Be Backend Side And Explicit

工具调用相关的 provider 配置 SHALL 只存在于 backend-side 配置，并对能力可用性做显式判定。

#### Scenario: 工具调用可用性配置

- GIVEN 后端需要判断当前 provider 与模型是否支持工具调用
- WHEN 后端读取配置
- THEN 判定 SHALL 基于显式配置的受支持模型范围
- AND 后端 SHALL NOT 假设任意兼容 provider 或任意模型都支持工具调用

#### Scenario: 严格模式的独立地址配置

- GIVEN 严格模式需要不同于默认的服务地址
- WHEN 后端启用严格模式
- THEN 该地址 SHALL 来自独立的 backend-side 配置项
- AND 启用严格模式但地址缺失时 后端 SHALL 视为配置错误而非静默降级

#### Scenario: 凭证边界

- GIVEN 工具调用相关配置存在
- WHEN 检查前端代码与 tracked files
- THEN provider 凭证 SHALL NOT 出现在其中
- AND 新增配置项 SHALL NOT 引入新的凭证字段

### Requirement: Agent Tool Call Persistence Must Follow Owner Scoped Business Storage

工具提议与执行记录 SHALL 作为归属用户的业务数据持久化。

#### Scenario: 工具调用数据被持久化

- GIVEN 用户会话中产生工具提议
- WHEN 后端持久化提议与执行状态
- THEN 每条记录 SHALL 携带所属用户标识
- AND 时间语义 SHALL 使用既有业务时区约定
- AND 用户被删除时其工具调用记录 SHALL 被级联清理

#### Scenario: 工具调用数据出现在日志中

- GIVEN 后端记录工具相关日志
- WHEN 日志被写出
- THEN 日志 SHALL NOT 包含对话原文或用户日记原文

## Accepted From C4 agent-guardrails-hardening

> 来源：openspec/changes/archive/2026-07-28-agent-guardrails-hardening/（C4，2026-07-28 用户验收）。
> 后端侧约束：检查层位置、配置边界、依赖边界与既有契约不回归。

### Requirement: Guardrail Checks Must Run Backend Side Without External Calls

护栏检查 SHALL 完全在后端本地完成，SHALL NOT 依赖任何外部服务调用。

#### Scenario: 忠实度判定的执行位置

- GIVEN 后端需要判定候选文本是否忠实于用户表达
- WHEN 判定被执行
- THEN 判定 SHALL 在后端进程内完成
- AND 判定 SHALL NOT 发起对 AI provider 或其他外部服务的调用

#### Scenario: 护栏检查的依赖边界

- GIVEN 护栏检查已实现
- WHEN 审查依赖清单
- THEN 检查 SHALL NOT 引入新的第三方分词或文本相似度依赖
- AND 依赖声明文件 SHALL 保持不变

#### Scenario: 前端不承担护栏判定

- GIVEN 护栏判定影响是否向用户呈现内容
- WHEN 审查判定实现位置
- THEN 判定 SHALL NOT 由前端执行
- AND 后端 SHALL NOT 依据客户端提交的判定结论放行内容

### Requirement: Guardrail Rejection Must Reuse Existing Tool Audit Channel

工具路径的护栏拒绝 SHALL 复用既有的提议审计通道，SHALL NOT 新增持久化结构。

#### Scenario: 不忠实提议的落痕

- GIVEN 一条工具提议因内容不忠实被拒绝
- WHEN 后端记录该次拒绝
- THEN 后端 SHALL 使用既有的提议审计记录与其守卫拒绝状态
- AND 后端 SHALL NOT 为此新增数据表

#### Scenario: 拒绝原因的可区分性

- GIVEN 提议可能因白名单、参数边界或内容不忠实被拒绝
- WHEN 审计记录被写出
- THEN 拒绝原因 SHALL 可区分这几类情形
- AND 拒绝原因 SHALL 以结构化常量表达，而非自由文本描述

#### Scenario: 审计内容的隐私边界

- GIVEN 护栏拒绝被审计
- WHEN 审计记录被写出
- THEN 记录 SHALL 只包含结构化摘要与判定指标
- AND 记录 SHALL NOT 包含候选文本原文或用户表达原文

### Requirement: Guardrail Configuration Must Be Backend Side And Credential Free

护栏阈值与开关 SHALL 只来自 backend-side 配置，且 SHALL NOT 引入凭证字段。

#### Scenario: 阈值配置

- GIVEN 忠实度判定存在覆盖比例、连续未覆盖片段长度与最短受检长度等阈值
- WHEN 这些阈值被读取
- THEN 阈值 SHALL 来自后端应用配置
- AND 阈值 SHALL 具备可用的默认值

#### Scenario: 护栏开关关闭时的行为

- GIVEN 忠实度判定被配置关闭
- WHEN 后端处理候选文本
- THEN 后端 SHALL 记录结构化日志说明该判定未生效
- AND 后端 SHALL NOT 静默地表现为判定已通过

#### Scenario: 配置项与凭证

- GIVEN 护栏配置项被新增
- WHEN 审查配置结构
- THEN 配置 SHALL NOT 包含 API key、token 或任何 provider 凭证
- AND provider 凭证 SHALL 仍只来自既有的 AI 配置来源

### Requirement: Guardrail Introduction Must Not Regress Existing Agent Contracts

引入护栏层 SHALL NOT 改变既有 Agent 契约与既有 AI 链路的行为。

#### Scenario: 既有 Agent 端点契约

- GIVEN 护栏层已引入
- WHEN 审查 Agent 会话与工具确认端点
- THEN 既有端点的字段语义 SHALL 保持不变
- AND 任何字段新增 SHALL 向后兼容

#### Scenario: 既有单轮 AI 链路

- GIVEN 后端存在与 Agent 对话无关的单轮 AI 端点
- WHEN 护栏层被引入
- THEN 这些端点的请求与响应链路 SHALL 保持不变

#### Scenario: 阶段与轮次语义

- GIVEN 某轮产出因护栏被拒绝或降级
- WHEN 后端处理该轮
- THEN 会话阶段推进与轮次计数语义 SHALL 与既有契约保持一致
- AND 失败轮重试语义 SHALL NOT 被改变

#### Scenario: 记录生命周期不依赖护栏

- GIVEN 护栏检查不可用或判定失败
- WHEN 用户保存草稿或封存记录
- THEN 记录保存与封存 SHALL 正常完成
- AND 护栏可用性 SHALL NOT 成为记录生命周期的依赖


## Accepted From C3a agent-memory-retrieval

> 来源：`openspec/changes/archive/2026-07-29-agent-memory-retrieval/`（C3 前半刀，2026-07-29 用户验收）。
> 范围：记忆检索的后端实现约束、配置项与会话用途标识。

### Requirement: Memory Retrieval Must Be Implemented Behind A Replaceable Port

记忆检索 SHALL 以抽象接口暴露给 Agent Runtime，实现细节 SHALL NOT 泄漏到调用方。

#### Scenario: 调用方依赖

- GIVEN Agent Runtime 需要历史记录片段
- WHEN Runtime 获取片段
- THEN Runtime SHALL 只依赖记忆检索接口
- AND Runtime SHALL NOT 直接依赖检索的持久化实现

#### Scenario: 接口的用途维度

- GIVEN 后续场景也需要消费记忆
- WHEN 检索接口被调用
- THEN 接口 SHALL 接受会话用途作为入参
- AND 后续场景 SHALL 复用同一接口而非另建检索实现

#### Scenario: 片段的结构

- GIVEN 检索返回结果
- WHEN 结果被传递给上下文组装
- THEN 每个片段 SHALL 携带记录标识、发生时间与可读的时间标签
- AND 每个片段的文本长度 SHALL 受配置上限约束

#### Scenario: 片段文本的取材范围

- GIVEN 后端为命中记录选取可注入的片段文本
- WHEN 片段被构造
- THEN 片段 SHALL 取自记录的说明性字段
- AND 片段 SHALL NOT 取自记录正文
- AND 无任何可用说明性字段时该记录 SHALL 被跳过

### Requirement: Memory Retrieval Must Not Introduce Full Text Indexes Or External Engines

记忆检索 SHALL 基于既有关系型存储与既有索引结构实现。

#### Scenario: 索引与依赖边界

- GIVEN 记忆检索实现完成
- WHEN 审查数据库结构与项目依赖
- THEN 数据库 SHALL NOT 新增全文索引
- AND 数据库 SHALL NOT 引入分词器配置
- AND 项目 SHALL NOT 新增用于检索、分词或相似度计算的第三方依赖

#### Scenario: 检索谓词的字段范围

- GIVEN 检索查询被构造
- WHEN 查询条件被检查
- THEN 查询 SHALL 包含用户标识谓词
- AND 查询 SHALL NOT 包含记录正文的匹配谓词
- AND 查询结果条数 SHALL 有上限

### Requirement: Memory Configuration Must Come From Backend Side Config Without New Credentials

记忆能力的开关与阈值 SHALL 来自 backend-side 配置。

#### Scenario: 配置项范围

- GIVEN 记忆能力存在开关、片段条数上限、单片段长度上限、时间范围与时间归属阈值
- WHEN 配置被声明
- THEN 这些配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入任何新的凭证字段

#### Scenario: 开关关闭时的可见痕迹

- GIVEN 记忆能力开关被关闭
- WHEN 后端处理一轮对话
- THEN 后端 SHALL 记录结构化痕迹说明记忆未生效
- AND 后端 SHALL NOT 静默表现为检索无命中


## Accepted From C3b agent-review-chat

> 来源：`openspec/changes/archive/2026-07-29-agent-review-chat/`（C3 后半刀，2026-07-29 用户验收）。
> 范围：会话用途的行为分支、开会话端点的用途参数、回看配置。
> 本刀在本 spec 只做新增；用途相关的两条 MODIFIED 落在 `agent-runtime`。

### Requirement: Session Purpose Must Drive Behaviour Through A Single Derived Mode

会话用途 SHALL 经由单一派生模式影响行为，SHALL NOT 在多处各自判断。

#### Scenario: 模式的派生与使用

- GIVEN 一个会话携带用途标识
- WHEN 后端需要决定记录状态要求、阶段推进方式、工具可用性或素材产出
- THEN 这些决定 SHALL 来自同一个由用途派生的模式
- AND 后端 SHALL NOT 在各处重复判断用途

#### Scenario: 未知或缺失用途

- GIVEN 会话的用途为空或无法识别
- WHEN 模式被派生
- THEN 模式 SHALL 回退为写作引导
- AND 后端 SHALL NOT 因此进入无模式状态

### Requirement: Session Start Endpoint Must Accept An Optional Purpose

开启会话的端点 SHALL 接受可选的用途参数，且 SHALL 保持向后兼容。

#### Scenario: 未指定用途

- GIVEN 请求未携带用途
- WHEN 后端开启会话
- THEN 会话 SHALL 为写作引导用途
- AND 既有客户端调用 SHALL NOT 需要修改

#### Scenario: 指定回看用途

- GIVEN 请求携带回看用途与记录标识
- WHEN 后端开启会话
- THEN 会话 SHALL 为回看用途

#### Scenario: 回看用途缺少记录标识

- GIVEN 请求携带回看用途但未指定记录
- WHEN 后端处理该请求
- THEN 后端 SHALL 拒绝该请求

#### Scenario: 会话读取、追加与结束端点

- GIVEN 回看会话已建立
- WHEN 客户端读取会话、追加消息或结束会话
- THEN 后端 SHALL 复用既有的会话端点
- AND 后端 SHALL NOT 为回看另建一套等价端点

### Requirement: Active Session Lookup Must Be Scoped By Purpose

进行中会话的查询 SHALL 按用途隔离。

#### Scenario: 同一记录上的不同用途会话

- GIVEN 同一条记录上存在不同用途的进行中会话
- WHEN 后端按用途查询进行中会话
- THEN 查询 SHALL 只返回该用途下的会话
- AND 查询 SHALL NOT 依赖记录状态互斥这一巧合来避免串会话

### Requirement: Review Chat Configuration Must Come From Backend Side Config

回看对话的参数 SHALL 来自 backend-side 配置。

#### Scenario: 配置项范围

- GIVEN 回看对话存在轮次上限与记录内容注入长度上限
- WHEN 配置被声明
- THEN 这些配置 SHALL 独立于写作引导的同类配置
- AND 配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入任何新的凭证字段

#### Scenario: 回看轮次上限与写作引导互不影响

- GIVEN 回看轮次上限被调整
- WHEN 写作引导会话进行
- THEN 写作引导的轮次上限 SHALL 保持不变

## Accepted From C5 agent-observability

> 来源：`openspec/changes/archive/2026-07-30-agent-observability/`（C5，2026-07-30 用户验收）。
> 范围：决策轨迹的持久化、查询、配置与清理。
> **本刀不新增任何 HTTP 端点**（design 决策 4：`AuthRole.ADMIN` 全仓无签发路径，
> `/admin` 下的端点在真实环境不可达；产品 API 下的 trace 端点违反 C5 非目标）。
> 既有 Agent 端点、DTO、失败重试语义均未改动。

### Requirement: Agent Turn Trace Must Be Persisted With Owner Scope And Cascade Cleanup

Agent 决策轨迹 SHALL 按用户与会话归属持久化，并随其宿主级联清理。

#### Scenario: 轨迹落库

- GIVEN 一轮 Agent 对话完成或提前返回
- WHEN 后端写出该轮轨迹
- THEN 轨迹 SHALL 记录所属用户、所属会话、轮次与尝试序号
- AND 同一轮的多次尝试 SHALL 可通过尝试序号区分

#### Scenario: 会话被删除

- GIVEN 某会话已产生轨迹
- WHEN 该会话被删除
- THEN 其轨迹 SHALL 被级联清理

#### Scenario: 用户被删除

- GIVEN 某用户已产生轨迹
- WHEN 该用户被删除
- THEN 其轨迹 SHALL 被级联清理

#### Scenario: 轨迹存储不是日记原文的授权存储

- GIVEN 用户日记原文只允许存在于被授权的业务存储
- WHEN 轨迹被写出
- THEN 轨迹存储 SHALL NOT 成为日记原文或对话原文的副本位置

### Requirement: Agent Turn Trace Must Not Share The Message Uniqueness Constraint

轨迹 SHALL 独立于对话消息存储，SHALL NOT 依赖或改动消息表的唯一性约束。

#### Scenario: 一轮多条痕迹

- GIVEN 一轮对话包含多个决策步骤
- WHEN 这些步骤的痕迹被写出
- THEN 痕迹 SHALL NOT 受「同会话同轮次同角色只允许一条」的约束限制

#### Scenario: 既有幂等约束不变

- GIVEN 对话消息表的唯一性约束是失败重试幂等的实现基石
- WHEN 引入轨迹存储
- THEN 该唯一性约束 SHALL 保持不变

### Requirement: Agent Turn Trace Must Be Queryable By Session For Developers

轨迹 SHALL 提供按会话取回的查询能力，且 SHALL NOT 经由产品接口暴露。

#### Scenario: 按会话取回

- GIVEN 某会话已产生多轮轨迹
- WHEN 按会话标识查询轨迹
- THEN 后端 SHALL 返回该会话按轮次与尝试序号有序的轨迹

#### Scenario: 产品接口的边界

- GIVEN 轨迹面向开发者排查
- WHEN 产品接口返回会话数据
- THEN 响应 SHALL NOT 包含轨迹数据

#### Scenario: 未新增对外端点

- GIVEN 轨迹已可查询
- WHEN 审查后端对外端点
- THEN C5 SHALL NOT 新增面向终端用户的轨迹端点
- AND C5 SHALL NOT 修改既有认证与令牌签发逻辑

### Requirement: Agent Turn Trace Content Must Be Structured And Non Reversible

轨迹字段 SHALL 只承载结构化标识、数值与不可还原的摘要。

#### Scenario: 轨迹字段的取值范围

- GIVEN 轨迹被写出
- WHEN 检查其字段取值
- THEN 取值 SHALL 限于结构化枚举标识、数值指标、长度与哈希前缀
- AND 取值 SHALL NOT 包含自由文本形式的用户表达

#### Scenario: 拒绝原因与违规类型的表达

- GIVEN 某轮发生护栏拒绝或降级
- WHEN 轨迹记录其原因
- THEN 原因 SHALL 以既有的结构化常量表达，而非自由文本描述

#### Scenario: 轨迹相关日志

- GIVEN 后端记录轨迹写入相关的运行日志
- WHEN 日志被写出
- THEN 日志 SHALL 只包含结构化元数据
- AND 日志 SHALL NOT 包含对话原文或用户日记原文

### Requirement: Guardrail Downgrade Traces Must Be Correlatable To Session And Turn

护栏降级与判定异常的痕迹 SHALL 可关联到具体会话与轮次。

#### Scenario: 回复路径的降级痕迹

- GIVEN 某轮回复在可观测启用时被护栏降级
- WHEN 该次降级被留痕
- THEN 痕迹 SHALL 携带该轮的会话标识与轮次
- AND 痕迹 SHALL NOT 以空值表达这两项

#### Scenario: 素材路径的降级痕迹

- GIVEN 某次素材产出在可观测启用时被护栏丢弃
- WHEN 该次丢弃被留痕
- THEN 痕迹 SHALL 携带该轮的会话标识与轮次

#### Scenario: 判定自身异常的痕迹

- GIVEN 某层护栏判定过程自身发生异常并按 fail-closed 处理
- WHEN 该情形被留痕
- THEN 轨迹 SHALL 记录该判定结论及其所属闸层
- AND 该记录 SHALL 可关联到发生该情形的会话与轮次

### Requirement: Observability Configuration Must Live In Backend Side Config

可观测能力的开关与保留策略 SHALL 由 backend-side 配置约束。

#### Scenario: 配置项归属

- GIVEN 可观测能力需要开关与保留期
- WHEN 这些配置被声明
- THEN 配置 SHALL 位于 backend-side 配置中
- AND 配置 SHALL NOT 引入新的凭证字段
- AND 配置 SHALL NOT 出现在前端代码或 tracked files 中的明文 secret

#### Scenario: 关闭时的行为

- GIVEN 可观测能力被配置关闭
- WHEN 后端处理一轮对话
- THEN 后端 SHALL 记录结构化痕迹说明该能力未生效
- AND 后端 SHALL NOT 静默表现为轨迹无数据

#### Scenario: 保留期与清理

- GIVEN 轨迹随时间累积
- WHEN 需要控制存储规模
- THEN 后端 SHALL 提供可配置的保留期与可执行的清理手段
- AND C5 SHALL NOT 引入自动调度任务执行清理

### Requirement: Trace Recording Must Not Alter Existing Agent Behavior

引入轨迹 SHALL NOT 改变任何既有 Agent 行为契约。

#### Scenario: 既有对话契约

- GIVEN 轨迹已接入对话链路
- WHEN 审查既有行为
- THEN 阶段推进、上下文组装、护栏阈值、记忆检索、工具白名单与回看逻辑 SHALL 保持不变

#### Scenario: 既有失败语义

- GIVEN provider 调用失败
- WHEN 该轮返回
- THEN 用户消息 SHALL 仍被保留
- AND Agent 回复 SHALL 仍不落库
- AND 同轮重试 SHALL 仍不重复计数

### Requirement: Agent Conversation Client Timeout Must Exceed Backend AI Timeout

> Type B（2026-07-30）：手验中记录页与回看页均报 `request: fail timeout`。
> 根因是前端默认超时与后端 AI 超时相等（均 10000ms），前端必然先断，
> 于是后端设计的显式失败语义被网络错误覆盖。

前端 Agent 对话请求的超时 SHALL 大于后端 AI 调用超时。

#### Scenario: 一轮对话耗时接近后端 AI 超时

- GIVEN 后端一轮 Agent 对话包含一次真实 provider 调用
- WHEN 该轮耗时接近后端 AI 超时上限
- THEN 前端 SHALL NOT 先于后端超时断开
- AND 用户 SHALL 看到后端返回的显式失败态，而非网络层超时错误

#### Scenario: 不调用 provider 的 Agent 请求

- GIVEN 某个 Agent 请求只读写数据库而不调用 provider
- WHEN 该请求的超时被设置
- THEN 该请求 SHALL NOT 被放宽到与对话请求相同的超时
- AND 真正的网络故障 SHALL 仍能及时暴露

---

## Accepted From C6 agent-eval-framework

> 来源：`openspec/changes/archive/2026-07-31-agent-eval-framework/`（C6，2026-07-31 用户验收）。
> 范围：评测资产在后端工程中的位置、离线约束、依赖边界与「不改生产代码」的硬约束。
> 评测的行为语义（不变量、快照、诚实边界）见 `agent-runtime` spec 的 C6 段落。
> **本刀 `src/main` 零改动**：`git diff --name-only -- backend/src/main` 输出为空。
> **闸门 3 未申请**（外调预算 0）。

### Requirement: Evaluation Assets Must Live In Test Scope Only

评测资产 SHALL 完全位于后端测试范围内。

#### Scenario: 评测资产的位置

- GIVEN 评测被实现
- WHEN 审查改动范围
- THEN 评测的代码与数据 SHALL 只存在于测试源与测试资源目录
- AND 生产源码 SHALL NOT 被修改

#### Scenario: 为可测性修改生产代码

- GIVEN 评测需要驱动某条既有路径
- WHEN 该路径难以从测试装配抵达
- THEN 后端 SHALL 优先复用既有的构造注入与既有的单一落库出口
- AND 若确实必须修改生产代码，该修改 SHALL 先经用户确认，SHALL NOT 由实现方自行扩大范围

#### Scenario: 生产路径上的组件

- GIVEN 某组件在 mock provider 配置下仍运行于生产路径
- WHEN 评测需要该组件产出它当前产不出的内容
- THEN 后端 SHALL 以测试范围的替身实现该需求
- AND 该组件本身 SHALL NOT 被修改

#### Scenario: 替身的最小化

> 替身替掉的越多，被评测覆盖的生产代码就越少。
> 极端情形下断言的是替身自身的行为，评测因此失去意义。

- GIVEN 评测需要在测试范围内替换某个协作者
- WHEN 设计该替身
- THEN 替身 SHALL 只覆盖真正跨越进程边界的行为
- AND 判定逻辑、状态推进、上下文组装与检索收口 SHALL 使用生产实现

#### Scenario: 评测必须驱动真实的生成分支

> mock provider 分支在组装上下文之前即返回，因此只走该分支的评测
> 无法观测上下文组装，也无法产生任何降级轨迹。

- GIVEN 评测需要覆盖上下文组装与降级路径
- WHEN 评测驱动一轮对话
- THEN 评测 SHALL 走真实 provider 的生成分支
- AND 评测 SHALL 能证明该分支确实被走到

### Requirement: Evaluation Must Not Introduce Dependencies Or Schema Changes

评测 SHALL 在既有依赖与既有 schema 下实现。

#### Scenario: 依赖边界

- GIVEN 评测需要解析用例数据文件
- WHEN 选择解析方式
- THEN 后端 SHALL 使用测试 classpath 上已存在的能力
- AND 构建配置与依赖清单 SHALL NOT 被修改

#### Scenario: 依赖为传递引入时的失败语义

- GIVEN 评测所依赖的解析能力来自传递依赖
- WHEN 该能力不可用
- THEN 评测 SHALL 明确失败
- AND 评测 SHALL NOT 静默跳过用例

#### Scenario: 持久化边界

- GIVEN 评测断言的是每轮的决策轨迹
- WHEN 评测被执行
- THEN 评测 SHALL 断言轨迹收集器的内存状态
- AND 评测 SHALL NOT 要求新增表、新增列或修改既有 schema
- AND 轨迹落库的正确性 SHALL 仍由既有的集成测试承担

#### Scenario: 配置边界

- GIVEN 评测需要特定的限值组合
- WHEN 评测装配配置对象
- THEN 评测 SHALL 在测试范围内构造配置
- AND 生产配置文件的默认值 SHALL NOT 被修改

### Requirement: Evaluation Must Run Without External Services

评测 SHALL 可在无网络、无数据库的环境下完成。

#### Scenario: 外部调用

- GIVEN 评测被执行
- WHEN 检查外部交互
- THEN 评测 SHALL NOT 发起任何真实 AI provider 调用
- AND 评测 SHALL NOT 依赖对象存储或其他外部服务

#### Scenario: 环境门控

- GIVEN 评测离线且无外调
- WHEN 决定其执行条件
- THEN 评测 SHALL NOT 被环境变量门控为默认跳过
- AND 既有的真实 provider 探针 SHALL 保持默认跳过

#### Scenario: 执行能力的如实表述

- GIVEN 评测可由后端既有的测试命令执行
- WHEN 描述其强制力
- THEN 后端 SHALL 如实表述为「可由既有测试命令执行」
- AND 后端 SHALL NOT 表述为已具备持续集成门槛
- AND 本变更 SHALL NOT 引入持续集成配置

### Requirement: Local Real Samples Must Be Excluded From Version Control

真实样本 SHALL 不进入版本控制。

#### Scenario: 忽略规则的形态

- GIVEN 评测支持使用真实样本作为本地输入
- WHEN 配置版本控制忽略规则
- THEN 该规则 SHALL 以通配形式覆盖同类样本文件
- AND 该规则 SHALL NOT 仅点名单个文件

#### Scenario: 忽略规则的落地顺序

- GIVEN 真实样本文件尚未创建
- WHEN 实现评测
- THEN 忽略规则 SHALL 先于任何真实样本文件落地并被验证
- AND 该顺序 SHALL NOT 被颠倒

#### Scenario: 入库用例的内容

- GIVEN 用例文件将进入版本控制
- WHEN 编写这些用例
- THEN 它们 SHALL 只包含合成内容
- AND 它们 SHALL NOT 包含用户真实日记内容

### Requirement: Existing Test Assets Must Not Be Weakened

评测的引入 SHALL NOT 削弱既有测试资产。

#### Scenario: 既有断言

- GIVEN 既有后端测试已通过
- WHEN 评测被引入
- THEN 既有断言 SHALL NOT 被修改
- AND 既有测试 SHALL 保持全部通过
- AND 既有的环境门控探针 SHALL 保持默认跳过，跳过数量 SHALL NOT 增加

#### Scenario: 既有护栏用例集

- GIVEN 既有的护栏边界用例集已覆盖确定性场景
- WHEN 新增维度以外置数据文件表达
- THEN 既有用例集 SHALL 原地保留
- AND 确定性护栏用例 SHALL NOT 被迁移出既有用例集

#### Scenario: 既有隐私断言

- GIVEN 既有测试已包含一条判定指标不泄漏内容的断言
- WHEN 新增结构化的同类断言
- THEN 既有断言 SHALL 保持不变
- AND 新增断言 SHALL 以整体形状校验取代逐词列举
## Accepted From C7 agent-reflection-loop

> Accepted on 2026-08-03. 范围：后端调用预算、轨迹聚合与无契约扩张边界。

### Requirement: Reflection Must Reuse Existing Backend Contracts

#### Scenario: 外部契约

- GIVEN C7 被实现
- WHEN 审查对外接口
- THEN SHALL NOT 新增或修改 Agent API、DTO 或前端协议
- AND SHALL NOT 新增数据库表、列或 migration
- AND SHALL NOT 修改工具白名单、会话状态机或记录生命周期

#### Scenario: 配置与依赖

- GIVEN reflection 上限固定为一次
- WHEN 审查配置与构建文件
- THEN SHALL NOT 新增 secret、provider credential 或运行时开关
- AND SHALL NOT 修改 package、lockfile 或 Maven dependencies

### Requirement: Reflection Provider Budget Must Be Strictly Bounded

#### Scenario: 非 CLOSING reply 正常路径调用数

- GIVEN initial 输出通过或命中不可恢复违规
- WHEN 本轮结束
- THEN provider 调用数 SHALL NOT 超过 1

#### Scenario: 非 CLOSING reply eligible path 调用数

- GIVEN initial 输出命中允许恢复的违规
- WHEN 本轮结束
- THEN provider 调用数 SHALL NOT 超过 2
- AND 任意异常 SHALL NOT 触发第三次调用

#### Scenario: CLOSING 调用数

- GIVEN CLOSING 一轮需要生成 reply 与 material
- WHEN C7 被执行
- THEN reply SHALL NOT 发起 reflection
- AND 该轮 SHALL 保持既有最多 2 次 provider 调用

#### Scenario: 超时配置

- GIVEN C7 可能执行两次 provider 调用
- WHEN 实现该环
- THEN backend 20s 与 frontend 30s 既有超时 SHALL 保持不变
- AND 若真实 canary 证明预算不可行，系统 SHALL 回到规划而非直接放宽超时

### Requirement: Trace Provider Duration Must Represent Total Turn Cost

#### Scenario: 单次调用

- GIVEN 本轮仅调用 provider 一次
- WHEN 持久化轨迹
- THEN 顶层 provider duration SHALL 等于该次调用耗时

#### Scenario: Reflection 调用

- GIVEN 本轮调用 initial 与 reflection 两次
- WHEN 持久化轨迹
- THEN 顶层 provider duration SHALL 为两次 provider 耗时之和
- AND 每次调用的耗时与 phase SHALL 保留在结构化 steps 中

### Requirement: Reflection Trace Must Preserve One Turn One Row

#### Scenario: 同一请求内部重写

- GIVEN reflection 在一个请求内部发生
- WHEN trace sink 持久化
- THEN SHALL 只写一条 `agent_turn_trace`
- AND SHALL NOT 因 reflection 增加 `attemptNo`
- AND steps SHALL 以受控标识描述 initial/reflection/terminal

#### Scenario: 隐私边界

- GIVEN reflection 轨迹被写出或记录日志
- WHEN 检查字段与 steps
- THEN SHALL NOT 包含候选文本、用户消息、日记、记忆片段、prompt 全文或 provider response

### Requirement: Reflection Must Be Verified Against Real MySQL Semantics

#### Scenario: 本地数据库验证

- GIVEN 含 reflection 的路径已由离线测试通过
- WHEN 进入验收
- THEN SHALL 在真实 MySQL 上验证事务完成与 trace 持久化
- AND H2 结果 SHALL NOT 被表述为真实 MySQL 验证

#### Scenario: 未获外调授权

- GIVEN 闸门 3 未被批准
- WHEN 汇报验证结果
- THEN 真实 provider 与真机重写效果 SHALL 标记为 SKIPPED
- AND SHALL NOT 从 scripted provider 结果推断真实模型质量

## Accepted From C8 agent-resilience

> Accepted on 2026-08-08. C8 复用既有 API、DTO、数据库与依赖，增加 request-scope
> provider-work deadline 与类型化失败边界。

### Requirement: Agent Conversation Client Timeout Must Exceed Backend AI Timeout

#### Scenario: 多调用 Agent 请求

- GIVEN 一次 Agent 请求可能执行两个 provider 子调用
- WHEN 配置 backend 与 frontend timeout
- THEN backend SHALL 具有小于 frontend 30000ms 的整轮 provider-work deadline
- AND 单次 provider timeout SHALL NOT 被误当成整轮请求上限
- AND frontend SHALL 预留 backend 返回结构化失败响应的时间

### Requirement: Provider Client Must Preserve Failure Identity Without Leaking Payloads

#### Scenario: HTTP 与解析边界

- GIVEN provider 返回 HTTP 错误或无效 2xx 响应
- WHEN `AgentModelClient` 抛出类型化失败
- THEN SHALL 保留稳定 category 与必要的内部 status identity
- AND SHALL NOT 在日志、trace 或 API 暴露 response body、request body、endpoint、credential 或异常 message

#### Scenario: interrupted

- GIVEN provider 调用线程被中断
- WHEN backend 捕获该失败
- THEN SHALL 恢复线程中断标记
- AND SHALL NOT 自动重试

### Requirement: Agent Provider Work Budget Must Be Request Scoped

#### Scenario: budget 配置

- GIVEN C8 默认整轮 provider-work budget 为 24000ms
- WHEN Agent request 开始
- THEN SHALL 创建一个 request-scope budget
- AND reply、reflection 与 material SHALL 共享该 budget
- AND SHALL NOT 通过为子流程创建新 budget 绕过总上限

#### Scenario: 单次上限

- GIVEN `app.ai.timeout-millis` 仍为 20000ms
- WHEN 发起下一 provider call
- THEN 实际 timeout SHALL 为单次上限与剩余整轮预算的较小值
- AND frontend 30000ms 与纯 DB request timeout SHALL 保持不变

### Requirement: Existing Agent Failure Contract Must Remain Stable

#### Scenario: backend response

- GIVEN Agent 返回 SUCCESS、UNAVAILABLE 或 FAILED
- WHEN 构造 `AgentSessionVO`
- THEN SHALL 继续使用既有 SUCCESS/UNAVAILABLE/FAILED 与 message 字段
- AND SHALL NOT 新增 retryable/failure-category 等外部字段
- AND SHALL NOT 暴露 HTTP status、异常类、provider body 或内部 endpoint

#### Scenario: pending turn

- GIVEN provider failure 后用户消息已持久化且 Assistant 未落库
- WHEN 用户主动重试
- THEN SHALL 复用同一 turn 与既有 attempt 语义
- AND SHALL NOT 重复插入用户消息或推进阶段机

### Requirement: Material Failure Must Not Break Record Lifecycle

#### Scenario: closing material 超时或失败

- GIVEN reply 已成功且 optional material 调用失败或预算耗尽
- WHEN 会话收束
- THEN Assistant reply 与 session end SHALL 保持成功
- AND material draft SHALL 为空
- AND turn outcome SHALL NOT 被反转为 provider FAILED

### Requirement: C8 Must Reuse Existing Storage And Dependencies

#### Scenario: 数据与构建边界

- GIVEN C8 被实现
- WHEN 审查 schema 与构建文件
- THEN SHALL NOT 新增数据库表或列
- AND SHALL 复用现有 `cause_type` / steps JSON 保存类型化分类
- AND SHALL NOT 新增 Maven dependency、provider credential、package 或 lockfile 改动
