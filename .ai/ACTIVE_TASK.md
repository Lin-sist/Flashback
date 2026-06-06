# Active Task

## Task

Current task: enter `m2-backend-optimization` backend implementation preparation.

M1 frontend visual foundation and User Center settings-style subpage reconstruction are complete enough for M2 backend work to proceed. Do not treat old M1 frontend implementation instructions as the active task.

The current goal is to use `openspec/changes/m2-backend-optimization/` as the active backend fact source before making backend code changes.

## Source Of Truth

- `AGENTS.md`
- `openspec/project.md`
- `.ai/HANDOFF_M1_VISUAL.md`
- `openspec/changes/m2-backend-optimization/proposal.md`
- `openspec/changes/m2-backend-optimization/design.md`
- `openspec/changes/m2-backend-optimization/tasks.md`
- `openspec/changes/m2-backend-optimization/specs/backend-core/spec.md`

Old `Docs/**` files may be used only as non-conflicting historical context. They are not the backend implementation fact source.

## Current Phase

M2 backend optimization may now proceed to backend code review and targeted backend changes.

Agents must first establish backend facts and constraints before modifying code:

- classify backend capabilities as `confirmed`, `partial`, `planned`, `out of scope`, or `unknown`
- compare actual backend behavior against the M2 OpenSpec documents
- document gaps before implementation
- fix only the smallest backend defects required by the current M2 task

## Primary M2 Focus

- record lifecycle correctness: `DRAFT -> SEALED -> UNLOCKED`
- sealed record immutability
- unlock timing, timezone consistency, and idempotency
- privacy, authentication, authorization, and ownership boundaries
- frontend API contract alignment for 我的记录、时光轴、时间回看、回信、标签
- stable pagination, sorting, status filtering, type filtering, and tag filtering
- WeChat Mini Program subscription message foundation for record unlock reminders
- safe minimal AI fallback boundaries

## Explicit M2 Decisions

- V2 tags are system-shared/global tags for the current demo unless a later change introduces user-created private tags.
- Tag list APIs may return shared enabled tags.
- Record/tag filtering and record-tag relationships must remain protected by record ownership.
- Record list pagination must use deterministic ordering; when sorting by creation time, use an explicit stable tie-breaker such as `created_at DESC, id DESC`.
- M2 may include a minimal WeChat subscription-message foundation: `openid` capability review, preview bypass fallback, seal-flow subscription authorization timing, unlock-task reminder hook, minimal reminder/outbox/log persistence, successful-send idempotency, and non-blocking failure handling.
- Production-only modules remain out of scope for M2.

## Out Of Scope

Do not implement or expand the following in M2 unless a separate OpenSpec change explicitly says so:

- production-grade WeChat subscription messages beyond the minimal unlock-reminder foundation
- SMS reminders or a production notification center
- real MAP / IMAGE / VOICE capability
- admin portal
- production deployment
- monitoring, alerting, or incident response
- AI capability enhancement
- H5/Web user-side acceptance target
- major frontend visual reconstruction
- broad backend rewrite
- package or lockfile updates unless required and explicitly justified

## File Reading Rules

Do not perform a full repository scan by default.

Before backend implementation, read:

- `AGENTS.md`
- `.ai/ACTIVE_TASK.md`
- `.ai/HANDOFF_M1_VISUAL.md`
- `openspec/project.md`
- `openspec/changes/m2-backend-optimization/proposal.md`
- `openspec/changes/m2-backend-optimization/design.md`
- `openspec/changes/m2-backend-optimization/tasks.md`
- `openspec/changes/m2-backend-optimization/specs/backend-core/spec.md`

After that, read only backend files directly required by the current task. If extra files are needed, state the reason before reading them.

## Verification Required

For backend code changes:

- run focused backend tests where practical
- run `mvn -q test` from `backend` when feasible
- document manual verification when automated tests are not practical
- record backend work and evidence in `.ai/AGENT_LOG.md`

For documentation-only changes:

- verify the active task and M2 OpenSpec documents no longer conflict with the current phase
- include `git diff --stat`

## Output Required

- modified files
- what changed
- verification result
- skipped verification reason, if any
- `git diff --stat`
- scope safety check
- remaining risks
