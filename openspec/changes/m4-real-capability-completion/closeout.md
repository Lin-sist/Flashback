# M4 Closeout And Archive Readiness

Date: 2026-06-30

## Status

M4 real capability completion is implementation-complete for the accepted OpenSpec scope. The previously pending real AI, real object-storage/media, and WeChat Developer Tools manual checks were confirmed by the user on 2026-06-30.

The remaining gaps are archive-process and database-query-plan evidence gaps, not known M4 scope or contract gaps.

## Completed M4 Content

- M4 fact source, guardrails, accepted contract gate, and backend contract decisions are in place.
- Real AI provider configuration and OpenAI-compatible adapter are implemented, with explicit unavailable/failed behavior and backend-only secret handling.
- Provider-neutral private object storage is implemented for Qiniu and S3-compatible providers, including upload authorization, object verification, delete, signed access URL, provider routing, and S3-compatible configuration support.
- Record attachments are implemented for images and raw voice files, including owner scope, DRAFT-only mutation, SEALED/UNLOCKED immutability, count limits, per-file limit, and total record limit.
- Record cover is implemented as optional same-record IMAGE attachment selection and is immutable after seal.
- Record location is implemented with CURRENT_LOCATION, MAP_PICKER, and MANUAL sources, DRAFT-only mutation, owner scope, and time-review/detail exposure.
- Mini Program real-mode AI/location/media/cover flows are connected to backend contracts, with explicit unavailable/error states rather than mock success.
- Authenticated real mode and explicit preview mode boundaries have been audited and preserved.
- Home, timeline, and time-review surfaces were moved toward backend-backed real data for M4 fields.
- Timeline filtering and pagination are implemented with one tag plus createdAt year/month/day filters, AND semantics, Asia/Shanghai business-time conversion, stable `created_at DESC, id DESC` ordering, `TimelinePageVO`, page metadata, and frontend/preview parity.
- Backend full suites, focused tests, frontend type-check, Mini Program build, generated artifact audits, and preview timeline harness checks have been recorded in `.ai/AGENT_LOG.md`.
- User-confirmed manual verification on 2026-06-30 covers real AI success, configured object-storage upload/object verification/signed URL/image preview/voice playback, Mini Program location, media, timeline filtering, cover display, and unlocked time-review location/media/M3 reflection behavior.
- README and reusable system-design documents were aligned to current M4 state without changing OpenSpec scope.

## Remaining M4 Archive Blockers

- Real MySQL `EXPLAIN` for the final timeline range/page query remains pending because the local MySQL service was unavailable in the recorded session.
- M4 delta specs have not been synced into accepted `openspec/specs/**`; archiving should sync `backend-core`, `miniapp-core`, and `v2-product-scope` requirements first.
- The official `openspec` CLI is unavailable in the current PowerShell environment, so archive/status checks must either be run elsewhere or performed manually with explicit evidence.

## M1 To M4 Closeout State

### M1 Frontend Visual Foundation

State: active change, not archive-ready.

Unclosed items:

- Visual comparison evidence for Home, Timeline, User Center, and secondary canonical pages is still missing.
- Long-content bottom-navigation overlap checks are still open.
- Token/shared-component extraction tasks remain open.
- Visual-only controls still need confirmation that they remain local/demo-only and do not imply real backend capability.
- The final M1 completion gate is still unchecked.

### M2 Backend Optimization

State: already archived at `openspec/changes/archive/2026-06-07-m2-backend-optimization`.

Unclosed items:

- No active M2 change remains.
- Historical residuals were carried forward or deferred: local database schema application, real WeChat subscription/login follow-up, and manual OpenSpec archive fallback because the CLI was unavailable.

### M3 Demo Core Flow Hardening

State: active change, implementation mostly complete, not fully archive-ready.

Unclosed items:

- The full combined manual demo loop remains unchecked: create -> AI organize -> seal -> unlock -> reminder attempt -> time review -> first and second `realityLater` submit -> no more modify action -> stage summary.
- M3 delta specs are not fully synced into accepted `openspec/specs/**`; archive should sync backend, miniapp, and product-scope requirements first.
- If the team treats real WeChat credentials/template IDs as required for archive, record the final manual evidence or explicitly carry it as an external dependency.

### M4 Real Capability Completion

State: implementation-complete, manual real-capability verification confirmed, archive readiness is partial.

Unclosed items:

- Real MySQL `EXPLAIN` for timeline range/page query remains pending.
- Delta spec sync into accepted specs.
- Official or manual archive workflow execution.

## Recommended Archive Decision

Recommended decision: do not archive M1 or M3 yet. For M4, archive after syncing delta specs into accepted specs and deciding whether real MySQL `EXPLAIN` can remain as carry-over query-plan evidence.

If strict archive means every task checkbox complete, M4 should wait for MySQL `EXPLAIN`. If archive means implementation scope and manual core-capability verification are complete, M4 can be archived after syncing delta specs and recording the MySQL query-plan evidence as a carry-over risk.
