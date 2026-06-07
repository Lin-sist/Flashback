# Active Task

## Task

Current task: implement `m3-demo-core-flow-hardening` backend rectification.

M2 backend optimization has been archived after its backend-core constraints were promoted into `openspec/specs/backend-core/spec.md`. Do not treat `openspec/changes/archive/2026-06-07-m2-backend-optimization/` as the active implementation source.

The current goal is to use `openspec/changes/m3-demo-core-flow-hardening/` as the active fact source before making backend code changes.

## Source Of Truth

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`
- `openspec/specs/miniapp-core/spec.md`
- `openspec/specs/v2-product-scope/spec.md`
- `openspec/specs/agent-collaboration/spec.md`
- `openspec/changes/m3-demo-core-flow-hardening/proposal.md`
- `openspec/changes/m3-demo-core-flow-hardening/design.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/backend-core/spec.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/miniapp-core/spec.md`
- `openspec/changes/m3-demo-core-flow-hardening/specs/v2-product-scope/spec.md`

Old `Docs/**` files may be used only as non-conflicting historical context. They are not the M3 implementation fact source.

Archived M2 documents may be used only as historical rationale when they do not conflict with accepted specs or M3 documents.

## Current Phase

M3 backend rectification may now proceed only after establishing current backend facts and confirming any open backend API contract decisions with the user.

Agents must first:

- classify relevant backend capabilities as `confirmed`, `partial`, `planned`, `out of scope`, or `unknown`
- compare actual backend behavior against accepted backend-core spec and M3 OpenSpec documents
- document gaps before implementation
- ask the user before finalizing uncertain backend API contracts
- implement only the smallest backend changes required by the current M3 task

## Primary M3 Backend Focus

- preserve account/password login
- add real WeChat Mini Program code login
- never trust client-supplied OpenID as login proof
- keep JWT response behavior aligned with the existing auth model where practical
- add structured record reflection support for `你当时以为` and `后来其实`
- ensure `你当时以为` is AI-organized without replacing user original content
- ensure `后来其实` can only be written after `UNLOCKED`
- add life node enum support with `OTHER` custom label
- complete real unlock reminder delivery path for WeChat subscription messages
- treat missing WeChat template ID as explicit `not_configured` behavior
- ensure reminder send is idempotent and non-blocking for unlock processing
- add user-manual stage summary generation
- preserve M2 lifecycle, ownership, privacy, stable query, and AI fallback constraints

## Explicit M3 Decisions

- M3 is demo core flow hardening, not production launch.
- Account/password login remains available.
- Real WeChat login is required.
- Real unlock reminders are required, but template IDs may be absent during implementation.
- Template ID absence must be explicit and must not be recorded as fake send success.
- Real reminder delivery must receive manual verification after template IDs are configured.
- AI is limited to gentle prompts and content organization.
- `你当时以为` is generated only when the user actively triggers AI organization.
- `后来其实` is user-authored after unlock only and may be submitted at most 2 times.
- Life nodes use fixed enum values first; `OTHER` allows user custom label.
- Non-`OTHER` life node custom labels must fail validation.
- Stage summary is triggered manually by the user, generated on demand, and not persisted in M3.
- Demo database rebuild is allowed.
- All implementation notes, verification evidence, skipped verification reasons, and manual WeChat verification results must be recorded in `.ai/AGENT_LOG.md`.

## Out Of Scope

Do not implement or expand the following in M3 unless a separate OpenSpec change explicitly says so:

- admin portal
- production deployment
- monitoring, alerting, or incident response
- SMS reminders
- production notification center
- admin template management
- campaign delivery
- complex retry orchestration
- complex AI growth analysis, scoring, diagnosis, or dashboards
- social feed or sharing features
- real MAP / IMAGE / VOICE capability
- H5/Web user-side acceptance target
- major frontend visual reconstruction
- broad backend rewrite
- package or lockfile updates unless required and explicitly justified

## File Reading Rules

Do not perform a full repository scan by default.

Before M3 backend implementation, read:

- `AGENTS.md`
- `.ai/ACTIVE_TASK.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`
- `openspec/changes/m3-demo-core-flow-hardening/proposal.md`
- `openspec/changes/m3-demo-core-flow-hardening/design.md`
- `openspec/changes/m3-demo-core-flow-hardening/tasks.md`
- relevant M3 spec deltas

After that, read only backend files directly required by the current M3 backend task. If extra files are needed, state the reason before reading them.

## Contract Confirmation Rule

Backend API contracts that are not already explicit in M3 OpenSpec must be confirmed with the user before implementation.

This includes:

- endpoint paths
- request/response DTO fields
- enum names
- persistence model choices where more than one reasonable option exists
- whether a capability is persisted or computed on demand
- frontend-visible error/status semantics

## Verification Required

For backend code changes:

- run focused backend tests where practical
- run `mvn -q test` from `backend` when feasible
- document manual verification when automated tests are not practical
- record backend work and evidence in `.ai/AGENT_LOG.md`

For WeChat-specific behavior:

- verify not-configured behavior without template IDs
- verify real delivery manually after template IDs are configured
- record manual WeChat Developer Tools verification evidence in `.ai/AGENT_LOG.md`

For documentation-only changes:

- verify active task and OpenSpec documents no longer conflict with the current phase
- include `git diff --stat`

## Output Required

- modified files
- what changed
- verification result
- skipped verification reason, if any
- `git diff --stat`
- scope safety check
- remaining risks
