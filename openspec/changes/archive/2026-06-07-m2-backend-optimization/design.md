# design.md

# Backend Optimization Design

## Purpose

This document defines the engineering design constraints for `m2-backend-optimization`.

The goal of this change is not to expand Flashback V2.0 into a production backend platform. The goal is to establish a reliable backend fact source and improve correctness for the existing demo-oriented Mini Program backend.

Backend optimization should prioritize:

1. record lifecycle correctness
2. privacy and ownership safety
3. API contract alignment with the V2.0 frontend
4. stable list, timeline, tag, and reply behavior
5. safe minimal AI fallback boundaries
6. minimal WeChat subscription message foundation for unlock reminders

It should avoid broad rewrites, production-only modules, and speculative feature expansion.

## Product Context

Flashback V2.0 is a WeChat Mini Program demo for presentation and review.

The core expression is:

> 写下此刻 · 时间回看

The product is not:

- an efficiency dashboard
- a social feed
- an admin system
- a production SaaS backend
- an AI-first writing tool
- a production notification platform

The backend should support a private, restrained, gentle record-and-review experience.

The core user loop is:

```text
新建记录 -> 保存草稿 / 封存 -> 到期解锁 -> 时间回看 -> 可选回信
```

## Current Baseline

Current documented user-side backend baseline includes:

- authentication
- record draft / seal / list / detail
- unlocked records
- replies
- tags
- timeline
- unlock task
- minimal AI fallback ability

Current record states:

- `DRAFT`
- `SEALED`
- `UNLOCKED`

Current record types:

- `FUTURE_LETTER`
- `NODE_RECORD`
- `EMOTION_NOTE`

Earlier backend technical direction:

- Spring Boot
- MyBatis
- MySQL
- Redis
- JWT
- scheduled unlock task

These are treated as current direction or existing baseline only when verified against actual code.

## Design Principles

### 1. Fact Source First

Agents must not assume that existing docs, old plans, or frontend expectations represent implemented backend behavior.

Before changing code, agents should classify backend capabilities as:

- confirmed
- partial
- planned
- out of scope
- unknown

Unknown or partial behavior should be documented before being fixed.

### 2. Minimal Safe Change

Prefer small, targeted changes that improve correctness.

Avoid:

- large backend rewrites
- framework replacement
- database/schema redesign unless clearly required
- broad abstraction work
- speculative cleanup
- production hardening unrelated to current demo needs

### 3. Lifecycle Before Features

Record lifecycle correctness has priority over feature expansion.

The backend must first protect the core state flow:

```text
DRAFT -> SEALED -> UNLOCKED
```

Key rules:

- draft records may be editable by their owner
- sealed records should not be editable through normal user APIs
- unlocked records should be readable by their owner
- unlock processing should be idempotent
- repeated unlock task execution must not create duplicate side effects
- missed unlock behavior must either be handled or clearly documented

### 4. Privacy Before Convenience

Flashback records are private emotional content.

The backend must treat privacy and ownership as first-class requirements.

Every user-facing record, reply, tag, and timeline API should be checked for:

- authenticated user identity
- ownership scope
- cross-user access prevention
- safe not-found or forbidden behavior
- sensitive log avoidance

The default assumption is:

> 我的记录只能我看。

### 5. Frontend Contract Alignment

The V2.0 frontend mental model is:

- Home
- 我的记录
- 时光轴
- 新建记录
- 封存详情
- 回看详情
- 回信
- 标签
- 个人中心

Backend APIs should be reviewed against these pages.

The frontend should not need excessive mock data or hidden compatibility logic for core flows.

If a backend response field is required by the V2.0 frontend but does not exist or is unstable, document it as a contract gap before changing implementation.

### 6. Demo-Oriented Robustness

This is a demo and review project, not a production launch.

Backend optimization should make the demo trustworthy and coherent, especially for:

- record creation
- draft editing
- sealing
- unlocking
- viewing unlocked records
- replying
- listing records
- timeline display
- tag filtering

Do not introduce production-only complexity unless it directly protects the current demo flow.

### 7. Notification Foundation, Not Notification Center

M2 may include the minimal WeChat Mini Program subscription message foundation needed for record unlock reminders.

This foundation should answer and, where safe, minimally implement:

