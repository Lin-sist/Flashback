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
