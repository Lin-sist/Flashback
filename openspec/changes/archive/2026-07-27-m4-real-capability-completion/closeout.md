# M4 Closeout And Archive Readiness

Date: 2026-07-27  
Previous readiness note: 2026-06-30

## Status

**ARCHIVED** (manual OpenSpec closeout on 2026-07-27).

M4 real capability completion is **implementation-complete** for the accepted OpenSpec scope. User-confirmed manual verification for real AI, object storage/media, and WeChat Developer Tools paths was recorded on 2026-06-30.

Archive disposition:

- M4 delta requirements accepted into baseline `openspec/specs/backend-core|miniapp-core|v2-product-scope/spec.md`
- Change directory moved to `openspec/changes/archive/2026-07-27-m4-real-capability-completion/`
- `.ai/ACTIVE_TASK.md` set to `IDLE`
- Official `openspec` CLI was not required; closeout performed manually with explicit evidence

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

## Truth Alignment (2026-07-27)

| Claim | Evidence | Disposition |
|---|---|---|
| Implementation scope complete | `tasks.md` 184+ checkboxes complete; product paths in closeout 2026-06-30 | **Accepted** |
| Manual real capability verification | User confirmation 2026-06-30 in prior closeout / AGENT_LOG | **Accepted** |
| Delta specs in baseline | Manual merge of ADDED requirements into three baseline specs on 2026-07-27 | **Done at archive** |
| Live MySQL `EXPLAIN` for timeline page query | MySQL80 service Stopped; start denied without elevation on 2026-07-27 | **Carry-over residual** (not product-scope gap) |
| Official openspec CLI | Not available in prior environments; manual archive used | **Accepted alternative** |

### Carry-over residual (not reopening M4)

1. **Real MySQL EXPLAIN** for `selectTimelinePageByUserAndCondition` / count sibling when MySQL is available:
   - SQL shape: owner-scoped `record r` + optional `created_at` range + optional tag `EXISTS`, `ORDER BY created_at DESC, id DESC`, `LIMIT/OFFSET`
   - Expected index support: `idx_record_user_created_id (user_id, created_at, id)` (see `backend/sql/mysql/m4-timeline-filter-pagination.sql` and test schema)
   - Action when convenient: start MySQL, apply index migration if needed, run `EXPLAIN` on representative filtered/paginated query, append evidence to AGENT_LOG as Type B residual — **do not reopen M4 change**

## M1 / M3 Note (unchanged by M4 archive)

- **M1** remains a historical active change directory (not archive-ready per 2026-06-30 note). Not in M4 scope.
- **M3** remains a historical active change directory (not fully archive-ready). Not reopened by this archive.
- **M2** already archived.

Post-M4 product work (Agent mainline) MUST use a **new** Type C change after blueprint freeze; must not attach to archived M4.

## Archive Checklist (executed)

- [x] Capability + manual verification accepted as complete
- [x] EXPLAIN disposition decided (carry-over residual)
- [x] Delta ADDED requirements synced to baseline specs
- [x] `tasks.md` has no open product-scope incomplete items
- [x] Change moved under `openspec/changes/archive/2026-07-27-m4-real-capability-completion/`
- [x] `ACTIVE_TASK` → IDLE
- [x] AGENT_LOG archive evidence appended
