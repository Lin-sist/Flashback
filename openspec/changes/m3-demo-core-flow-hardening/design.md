# M3 Demo Core Flow Hardening Design

## Purpose

This design constrains M3 implementation work for Flashback V2.0.

M3 should make the demo core flow complete and real enough to verify. It should not turn the project into a production system or a visual redesign project.

The target loop is:

```text
account/password or WeChat login
  -> create record
  -> AI organizes "你当时以为"
  -> save or seal
  -> request unlock reminder authorization
  -> unlock when time arrives
  -> send or record unlock reminder attempt
  -> time review
  -> user fills "后来其实"
  -> user manually generates stage summary
```

## Product Principles

### 1. Complete Flow Before Polish

M3 frontend work should prioritize whether a user can complete the demo loop. It should not perform major visual reconstruction or broad component abstraction.

Acceptable frontend work:

- connect real APIs
- add missing inputs and states
- fix flow blockers
- make empty/loading/error states understandable
- align visible naming

Avoided frontend work:

- full page redesign
- new visual system
- broad shared component extraction
- marketing-style landing page
- unrelated animation or decoration

### 2. Real Identity With Demo Fallback

M3 requires real WeChat login, but account/password login remains supported.

Account/password login is a valid development and demo fallback. It must not be removed during M3.

WeChat login should use Mini Program code login:

```text
frontend uni.login
  -> backend /auth/wechat-login
  -> WeChat code2session
  -> openid lookup/create/bind
  -> JWT response
```

When WeChat configuration is missing, the failure must be explicit and understandable. It should not silently fall back to a fake OpenID.

### 3. Original Content Is Sacred

User original content MUST remain preserved.

AI may organize:

- gentle writing prompts
- summary-like content
- "你当时以为"
- stage summary text

AI MUST NOT:

- replace original user content
- change record lifecycle state
- block non-AI record operations when it fails
- become the only source of truth for user memory

### 4. Reflection Fields Have Clear Timing

"你当时以为" is generated only when the user actively triggers AI organization.

"后来其实" is user-authored after unlock only. The user may submit it at most 2 times after unlock. After the second submission, the backend must reject further updates and the frontend must stop showing the "修改" action.

The backend should enforce this timing rule. The frontend should make the timing legible.

### 5. Life Nodes Are Structured But Small

M3 life node records should use fixed enum values first:

- GRADUATION
- WORK
- MOVE
- RELATIONSHIP
- HEALTH
- FAMILY
- TURNING_POINT
- OTHER

Visible Chinese labels may be:

- 毕业
- 工作
- 搬家
- 关系
- 健康
- 家庭
- 转折
- 其他

When the enum value is OTHER, the user may provide a custom life node label.

Agents MUST NOT introduce a large taxonomy, user-created category management, or admin-managed node system in M3.

### 6. Reminder Delivery Is Real But Demo-Scoped

M3 should implement the real WeChat subscription-message delivery path for unlock reminders.

However, WeChat template IDs are not currently available. The design must support this safely:

- template ID is configuration-driven
- missing template ID produces an explicit not-configured state
- not-configured state does not block seal
- not-configured state does not block unlock
- not-configured state should be visible in logs or reminder status
- no fake success should be recorded

Reminder behavior must be:

- idempotent for successful sends by record and template type
- non-blocking for unlock processing
- privacy-safe in logs
- limited to unlock reminders

M3 MUST NOT build a notification center, SMS system, admin template manager, or campaign system.

### 7. Stage Summary Is Manual And Lightweight

Stage summary generation is triggered by user manual action.

It may combine:

- recent record count
- life node records
- unlocked records
- "你当时以为"
- "后来其实"
- tags or record types
- AI-organized gentle summary text

It should not become a dashboard, scoring system, mental-health assessment, or complex analytics product.

### 8. Database Rebuild Is Allowed

M3 may update schema files and test schema directly.

Because this remains a demo, database rebuild is allowed. Agents do not need to create a full migration framework in M3.

Agents should still keep schema changes narrow and document rebuild expectations.

## Backend Design Notes

### Contract Confirmation Gate

M3 backend work must not finalize uncertain API contracts by assumption.

Before implementation, agents must confirm user decisions for:

- endpoint paths when no existing endpoint clearly applies
- request and response DTO field names
- enum names exposed to frontend
- whether a feature is persisted or computed on demand
- frontend-visible error or status semantics
- reminder status values that may be shown to the Mini Program

Open contract questions are tracked in `openspec/changes/m3-demo-core-flow-hardening/backend-contract-decisions.md`.

If a backend implementation can use an existing endpoint without changing the external contract, agents may proceed after documenting that fact in `.ai/AGENT_LOG.md`.

