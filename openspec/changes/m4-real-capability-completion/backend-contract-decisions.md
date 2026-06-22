# M4 Backend Contract Decisions

## Purpose

This document turns M4's backend-facing open questions into accepted implementation contracts.

The user accepted the initial eight contract assumptions on 2026-06-17 and the timeline filtering/pagination contract on 2026-06-22. M4 backend implementation agents MUST follow this document unless the user explicitly updates it.

Status meanings:

- `Accepted`: confirmed by user or inherited from accepted specs.
- `Deferred`: intentionally outside M4.
- `Open`: must be discussed before implementation. There should be no `Open` decisions before backend implementation starts.

M4 implementation agents MUST read this file before backend contract work.

## External Reference Baseline

Agents implementing provider/storage code SHOULD re-check official docs before coding because provider APIs may change.

Current reference links used for this decision file:

- DeepSeek API Docs: `https://api-docs.deepseek.com/`
- Qiniu upload token docs: `https://developer.qiniu.com/kodo/1208/upload-token`
- Qiniu private download docs: `https://developer.qiniu.com/kodo/1656/download-private`
- Qiniu object stat docs: `https://developer.qiniu.com/kodo/1308/stat`

## Decision Summary

| Area | Status | Decision |
| --- | --- | --- |
| API style | Accepted | Keep existing REST style and place record-owned subresources under `/api/records/{recordId}`. |
| AI provider adapter | Accepted | Implement one OpenAI-compatible adapter first, with DeepSeek as the default configured provider. |
| Dedicated NVIDIA NIM adapter | Deferred | Do not build a NIM-specific adapter in M4. Use `OPENAI_COMPATIBLE` if a compatible domestic endpoint is provided later. |
| Configurable object storage | Accepted | Use private buckets through `QINIU` or `S3_COMPATIBLE`, backend-issued upload authorization, provider object verification, and backend-signed download URL. |
| Upload persistence | Accepted | Do not persist attachment metadata until configured-provider object verification succeeds. Authorization issuance may be stateless except logs. |
| Attachment mutation | Accepted | DRAFT only. SEALED and UNLOCKED records reject add/delete/replace/re-record. |
| Draft delete remote object | Accepted | Route by persisted provider and delete the remote object first; if it succeeds or is already missing, remove metadata. If provider delete fails, return failure and keep metadata. |
| Cover source | Accepted | Cover must be selected from the same record's IMAGE attachment. No standalone cover upload. |
| Location persistence | Accepted | Use separate `record_location` persistence or equivalent separate model. Do not put all location fields directly into `record` unless implementation proves it is materially simpler. |
| Location geocoding | Deferred | Backend geocoding/reverse geocoding is outside M4. Store coordinates/name/address supplied by Mini Program. |
| Signed media URL expiry | Accepted | Default 600 seconds; make it backend configurable. |
| Error model | Accepted | Reuse existing `ApiResponse` + HTTP status + current `ErrorCode` style. Add new numeric error codes only if the existing coarse codes cannot express required frontend behavior. |
| Timeline filtering and pagination | Accepted | Keep `GET /api/records/timeline`; support one tag plus `createdAt` year/month/day filters with AND semantics and return paginated `TimelinePageVO` groups. |

## AI Provider Contract

### Configuration

Accepted backend properties:

```yaml
app:
  ai:
    provider: ${AI_PROVIDER:mock}
    base-url: ${AI_BASE_URL:https://api.deepseek.com}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:deepseek-v4-pro}
    timeout-millis: ${AI_TIMEOUT_MILLIS:10000}
    real-mode-mock-enabled: ${AI_REAL_MODE_MOCK_ENABLED:false}
```

Accepted provider enum/string values:

- `mock`: local/test/explicit preview development only.
- `deepseek`: first real provider target.
- `openai-compatible`: generic domestic model endpoint using OpenAI-compatible chat completion shape.

Implementation notes:

