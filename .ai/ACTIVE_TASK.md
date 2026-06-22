# Active Task

## Task

Current task: prepare and implement `m4-real-capability-completion`.

M3 demo core flow hardening is now historical baseline for M4. Do not treat M3 documents as the active implementation source unless M4 explicitly references them for inherited behavior.

The current goal is to use `openspec/changes/m4-real-capability-completion/` as the active fact source before making M4 code changes.

## Source Of Truth

- `AGENTS.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`
- `openspec/specs/miniapp-core/spec.md`
- `openspec/specs/v2-product-scope/spec.md`
- `openspec/specs/agent-collaboration/spec.md`
- `openspec/changes/m4-real-capability-completion/proposal.md`
- `openspec/changes/m4-real-capability-completion/design.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md`
- `openspec/changes/m4-real-capability-completion/specs/backend-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/miniapp-core/spec.md`
- `openspec/changes/m4-real-capability-completion/specs/v2-product-scope/spec.md`

Old `Docs/**` files may be used only as non-conflicting historical context. They are not the M4 implementation fact source.

Archived M2 documents and inactive M3 documents may be used only as historical rationale when they do not conflict with accepted specs or M4 documents.

## Current Phase

M4 real capability completion may proceed only after establishing current code facts and following the accepted API/storage/provider contracts in `backend-contract-decisions.md`.

Agents must first:

- classify relevant capabilities as `confirmed`, `partial`, `planned`, `out of scope`, or `unknown`
- compare actual frontend/backend behavior against accepted specs and M4 OpenSpec documents
- document gaps before implementation
- follow `backend-contract-decisions.md` for accepted API contracts, DTO fields, enum names, storage key policies, provider behavior, and frontend-visible error/status semantics
- ask the user only before changing accepted M4 contracts or when code facts make an accepted contract impossible or unsafe
- implement the smallest changes required by the current M4 task

## Primary M4 Focus

- replace real-path mock AI behavior with real backend AI provider integration
- keep AI API keys and provider secrets backend-side only
- support domestic model direction, with DeepSeek or compatible OpenAI-style domestic endpoints as the current provider strategy
- preserve AI failure as explicit unavailable/failed behavior instead of fake success
- implement real record location with current location, map picker, and manual input
- implement real image and voice attachments backed by configurable private object storage
- issue provider-neutral upload authorization from backend and verify uploaded objects before persisting available attachments
- support max 9 images, max 9 voice files, max 40 MB per file, and max 300 MB total attachments per record
- compress images by default before upload
- store voice as raw audio only; do not add transcription in M4
- support image preview and voice playback
- support draft-only voice preview, re-record, and delete behavior
- support cover selection only from the same record's image attachments
- show covers on timeline/home cards where applicable
- complete timeline filtering with one tag plus created-time year/month/day filters
- paginate timeline records with stable `created_at DESC, id DESC` ordering and grouped page responses
- show location in time review after unlock
- freeze location, attachments, and cover after record seal
- keep preview mode available but isolated from authenticated real user paths
- make home review cards and time review surfaces backend-backed in real mode

## Explicit M4 Decisions

- M4 is near-production usability for core Mini Program functions, not a local-only demo.
- M4 is still not production deployment/release hardening.
- Preview may remain, but it must not leak into authenticated real user behavior.
- M4 uses a provider-neutral object-storage contract. Qiniu and S3-compatible providers are supported through backend configuration.
- Object-storage buckets are private.
- Backend must verify object existence after upload.
- Images are compressed by default.
- Cover must come from image attachments already attached to the same record.
- Sealed and unlocked records cannot delete or modify location, attachments, or cover.
- Settings page work is deferred outside M4.
- Voice transcription and voice AI analysis are outside M4.
- Timeline filtering uses `createdAt` in the `Asia/Shanghai` business timezone, supports one tag at a time, and combines tag/date conditions with AND semantics.
- Timeline pagination uses the existing `/api/records/timeline` endpoint and the accepted `TimelinePageVO` response contract.

## Out Of Scope

Do not implement or expand the following in M4 unless a separate OpenSpec change explicitly says so:

- admin portal
- production deployment or release operations
- monitoring, alerting, observability platform work, or incident response
- settings page enhancements
- SMS reminders
- production notification center
- admin template management
- campaign delivery
- social feed, sharing, or public record discovery
- speech-to-text, voice transcription, transcript search, or voice AI analysis
- complex AI growth analysis, scoring, diagnosis, psychological assessment, or dashboards
- provider-specific features beyond upload, verification, private access URL, and delete required by record attachments
- album management outside record attachments
- standalone cover upload not tied to a record image attachment
- H5/Web user-side acceptance target
- major frontend visual reconstruction
- broad backend rewrite
- package or lockfile updates unless required and explicitly justified

## File Reading Rules

Do not perform a full repository scan by default.

Before M4 implementation, read:

- `AGENTS.md`
- `.ai/ACTIVE_TASK.md`
- `openspec/project.md`
- `openspec/specs/backend-core/spec.md`
- `openspec/specs/miniapp-core/spec.md`
- `openspec/specs/v2-product-scope/spec.md`
- `openspec/specs/agent-collaboration/spec.md`
- `openspec/changes/m4-real-capability-completion/proposal.md`
- `openspec/changes/m4-real-capability-completion/design.md`
- `openspec/changes/m4-real-capability-completion/tasks.md`
- relevant M4 spec deltas
- `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md` before backend contract work

After that, read only frontend/backend files directly required by the current M4 task. If extra files are needed, state the reason before reading them.

## Contract Change Rule

API and integration contracts already explicit in M4 OpenSpec and `backend-contract-decisions.md` are accepted and should be implemented as written.

The user must confirm only contract changes or newly discovered gaps. This includes changes to:

- endpoint paths
- request/response DTO fields
- enum names
- persistence model choices where more than one reasonable option exists
- object-storage key policy
- signed URL expiry behavior
- provider names and AI configuration fields
- frontend-visible error/status semantics
- whether a capability is persisted or computed on demand

## Verification Required

For backend code changes:

- run focused backend tests where practical
- run `mvn -q test` from `backend` when feasible
- document manual verification when automated tests are not practical
- record backend work and evidence in `.ai/AGENT_LOG.md`

For frontend code changes:

- run focused frontend checks where practical
- run type-check and Mini Program build when feasible
- manually verify WeChat Mini Program behavior when location/media APIs cannot be automated
- record frontend work and evidence in `.ai/AGENT_LOG.md`

For integration-specific behavior:

- verify real AI success when credentials are available
- verify explicit AI unavailable/failure behavior
- verify configured-provider upload authorization, object existence verification, signed URL/media access, image preview, and voice playback
- verify attachment and location immutability after seal
- verify preview mode remains isolated
- verify timeline single-tag and year/month/day filters, pagination, stable ordering, safe empty results, and preview parity
- record all manual verification evidence in `.ai/AGENT_LOG.md`

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