### Authentication

Backend should support both:

- existing account/password login
- WeChat code login

The existing JWT model should be reused unless there is a verified blocker.

OpenID must be treated as a trusted server-side value obtained through WeChat code2session, not as a client-supplied identity.

The accepted WeChat login endpoint is `POST /api/auth/wechat-login` with request `{ "code": "string" }`. Account/password to WeChat binding is deferred in M3.

### Records

Record data should be extended narrowly for M3 reflection fields.

Accepted external fields:

- `beliefThen` for "你当时以为"
- `realityLater` for "后来其实"
- life_node_type
- life_node_custom_label

Persistence naming may follow existing backend style if the API contract remains stable.

### AI

AI operations should be separate from core record persistence where practical.

If AI organization fails:

- record creation may continue
- record update may continue
- seal may continue
- user original content remains intact
- frontend receives a safe fallback or an explicit AI-unavailable state

### Reminder

Reminder implementation should build on M2's foundation where available.

Recommended states:

- requested
- authorized
- denied
- not_configured
- send_pending
- send_success
- send_failed

Exact enum names may differ, but implementation must distinguish successful delivery from missing configuration and failed delivery.

Real delivery has two verification levels:

- without template ID: verify `not_configured`, idempotency, and non-blocking unlock behavior
- after template ID is configured: manually verify real WeChat subscription-message delivery and record the result in `.ai/AGENT_LOG.md`

### Stage Summary

Stage summaries are generated on demand through `POST /api/stage-summaries/generate` and are not persisted in M3.

The API response must be scoped to the authenticated user and must not mix other users' records. The frontend entry point is Personal Center only.

## Frontend Design Notes

### Login

Login page should make both paths available:

- WeChat login as the Mini Program primary path
- account/password as fallback or development path

Preview mode may remain, but must stay clearly separate from real auth readiness.

### Record Editor

Record editor should support:

- record type
- unlock time
- original content
- AI organization for "你当时以为"
- life node enum when record type is NODE_RECORD
- custom life node label when enum is OTHER

### Seal Flow

After seal succeeds, the frontend may request subscription authorization.

Refusing subscription authorization must not undo the seal operation.

### Time Review

Time review should show:

- original content
- "你当时以为"
- unlock time or arrived status
- "后来其实" input after unlock
- optional reply behavior where already supported

### Stage Summary

Stage summary should be triggered by a user action in Personal Center.

Do not make stage summary a forced step in the record lifecycle.

## Verification Strategy

M3 implementation should verify:

1. account/password login still works
2. WeChat login works with valid configuration or fails explicitly without fake success
3. record creation preserves original content
4. AI organization can populate "你当时以为"
5. AI failure does not block record save/seal
6. sealed records remain immutable
7. unlock task unlocks eligible records
8. "后来其实" is rejected before unlock
9. "后来其实" is accepted after unlock and rejected after the 2-submit limit is exhausted
10. life node enum and OTHER custom label work
11. reminder send is idempotent and non-blocking
12. missing template ID is recorded as not configured
13. stage summary is generated only by manual user action
14. frontend type-check and Mini Program build pass where feasible
15. manual WeChat Developer Tools verification is documented where automation is not practical
16. all implementation notes, verification evidence, skipped verification reasons, and manual WeChat verification results are written to `.ai/AGENT_LOG.md`
17. stage summary is generated on demand and not persisted in M3

## Phase Boundaries

### Backend Phase

Backend agents may update backend code, database schema, backend tests, and backend-facing API contracts within M3 scope.

Backend agents should not implement frontend pages unless the task explicitly enters the integration phase.

### Frontend Phase

Frontend agents may connect M3 backend contracts to Mini Program pages and services after the backend contract is confirmed.

Frontend agents should not change backend behavior unless the task explicitly returns to the backend phase.

### Integration Phase

Integration work may connect frontend and backend, run end-to-end demo flow checks, and document manual WeChat Developer Tools verification.

Integration work must still avoid production launch, admin, notification center, SMS, monitoring, and large visual redesign scope.

## Risk Controls

### Scope Drift Into Production

If work adds admin management, production notification center, SMS, deployment, monitoring, or campaign delivery, it is out of M3 scope.

### AI Expansion

If work turns AI into scoring, diagnosis, long-term psychological analysis, or an unavoidable lifecycle dependency, it is out of M3 scope.

### Visual Overreach

If work becomes a major frontend visual redesign, it is out of M3 scope.

### Fake Real Integrations

If WeChat login or reminder delivery silently uses fake production-like success, it violates M3.

Missing configuration should be explicit, not hidden.