- DeepSeek official docs currently describe OpenAI-compatible access with `base_url` `https://api.deepseek.com`, Bearer API key, and `/chat/completions` style calls. Agents MUST re-check official provider docs before implementing if details matter.
- `deepseek-v4-pro` is the accepted model default as of this document update. `deepseek-chat` and `deepseek-reasoner` should not be chosen as new defaults because DeepSeek docs mark them as deprecated after 2026-07-24.
- API keys MUST stay backend-side. Do not expose them through Mini Program env, build config, logs, response bodies, or tracked files.

### Existing AI Endpoints

Keep existing endpoints:

- `POST /api/ai/writing-prompts`
- `POST /api/ai/summarize-record`
- `POST /api/stage-summaries/generate`

Do not introduce new AI endpoints in the first backend phase unless current contracts cannot support required behavior.

### AI Failure Semantics

Accepted behavior:

- Missing provider config: return explicit unavailable/failure state.
- Provider HTTP timeout/error: return explicit unavailable/failure state.
- Invalid provider response: return explicit unavailable/failure state.
- Record save/seal remains non-blocking when AI is optional.
- Authenticated real mode MUST NOT return mock content as real provider output.

Accepted response extension, if needed:

```json
{
  "source": "deepseek",
  "status": "SUCCESS",
  "message": null
}
```

Allowed `status` values:

- `SUCCESS`
- `UNAVAILABLE`
- `FAILED`
- `FALLBACK`

`FALLBACK` is allowed only when the UI can clearly identify it as fallback, not real AI success.

## Configurable Object Storage Contract

### Configuration

Accepted backend properties:

```yaml
app:
  storage:
    provider: ${STORAGE_PROVIDER:qiniu}
    qiniu:
      access-key: ${QINIU_ACCESS_KEY:}
      secret-key: ${QINIU_SECRET_KEY:}
      bucket: ${QINIU_BUCKET:}
      region: ${QINIU_REGION:}
      private-domain: ${QINIU_PRIVATE_DOMAIN:}
      upload-token-ttl-seconds: ${QINIU_UPLOAD_TOKEN_TTL_SECONDS:600}
      download-url-ttl-seconds: ${QINIU_DOWNLOAD_URL_TTL_SECONDS:600}
      key-prefix: ${QINIU_KEY_PREFIX:flashback}
    s3:
      endpoint: ${S3_ENDPOINT:}
      region: ${S3_REGION:}
      access-key: ${S3_ACCESS_KEY:}
      secret-key: ${S3_SECRET_KEY:}
      session-token: ${S3_SESSION_TOKEN:}
      bucket: ${S3_BUCKET:}
      path-style-access: ${S3_PATH_STYLE_ACCESS:false}
      upload-token-ttl-seconds: ${S3_UPLOAD_TOKEN_TTL_SECONDS:600}
      download-url-ttl-seconds: ${S3_DOWNLOAD_URL_TTL_SECONDS:600}
      key-prefix: ${S3_KEY_PREFIX:flashback}
```

Accepted media limits:

```yaml
app:
  media:
    max-image-count-per-record: 9
    max-voice-count-per-record: 9
    max-file-size-bytes: 41943040
    max-total-size-bytes-per-record: 314572800
```

### Object Key Policy

Backend MUST generate or approve keys. Accepted generated key format:

```text
{keyPrefix}/users/{userId}/records/{recordId}/{type}/{uuid}.{extension}
```

Rules:

- `type` is `image` or `voice`.
- Extension is derived from an allowlisted MIME type, not blindly from original filename.
- Client-supplied arbitrary keys are rejected.
- Upload token scope should target a single bucket/key where practical.

Provider values:

