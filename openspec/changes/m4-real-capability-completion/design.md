# M4 Real Capability Completion Design

## Purpose

M4 should make Flashback V2.0 feel honestly usable in its core Mini Program flow. The milestone is not a production launch program, but it is stricter than M3 demo hardening: core user-facing features should use real logic and real persistence in authenticated paths.

The target loop is:

```text
real login
  -> create or edit draft
  -> add content, location, images, voice, and optional cover
  -> optionally use real AI organization
  -> save or seal
  -> unlock when time arrives
  -> review original content, location, media, "你当时以为", and "后来其实"
  -> browse timeline/home cards with real cover and status data
```

## Product Principles

### 1. Real Core Capability Before Nice-To-Have Settings

M4 prioritizes core capability completion:

- AI actually calls a configured provider.
- Location actually persists and displays.
- Images and voice actually upload, persist, preview, and play.
- Timeline/home/time review surfaces use real backend data in authenticated mode.

Settings, admin features, observability, deployment hardening, and visual polish are deferred.

### 2. Preview Is Allowed, But It Must Be Honest

Preview mode may keep using local preview data for demonstration.

Authenticated real mode must not silently fall back to preview/mock data. If a real integration is unavailable, the user should see an explicit unavailable or retryable state rather than fake success.

### 3. Original Record Content And Sealed Records Are Protected

The user's original record content remains the core memory artifact.

AI output, location, attachments, cover, and playback metadata are supporting context. They must not replace original content or weaken lifecycle rules.

Once a record is sealed:

- original content is immutable
- location is immutable
- attachments are immutable
- cover is immutable

Unlocked records remain immutable for the sealed content and supporting context, except for M3-approved later reflection behavior.

### 4. Private Media By Default

Record media belongs to the user and should not be public by default.

Every configured storage provider MUST use a private bucket. The Mini Program should access media through short-lived signed URLs or equivalent private-access-safe URLs returned by the backend.

Backend logs and database records must not expose secrets, upload tokens, or unnecessary private content.

### 5. Small, Explicit Contracts

M4 should avoid broad backend rewrites.

Recommended model shape:

```text
record
  ├─ location 0..1
  ├─ cover_attachment_id 0..1
  └─ attachments 0..18
       ├─ IMAGE max 9
       └─ VOICE max 9
```

Implementation may use separate tables or equivalent persistence if the external contract and lifecycle rules remain clear.

## Backend Design Notes

### Contract Decisions

Backend implementation MUST use `openspec/changes/m4-real-capability-completion/backend-contract-decisions.md` as the M4 contract decision layer.

That file contains accepted defaults for endpoint paths, DTO shapes, AI provider configuration, object key policy, signed URL expiry, location rules, cover rules, and error semantics. Agents MUST implement those accepted defaults unless the user updates the decision file before implementation.

### AI Provider

M4 should replace mock-first AI behavior with a provider adapter.

Recommended configuration:

- `provider`
- `baseUrl`
- `apiKey`
- `model`
- request timeout
- optional provider-specific switches

Recommended provider strategy:

- primary path: DeepSeek or another configured domestic model
- compatibility path: OpenAI-compatible chat completion style endpoint for compatible domestic model services
- mock provider: allowed only for tests or explicit preview/development mode, not as authenticated real-mode success

AI failure handling:

- missing config returns explicit unavailable state
- provider HTTP failure returns explicit failed/unavailable state
- invalid provider response returns explicit failed/unavailable state
- record save/seal should continue when AI is not required
- original content is never replaced by AI output

### Configurable Object Storage

Recommended upload flow:

```text
Mini Program asks backend for upload token
  -> backend validates user, record ownership, draft status, media type, count, size policy
  -> backend selects the active provider and returns its short-lived upload authorization
  -> Mini Program uploads directly using the returned method, headers, and form fields
  -> Mini Program reports uploaded object metadata to backend
  -> backend verifies object exists through that provider and checks size/type/key ownership
  -> backend persists attachment metadata
  -> backend returns attachment and private access URL when needed
```

Secrets:

- All provider AK/SK credentials stay backend-side.
- Upload authorizations should be short-lived and scoped as narrowly as practical.
- Frontend must not contain provider secret keys.

Provider strategy:

- `QINIU` preserves the current private Kodo flow.
- `S3_COMPATIBLE` uses Signature V4 and configurable endpoint/region/path-style settings. It is the compatibility path for AWS S3, MinIO, and S3-compatible modes offered by providers such as Alibaba Cloud OSS and Tencent Cloud COS.
- The backend uses AWS SDK v2 S3 signing/client support rather than maintaining a custom Signature V4 implementation; this is the required and justified backend dependency change for the compatibility path.
- Aliases such as `aliyun-oss`, `tencent-cos`, `aws-s3`, and `minio` resolve to `S3_COMPATIBLE`; endpoint and region still come from backend configuration.
- For Alibaba Cloud OSS, follow its AWS SDK Java 2.x compatibility contract: use the `s3.oss-{region}.aliyuncs.com` endpoint, virtual-hosted access, and disable chunked encoding.
- New uploads use `app.storage.provider`. Existing attachment reads/deletes route by persisted `storage_provider`, so old provider credentials must remain configured while old objects are still needed.

Object key policy:

- keys should be generated or authorized by backend
- keys should include user/record scoping or an equivalent unguessable namespace
- client-supplied arbitrary keys should not be trusted without backend validation

Access policy:

