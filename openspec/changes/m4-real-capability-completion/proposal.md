# M4 Real Capability Completion

## Background

Flashback V2.0 has completed the M3 demo core-flow hardening work to a usable baseline: real authentication paths, record lifecycle, reminder foundation, reflection fields, and stage summary are now available or documented as M3 behavior.

M4 is the next engineering optimization milestone. Its purpose is to move the Mini Program from "demo flow can run" toward "near production usable" for the user-facing core capabilities.

The user has confirmed these M4 decisions:

- M4 should be close to production usable, not only a local demo.
- Preview mode may remain, but preview/mock behavior must be isolated from real user paths.
- AI should use real provider calls. Current provider direction is domestic large models, with DeepSeek as a primary candidate and OpenAI-compatible endpoint support for compatible domestic model platforms. API keys must stay backend-side.
- Record location must support current location, map picker, and manual input.
- Record images and voice must be real features, backed by configurable object storage.
- Storage buckets are private; Qiniu and S3-compatible providers are supported.
- The backend must verify object existence after upload.
- Each record supports at most 9 images and 9 voice files.
- Each file is limited to 40 MB, and each record is limited to 300 MB total attachments.
- Images are compressed by default before upload.
- Voice stores the original audio file only; speech-to-text is not part of M4.
- Draft voice files may be previewed, re-recorded, or deleted.
- Images may be previewed and voice files may be played.
- A record cover may be selected from existing image attachments only.
- Sealed and unlocked records are immutable: attachments, location, and cover cannot be deleted or changed after sealing.
- Location is shown in time review after unlock.
- Timeline filtering uses one tag plus created-time year/month/day filters, combines conditions with AND semantics, and paginates records without changing the quiet timeline presentation.
- Settings page work is out of M4 scope.

## Problem

Several visible M3 surfaces still behave like placeholders or partial demo surfaces:

1. AI behavior is still mock/fallback-first rather than a real provider integration.
2. Record editor auxiliary modules for location, image, and voice are visual entry points rather than real workflows.
3. Timeline and home review cards can still rely on static or preview-like data in real paths.
4. Record data contracts do not yet persist location, media attachments, signed media access, or cover selection.
5. Preview data is useful for demos, but its boundary must be explicit so real users do not receive mock success.
6. Timeline filtering is only partially surfaced: the backend already accepts year and single-tag conditions, but the Mini Program exposes only a free-form year input, does not support month/day selection, and loads the complete matching timeline without pagination.

If these gaps remain, the Mini Program can look complete while still failing the user's expectation that all core functions are actually usable.

## Goals

M4 SHALL make the user-facing Mini Program core capabilities real and verifiable:

1. Replace real-path mock AI behavior with configurable real AI provider calls.
2. Keep AI failure explicit and non-destructive; do not fake successful AI output.
3. Implement record location with current location, map picker, and manual input.
4. Implement image and voice attachments through configurable private object storage.
5. Implement backend-issued upload tokens and backend verification of uploaded objects.
6. Enforce attachment count, per-file size, and per-record total size limits.
7. Support image preview, voice playback, and draft-only attachment deletion/re-recording.
8. Support cover selection from the record's own image attachments.
9. Show record covers on timeline/home record cards where applicable.
10. Show location in time review after unlock.
11. Freeze location, attachments, and cover after record sealing.
12. Separate preview/mock data from authenticated real user paths.
13. Make home review cards and time review surfaces backend-backed in real mode.
14. Keep V2.0 naming and the quiet, private, time-oriented product intention.
15. Complete timeline filtering with one tag and created-time year/month/day granularity.
16. Paginate timeline records with stable ordering and grouped responses suitable for incremental Mini Program loading.

## Non-Goals

M4 MUST NOT implement:

- production deployment or release operations
- monitoring, alerting, incident response, or observability platform work
- admin portal or admin media management
- settings page enhancements
- SMS reminders
- production notification center
- campaign delivery
- social feed, sharing, or public record discovery
- speech-to-text, voice transcription, or voice AI analysis
- complex AI scoring, diagnosis, psychological assessment, or dashboards
- provider-specific storage features beyond the attachment lifecycle required by M4
- album management outside record attachments
- standalone cover upload not tied to a record image attachment
- H5/Web user-side acceptance target
- major frontend visual reconstruction unrelated to completing real functions
- broad backend rewrite
- package or lockfile changes unless required and explicitly justified

## Scope

### P0: M4 Fact Source and Guardrails

This OpenSpec change is the active fact source for M4 implementation.

Agents working on M4 MUST read:

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
- `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md`
- relevant M4 spec deltas
- only directly required frontend/backend files

Backend API contracts, storage key policies, DTO fields, enum names, and frontend-visible error/status semantics accepted in `backend-contract-decisions.md` MUST be implemented as written unless the user updates that file.

### P1: Real AI Provider Integration

M4 includes real AI provider calls for the existing AI-supported capabilities:

- "你当时以为" organization
- gentle prompts or summaries already present in the product
- stage summary where M3 already supports manual generation

M4 AI integration MUST:

- keep API keys in backend configuration only
- support DeepSeek or a compatible domestic model endpoint
- support OpenAI-compatible request/response shape where practical
- expose explicit unavailable/error states when configuration or provider calls fail
- preserve user original content
- avoid turning AI into a required lifecycle dependency