- `qiniu` -> `QINIU`
- `s3-compatible`, `aws-s3`, `aliyun-oss`, `tencent-cos`, or `minio` -> `S3_COMPATIBLE`
- Switching the active value affects new uploads. Existing attachments continue to use their persisted provider and require that provider's credentials to remain configured.
- Alibaba Cloud OSS through AWS SDK Java 2.x uses an endpoint such as `https://s3.oss-{region}.aliyuncs.com`, virtual-hosted access (`path-style-access: false`), and disabled chunked encoding.

### Upload Authorization Endpoint

Accepted endpoint:

```http
POST /api/records/{recordId}/attachments/upload-token
```

Request:

```json
{
  "type": "IMAGE",
  "fileName": "example.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 123456
}
```

Response data:

```json
{
  "provider": "QINIU",
  "bucket": "flashback-private",
  "key": "flashback/users/1/records/10/image/uuid.jpg",
  "uploadMethod": "POST_MULTIPART",
  "uploadUrl": "https://upload.qiniup.com",
  "fileFieldName": "file",
  "uploadHeaders": {},
  "uploadFormData": {
    "token": "short-lived-token",
    "key": "flashback/users/1/records/10/image/uuid.jpg"
  },
  "expiresAt": "2026-06-17T12:00:00",
  "maxFileSizeBytes": 41943040
}
```

Validation:

- Authenticated user owns the record.
- Record is `DRAFT`.
- Type is `IMAGE` or `VOICE`.
- Count and total-size limits are checked against committed attachments plus requested size.
- MIME type is allowlisted.

### Attachment Commit Endpoint

Accepted endpoint:

```http
POST /api/records/{recordId}/attachments/commit
```

Request:

```json
{
  "provider": "QINIU",
  "type": "IMAGE",
  "key": "flashback/users/1/records/10/image/uuid.jpg",
  "fileName": "example.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 123456,
  "width": 1200,
  "height": 800,
  "durationSeconds": null
}
```

Response data: `RecordAttachmentVO`.

Backend behavior:

- Verify the object exists using the selected provider metadata/stat API.
- Verify provider-reported size and MIME type when available.
- Verify key belongs to the authenticated user's record namespace.
- Persist attachment metadata only after verification succeeds.

### Attachment Delete Endpoint

Accepted endpoint:

```http
DELETE /api/records/{recordId}/attachments/{attachmentId}
```

Behavior:

- Allowed only for owner and `DRAFT` record.
- Reject after seal/unlock.
- If attachment is the current cover, clear `coverAttachmentId` after delete succeeds.
- Route using attachment `storageProvider` and attempt remote delete before removing metadata. If the provider reports object missing, treat as safe to remove metadata. If delete fails, return failure and keep metadata.

### Media Access Endpoint

Accepted endpoint:

```http
GET /api/records/{recordId}/attachments/{attachmentId}/access-url
```

Response data:

```json
{
  "attachmentId": 1,
  "url": "https://private-domain/key?e=...&token=...",
  "expiresAt": "2026-06-17T12:00:00"
}
```

Behavior:

- Owner-scoped.
- Returns signed URL for private bucket access.
- Default expiry is 600 seconds unless config overrides.
- URL must not be stored as permanent record state.

### RecordAttachmentVO

Accepted response shape:

```json
{
  "id": 1,
  "recordId": 10,
  "type": "IMAGE",
  "status": "AVAILABLE",
  "fileName": "example.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 123456,
  "width": 1200,
  "height": 800,
  "durationSeconds": null,
  "sortOrder": 0,
  "createdAt": "2026-06-17T12:00:00",
  "accessUrl": null
}
```

Accepted persisted status values:

- `AVAILABLE`
- `DELETED`

Upload UI states such as selecting, compressing, uploading, verifying, and failed are frontend-local unless implementation introduces an explicit upload-session model.

## Cover Contract

Accepted endpoint:

```http
PUT /api/records/{recordId}/cover
```

Request:

```json
{
  "attachmentId": 1
}
```

Clear cover request:

```json
{
  "attachmentId": null
}
```

Response data: `RecordDetailVO`.