- bucket is private
- backend returns signed access URLs or equivalent short-lived media URLs
- media URL expiry should be documented in code or config

### Attachment Data Model

Recommended `record_attachment` fields:

- `id`
- `record_id`
- `user_id`
- `type`: `IMAGE` or `VOICE`
- `storage_provider`: `QINIU`
- `bucket`
- `storage_key`
- `mime_type`
- `size_bytes`
- `duration_seconds` for voice where available
- `width` and `height` for images where available
- `sort_order`
- `status`: pending, available, failed, or equivalent
- `created_at`
- `updated_at`

Rules:

- max 9 `IMAGE` attachments per record
- max 9 `VOICE` attachments per record
- max 40 MB per file
- max 300 MB total attachments per record
- only draft records can add/delete/change attachments
- sealed/unlocked records reject attachment mutation
- attachment reads must be owner-scoped

### Cover

Recommended model:

- `record.cover_attachment_id` points to one image attachment owned by the same record.

Rules:

- cover is optional
- cover must reference an `IMAGE` attachment
- cover must belong to the same record
- cover cannot reference voice or another user's attachment
- cover cannot be changed after seal
- deleting the current cover image in draft should clear cover or require selecting another cover
- there is no standalone cover upload in M4

### Location

Recommended location fields:

- `source`: `CURRENT_LOCATION`, `MAP_PICKER`, or `MANUAL`
- `name`
- `address`
- `latitude`
- `longitude`

Rules:

- current location and map picker may include latitude/longitude
- manual input may omit coordinates
- location is optional
- draft records can create/update/delete location
- sealed/unlocked records reject location mutation
- time review shows location after unlock when present
- list/timeline cards may show a compact location label if it does not clutter the page

### Lifecycle Enforcement

Backend must enforce M4 mutation rules regardless of frontend behavior:

- DRAFT: content, AI supporting fields, location, attachments, and cover may be edited through supported APIs.
- SEALED: original content, location, attachments, and cover are immutable.
- UNLOCKED: original content, location, attachments, and cover remain immutable; M3 later reflection rules still apply.

### API Contract Guidance

Exact endpoint paths and DTOs are accepted in `backend-contract-decisions.md`.

Recommended API groups:

- AI organization/generation endpoints under existing AI or record APIs
- record location update/delete under record ownership
- attachment upload token creation
- attachment commit/verify after configured-provider upload
- attachment delete for draft only
- media signed URL retrieval
- cover update for draft only

The implementation should prefer existing controller/service patterns over introducing a new architecture.

## Frontend Design Notes

### Record Editor

Record editor should provide real controls for:

- adding/editing/removing location
- choosing current location
- choosing location from map
- manually entering location
- selecting up to 9 images
- compressing images by default before upload
- previewing images
- deleting draft images
- recording up to 9 voice files
- playing draft voice files
- re-recording/deleting draft voice files
- selecting cover from uploaded image attachments

The interface should use existing visual language and avoid a large redesign.

### Timeline And Home Cards

Timeline/home cards should use real record data in authenticated mode:

- cover image when available
- real status and unlock/review timing
- no hard-coded countdown or review card content in real mode
- safe fallback visual when no cover exists

Preview mode may still show curated demo cards, but only inside explicit preview mode.

### Time Review

Time review should show:

- original record content
- "你当时以为" when available
- "后来其实" per M3 rules
- location when present
- image preview
- voice playback
- cover if useful in the current visual composition

Location, attachments, and cover must appear read-only in time review.

### Upload UX

Minimum expected states:

- selecting
- compressing image
- uploading
- verifying
- available
- failed with retry or remove in draft

Voice and image flows do not need advanced background upload orchestration in M4, but they should not leave the user thinking a failed upload was saved.

## Verification Strategy

M4 implementation should verify:

1. real AI provider success path with configured test key where available
2. AI missing configuration path
3. AI provider failure path
4. no frontend/tracked-file secret leak for AI or object-storage keys
5. provider-neutral upload authorization creation is authenticated and owner-scoped
6. configured-provider object existence verification before attachment availability
7. image count, voice count, per-file size, and record total size limits
8. draft attachment delete/re-record behavior
9. sealed/unlocked attachment mutation rejection
10. cover same-record image-only validation
11. location source validation
12. draft location edit/delete behavior
13. sealed/unlocked location mutation rejection
14. real-mode timeline/home card data without preview fallback
15. preview mode still works when explicitly entered
16. time review displays unlocked record location/media correctly
17. backend tests or focused integration tests where practical
18. frontend type-check and Mini Program build where feasible
19. manual WeChat Developer Tools verification for media/location flows

All implementation notes, verification evidence, skipped verification reasons, and manual verification results must be written to `.ai/AGENT_LOG.md`.

## Risk Controls

### Scope Drift Into Production Platform Work

M4 may be near production usable for core functions, but it must not become a deployment, monitoring, admin, incident response, or settings-page project.

### Fake Integration Success

If AI, configured object storage, location, or media playback cannot complete, the system must expose a real unavailable/failed state. Silent mock success is out of scope and violates M4.

### Media Privacy

Private media access must not be weakened for convenience. Public bucket URLs or long-lived public access should be treated as a scope and privacy risk unless the user explicitly changes the storage policy.

### Payload And Performance

The current M4 limits are intentionally conservative:

- 9 images
- 9 voice files
- 40 MB per file
- 300 MB per record

If Mini Program performance or storage behavior shows these limits are too high, implementation should record evidence and propose a spec update before changing accepted limits.