### P1: Record Location

M4 includes a real record location module with three input modes:

- current location
- map picker
- manual input

Location MUST be user-owned and record-owned. Draft records may edit or remove location. Sealed and unlocked records MUST NOT allow location mutation. Time review MUST show location after unlock when location exists.

### P1: Record Attachments and Cover

M4 includes real image and voice attachments:

- configurable private object-storage provider
- backend-issued provider-neutral upload authorization
- frontend direct upload to the configured provider where suitable
- backend object existence verification after upload
- signed or short-lived access URL for preview/playback
- max 9 images per record
- max 9 voice files per record
- max 40 MB per file
- max 300 MB total attachments per record
- default image compression before upload
- raw voice file storage only
- cover selected from the record's own image attachments only

Draft records may add, delete, re-order where supported, re-record voice, and change cover. Sealed and unlocked records MUST NOT allow attachment deletion, replacement, or cover changes.

### P1: Mock Boundary and Real Data Surfaces

M4 includes a real-path mock cleanup:

- preview data may remain for explicit preview mode
- authenticated real users MUST use backend-backed data
- home review cards MUST not use hard-coded countdown/review data in real mode
- time review MUST use backend-backed record detail, attachments, cover, location, and reflection data
- AI fallback MUST not pretend to be a successful real provider response

### P1: Timeline Filtering And Pagination

M4 includes a focused timeline browsing contract for users whose records have grown beyond a single short list:

- filter by one enabled/shared tag at a time
- filter by record creation year, month, or exact date
- combine tag and date filters with AND semantics
- interpret date filters against `createdAt` in the `Asia/Shanghai` business timezone
- paginate matching records with stable `created_at DESC, id DESC` ordering
- preserve backend-provided year-month groups while incrementally loading and merging pages
- return safe empty results for valid filters with no matches
- keep preview-mode filtering behavior aligned with authenticated real mode

Multiple-tag boolean logic, keyword search, record-type/status filters, and persisted filter preferences remain outside this M4 addition.

### P2: Mini Program UX Completion

M4 frontend work focuses on functional completion rather than visual redesign:

- record editor location selector
- image picker, compression, upload, preview, delete in draft
- voice recorder, playback, re-record, upload, delete in draft
- cover selection from image attachments
- timeline/home cover display
- time review location, image preview, and voice playback
- loading, error, retry, and upload-progress states where they affect usability
- a restrained timeline filter sheet, applied-filter summary, filtered empty state, and load-more state

## Acceptance Criteria

M4 is accepted when:

1. M4 documents clearly constrain implementation scope and active task source of truth.
2. Real-path AI calls a configured backend provider instead of mock output.
3. AI missing configuration or provider failure is explicit and does not fake success.
4. API keys and provider secrets are not present in frontend code or tracked files.
5. Location supports current location, map picker, and manual input.
6. Location can be changed in draft and is immutable after seal.
7. Location is shown in time review after unlock when present.
8. Images can be selected, compressed by default, uploaded, previewed, deleted in draft, and accessed through private-storage-safe URLs.
9. Voice files can be recorded, uploaded, played, re-recorded, and deleted in draft.
10. Attachments obey max 9 images, max 9 voice files, 40 MB per file, and 300 MB per record.
11. Backend verifies configured-provider object existence before persisting attachment metadata as available.
12. Cover can be selected only from the same record's image attachments.
13. Timeline/home record cards show cover when available.
14. Sealed and unlocked records reject location, attachment, and cover mutation.
15. Real authenticated paths do not depend on preview/mock data.
16. Preview mode remains available and explicitly isolated.
17. Home review cards and time review data are backend-backed in real mode.
18. WeChat Mini Program build passes where feasible.
19. Backend tests or focused verification cover AI configuration, attachment limits, provider routing/object verification, ownership, immutability, and location behavior.
20. Manual Mini Program verification evidence is recorded in `.ai/AGENT_LOG.md`.
21. Timeline supports one-tag and created-time year/month/day filters with AND semantics.
22. Timeline results use `TimelinePageVO`, default to 20 records per page, cap page size at 50, and retain stable `created_at DESC, id DESC` ordering.
23. Applying or resetting a timeline filter restarts pagination; loading later pages merges repeated year-month groups without duplicate records.
24. Valid filters with no matches return a safe empty state, while invalid date combinations return explicit validation errors.
25. Explicit preview mode supports the same timeline filter and pagination semantics without leaking preview data into authenticated real mode.

## Recommended Implementation Order

1. Confirm M4 active task and read only required OpenSpec documents.
2. Establish current code facts for AI, records, home cards, timeline, preview, and record editor auxiliary modules.
3. Follow the accepted contracts in `backend-contract-decisions.md`; ask the user only before changing them.
4. Implement real AI provider adapter and configuration boundary.
5. Implement storage schema and provider-neutral upload-authorization/object-verification backend.
6. Implement attachment metadata APIs and immutable lifecycle rules.
7. Implement location schema/APIs and immutable lifecycle rules.
8. Implement cover selection and timeline/home display contracts.
9. Implement the accepted timeline filter and pagination contract.
10. Connect Mini Program location, image, voice, cover, and timeline filter flows.
11. Clean real-path mock usage while preserving explicit preview mode.
12. Run backend tests, frontend type-check/build, Mini Program build, and manual end-to-end verification.