Validation:

- Owner-scoped.
- Record is `DRAFT`.
- Attachment exists, belongs to the same record, has type `IMAGE`, and has status `AVAILABLE`.
- SEALED and UNLOCKED records reject cover mutation.

Accepted list/detail fields:

- `cover`: compact attachment metadata or null.
- `coverUrl`: optional short-lived URL only when the endpoint already returns display-ready cards.

Prefer `cover` metadata plus explicit media access endpoint if URL expiry causes stale-card risk.

## Location Contract

Accepted endpoints:

```http
PUT /api/records/{recordId}/location
DELETE /api/records/{recordId}/location
```

Request:

```json
{
  "source": "MAP_PICKER",
  "name": "人民公园",
  "address": "上海市黄浦区南京西路",
  "latitude": 31.2317,
  "longitude": 121.4746
}
```

Source values:

- `CURRENT_LOCATION`
- `MAP_PICKER`
- `MANUAL`

Validation:

- Owner-scoped.
- Record is `DRAFT`.
- `CURRENT_LOCATION` and `MAP_PICKER` require latitude and longitude.
- `MANUAL` requires at least one of `name` or `address`; coordinates are optional.
- Backend does not call geocoding or reverse-geocoding services in M4.

Accepted `RecordLocationVO`:

```json
{
  "source": "MAP_PICKER",
  "name": "人民公园",
  "address": "上海市黄浦区南京西路",
  "latitude": 31.2317,
  "longitude": 121.4746
}
```

Record detail/time review should include `location`.

List/timeline/home MAY include compact `locationLabel` only if UI needs it; time review MUST include full location when present.

## Timeline Filtering And Pagination Contract

### Endpoint And Query

Accepted endpoint:

- `GET /api/records/timeline`

Accepted optional query fields:

- `tagId: Long` — one enabled/shared tag only
- `year: Integer` — `1970..9999`
- `month: Integer` — `1..12`; requires `year`
- `day: Integer` — valid calendar day; requires `year` and `month`
- `pageNum: Integer` — default `1`, minimum `1`
- `pageSize: Integer` — default `20`, minimum `1`, maximum `50`

Tag and date filters combine with AND semantics. Multiple `tagId` values and multi-tag ANY/ALL behavior are not part of M4.

`RecordTimelineQuery` MAY reuse the existing `PageQuery` shape, but it MUST enforce the accepted timeline defaults and limits itself. The current global `PageQuery` defaults to `pageSize=10` and allows up to `200`; inheriting those values unchanged does not satisfy this contract. Timeline requests MUST default to `20` and reject values above `50`.

Date filters use record `createdAt` in the `Asia/Shanghai` business timezone. They MUST NOT use `unlockAt`, `sealedAt`, or `unlockedAt`. The service should use the existing `app.time.zone-id`/business clock contract to convert the selected granularity into `LocalDateTime` `[createdFrom, createdBefore)` boundaries; it MUST NOT use the JVM system-default timezone. The mapper then uses range predicates rather than wrapping `created_at` in `YEAR()`, `MONTH()`, or `DAY()` functions.

Invalid month/day dependency or an impossible calendar date returns `40000 BAD_REQUEST`. A syntactically valid but missing, disabled, or non-matching `tagId`, or any valid filter with no matching records, returns a successful empty page without exposing other users' records. Filtering MUST NOT make disabled tags queryable when `/api/tags` only exposes enabled tags.

### Ordering And Pagination

Accepted record ordering:

- `created_at DESC, id DESC`

Pagination is applied to records before grouping. The implementation reuses page-number pagination for consistency with current record-list APIs. Filter changes or reset restart at page 1.

Before adding an index, implementation MUST inspect the current schema/query plan. If required, the smallest expected record traversal index is `(user_id, created_at, id)`; tag filtering should also have an index that supports tag-to-record lookup.

### Response

