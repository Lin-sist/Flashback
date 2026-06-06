# proposal.md

# Backend Optimization Foundation for Flashback V2.0

## Background

Flashback V2.0 is currently a WeChat Mini Program demo for presentation and review. The product expression is “写下此刻” and the intended experience is private, restrained, gentle, paper-like, and emotionally quiet.

M1 frontend visual reconstruction is largely complete. The frontend has completed page-level high-fidelity translation for Home, Timeline, Profile, My Records, New Record, sealed/unlocked detail, Login, and Profile setting subpages.

However, the current primary OpenSpec change is still focused on frontend visual foundation. It should not be treated as the backend fact source.

The next engineering phase needs a dedicated backend optimization OpenSpec change to clarify backend scope, verify actual implementation status, and guide follow-up backend work without expanding into production-only features.

## Current Backend Baseline

Based on existing documentation, V1.0.1 user-side baseline includes:

- authentication
- record draft / seal / list / detail
- unlocked records
- replies
- tags
- timeline
- unlock task
- minimal AI fallback ability

Current record states:

- DRAFT
- SEALED
- UNLOCKED

Current record types:

- FUTURE_LETTER
- NODE_RECORD
- EMOTION_NOTE

The expected core product loop is:

新建记录 -> 保存草稿 / 封存 -> 到期解锁 -> 时间回看 -> 可选回信

Earlier backend direction refers to:

- Spring Boot
- MyBatis
- MySQL
- Redis
- JWT
- scheduled unlock task

## Problem

The project currently lacks a backend-focused OpenSpec fact source.

Before starting backend optimization, the project needs to separate confirmed backend capabilities from planned or demo-only capabilities, and define acceptance criteria for the core backend loop.

The most important backend trust point is not feature quantity. It is whether private records reliably belong to the correct user, follow the correct lifecycle, unlock correctly, and support the frontend V2.0 user experience without excessive mock or compatibility logic.

## Goals

This change aims to:

1. Establish a backend optimization fact source for Flashback V2.0.
2. Audit and stabilize the record lifecycle:
   - DRAFT -> SEALED -> UNLOCKED
   - draft editability
   - sealed immutability
   - unlock timing
   - unlock idempotency
3. Verify privacy, authentication, authorization, and data ownership boundaries.
4. Align backend API behavior with the V2.0 frontend mental model:
   - 我的记录
   - 时光轴
   - 时间回看
   - 回信
   - 标签
5. Improve list, timeline, tag, status filtering, pagination, and ordering stability.
6. Define the minimal safe boundary for AI fallback so it does not block or pollute the record lifecycle.
7. Add a minimal WeChat Mini Program subscription message foundation for record unlock reminders.
8. Keep production-only modules out of this phase unless separately specified.

## Non-Goals

This change does not include:

- production-grade WeChat subscription messages
- a production notification center
- SMS reminder delivery
- real MAP / IMAGE / VOICE implementation
- admin portal
- production deployment
- monitoring, alerting, or incident response
- AI capability enhancement
- H5/Web user-side acceptance target
- major frontend visual reconstruction
- database/schema rewrite unless required by verified backend defects
- package or lockfile changes unless explicitly justified

## Scope

### P0: Backend Fact Source

Create a backend-focused OpenSpec change and verify actual code against existing docs.

The output should identify:

- confirmed implemented backend features
- partially implemented backend features
- frontend mock or compatibility assumptions
- planned but not implemented features
- risks that affect the demo experience

### P1: Record Lifecycle and Unlock Correctness

Audit and stabilize:

- draft creation
- draft update
- seal operation
- sealed record immutability
- unlock time calculation
- scheduled unlock execution
- repeated unlock task execution
- missed unlock task recovery
- timezone consistency
- unlocked record read behavior
- reply creation after unlock

### P1: Privacy, Authentication, and Data Ownership

Audit and stabilize:

- JWT authentication
- user identity binding
- record ownership checks
- list/detail authorization
- reply ownership
- tag ownership
- timeline ownership
- unlock task ownership safety
- sensitive log output
- cross-user access prevention

### P2: Frontend API Contract Alignment

Ensure backend APIs can support the V2.0 frontend user mental model:

- 我的记录
- 时光轴
- 时间回看
- 回信
- 标签筛选
- 状态筛选
- 草稿 / 封存 / 已解锁 grouping

The frontend should not rely on excessive mock data, hardcoded compatibility layers, or hidden assumptions for core backend behavior.

### P2: List, Timeline, and Tag Query Stability

Improve or verify:

- pagination
- sorting
- indexes
- status filtering
- type filtering
- tag filtering
- timeline aggregation
- empty states
- deleted or unavailable data behavior

### P3: AI Fallback Boundary

Confirm that minimal AI fallback:

- is optional or safely degradable
- does not block draft, seal, unlock, list, or detail flows
- does not rewrite or pollute user original text
- does not change record lifecycle state
- fails safely with clear fallback behavior

### P3: WeChat Subscription Message Foundation

Add the smallest foundation required for WeChat Mini Program unlock reminders without turning M2 into a production notification system.

This includes:

- confirming whether the current user model already has `openid`
- confirming how local login can later bind to a WeChat identity
- keeping preview bypass mode on demo fallback behavior instead of real subscription delivery
- identifying the frontend seal-flow moment for requesting Mini Program subscription authorization
- adding or documenting the backend hook after records become `UNLOCKED`
- defining minimal persistence for reminder/outbox/log behavior
- requiring idempotency for successful sends by `record_id + template_type`
- ensuring notification failure does not block unlock processing
- avoiding sensitive record content, token data, or unnecessary user identifiers in logs

This does not include:

- production-grade notification center behavior
- SMS reminder implementation
- admin management of templates or campaigns
- large-scale retry orchestration
- real WeChat release hardening

### P4: Deferred Modules

The following remain planned or deferred unless a separate change is opened:

- production-grade notification center
- SMS reminder delivery
- admin portal
- real media/location capability
- production observability
- enhanced AI features

## Acceptance Criteria

This change is accepted when:

1. Backend capabilities are documented as confirmed, partial, planned, or out of scope.
2. Record lifecycle behavior is verified against DRAFT, SEALED, and UNLOCKED states.
3. Sealed records cannot be edited through normal user APIs.
4. Unlock operation is idempotent and safe to run repeatedly.
5. User-owned data cannot be accessed by other users through list/detail/timeline/reply/tag APIs.
6. Backend APIs are mapped to V2.0 frontend pages and core user flows.
7. List, timeline, and tag queries have stable pagination and sorting expectations.
8. AI fallback failure does not block core record operations.
9. WeChat subscription message foundation is either implemented minimally or documented with precise follow-up tasks.
10. Notification send attempts are designed to be idempotent and non-blocking for unlock processing.
11. Production-only features remain outside this phase.
12. Any code changes are logged in `.ai/AGENT_LOG.md`.

## Implementation Notes

Agents should not perform a full repository scan by default.

Before implementation, read only:

- AGENTS.md
- .ai/ACTIVE_TASK.md
- .ai/HANDOFF_M1_VISUAL.md
- openspec/project.md
- openspec/changes/m2-backend-optimization/proposal.md
- openspec/changes/m2-backend-optimization/design.md
- openspec/changes/m2-backend-optimization/tasks.md
- openspec/changes/m2-backend-optimization/specs/backend-core/spec.md
- backend files directly required by the current task

Additional files may be read only when the reason is stated first.

This change should preserve the demo nature of Flashback V2.0. Backend optimization should improve correctness, trust, and API alignment, not expand the product into a production platform.