- whether the current user model has `openid`
- how local-account users can later bind a WeChat identity
- how preview bypass mode avoids real subscription authorization and delivery
- where the frontend seal flow should request Mini Program subscription authorization
- where the unlock task should enqueue or attempt reminder sending after `SEALED -> UNLOCKED`
- how successful sends are idempotent by `record_id + template_type`
- how failed sends are logged without blocking unlock processing
- how logs avoid sensitive record content, tokens, and unnecessary user identifiers

M2 must not build:

- a production notification center
- SMS reminders
- campaign management
- admin template management
- broad retry orchestration or observability infrastructure

## Out of Scope

This change must not implement or expand the following unless a separate OpenSpec change is created:

- production-grade WeChat subscription messages beyond the minimal unlock-reminder foundation
- SMS reminder delivery
- real MAP / IMAGE / VOICE implementation
- admin portal
- production deployment
- monitoring / alerting / incident response
- AI capability enhancement
- H5/Web user-side acceptance target
- major frontend visual reconstruction
- package or lockfile updates unless explicitly justified
- full database redesign unless required by verified P1 correctness defects

## Backend Areas to Review

### Authentication

Review:

- login flow
- JWT generation
- JWT parsing
- authenticated user context
- token failure behavior
- unauthenticated request rejection

Do not change authentication design broadly unless an ownership or correctness defect requires it.

### Records

Review:

- draft creation
- draft update
- seal operation
- record list
- record detail
- unlocked record query
- state transitions
- type validation
- ownership checks

Records are the highest-priority backend domain.

### Unlock Task

Review:

- scheduled task entry point
- eligible record selection
- unlock time comparison
- timezone behavior
- idempotency
- repeated execution
- missed execution recovery or limitation
- ownership safety

The unlock task should only process eligible `SEALED` records whose unlock time has arrived.

### Replies

Review:

- reply creation
- reply visibility
- reply ownership
- reply relationship to unlocked records
- behavior before unlock

Replies should not allow bypassing record ownership or unlock rules.

### Tags

Review:

- tag creation or selection behavior
- tag ownership model
- tag filtering
- tag list behavior
- record-tag relationship

For the V2 demo, tag definitions are system-shared/global unless a later OpenSpec change introduces user-created private tags.

This means:

- tag list APIs may return shared enabled tags
- tag definitions do not need per-user ownership in M2
- record-tag relationships must remain protected by record ownership
- tag filtering must only return records owned by the authenticated user

### Timeline

Review:

- timeline source data
- timeline ordering
- user scope
- pagination or grouping
- empty state
- relationship to sealed and unlocked records

Timeline should reflect the V2.0 frontend concept of delayed review and personal memory flow.

### WeChat Subscription Message Foundation

Review:

- user identity fields, especially `openid`
- whether a future WeChat login or binding flow can attach `openid` to the current user
- preview bypass behavior for subscription authorization and sending
- frontend API contract around seal success and subscription authorization timing
- unlock task extension point after a record is successfully transitioned to `UNLOCKED`
- existing message, notification, reminder, or unlock notice tables
- minimal data model needs:
  - `user_wechat_identity` or `user.openid`
  - `record_reminder` or `notification_outbox`
  - `notification_log`
- idempotency rule: one successful send per `record_id + template_type`
- failure behavior: record failure without blocking the unlock task
- sensitive log avoidance

The preferred implementation is an incremental foundation. It should not introduce a production notification subsystem.

### AI Fallback

Review:

- where AI fallback is invoked
- whether it blocks record operations
- whether it mutates original user content
- whether it changes lifecycle state
- whether it can fail safely

AI fallback is supporting capability only. It must not become the core state driver.

## Recommended Work Sequence

Agents should follow this sequence:

1. Read handoff context:
   - `AGENTS.md`
   - `.ai/ACTIVE_TASK.md`
   - `.ai/HANDOFF_M1_VISUAL.md`
   - this OpenSpec change
2. Identify relevant backend files for the current task only.
3. Build a backend fact checklist:
   - authentication
   - records
   - replies
   - tags
   - timeline
   - unlock task
   - WeChat subscription message foundation
   - AI fallback
4. Compare actual code behavior with documented expectations.
5. Record gaps before implementation.
6. Fix only the smallest necessary backend defects.
7. Add or update tests where practical.
8. Record manual verification evidence when automated tests are not practical.
9. Update `.ai/AGENT_LOG.md`.
10. Leave deferred modules untouched.