The existing `ApiResponse` wrapper remains unchanged. `data` becomes `TimelinePageVO`:

```json
{
  "groups": [
    {
      "yearMonth": "2026-06",
      "items": []
    }
  ],
  "total": 42,
  "pageNum": 1,
  "pageSize": 20,
  "hasMore": true
}
```

Field semantics:

- `groups`: records from the current page grouped by `createdAt` year-month
- `total`: total matching record count, not group count
- `pageNum`: applied page number
- `pageSize`: applied page size
- `hasMore`: whether another record page exists

A year-month may appear again on a later page when the page boundary splits that month. The Mini Program MUST merge repeated groups and deduplicate items by record id during normal sequential loading.

The backend response change and both real/preview frontend consumers MUST land in the same implementation checkpoint. Do not leave an intermediate state where the backend returns `TimelinePageVO` while the Mini Program still expects `TimelineGroupVO[]`, or vice versa.

## Record Response Extensions

Accepted additions:

`RecordDetailVO`:

- `location`
- `attachments`
- `cover`

`RecordListItemVO`:

- `cover`
- optional `locationLabel`

`TimelineItemVO`:

- `cover`
- optional `locationLabel`

`TimelinePageVO`:

- `groups: List<TimelineGroupVO>`
- `total: long`
- `pageNum: int`
- `pageSize: int`
- `hasMore: boolean`

Do not embed permanent signed URLs in persisted models. If list/timeline cards need display-ready cover URLs, backend may generate short-lived URLs at response time.

## Error Semantics

Accepted default:

- Reuse current `ApiResponse` wrapper.
- Reuse current HTTP status conventions.
- Use `40000 BAD_REQUEST` for validation and illegal lifecycle mutation.
- Use `40100 UNAUTHORIZED` for missing/invalid auth.
- Use `40300 FORBIDDEN` for ownership/permission violations where not using safe not-found.
- Use `40400 NOT_FOUND` for missing records/attachments or safe not-found ownership protection.
- Use `50000 INTERNAL_ERROR` with HTTP 503 for upstream AI/object-storage unavailable where current code style already uses service unavailable.

Frontend-visible messages should be specific enough to distinguish:

- AI not configured
- AI unavailable
- storage not configured
- upload token unavailable
- object verification failed
- media access URL unavailable
- record is sealed and cannot be modified
- attachment limit exceeded
- total attachment size exceeded

If frontend needs machine-readable substatus beyond `code`, add scoped `status` fields to response VOs instead of inventing many global error codes first.

## Accepted Contract Decisions From User Confirmation

The following decisions were accepted by the user on 2026-06-17:

1. M4 accepts the REST endpoints under `/api/records/{recordId}`.
2. M4 implements one OpenAI-compatible adapter first.
3. M4 defers a dedicated NVIDIA NIM adapter.
4. Upload-authorization issuance remains stateless, and attachments are persisted only after configured-provider verification.
5. Draft delete routes by persisted provider and requires successful remote delete before metadata removal, except object-not-found may be treated as safe cleanup.
6. Signed URL expiry defaults to 600 seconds and remains configurable.
7. Backend geocoding and reverse geocoding stay out of M4.
8. Record create/update payloads remain focused on text fields; location, media, and cover use separate endpoints.
9. On 2026-06-21 the user replaced the Qiniu-only decision with a configurable provider-neutral contract. Qiniu and S3-compatible providers are accepted; provider-specific features outside the record attachment lifecycle remain out of scope.
10. On 2026-06-22 the user accepted the timeline filtering and pagination contract: `createdAt` date semantics in `Asia/Shanghai`, one tag at a time, AND composition, `pageNum`/`pageSize`, stable `created_at DESC, id DESC` ordering, and `TimelinePageVO` grouped responses.

Agents MUST NOT reopen these questions during implementation unless new code facts make the accepted contract impossible or unsafe. If that happens, the agent MUST document the blocker and ask before changing this file.
