# tasks.md

# Backend Optimization Tasks

## 0. Guardrails

-  Do not modify M1 frontend visual OpenSpec tasks unless there is direct evidence for the specific unchecked acceptance item.
-  Do not close frontend visual evidence, bottom navigation long-content checks, or demo-only control checks as part of backend work.
-  Do not implement production-grade WeChat subscription messages in this change.
-  Do allow a minimal WeChat Mini Program subscription message foundation for record unlock reminders when it remains idempotent, non-blocking, and demo-scoped.
-  Do not implement SMS reminders, a production notification center, admin template management, or campaign delivery in this change.
-  Do not implement real MAP / IMAGE / VOICE capabilities in this change.
-  Do not implement admin portal, production deployment, monitoring, or alerting in this change.
-  Do not enhance AI capability beyond verifying minimal fallback boundaries.
-  Do not modify package or lockfile files unless required and explicitly justified.
-  Record all backend work and findings in `.ai/AGENT_LOG.md`.

## 1. Establish Backend Fact Source

-  Read `AGENTS.md`.
-  Read `.ai/ACTIVE_TASK.md`.
-  Read `.ai/HANDOFF_M1_VISUAL.md`.
-  Read this OpenSpec change.
-  Identify backend entry points without full repository scanning.
-  Confirm actual backend modules related to authentication, records, replies, tags, timeline, unlock task, and AI fallback.
-  Confirm actual backend modules related to WeChat identity, message/reminder persistence, unlock notice logging, and preview bypass behavior.
-  Create a backend fact checklist with the following labels:
  - confirmed
  - partial
  - planned
  - out of scope
  - unknown
-  Compare actual code behavior against existing documentation.
-  Document gaps between docs and code.

## 2. Audit Record Lifecycle

-  Confirm supported record states:
  - DRAFT
  - SEALED
  - UNLOCKED
-  Confirm supported record types:
  - FUTURE_LETTER
  - NODE_RECORD
  - EMOTION_NOTE
-  Verify draft creation behavior.
-  Verify draft update behavior.
-  Verify seal behavior.
-  Verify sealed records cannot be edited through normal user APIs.
-  Verify sealed records preserve original user content.
-  Verify unlock time calculation.
-  Verify scheduled unlock task behavior.
-  Verify repeated unlock task execution is idempotent.
-  Verify missed unlock task recovery behavior or document the current limitation.
-  Verify timezone handling and document the expected timezone source.
-  Verify unlocked record detail behavior.
-  Verify reply creation after unlock.
-  Verify reply behavior before unlock is rejected or safely unavailable.

## 3. Audit Privacy, Authentication, and Data Ownership

-  Verify JWT authentication flow.
-  Verify user identity is consistently resolved from authenticated context.
-  Verify users can only list their own records.
-  Verify users can only read details of their own records.
-  Verify users can only create, update, seal, or reply to their own records.
-  Verify timeline data is scoped to the current user.
-  Verify tags are scoped to the current user or safely shared by design.
-  Treat V2 demo tags as system-shared/global tags unless a later change introduces user-created private tags.
-  Verify record-tag relationships and tag filtering remain protected by record ownership even when tag definitions are shared.
-  Verify unlock task cannot expose or mutate records across users incorrectly.
-  Check for sensitive log output involving record content, tokens, or user identifiers.
-  Document any authorization gaps as P1 defects.

## 4. Align Backend API Contract with V2.0 Frontend

-  Map backend APIs to frontend pages:
  - Home
  - 我的记录
  - 时光轴
  - 新建记录
  - 封存详情
  - 回看详情
  - 个人中心
-  Confirm API support for draft list or draft visibility.
-  Confirm API support for sealed records.
-  Confirm API support for unlocked records.
-  Confirm API support for record detail.
-  Confirm API support for replies.
-  Confirm API support for tags.
-  Confirm API support for timeline display.
-  Identify frontend mock data or compatibility code that should be replaced by real backend contracts.
-  Document response fields required by V2.0 frontend.
-  Document missing or unstable fields.

## 5. Stabilize List, Timeline, and Tag Queries

-  Verify pagination behavior for record lists.
-  Verify stable sorting for record lists.
-  Ensure record list ordering uses a deterministic tie-breaker such as `created_at DESC, id DESC`.
-  Verify filtering by state.
-  Verify filtering by record type.
-  Verify filtering by tag.
-  Verify timeline aggregation behavior.
-  Verify empty-state behavior.
-  Verify deleted, unavailable, or malformed record behavior if applicable.
-  Check whether database indexes support expected query patterns.
-  Document any performance or correctness risks.

## 6. Audit WeChat Subscription Message Foundation

-  Confirm whether the current user model has an `openid` field.
-  Confirm whether the current login system can support future WeChat `openid` binding.
-  Confirm whether preview bypass mode can skip real subscription authorization and delivery while keeping demo fallback behavior.
-  Confirm whether the record seal flow has a suitable frontend moment to request Mini Program subscription authorization.
-  Confirm whether the unlock task has a safe hook after records transition from `SEALED` to `UNLOCKED`.
-  Confirm whether message, notification, reminder, or unlock notice tables already exist.
-  If persistence is missing or insufficient, propose the smallest data model:
  - `user_wechat_identity` or `user.openid`
  - `record_reminder` or `notification_outbox`
  - `notification_log`
-  Ensure subscription-message success is idempotent by `record_id + template_type`.
-  Ensure failed send attempts can be recorded without blocking unlock processing.
-  Ensure logs do not include sensitive record content, tokens, or unnecessary user identifiers.
-  Treat phone number acquisition only as future WeChat login/account-binding evaluation, not as SMS reminder implementation.
-  Document whether this foundation should be implemented in M2 or split into a follow-up subtask.

## 7. Verify AI Fallback Boundary

-  Confirm where minimal AI fallback is used.
-  Confirm AI fallback failure does not block draft creation.
-  Confirm AI fallback failure does not block sealing.
-  Confirm AI fallback failure does not block unlocking.
-  Confirm AI fallback does not rewrite or replace user original content.
-  Confirm AI fallback does not change record lifecycle state.
-  Confirm AI fallback can be disabled or safely bypassed, or document current limitation.
-  Document AI fallback as supporting capability, not core product dependency.

## 8. Validation and Evidence

-  Add or update backend tests where practical.
-  Add manual verification notes for flows that lack tests.
-  Verify core flow:
  - create draft
  - update draft
  - seal record
  - wait or simulate unlock
  - view unlocked record
  - create reply
-  Verify cross-user access rejection.
-  Verify repeated unlock task run.
-  Verify list and timeline response consistency.
-  Verify notification foundation behavior where implemented:
  - preview bypass skips real delivery
  - unlock processing remains non-blocking
  - successful send/outbox handling is idempotent
-  Record validation evidence in `.ai/AGENT_LOG.md`.

## 9. Final Review

-  Confirm this change did not expand into deferred production modules.
-  Confirm frontend visual M1 scope was not accidentally modified.
-  Confirm API contract findings are documented.
-  Confirm WeChat subscription message foundation findings are documented.
-  Confirm backend fact source is updated.
-  Confirm remaining risks are listed clearly.
-  Confirm next recommended backend implementation tasks are ready for handoff.