## Agent File Reading Rules

Agents should not perform a full repository scan by default.

Agents should first read:

```text
AGENTS.md
.ai/ACTIVE_TASK.md
.ai/HANDOFF_M1_VISUAL.md
openspec/changes/m2-backend-optimization/proposal.md
openspec/changes/m2-backend-optimization/tasks.md
openspec/changes/m2-backend-optimization/design.md
openspec/changes/m2-backend-optimization/specs/backend-core/spec.md
```

After that, read only files directly required by the current backend task.

If extra files are needed, the agent should state the reason before reading them.

## Implementation Strategy

### Preferred Approach

Use incremental backend changes:

- verify current behavior
- write down observed gaps
- fix P1 correctness defects first
- keep API changes minimal
- preserve existing frontend-compatible behavior
- avoid unnecessary model/schema churn
- add tests around state and ownership where possible

### Avoided Approach

Do not start with:

- large refactor
- new architecture
- new admin system
- production notification subsystem
- new AI pipeline
- production deployment setup
- broad package upgrades
- unrelated formatting changes

## API Contract Design Notes

Backend responses should be reviewed for whether they support the V2.0 frontend pages.

For records, responses should make the following clear when applicable:

- record id
- owner identity handled internally
- title or display text
- record type
- record state
- created time
- updated time
- sealed time
- unlock time
- unlocked time
- tags
- reply availability
- frontend display status

Do not add fields blindly. Add or document fields only when required by the current frontend flow or backend correctness.

## Data and Time Design Notes

Time behavior must be explicit.

Review and document:

- stored timezone assumptions
- server timezone assumptions
- unlock time comparison source
- frontend displayed time expectations
- behavior when unlock time is in the past
- behavior when scheduled unlock task is delayed
- deterministic ordering for paginated lists, including a stable tie-breaker such as `created_at DESC, id DESC`

For this demo project, the most important requirement is consistency and explainability, not production-grade distributed scheduling.

## Testing Strategy

Prioritize tests or manual checks for:

1. create draft
2. update draft
3. seal draft
4. reject edit after seal
5. unlock eligible sealed record
6. repeated unlock task execution
7. view unlocked record
8. create reply after unlock
9. reject cross-user record detail access
10. reject cross-user mutation
11. list only current user records
12. timeline only current user records
13. tag filtering respects ownership or documented sharing rules
14. WeChat subscription message foundation is idempotent and non-blocking where implemented
15. AI fallback failure does not block core record flow

Where automated tests are not practical, record manual verification evidence in `.ai/AGENT_LOG.md`.

## Risk Areas

### Risk: Agent Expands Scope

Backend agents may attempt to turn the M2 subscription-message foundation into reminders, notification-center behavior, admin portals, or production deployment.

Mitigation:

- keep non-goals explicit
- require separate OpenSpec change for deferred modules
- reject unrelated implementation changes

### Risk: Agent Refactors Before Understanding

Agents may rewrite services before confirming actual behavior.

Mitigation:

- require fact checklist first
- require code/doc gap notes
- prefer minimal safe changes

### Risk: Frontend Mock Hides Backend Gaps

The V2.0 frontend may still rely on mock data or compatibility fields.

Mitigation:

- map frontend pages to backend APIs
- document contract gaps
- fix only core flow blockers in this change

### Risk: Privacy Bugs Are Missed

List/detail/timeline/tag/reply APIs may accidentally expose cross-user data.

Mitigation:

- treat ownership checks as P1
- verify every user-facing API
- add tests or manual evidence for cross-user rejection

### Risk: Unlock Task Is Not Idempotent

Repeated scheduled task execution may create duplicate effects or inconsistent state.

Mitigation:

- verify eligible record selection
- ensure only `SEALED` records are unlocked
- repeated execution should be safe
- document missed-task behavior if not fully handled

## Acceptance Design Summary

This design is satisfied when backend work remains focused on:

- verified backend facts
- record lifecycle correctness
- privacy and ownership safety
- frontend API contract alignment
- stable list/timeline/tag behavior
- safe AI fallback boundaries
- minimal, idempotent, non-blocking WeChat subscription message foundation

This design is not satisfied if the change becomes:

- a productionization project
- an admin platform project
- an AI enhancement project
- a production notification-center project
- a broad backend rewrite
- a frontend visual cleanup project
