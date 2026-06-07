# M3 Backend Contract Decisions

## Purpose

This document records the backend API contract decisions confirmed by the user for M3 backend rectification.

Agents MUST follow the accepted decisions below unless a later OpenSpec change updates them.

## Decision Status Legend

- `Pending`: user confirmation required before implementation.
- `Accepted`: confirmed by user and ready for implementation.
- `Deferred`: intentionally postponed to a later OpenSpec change.

## 1. WeChat Login Contract

Status: Accepted

Accepted contract:

- endpoint: `POST /api/auth/wechat-login`
- request: `{ "code": "string" }`
- response: reuse the existing login response shape, including token and user info
- missing WeChat configuration should fail explicitly with a frontend-understandable unavailable/not-configured response

Implementation constraints:

- backend obtains OpenID through WeChat code2session
- backend MUST NOT trust client-supplied OpenID as login proof
- frontend auth service should reuse existing token handling where practical

## 2. Account Password And WeChat Relationship

Status: Accepted

Accepted contract:

- account/password login remains independent and available
- WeChat login creates or logs in a user by trusted OpenID
- binding an existing account/password user to WeChat is deferred

Implementation constraints:

- M3 MUST NOT remove account/password login
- M3 does not need to solve account merge or account binding edge cases
- in the demo, one person using both login paths may become separate users unless a later change implements binding

## 3. Reflection Field Contract

Status: Accepted

Accepted contract:

- store "你当时以为" as `beliefThen`
- store "后来其实" as `realityLater`
- preserve existing `content` as the original user text
- `beliefThen` is generated only when the user actively triggers AI organization

Implementation constraints:

- AI output MUST NOT replace original `content`
- AI failure MUST NOT block record save or seal when non-AI validation passes
- user-triggered AI organization should be represented clearly in backend contract and frontend flow

## 4. Later Reflection Endpoint

Status: Accepted

Accepted contract:

- endpoint: `PUT /api/records/{recordId}/later-reflection`
- request: `{ "realityLater": "string" }`
- allowed only when the record is `UNLOCKED` and owned by the user
- the user may submit `realityLater` at most 2 times after unlock

Implementation constraints:

- first submission creates or saves "后来其实"
- after the first submission, frontend may show a "修改" action
- after the second submission, frontend MUST NOT show the "修改" action
- backend MUST enforce the 2-submit limit even if the frontend sends extra requests
- attempts before unlock, by another user, or after the edit limit is exhausted MUST be rejected safely

## 5. Life Node Contract

Status: Accepted

Accepted contract:

- backend enum: `GRADUATION`, `WORK`, `MOVE`, `RELATIONSHIP`, `HEALTH`, `FAMILY`, `TURNING_POINT`, `OTHER`
- DTO fields: `lifeNodeType`, `lifeNodeCustomLabel`
- `lifeNodeCustomLabel` is valid only when `lifeNodeType = OTHER`
- for non-OTHER values, submitted custom labels MUST fail validation

Implementation constraints:

- do not build category management in M3
- do not add user-created life node types beyond the `OTHER` custom label
- keep NODE_RECORD behavior compatible with existing record lifecycle and ownership rules

## 6. Reminder Status Contract

Status: Accepted

Accepted contract:

- statuses: `REQUESTED`, `AUTHORIZED`, `DENIED`, `NOT_CONFIGURED`, `SEND_PENDING`, `SEND_SUCCESS`, `SEND_FAILED`, `SKIPPED_NO_OPENID`
- reminder status may be exposed to frontend when needed for seal/result UI
- `DENIED` should be recorded when the user refuses subscription authorization

Implementation constraints:

- missing template ID MUST become `NOT_CONFIGURED`
- `NOT_CONFIGURED` MUST NOT block seal or unlock
- `NOT_CONFIGURED` MUST NOT be recorded as fake send success
- successful sends remain idempotent by record and template type
- send failure remains non-blocking for unlock processing

## 7. Stage Summary Contract

Status: Accepted

Accepted contract:

- endpoint: `POST /api/stage-summaries/generate`
- response returns generated summary directly
- M3 generates summaries on demand only
- M3 does not persist stage summaries
- M3 may use a default recent-period scope first
- frontend entry point is Personal Center only

Implementation constraints:

- stage summary is triggered by explicit user action
- summary generation must use only the authenticated user's eligible data
- AI failure should return a safe fallback or explicit unavailable state
- do not build summary history, dashboard, scoring, diagnosis, or analytics center in M3

## 8. Verification Contract

Status: Accepted

Accepted contract:

- not-configured reminder behavior is automated-testable and required
- real WeChat delivery is manually verified after template ID configuration
- all implementation notes, verification evidence, skipped verification reasons, and manual WeChat verification results go to `.ai/AGENT_LOG.md`

Implementation constraints:

- real delivery verification may remain pending only when template IDs or WeChat configuration are unavailable
- pending real delivery verification must be stated explicitly in final handoff and `.ai/AGENT_LOG.md`

